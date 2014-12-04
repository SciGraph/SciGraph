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
package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;
import edu.sdsc.scigraph.owlapi.CurieUtil;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.VocabularyNeo4jImpl;

public class Neo4jModule extends AbstractModule {

  private Optional<String> graphLocation = Optional.absent();

  private Map<String, String> curieMap = new HashMap<>();

  public Neo4jModule(OntologyConfiguration configuration) {
    this.graphLocation = Optional.of(configuration.getGraphLocation());
    this.curieMap = configuration.getCuries();
  }

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("neo4j.location")).toInstance(graphLocation.get());
    bind(new TypeLiteral<Map<String, String>>(){}).annotatedWith(Names.named("neo4j.curieMap")).toInstance(curieMap);
    bind(CurieUtil.class);
    bind(Vocabulary.class).to(VocabularyNeo4jImpl.class).in(Singleton.class);
    bind(new TypeLiteral<ConcurrentMap<String, Long>>(){}).to(IdMap.class).in(Singleton.class);
  }

  private static final Set<String> NODE_PROPERTIES_TO_INDEX = newHashSet(CommonProperties.URI,
      NodeProperties.LABEL, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX,
      CommonProperties.ONTOLOGY, CommonProperties.FRAGMENT,
      Concept.CATEGORY, Concept.SYNONYM, Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX);

  private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(IndexManager.PROVIDER,
      "lucene", "analyzer", VocabularyIndexAnalyzer.class.getName());

  private static void setupIndex(AutoIndexer<?> index, Set<String> properties) {
    for (String property : properties) {
      index.startAutoIndexingProperty(property);
    }
    index.setEnabled(true);
  }

  public static void setupAutoIndexing(GraphDatabaseService graphDb) {
    try (Transaction tx = graphDb.beginTx()) {
      graphDb.index().forNodes("node_auto_index", INDEX_CONFIG);
      setupIndex(graphDb.index().getNodeAutoIndexer(), NODE_PROPERTIES_TO_INDEX);
      tx.success();
    }
  }

  @Provides
  @Singleton
  DB getMaker(@Named("neo4j.location") String neo4jLocation) {
    File dbLocation = new File(neo4jLocation, "SciGraphIdMap");
    return DBMaker.newFileDB(dbLocation).closeOnJvmShutdown().transactionDisable().mmapFileEnable().make();
  }

  @Provides
  @Singleton
  GraphDatabaseService getGraphDatabaseService(@Named("neo4j.location") String neo4jLocation) throws IOException {
    try {
      final GraphDatabaseService graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(neo4jLocation)
      .setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, "500M")
      .setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, "500M")
      .setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, "500M")
      .setConfig(GraphDatabaseSettings.strings_mapped_memory_size, "500M")
      .setConfig(GraphDatabaseSettings.arrays_mapped_memory_size, "500M")
      .newGraphDatabase();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() { graphDb.shutdown(); }
      });

      setupAutoIndexing(graphDb);
      return graphDb;
    } catch (Exception e) {
      if (Throwables.getRootCause(e).getMessage().contains("lock file")) {
        throw new IOException(format("The graph at \"%s\" is locked by another process", neo4jLocation));
      }
      throw e;
    }
  }

  @Provides
  @Singleton
  ExecutionEngine getExecutionEngine(GraphDatabaseService graphDb) {
    return new ExecutionEngine(graphDb);
  }


}
