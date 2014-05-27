package edu.sdsc.scigraph.internal.reachability;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.ForwardingMap;

@ThreadSafe
class MemoryReachabilityIndex extends ForwardingMap<Long, InOutList> {

  ConcurrentSkipListMap<Long, InOutList> delegate = new ConcurrentSkipListMap<>();

  @Override
  protected Map<Long, InOutList> delegate() {
    return delegate;
  }

  @Override
  public InOutList get(Object key) {
    delegate.putIfAbsent((Long) key, new InOutList());
    return delegate.get(key);
  }

}