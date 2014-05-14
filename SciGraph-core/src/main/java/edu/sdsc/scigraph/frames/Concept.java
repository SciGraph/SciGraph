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

import com.tinkerpop.frames.Property;

public interface Concept extends NodeProperties {

  public static final String PREFERRED_LABEL = "preferedLabel";
  public static final String SYNONYM = "synonym";
  public static final String ACRONYM = "acronym";
  public static final String ABREVIATION = "abbreviation";
  public static final String DEFINITION = "definition";
  public static final String CATEGORY = "category"; 

  @Property(PREFERRED_LABEL)
  public void setPreferredLabel(String preferredLabel);

  @Property(PREFERRED_LABEL)
  public String getPreferredLabel();

  @Property(SYNONYM)
  public Iterable<String> getSynonyms();

  @Property(SYNONYM)
  public void addSynonym(String synonym);

  @Property(ACRONYM)
  public Iterable<String> getAcronyms();

  @Property(ACRONYM)
  public void addAcronym(String acronym);

  @Property(ABREVIATION)
  public Iterable<String> getAbbreviations();

  @Property(ABREVIATION)
  public void addAbbreviation(String abbreviation);

  @Property(DEFINITION)
  public void setDefinition(String term);

  @Property(DEFINITION)
  public String getDefinition();

  @Property(CATEGORY)
  public void addCategory(String category);

  @Property(CATEGORY)
  public Iterable<String> getCategories();

}
