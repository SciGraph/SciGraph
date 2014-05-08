package edu.sdsc.scigraph.annotation;

import javax.xml.bind.annotation.XmlRootElement;

/***
 * Represents an annotated section of text
 */
@XmlRootElement(name = "annotation")
public final class EntityAnnotation extends Token<Entity> implements Comparable<EntityAnnotation> {

  protected EntityAnnotation() {
    this(null, 0, 0);
  }

  public EntityAnnotation(Entity entity, int start, int end) {
    super(entity, start, end);
  }

  public int length() {
    return getEnd() - getStart();
  }

  /***
   * @param annotation
   * @return true if annotation contains or overlaps this
   */
  public boolean intersects(EntityAnnotation annotation) {
    return range.isConnected(annotation.range);
  }

  @Override
  public int compareTo(EntityAnnotation o) {
    if (equals(o))
      return 0;
    else if (getEnd() > o.getEnd())
      return 1;
    else if (getEnd() < o.getEnd())
      return -1;
    else if (getStart() < o.getStart())
      return 1;
    else
      return -1;
  }

}
