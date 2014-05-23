package edu.sdsc.scigraph.internal.reachability;

import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ForwardingMap;

class MemoryReachabilityIndex extends ForwardingMap<Long, InOutList>{

  TreeMap<Long, InOutList> delegate = new TreeMap<>();

  @Override
  protected Map<Long, InOutList> delegate() {
    return delegate;
  }

  @Override
  public InOutList get(Object key) {
    if (!containsKey(key)) {
      super.put((Long)key, new InOutList());
    }
    return super.get(key);
  }

}