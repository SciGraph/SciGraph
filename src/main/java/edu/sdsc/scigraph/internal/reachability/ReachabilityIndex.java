package edu.sdsc.scigraph.internal.reachability;

import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Longs;

public class ReachabilityIndex {

  private static final Logger logger = Logger.getLogger(ReachabilityIndex.class.getName());

  private static final String INDEX_EXISTS_PROPERTY = "ReachablilityIndexExists";
  private static final String IN_LIST_PROPERTY  = "ReachablilityIndexInList";
  private static final String OUT_LIST_PROPERTY = "ReachablilityIndexOutList";

  private final GraphDatabaseService graphDb;
  private final Set<Long> forbiddenNodes;
  private final Node metaDataNode;

  /***
   * Manage a reachability index object on a graph
   * @param graphDb The graph on which to build the reachability index
   */
  public ReachabilityIndex(GraphDatabaseService graphDb) {
    this(graphDb, Collections.<Long>emptySet());
  }

  /**
   * Manage a reachability index object on a graph.
   * @param graph The graph on which to build the reachability index
   * @param forbiddenNodes Nodes that should be not be traversed during index building
   */
  public ReachabilityIndex(GraphDatabaseService graphdb, Collection<Long> forbiddenNodes) {
    this.graphDb = graphdb;
    this.forbiddenNodes = ImmutableSet.copyOf(forbiddenNodes);
    metaDataNode = graphDb.getNodeById(0);
  }

  /***
   * @return if a reachability index has already been created on this graph.
   */
  public boolean indexExists() {
    return (boolean)metaDataNode.getProperty(INDEX_EXISTS_PROPERTY, false);
  }

  /**
   * Create a reachability index on a graph.
 * @throws InterruptedException 
   */
  public void createIndex() {
    if (indexExists()) {
      throw new IllegalStateException("Reachability index already exists. Drop it first and then recreate it.");
    }

    long startTime = System.currentTimeMillis();
    Set<Entry<Long, Integer>> hopCoverages = getHopCoverages();
    long endTime = System.currentTimeMillis();
    logger.fine(format("Takes %d second(s) to calculate HopCoverage",
        TimeUnit.MILLISECONDS.toSeconds(endTime - startTime)));

    MemoryReachabilityIndex inMemoryIndex = new MemoryReachabilityIndex();

    TraversalDescription incomingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.INCOMING))
        .evaluator(new ReachabilityEvaluator(inMemoryIndex, Direction.INCOMING, forbiddenNodes));

    TraversalDescription outgoingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.OUTGOING))
        .evaluator(new ReachabilityEvaluator(inMemoryIndex, Direction.OUTGOING, forbiddenNodes));

    long bfsTime = 0, rbfsTime = 0;
    for (Entry<Long, Integer> coverage : hopCoverages) {
      Node workingNode = graphDb.getNodeById(coverage.getKey());
      startTime = System.currentTimeMillis();
      
      InOutListTraverser incomingListTaverser = new InOutListTraverser(incomingTraversal,workingNode);
      incomingListTaverser.start();
      
      InOutListTraverser outgoingListTaverser = new InOutListTraverser(outgoingTraversal,workingNode);
      outgoingListTaverser.start();
      
  	  try {
		incomingListTaverser.join();
     	outgoingListTaverser.join();
	  } catch (InterruptedException e) {
		throw new RuntimeException ("Reachability index creation inerrupted.");
	  }

      endTime = System.currentTimeMillis() ;
      rbfsTime  += endTime - startTime;
      for (Path p :outgoingTraversal.traverse(workingNode)) {
        logger.finest(p.toString()); // Avoids unused variable warning 
      }
      bfsTime += System.currentTimeMillis() - endTime;
    }

    logger.fine("BFS index building time: " + (bfsTime/1000) + " sec(s), RBFS index building time: " + (rbfsTime /1000) + " sec(s).");
    commitIndexToGraph(inMemoryIndex);
    logger.info("Reachability index created.");
  }

  void commitIndexToGraph(MemoryReachabilityIndex inMemoryIndex) {
    Transaction tx = graphDb.beginTx();

    int operationCount = 0;
    for(Entry<Long, InOutList> e: inMemoryIndex.entrySet() ) {
      Node node = graphDb.getNodeById(e.getKey());
      operationCount++;
      if ( operationCount % 500000 == 0 ) {
        logger.fine("commit transaction when populating in-out list.");
        tx.success();
        tx.finish();
        tx = graphDb.beginTx();
      }
      node.setProperty(IN_LIST_PROPERTY, Longs.toArray(e.getValue().getInList()));
      node.setProperty(OUT_LIST_PROPERTY, Longs.toArray(e.getValue().getOutList()));
    }

    metaDataNode.setProperty(INDEX_EXISTS_PROPERTY, true);
    tx.success();
    tx.finish();
  }

  public void dropIndex() {
    if (indexExists()) {
      Transaction tx = graphDb.beginTx();

      // ...cleanup the index.
      for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
        n.removeProperty(IN_LIST_PROPERTY);
        n.removeProperty(OUT_LIST_PROPERTY);
      }

      // reset the flag.
      metaDataNode.setProperty(INDEX_EXISTS_PROPERTY, false);

      tx.success();
      tx.finish();
      logger.info("Reachability index dropped.");
    } else {
      logger.warning("There was no reachability index to drop.");
    }
  }

  /** 
   * @return The hopCoverage for each node and sort them in descending order.
   */
  private SortedSet<Entry<Long,Integer>> getHopCoverages(){
    SortedSet<Entry<Long,Integer>> nodeSet = new TreeSet<Entry<Long,Integer>>(
        new Comparator<Entry<Long,Integer>>() {
          public int compare(Entry<Long,Integer> a, Entry<Long,Integer> b) {
            int difference = b.getValue() - a.getValue();
            return (0 != difference) ? difference : (int)(a.getKey() - b.getKey());
          }
        }
        );

    for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
      int relationshipCount = size(n.getRelationships());
      nodeSet.add(new AbstractMap.SimpleEntry<Long, Integer>(n.getId(), relationshipCount));
    }

    return nodeSet;
  }

  /**
   * @param startNode
   * @param endNode
   * @return Return true if startNode can reach endNode
   */
  public boolean canReach(Node startNode, Node endNode) {
    if (!indexExists()) {
      throw new IllegalStateException("Reachability index must be created first."); 
    }
    long[] outList = (long[])startNode.getProperty(OUT_LIST_PROPERTY);
    long[] inList = (long[])endNode.getProperty(IN_LIST_PROPERTY);
    int i = 0, j = 0;

    while (i < outList.length && j < inList.length) {
      if (outList[i] < inList[j]) { 
        i++;
      } else if (inList[j] < outList[i]) {
        j++;
      } else {
        return true;
      }
    }
    return false;
  }
  
  
  protected class InOutListTraverser  extends Thread {
	  
	  TraversalDescription traversalDescription = null;
	  Node startNode = null;
	  
	  protected InOutListTraverser(TraversalDescription td, Node starter) {
		  this.traversalDescription = td;  
		  this.startNode = starter;
	  }
	  
	  public void run() {
		 if (startNode == null) {
			 logger.info("null pointer for startNode");
			 return;
		 }
		  for (Path p: traversalDescription.traverse(startNode)) {
	          logger.finest(p.toString()); // Avoids unused variable warning
	        }
	  }
	  
  }
  
}
