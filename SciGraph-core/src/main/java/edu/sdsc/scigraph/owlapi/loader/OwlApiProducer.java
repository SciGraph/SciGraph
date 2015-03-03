package edu.sdsc.scigraph.owlapi.loader;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.sdsc.scigraph.owlapi.OwlApiUtils;
import edu.sdsc.scigraph.owlapi.ReasonerUtil;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.OntologySetup;

public class OwlApiProducer implements Producer {

  private final static Logger logger = Logger.getLogger(OwlApiProducer.class.getName());

  private final OntologySetup ontologyConfig;
  private final BlockingQueue<OWLObject> queue;

  public OwlApiProducer(OntologySetup setup, BlockingQueue<OWLObject> queue) {
    this.ontologyConfig = setup;
    this.queue = queue;
  }

  @Override
  public void produce() {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    try {
      OWLOntology ont = OwlApiUtils.loadOntology(manager, ontologyConfig.url());
      if (ontologyConfig.getReasonerConfiguration().isPresent()) {
        ReasonerUtil util = new ReasonerUtil(ontologyConfig.getReasonerConfiguration().get(), manager, ont);
        util.reason();
      }
      logger.info("Adding axioms for: " + ontologyConfig);
      for (OWLOntology ontology: manager.getOntologies()) {
        for (OWLObject object: ontology.getNestedClassExpressions()) {
          queue.put(object);
        }
        for (OWLObject object: ontology.getSignature(true)) {
          queue.put(object);
        }
        for (OWLObject object: ontology.getAxioms()) {
          queue.put(object);
        }
      }
      logger.info("Finished processing ontology: " + ontologyConfig);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to load ontology: " + ontologyConfig, e);
    }
  }

}
