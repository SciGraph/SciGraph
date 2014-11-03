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
package edu.sdsc.scigraph.owlapi.loader;

import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration;

class OwlLoaderModule extends AbstractModule {

  OwlLoadConfiguration config;

  public OwlLoaderModule(OwlLoadConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(OwlLoadConfiguration.class).toInstance(config);
    bindConstant().annotatedWith(Names.named("uniqueProperty")).to(CommonProperties.URI);
    bind(new TypeLiteral<Set<String>>() {
    }).annotatedWith(Names.named("indexedProperties")).toInstance(config.getIndexedNodeProperties());
    bind(new TypeLiteral<Set<String>>() {
    }).annotatedWith(Names.named("exactProperties")).toInstance(config.getExactNodeProperties());
    bind(new TypeLiteral<Map<String, String>>() {
    }).annotatedWith(Names.named("owl.categories")).toInstance(config.getCategories());
  }

  @Provides
  @Singleton
  BatchInserter getInserter() {
    BatchOwlLoader.logger.info("Getting BatchInserter");
    return BatchInserters.inserter(config.getOntologyConfiguration().getGraphLocation());
  }

}