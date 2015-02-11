package edu.sdsc.scigraph.services.jersey.dynamic;

interface CypherInflectorFactory {

  public CypherInflector create(CypherResourceConfig config);

}
