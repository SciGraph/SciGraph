package edu.sdsc.scigraph.owlapi.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import edu.sdsc.scigraph.owlapi.OwlRelationships;
import uk.ac.manchester.cs.owl.owlapi.turtle.parser.ParseException;
import uk.ac.manchester.cs.owl.owlapi.turtle.parser.TripleHandler;
import uk.ac.manchester.cs.owl.owlapi.turtle.parser.TurtleParser;

public class TtlProducer implements Producer, TripleHandler {

  private static final Logger logger = Logger.getLogger(TtlProducer.class.getName());
  
  private final BlockingQueue<OWLObject> queue;
  private final TurtleParser parser;
  private static OWLDataFactory factory = OWLManager.getOWLDataFactory();

  public TtlProducer(BlockingQueue<OWLObject> queue) throws FileNotFoundException {
    this.queue = queue;
    parser = new TurtleParser(new FileInputStream(new File("/tmp/hpoa.ttl")), this, IRI.create("http://x.org"));
  }

  @Override
  public void produce() {
    try {
      parser.parseDocument();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void handleTriple(IRI subject, IRI predicate, IRI object) {
    logger.info(String.format("i i i, %s %s %s", subject, predicate, object));
  }

  @Override
  public void handleTriple(IRI subject, IRI predicate, String object) {
    logger.info(String.format("i i s %s %s %s", subject, predicate, object));
  }

  @Override
  public void handleTriple(IRI subject, IRI predicate, String object, String lang) {
    logger.info(String.format("i i s l %s %s %s %s", subject, predicate, object, lang));
  }

  @Override
  public void handleTriple(IRI subject, IRI predicate, String object, IRI datatype) {
    logger.info(String.format("i i s d %s %s %s %s", subject, predicate, object, datatype));
  }

  // Ignore unnecessary directives

  @Override
  public void handlePrefixDirective(String prefixName, String prefix) { /* IGNORE */ } 

  @Override
  public void handleBaseDirective(String base) { /* IGNORE */ }

  @Override
  public void handleComment(String comment) { /* IGNORE */ }

  @Override
  public void handleEnd() { /* IGNORE */ }

}
