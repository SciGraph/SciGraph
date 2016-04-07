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

import io.dropwizard.jersey.caching.CacheControl;
import io.scigraph.internal.CypherUtil;
import io.scigraph.services.jersey.BaseResource;
import io.scigraph.services.jersey.JaxRsUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/cypher")
@Api(value = "/cypher", description = "Cypher utility services")
@Produces({MediaType.TEXT_PLAIN})
public class CypherUtilService extends BaseResource {

  final private CypherUtil cypherUtil;

  @Inject
  CypherUtilService(CypherUtil cypherUtil) {
    this.cypherUtil = cypherUtil;
  }

  @GET
  @Timed
  @Produces({MediaType.TEXT_PLAIN})
  @Path("/resolve")
  @ApiOperation(
      value = "Cypher query resolver",
      response = String.class,
      notes = "Only resolves curies of the relationships, not the ones in the nodes or in the START.")
  public String resolve(
      @ApiParam(value = "The cypher query to resolve", required = true) @QueryParam("cypherQuery") String cypherQuery) {
    return cypherUtil.resolveRelationships(cypherQuery);
  }


  @GET
  @Path("/curies")
  @ApiOperation(value = "Get the curie map", response = String.class, responseContainer = "Map")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON})
  public Object getCuries(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false) @QueryParam("callback") String callback) {
    return JaxRsUtil.wrapJsonp(request.get(),
        new GenericEntity<Map<String, String>>(cypherUtil.getCurieMap()) {}, callback);
  }

}
