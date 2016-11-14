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
package io.scigraph.owlapi;

import io.scigraph.frames.CommonProperties;
import io.scigraph.neo4j.GraphBatchImpl;
import io.scigraph.neo4j.IdMap;
import io.scigraph.neo4j.RelationshipMap;

import java.io.File;
import java.util.Collections;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class GraphOwlVisitorBatchGraphTest extends GraphOwlVisitorTestBase<GraphBatchImpl> {

  @Override
  protected GraphBatchImpl createInstance() throws Exception {
    BatchInserter inserter = BatchInserters.inserter(new File(path));
    return new GraphBatchImpl(inserter, CommonProperties.IRI, Collections.<String>emptySet(),
        Collections.<String>emptySet(), new IdMap(), new RelationshipMap());
  }

}
