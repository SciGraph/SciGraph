package edu.sdsc.scigraph.internal.reachability;


import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashSet;
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

import com.google.common.base.Optional;
import com.google.common.primitives.Longs;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;

public class ReachabilityIndex {

  private static final Logger logger = Logger.getLogger(ReachabilityIndex.class.getName());

  public static final String dataDictionaryURI = "http://scigraph.scichruch.com/metadata";

  public static final String thingURI = "http://www.w3.org/2002/07/owl#Thing";

  private static final String INDEX_EXISTS_PROPERTY = "ReachablilityIndexExists";
  protected static final String propInList  = "ReachablilityIndexInList";
  protected static final String propOutList = "ReachablilityIndexOutList";

  private Graph<Concept> graph;
  private GraphDatabaseService graphDb;
  private boolean indexExists; // flag if there is an index to use.
  private final Set<Long> forbiddenNodes = new HashSet<>(); // node id for entity "http://www.w3.org/2002/07/owl#Thing"

  /**
   * Initialize a reachability index object on graph. 
   * @param graph
   */
  public ReachabilityIndex(Graph<Concept> graph) {
    this.graph = graph;
    graphDb = graph.getGraphDb();

    Optional<Node> opt = graph.getNode(dataDictionaryURI);

    if (!opt.isPresent()) {
      indexExists= false;
    } else {
      indexExists = graph.getProperty(opt.get(), INDEX_EXISTS_PROPERTY, Boolean.class).or(false);
    }

    opt = graph.getNode(thingURI);
    if (opt.isPresent()) {
      forbiddenNodes.add(opt.get().getId());
    }
  }

  /**
   * Initialize a reachability index object on graphDb. 
   * @param graph
   */
  public ReachabilityIndex(GraphDatabaseService graphdb) {
    graphDb = graphdb;
    graph = null;

    Node n = graphDb.getNodeById(0);

    indexExists = (boolean)n.getProperty(INDEX_EXISTS_PROPERTY, false);
  }


  /**
   * Create a reachability index on a graph.
   */
  public void creatIndex() {
    if (indexExists) {
      throw new IllegalStateException("Reachability index already exists. Drop it first then create again.");
    }

    long startTime = System.currentTimeMillis();

    Set<Entry<Long, Integer>> hopCoverages = getHopCoverages();

    long endTime = System.currentTimeMillis();

    logger.info(format("Takes %d second(s) to calculate HopCoverage",
        TimeUnit.MILLISECONDS.toSeconds(endTime - startTime)));

    MemoryReachabilityIndex inMemoryIdx = new MemoryReachabilityIndex();

    TraversalDescription incomingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.INCOMING))
        .evaluator(new ReachabilityEvaluator(inMemoryIdx, Direction.INCOMING, forbiddenNodes));

    TraversalDescription outgoingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.OUTGOING))
        .evaluator(new ReachabilityEvaluator(inMemoryIdx, Direction.OUTGOING, forbiddenNodes));

    long bfsTime = 0, rbfsTime = 0;
    for (Entry<Long, Integer> coverage : hopCoverages) {
      Node workingNode = graphDb.getNodeById(coverage.getKey());
      startTime = System.currentTimeMillis();
      for (Path p: incomingTraversal.traverse(workingNode)) {
        logger.fine(p.toString()); // Avoids unused variable warning
      }
      endTime = System.currentTimeMillis() ;
      rbfsTime  += endTime - startTime;
      for (Path p :outgoingTraversal.traverse(workingNode)) {
        logger.fine(p.toString()); // Avoids unused variable warning 
      }
      bfsTime += System.currentTimeMillis() - endTime;
    }

    logger.info("BFS index building time: " + (bfsTime/1000) + " sec(s), RBFS index building time: " + (rbfsTime /1000) + " sec(s).");
    // store the inMemoryIdx into db.
    Transaction tx = graphDb.beginTx();

    int counter=0;
    for(Entry<Long, InOutList> e: inMemoryIdx.entrySet() ) {
      Node node = graphDb.getNodeById(e.getKey());
      counter++;
      if ( counter % 500000 == 0 ) {
        logger.info("commit transaction when populating in-out list.");
        tx.success();
        tx.finish();
        tx = graphDb.beginTx();
      }

      node.setProperty(propInList, Longs.toArray(e.getValue().getInList()));
      node.setProperty(propOutList, Longs.toArray(e.getValue().getOutList()));
    }

    Node n = (null == graph) ? graphDb.getNodeById(0): graph.getOrCreateNode(dataDictionaryURI);
    n.setProperty(INDEX_EXISTS_PROPERTY, true);
    tx.success();
    tx.finish();

    indexExists = true;
    logger.info("Reachability index created.");
  }

  public void dropIndex() {
    if (indexExists) {
      Transaction tx = graphDb.beginTx();
      Node n0;
      if (graph != null ) {
        n0 = graph.getNode(dataDictionaryURI).get();
      } else {
        n0 = graphDb.getNodeById(0);
      }
      // ...cleanup the index.
      for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
        n.removeProperty(propInList);
        n.removeProperty(propOutList);
      }

      // reset the flag.
      n0.setProperty(INDEX_EXISTS_PROPERTY, false);

      tx.success();
      tx.finish();

      indexExists = false;
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
    if (!indexExists) {
      throw new IllegalStateException("Reachability index must be created first."); 
    }
    long[] outList = (long[])startNode.getProperty(propOutList);
    long[] inList = (long[])endNode.getProperty(propInList);
    int i=0, j=0;

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
}
