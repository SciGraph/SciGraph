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
package edu.sdsc.scigraph.services;

import java.util.List;

import ru.vyarus.dropwizard.guice.module.support.ConfigurationAwareModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import edu.sdsc.scigraph.annotation.EntityModule;
import edu.sdsc.scigraph.lexical.LexicalLibModule;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.opennlp.OpenNlpModule;
import edu.sdsc.scigraph.owlapi.curies.CurieModule;
import edu.sdsc.scigraph.services.configuration.ApplicationConfiguration;
import edu.sdsc.scigraph.services.jersey.dynamic.DynamicResourceModule;
import edu.sdsc.scigraph.services.refine.RefineModule;
import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;

class SciGraphApplicationModule extends AbstractModule implements ConfigurationAwareModule<ApplicationConfiguration> {

  ApplicationConfiguration configuration;

  @Override
  public void setConfiguration(ApplicationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new Neo4jModule(configuration.getGraphConfiguration()));
    install(new EntityModule());
    install(new LexicalLibModule());
    install(new OpenNlpModule());
    install(new RefineModule(configuration.getServiceMetadata()));
    install(new DynamicResourceModule());
    install(new CurieModule());
  }

  @Provides
  List<Apis> getApis() {
    return configuration.getCypherResources();
  }

}