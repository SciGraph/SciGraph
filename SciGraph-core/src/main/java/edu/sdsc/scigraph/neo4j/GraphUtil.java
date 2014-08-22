package edu.sdsc.scigraph.neo4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/***
 * Convenience utilities for dealing with Neo4j graphs.
 * 
 * <b>Note:</b> Clients are responsible for managing transactions around these methods.
 */
public class GraphUtil {

  /***
   * Add a property value to a container.
   * 
   * Abstracts dealing with underlying value arrays for the client. The property may already have a
   * value. <br />
   * <b>Note:</b> duplicate values are ignored.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param value
   *          The value to append
   */
  public static void addProperty(PropertyContainer container, String property, Object value) {
    if (container.hasProperty(property)) {
      // We might be creating or updating an array - read everything into a Set<>
      Object origValue = container.getProperty(property);
      Class<?> clazz = value.getClass();
      Set<Object> valueSet = new LinkedHashSet<>();
      if (origValue.getClass().isArray()) {
        for (int i = 0; i < Array.getLength(origValue); i++) {
          valueSet.add(Array.get(origValue, i));
        }
      } else {
        valueSet.add(origValue);
      }
      valueSet.add(value);

      // Now write the set back if necessary
      if (valueSet.size() > 1) {
        Object newArray = Array.newInstance(clazz, valueSet.size());
        int i = 0;
        for (Object obj : valueSet) {
          Array.set(newArray, i++, clazz.cast(obj));
        }
        container.setProperty(property, newArray);
      }
    } else {
      container.setProperty(property, value);
    }
  }

  /***
   * Get a single valued property in a type safe way.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param type
   *          The type of the property
   * @return An optional property value
   * @throws ClassCastException
   *           if the property types don't match
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
   * Get multi-valued properties.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param type
   *          The type of the property
   * @return An list of property values
   * @throws ClassCastException
   *           if the property types don't match
   */
  static public <T> List<T> getProperties(PropertyContainer container, String property,
      Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.hasProperty(property)) {
      if (container.getProperty(property).getClass().isArray()) {
        Object value = container.getProperty(property);
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < Array.getLength(value); i++) {
          objects.add(Array.get(value, i));
        }
        for (Object o : objects) {
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
