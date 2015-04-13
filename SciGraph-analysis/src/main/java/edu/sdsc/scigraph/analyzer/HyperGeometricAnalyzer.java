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
package edu.sdsc.scigraph.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class HyperGeometricAnalyzer {

	private GraphDatabaseService graphDb;

	public HyperGeometricAnalyzer(GraphDatabaseService graphDb) {
		this.graphDb = graphDb;
	}

	private double getCountFrom(Set<AnalyzerResult> set, Long id) throws Exception {
		for (AnalyzerResult el : set) {
			if (el.getN().getId() == id) {
				return el.getCount();
			}
		}
		throw new Exception("Coud not retrieve count for id " + id);
	}

	private Set<AnalyzerResult> getSampleSetNodes(List<String> sampleSet, String ontologyClass, String path) {
		Set<AnalyzerResult> sampleSetNodes = new HashSet<AnalyzerResult>();
		Result result = graphDb.execute("match (n:" + ontologyClass + ")-[rel:" + path
				+ "]->(t) where HAS (n.label) and n.label in " + sampleSet + " return t, count(*)");
		while (result.hasNext()) {
			Map<String, Object> map = result.next();
			sampleSetNodes.add(new AnalyzerResult((Node) map.get("t"), (Long) map.get("count(*)")));
		}
		return sampleSetNodes;
	}

	private Set<AnalyzerResult> getCompleteSetNodes() {
		Result result2 = graphDb.execute("match (n:pizza)-[rel:hasTopping]->(t) return t, count(*)");
		Set<AnalyzerResult> allSubjects = new HashSet<AnalyzerResult>();
		while (result2.hasNext()) {
			Map<String, Object> map = result2.next();
			allSubjects.add(new AnalyzerResult((Node) map.get("t"), (Long) map.get("count(*)")));
		}
		return allSubjects;
	}

	private int getTotalCount(String ontologyClass) {
		Result result3 = graphDb.execute("match (n:" + ontologyClass + ") return count(*)");
		int totalCount = 0;
		while (result3.hasNext()) {
			Map<String, Object> map = result3.next();
			totalCount = ((Long) map.get("count(*)")).intValue();
		}
		return totalCount;
	}

	public List<AnalyzerResult> analyze(List<String> sampleSet, String ontologyClass, String path) {
		ArrayList<AnalyzerResult> pValues = new ArrayList<AnalyzerResult>();
		try (Transaction tx = graphDb.beginTx()) {
			Set<AnalyzerResult> sampleSetNodes = getSampleSetNodes(sampleSet, ontologyClass, path);

			Set<AnalyzerResult> completeSetNodes = getCompleteSetNodes();

			int totalCount = getTotalCount(ontologyClass);

			// apply the HyperGeometricDistribution for each topping
			for (AnalyzerResult n : sampleSetNodes) {
				HypergeometricDistribution hypergeometricDistribution = new HypergeometricDistribution(totalCount,
						(int) getCountFrom(completeSetNodes, n.getN().getId()), sampleSet.size());
				double p = hypergeometricDistribution.upperCumulativeProbability((int) n.getCount());
				pValues.add(new AnalyzerResult(n.getN(), p));
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
