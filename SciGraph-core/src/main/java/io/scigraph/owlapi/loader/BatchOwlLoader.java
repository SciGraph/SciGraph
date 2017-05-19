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
package io.scigraph.owlapi.loader;

import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
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

import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.Neo4jModule;
import io.scigraph.owlapi.OwlApiUtils;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import io.scigraph.owlapi.loader.bindings.IndicatesAddEdgeLabel;
import io.scigraph.owlapi.loader.bindings.IndicatesAllNodesLabel;
import io.scigraph.owlapi.loader.bindings.IndicatesAnonymousNodeProperty;
import io.scigraph.owlapi.loader.bindings.IndicatesCliqueConfiguration;
import io.scigraph.owlapi.loader.bindings.IndicatesMappedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfConsumerThreads;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfProducerThreads;
import io.scigraph.owlapi.postprocessors.AllNodesLabeler;
import io.scigraph.owlapi.postprocessors.AnonymousNodeTagger;
import io.scigraph.owlapi.postprocessors.Clique;
import io.scigraph.owlapi.postprocessors.CliqueConfiguration;
import io.scigraph.owlapi.postprocessors.EdgeLabeler;

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
  List<OntologySetup> ontologies;

  @Inject
  @IndicatesMappedProperties
  List<MappedProperty> mappedProperties;

  @Inject
  Provider<OwlOntologyConsumer> consumerProvider;

  @Inject
  Provider<OwlOntologyProducer> producerProvider;

  @Inject
  BlockingQueue<OWLCompositeObject> queue;

  @Inject
  BlockingQueue<OntologySetup> urlQueue;

  @Inject
  ExecutorService exec;

  @Inject
  @IndicatesCliqueConfiguration
  Optional<CliqueConfiguration> cliqueConfiguration;

  @Inject
  @IndicatesAddEdgeLabel
  Optional<Boolean> addEdgeLabel;

  @Inject
  @IndicatesAllNodesLabel
  Optional<String> allNodesLabel;

  @Inject
  @IndicatesAnonymousNodeProperty
  Optional<String> anonymousNodeProperty;

  static {
    System.setProperty("entityExpansionLimit", Integer.toString(1_000_000));
    OwlApiUtils.silenceOboParser();
  }

  public void loadOntology() throws InterruptedException, ExecutionException {
    CompletionService<Long> completionService = new ExecutorCompletionService<Long>(exec);
    Set<Future<?>> futures = new HashSet<>();
    if (!ontologies.isEmpty()) {
      for (int i = 0; i < numConsumers; i++) {
        futures.add(completionService.submit(consumerProvider.get()));
      }
      for (int i = 0; i < numProducers; i++) {
        futures.add(completionService.submit(producerProvider.get()));
      }
      for (OntologySetup ontology : ontologies) {
        urlQueue.offer(ontology);
      }
      for (int i = 0; i < numProducers; i++) {
        urlQueue.offer(POISON_STR);
      }
    }

    while (futures.size() > 0) {
      Future<?> completedFuture = completionService.take();
      futures.remove(completedFuture);
      try {
        completedFuture.get();
      } catch (ExecutionException e) {
        logger.log(Level.SEVERE, "Stopping batchLoading due to: " + e.getMessage(), e);
        e.printStackTrace();
        exec.shutdownNow();
        throw new InterruptedException(e.getCause().getMessage());
      }
    }

    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.DAYS);
    graph.shutdown();
    logger.info("Postprocessing...");
    postprocessorProvider.get().postprocess();

    if (anonymousNodeProperty.isPresent()) {
      postprocessorProvider.runAnonymousNodeTagger(anonymousNodeProperty.get());
    }
    
    if (cliqueConfiguration.isPresent()) {
      postprocessorProvider.runCliquePostprocessor(cliqueConfiguration.get());
    }

    if (addEdgeLabel.orElse(false)) {
      postprocessorProvider.runEdgeLabelerPostprocessor();
    }

    if (allNodesLabel.isPresent()) {
      postprocessorProvider.runAllNodesLabeler(allNodesLabel.get());
    }

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

    public void runCliquePostprocessor(CliqueConfiguration cliqueConfiguration) {
      Clique clique = new Clique(graphDb, cliqueConfiguration);
      clique.run();
    }

    public void runEdgeLabelerPostprocessor() {
      EdgeLabeler edgeLabeler = new EdgeLabeler(graphDb);
      edgeLabeler.run();
    }

    public void runAllNodesLabeler(String label) {
      AllNodesLabeler allNodesLabeler = new AllNodesLabeler(label, graphDb);
      allNodesLabeler.run();
    }

    public void runAnonymousNodeTagger(String anonymousProperty) {
      AnonymousNodeTagger anonymousNodeTagger = new AnonymousNodeTagger(anonymousProperty, graphDb);
      anonymousNodeTagger.run();
    }

    public void shutdown() {
      try (Transaction tx = graphDb.beginTx()) {
        logger.info(size(graphDb.getAllNodes()) + " nodes");
        logger.info(size(graphDb.getAllRelationships()) + " relationships");
        tx.success();
      }
      graphDb.shutdown();
    }

  }

  public static void load(OwlLoadConfiguration config)
      throws InterruptedException, ExecutionException {
    Injector i = Guice.createInjector(new OwlLoaderModule(config),
        new Neo4jModule(config.getGraphConfiguration()));
    BatchOwlLoader loader = i.getInstance(BatchOwlLoader.class);
    logger.info("Loading ontologies...");
    Stopwatch timer = Stopwatch.createStarted();
    // TODO catch exception and delete the incomplete graph through the graph location
    loader.loadOntology();
    DB mapDb = i.getInstance(DB.class);
    mapDb.close();
    logger.info(format("Loading took %d minutes", timer.elapsed(TimeUnit.MINUTES)));
  }

  protected static Options getOptions() {
    Option configPath =
        new Option("c", "configpath", true, "The location of the configuration file");
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

    OwlLoadConfigurationLoader owlLoadConfigurationLoader =
        new OwlLoadConfigurationLoader(new File(cmd.getOptionValue('c').trim()));
    OwlLoadConfiguration config = owlLoadConfigurationLoader.loadConfig();
    load(config);
    // TODO: Is Guice causing this to hang? #44
    System.exit(0);
  }

}
