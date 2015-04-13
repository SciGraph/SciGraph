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

	public double getCountFrom(Set<AnalyzerResult> set, Long id) {
		for (AnalyzerResult el : set) {
			if (el.getN().getId() == id) {
				return el.getCount();
			}
		}
		System.out.println("NOT GOOD"); // TODO throw proper exception
		return 0;
	}

	public List<AnalyzerResult> analyze(List<String> sampleSet) {
		ArrayList<AnalyzerResult> pValues = new ArrayList<AnalyzerResult>();
		Set<AnalyzerResult> sampleSetNodes = new HashSet<AnalyzerResult>();
		try (Transaction tx = graphDb.beginTx();
				Result result = graphDb
						.execute("match (n:pizza)-[rel:hasTopping]->(t) where HAS (n.label) and n.label in "
								+ sampleSet + " return t, count(*)")) {
			while (result.hasNext()) {
				Map<String, Object> map = result.next();
				sampleSetNodes.add(new AnalyzerResult((Node) map.get("t"), (Long) map.get("count(*)")));
			}
			System.out.println("sympleSetNodes " + sampleSetNodes);

			Result result2 = graphDb.execute("match (n:pizza)-[rel:hasTopping]->(t) return t, count(*)");
			Set<AnalyzerResult> allSubjects = new HashSet<AnalyzerResult>();
			while (result2.hasNext()) {
				Map<String, Object> map = result2.next();
				allSubjects.add(new AnalyzerResult((Node) map.get("t"), (Long) map.get("count(*)")));
			}
			System.out.println("allSubjects " + allSubjects);

			Result result3 = graphDb.execute("match (n:pizza) return count(*)");
			int totalNumberOfPizzas = 0;
			while (result3.hasNext()) {
				Map<String, Object> map = result3.next();
				totalNumberOfPizzas = ((Long) map.get("count(*)")).intValue();
			}
			System.out.println("total " + totalNumberOfPizzas);

			for (AnalyzerResult n : sampleSetNodes) {
				HypergeometricDistribution hypergeometricDistribution = new HypergeometricDistribution(
						totalNumberOfPizzas, (int) getCountFrom(allSubjects, n.getN().getId()), sampleSet.size());
				double p = hypergeometricDistribution.upperCumulativeProbability((int) n.getCount());
				pValues.add(new AnalyzerResult(n.getN(), p));
			}
			System.out.println("pValues " + pValues);

			Collections.sort(pValues, new AnalyzerResultComparator());

			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pValues;
	}

}
