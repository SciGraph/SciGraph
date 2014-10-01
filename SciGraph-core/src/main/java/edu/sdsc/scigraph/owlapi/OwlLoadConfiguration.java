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
package edu.sdsc.scigraph.owlapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;

import edu.sdsc.scigraph.neo4j.OntologyConfiguration;

public class OwlLoadConfiguration {

  private OntologyConfiguration ontologyConfiguration;
  private List<String> ontologyUrls = new ArrayList<>();
  private Map<String, String> categories = new HashMap<>();
  private List<MappedProperty> mappedProperties = new ArrayList<>();
  private Set<String> indexedNodeProperties = new HashSet<>();
  private Set<String> exactNodeProperties = new HashSet<>();

  public OntologyConfiguration getOntologyConfiguration() {
    return ontologyConfiguration;
  }

  public void setOntologyConfiguration(OntologyConfiguration ontologyConfiguration) {
    this.ontologyConfiguration = ontologyConfiguration;
  }

  public List<String> getOntologyUrls() {
    return ontologyUrls;
  }

  public Map<String, String> getCategories() {
    return categories;
  }

  public List<MappedProperty> getMappedProperties() {
    return mappedProperties;
  }

  public Set<String> getIndexedNodeProperties() {
    return indexedNodeProperties;
  }

  public Set<String> getExactNodeProperties() {
    return exactNodeProperties;
  }

  public static class MappedProperty {
    String name;
    List<String> properties;

    public String getName() {
      return name;
    }

    public List<String> getProperties() {
      return properties;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this.getClass())
          .add("name", name)
          .add("properties", properties)
          .toString();
    }

  }

}