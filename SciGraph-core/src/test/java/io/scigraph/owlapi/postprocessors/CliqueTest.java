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
package io.scigraph.owlapi.postprocessors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.frames.NodeProperties;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class CliqueTest extends GraphTestBase {

  Node clique11, clique12, clique13, clique21, clique22;
  Graph graph = new TinkerGraph();
  Clique clique;
  static final RelationshipType IS_EQUIVALENT = OwlRelationships.OWL_EQUIVALENT_CLASS;
  static final String leaderAnnotation = "https://monarchinitiative.org/MONARCH_cliqueLeader";

  @Before
  public void setup() {
    clique11 = createNode("http://x.org/a");
    clique12 = createNode("http://x.org/b");
    clique13 = createNode("http://x.org/c");
    clique21 = createNode("http://x.org/d");
    clique22 = createNode("http://x.org/e");
    Relationship r1 = clique11.createRelationshipTo(clique12, IS_EQUIVALENT);
    Relationship r2 = clique12.createRelationshipTo(clique13, IS_EQUIVALENT);
    Relationship r3 = clique21.createRelationshipTo(clique22, IS_EQUIVALENT);
    Relationship r4 = clique12.createRelationshipTo(clique22, RelationshipType.withName("hasPhenotype"));
    Relationship r5 = clique13.createRelationshipTo(clique21, RelationshipType.withName("hasPhenotype"));
    
    CliqueConfiguration cliqueConfiguration = new CliqueConfiguration();
    Set<String> rel =  new HashSet<String>();
    rel.add(IS_EQUIVALENT.name());
    cliqueConfiguration.setRelationships(rel);
    Set<String> forbidden =  new HashSet<String>();
    forbidden.add("anonymous");
    cliqueConfiguration.setLeaderForbiddenLabels(forbidden);
    cliqueConfiguration.setLeaderAnnotation(leaderAnnotation);

    clique = new Clique(graphDb, cliqueConfiguration);
  }

  @Test
  public void edgesAreMovedToLeader() {
    ResourceIterator<Node> allNodes = graphDb.getAllNodes().iterator();
    Node n1 = getNode("http://x.org/a", allNodes);
    Node n2 = getNode("http://x.org/b", allNodes);
    Node n3 = getNode("http://x.org/c", allNodes);
    Node n4 = getNode("http://x.org/d", allNodes);
    Node n5 = getNode("http://x.org/e", allNodes);
    assertThat(n1.getDegree(RelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n2.getDegree(RelationshipType.withName("hasPhenotype")), is(1));
    assertThat(n3.getDegree(RelationshipType.withName("hasPhenotype")), is(1));
    assertThat(n1.getDegree(IS_EQUIVALENT), is(1));
    assertThat(n2.getDegree(IS_EQUIVALENT), is(2));
    assertThat(n3.getDegree(IS_EQUIVALENT), is(1));
    assertThat(n4.getDegree(), is(2));
    assertThat(n5.getDegree(), is(2));

    clique.run();

    assertThat(n1.getDegree(RelationshipType.withName("hasPhenotype")), is(2));
    assertThat(n2.getDegree(RelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n3.getDegree(RelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n1.getDegree(IS_EQUIVALENT), is(2));
    assertThat(n2.getDegree(IS_EQUIVALENT), is(1));
    assertThat(n3.getDegree(IS_EQUIVALENT), is(1));
    assertThat(n4.getDegree(), is(3));
    assertThat(n5.getDegree(), is(1));
    assertThat(n1.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(true));
    assertThat(n2.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
    assertThat(n3.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
    assertThat(n4.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(true));
    assertThat(n5.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
  }
  
  @Test
  public void prefixLeaderPrioritizer() {
    Node a = createNode("http://x.org/a");
    Node c = createNode("http://y.org/c");
    Node d = createNode("http://z.org/d");
    List<Node> cliqueNode = Arrays.asList(a, createNode("http://x.org/b"), c, d, createNode("http://x.org/e"));
    assertThat(clique.electCliqueLeader(cliqueNode, new ArrayList<String>()).getId(), is(a.getId()));
    assertThat(clique.electCliqueLeader(cliqueNode, Arrays.asList("http://z.org/", "http://x.org/", "http://y.org/")).getId(), is(d.getId()));
    assertThat(clique.electCliqueLeader(cliqueNode, Arrays.asList("fake", "fake", "fake")).getId(), is(a.getId()));
    assertThat(clique.electCliqueLeader(cliqueNode, Arrays.asList("http://y.org/", "http://x.org/", "http://y.org/")).getId(), is(c.getId()));
    assertThat(clique.electCliqueLeader(cliqueNode, Arrays.asList("http://x.org/", "http://x.org/", "http://y.org/")).getId(), is(a.getId()));
  }
  
  @Test
  public void prefixLeaderPrioritizerWithReaIris() {
    Node a = createNode("http://purl.obolibrary.org/obo/NCBITaxon_10116");
    Node b = createNode("http://identifiers.org/FB:FBsp00000020");
    List<Node> cliqueNode = Arrays.asList(a, b);
    assertThat(clique.electCliqueLeader(cliqueNode, Arrays.asList("http://www.ncbi.nlm.nih.gov/gene/", "http://www.ncbi.nlm.nih.gov/pubmed/",  "http://purl.obolibrary.org/obo/NCBITaxon_",
        "http://identifiers.org/ensembl/", "http://purl.obolibrary.org/obo/DOID_", "http://purl.obolibrary.org/obo/HP_")).getId(), is(a.getId()));
  }

  @Test
  public void designatedLeaderPrioritizer() {
    Node a = createNode("http://x.org/a");
    Node b = createNode("http://x.org/b");
    Node c = createNode("http://y.org/c");
    Node d = createNode("http://z.org/d");
    c.setProperty(leaderAnnotation, true);
    List<Node> cliqueNode = Arrays.asList(a, b, c, d);
    assertThat(clique.electCliqueLeader(cliqueNode, new ArrayList<String>()).getId(), is(c.getId()));
  }

  @Test
  public void designatedLeaderPrioritizerOverPriorityList() {
    Node a = createNode("http://x.org/a");
    Node b = createNode("http://x.org/b");
    Node c = createNode("http://y.org/c");
    Node d = createNode("http://z.org/d");
    c.setProperty(leaderAnnotation, true);
    List<String> priorityList = Arrays.asList("http://z.org/", "http://x.org/");
    List<Node> cliqueNode = Arrays.asList(a, b, c, d);
    assertThat(clique.electCliqueLeader(cliqueNode, priorityList).getId(), is(c.getId()));
  }

  @Test
  public void anonymousLeader() {
    Node a = createNode("http://x.org/a");
    Node b = createNode("http://x.org/b");
    Node c = createNode("http://x.org/c");
    a.addLabel(OwlLabels.OWL_ANONYMOUS);
    b.addLabel(OwlLabels.OWL_ANONYMOUS);
    List<Node> cliqueNode = Arrays.asList(a, b, c);
    assertThat(clique.electCliqueLeader(cliqueNode, new ArrayList<String>()).getId(), is(c.getId()));
    assertThat(clique.electCliqueLeader(Arrays.asList(a, b), new ArrayList<String>()).getId(), is(a.getId()));
  }

  private Node getNode(String iri, Iterator<Node> allNodes) {
    while (allNodes.hasNext()) {
      Node currentNode = allNodes.next();
      Optional<String> optionalIri = GraphUtil.getProperty(currentNode, NodeProperties.IRI, String.class);
      if (optionalIri.isPresent()) {
        if (optionalIri.get().equals(iri)) {
          return currentNode;
        }
      }
    }
    return null;
  }
}
