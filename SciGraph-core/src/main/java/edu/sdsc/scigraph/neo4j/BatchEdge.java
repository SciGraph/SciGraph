package edu.sdsc.scigraph.neo4j;

import java.util.Objects;

class BatchEdge {

  private final Long start;
  private final Long end;
  private final String type;

  public BatchEdge(long start, long end, String type) {
    super();
    this.start = start;
    this.end = end;
    this.type = type;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public String getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BatchEdge) {
      BatchEdge c = (BatchEdge) obj;
      return Objects.equals(start, c.getStart()) && Objects.equals(end, c.getEnd())
          && Objects.equals(type, c.getType());
    } else {
      return false;
    }

  }

}
