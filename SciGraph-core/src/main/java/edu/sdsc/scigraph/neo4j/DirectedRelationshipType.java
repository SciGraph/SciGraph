package edu.sdsc.scigraph.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

/***
 * A convenience class for representing a RelationshipType and Direction.
 */
public class DirectedRelationshipType {

  private final RelationshipType type;
  private final Direction direction;

  public DirectedRelationshipType(RelationshipType type) {
    this(type, Direction.BOTH);
  }

  public DirectedRelationshipType(RelationshipType type, Direction direction) {
    this.type = type;
    this.direction = direction;
  }

  public RelationshipType getType() {
    return type;
  }

  public Direction getDirection() {
    return direction;
  }

}
