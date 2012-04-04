/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.forex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.time.calendar.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.forex.calculator.PV01ForexCalculator;
import com.opengamma.analytics.financial.forex.method.MultipleCurrencyInterestRateCurveSensitivity;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.analytics.conversion.ForexSecurityConverter;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.fx.FXUtils;
import com.opengamma.financial.security.option.FXBarrierOptionSecurity;
import com.opengamma.financial.security.option.FXDigitalOptionSecurity;
import com.opengamma.financial.security.option.FXOptionSecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.DoublesPair;

/**
 * 
 */
public class ForexOptionBlackPV01Function extends AbstractFunction.NonCompiledInvoker {
  private static final Logger s_logger = LoggerFactory.getLogger(ForexOptionBlackPV01Function.class);
  private static final PV01ForexCalculator CALCULATOR = PV01ForexCalculator.getInstance();
  private static final ForexSecurityConverter CONVERTER = new ForexSecurityConverter();

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.SECURITY;
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() != ComputationTargetType.SECURITY) {
      return false;
    }
    return target.getSecurity() instanceof FXOptionSecurity || target.getSecurity() instanceof FXBarrierOptionSecurity || target.getSecurity() instanceof FXDigitalOptionSecurity;
  }

  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target, final Set<ValueRequirement> desiredValues) {
    final ZonedDateTime date = executionContext.getValuationClock().zonedDateTime();
    final FinancialSecurity security = (FinancialSecurity) target.getSecurity();
    final ValueRequirement desiredValue = desiredValues.iterator().next();
    final String curveName = desiredValue.getConstraint(ValuePropertyNames.CURVE);
    final String putCurveName = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_PUT_CURVE);
    final String putForwardCurveName = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE);
    final String putCurveCalculationMethod = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD);
    final String callCurveName = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_CALL_CURVE);
    final String callForwardCurveName = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE);
    final String callCurveCalculationMethod = desiredValue.getConstraint(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD);
    final String surfaceName = desiredValue.getConstraint(ValuePropertyNames.SURFACE);
    final String currency = desiredValue.getConstraint(ValuePropertyNames.CURRENCY);
    final String forwardCurveName;
    final Currency curveCurrency;
    final Currency foreignCurrency;
    if (currency.equals(security.accept(ForexVisitors.getPutCurrencyVisitor()).getCode())) {
      forwardCurveName = putForwardCurveName;
      curveCurrency = Currency.of(currency);
      foreignCurrency = security.accept(ForexVisitors.getCallCurrencyVisitor());
    } else {
      forwardCurveName = callForwardCurveName;
      curveCurrency = Currency.of(currency);
      foreignCurrency = security.accept(ForexVisitors.getPutCurrencyVisitor());
    }
    final String fullCurveName = curveName + "_" + curveCurrency;
    final Object curveSensitivitiesObject = inputs.getValue(getCurveSensitivitiesRequirement(putCurveName, putForwardCurveName, putCurveCalculationMethod,
        callCurveName, callForwardCurveName, callCurveCalculationMethod, surfaceName, target));
    if (curveSensitivitiesObject == null) {
      throw new OpenGammaRuntimeException("Could not get curve sensitivities");
    }
    final ValueRequirement spotRequirement = security.accept(ForexVisitors.getSpotIdentifierVisitor());
    final Object spotFXObject = inputs.getValue(spotRequirement);
    if (spotFXObject == null) {
      throw new OpenGammaRuntimeException("Could not get " + spotRequirement);
    }
    final double spotFX = (Double) spotFXObject;
    final MultipleCurrencyInterestRateCurveSensitivity curveSensitivities = (MultipleCurrencyInterestRateCurveSensitivity) curveSensitivitiesObject;
    final Map<String, List<DoublesPair>> sensitivitiesForCurrency = getSensitivitiesForCurve(curveSensitivities, curveCurrency, foreignCurrency, spotFX);
    final ValueProperties properties = getResultProperties(curveCurrency.getCode(), curveName, putCurveName, putForwardCurveName, putCurveCalculationMethod,
        callCurveName, callForwardCurveName, callCurveCalculationMethod, surfaceName);
    final InstrumentDefinition<?> definition = security.accept(CONVERTER);
    final InstrumentDerivative option = definition.toDerivative(date, new String[] {fullCurveName, forwardCurveName});
    final ValueSpecification spec = new ValueSpecification(ValueRequirementNames.PV01, target.toSpecification(), properties);
    final Map<String, Double> pv01 = option.accept(CALCULATOR, sensitivitiesForCurrency);
    return Collections.singleton(new ComputedValue(spec, pv01.get(fullCurveName)));
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
    final ValueSpecification resultSpec = new ValueSpecification(ValueRequirementNames.PV01, target.toSpecification(), getResultProperties());
    return Collections.singleton(resultSpec);
  }

  @Override
  public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue) {
    final FinancialSecurity security = (FinancialSecurity) target.getSecurity();
    final ValueProperties constraints = desiredValue.getConstraints();
    final Set<String> curveNames = constraints.getValues(ValuePropertyNames.CURVE);
    if (curveNames == null || curveNames.size() != 1) {
      s_logger.error("Did not specify a curve name for requirement {}", desiredValue);
      return null;
    }
    final Set<String> currencies = constraints.getValues(ValuePropertyNames.CURVE_CURRENCY);
    if (currencies == null || currencies.size() != 1) {
      s_logger.error("Did not specify a currency for requirement {}", desiredValue);
      return null;
    }
    final Set<String> putCurveNames = constraints.getValues(ForexOptionBlackFunction.PROPERTY_PUT_CURVE);
    if (putCurveNames == null || putCurveNames.size() != 1) {
      return null;
    }
    final Set<String> callCurveNames = constraints.getValues(ForexOptionBlackFunction.PROPERTY_CALL_CURVE);
    if (callCurveNames == null || callCurveNames.size() != 1) {
      return null;
    }
    final String curveName = curveNames.iterator().next();
    final String putCurveName = putCurveNames.iterator().next();
    final String callCurveName = callCurveNames.iterator().next();
    if (!(curveName.equals(putCurveName) || curveName.equals(callCurveName))) {
      s_logger.error("Did not specify a curve to which this security is sensitive; asked for {}", curveName);
      return null;
    }
    final Set<String> surfaceNames = constraints.getValues(ValuePropertyNames.SURFACE);
    if (surfaceNames == null || surfaceNames.size() != 1) {
      return null;
    }
    final Set<String> putForwardCurveNames = constraints.getValues(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE);
    if (putForwardCurveNames == null || putForwardCurveNames.size() != 1) {
      return null;
    }
    final Set<String> callForwardCurveNames = constraints.getValues(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE);
    if (callForwardCurveNames == null || callForwardCurveNames.size() != 1) {
      return null;
    }
    final Set<String> putCurveCalculationMethods = constraints.getValues(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD);
    if (putCurveCalculationMethods == null || putCurveCalculationMethods.size() != 1) {
      return null;
    }
    final Set<String> callCurveCalculationMethods = constraints.getValues(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD);
    if (callCurveCalculationMethods == null || callCurveCalculationMethods.size() != 1) {
      return null;
    }
    final String currency = currencies.iterator().next();
    final String putForwardCurveName = putForwardCurveNames.iterator().next();
    final String callForwardCurveName = callForwardCurveNames.iterator().next();
    final String putCurveCalculationMethod = putCurveCalculationMethods.iterator().next();
    final String callCurveCalculationMethod = callCurveCalculationMethods.iterator().next();
    final String surfaceName = surfaceNames.iterator().next();
    final ValueRequirement spotRequirement = security.accept(ForexVisitors.getSpotIdentifierVisitor());
    final Set<ValueRequirement> requirements = new HashSet<ValueRequirement>();
    requirements.add(spotRequirement);
    requirements.add(getCurveSpecRequirement(Currency.of(currency), curveName));
    requirements.add(getCurveSensitivitiesRequirement(putCurveName, putForwardCurveName, putCurveCalculationMethod, callCurveName, callForwardCurveName, callCurveCalculationMethod,
        surfaceName, target));
    return requirements;
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target, final Map<ValueSpecification, ValueRequirement> inputs) {
    String currency = null;
    String curveName = null;
    for (final Map.Entry<ValueSpecification, ValueRequirement> entry : inputs.entrySet()) {
      final ValueSpecification key = entry.getKey();
      if (key.getValueName().equals(ValueRequirementNames.YIELD_CURVE_SPEC)) {
        final ValueProperties constraints = key.getProperties();
        currency = key.getTargetSpecification().getUniqueId().getValue();
        curveName = constraints.getValues(ValuePropertyNames.CURVE).iterator().next();
        break;
      }
    }
    assert currency != null;
    assert curveName != null;
    final ValueSpecification resultSpec = new ValueSpecification(ValueRequirementNames.PV01, target.toSpecification(),
        getResultProperties(currency, curveName));
    return Collections.singleton(resultSpec);
  }

  private ValueRequirement getCurveSpecRequirement(final Currency currency, final String curveName) {
    final ValueProperties properties = ValueProperties.builder().with(ValuePropertyNames.CURVE, curveName).get();
    return new ValueRequirement(ValueRequirementNames.YIELD_CURVE_SPEC, ComputationTargetType.PRIMITIVE, currency.getUniqueId(), properties);
  }

  private ValueProperties getResultProperties() {
    return createValueProperties()
        .withAny(ValuePropertyNames.CURVE)
        .withAny(ValuePropertyNames.CURVE_CURRENCY)
        .withAny(ValuePropertyNames.CURRENCY)
        .with(ValuePropertyNames.CALCULATION_METHOD, ForexOptionBlackFunction.BLACK_METHOD)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD)
        .withAny(ValuePropertyNames.SURFACE).get();
  }

  private ValueProperties getResultProperties(final String currency, final String curveName) {
    return createValueProperties()
        .with(ValuePropertyNames.CURVE, curveName)
        .with(ValuePropertyNames.CURVE_CURRENCY, currency)
        .with(ValuePropertyNames.CURRENCY, currency)
        .with(ValuePropertyNames.CALCULATION_METHOD, ForexOptionBlackFunction.BLACK_METHOD)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE)
        .withAny(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD)
        .withAny(ValuePropertyNames.SURFACE).get();
  }

  private ValueProperties getResultProperties(final String ccy, final String curveName, final String putCurveName, final String putForwardCurveName,
      final String putCurveCalculationMethod, final String callCurveName, final String callForwardCurveName, final String callCurveCalculationMethod,
      final String surfaceName) {
    return createValueProperties()
        .with(ValuePropertyNames.CURVE, curveName)
        .with(ValuePropertyNames.CURVE_CURRENCY, ccy)
        .with(ValuePropertyNames.CURRENCY, ccy)
        .with(ValuePropertyNames.CALCULATION_METHOD, ForexOptionBlackFunction.BLACK_METHOD)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_CURVE, putCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE, putForwardCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD, putCurveCalculationMethod)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_CURVE, callCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE, callForwardCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD, callCurveCalculationMethod)
        .with(ValuePropertyNames.SURFACE, surfaceName).get();
  }

  private ValueRequirement getCurveSensitivitiesRequirement(final String putCurveName, final String putForwardCurveName, final String putCurveCalculationMethod, final String callCurveName,
      final String callForwardCurveName, final String callCurveCalculationMethod, final String surfaceName, final ComputationTarget target) {
    final ValueProperties properties = ValueProperties.builder()
        .with(ValuePropertyNames.CALCULATION_METHOD, ForexOptionBlackFunction.BLACK_METHOD)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_CURVE, putCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_FORWARD_CURVE, putForwardCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_PUT_CURVE_CALCULATION_METHOD, putCurveCalculationMethod)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_CURVE, callCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_FORWARD_CURVE, callForwardCurveName)
        .with(ForexOptionBlackFunction.PROPERTY_CALL_CURVE_CALCULATION_METHOD, callCurveCalculationMethod)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(ValuePropertyNames.CURRENCY, ForexOptionBlackSingleValuedFunction.getResultCurrency(target)).get();
    return new ValueRequirement(ValueRequirementNames.FX_CURVE_SENSITIVITIES, target.toSpecification(), properties);
  }

  //TODO should not live in financial
  private Map<String, List<DoublesPair>> getSensitivitiesForCurve(final MultipleCurrencyInterestRateCurveSensitivity curveSensitivities, final Currency curveCurrency,
      final Currency foreignCurrency, final double spotFX) {
    final Currency sensitivityCurrency = curveSensitivities.getCurrencies().iterator().next();
    if (curveCurrency.equals(sensitivityCurrency)) {
      return curveSensitivities.getSensitivity(curveCurrency).getSensitivities();
    }
    final Map<String, List<DoublesPair>> result = new HashMap<String, List<DoublesPair>>();
    double conversionFX;
    if (FXUtils.isInBaseQuoteOrder(curveCurrency, foreignCurrency)) {
      conversionFX = 1. / spotFX;
    } else {
      conversionFX = spotFX;
    }
    for (final Map.Entry<String, List<DoublesPair>> entry : curveSensitivities.getSensitivity(sensitivityCurrency).getSensitivities().entrySet()) {
      final List<DoublesPair> convertedSensitivities = new ArrayList<DoublesPair>();
      for (final DoublesPair pair : entry.getValue()) {
        final DoublesPair convertedPair = DoublesPair.of(pair.first, pair.second * conversionFX);
        convertedSensitivities.add(convertedPair);
      }
      result.put(entry.getKey(), convertedSensitivities);
    }
    return result;
  }
}
