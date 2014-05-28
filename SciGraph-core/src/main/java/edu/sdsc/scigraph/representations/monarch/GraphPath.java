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