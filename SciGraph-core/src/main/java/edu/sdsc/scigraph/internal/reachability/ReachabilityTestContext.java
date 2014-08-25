package edu.sdsc.scigraph.internal.reachability;

/**
 * Helper class for parallel canReach() test..
 * 
 * @author chenjing
 *
 */
public class ReachabilityTestContext {

  private boolean allSatisfied = true;

  boolean isAllSatisfied() {
    return allSatisfied;
  }

  void setAllSatisfied(boolean flag) {
    allSatisfied = flag;
  }

}
