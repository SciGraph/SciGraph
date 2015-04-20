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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.analyzer.AnalyzeRequest;
import edu.sdsc.scigraph.analyzer.AnalyzerResult;
import edu.sdsc.scigraph.analyzer.HyperGeometricAnalyzer;
import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;

@Path("/analyzer")
@Api(value = "/analyzer", description = "Analysis services")
@Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
public class AnalyzerService extends BaseResource {

  private final Provider<HyperGeometricAnalyzer> provider;

  @Inject
  AnalyzerService(Provider<HyperGeometricAnalyzer> provider) {
    this.provider = provider;
  }

  @GET
  @Timed
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
  @Path("/enrichment")
  @ApiOperation(value = "Class Enrichment Service", response = String.class,
  notes="")
  public Object enrich(
      @ApiParam( value = "A list of CURIEs for nodes whose attributes are to be tested for enrichment. For example, a list of genes.", required = true)
      @QueryParam("sample") Set<String> samples,
      @ApiParam( value = "CURIE for parent ontology class for the attribute to be tested. For example, GO biological process", required = true)
      @QueryParam("ontologyClass") String ontologyClass,
      @ApiParam( value = "A path expression that connects sample nodes to attribute class nodes", required = true)
      @QueryParam("path") String path,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    HyperGeometricAnalyzer hyperGeometricAnalyzer = provider.get();
    AnalyzeRequest analyzeRequest = new AnalyzeRequest();
    analyzeRequest.setOntologyClass(ontologyClass);
    analyzeRequest.setPath(path);
    analyzeRequest.setSamples(samples);
    List<AnalyzerResult> analyzeResult = hyperGeometricAnalyzer.analyze(analyzeRequest);
    GenericEntity<List<AnalyzerResult>> response = new GenericEntity<List<AnalyzerResult>>(analyzeResult){};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/enrichment")
  public List<AnalyzerResult> enrichPost(@Valid AnalyzeRequest analyzeRequest) {
    HyperGeometricAnalyzer hyperGeometricAnalyzer = provider.get();
    return hyperGeometricAnalyzer.analyze(analyzeRequest);
  }

}
