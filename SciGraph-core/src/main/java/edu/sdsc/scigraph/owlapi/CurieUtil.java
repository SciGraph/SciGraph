package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Iterables.getFirst;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class CurieUtil {

  private final BiMap<String, String> curieMap;

  @Inject
  CurieUtil(@Named("neo4j.curieMap") Map<String, String> curieMap) {
    this.curieMap = ImmutableBiMap.copyOf(curieMap);
  }

  public Optional<String> getCurie(final String uri) {
    Preconditions.checkNotNull(uri);
    for (Entry<String, String> entry: curieMap.entrySet()) {
      if (uri.startsWith(entry.getKey())) {
        return Optional.of(String.format("%s:%s", entry.getValue(), uri.substring(entry.getKey().length(), uri.length())));
      }
    }
    return Optional.absent();
  }

  public Optional<String> getFullUri(String curie) {
    Preconditions.checkNotNull(curie);
    String prefix = getFirst(Splitter.on(':').split(curie), null);
    if (null != prefix && curieMap.inverse().containsKey(prefix)) {
      String uriPrefix = curieMap.inverse().get(prefix);
      return Optional.of(String.format("%s%s", uriPrefix, curie.substring(curie.indexOf(':') + 1)));
    }
    return Optional.absent();
  }

}
