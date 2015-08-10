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
package io.scigraph.services;

import ru.vyarus.dropwizard.guice.injector.InjectorFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/*** 
 * This allows access to the {@link Injector} from {@link #configureSwagger(Environment, String)} 
 */
class TransparentInjectorFactory implements InjectorFactory {

  private Injector i;

  @Override
  public Injector createInjector(Stage stage, Iterable<? extends Module> modules) {
    i = Guice.createInjector(modules);
    return i;
  }

  Injector getInjector() {
    return i;
  }

}