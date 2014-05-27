package edu.sdsc.scigraph.internal.reachability;

import java.util.Collections;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import com.google.common.base.Predicate;

class ReachabilityEvaluator implements Evaluator {

  private final MemoryReachabilityIndex inMemoryIdx;
  private final Direction direction;
  private final Predicate<Node> nodePredicate;

  ReachabilityEvaluator (MemoryReachabilityIndex inMemoryIdx, 
      Direction direction,
      Predicate<Node> nodePredicate) {
    this.inMemoryIdx = inMemoryIdx;
    this.direction = direction;
    this.nodePredicate = nodePredicate;
  }

  @Override
  public Evaluation evaluate(Path path) {
    if (!nodePredicate.apply(path.endNode())) {
      return Evaluation.EXCLUDE_AND_PRUNE;
    }

    long startId = path.startNode().getId(); // Vi
    long currentId = path.endNode().getId();

    if (0 == path.length()) {
      // first node in the traverse - add itself to the in-out list
      InOutList listPair = inMemoryIdx.get(currentId);
      listPair.getInList().add(currentId);
      listPair.getOutList().add(currentId);
      return Evaluation.INCLUDE_AND_CONTINUE;
    }
    else if (direction == Direction.INCOMING ) {
      // doing reverse BFS
      if (nodesAreConnectedInIndex(currentId, startId)) {
        return Evaluation.EXCLUDE_AND_PRUNE;
      } else {
        InOutList listPair = inMemoryIdx.get(currentId);
        listPair.getOutList().add(startId);
        return Evaluation.INCLUDE_AND_CONTINUE;
      }
    } else {
      //doing BFS
      if ( nodesAreConnectedInIndex(startId,currentId)) { // cur is w
        return Evaluation.EXCLUDE_AND_PRUNE;
      } else {
        InOutList listPair = inMemoryIdx.get(currentId);
        listPair.getInList().add(startId);
        return Evaluation.INCLUDE_AND_CONTINUE;
      }
    }
  }

  private boolean nodesAreConnectedInIndex(long nodeIdOut, long nodeIdIn) {
    Set<Long> outList = inMemoryIdx.get(nodeIdOut).getOutList();
    Set<Long> inList = inMemoryIdx.get(nodeIdIn).getInList();
    return !Collections.disjoint(outList, inList);
  }

}
