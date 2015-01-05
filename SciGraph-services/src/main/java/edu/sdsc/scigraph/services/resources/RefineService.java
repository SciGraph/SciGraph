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

import static com.google.common.base.Strings.isNullOrEmpty;
import io.dropwizard.jersey.caching.CacheControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import org.apache.lucene.analysis.Analyzer;
import org.dozer.DozerBeanMapper;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.lucene.ExactAnalyzer;
import edu.sdsc.scigraph.owlapi.CurieUtil;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;
import edu.sdsc.scigraph.services.refine.RefineQueries;
import edu.sdsc.scigraph.services.refine.RefineQuery;
import edu.sdsc.scigraph.services.refine.RefineResult;
import edu.sdsc.scigraph.services.refine.RefineResults;
import edu.sdsc.scigraph.services.refine.RefineUtil;
import edu.sdsc.scigraph.services.refine.ServiceMetadata;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

@Path("/refine") 
@Api(value = "/refine", description = "OpenRefine services")
@Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP})
public class RefineService extends BaseResource {

  private final Vocabulary vocabulary;
  private final DozerBeanMapper mapper;
  private final CurieUtil curieUtil;
  private final ServiceMetadata metadata;

  private static final Analyzer analyzer = new ExactAnalyzer();

  @Inject
  RefineService(Vocabulary vocabulary, DozerBeanMapper mapper, CurieUtil curieUtil) {
    this.vocabulary = vocabulary;
    this.mapper = mapper;
    this.curieUtil = curieUtil;
    metadata = new ServiceMetadata();
    // TODO: How should this be populated on a per application basis?
    metadata.setIdentifierSpace("http://example.org");
    metadata.setName("SciGraph");
    metadata.setSchemaSpace("http://example.org");
  }

  @POST
  @Path("/reconcile")
  @ApiOperation(value = "Reconcile terms",
  notes = "",
  response = RefineResult.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm(
      @ApiParam( value = "Query (deprecated)", required = false)
      @FormParam("query") String query,
      @ApiParam( value = "Queries", required = false )
      @FormParam("queries") String queries,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @FormParam("callback") String callback) {
    try {
    if (isNullOrEmpty(query) && isNullOrEmpty(queries)) {
      GenericEntity<ServiceMetadata> response = new GenericEntity<ServiceMetadata>(metadata){};
      return JaxRsUtil.wrapJsonp(request, response, callback);
    } else if (!isNullOrEmpty(queries)) {
      RefineQueries refineQueries = RefineUtil.getQueries(queries);
      Map<String, RefineResults> resultMap = new HashMap<>();
      for (Entry<String, RefineQuery> entry: refineQueries.entrySet()) {
        RefineResults results = getResults(entry.getValue());
        resultMap.put(entry.getKey(), results);
      }
      GenericEntity<Map<String, RefineResults>> response = new GenericEntity<Map<String, RefineResults>>(resultMap){};
      return JaxRsUtil.wrapJsonp(request, response, callback);
    } else if (!isNullOrEmpty(query)) {
        RefineQuery refineQuery = RefineUtil.getQuery(query);
        RefineResults results = getResults(refineQuery);
        GenericEntity<RefineResults> response = new GenericEntity<RefineResults>(results){};
        return JaxRsUtil.wrapJsonp(request, response, callback);
    } else {
      // TODO: throw exception
      return null;
    }
    } catch (IOException e) {
      return null;
    }
  }

  RefineResults getResults(RefineQuery refineQuery) {
    Vocabulary.Query vocabQuery = RefineUtil.getVocabularyQuery(refineQuery);
    List<Concept> concepts = vocabulary.getConceptsFromTerm(vocabQuery);
    RefineResults results = RefineUtil.conceptsToRefineResults(concepts);
    return results;
  }
  
}
