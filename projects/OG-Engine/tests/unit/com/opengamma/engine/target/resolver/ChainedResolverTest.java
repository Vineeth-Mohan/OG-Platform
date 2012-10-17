/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.target.resolver;

import static org.testng.Assert.assertEquals;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.opengamma.engine.target.resolver.ChainedResolver;
import com.opengamma.engine.target.resolver.Resolver;
import com.opengamma.id.UniqueId;
import com.opengamma.util.money.Currency;

/**
 * Test the {@link ChainedResolver} class
 */
@Test
public class ChainedResolverTest {

  @SuppressWarnings("unchecked")
  public void testFirst() {
    final Resolver first = Mockito.mock(Resolver.class);
    final Resolver second = Mockito.mock(Resolver.class);
    final Resolver chained = ChainedResolver.CREATE.execute(second, first);
    Mockito.when(first.resolve(UniqueId.of("Foo", "1"))).thenReturn(Currency.USD);
    Mockito.when(second.resolve(UniqueId.of("Foo", "1"))).thenReturn(Currency.GBP);
    assertEquals(chained.resolve(UniqueId.of("Foo", "1")), Currency.USD);
    Mockito.verifyZeroInteractions(second);
  }

  @SuppressWarnings("unchecked")
  public void testSecond() {
    final Resolver first = Mockito.mock(Resolver.class);
    final Resolver second = Mockito.mock(Resolver.class);
    final Resolver chained = ChainedResolver.CREATE.execute(second, first);
    Mockito.when(first.resolve(UniqueId.of("Foo", "1"))).thenReturn(null);
    Mockito.when(second.resolve(UniqueId.of("Foo", "1"))).thenReturn(Currency.GBP);
    assertEquals(chained.resolve(UniqueId.of("Foo", "1")), Currency.GBP);
  }
  
}