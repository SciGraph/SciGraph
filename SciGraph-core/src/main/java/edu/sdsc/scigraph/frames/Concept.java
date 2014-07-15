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

import com.tinkerpop.frames.Property;

public interface Concept extends NodeProperties {

  String PREFERRED_LABEL = "preferedLabel";
  String SYNONYM = "synonym";
  String ACRONYM = "acronym";
  String ABREVIATION = "abbreviation";
  String DEFINITION = "definition";
  String CATEGORY = "category";

  @Property(PREFERRED_LABEL)
  void setPreferredLabel(String preferredLabel);

  @Property(PREFERRED_LABEL)
  String getPreferredLabel();

  @Property(SYNONYM)
  Iterable<String> getSynonyms();

  @Property(SYNONYM)
  void addSynonym(String synonym);

  @Property(ACRONYM)
  Iterable<String> getAcronyms();

  @Property(ACRONYM)
  void addAcronym(String acronym);

  @Property(ABREVIATION)
  Iterable<String> getAbbreviations();

  @Property(ABREVIATION)
  void addAbbreviation(String abbreviation);

  @Property(DEFINITION)
  void setDefinition(String term);

  @Property(DEFINITION)
  String getDefinition();

  @Property(CATEGORY)
  void addCategory(String category);

  @Property(CATEGORY)
  Iterable<String> getCategories();

}
