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

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
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
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.neo4j.BatchGraph;

public class BatchOwlLoader {

  private static final Logger logger = Logger.getLogger(BatchOwlLoader.class.getName());

  @Inject
  OWLOntologyWalker walker;

  @Inject
  BatchOwlVisitor visitor;
  
  @Inject
  PostpostprocessorProvider postprocessorProvider;

  BatchOwlLoader() {
    System.setProperty("entityExpansionLimit", Integer.toString(1_000_000));
  }

  void loadOntology() {
    Stopwatch timer = Stopwatch.createStarted();
    logger.info("Walking ontology structure...");
    walker.walkStructure(visitor);
    visitor.shutdown();
    logger.info(format("Walking ontology structure took %d seconds",
        timer.elapsed(TimeUnit.SECONDS)));
    timer.reset();
    timer.start();
    logger.info("Postprocessing...");
    postprocessorProvider.get().postprocess();
    logger.info(format("Postprocessing took %d seconds", timer.elapsed(TimeUnit.SECONDS)));
    postprocessorProvider.shutdown();
  }

  protected static Options getOptions() {
    Option configPath = new Option("c", "configpath", true,
        "The location of the configuration file");
    configPath.setRequired(true);
    Options options = new Options();
    options.addOption(configPath);
    return options;
  }

  static class PostpostprocessorProvider implements Provider<OwlPostprocessor> {

    @Inject
    OwlLoadConfiguration config;
    
    GraphDatabaseService graphDb;
    
    @Override
    public OwlPostprocessor get() {
      graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(config.getOntologyConfiguration().getGraphLocation());
      return new OwlPostprocessor(graphDb, config.getCategories());
    }
    
    public void shutdown() {
      graphDb.shutdown();
    }
    
  }
  
  static class OwlLoaderModule extends AbstractModule {

    OwlLoadConfiguration config;

    public OwlLoaderModule(OwlLoadConfiguration config) {
      this.config = config;
    }

    private static final Set<String> NODE_PROPERTIES_TO_INDEX = newHashSet(CommonProperties.URI,
        NodeProperties.LABEL, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX,
        CommonProperties.CURIE, CommonProperties.ONTOLOGY, CommonProperties.FRAGMENT,
        Concept.CATEGORY, Concept.SYNONYM, Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX);
    private static final Set<String> EXACT_PROPERTIES = newHashSet(NodeProperties.LABEL,
        Concept.SYNONYM);

    @Override
    protected void configure() {
      bind(OwlLoadConfiguration.class).toInstance(config);
      bindConstant().annotatedWith(Names.named("uniqueProperty")).to(CommonProperties.URI);
      bind(new TypeLiteral<Set<String>>() {
      }).annotatedWith(Names.named("indexedProperties")).toInstance(config.getIndexedNodeProperties());
      bind(new TypeLiteral<Set<String>>() {
      }).annotatedWith(Names.named("exactProperties")).toInstance(config.getExactNodeProperties());
      bind(new TypeLiteral<Map<String, String>>() {
      }).annotatedWith(Names.named("owl.categories")).toInstance(config.getCategories());
    }

    @Provides
    @Singleton
    BatchInserter getInserter() {
      logger.info("Getting BatchInserter");
      return BatchInserters.inserter(config.getOntologyConfiguration().getGraphLocation());
    }

    @Provides
    @Singleton
    BatchOwlVisitor getBatchVisitor(OWLOntologyWalker walker, BatchGraph graph) {
      return new BatchOwlVisitor(walker, graph, config.getCuries(), config.getMappedProperties());
    }

    @Provides
    @Singleton
    OWLOntologyWalker getOntologyWalker()
        throws OWLOntologyCreationException {
      logger.info("Loading ontologies with owlapi...");
      Stopwatch timer = Stopwatch.createStarted();
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
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
    Injector i = Guice.createInjector(new OwlLoaderModule(config));
    BatchOwlLoader loader = i.getInstance(BatchOwlLoader.class);
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
