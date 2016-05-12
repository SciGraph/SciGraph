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
package io.scigraph.neo4j;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/***
 * A convenience class for representing a RelationshipType and Direction.
 */
public final class DirectedRelationshipType {

  private final RelationshipType type;

  private final Direction direction;

  public DirectedRelationshipType(RelationshipType type) {
    this(type, Direction.BOTH);
  }

  public DirectedRelationshipType(RelationshipType type, Direction direction) {
    this.type = checkNotNull(type);
    this.direction = checkNotNull(direction);
  }

  @JsonCreator
  public DirectedRelationshipType(@JsonProperty("type") String type, @JsonProperty("direction") String direction) {
    this.type = RelationshipType.withName(type);
    this.direction = Direction.valueOf(direction);
  }

  @JsonIgnore
  public RelationshipType getType() {
    return type;
  }

  @JsonProperty("type")
  public String getRelationshipType() {
    return type.name();
  }
  
  @JsonProperty
  public Direction getDirection() {
    return direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type.name(), direction);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DirectedRelationshipType)) {
      return false;
    }
    DirectedRelationshipType c = (DirectedRelationshipType) obj;
    return Objects.equals(type.name(), c.getType().name()) && Objects.equals(direction, c.getDirection());
  }

  @Override
  public String toString() {
    return type.name() + " - " + direction.name();
  }

}
