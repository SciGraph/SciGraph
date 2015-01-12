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
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.owlapi.OwlApiUtils;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.owlapi.OwlPostprocessor;

public class BatchOwlLoader {

  static final Logger logger = Logger.getLogger(BatchOwlLoader.class.getName());

  static final OWLObject POISON = IRI.create("http://poison.org");
  static final String POISON_STR = "Poison String";

  private static final int numCores = Runtime.getRuntime().availableProcessors();
  
  static final int CONSUMER_COUNT = numCores;
  static final int PRODUCER_COUNT = numCores;

  @Inject
  PostpostprocessorProvider postprocessorProvider;

  @Inject
  BatchGraph graph;
  
  @Inject
  @Named("owl.mappedProperties") List<MappedProperty> mappedProperties;

  Collection<String> urls;

  BlockingQueue<OWLObject> queue = new LinkedBlockingQueue<>();
  BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

  ExecutorService exec = Executors.newFixedThreadPool(CONSUMER_COUNT + PRODUCER_COUNT);

  static {
    System.setProperty("entityExpansionLimit", Integer.toString(1_000_000));
    OwlApiUtils.silenceOboParser();
  }

  void loadOntology() throws InterruptedException {
    for (int i = 0; i < CONSUMER_COUNT; i++) {
      exec.submit(new OwlOntologyWalkerConsumer(queue, graph, PRODUCER_COUNT, mappedProperties));
    }
    for (int i = 0; i < PRODUCER_COUNT; i++) {
      exec.submit(new OwlOntologyWalkerProducer(queue, urlQueue, CONSUMER_COUNT));
    }
    for (String url: urls) {
      urlQueue.offer(url);
    }
    for (int i = 0; i < PRODUCER_COUNT; i++) {
      urlQueue.offer(POISON_STR);
    }
    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.DAYS);
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
    Injector i = Guice.createInjector(new OwlLoaderModule(config), new Neo4jModule(config.getOntologyConfiguration()));
    BatchOwlLoader loader = i.getInstance(BatchOwlLoader.class);
    loader.urls = config.getOntologyUrls();
    logger.info("Loading ontologies...");
    Stopwatch timer = Stopwatch.createStarted();
    loader.loadOntology();
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

  public static void main(String[] args) throws OWLOntologyCreationException, JsonParseException,
  JsonMappingException, IOException, InterruptedException {
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(getOptions(), args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("OwlLoader", getOptions());
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
