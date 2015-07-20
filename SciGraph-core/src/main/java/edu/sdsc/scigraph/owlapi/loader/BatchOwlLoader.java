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
package edu.sdsc.scigraph.owlapi.loader;

import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mapdb.DB;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.owlapi.OwlApiUtils;
import edu.sdsc.scigraph.owlapi.OwlPostprocessor;
import edu.sdsc.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import edu.sdsc.scigraph.owlapi.loader.bindings.IndicatesMappedProperties;
import edu.sdsc.scigraph.owlapi.loader.bindings.IndicatesNumberOfConsumerThreads;
import edu.sdsc.scigraph.owlapi.loader.bindings.IndicatesNumberOfProducerThreads;

public class BatchOwlLoader {

  private static final Logger logger = Logger.getLogger(BatchOwlLoader.class.getName());

  static final OntologySetup POISON_STR = new OntologySetup();

  @Inject
  @IndicatesNumberOfConsumerThreads
  int numConsumers;

  @Inject
  @IndicatesNumberOfProducerThreads
  int numProducers;

  @Inject
  PostpostprocessorProvider postprocessorProvider;

  @Inject
  Graph graph;

  @Inject
  @IndicatesMappedProperties 
  List<MappedProperty> mappedProperties;

  @Inject
  Provider<OwlOntologyConsumer> consumerProvider;

  @Inject
  Provider<OwlOntologyProducer> producerProvider;

  Collection<OntologySetup> ontologies;

  @Inject
  BlockingQueue<OWLCompositeObject> queue;

  @Inject
  BlockingQueue<OntologySetup> urlQueue;
  
  @Inject
  ExecutorService exec; 
  
  static {
    System.setProperty("entityExpansionLimit", Integer.toString(1_000_000));
    OwlApiUtils.silenceOboParser();
  }

  void loadOntology() throws InterruptedException {
    Set<Future<?>> futures = new HashSet<>();
    if (!ontologies.isEmpty()) {
      for (int i = 0; i < numConsumers; i++) {
        futures.add(exec.submit(consumerProvider.get()));
      }
      for (int i = 0; i < numProducers; i++) {
        futures.add(exec.submit(producerProvider.get()));
      }
      for (OntologySetup ontology: ontologies) {
        urlQueue.offer(ontology);
      }
      for (int i = 0; i < numProducers; i++) {
        urlQueue.offer(POISON_STR);
      }
    }
    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.DAYS);
    try {
      for (Future<?> future: futures) {
        future.get();
      }
    } catch (ExecutionException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    graph.shutdown();
    logger.info("Postprocessing...");
    postprocessorProvider.get().postprocess();
    postprocessorProvider.shutdown();
  }

  static class PostpostprocessorProvider implements Provider<OwlPostprocessor> {

    @Inject
    OwlLoadConfiguration config;

    @Inject
    Provider<GraphDatabaseService> graphDbProvider;

    GraphDatabaseService graphDb;

    @Override
    public OwlPostprocessor get() {
      graphDb = graphDbProvider.get();
      return new OwlPostprocessor(graphDb, config.getCategories());
    }

    public void shutdown() {
      try (Transaction tx = graphDb.beginTx()) {
        logger.info(size(GlobalGraphOperations.at(graphDb).getAllNodes()) + " nodes");
        logger.info(size(GlobalGraphOperations.at(graphDb).getAllRelationships()) + " relationships");
        tx.success();
      }
      graphDb.shutdown();
    }

  }

  public static void load(OwlLoadConfiguration config) throws InterruptedException {
    Injector i = Guice.createInjector(new OwlLoaderModule(config), new Neo4jModule(config.getGraphConfiguration()));
    BatchOwlLoader loader = i.getInstance(BatchOwlLoader.class);
    loader.ontologies = config.getOntologies();
    logger.info("Loading ontologies...");
    Stopwatch timer = Stopwatch.createStarted();
    loader.loadOntology();
    DB mapDb = i.getInstance(DB.class);
    mapDb.close();
    logger.info(format("Loading took %d minutes", timer.elapsed(TimeUnit.MINUTES)));
  }

  protected static Options getOptions() {
    Option configPath = new Option("c", "configpath", true,
        "The location of the configuration file");
    configPath.setRequired(true);
    Options options = new Options();
    options.addOption(configPath);
    return options;
  }

  public static void main(String[] args) throws Exception {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(getOptions(), args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(BatchOwlLoader.class.getSimpleName(), getOptions());
      System.exit(-1);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    OwlLoadConfiguration config = mapper.readValue(new File(cmd.getOptionValue('c').trim()),
        OwlLoadConfiguration.class);
    load(config);
    // TODO: Is Guice causing this to hang? #44
    System.exit(0);
  }

}
