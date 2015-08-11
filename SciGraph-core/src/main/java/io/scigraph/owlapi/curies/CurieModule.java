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
package io.scigraph.owlapi.curies;

import java.lang.reflect.Method;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;

public class CurieModule extends AbstractModule {

  private static final class TransactionMethodMatcher extends AbstractMatcher<Method> {
    @Override
    public boolean matches(final Method method) {
      return method.isAnnotationPresent(AddCuries.class) && !method.isSynthetic();
    }
  }

  @Override
  protected void configure() {
    CurieAdder adder = new CurieAdder();
    bindInterceptor(Matchers.annotatedWith(AddCuries.class), new TransactionMethodMatcher(), adder);
    requestInjection(adder);
  }

}
