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
package io.scigraph.services.api.vocabulary;

import io.scigraph.services.api.graph.ConceptDTOLite;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
public class Completion implements Comparable<Completion> {

  final String completion;
  final String type;
  final ConceptDTOLite concept;

  Completion() {
    this(null, null, null);
  }

  public Completion(String completion, String type, ConceptDTOLite concept) {
    this.completion = completion;
    this.type = type;
    this.concept = concept;
  }

  public String getCompletion() {
    return completion;
  }

  public String getType() {
    return type;
  }

  public ConceptDTOLite getConcept() {
    return concept;
  }

  @Override
  public int compareTo(Completion o) {
    return String.CASE_INSENSITIVE_ORDER.compare(getCompletion(), o.getCompletion());
  }

}
