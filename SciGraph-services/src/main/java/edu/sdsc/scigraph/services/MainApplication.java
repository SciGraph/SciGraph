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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.eclipse.jetty.servlet.FilterHolder;
import org.glassfish.jersey.server.ServerProperties;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Joiner;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
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
import edu.sdsc.scigraph.services.auth.BasicAuthFilter;
import edu.sdsc.scigraph.services.configuration.ApiConfiguration;
import edu.sdsc.scigraph.services.configuration.ApplicationConfiguration;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.refine.RefineModule;

public class MainApplication extends Application<ApplicationConfiguration> {

  public static void main(String[] args) throws Exception {
    new MainApplication().run(args);
  }

  @Override
  public String getName() {
    return "SciCrunch Web Services";
  }

  @Override
  public void initialize(Bootstrap<ApplicationConfiguration> bootstrap) {
    initializeSwaggger(bootstrap);
    bootstrap.addBundle(new ViewBundle());
  }

  void addWriters(JerseyEnvironment environment, Injector i) throws IOException {
    for (ClassInfo classInfo: ClassPath.from(getClass().getClassLoader()).getTopLevelClasses("edu.sdsc.scigraph.services.jersey.writers")) {
      environment.register(i.getInstance(classInfo.load()));
    }
  }

  String getTypeMappings() {
    List<String> mappings = new ArrayList<>();
    mappings.add("xml" + " : " + MediaType.APPLICATION_XML_TYPE);
    mappings.add("json" + " : " + MediaType.APPLICATION_JSON_TYPE);
    mappings.add("jsonp" + " : " + CustomMediaTypes.APPLICATION_JSONP_TYPE);
    mappings.add("csv" + " : " + CustomMediaTypes.TEXT_CSV_TYPE);
    mappings.add("tsv" + " : " + CustomMediaTypes.TEXT_TSV_TYPE);
    mappings.add("ris" + " : " + CustomMediaTypes.APPLICATION_RIS_TYPE);
    mappings.add("graphson" + " : " + CustomMediaTypes.APPLICATION_GRAPHSON_TYPE);
    mappings.add("graphml" + " : " + CustomMediaTypes.APPLICATION_GRAPHML_TYPE);
    mappings.add("gml" + " : " + CustomMediaTypes.TEXT_GML_TYPE);
    mappings.add("jpg" + " : " + CustomMediaTypes.IMAGE_JPEG_TYPE);
    mappings.add("jpeg" + " : " + CustomMediaTypes.IMAGE_JPEG_TYPE);
    mappings.add("png" + " : " + CustomMediaTypes.IMAGE_PNG_TYPE);
    return Joiner.on(",").join(mappings);
  }

  void configureJersey(JerseyEnvironment environment, Injector i) throws IOException {
    addWriters(environment, i);
  }

  void initializeSwaggger(Bootstrap<ApplicationConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/swagger/", "/docs", "index.html"));
  }

  /***
   * The context path must be set before configuring swagger
   * @param environment
   */
  void configureSwagger(Environment environment) {
    environment.jersey().register(new ApiListingResourceJSON());
    environment.jersey().register(new ApiDeclarationProvider());
    environment.jersey().register(new ResourceListingProvider());
    ScannerFactory.setScanner(new DefaultJaxrsScanner());
    ClassReaders.setReader(new DefaultJaxrsApiReader());
    SwaggerConfig config = ConfigFactory.config();
    config.setApiVersion("1.0.1");
    config.setBasePath(".." + environment.getApplicationContext().getContextPath());
  }

  void configureAuthentication(ApiConfiguration configuration, Environment environment) throws ClassNotFoundException {
    FilterHolder holder = environment.getApplicationContext().addFilter(BasicAuthFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    holder.setInitParameter(BasicAuthFilter.KEY_REQUEST_PARAM, configuration.getApikeyParameter());
    holder.setInitParameter(BasicAuthFilter.DEFAULT_API_KEY, configuration.getDefaultApikey());

    JdbcRealm realm = new JdbcRealm();
    realm.setAuthenticationQuery(configuration.getAuthenticationQuery());
    realm.setPermissionsQuery(configuration.getPermissionQuery());
    realm.setUserRolesQuery(configuration.getRoleQuery());
    realm.setPermissionsLookupEnabled(true);
    realm.setDataSource(configuration.getAuthDataSourceFactory().build(environment.metrics(), "shiro db"));

    DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
    ((DefaultSessionStorageEvaluator)((DefaultSubjectDAO)securityManager.getSubjectDAO()).getSessionStorageEvaluator()).setSessionStorageEnabled(false);
    SecurityUtils.setSecurityManager(securityManager);

    /*environment.jersey().register(
        new CachingAuthenticator<BasicCredentials, Subject>(
            environment.metrics(), 
            new BasicAuthProvider<Subject>(new BasicAuthenticator(), "SciGraph"),
            configuration.getAuthenticationCachePolicy()));*/
    // TODO: fix this
    //environment.jersey().register(new BasicAuthProvider<Subject>(new BasicAuthenticator(), "SciGraph"));
  }

  @Override
  public void run(ApplicationConfiguration configuration, Environment environment) throws Exception {
    environment.getApplicationContext().setContextPath("/" + configuration.getApplicationContextPath());

    List<Module> modules = new ArrayList<>();
    modules.add(new Neo4jModule(configuration.getGraphConfiguration()));
    modules.add(new EntityModule());
    modules.add(new LexicalLibModule());
    modules.add(new OpenNlpModule());
    modules.add(new RefineModule(configuration.getServiceMetadata()));
    Injector i = Guice.createInjector(modules);

    environment.getApplicationContext().getInitParams().put(
        ServerProperties.MEDIA_TYPE_MAPPINGS, getTypeMappings());
    configureJersey(environment.jersey(), i);
    configureSwagger(environment);
    if (configuration.getApiConfiguration().isPresent()) {
      configureAuthentication(configuration.getApiConfiguration().get(), environment);
    }
    
    //Add managed objects
    environment.lifecycle().manage(i.getInstance(Neo4jManager.class));

    //Add health checks
    for (ClassInfo classInfo: ClassPath.from(getClass().getClassLoader()).getTopLevelClasses("edu.sdsc.scigraph.services.health")) {
      environment.healthChecks().register("Neo4j health check", (HealthCheck)i.getInstance(classInfo.load()));
    }

    //Add resources
    for (ClassInfo classInfo: ClassPath.from(getClass().getClassLoader()).getTopLevelClasses("edu.sdsc.scigraph.services.resources")) {
      environment.jersey().register(i.getInstance(classInfo.load()));
    }

  }

}
