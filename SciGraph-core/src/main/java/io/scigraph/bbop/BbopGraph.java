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
package io.scigraph.bbop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * A bean representation of the <a href="https://github.com/berkeleybop/bbop-js/wiki/Graph">bbop-js</a>
 * graph representation.
 */
public final class BbopGraph {

  List<BbopNode> nodes = new ArrayList<>();
  List<BbopEdge> edges = new ArrayList<>();
  Map<String, Object> meta = new HashMap<>();

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

  public Map<String, Object> getMeta() {
    return meta;
  }

  public void setMeta(Map<String, Object> meta) {
    this.meta = meta;
  }
}
