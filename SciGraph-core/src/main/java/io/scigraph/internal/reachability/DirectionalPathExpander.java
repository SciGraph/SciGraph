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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

/***
 * A utility path expander to expand <i>any</i> relationship in a direction
 */
final class DirectionalPathExpander implements PathExpander<Void> {

  private final Direction direction;

  public DirectionalPathExpander(Direction direction) {
    this.direction = direction;
  }

  @Override
  public Iterable<Relationship> expand(Path path, BranchState<Void> state) {
    return path.endNode().getRelationships(direction);
  }

  @Override
  public PathExpander<Void> reverse() {
    return null;
  }

}
