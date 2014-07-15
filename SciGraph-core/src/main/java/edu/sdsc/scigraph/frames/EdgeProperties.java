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

import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.Property;

public interface EdgeProperties extends CommonProperties, EdgeFrame {

  String TRANSITIVE = "transitive";
  String REFLEXIVE = "reflexive";
  String SYMMETRIC = "symmetric";
  String QUANTIFICATION_TYPE = "quantificationType";

  @Property(TRANSITIVE)
  void setTransitive(boolean transitive);

  @Property(TRANSITIVE)
  boolean isTransitive();

  @Property(REFLEXIVE)
  void setReflexive(boolean reflexive);

  @Property(REFLEXIVE)
  boolean isReflexive();

  @Property(SYMMETRIC)
  void setSymmetric(boolean symmetric);

  @Property(SYMMETRIC)
  boolean isSymmetric();

  @Property(QUANTIFICATION_TYPE)
  void setQuantificationType(String type);

  @Property(QUANTIFICATION_TYPE)
  String getQuantificationType();

}
