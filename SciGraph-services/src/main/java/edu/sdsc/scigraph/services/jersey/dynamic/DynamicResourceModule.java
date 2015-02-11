package edu.sdsc.scigraph.services.jersey.dynamic;

import com.google.inject.AbstractModule;
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

}
