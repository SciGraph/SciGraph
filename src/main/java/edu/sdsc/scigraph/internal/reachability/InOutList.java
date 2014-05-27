package edu.sdsc.scigraph.internal.reachability;

import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Objects;

class InOutList {

  Set<Long> inList = new TreeSet<>();
  Set<Long> outList = new TreeSet<>();

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