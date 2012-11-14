/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.temptarget;

import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.fudgemsg.mapping.FudgeDeserializer;
import org.fudgemsg.mapping.FudgeSerializer;

import com.opengamma.util.ArgumentChecker;

/**
 * Test implementation of {@link TempTarget}.
 */
public final class MockTempTarget extends TempTarget {

  private static final long serialVersionUID = 1L;

  private final String _value;

  public MockTempTarget(final String value) {
    ArgumentChecker.notNull(value, "value");
    _value = value;
  }

  private MockTempTarget(final FudgeDeserializer deserializer, final FudgeMsg msg) {
    super(deserializer, msg);
    _value = msg.getString("value");
  }

  public static MockTempTarget fromFudgeMsg(final FudgeDeserializer deserializer, final FudgeMsg msg) {
    return new MockTempTarget(deserializer, msg);
  }

  @Override
  public void toFudgeMsg(final FudgeSerializer serializer, final MutableFudgeMsg message) {
    super.toFudgeMsg(serializer, message);
    serializer.addToMessage(message, "value", null, _value);
  }

  // TempTarget

  @Override
  protected boolean equalsImpl(final Object o) {
    return _value.equals(((MockTempTarget) o)._value);
  }

  @Override
  protected int hashCodeImpl() {
    return _value.hashCode();
  }

}