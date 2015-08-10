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

import io.dropwizard.lifecycle.Managed;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;

public class Neo4jManager implements Managed {

  private final Logger logger = Logger.getLogger(Neo4jManager.class.getName());
  
  private final GraphDatabaseService graphDb;

  @Inject
  public Neo4jManager(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  @Override
  public void start() throws Exception {
    logger.info("Starting Neo4j graph manager");
  }

  @Override
  public void stop() throws Exception {
    logger.info("Shutting down Neo4j graph");
    graphDb.shutdown();
  }

}
