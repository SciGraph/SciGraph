package edu.sdsc.scigraph.neo4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

public class GraphUtil {

  /***
   * @param container
   * @param property
   * @param type
   * @return the single property value for node with the supplied type
   */
  static public <T> Optional<T> getProperty(PropertyContainer container, String property,
      Class<T> type) {
    Optional<T> value = Optional.<T> absent();
    if (container.hasProperty(property)) {
      value = Optional.<T> of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  /***
   * @param container
   * @param property
   * @param type
   * @return a list of properties for node with the supplied type
   */
  static public <T> List<T> getProperties(PropertyContainer container, String property,
      Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.hasProperty(property)) {
      if (container.getProperty(property).getClass().isArray()) {
        for (Object o : (Object[]) container.getProperty(property)) {
          list.add(type.cast(o));
        }
      } else {
        list.add(type.cast(container.getProperty(property)));
      }
    }

    return list;
  }

  static public Iterable<Relationship> getRelationships(final Node a, final Node b,
      RelationshipType type) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(type);

    return filter(a.getRelationships(type), new Predicate<Relationship>() {
      @Override
      public boolean apply(Relationship relationship) {
        return relationship.getEndNode().equals(b);
      }
    });
  }

  static String getLastPathFragment(URI uri) {
    return uri.getPath().replaceFirst(".*/([^/?]+).*", "$1");
  }

  public static String getFragment(URI uri) {
    if (null != uri.getFragment()) {
      return uri.getFragment();
    } else if (uri.toString().startsWith("mailto:")) {
      return uri.toString().substring("mailto:".length());
    } else {
      return getLastPathFragment(uri);
    }
  }

}
