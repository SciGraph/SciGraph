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
package io.scigraph.services.resources;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

import io.dropwizard.jersey.caching.CacheControl;
import io.scigraph.frames.Concept;
import io.scigraph.services.jersey.BaseResource;
import io.scigraph.services.jersey.CustomMediaTypes;
import io.scigraph.services.jersey.JSONProcessingException;
import io.scigraph.services.jersey.JaxRsUtil;
import io.scigraph.services.refine.ConceptView;
import io.scigraph.services.refine.RefineQueries;
import io.scigraph.services.refine.RefineQuery;
import io.scigraph.services.refine.RefineResult;
import io.scigraph.services.refine.RefineResults;
import io.scigraph.services.refine.RefineUtil;
import io.scigraph.services.refine.ServiceMetadata;
import io.scigraph.vocabulary.Vocabulary;

@Path("/refine") 
@Api(value = "/refine", 
description = "OpenRefine Reconciliation Services")
@SwaggerDefinition(tags = {@Tag(name="refine", description="OpenRefine Reconciliation Services")})
// *NOTE* OpenRefine requires a JSONP response - don't remove it.
@Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP})
public class RefineService extends BaseResource {

  private final Vocabulary vocabulary;
  private final ServiceMetadata metadata;

  @Inject
  RefineService(Vocabulary vocabulary, ServiceMetadata metadata) {
    this.vocabulary = vocabulary;
    this.metadata = metadata;
  }

  @POST
  @Path("/reconcile")
  @ApiOperation(value = "Reconcile terms",
  notes = DocumentationStrings.RECONCILE_NOTES,
  response = RefineResult.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm_POST(
      @ApiParam( value = DocumentationStrings.RECONCILE_QUERY_DOC, required = false)
      @FormParam("query") String query,
      @ApiParam( value = DocumentationStrings.RECONCILE_QUERIES_DOC, required = false )
      @FormParam("queries") String queries) {
    return suggestFromTerm(query, queries, null);
  }

  @GET
  @Path("/reconcile")
  @ApiOperation(value = "Reconcile terms",
  notes = DocumentationStrings.RECONCILE_NOTES,
  response = RefineResult.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm(
      @ApiParam( value = DocumentationStrings.RECONCILE_QUERY_DOC, required = false)
      @QueryParam("query") String query,
      @ApiParam( value = DocumentationStrings.RECONCILE_QUERIES_DOC, required = false )
      @QueryParam("queries") String queries,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    try {
      if (isNullOrEmpty(query) && isNullOrEmpty(queries)) {
        GenericEntity<ServiceMetadata> response = new GenericEntity<ServiceMetadata>(metadata){};
        return JaxRsUtil.wrapJsonp(request.get(), response, callback);
      } else if (!isNullOrEmpty(queries)) {
        RefineQueries refineQueries = RefineUtil.getQueries(queries);
        Map<String, RefineResults> resultMap = new HashMap<>();
        for (Entry<String, RefineQuery> entry: refineQueries.entrySet()) {
          RefineResults results = getResults(entry.getValue());
          resultMap.put(entry.getKey(), results);
        }
        GenericEntity<Map<String, RefineResults>> response = new GenericEntity<Map<String, RefineResults>>(resultMap){};
        return JaxRsUtil.wrapJsonp(request.get(), response, callback);
      } else if (!isNullOrEmpty(query)) {
        RefineQuery refineQuery = RefineUtil.getQuery(query);
        RefineResults results = getResults(refineQuery);
        GenericEntity<RefineResults> response = new GenericEntity<RefineResults>(results){};
        return JaxRsUtil.wrapJsonp(request.get(), response, callback);
      }
    } catch (IOException e) {
      String badJson = isNullOrEmpty(query) ? queries : query;
      throw new JSONProcessingException(badJson);
    }
    return null;
  }

  RefineResults getResults(RefineQuery refineQuery) {
    Vocabulary.Query vocabQuery = RefineUtil.getVocabularyQuery(refineQuery);
    List<Concept> concepts = vocabulary.getConceptsFromTerm(vocabQuery);
    RefineResults results = RefineUtil.conceptsToRefineResults(concepts);
    return results;
  }

  @GET
  @Path("/view/{id}")
  @Produces(MediaType.TEXT_HTML)
  public ConceptView getView(@PathParam("id") String id) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Optional<Concept> concept = vocabulary.getConceptFromId(query);
    if (!concept.isPresent()) {
      throw new WebApplicationException(404);
    }
    return new ConceptView(concept.get());
  }

  @GET
  @Path("/preview/{id}")
  @Produces(MediaType.TEXT_HTML)
  public ConceptView getPreview(@PathParam("id") String id) {
    return getView(id);
  }

}
