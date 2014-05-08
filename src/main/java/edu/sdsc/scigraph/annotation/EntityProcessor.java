package edu.sdsc.scigraph.annotation;

import java.io.IOException;
import java.util.List;

/***
 * EntityProcessor processes content looking for text that can be annotated.
 */
public interface EntityProcessor {

  /***
   * @param configuration
   * @return A list of entities found in the content
   * @throws IOException
   */
  public List<EntityAnnotation> annotateEntities(EntityFormatConfiguration configuration)
      throws IOException;

}