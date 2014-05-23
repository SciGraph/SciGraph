package edu.sdsc.scigraph.internal.reachability;

import java.util.HashSet;
import java.util.Set;

class InOutList {

  Set<Long> inList = new HashSet<>();
  Set<Long> outList = new HashSet<>();

  Set<Long> getInList() {
    return inList;
  }

  public Set<Long> getOutList() {
    return outList;
  }

}