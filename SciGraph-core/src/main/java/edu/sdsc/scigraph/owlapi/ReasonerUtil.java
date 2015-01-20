package edu.sdsc.scigraph.owlapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

class ReasonerUtil {

  private static final Logger logger = Logger.getLogger(ReasonerUtil.class.getName());
  private final static OWLDataFactory factory = OWLManager.getOWLDataFactory();

  private final OWLReasonerFactory reasonerFactory;
  private final OWLOntologyManager manager;
  private final OWLOntology ont;

  @Inject
  ReasonerUtil(OWLReasonerFactory reasonerFactory, OWLOntologyManager manager, OWLOntology ont) {
    this.reasonerFactory = reasonerFactory;
    this.manager = manager;
    this.ont = ont;
  }

  int removeAxioms(AxiomType<?> type) {
    int count = 0;
    for (OWLOntology importedOnt: ont.getImportsClosure()) {
      Set<? extends OWLAxiom> axioms = importedOnt.getAxioms(type);
      List<OWLOntologyChange> removals= manager.removeAxioms(importedOnt, axioms);
      count += removals.size();
    }
    return count;
  }

  /***
   * Remove all axioms that would generate extra unsatisfiable classes for the reasoner
   */
  void removeUnsatisfiableClasses() {
    int removalCount = 0;
    removalCount += removeAxioms(AxiomType.DISJOINT_CLASSES);
    removalCount += removeAxioms(AxiomType.DATA_PROPERTY_DOMAIN);
    removalCount += removeAxioms(AxiomType.DATA_PROPERTY_RANGE);
    logger.info("Removed " + removalCount + " axioms to prevent unsatisfiable classes");
  }

  boolean shouldReason(OWLReasoner reasoner, OWLOntology ont) {
    if (!reasoner.isConsistent()) {
      logger.warning("Not reasoning on " + ont + " because it is inconsistent.");
      return false;
    }
    Set<OWLClass> nothingSubclasses = reasoner.getSubClasses(factory.getOWLNothing(), true).getFlattened();
    if (!nothingSubclasses.isEmpty()) {
      logger.warning("Not reasoning on " + ont + " because " + nothingSubclasses.size() + " classes are sublasses of nothing");
      return false;
    }
    return true;
  }

  AddAxiom getCompleteEquivalence(OWLReasoner reasoner, OWLClass ce) {
    Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(ce).getEntities();
    OWLEquivalentClassesAxiom equivalentAxiom = factory.getOWLEquivalentClassesAxiom(equivalentClasses);
    return new AddAxiom(ont, equivalentAxiom);
  }

  //Ticket #42
  List<OWLOntologyChange> getDirectInferredEdges(OWLReasoner reasoner, OWLClass ce) {
    List<OWLOntologyChange> changes = new ArrayList<>();
    //find direct inferred superclasses, D
    Set<OWLClass> directSuperclasses = reasoner.getSuperClasses(ce, true).getFlattened();
    //find indirect superclasses, I
    Set<OWLClass> indirectSuperclasses = reasoner.getSuperClasses(ce, false).getFlattened();
    //find asserted superclasses, A
    Set<OWLClassExpression> assertedSuperclasses = ce.asOWLClass().getSuperClasses(ont);
    //for each d in D, add an edge subClassOf(x d)
    for (OWLClass directSuperclass: directSuperclasses) {
      OWLAxiom axiom = factory.getOWLSubClassOfAxiom(ce, directSuperclass);
      AddAxiom addAxiom = new AddAxiom(ont, axiom);
      changes.add(addAxiom);
    }
    //for each a in A
    for (OWLClassExpression assertedSuperclass: assertedSuperclasses) {
      if (assertedSuperclass.isAnonymous()) {
        continue;
      }
      //if a is in I and not in D, then remove edge subClassOf(x a)
      if (indirectSuperclasses.contains(assertedSuperclass.asOWLClass()) &&
          !directSuperclasses.contains(assertedSuperclass.asOWLClass())) {
        OWLAxiom axiom = factory.getOWLSubClassOfAxiom(ce, assertedSuperclass);
        RemoveAxiom removeAxiom = new RemoveAxiom(ont, axiom);
        changes.remove(removeAxiom);
      }
    }
    return changes;
  }

  void reason(OWLReasoner reasoner, boolean completeEquivalenceClasses, boolean addDirectInferredSubclasses) {
    List<OWLOntologyChange> changes = new ArrayList<>();

    for (OWLClass ce: ont.getClassesInSignature(true)) {
      if (completeEquivalenceClasses) {
        changes.add(getCompleteEquivalence(reasoner, ce));
      }
      if (addDirectInferredSubclasses) {
        changes.addAll(getDirectInferredEdges(reasoner, ce));
      }
    }
    
    logger.info("Applying reasoned axioms: " + changes.size());
    manager.applyChanges(changes);
    logger.info("Completed applying reasoning changes");
  }

  public void reason(boolean completeEquivalenceClasses, boolean addDirectInferredSubclasses) {
    //TODO: Move this to a configuration file
    org.apache.log4j.Logger.getLogger("org.semanticweb.elk").setLevel(org.apache.log4j.Level.FATAL);

    removeUnsatisfiableClasses();

    logger.info("Creating reasoner for " + ont);
    OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
    logger.info("Completed creating reasoner for " + ont);

    if (!shouldReason(reasoner, ont)) {
      return;
    }
    reason(reasoner, completeEquivalenceClasses, addDirectInferredSubclasses);
    reasoner.dispose();
  }

}
