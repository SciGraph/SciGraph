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

import io.scigraph.annotation.EntityModule;
import io.scigraph.lexical.LexicalLibModule;
import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphTransactionalImpl;
import io.scigraph.neo4j.Neo4jModule;
import io.scigraph.opennlp.OpenNlpModule;
import io.scigraph.owlapi.curies.CurieModule;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.scigraph.services.jersey.dynamic.DynamicResourceModule;
import io.scigraph.services.refine.RefineModule;

import java.util.Map;

import io.swagger.models.Path;
import ru.vyarus.dropwizard.guice.module.support.ConfigurationAwareModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

class SciGraphApplicationModule extends AbstractModule implements ConfigurationAwareModule<ApplicationConfiguration> {

  ApplicationConfiguration configuration;

  @Override
  public void setConfiguration(ApplicationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new Neo4jModule(configuration.getGraphConfiguration(), true, true));
    bind(Graph.class).to(GraphTransactionalImpl.class);
    install(new EntityModule());
    install(new LexicalLibModule());
    install(new OpenNlpModule());
    install(new RefineModule(configuration.getServiceMetadata()));
    install(new DynamicResourceModule());
    install(new CurieModule());
  }

  @Provides
  Map<String,Path> getPaths() {
    return configuration.getCypherResources();
  }

}