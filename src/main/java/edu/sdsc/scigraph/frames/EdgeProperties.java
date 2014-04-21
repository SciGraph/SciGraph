/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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

  public final static String TRANSITIVE = "transitive";
  public final static String REFLEXIVE = "reflexive";
  public final static String SYMMETRIC = "symmetric";
  public final static String QUANTIFICATION_TYPE = "quantificationType";

  @Property(TRANSITIVE)
  public void setTransitive(boolean transitive);

  @Property(TRANSITIVE)
  public boolean isTransitive();

  @Property(REFLEXIVE)
  public void setReflexive(boolean reflexive);

  @Property(REFLEXIVE)
  public boolean isReflexive();

  @Property(SYMMETRIC)
  public void setSymmetric(boolean symmetric);

  @Property(SYMMETRIC)
  public boolean isSymmetric();

  @Property(QUANTIFICATION_TYPE)
  public void setQuantificationType(String type);

  @Property(QUANTIFICATION_TYPE)
  public String getQuantificationType();

}
