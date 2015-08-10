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

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Sets;

/***
 * Represents a collection of overlapping terms.
 * <p>
 * For instance if "foo" is a recognized term and "foo nacho" is a recognized
 * term we want to make sure that we embed both terms in the annotation...
 * 
 * @param The
 *          type of annotation in the group
 */
public class EntityAnnotationGroup extends ForwardingQueue<EntityAnnotation> {

  private static final Comparator<EntityAnnotation> comparator = new LengthComparator();

  private final PriorityQueue<EntityAnnotation> delegate = new PriorityQueue<EntityAnnotation>(5,
      comparator);

  @Override
  protected Queue<EntityAnnotation> delegate() {
    return delegate;
  }

  /***
   * @param annotation
   * @return true if annotation intersects any member of this group
   */
  public boolean intersects(EntityAnnotation annotation) {
    for (EntityAnnotation member : this) {
      if (member.intersects(annotation))
        return true;
    }
    return false;
  }

  /***
   * @return the minimum offset in this annotation group
   */
  public int getStart() {
    int start = Integer.MAX_VALUE;
    for (EntityAnnotation member : this) {
      if (member.getStart() < start) {
        start = member.getStart();
      }
    }
    return start;
  }

  /***
   * @return the maximum offset in this annotation group
   */
  public int getEnd() {
    int end = Integer.MIN_VALUE;
    for (EntityAnnotation member : this) {
      if (member.getEnd() > end) {
        end = member.getEnd();
      }
    }
    return end;
  }

  /***
   * @return All of the entities contained in this group
   */
  public Set<Entity> getAnnotations() {
    Set<Entity> entities = Sets.newHashSet();
    for (EntityAnnotation member : this) {
      entities.add(member.getToken());
    }
    return entities;
  }

  @Override
  public boolean equals(Object obj) {
    return Arrays.equals(delegate.toArray(), ((EntityAnnotationGroup)obj).toArray());
  }

  private static class LengthComparator implements Comparator<EntityAnnotation> {

    @Override
    public int compare(EntityAnnotation o1, EntityAnnotation o2) {
      if (o1.length() > o2.length()) {
        return -1;
      } else if (o2.length() > o1.length()) {
        return 1;
      } else {
        return 0;
      }
    }

  }

}
