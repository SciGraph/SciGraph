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
package io.scigraph.internal.reachability;

import java.util.Collections;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import com.google.common.base.Predicate;

final class ReachabilityEvaluator implements Evaluator {

  private final InMemoryReachabilityIndex inMemoryIndex;
  private final Direction direction;
  private final Predicate<Node> nodePredicate;

  ReachabilityEvaluator(InMemoryReachabilityIndex inMemoryIdx,
      Direction direction,
      Predicate<Node> nodePredicate) {
    this.inMemoryIndex = inMemoryIdx;
    this.direction = direction;
    this.nodePredicate = nodePredicate;
  }

  @Override
  public Evaluation evaluate(Path path) {
    long currentId = path.endNode().getId();
    if (!nodePredicate.apply(path.endNode())) {
      inMemoryIndex.get(currentId);
      return Evaluation.EXCLUDE_AND_PRUNE;
    }

    long startId = path.startNode().getId(); // Vi
    InOutList listPair = inMemoryIndex.get(currentId);

    if (0 == path.length()) {
      // first node in the traverse - add itself to the in-out list
      listPair.getInList().add(currentId);
      listPair.getOutList().add(currentId);
      return Evaluation.INCLUDE_AND_CONTINUE;
    }
    else if (direction == Direction.INCOMING ) {
      // doing reverse BFS
      if (nodesAreConnected(currentId, startId)) {
        return Evaluation.EXCLUDE_AND_PRUNE;
      } else {
        listPair.getOutList().add(startId);
        return Evaluation.INCLUDE_AND_CONTINUE;
      }
    } else {
      //doing BFS
      if (nodesAreConnected(startId, currentId)) { // cur is w
        return Evaluation.EXCLUDE_AND_PRUNE;
      } else {
        listPair.getInList().add(startId);
        return Evaluation.INCLUDE_AND_CONTINUE;
      }
    }
  }

  boolean nodesAreConnected(long nodeIdOut, long nodeIdIn) {
    Set<Long> outList = inMemoryIndex.get(nodeIdOut).getOutList();
    Set<Long> inList = inMemoryIndex.get(nodeIdIn).getInList();
    return !Collections.disjoint(outList, inList);
  }

}
