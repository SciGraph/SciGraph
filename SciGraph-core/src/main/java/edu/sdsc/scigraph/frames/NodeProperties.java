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
package edu.sdsc.scigraph.frames;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface NodeProperties extends CommonProperties, VertexFrame {

  String LABEL = "label";
  String INFERRED = "inferred";
  String ANONYMOUS = "anonymous";

  @Property(LABEL)
  void addLabel(String label);

  @Property(LABEL)
  Iterable<String> getLabels();

  @Property(INFERRED)
  void setInferred(boolean inferred);

  @Property(INFERRED)
  void isInferred();

  @Property(ANONYMOUS)
  void setAnonymous(boolean anonymous);

  @Property(ANONYMOUS)
  boolean isAnonymous();

  @Adjacency(label="SUPERCLASS_OF")
  Iterable<Concept> getSubclasses();

  @Adjacency(label="SUBCLASS_OF")
  Iterable<Concept> getSuperclasses();
  
  @Adjacency(label="EQUIVALENT_TO")
  Iterable<Concept> getEquivalentClasses();

}
