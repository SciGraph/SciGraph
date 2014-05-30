/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.internal.reachability;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;

import java.util.AbstractMap;
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
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Longs;

public class ReachabilityIndex {

  private static final Logger logger = Logger.getLogger(ReachabilityIndex.class.getName());

  private static final String INDEX_EXISTS_PROPERTY = "ReachablilityIndexExists";
  private static final String IN_LIST_PROPERTY  = "ReachablilityIndexInList";
  private static final String OUT_LIST_PROPERTY = "ReachablilityIndexOutList";

  private final GraphDatabaseService graphDb;
  private final Node metaDataNode;
  
  private int transactionBatchSize = 500000;   //default transaction size.

  /***
   * Manage a reachability index object on a graph
   * @param graphDb The graph on which to build the reachability index
   */
  public ReachabilityIndex(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    metaDataNode = graphDb.getNodeById(0);
  }

  /***
   * @return if a reachability index has already been created on this graph.
   */
  public boolean indexExists() {
    return (boolean)metaDataNode.getProperty(INDEX_EXISTS_PROPERTY, false);
  }

  public void createIndex() throws InterruptedException {
    createIndex(Predicates.<Node>alwaysTrue());
  }

  
  public String getInListPropertyName() { return IN_LIST_PROPERTY;}
  public String getOutListPropertyName() { return OUT_LIST_PROPERTY;}
  
  public void setTransactionBatchSize(int size) { this.transactionBatchSize = size;}
  
  /**
   * Create a reachability index on a graph.
   * @throws InterruptedException 
   */
  public void createIndex(Predicate<Node> nodePredicate) throws InterruptedException {
    if (indexExists()) {
      throw new IllegalStateException("Reachability index already exists. Drop it first and then recreate it.");
    }

    long startTime = System.currentTimeMillis();
    Set<Entry<Long, Integer>> hopCoverages = getHopCoverages(nodePredicate);
    long endTime = System.currentTimeMillis();
    logger.info(format("Takes %d second(s) to calculate HopCoverage",
        TimeUnit.MILLISECONDS.toSeconds(endTime - startTime)));

    MemoryReachabilityIndex inMemoryIndex = new MemoryReachabilityIndex();

    TraversalDescription incomingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.INCOMING))
        .evaluator(new ReachabilityEvaluator(inMemoryIndex, Direction.INCOMING, nodePredicate));

    TraversalDescription outgoingTraversal = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
        .expand(new DirectionalPathExpander(Direction.OUTGOING))
        .evaluator(new ReachabilityEvaluator(inMemoryIndex, Direction.OUTGOING, nodePredicate));

    startTime = System.currentTimeMillis();
    
    for (Entry<Long, Integer> coverage : hopCoverages) {
    
      Node workingNode = graphDb.getNodeById(coverage.getKey());

      if ( coverage.getValue()<0 ) {
    	inMemoryIndex.put(coverage.getKey(), new InOutList()) ; 
      } else {
        InOutListTraverser incomingListTaverser = new InOutListTraverser(incomingTraversal,workingNode);
        incomingListTaverser.start();

        InOutListTraverser outgoingListTaverser = new InOutListTraverser(outgoingTraversal,workingNode);
        outgoingListTaverser.start();

        incomingListTaverser.join();
        outgoingListTaverser.join();
      }

    }

    endTime = System.currentTimeMillis();

    logger.info("InMemoryReachability index building time: " + ((endTime-startTime)/1000) + " sec(s).");
    commitIndexToGraph(inMemoryIndex);
    logger.info("Reachability index created.");
  }

  void commitIndexToGraph(MemoryReachabilityIndex inMemoryIndex) {
    Transaction tx = graphDb.beginTx();

    int operationCount = 0;
    for(Entry<Long, InOutList> e: inMemoryIndex.entrySet() ) {
      Node node = graphDb.getNodeById(e.getKey());
      operationCount++;
      if ( operationCount % transactionBatchSize == 0 ) {
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
      int counter = 0;
      for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
        n.removeProperty(IN_LIST_PROPERTY);
        n.removeProperty(OUT_LIST_PROPERTY);
    	counter ++;
    	if ( counter % transactionBatchSize == 0) {
    		logger.fine("commit transaction when populating in-out list.");
            tx.success();
            tx.finish();
            tx = graphDb.beginTx();
    	}
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
   * @return The hop coverage for each node sorted in descending order.
   */
  SortedSet<Entry<Long,Integer>> getHopCoverages(Predicate<Node> nodePredicate){
    SortedSet<Entry<Long,Integer>> nodeSet = new TreeSet<Entry<Long,Integer>>(
        new Comparator<Entry<Long,Integer>>() {
          @Override
          public int compare(Entry<Long,Integer> a, Entry<Long,Integer> b) {
            int difference = b.getValue() - a.getValue();
            return (0 != difference) ? difference : (int)(a.getKey() - b.getKey());
          }
        });

    for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
      if ( n.getId() > 0 ) {
        int relationshipCount = nodePredicate.apply(n) ? 	  
    		  size(n.getRelationships()) : -1;
        nodeSet.add(new AbstractMap.SimpleEntry<Long, Integer>(n.getId(), relationshipCount));
      }
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

  static class InOutListTraverser extends Thread {

    private final TraversalDescription traversalDescription;
    private final Node startNode;

    InOutListTraverser(TraversalDescription td, Node startNode) {
      checkNotNull(startNode, "startNode must not be null.");
      this.traversalDescription = td;
      this.startNode = startNode;
    }

    @Override
    public void run() {
      for (Path p: traversalDescription.traverse(startNode)) {
        logger.finest(p.toString()); // Avoids unused variable warning
      }
    }

  }

}
