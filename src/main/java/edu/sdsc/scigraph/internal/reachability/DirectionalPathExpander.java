package edu.sdsc.scigraph.internal.reachability;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

/***
 * A utility path expander to expand <i>any</i> relationship in a direction
 */
class DirectionalPathExpander implements PathExpander<Void> {

  private final Direction direction;

  DirectionalPathExpander (Direction direction) {
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
