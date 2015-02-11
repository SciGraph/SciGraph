package edu.sdsc.scigraph.services.jersey.dynamic;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.google.inject.assistedinject.Assisted;

public class DynamicCypherResource extends ResourceConfig {

  final Resource.Builder resourceBuilder = Resource.builder();

  @Inject
  DynamicCypherResource(CypherInflectorFactory factory, @Assisted CypherResourceConfig config) {
    resourceBuilder.path(config.getPath());
    ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
    methodBuilder.produces(MediaType.APPLICATION_JSON_TYPE)
    .handledBy(factory.create(config));
  }

  public Resource.Builder getBuilder() {
    return resourceBuilder;
  }

}
