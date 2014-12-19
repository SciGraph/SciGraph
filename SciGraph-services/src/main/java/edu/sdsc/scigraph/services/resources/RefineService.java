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

import io.dropwizard.jersey.caching.CacheControl;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.lucene.analysis.Analyzer;
import org.dozer.DozerBeanMapper;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.lucene.ExactAnalyzer;
import edu.sdsc.scigraph.owlapi.CurieUtil;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

@Path("/refine") 
@Api(value = "/refine", description = "OpenRefine services")
@Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP,
  MediaType.APPLICATION_XML, })
public class RefineService extends BaseResource {

  private final Vocabulary vocabulary;
  private final DozerBeanMapper mapper;
  private final CurieUtil curieUtil;

  private static final Analyzer analyzer = new ExactAnalyzer();

  @Inject
  RefineService(Vocabulary vocabulary, DozerBeanMapper mapper, CurieUtil curieUtil) {
    this.vocabulary = vocabulary;
    this.mapper = mapper;
    this.curieUtil = curieUtil;
  }

  @GET
  @Path("/reconcile")
  @ApiOperation(value = "Reconcile terms",
  notes = "Suggests terms based on a mispelled or mistyped term.",
  response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm(
      @ApiParam( value = "Mispelled term", required = true )
      @PathParam("term") String term,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("1") int limit,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    return null;
  }

}
