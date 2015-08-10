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
package io.scigraph.frames;

public class EdgeProperties extends CommonProperties {

  public EdgeProperties(long id) {
    super(id);
  }
  public static final String TRANSITIVE = "transitive";
  public static final String REFLEXIVE = "reflexive";
  public static final String SYMMETRIC = "symmetric";
  public static final String QUANTIFICATION_TYPE = "quantificationType";

}
