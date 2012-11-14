/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.time.Instant;
import javax.time.calendar.LocalDate;
import javax.time.calendar.LocalDateTime;
import javax.time.calendar.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.ViewComputationResultModel;
import com.opengamma.engine.view.ViewDefinition;
import com.opengamma.engine.view.ViewDeltaResultModel;
import com.opengamma.engine.view.ViewProcessor;
import com.opengamma.engine.view.calc.ViewCycleMetadata;
import com.opengamma.engine.view.client.ViewClient;
import com.opengamma.engine.view.compilation.CompiledViewDefinition;
import com.opengamma.engine.view.execution.ArbitraryViewCycleExecutionSequence;
import com.opengamma.engine.view.execution.ExecutionOptions;
import com.opengamma.engine.view.execution.ViewCycleExecutionOptions;
import com.opengamma.engine.view.execution.ViewExecutionFlags;
import com.opengamma.engine.view.execution.ViewExecutionOptions;
import com.opengamma.engine.view.listener.ViewResultListener;
import com.opengamma.financial.OpenGammaExecutionContext;
import com.opengamma.financial.analytics.timeseries.DateConstraint;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.ConfigSearchResult;
import com.opengamma.util.async.AsynchronousExecution;
import com.opengamma.util.async.AsynchronousOperation;
import com.opengamma.util.async.ResultCallback;
import com.opengamma.util.time.DateUtils;

/**
 * Iterates a view client over historical data to produce historical valuations of one or more targets. The targets required and the parameters for the valuation are encoded into a
 * {@link ViewEvaluationTarget}. The results of the valuation are written to the value cache and dependent nodes in the graph will extract time series appropriate to their specific targets.
 */
public class ViewEvaluationFunction extends AbstractFunction.NonCompiledInvoker {

  private static final Logger s_logger = LoggerFactory.getLogger(ViewEvaluationFunction.class);

  // CompiledFunctionDefinition

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.of(ViewEvaluationTarget.class);
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
    return Collections.singleton(new ValueSpecification(ValueRequirementNames.VALUE, target.toSpecification(), createValueProperties().get()));
  }

  @Override
  public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue) {
    return Collections.emptySet();
  }

  protected UniqueId storeViewDefinition(final FunctionExecutionContext executionContext, final UniqueId targetId, final ViewDefinition viewDefinition) {
    final String name = targetId.toString(); // Use the tempTarget identifier as a name to reuse an existing config item
    final ConfigMaster master = OpenGammaExecutionContext.getConfigMaster(executionContext);
    if (master == null) {
      throw new IllegalStateException("Execution context does not contain a " + OpenGammaExecutionContext.CONFIG_MASTER_NAME);
    }
    final ConfigSearchRequest<ViewDefinition> request = new ConfigSearchRequest<ViewDefinition>(ViewDefinition.class);
    request.setName(name);
    final ConfigSearchResult<ViewDefinition> result = master.search(request);
    if (result.getDocuments() != null) {
      for (final ConfigDocument document : result.getDocuments()) {
        if (viewDefinition.equals(document.getValue().getValue())) {
          // Found a matching one
          s_logger.debug("Using previous view definition {}", document.getUniqueId());
          return document.getUniqueId();
        } else {
          // Found a dead one; either our temp target unique identifiers are not unique (different repositories MUST have different schemes) or the identifier
          // sequence has been restarted/repeated and is colliding with old or dead configuration documents.
          s_logger.info("Deleting expired view definition {}", document.getUniqueId());
          master.removeVersion(document.getUniqueId());
        }
      }
    }
    final ConfigItem<ViewDefinition> item = ConfigItem.of(viewDefinition);
    item.setName(viewDefinition.getUniqueId().toString());
    final UniqueId uid = master.add(new ConfigDocument(item)).getUniqueId();
    s_logger.info("Created new view definition {} for {}", uid, name);
    return uid;
  }

  protected ViewExecutionOptions getExecutionOptions(final FunctionExecutionContext executionContext, final ViewEvaluationTarget target) {
    final EnumSet<ViewExecutionFlags> flags = EnumSet.of(ViewExecutionFlags.WAIT_FOR_INITIAL_TRIGGER);
    LocalDate startDate = DateConstraint.getLocalDate(executionContext, target.getFirstValuationDate());
    if (startDate == null) {
      startDate = executionContext.getValuationClock().today();
    }
    if (!target.isIncludeFirstValuationDate()) {
      startDate = startDate.plusDays(1);
    }
    LocalDate finishDate = DateConstraint.getLocalDate(executionContext, target.getLastValuationDate());
    if (finishDate == null) {
      finishDate = executionContext.getValuationClock().today();
    }
    if (!target.isIncludeLastValuationDate()) {
      finishDate = finishDate.minusDays(1);
    }
    if (startDate.isAfter(finishDate)) {
      throw new IllegalArgumentException("First valuation date " + startDate + " is after last valuation date " + finishDate);
    }
    LocalDateTime date = LocalDateTime.of(startDate, target.getValuationTime());
    final Collection<ViewCycleExecutionOptions> cycles = new ArrayList<ViewCycleExecutionOptions>(DateUtils.getDaysBetween(startDate, true, finishDate, true));
    do {
      final ZonedDateTime valuation = ZonedDateTime.of(date, target.getTimeZone());
      cycles.add(ViewCycleExecutionOptions.builder().setValuationTime(valuation).setMarketDataSpecification(MarketData.historical(startDate, null))
          .setResolverVersionCorrection(VersionCorrection.of(valuation, target.getCorrection())).create());
      if (date.toLocalDate().equals(finishDate)) {
        break;
      }
      date = date.plusDays(1);
    } while (true);
    return ExecutionOptions.of(new ArbitraryViewCycleExecutionSequence(cycles), flags);
  }

  // FunctionInvoker

  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target,
      final Set<ValueRequirement> desiredValues) throws AsynchronousExecution {
    final ViewEvaluationTarget viewEvaluation = (ViewEvaluationTarget) target.getValue();
    final UniqueId viewId = storeViewDefinition(executionContext, target.getUniqueId(), viewEvaluation.getViewDefinition());
    final ViewProcessor viewProcessor = OpenGammaExecutionContext.getViewProcessor(executionContext);
    if (viewProcessor == null) {
      throw new IllegalStateException("Execution context does not contain a " + OpenGammaExecutionContext.VIEW_PROCESSOR_NAME);
    }
    final ViewClient viewClient = viewProcessor.createViewClient(viewEvaluation.getViewDefinition().getMarketDataUser());
    final UniqueId viewClientId = viewClient.getUniqueId();
    s_logger.info("Created view client {}, connecting to {}", viewClientId, viewId);
    viewClient.attachToViewProcess(viewId, getExecutionOptions(executionContext, viewEvaluation), true);
    final AsynchronousOperation<Set<ComputedValue>> async = new AsynchronousOperation<Set<ComputedValue>>();
    final AtomicReference<ResultCallback<Set<ComputedValue>>> asyncResult = new AtomicReference<ResultCallback<Set<ComputedValue>>>(async.getCallback());
    viewClient.setResultListener(new ViewResultListener() {

      @Override
      public UserPrincipal getUser() {
        return viewEvaluation.getViewDefinition().getMarketDataUser();
      }

      @Override
      public void viewDefinitionCompiled(final CompiledViewDefinition compiledViewDefinition, final boolean hasMarketDataPermissions) {
        // This is good. Don't need to do anything.
        s_logger.debug("View definition compiled for {}", viewClientId);
      }

      @Override
      public void viewDefinitionCompilationFailed(final Instant valuationTime, final Exception exception) {
        s_logger.error("View compilation failure for {} - {}", viewClientId, exception);
        final ResultCallback<?> callback = asyncResult.getAndSet(null);
        if (callback != null) {
          try {
            callback.setException(new OpenGammaRuntimeException("View definition compilation failed for " + valuationTime, exception));
          } finally {
            s_logger.info("Shutting down view client {}", viewClientId);
            viewClient.shutdown();
          }
        } else {
          s_logger.warn("Callback already made at view compilation failure for {}", viewClientId);
        }
      }

      @Override
      public void cycleStarted(final ViewCycleMetadata cycleMetadata) {
        // This is good. Don't need to do anything.
        s_logger.debug("Cycle started for {}", viewClientId);
      }

      @Override
      public void cycleFragmentCompleted(final ViewComputationResultModel fullFragment, final ViewDeltaResultModel deltaFragment) {
        // This shouldn't happen. We've asked for full results only
        s_logger.error("Cycle fragment completed for {}", viewClientId);
        viewClient.shutdown();
        assert false;
      }

      // TODO: are the listener methods guaranteed to be sequential; can cycleCompleted happen concurrently? can processComplete be called
      // before cycleCompleted returns?

      @Override
      public void cycleCompleted(final ViewComputationResultModel fullResult, final ViewDeltaResultModel deltaResult) {
        s_logger.debug("Cycle completed for {}", viewClientId);
        viewClient.triggerCycle();
        // TODO: extract the data
        throw new UnsupportedOperationException("TODO");
      }

      @Override
      public void cycleExecutionFailed(final ViewCycleExecutionOptions executionOptions, final Exception exception) {
        s_logger.error("Cycle execution failed for {}", viewClientId);
        final ResultCallback<?> callback = asyncResult.getAndSet(null);
        if (callback != null) {
          try {
            callback.setException(new OpenGammaRuntimeException("View cycle execution failed for " + executionOptions, exception));
          } finally {
            s_logger.info("Shutting down view client {}", viewClientId);
            viewClient.shutdown();
          }
        } else {
          s_logger.warn("Callback already made at cycle execution failure for {}", viewClientId);
        }
      }

      @Override
      public void processCompleted() {
        s_logger.info("View process completed for {}", viewClientId);
        final ResultCallback<Set<ComputedValue>> callback = asyncResult.getAndSet(null);
        if (callback != null) {
          try {
            // TODO: bundle up the results we've been collecting and report it to the callback
            throw new UnsupportedOperationException("TODO");
          } finally {
            s_logger.info("Shutting down view client {}", viewClientId);
            viewClient.shutdown();
          }
        } else {
          s_logger.warn("Callback already made at process completion of {}", viewClientId);
        }
      }

      @Override
      public void processTerminated(final boolean executionInterrupted) {
        // Normally we would have expected one of the other notifications, so if the callback exists we report an error
        final ResultCallback<?> callback = asyncResult.getAndSet(null);
        if (callback != null) {
          try {
            s_logger.error("View process terminated for {}", viewClientId);
            callback.setException(new OpenGammaRuntimeException(executionInterrupted ? "Execution interrupted" : "View process terminated"));
          } finally {
            s_logger.info("Shutting down view client {}", viewClientId);
            viewClient.shutdown();
          }
        } else {
          s_logger.debug("View process terminated for {}", viewClientId);
        }
      }

      @Override
      public void clientShutdown(final Exception e) {
        // Normally we would have expected one of the other notifications or this in response to us calling "shutdown", so if the callback exists we report an error
        final ResultCallback<?> callback = asyncResult.getAndSet(null);
        if (callback != null) {
          s_logger.error("View client shutdown for {}", viewClientId);
          callback.setException(new OpenGammaRuntimeException("View client shutdown", e));
        } else {
          s_logger.debug("View client shutdown for {}", viewClientId);
        }
      }

    });
    viewClient.triggerCycle();
    return async.getResult();
  }

}