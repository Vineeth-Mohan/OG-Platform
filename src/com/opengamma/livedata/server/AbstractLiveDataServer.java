/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.livedata.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.fudgemsg.FudgeFieldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.id.DomainSpecificIdentifier;
import com.opengamma.id.IdentificationDomain;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.LiveDataSubscriptionRequest;
import com.opengamma.livedata.LiveDataSubscriptionResponse;
import com.opengamma.livedata.LiveDataSubscriptionResponseMsg;
import com.opengamma.livedata.LiveDataSubscriptionResult;
import com.opengamma.livedata.entitlement.LiveDataEntitlementChecker;
import com.opengamma.livedata.entitlement.PermissiveLiveDataEntitlementChecker;
import com.opengamma.livedata.normalization.StandardRules;
import com.opengamma.livedata.resolver.DistributionSpecificationResolver;
import com.opengamma.livedata.resolver.NaiveDistributionSpecificationResolver;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.PerformanceCounter;

/**
 * The base class from which most OpenGamma Live Data feed servers should
 * extend. Handles most common cases for distributed contract management.
 * 
 * @author kirk
 */
public abstract class AbstractLiveDataServer {
  private static final Logger s_logger = LoggerFactory
      .getLogger(AbstractLiveDataServer.class);

  private final Set<MarketDataFieldReceiver> _fieldReceivers = new CopyOnWriteArraySet<MarketDataFieldReceiver>();
  private final Set<SubscriptionListener> _subscriptionListeners = new CopyOnWriteArraySet<SubscriptionListener>();
  private final Set<Subscription> _currentlyActiveSubscriptions = new CopyOnWriteArraySet<Subscription>();
  private final Map<String, Subscription> _securityUniqueId2Subscription = new ConcurrentHashMap<String, Subscription>();
  private final Map<LiveDataSpecification, Subscription> _fullyQualifiedSpec2Subscription = new ConcurrentHashMap<LiveDataSpecification, Subscription>();

  private final AtomicLong _numUpdatesSent = new AtomicLong(0);
  private final PerformanceCounter _performanceCounter = new PerformanceCounter(60);

  private final Lock _subscriptionLock = new ReentrantLock();

  private DistributionSpecificationResolver _distributionSpecificationResolver = new NaiveDistributionSpecificationResolver();
  private LiveDataEntitlementChecker _entitlementChecker = new PermissiveLiveDataEntitlementChecker();

  /**
   * @return the distributionSpecificationResolver
   */
  public DistributionSpecificationResolver getDistributionSpecificationResolver() {
    return _distributionSpecificationResolver;
  }

  /**
   * @param distributionSpecificationResolver
   *          the distributionSpecificationResolver to set
   */
  public void setDistributionSpecificationResolver(
      DistributionSpecificationResolver distributionSpecificationResolver) {
    _distributionSpecificationResolver = distributionSpecificationResolver;
  }

  public void addMarketDataFieldReceiver(MarketDataFieldReceiver fieldReceiver) {
    ArgumentChecker.checkNotNull(fieldReceiver, "Market Data Field Receiver");
    _fieldReceivers.add(fieldReceiver);
  }

  public void setMarketDataFieldReceivers(
      Collection<MarketDataFieldReceiver> fieldReceivers) {
    _fieldReceivers.clear();
    for (MarketDataFieldReceiver receiver : fieldReceivers) {
      addMarketDataFieldReceiver(receiver);
    }
  }

  public void addSubscriptionListener(SubscriptionListener subscriptionListener) {
    ArgumentChecker.checkNotNull(subscriptionListener, "Subscription Listener");
    _subscriptionListeners.add(subscriptionListener);
  }

  public void setSubscriptionListeners(
      Collection<SubscriptionListener> subscriptionListeners) {
    _subscriptionListeners.clear();
    for (SubscriptionListener subscriptionListener : subscriptionListeners) {
      addSubscriptionListener(subscriptionListener);
    }
  }

  /**
   * @return the entitlementChecker
   */
  public LiveDataEntitlementChecker getEntitlementChecker() {
    return _entitlementChecker;
  }

  /**
   * @param entitlementChecker
   *          the entitlementChecker to set
   */
  public void setEntitlementChecker(
      LiveDataEntitlementChecker entitlementChecker) {
    _entitlementChecker = entitlementChecker;
  }
  
  public String getDefaultNormalizationRuleSetId() {
    return StandardRules.getOpenGamma().getId();
  }

  /**
   * Subscribes to the given ticker using the underlying market
   * data provider.
   * 
   * @return Subscription handle
   */
  protected abstract Object doSubscribe(String uniqueId);

  /**
   * @param subscriptionHandle
   *          The object that was returned by subscribe()
   */
  protected abstract void doUnsubscribe(Object subscriptionHandle);

  /**
   * @return Identification domain that uniquely identifies securities for this
   *         type of server.
   */
  protected abstract IdentificationDomain getUniqueIdDomain();
  
  public DistributionSpecification subscribe(String securityUniqueId) {
    return subscribe(securityUniqueId, false);
  }
  
  public DistributionSpecification subscribe(String securityUniqueId, boolean persistent) {
    return subscribe(
        new LiveDataSpecification(
            getDefaultNormalizationRuleSetId(),
            new DomainSpecificIdentifier(getUniqueIdDomain(), securityUniqueId)), 
        persistent);    
  }

  public DistributionSpecification subscribe(LiveDataSpecification liveDataSpecificationFromClient, 
      boolean persistent) {
    DistributionSpecification distributionSpec = getDistributionSpecificationResolver()
        .getDistributionSpecification(liveDataSpecificationFromClient);
    LiveDataSpecification fullyQualifiedSpec = distributionSpec.getFullyQualifiedLiveDataSpecification();

    _subscriptionLock.lock();
    try {
      Subscription subscription;
      
      if (isSubscribedTo(fullyQualifiedSpec)) {
        s_logger.info("Already subscribed to {}", fullyQualifiedSpec);

        // Might be necessary to turn the subscription into a persistent one. We
        // never turn it back from persistent to non-persistent, however.
        subscription = getSubscription(fullyQualifiedSpec);
        if (!subscription.isPersistent() && persistent) {
          changePersistent(subscription, true);
        }

      } else {

        String securityUniqueId = fullyQualifiedSpec.getIdentifier(getUniqueIdDomain());
        if (securityUniqueId == null) {
          throw new IllegalArgumentException("Qualified spec "
              + fullyQualifiedSpec + " does not contain ID of domain "
              + getUniqueIdDomain());
        }
        Object subscriptionHandle = doSubscribe(securityUniqueId);

        subscription = new Subscription(securityUniqueId, subscriptionHandle, 
            persistent, Collections.singleton(distributionSpec));

        _currentlyActiveSubscriptions.add(subscription);
        _securityUniqueId2Subscription.put(securityUniqueId,
            subscription);

        for (SubscriptionListener listener : _subscriptionListeners) {
          try {
            listener.subscribed(subscription);
          } catch (RuntimeException e) {
            s_logger.error("Listener subscribe failed", e);
          }
        }

        s_logger.info("Created subscription to {}", fullyQualifiedSpec);
      }
      
      _fullyQualifiedSpec2Subscription.put(fullyQualifiedSpec,
          subscription);
      subscription.addDistributionSpec(distributionSpec);
    
    } finally {
      _subscriptionLock.unlock();
    }

    return distributionSpec;
  }

  /**
   * Processes a market data subscription request by going through the steps of
   * resolution, entitlement check, and subscription.
   */
  public LiveDataSubscriptionResponseMsg subscriptionRequestMade(
      LiveDataSubscriptionRequest subscriptionRequest) {

    ArrayList<LiveDataSubscriptionResponse> responses = new ArrayList<LiveDataSubscriptionResponse>();
    for (LiveDataSpecification requestedSpecification : subscriptionRequest
        .getSpecifications()) {

      try {

        // Check that this spec can be found
        DistributionSpecification distributionSpec;
        try {
          distributionSpec = getDistributionSpecificationResolver()
            .getDistributionSpecification(requestedSpecification);
        } catch (IllegalArgumentException e) {
          s_logger.info("Unable to work out distribution spec for specification {}",
              requestedSpecification);
          responses.add(new LiveDataSubscriptionResponse(
              requestedSpecification, null,
              LiveDataSubscriptionResult.NOT_PRESENT, e.getMessage(), null));
          continue;
        }

        // Entitlement check
        if (!getEntitlementChecker().isEntitled(
            subscriptionRequest.getUserName(), distributionSpec)) {
          s_logger.info("User {} not entitled to specification {}",
              subscriptionRequest.getUserName(), requestedSpecification);
          // TODO kirk 2009-10-28 -- Extend interface on EntitlementChecker to
          // get a user message.
          responses.add(new LiveDataSubscriptionResponse(
              requestedSpecification, requestedSpecification,
              LiveDataSubscriptionResult.NOT_AUTHORIZED, null, null));
          continue;
        }

        // Subscribe
        distributionSpec = subscribe(requestedSpecification, subscriptionRequest.getPersistent());

        LiveDataSubscriptionResponse response = new LiveDataSubscriptionResponse(
            requestedSpecification, 
            distributionSpec.getFullyQualifiedLiveDataSpecification(), 
            LiveDataSubscriptionResult.SUCCESS,
            null, 
            distributionSpec.getJmsTopic());
        responses.add(response);

      } catch (Exception e) {
        s_logger.error("Failed to subscribe to " + requestedSpecification, e);
        responses.add(new LiveDataSubscriptionResponse(requestedSpecification,
            null, LiveDataSubscriptionResult.INTERNAL_ERROR, e.getMessage(),
            null));
      }

    }

    return new LiveDataSubscriptionResponseMsg(subscriptionRequest
        .getUserName(), responses);
  }

  /**
   * Unsubscribes from market data.
   * Works even if the subscription is persistent.
   * 
   * @return true if a market data subscription was actually removed. false
   *         otherwise.
   */
  boolean unsubscribe(String securityUniqueId) {
    Subscription sub = getSubscription(securityUniqueId);
    if (sub == null) {
      return false;
    }
    changePersistent(sub, false); // make sure it will actually be deleted
    return unsubscribe(sub);
  }

  /**
   * Unsubscribes from market data.
   * If the subscription is persistent, this method is a no-op.
   * 
   * @return true if a market data subscription was actually removed. false
   *         otherwise.
   */
  boolean unsubscribe(Subscription subscription) {
    ArgumentChecker.checkNotNull(subscription, "Subscription");

    boolean actuallyUnsubscribed = false;

    _subscriptionLock.lock();
    try {
      if (isSubscribedTo(subscription) && !subscription.isPersistent()) {

        s_logger.info("Unsubscribing from {}", subscription);

        doUnsubscribe(subscription.getHandle());
        actuallyUnsubscribed = true;

        _currentlyActiveSubscriptions.remove(subscription);
        _securityUniqueId2Subscription.remove(subscription
            .getSecurityUniqueId());
        
        for (DistributionSpecification distributionSpec : subscription.getDistributionSpecs()) {
          _fullyQualifiedSpec2Subscription.remove(distributionSpec.getFullyQualifiedLiveDataSpecification());
        }

        for (SubscriptionListener listener : _subscriptionListeners) {
          try {
            listener.unsubscribed(subscription);
          } catch (RuntimeException e) {
            s_logger.error("Listener unsubscribe failed", e);
          }
        }

        s_logger.info("Unsubscribed from {}", subscription);

      } else {
        s_logger
            .warn(
                "Received unsubscription request for non-active/persistent subscription: {}",
                subscription);
      }

    } finally {
      _subscriptionLock.unlock();
    }

    return actuallyUnsubscribed;
  }

  boolean changePersistent(Subscription subscription, boolean persistent) {

    boolean actuallyChanged = false;

    _subscriptionLock.lock();
    try {
      if (isSubscribedTo(subscription)
          && persistent != subscription.isPersistent()) {

        s_logger.info("Changing subscription {} persistence status to {}",
            subscription, persistent);

        subscription.setPersistent(persistent);
        actuallyChanged = true;

        for (SubscriptionListener listener : _subscriptionListeners) {
          try {
            listener.persistentChanged(subscription);
          } catch (RuntimeException e) {
            s_logger.error("Listener persistentChanged failed", e);
          }
        }

      } else {
        s_logger.warn("No-op changePersistent() received: {} {}", subscription,
            persistent);
      }

    } finally {
      _subscriptionLock.unlock();
    }

    return actuallyChanged;
  }
  
  public boolean isSubscribedTo(String securityUniqueId) {
    return _securityUniqueId2Subscription.containsKey(securityUniqueId);    
  }
  
  public boolean isSubscribedTo(LiveDataSpecification fullyQualifiedSpec) {
    return _fullyQualifiedSpec2Subscription.containsKey(fullyQualifiedSpec);
  }

  public boolean isSubscribedTo(Subscription subscription) {
    return _currentlyActiveSubscriptions.contains(subscription);
  }

  public void liveDataReceived(String securityUniqueId,
      FudgeFieldContainer liveDataFields) {
    s_logger.debug("Live data received: {}", liveDataFields);

    _numUpdatesSent.incrementAndGet();
    _performanceCounter.hit();
    
    Subscription subscription = getSubscription(securityUniqueId);
    if (subscription == null) {
      s_logger.warn("Got data for invalid security unique ID {}", securityUniqueId);
      return;
    }

    // TODO kirk 2009-10-29 -- This needs to be much better.
    for (MarketDataFieldReceiver receiver : _fieldReceivers) {
      receiver.marketDataReceived(subscription, liveDataFields);
    }
  }

  public Set<String> getActiveDistributionSpecs() {
    Set<String> subscriptions = new HashSet<String>();
    for (Subscription subscription : _currentlyActiveSubscriptions) {
      for (DistributionSpecification distributionSpec : subscription.getDistributionSpecs()) {
        subscriptions.add(distributionSpec.toString());
      }
    }
    return subscriptions;
  }

  public Set<String> getActiveSubscriptionIds() {
    Set<String> subscriptions = new HashSet<String>();
    for (Subscription subscription : _currentlyActiveSubscriptions) {
      subscriptions.add(subscription.getSecurityUniqueId());
    }
    return subscriptions;
  }

  public int getNumActiveSubscriptions() {
    return _currentlyActiveSubscriptions.size();
  }

  public long getNumLiveDataUpdatesSent() {
    return _numUpdatesSent.get();
  }
  
  public double getNumLiveDataUpdatesSentPerSecondOverLastMinute() {
    return _performanceCounter.getHitsPerSecond();
  }

  public Set<Subscription> getSubscriptions() {
    return Collections.unmodifiableSet(_currentlyActiveSubscriptions);
  }

  public Subscription getSubscription(LiveDataSpecification spec) {
    return _fullyQualifiedSpec2Subscription.get(spec);
  }

  public Subscription getSubscription(String securityUniqueId) {
    return _securityUniqueId2Subscription.get(securityUniqueId);
  }

}
