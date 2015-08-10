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

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.Concept;
import io.scigraph.frames.EdgeProperties;
import io.scigraph.frames.NodeProperties;
import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

public class ConceptTest {

  @Test
  public void conceptEqualsContract() {
    EqualsVerifier.forClass(Concept.class).verify();
  }

  @Test
  public void commonPropertiesEqualsContract() {
    EqualsVerifier.forClass(CommonProperties.class).verify();
  }

  @Test
  public void edgePropertiesEqualsContract() {
    EqualsVerifier.forClass(EdgeProperties.class).verify();
  }

  @Test
  public void nodePropertiesEqualsContract() {
    EqualsVerifier.forClass(NodeProperties.class).verify();
  }

}
