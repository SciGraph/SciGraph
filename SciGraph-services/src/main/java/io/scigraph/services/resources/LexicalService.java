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
import io.scigraph.annotation.Token;
import io.scigraph.lexical.LexicalLib;
import io.scigraph.lexical.pos.PosToken;
import io.scigraph.services.jersey.BaseResource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Path("/lexical")
@Api(value = "/lexical", description = "Lexical services")
@SwaggerDefinition(tags = {@Tag(name="lexical", description="Lexical services")})
@Produces({ MediaType.APPLICATION_JSON })
public class LexicalService extends BaseResource {

  @Inject
  private LexicalLib lexicalLib;

  /***
   * Break text into sentences.
   * 
   * @param text The source text
   * @param callback The name of the JSONP callback function
   * @return A list of sentences
   */
  @GET
  @Path("/sentences")
  @ApiOperation(value = "Split text into sentences.", 
  response = String.class,
  responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getSentences(
      @ApiParam( value = "The text to split", required = true )
      @QueryParam("text") @DefaultValue("") String text) {
    List<String> sentences = lexicalLib.extractSentences(text);
    return sentences;
  }

  /***
   * Extract POS tags from input text
   * 
   * @param text The source text
   * @param callback The name of the JSONP callback function
   * @return A list of POS tokens
   */
  @GET
  @Path("/pos")
  @ApiOperation(value = "Tag parts of speech.", 
  response = PosToken.class,
  responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public List<PosToken> getPos(
      @ApiParam( value = "The text to tag", required = true )
      @QueryParam("text") @DefaultValue("") String text) {
    List<PosToken> tokens = lexicalLib.tagPOS(text);
    return tokens;
  }

  /***
   * Extract "chunks" from input text. Chunks are based on POS heuristics.
   * 
   * @param text The source text
   * @param callback The name of the JSONP callback function
   * @return A list of chunks
   */
  @GET
  @Path("/chunks")
  @ApiOperation(value = "Extract entities from text.", response = Token.class,
  responseContainer = "List",
  notes = "The extracted chunks are based upon POS tagging. This may result in different results that extracting entities.")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public  List<Token<String>> getChunks(
      @ApiParam( value = "The text from which to extract chunks", required = true )
      @QueryParam("text") @DefaultValue("") String text) {
    List<Token<String>> chunks = lexicalLib.getChunks(text);
    return chunks;
  }

  /***
   * Extract entities from text. Entities are based on a HMM.
   * 
   * @param text The source text
   * @param callback The name of the JSONP callback function
   * @return A list of entities
   */
  @GET
  @Path("/entities")
  @ApiOperation(value = "Extract entities from text.", response = Token.class,
  responseContainer = "List",
  notes = "The extracted entites are based upon a Hidden Markov Model. This may result in different results that extracting chunks.")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getEntities(
      @ApiParam( value = "The text from which to extract entities", required = true )
      @QueryParam("text") @DefaultValue("") String text) {
    List<Token<String>> chunks = lexicalLib.getEntities(text);
    return chunks;
  }

}
