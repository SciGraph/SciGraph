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
package edu.sdsc.scigraph.services.resources;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import com.codahale.metrics.annotation.Timed;

import edu.sdsc.scigraph.analyzer.AnalyzerResult;
import edu.sdsc.scigraph.analyzer.HyperGeometricAnalyzer;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.GraphApi;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.services.jersey.BaseResource;

@Path("/analyzer")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyzerService extends BaseResource {

	private final GraphDatabaseService graphDb;
	private final GraphApi api;

	@Inject
	AnalyzerService(GraphDatabaseService graphDb, GraphApi api) {
		this.graphDb = graphDb;
		this.api = api;
	}

	@GET
	@Timed
	public String analyze(@QueryParam("pizza") List<String> pizzas) {
		HyperGeometricAnalyzer hyperGeometricAnalyzer = new HyperGeometricAnalyzer(graphDb);
		List<AnalyzerResult> pValues = hyperGeometricAnalyzer.analyze(pizzas);
		String response = "";
		for (AnalyzerResult p : pValues) {
			response += p.getN() + " " + p.getCount() + "\n";
		}
		System.out.println(pValues);
		return response;
	}

	public String formatNode(Node n) {
		return n.getId()
				+ " " // + n.getLabels() + " " + n.getPropertyKeys() + " "
				+ GraphUtil.getProperties(n, NodeProperties.LABEL, String.class) + " "
				+ GraphUtil.getProperties(n, Concept.CATEGORY, String.class);
		// + " "
		// + n.getRelationships()
		// + " "
	}

}
