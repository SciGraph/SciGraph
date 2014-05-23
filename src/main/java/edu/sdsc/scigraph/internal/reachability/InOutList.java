package edu.sdsc.scigraph.internal.reachability;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Objects;

class InOutList {

  Set<Long> inList = new HashSet<>();
  Set<Long> outList = new HashSet<>();

  Set<Long> getInList() {
    return inList;
  }

  public Set<Long> getOutList() {
    return outList;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
    .add("inList", inList)
    .add("outList", outList)
    .toString();
  }

}