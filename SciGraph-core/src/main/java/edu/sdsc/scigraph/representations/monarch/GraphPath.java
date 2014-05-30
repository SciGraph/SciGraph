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
package edu.sdsc.scigraph.representations.monarch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class GraphPath {

  @XmlElement
  @JsonProperty
  public List<Vertex> nodes = new ArrayList<>();

  @XmlElement
  @JsonProperty
  public List<Edge> edges = new ArrayList<>();

  @XmlRootElement
  public static class Vertex {
    @XmlElement
    @JsonProperty
    String id;

    @XmlElement
    @JsonProperty("lbl")
    String label;

    @XmlElement
    @JsonProperty
    public Map<String, Object> meta = new HashMap<>();

    Vertex() {
    }

    public Vertex(String id, String label) {
      this.id = id;
      this.label = label;
    }
  }

  @XmlRootElement
  public static class Edge {
    @XmlElement
    @JsonProperty("sub")
    String subject;

    @XmlElement
    @JsonProperty("obj")
    String object;

    @XmlElement
    @JsonProperty("pred")
    String predicate;

    @XmlElement
    @JsonProperty
    public Map<String, Object> meta = new HashMap<>();

    Edge() {
    }

    public Edge(String subject, String object, String predicate) {
      this.subject = subject;
      this.object = object;
      this.predicate = predicate;
    }

  }

}