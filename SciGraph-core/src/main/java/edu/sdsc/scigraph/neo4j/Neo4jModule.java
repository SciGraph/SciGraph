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

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Singleton;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;
import edu.sdsc.scigraph.neo4j.bindings.IndicatesCurieMapping;
import edu.sdsc.scigraph.neo4j.bindings.IndicatesNeo4jGraphLocation;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.VocabularyNeo4jImpl;

public class Neo4jModule extends AbstractModule {

  private final Neo4jConfiguration configuration;

  public Neo4jModule(Neo4jConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(IndicatesNeo4jGraphLocation.class).toInstance(configuration.getLocation());
    bind(new TypeLiteral<Map<String, String>>(){}).annotatedWith(IndicatesCurieMapping.class).toInstance(configuration.getCuries());
    bind(Vocabulary.class).to(VocabularyNeo4jImpl.class).in(Singleton.class);
    bind(new TypeLiteral<ConcurrentMap<String, Long>>(){}).to(IdMap.class).in(Singleton.class);
  }

  private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(IndexManager.PROVIDER,
      "lucene", "analyzer", VocabularyIndexAnalyzer.class.getName());

  private static void setupIndex(AutoIndexer<?> index, Collection<String> properties) {
    for (String property : properties) {
      index.startAutoIndexingProperty(property);
    }
    index.setEnabled(true);
  }

  public static void setupAutoIndexing(GraphDatabaseService graphDb, Neo4jConfiguration config) {
    try (Transaction tx = graphDb.beginTx()) {
      graphDb.index().forNodes("node_auto_index", INDEX_CONFIG);
      Set<String> indexProperties = newHashSet(CommonProperties.IRI);
      indexProperties.addAll(config.getIndexedNodeProperties());
      indexProperties.addAll(transform(config.getExactNodeProperties(), new Function<String, String>() {
        @Override
        public String apply(String index) {
          return index + LuceneUtils.EXACT_SUFFIX;
        }
      }));
      setupIndex(graphDb.index().getNodeAutoIndexer(), indexProperties);
      tx.success();
    }
  }

  @Provides
  @Singleton
  DB getMaker() {
    File dbLocation = new File(configuration.getLocation(), "SciGraphIdMap");
    return DBMaker.newFileDB(dbLocation).closeOnJvmShutdown().transactionDisable().mmapFileEnable().make();
  }

  @SuppressWarnings("deprecation")
  @Provides
  @Singleton
  GraphDatabaseService getGraphDatabaseService() throws IOException {
    try {
      final GraphDatabaseService graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(configuration.getLocation())
      .setConfig(configuration.getNeo4jConfig())
      .newGraphDatabase();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() { graphDb.shutdown(); }
      });

      setupAutoIndexing(graphDb, configuration);
      return graphDb;
    } catch (Exception e) {
      if (Throwables.getRootCause(e).getMessage().contains("lock file")) {
        throw new IOException(format("The graph at \"%s\" is locked by another process", configuration.getLocation()));
      }
      throw e;
    }
  }

}
