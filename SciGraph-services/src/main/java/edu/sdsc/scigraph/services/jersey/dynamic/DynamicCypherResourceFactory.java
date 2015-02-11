package edu.sdsc.scigraph.services.jersey.dynamic;

interface DynamicCypherResourceFactory {

  public DynamicCypherResource create(CypherResourceConfig config);

}
