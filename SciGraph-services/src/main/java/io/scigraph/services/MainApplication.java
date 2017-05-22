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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.scigraph.services.jersey.MediaTypeMappings;
import io.scigraph.services.jersey.dynamic.DynamicCypherResourceFactory;
import io.scigraph.services.jersey.dynamic.SwaggerFilter;
import io.scigraph.services.resources.SwaggerJsonBareService;
import io.scigraph.services.swagger.SwaggerDocUrlFilter;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Path;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.filter.UriConnegFilter;

import ru.vyarus.dropwizard.guice.GuiceBundle;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import io.swagger.config.ScannerFactory;
import io.swagger.jaxrs.config.DefaultJaxrsScanner;

public class MainApplication extends Application<ApplicationConfiguration> {

  public static void main(String[] args) throws Exception {
    new MainApplication().run(args);
  }

  TransparentInjectorFactory factory = new TransparentInjectorFactory();

  @Override
  public void initialize(Bootstrap<ApplicationConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/swagger/", "/docs", "index.html"));
    bootstrap.addBundle(new ViewBundle<ApplicationConfiguration>() {
      @Override
      public Map<String, Map<String, String>> getViewConfiguration(
          ApplicationConfiguration configuration) {
        return new HashMap<>();
      }
    });
    bootstrap.addBundle(GuiceBundle.builder()
        .enableAutoConfig("io.scigraph.services")
        .injectorFactory(factory).modules(new SciGraphApplicationModule()).build());
  }

  /***
   * The context path must be set before configuring swagger
   * @param environment
   */
  void configureSwagger(Environment environment, String basePath) {
    environment.jersey().register(new ApiListingResource());
    environment.jersey().register(new SwaggerJsonBareService());
    environment.jersey().register(new SwaggerSerializers());
    ScannerFactory.setScanner(new DefaultJaxrsScanner());
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    BeanConfig config = new BeanConfig();

    // api specific configuration
    config.setTitle("SciGraph");
    config.setVersion("1.0.1");
    config.setResourcePackage("io.scigraph.services.resources");
    config.setScan(true);
    // TODO: Fix this so the swagger client generator can work correctly
    config.setBasePath("/" + basePath);
  }

  void configureCors(Environment environment) {
    final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

    // Configure CORS parameters
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

    // Add URL mapping
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
  }

  void addWriters(JerseyEnvironment environment) throws Exception {
    for (ClassInfo classInfo: ClassPath.from(getClass().getClassLoader()).getTopLevelClasses("io.scigraph.services.jersey.writers")) {
      if (!Modifier.isAbstract(classInfo.load().getModifiers())) {
        environment.register(factory.getInjector().getInstance(classInfo.load()));
      }
    }
  }

  @Override
  public void run(ApplicationConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(new UriConnegFilter(new MediaTypeMappings(), Collections.<String, String>emptyMap()));
    Map<String, Object> props = new HashMap<>();
    props.put(MessageProperties.LEGACY_WORKERS_ORDERING, true);
    environment.jersey().getResourceConfig().addProperties(props);
    addWriters(environment.jersey());
    configureCors(environment);

    //TODO: This path should not be hard coded.
    configureSwagger(environment, "scigraph");
    environment.servlets().
            addFilter("Swagger Filter", factory.getInjector().getInstance(SwaggerFilter.class))
    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/swagger.json", "/swagger");

    environment.servlets().addFilter("swaggerDocResolver", new SwaggerDocUrlFilter())
    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

    DynamicCypherResourceFactory cypherFactory = factory.getInjector().getInstance(DynamicCypherResourceFactory.class);
    for (Map.Entry<String,Path> config: configuration.getCypherResources().entrySet()) {
      environment.jersey().getResourceConfig().registerResources(cypherFactory.create(config.getKey(), config.getValue()).getBuilder().build());
    }
  }

}
