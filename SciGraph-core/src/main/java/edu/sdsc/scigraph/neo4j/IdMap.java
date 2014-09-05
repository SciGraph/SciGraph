package edu.sdsc.scigraph.neo4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.ForwardingConcurrentMap;

/***
 * A map of external unique keys to internal Neo4j IDs for keeping track of these mappings during a
 * bulk load.
 *
 * TODO: This could be switched to MapDB if this structure needs to persist.
 *
 */
@ThreadSafe
public class IdMap extends ForwardingConcurrentMap<String, Long> {

  private static final int INITIAL_CAPACITY = 200_000;
  
  private final ConcurrentHashMap<String, Long> delegate;
  private final AtomicLong idCounter = new AtomicLong();

  public IdMap() {
    this(INITIAL_CAPACITY);
  }

  public IdMap(int initialCapacity) {
    delegate = new ConcurrentHashMap<String, Long>(initialCapacity);
  }

  @Override
  protected ConcurrentMap<String, Long> delegate() {
    return delegate();
  }

  @Override
  public Long get(Object key) {
    delegate.putIfAbsent((String) key, idCounter.getAndIncrement());
    return delegate.get(key);
  }

}
