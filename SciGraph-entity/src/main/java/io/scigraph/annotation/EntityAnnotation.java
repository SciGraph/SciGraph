/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.annotation;


/***
 * Represents an annotated section of text
 */
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
    if (range.equals(o.range))
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
