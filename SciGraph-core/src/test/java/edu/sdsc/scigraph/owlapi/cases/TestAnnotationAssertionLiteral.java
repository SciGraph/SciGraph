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
package edu.sdsc.scigraph.owlapi.cases;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.Node;

/***
 * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#subclassof-axioms
 */
public class TestAnnotationAssertionLiteral extends OwlTestCase {

  @Test
  public void testAnnotationAssertion() {
    Node i = getNode("http://example.org/i");
    assertThat("property value is set to foo", i.getProperty("http://example.org/p").toString(),
        is("foo"));
  }

}
