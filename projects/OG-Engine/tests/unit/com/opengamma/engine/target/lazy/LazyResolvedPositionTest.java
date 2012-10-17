/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.target.lazy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.testng.annotations.Test;

import com.opengamma.core.position.Position;
import com.opengamma.core.position.impl.SimplePosition;
import com.opengamma.engine.DefaultCachingComputationTargetResolver;
import com.opengamma.engine.target.MockComputationTargetResolver;
import com.opengamma.engine.target.TargetResolverPosition;
import com.opengamma.engine.target.lazy.LazyResolveContext;
import com.opengamma.engine.target.lazy.LazyResolvedPosition;
import com.opengamma.id.UniqueId;
import com.opengamma.util.ehcache.EHCacheUtils;

/**
 * Tests the {@link LazyResolvedPosition} class
 */
@Test
public class LazyResolvedPositionTest {

  public void testBasicMethods() {
    final MockComputationTargetResolver resolver = MockComputationTargetResolver.resolved();
    final Position underlying = resolver.getPositionSource().getPosition(UniqueId.of("Position", "0"));
    final Position position = new LazyResolvedPosition(new LazyResolveContext(resolver.getSecuritySource()), underlying);
    assertEquals(position.getAttributes(), underlying.getAttributes());
    assertEquals(position.getQuantity(), underlying.getQuantity());
    assertEquals(position.getTrades().size(), underlying.getTrades().size());
    assertEquals(position.getUniqueId(), underlying.getUniqueId());
  }

  public void testSerialization_full() throws Exception {
    final MockComputationTargetResolver resolver = MockComputationTargetResolver.resolved();
    final Position underlying = resolver.getPositionSource().getPosition(UniqueId.of("Position", "0"));
    Position position = new LazyResolvedPosition(new LazyResolveContext(resolver.getSecuritySource()), underlying);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(position);
    final Object resultObject = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    assertTrue(resultObject instanceof SimplePosition);
    position = (Position) resultObject;
    assertEquals(position.getAttributes(), underlying.getAttributes());
    assertEquals(position.getQuantity(), underlying.getQuantity());
    assertEquals(position.getTrades().size(), underlying.getTrades().size());
    assertEquals(position.getUniqueId(), underlying.getUniqueId());
  }

  public void testSerialization_targetResolver() throws Exception {
    final MockComputationTargetResolver resolver = MockComputationTargetResolver.resolved();
    final Position underlying = resolver.getPositionSource().getPosition(UniqueId.of("Position", "0"));
    Position position = new LazyResolvedPosition(new LazyResolveContext(resolver.getSecuritySource(), new DefaultCachingComputationTargetResolver(resolver,
        EHCacheUtils.createCacheManager())), underlying);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(position);
    final Object resultObject = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    assertTrue(resultObject instanceof TargetResolverPosition);
    position = (Position) resultObject;
    assertEquals(position.getAttributes(), underlying.getAttributes());
    assertEquals(position.getQuantity(), underlying.getQuantity());
    assertEquals(position.getTrades().size(), underlying.getTrades().size());
    assertEquals(position.getUniqueId(), underlying.getUniqueId());
  }

}