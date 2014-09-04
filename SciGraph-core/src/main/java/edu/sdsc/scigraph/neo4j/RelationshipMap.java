package edu.sdsc.scigraph.neo4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import org.neo4j.graphdb.RelationshipType;

import com.google.common.collect.ForwardingConcurrentMap;

/***
 * A map of external unique keys to internal Neo4j IDs for keeping track of these mappings during a
 * bulk load.
 *
 * TODO: This could be switched to MapDB if this structure needs to persist.
 *
 */
@ThreadSafe
public class RelationshipMap extends ForwardingConcurrentMap<Edge, Long> {

  private static final int INITIAL_CAPACITY = 200_000;
  
  private final ConcurrentHashMap<Edge, Long> delegate;

  public RelationshipMap() {
    this(INITIAL_CAPACITY);
  }

  public RelationshipMap(int initialCapacity) {
    delegate = new ConcurrentHashMap<Edge, Long>(initialCapacity);
  }
  
  @Override
  protected ConcurrentMap<Edge, Long> delegate() {
    return delegate();
  }
  
  public Long get(long start, long end, RelationshipType type) {
    return delegate.get(new Edge(start, end, type.toString()));
  }

  public boolean containsKey(long start, long end, RelationshipType type) {
    return delegate.containsKey(new Edge(start, end, type.toString()));
  };
  
  public Long put(long start, long end, RelationshipType type, Long value) {
    return delegate.put(new Edge(start, end, type.toString()), value);
  };

}
