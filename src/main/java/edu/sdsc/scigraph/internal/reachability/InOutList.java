package edu.sdsc.scigraph.internal.reachability;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Objects;

@ThreadSafe
class InOutList {

  Set<Long> inList = new ConcurrentSkipListSet<>();
  Set<Long> outList = new ConcurrentSkipListSet<>();

  Set<Long> getInList() {
    return inList;
  }

  Set<Long> getOutList() {
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