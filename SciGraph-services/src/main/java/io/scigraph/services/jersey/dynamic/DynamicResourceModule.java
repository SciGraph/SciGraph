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
package io.scigraph.services.jersey.dynamic;

import io.scigraph.internal.EvidenceAspect;
import io.scigraph.internal.GraphAspect;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class DynamicResourceModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
    .implement(CypherInflector.class, CypherInflector.class)
    .build(CypherInflectorFactory.class));
    install(new FactoryModuleBuilder()
    .implement(DynamicCypherResource.class, DynamicCypherResource.class)
    .build(DynamicCypherResourceFactory.class));
  }

  @Provides
  Map<String, GraphAspect> getAspectMap(EvidenceAspect evidenceAspect) {
    Map<String, GraphAspect> aspectMap = new HashMap<>();
    aspectMap.put("evidence", evidenceAspect);
    return aspectMap;
  }

}
