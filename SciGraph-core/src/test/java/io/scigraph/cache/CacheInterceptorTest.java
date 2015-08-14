/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class CacheInterceptorTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  Foo foo;
  Cache<CacheableMethodInvocation, Object> cache;

  @Before
  public void setup() {
    cache = CacheBuilder.newBuilder().build();
    Injector i = Guice.createInjector(new CacheModule(), new AbstractModule() {

      @Override
      protected void configure() {}

      @Provides
      @Singleton
      Cache<CacheableMethodInvocation, Object> getCache() {
        return cache;
      }
    });

    foo = i.getInstance(Foo.class);
  }

  @Test
  public void cacheBehaves() {
    assertThat(cache.size(), is(0L));
    foo.multiply(3, 2);
    assertThat(cache.size(), is(1L));
    foo.multiply(3, 2);
    assertThat(cache.size(), is(1L));
  }

  @Test
  public void throwExceptionsAreNotCached() throws Exception {
    assertThat(cache.size(), is(0L));
    exception.expect(Exception.class);
    foo.thower();
    assertThat(cache.size(), is(0L));
  }

  static class Foo {

    @Cacheable
    int multiply(int a, int b) {
      return a * b;
    }

    @Cacheable
    int thower() throws Exception {
      throw new Exception();
    }

  }

}
