/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.livedata.server;

import java.io.Serializable;

import org.fudgemsg.FudgeFieldContainer;

import com.opengamma.id.DomainSpecificIdentifiers;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.normalization.NormalizationRuleSet;
import com.opengamma.util.ArgumentChecker;

/**
 *     
 *
 * @author pietari
 */
public class DistributionSpecification implements Serializable {
  
  /** What market data is being distributed (e.g., AAPL stock) */
  private final DomainSpecificIdentifiers _identifiers;
  
  /** Topic it's published to */
  private final String _jmsTopic;
  
  /** The format it's distributed in */
  private final NormalizationRuleSet _normalizationRuleSet;
  
  public DistributionSpecification(DomainSpecificIdentifiers identifiers, 
      NormalizationRuleSet normalizationRuleSet,
      String jmsTopic) {
    ArgumentChecker.checkNotNull(identifiers, "Identifier(s) for the market data ticker this distribution spec relates to");
    ArgumentChecker.checkNotNull(normalizationRuleSet, "Normalization rules to apply before sending data to the JMS topic");
    ArgumentChecker.checkNotNull(jmsTopic, "JMS topic name");
    _identifiers = identifiers;
    _normalizationRuleSet = normalizationRuleSet;
    _jmsTopic = jmsTopic;
  }
  
  public LiveDataSpecification getFullyQualifiedLiveDataSpecification() {
    return new LiveDataSpecification(
        _normalizationRuleSet.getId(),
        _identifiers);
  }

  public NormalizationRuleSet getNormalizationRuleSet() {
    return _normalizationRuleSet;
  }
  
  public String getJmsTopic() {
    return _jmsTopic;
  }
  
  public String toString() {
    return _jmsTopic;
  }
  
  public FudgeFieldContainer getNormalizedMessage(FudgeFieldContainer msg) {
    return _normalizationRuleSet.getNormalizedMessage(msg);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((_identifiers == null) ? 0 : _identifiers.hashCode());
    result = prime * result + ((_jmsTopic == null) ? 0 : _jmsTopic.hashCode());
    result = prime
        * result
        + ((_normalizationRuleSet == null) ? 0 : _normalizationRuleSet
            .hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DistributionSpecification other = (DistributionSpecification) obj;
    if (_identifiers == null) {
      if (other._identifiers != null)
        return false;
    } else if (!_identifiers.equals(other._identifiers))
      return false;
    if (_jmsTopic == null) {
      if (other._jmsTopic != null)
        return false;
    } else if (!_jmsTopic.equals(other._jmsTopic))
      return false;
    if (_normalizationRuleSet == null) {
      if (other._normalizationRuleSet != null)
        return false;
    } else if (!_normalizationRuleSet.equals(other._normalizationRuleSet))
      return false;
    return true;
  }

}
