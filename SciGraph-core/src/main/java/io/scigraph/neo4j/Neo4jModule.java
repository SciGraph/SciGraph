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
package io.scigraph.neo4j;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import io.scigraph.frames.CommonProperties;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.lucene.VocabularyIndexAnalyzer;
import io.scigraph.neo4j.bindings.IndicatesCurieMapping;
import io.scigraph.neo4j.bindings.IndicatesNeo4jGraphLocation;
import io.scigraph.vocabulary.Vocabulary;
import io.scigraph.vocabulary.VocabularyNeo4jImpl;

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
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.configuration.Settings;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

public class Neo4jModule extends AbstractModule {

  private final Neo4jConfiguration configuration;
  private boolean readOnly = false;
  private boolean enableGuard = false;

  public Neo4jModule(Neo4jConfiguration configuration) {
    this.configuration = configuration;
  }

  public Neo4jModule(Neo4jConfiguration configuration, boolean readOnly, boolean enableGuard) {
    this.configuration = configuration;
    this.readOnly = readOnly;
    this.enableGuard = enableGuard;

  }

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(IndicatesNeo4jGraphLocation.class).toInstance(
        configuration.getLocation());
    bind(new TypeLiteral<Map<String, String>>() {}).annotatedWith(IndicatesCurieMapping.class)
        .toInstance(configuration.getCuries());
    bind(Vocabulary.class).to(VocabularyNeo4jImpl.class).in(Singleton.class);
    bind(new TypeLiteral<ConcurrentMap<String, Long>>() {}).to(IdMap.class).in(Singleton.class);
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
      indexProperties.addAll(transform(config.getExactNodeProperties(),
          new Function<String, String>() {
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
    return DBMaker.newFileDB(dbLocation).closeOnJvmShutdown().transactionDisable().mmapFileEnable()
        .make();
  }

  @Provides
  @Singleton
  GraphDatabaseService getGraphDatabaseService() throws IOException {
    try {
      GraphDatabaseBuilder graphDatabaseBuilder =
          new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
              new File(configuration.getLocation())).setConfig(configuration.getNeo4jConfig());
      if (readOnly) {
        graphDatabaseBuilder.setConfig(GraphDatabaseSettings.read_only, Settings.TRUE);
      }
      if (enableGuard) {
        graphDatabaseBuilder
            .setConfig(GraphDatabaseSettings.execution_guard_enabled, Settings.TRUE);
      }
      final GraphDatabaseService graphDb = graphDatabaseBuilder.newGraphDatabase();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          graphDb.shutdown();
        }
      });

      if (!readOnly) { // No need of auto-indexing in read-only mode
        setupAutoIndexing(graphDb, configuration);
      }

      return graphDb;
    } catch (Exception e) {
      if (Throwables.getRootCause(e).getMessage().contains("lock file")) {
        throw new IOException(format("The graph at \"%s\" is locked by another process",
            configuration.getLocation()));
      }
      throw e;
    }
  }

}
