/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.view.cache;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.fudgemsg.FudgeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.id.UniqueId;
import com.opengamma.transport.DirectFudgeConnection;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;

@Test
public class ReleaseCacheMessageTest {

  private static final Logger s_logger = LoggerFactory.getLogger(ReleaseCacheMessageTest.class);
  private static final FudgeContext s_fudgeContext = OpenGammaFudgeContext.getInstance();

  private static class ReportingBinaryDataStoreFactory implements BinaryDataStoreFactory {

    private final Set<ViewComputationCacheKey> _cachesCreated = new HashSet<ViewComputationCacheKey>();
    private final Set<ViewComputationCacheKey> _cachesDestroyed = new HashSet<ViewComputationCacheKey>();

    private final String _name;

    public ReportingBinaryDataStoreFactory(final String name) {
      _name = name;
    }

    @Override
    public BinaryDataStore createDataStore(final ViewComputationCacheKey cacheKey) {
      s_logger.debug("{} cache created - {}", _name, cacheKey);
      _cachesCreated.add(cacheKey);
      return new InMemoryBinaryDataStore() {

        @Override
        public void delete() {
          s_logger.debug("{} cache destroyed - {}", _name, cacheKey);
          _cachesDestroyed.add(cacheKey);
          super.delete();
        }

      };
    }

  }

  private void putStuffIntoCache(final ViewComputationCache cache) {
    cache.putPrivateValue(new ComputedValue(new ValueSpecification(new ValueRequirement("Value", "Foo"), "function ID"), "Bar"));
    cache.putSharedValue(new ComputedValue(new ValueSpecification(new ValueRequirement("Value", "Foo"), "function ID"), "Bar"));
  }
  
  private void pause () {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
  }

  private void delayedEquals(final int expectedSize, final Set<ViewComputationCacheKey> set) {
    for (int i = 0; i < 5; i++) {
      if (expectedSize == set.size()) {
        return;
      } else if (expectedSize < set.size()) {
        break;
      }
      pause ();
    }
    Assert.fail("expected=" + expectedSize + ", was=" + set.size());
  }

  @Test
  public void testCacheReleaseMessage() {
    final ReportingBinaryDataStoreFactory privateServerStore = new ReportingBinaryDataStoreFactory("server private");
    final ReportingBinaryDataStoreFactory sharedStore = new ReportingBinaryDataStoreFactory("server shared");
    final DefaultViewComputationCacheSource cacheSource = new DefaultViewComputationCacheSource(
        new InMemoryIdentifierMap(), s_fudgeContext, new DefaultFudgeMessageStoreFactory(privateServerStore,
            s_fudgeContext), new DefaultFudgeMessageStoreFactory(sharedStore, s_fudgeContext));
    s_logger.info("Creating server local caches");
    UniqueId viewCycle1Id = UniqueId.of("Test", "ViewCycle", "1");
    UniqueId viewCycle2Id = UniqueId.of("Test", "ViewCycle", "2");
    UniqueId viewCycle3Id = UniqueId.of("Test", "ViewCycle", "3");
    UniqueId viewCycle4Id = UniqueId.of("Test", "ViewCycle", "4");
    putStuffIntoCache(cacheSource.getCache(viewCycle1Id, "Config 1"));
    putStuffIntoCache(cacheSource.getCache(viewCycle1Id, "Config 2"));
    putStuffIntoCache(cacheSource.getCache(viewCycle2Id, "Config 1"));
    putStuffIntoCache(cacheSource.getCache(viewCycle2Id, "Config 2"));
    putStuffIntoCache(cacheSource.getCache(viewCycle3Id, "Config 1"));
    putStuffIntoCache(cacheSource.getCache(viewCycle3Id, "Config 2"));
    putStuffIntoCache(cacheSource.getCache(viewCycle4Id, "Config 1"));
    putStuffIntoCache(cacheSource.getCache(viewCycle4Id, "Config 2"));
    assertEquals(8, privateServerStore._cachesCreated.size());
    assertEquals(0, privateServerStore._cachesDestroyed.size());
    assertEquals(8, sharedStore._cachesCreated.size());
    assertEquals(0, sharedStore._cachesDestroyed.size());
    s_logger.info("Releasing server local caches");
    cacheSource.releaseCaches(viewCycle1Id);
    assertEquals(2, privateServerStore._cachesDestroyed.size());
    assertEquals(2, sharedStore._cachesDestroyed.size());
    cacheSource.releaseCaches(viewCycle3Id);
    assertEquals(4, privateServerStore._cachesDestroyed.size());
    assertEquals(4, sharedStore._cachesDestroyed.size());
    final ViewComputationCacheServer server = new ViewComputationCacheServer(cacheSource);
    final ReportingBinaryDataStoreFactory privateClientStore = new ReportingBinaryDataStoreFactory("client private");
    final DirectFudgeConnection conduit = new DirectFudgeConnection(cacheSource.getFudgeContext());
    conduit.connectEnd1(server);
    final RemoteViewComputationCacheSource remoteSource = new RemoteViewComputationCacheSource(new RemoteCacheClient(
        conduit.getEnd2()), new DefaultFudgeMessageStoreFactory(privateClientStore, s_fudgeContext), EHCacheUtils
        .createCacheManager());
    s_logger.info("Using server cache at remote client");
    putStuffIntoCache(remoteSource.getCache(viewCycle2Id, "Config 1"));
    assertEquals(8, privateServerStore._cachesCreated.size());
    assertEquals(8, sharedStore._cachesCreated.size());
    assertEquals(1, privateClientStore._cachesCreated.size());
    assertEquals(0, privateClientStore._cachesDestroyed.size());
    s_logger.info("Releasing cache used by remote client");
    cacheSource.releaseCaches(viewCycle2Id);
    assertEquals(6, privateServerStore._cachesDestroyed.size());
    assertEquals(6, sharedStore._cachesDestroyed.size());
    delayedEquals(1, privateClientStore._cachesDestroyed);
    s_logger.info("Releasing cache not used by remote client");
    cacheSource.releaseCaches(viewCycle4Id);
    assertEquals(8, privateServerStore._cachesDestroyed.size());
    assertEquals(8, sharedStore._cachesDestroyed.size());
    for (int i = 0; i < 5; i++) {
      assertEquals(1, privateClientStore._cachesDestroyed.size());
      pause ();
    }
    s_logger.info("Using new cache at remote client");
    UniqueId viewCycle5Id = UniqueId.of("Test", "ViewCycle", "5");
    putStuffIntoCache(remoteSource.getCache(viewCycle5Id, "Config 1"));
    assertEquals(9, privateServerStore._cachesCreated.size());
    assertEquals(9, sharedStore._cachesCreated.size());
    assertEquals(2, privateClientStore._cachesCreated.size());
    s_logger.info("Releasing cache used by remote client");
    cacheSource.releaseCaches(viewCycle5Id);
    assertEquals(9, privateServerStore._cachesDestroyed.size());
    assertEquals(9, sharedStore._cachesDestroyed.size());
    delayedEquals(2, privateClientStore._cachesDestroyed);
  }

}
