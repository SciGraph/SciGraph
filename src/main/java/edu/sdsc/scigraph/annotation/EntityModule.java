package edu.sdsc.scigraph.annotation;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

public class EntityModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EntityProcessor.class).to(EntityProcessorImpl.class).in(Singleton.class);
  }

}
