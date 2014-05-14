/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class BatchGraphSpike {

  BatchInserter inserter;
  
  @Before
  public void setUp() throws Exception {
     inserter = BatchInserters.inserter("target/batch");
  }
  
  @After
  public void tearDown() throws IOException {
    FileUtils.deleteDirectory(new File("target/batch"));
  }

  @Test
  public void test() {
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put("uri", "http://example.org/foo");
    long nodeId = inserter.createNode(propertyMap);
    propertyMap = inserter.getNodeProperties(nodeId);
    propertyMap.put("label", "Foo");
    inserter.shutdown();
    
    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase("target/batch");
    System.out.println(newArrayList(GlobalGraphOperations.at(db).getAllNodes()));
    db.shutdown();
  }

}
