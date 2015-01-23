package edu.sdsc.scigraph.neo4j;

import java.util.Collection;
import java.util.List;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;

public interface GraphInterface {

  long createNode(String id);

  Optional<Long> getNode(String id);

  long createRelationship(long start, long end, RelationshipType type);

  Optional<Long> getRelationship(long start, long end, RelationshipType type);

  Collection<Long> createRelationshipsPairwise(Collection<Long> nodeIds, RelationshipType type);

  void setNodeProperty(long node, String property, Object value);

  void addNodeProperty(long node, String property, Object value);

  <T> Optional<T> getNodeProperty(long node, String property, Class<T> type);

  <T> List<T> getNodeProperties(long node, String property, Class<T> type);

  void setRelationshipProperty(long relationship, String property, Object value);

  void addRelationshipProperty(long relationship, String property, Object value);

  <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type);

  <T> List<T> getRelationshipProperties(long relationship, String property, Class<T> type);

  void setLabel(long node, Label label);

  void addLabel(long node, Label label);

  Collection<Label> getLabels(long node);

  void shutdown();
}
