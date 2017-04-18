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
package io.scigraph.analyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.prefixcommons.CurieUtil;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.NodeProperties;
import io.scigraph.internal.CypherUtil;
import io.scigraph.neo4j.Graph;

public class HyperGeometricAnalyzer {

  private final GraphDatabaseService graphDb;
  private final CurieUtil curieUtil;
  private final Graph graph;
  private final CypherUtil cypherUtil;

  @Inject
  HyperGeometricAnalyzer(GraphDatabaseService graphDb, CurieUtil curieUtil, Graph graph,
      CypherUtil cypherUtil) {
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
    this.graph = graph;
    this.cypherUtil = cypherUtil;
  }

  private double computeBonferroniCoeff(Set<AnalyzerInnerNode> set) {
    // Consider only properties which appear more than once
    int count = 0;
    for (AnalyzerInnerNode el : set) {
      if (el.getCount() > 1) {
        count++;
      }
    }
    return count;
  }

  private double getCountFrom(Set<AnalyzerInnerNode> set, Long id) throws Exception {
    for (AnalyzerInnerNode el : set) {
      if (el.getNodeId() == id) {
        return el.getCount();
      }
    }
    throw new Exception("Coud not retrieve count for id " + id);
  }

  private Set<AnalyzerInnerNode> resolveToParents(Long nodeId, Long count) {
    Set<AnalyzerInnerNode> innerNodeSet = new HashSet<>();
    String query = "match (n)<-[:subClassOf*]-(p) where id(n) = " + nodeId + " return p";
    Result result = cypherUtil.execute(query);
    while (result.hasNext()) {
      Map<String, Object> map = result.next();
      innerNodeSet.add(new AnalyzerInnerNode(((Node) map.get("p")).getId(), count));
    }
    return innerNodeSet;
  }
  
  
  private Set<AnalyzerInnerNode> getSampleSetNodes(AnalyzeRequest request) throws Exception {
    Map<Long, AnalyzerInnerNode> sampleNodes = new HashMap<Long, AnalyzerInnerNode>();
    List<Long> sampleSetId = new ArrayList<>();
    for (String sample : request.getSamples()) {
      sampleSetId.add(getNodeIdFromIri(sample));
    }
    String query =
        "match (r)<-[:subClassOf*]-(n)" + request.getPath()
            + "(i) where id(n) in " + sampleSetId + " and id(r) = "
            + getNodeIdFromIri(request.getOntologyClass()) + " with n, i as t return t, count(distinct n)";
    //System.out.println(query);
    Result result = cypherUtil.execute(query);
    while (result.hasNext()) {
      Map<String, Object> map = result.next();
      Long count = (Long) map.get("count(distinct n)");
      Long nodeId = ((Node) map.get("t")).getId();
      sampleNodes.put(nodeId, new AnalyzerInnerNode(nodeId, count));
      for(AnalyzerInnerNode parentToAdd: resolveToParents(nodeId, count)){
        if(sampleNodes.containsKey(parentToAdd.getNodeId())){
          AnalyzerInnerNode existingNode = sampleNodes.get(parentToAdd.getNodeId());
          sampleNodes.put(existingNode.getNodeId(),new  AnalyzerInnerNode(existingNode.getNodeId(), existingNode.getCount() + parentToAdd.getCount()));
        }else{
          sampleNodes.put(parentToAdd.getNodeId(), parentToAdd);
        }
      }
    }
    return new HashSet<AnalyzerInnerNode>(sampleNodes.values());
  }
  
  private Set<AnalyzerInnerNode> getCompleteSetNodes(AnalyzeRequest request) throws Exception {
    String query =
        "match (r)<-[:subClassOf*]-(n)" + request.getPath()
            + "(i) where id(r) = "
            + getNodeIdFromIri(request.getOntologyClass()) + " with n, i as t return t, count(distinct n)";
    //System.out.println(query);
    Result result2 = cypherUtil.execute(query);
    Set<AnalyzerInnerNode> allSubjects = new HashSet<>();
    while (result2.hasNext()) {
      Map<String, Object> map = result2.next();
      Long count = (Long) map.get("count(distinct n)");
      Long nodeId = ((Node) map.get("t")).getId();
      allSubjects.add(new AnalyzerInnerNode(nodeId, count));
      allSubjects.addAll(resolveToParents(nodeId, count));
    }
    return allSubjects;
  }

  private int getTotalCount(String ontologyClass) throws Exception {
    String query =
        "match (n)-[:subClassOf*]->(r) where id(r) = " + getNodeIdFromIri(ontologyClass)
            + " return count(*)";
    Result result3 = cypherUtil.execute(query);
    int totalCount = 0;
    while (result3.hasNext()) {
      Map<String, Object> map = result3.next();
      totalCount = ((Long) map.get("count(*)")).intValue();
    }
    return totalCount;
  }

  private String resolveCurieToIri(String curie) throws Exception {
    if (curieUtil.getCurie(curie).isPresent()) {
      return curie; // nothing to do, it is already an IRI
    }
    Optional<String> resolvedCurie = curieUtil.getIri(curie);
    if (resolvedCurie.isPresent()) {
      return resolvedCurie.get();
    } else {
      throw new Exception("Curie " + curie + " not recognized.");
    }
  }

  private long getNodeIdFromIri(String iri) throws Exception {
    Optional<Long> nodeIdOpt = graph.getNode(iri);
    if (nodeIdOpt.isPresent()) {
      return nodeIdOpt.get();
    } else {
      throw new Exception(iri + " does not map to a node.");
    }
  }
  
  AnalyzeRequest processRequest(AnalyzeRequest request) throws Exception {
    String resolvedOntologyClass = resolveCurieToIri(request.getOntologyClass());
    Collection<String> resolvedSamples = new ArrayList<String>();
    for (String sample : request.getSamples()) {
      resolvedSamples.add(resolveCurieToIri(sample));
    }

    AnalyzeRequest resolvedAnalyzeRequest = new AnalyzeRequest();
    resolvedAnalyzeRequest.setSamples(resolvedSamples);
    resolvedAnalyzeRequest.setPath(request.getPath());
    resolvedAnalyzeRequest.setOntologyClass(resolvedOntologyClass);
    return resolvedAnalyzeRequest;
  }

  public List<AnalyzerResult> analyze(AnalyzeRequest request) {
    List<AnalyzerResult> pValues = new ArrayList<>();
    try (Transaction tx = graphDb.beginTx()) {
      AnalyzeRequest processedRequest = processRequest(request);

      Set<AnalyzerInnerNode> sampleSetNodes = getSampleSetNodes(processedRequest);

      Set<AnalyzerInnerNode> completeSetNodes = getCompleteSetNodes(processedRequest);

      double bonferroniCoeff = computeBonferroniCoeff(completeSetNodes);

      int totalCount = getTotalCount(processedRequest.getOntologyClass());

      // apply the HyperGeometricDistribution for each node
      for (AnalyzerInnerNode n : sampleSetNodes) {
        HypergeometricDistribution hypergeometricDistribution =
            new HypergeometricDistribution(totalCount, (int) getCountFrom(completeSetNodes,
                n.getNodeId()), processedRequest.getSamples().size());
        double p =
            hypergeometricDistribution.upperCumulativeProbability((int) n.getCount())
                * bonferroniCoeff;
        String iri = graph.getNodeProperty(n.getNodeId(), CommonProperties.IRI, String.class).get();
        String curie = curieUtil.getCurie(iri).orElse(iri);
        String labels =
            StringUtils.join(
                graph.getNodeProperties(n.getNodeId(), NodeProperties.LABEL, String.class), ", ");
        pValues.add(new AnalyzerResult(labels, curie, p));
      }

      // sort by p-value
      Collections.sort(pValues, new AnalyzerResultComparator());

      tx.success();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return pValues;
  }

}
