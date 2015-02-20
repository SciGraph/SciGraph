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

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.filter.UriConnegFilter;

import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.injector.InjectorFactory;
import ru.vyarus.dropwizard.guice.module.support.ConfigurationAwareModule;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

import edu.sdsc.scigraph.annotation.EntityModule;
import edu.sdsc.scigraph.lexical.LexicalLibModule;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.opennlp.OpenNlpModule;
import edu.sdsc.scigraph.services.configuration.ApplicationConfiguration;
import edu.sdsc.scigraph.services.jersey.MediaTypeMappings;
import edu.sdsc.scigraph.services.jersey.dynamic.DynamicCypherResourceFactory;
import edu.sdsc.scigraph.services.jersey.dynamic.DynamicResourceModule;
import edu.sdsc.scigraph.services.jersey.dynamic.SwaggerFilter;
import edu.sdsc.scigraph.services.refine.RefineModule;
import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;

public class MainApplication extends Application<ApplicationConfiguration> {

  public static void main(String[] args) throws Exception {
    new MainApplication().run(args);
  }

  TransparentInjectorFactory factory = new TransparentInjectorFactory();

  /*** 
   * This allows access to the injector from {@link #configureSwagger(Environment, String)} 
   */
  private static class TransparentInjectorFactory implements InjectorFactory {

    Injector i;

    @Override
    public Injector createInjector(Stage stage, Iterable<? extends Module> modules) {
      i = Guice.createInjector(modules);
      return i;
    }

    Injector getInjector() {
      return i;
    }
  }

  static class MetaModule extends AbstractModule implements ConfigurationAwareModule<ApplicationConfiguration> {

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
    }
    
    @Provides
    List<Apis> getApis() {
      return configuration.getCypherResources();
    }
    

  }

  @Override
  public void initialize(Bootstrap<ApplicationConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/swagger/", "/docs", "index.html"));
    bootstrap.addBundle(new ViewBundle());
    bootstrap.addBundle(GuiceBundle.builder()
        .enableAutoConfig("edu.sdsc.scigraph.services")
        .injectorFactory(factory).modules(new MetaModule()).build());
  }

  /***
   * The context path must be set before configuring swagger
   * @param environment
   */
  void configureSwagger(Environment environment, String basePath) {
    environment.jersey().register(new ApiListingResourceJSON());
    environment.jersey().register(new ApiDeclarationProvider());
    environment.jersey().register(new ResourceListingProvider());
    ScannerFactory.setScanner(new DefaultJaxrsScanner());
    ClassReaders.setReader(new DefaultJaxrsApiReader());
    SwaggerConfig config = ConfigFactory.config();
    config.setApiVersion("1.0.1");
    config.setBasePath("../../" + basePath);
  }

  void addWriters(JerseyEnvironment environment) throws Exception {
    for (ClassInfo classInfo: ClassPath.from(getClass().getClassLoader()).getTopLevelClasses("edu.sdsc.scigraph.services.jersey.writers")) {
      environment.register(classInfo.load().newInstance());
    }
  }

  @Override
  public void run(ApplicationConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(new UriConnegFilter(new MediaTypeMappings(), Collections.<String, String>emptyMap()));
    Map<String, Object> props = new HashMap<>();
    props.put(MessageProperties.LEGACY_WORKERS_ORDERING, true);
    environment.jersey().getResourceConfig().addProperties(props);
    addWriters(environment.jersey());
    //TODO: This path should not be hard coded.
    configureSwagger(environment, "scigraph");
    environment.servlets().addFilter("Swagger Filter", factory.getInjector().getInstance(SwaggerFilter.class))
    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/api-docs/");

    DynamicCypherResourceFactory cypherFactory = factory.getInjector().getInstance(DynamicCypherResourceFactory.class);
    for (Apis config: configuration.getCypherResources()) {
      environment.jersey().getResourceConfig().registerResources(cypherFactory.create(config).getBuilder().build());
    }
  }

}
