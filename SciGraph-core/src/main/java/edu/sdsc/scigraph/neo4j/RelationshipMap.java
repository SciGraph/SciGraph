package edu.sdsc.scigraph.neo4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.neo4j.graphdb.RelationshipType;

import com.google.common.collect.ForwardingConcurrentMap;

/***
 * A map of external unique keys to internal Neo4j IDs for keeping track of these mappings during a
 * bulk load.
 *
 * TODO: This could be switched to MapDB if this structure needs to persist.
 *
 */
public class RelationshipMap extends ForwardingConcurrentMap<Composite, Long> {

  ConcurrentHashMap<Composite, Long> delegate = new ConcurrentHashMap<Composite, Long>(200_000);

  @Override
  protected ConcurrentMap<Composite, Long> delegate() {
    return delegate();
  }
  
  public Long get(long start, long end, RelationshipType type) {
    return delegate.get(new Composite(start, end, type.toString()));
  }

  public boolean containsKey(long start, long end, RelationshipType type) {
    return delegate.containsKey(new Composite(start, end, type.toString()));
  };
  
  public Long put(long start, long end, RelationshipType type, Long value) {
    return delegate.put(new Composite(start, end, type.toString()), value);
  };

}
