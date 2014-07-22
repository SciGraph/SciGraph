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
package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.bindings.IndicatesNeo4j;

public class OwlLoader {

  private static final Logger logger = Logger.getLogger(OwlLoader.class.getName());

  @Inject
  @IndicatesNeo4j
  AtomicBoolean inTransaction;

  @Inject
  Graph<Concept> graph;

  @Inject
  OWLOntologyWalker walker;

  @Inject
  OwlVisitor visitor;

  OwlLoader() {
    System.setProperty("entityExpansionLimit", Integer.toString(1_000_000));
  }

  void loadOntology() {
    Stopwatch timer = Stopwatch.createStarted();
    inTransaction.set(true);
    Transaction tx = graph.getGraphDb().beginTx();
    try {
      logger.info("Walking ontology structure...");
      walker.walkStructure(visitor);
      logger.info(format("Walking ontology structure took %d seconds",
          timer.elapsed(TimeUnit.SECONDS)));
      timer.reset();
      timer.start();
      logger.info("Postprocessing...");
      // visitor.postProcess();
      tx.success();
      logger.info(format("Postprocessing took %d seconds", timer.elapsed(TimeUnit.SECONDS)));
    } finally {
      timer.reset();
      timer.start();
      logger.info("Committing changes...");
      tx.finish();
      logger.info(format("Committing took %d seconds", timer.elapsed(TimeUnit.SECONDS)));
    }
    try (Transaction tx2 = graph.getGraphDb().beginTx()) {
      int nodes = size(GlobalGraphOperations.at(graph.getGraphDb()).getAllNodes());
      int edges = size(GlobalGraphOperations.at(graph.getGraphDb()).getAllRelationships());
      logger.info(format("...done. Graph contains %s nodes and %s edges.", nodes, edges));
      tx2.success();
    }
    graph.shutdown();
  }

  protected static Options getOptions() {
    Option configPath = new Option("c", "configpath", true,
        "The location of the configuration file");
    configPath.setRequired(true);
    Options options = new Options();
    options.addOption(configPath);
    return options;
  }

  static class OwlLoaderModule extends AbstractModule {

    OwlLoadConfiguration config;

    public OwlLoaderModule(OwlLoadConfiguration config) {
      this.config = config;
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    OwlVisitor getVisitor(OWLOntologyWalker walker, Graph<Concept> graph) {
      return new OwlVisitor(walker, graph, config.getCuries(), config.getCategories(),
          config.getMappedProperties());
    }

    @Provides
    @Singleton
    OWLOntologyWalker getOntologyWalker(FileCachingIRIMapper mapper)
        throws OWLOntologyCreationException {
      logger.info("Loading ontologies with owlapi...");
      Stopwatch timer = Stopwatch.createStarted();
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      manager.addIRIMapper(mapper);
      for (String url : config.getOntologyUrls()) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
          manager.loadOntology(IRI.create(url));
        } else {
          manager.loadOntologyFromOntologyDocument(new File(url));
        }
      }
      logger.info(format("loaded ontologies with owlapi in %d seconds",
          timer.elapsed(TimeUnit.SECONDS)));
      return new OWLOntologyWalker(manager.getOntologies());
    }

  }

  public static void load(OwlLoadConfiguration config) {
    Injector i = Guice.createInjector(new Neo4jModule(config.getOntologyConfiguration()),
        new OwlLoaderModule(config));
    OwlLoader loader = i.getInstance(OwlLoader.class);
    logger.info("Starting to load ontologies...");
    Stopwatch timer = Stopwatch.createStarted();
    loader.loadOntology();
    logger.info(format("Processing took %d minutes", timer.elapsed(TimeUnit.MINUTES)));
  }

  public static void main(String[] args) throws OWLOntologyCreationException, JsonParseException,
      JsonMappingException, IOException {
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
  }

}
