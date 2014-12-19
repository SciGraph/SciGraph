package edu.sdsc.scigraph.services.refine;

import java.util.Map;
import java.util.Objects;

public class RefineQuery {

  String query;

  int limit;

  String type;

  String type_strict;

  Map<String, Object> properties;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType_strict() {
    return type_strict;
  }

  public void setType_strict(String type_strict) {
    this.type_strict = type_strict;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, limit, type, type_strict, properties);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RefineQuery other = (RefineQuery) obj;
    return Objects.equals(this.query, other.query)
        && Objects.equals(this.limit, other.limit)
        && Objects.equals(this.type, other.type)
        && Objects.equals(this.type_strict, other.type_strict)
        && Objects.equals(this.properties, other.properties);
  }

}
