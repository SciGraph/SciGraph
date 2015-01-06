package edu.sdsc.scigraph.services.api.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BbopGraph {

  List<BbopNode> nodes = new ArrayList<>();
  List<BbopEdge> edges = new ArrayList<>();

  public List<BbopNode> getNodes() {
    return nodes;
  }

  public void setNodes(List<BbopNode> nodes) {
    this.nodes = nodes;
  }

  public List<BbopEdge> getEdges() {
    return edges;
  }

  public void setEdges(List<BbopEdge> edges) {
    this.edges = edges;
  }

  public static class BbopNode {
    String id;
    String lbl;
    Map<String, Object> meta = new HashMap<>();

    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public String getLbl() {
      return lbl;
    }
    public void setLbl(String lbl) {
      this.lbl = lbl;
    }
    public Map<String, Object> getMeta() {
      return meta;
    }
    public void setMeta(Map<String, Object> meta) {
      this.meta = meta;
    }

  }

  public static class BbopEdge{
    String sub;
    String obj;
    String pred;
    Map<String, Object> meta = new HashMap<>();

    public String getSub() {
      return sub;
    }
    public void setSub(String sub) {
      this.sub = sub;
    }
    public String getObj() {
      return obj;
    }
    public void setObj(String obj) {
      this.obj = obj;
    }
    public String getPred() {
      return pred;
    }
    public void setPred(String pred) {
      this.pred = pred;
    }
    public Map<String, Object> getMeta() {
      return meta;
    }
    public void setMeta(Map<String, Object> meta) {
      this.meta = meta;
    }

  }

}
