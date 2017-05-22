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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.dozer.DozerBeanMapper;
import org.prefixcommons.CurieUtil;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.prefixcommons.CurieUtil;

import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.IntParam;
import io.scigraph.frames.Concept;
import io.scigraph.lucene.ExactAnalyzer;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.services.api.graph.ConceptDTO;
import io.scigraph.services.api.graph.ConceptDTOLite;
import io.scigraph.services.api.vocabulary.Completion;
import io.scigraph.services.jersey.BaseResource;
import io.scigraph.vocabulary.Vocabulary;
import io.scigraph.vocabulary.Vocabulary.Query;

@Path("/vocabulary") 
@Api(value = "/vocabulary", description = "Vocabulary services")
@SwaggerDefinition(tags = {@Tag(name="vocabulary", description="Vocabulary services")})
@Produces({ MediaType.APPLICATION_JSON })
public class VocabularyService extends BaseResource {

  private final Vocabulary vocabulary;
  private final DozerBeanMapper mapper;
  private final CurieUtil curieUtil;

  private static final Analyzer analyzer = new ExactAnalyzer();

  private Function<Concept, ConceptDTO> conceptDtoTransformer = new Function<Concept, ConceptDTO>() {

    @Override
    public ConceptDTO apply(Concept input) {
      ConceptDTO dto =  mapper.map(input, ConceptDTO.class);
      Optional<String> curie = curieUtil.getCurie(dto.getIri());
      if (curie.isPresent()) {
        dto.setCurie(curie.get());
      }
      return dto;
    }

  };

  private Function<Concept, ConceptDTOLite> conceptDtoLiteTransformer = new Function<Concept, ConceptDTOLite>() {

    @Override
    public ConceptDTOLite apply(Concept input) {
      ConceptDTOLite dto =  mapper.map(input, ConceptDTOLite.class);
      Optional<String> curie = curieUtil.getCurie(dto.getIri());
      if (curie.isPresent()) {
        dto.setCurie(curie.get());
      }
      return dto;
    }

  };

  @Inject
  VocabularyService(Vocabulary vocabulary, DozerBeanMapper mapper, CurieUtil curieUtil) {
    this.vocabulary = vocabulary;
    this.mapper = mapper;
    this.curieUtil = curieUtil;
  }

  @GET
  @Path("/id/{id}")
  @ApiOperation(value = "Find a concept by its ID",
  notes = "Find concepts that match either a IRI or a CURIE. ",
  response = ConceptDTO.class)
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with ID could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public ConceptDTO findById(
      @ApiParam( value = "ID to find", required = true)
      @PathParam("id") String id) throws Exception {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Optional<Concept> concept = vocabulary.getConceptFromId(query);
    if (!concept.isPresent()) {
      throw new WebApplicationException(404);
    } else {
      ConceptDTO dto = conceptDtoTransformer.apply(concept.get());
      return dto;
    }
  }

  static Set<String> getMatchingCompletions(String prefix, Iterable<String> candidates) {
    Set<String> matches = new HashSet<>();

    String tokenizedPrefix = LuceneUtils.getTokenization(analyzer, prefix);

    for (String candidate: candidates) {
      String tokenizedCandidate = LuceneUtils.getTokenization(analyzer, candidate);
      if (StringUtils.startsWithIgnoreCase(tokenizedCandidate, tokenizedPrefix)) {
        matches.add(candidate);
      }
    }
    return matches;
  }

  static List<String> getCompletion(Query query, Concept result) {
    List<String> completions = new ArrayList<>();
    completions.addAll(getMatchingCompletions(query.getInput(), result.getLabels()));
    if (query.isIncludeSynonyms()) {
      completions.addAll(getMatchingCompletions(query.getInput(), result.getSynonyms()));
    }
    return completions;
  }

  List<Completion> getCompletions(Query query, List<Concept> concepts) {
    List<Completion> completions = new ArrayList<>();
    for (Concept concept : concepts) {
      for (String completion : getMatchingCompletions(query.getInput(), concept.getLabels())) {
        completions.add(new Completion(completion, "label", conceptDtoLiteTransformer
            .apply(concept)));
      }
      if (query.isIncludeSynonyms()) {
        for (String completion : getMatchingCompletions(query.getInput(), concept.getSynonyms())) {
          completions.add(new Completion(completion, "synonym", conceptDtoLiteTransformer
              .apply(concept)));
        }
      }
      if (query.isIncludeAbbreviations()) {
        for (String completion : getMatchingCompletions(query.getInput(), concept.getAbbreviations())) {
          completions.add(new Completion(completion, "abbreviation", conceptDtoLiteTransformer
              .apply(concept)));
        }
      }
      if (query.isIncludeAcronyms()) {
        for (String completion : getMatchingCompletions(query.getInput(), concept.getAcronyms())) {
          completions.add(new Completion(completion, "acronym", conceptDtoLiteTransformer
              .apply(concept)));
        }
      }
    }
    sort(completions);
    return completions;
  }

  @GET
  @Path("/autocomplete/{term}")
  @ApiOperation(value = "Find a concept by its prefix",
  notes = "This resource is designed for autocomplete services.",
  response = Completion.class,
  responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public List<Completion> findByPrefix(
      @ApiParam( value = "Term prefix to find", required = true )
      @PathParam("term") String termPrefix,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("20") IntParam limit,
      @ApiParam( value = DocumentationStrings.SEARCH_SYNONYMS, required = false )
      @QueryParam("searchSynonyms") @DefaultValue("true") BooleanParam searchSynonyms,
      @ApiParam( value = DocumentationStrings.SEARCH_ABBREVIATIONS, required = false )
      @QueryParam("searchAbbreviations") @DefaultValue("false") BooleanParam searchAbbreviations,
      @ApiParam( value = DocumentationStrings.SEARCH_ACRONYMS, required = false )
      @QueryParam("searchAcronyms") @DefaultValue("false") BooleanParam searchAcronyms,
      @ApiParam( value = DocumentationStrings.INCLUDE_DEPRECATED_CLASSES, required = false )
      @QueryParam("includeDeprecated") @DefaultValue("false") BooleanParam includeDeprecated,
      @ApiParam( value = "Categories to search (defaults to all)", required = false )
      @QueryParam("category") List<String> categories,
      @ApiParam( value = "CURIE prefixes to search (defaults to all)", required = false )
      @QueryParam("prefix") List<String> prefixes) {
    Vocabulary.Query.Builder builder = new Vocabulary.Query.Builder(termPrefix).
        categories(categories).
        prefixes(prefixes).
        includeDeprecated(includeDeprecated.get()).
        includeSynonyms(searchSynonyms.get()).
        includeAbbreviations(searchAbbreviations.get()).
        includeAcronyms(searchAcronyms.get()).
        limit(1000);
    List<Concept> concepts = vocabulary.getConceptsFromPrefix(builder.build());
    List<Completion> completions = getCompletions(builder.build(), concepts);
    // TODO: Move completions to scigraph-core for #51
    sort(completions);
    int endIndex = limit.get() > completions.size() ? completions.size() : limit.get();
    completions = completions.subList(0, endIndex);
    return completions;
  }

  @GET
  @Path("/term/{term}")
  @ApiOperation(value = "Find a concept from a term",
  notes = "Makes a best effort to provide \"exactish\" matching. " +
      "Individual tokens within multi-token labels are not matched" + 
      " (ie: \"foo bar\" would not be returned by a search for \"bar\")."
      + " Results are not guaranteed to be unique.",
      response = Concept.class,
      responseContainer="List")
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with term could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public List<ConceptDTO> findByTerm(
      @ApiParam( value = "Term to find", required = true )
      @PathParam("term") String term,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("20") IntParam limit,
      @ApiParam( value = DocumentationStrings.SEARCH_SYNONYMS, required = false )
      @QueryParam("searchSynonyms") @DefaultValue("true") BooleanParam searchSynonyms,
      @ApiParam( value = DocumentationStrings.SEARCH_ABBREVIATIONS, required = false )
      @QueryParam("searchAbbreviations") @DefaultValue("false") BooleanParam searchAbbreviations,
      @ApiParam( value = DocumentationStrings.SEARCH_ACRONYMS, required = false )
      @QueryParam("searchAcronyms") @DefaultValue("false") BooleanParam searchAcronyms,
      @ApiParam( value = "Categories to search (defaults to all)", required = false )
      @QueryParam("category") List<String> categories,
      @ApiParam( value = "CURIE prefixes to search (defaults to all)", required = false )
      @QueryParam("prefix") List<String> prefixes) {
    Vocabulary.Query.Builder builder = new Vocabulary.Query.Builder(term).
        categories(categories).
        prefixes(prefixes).
        includeSynonyms(searchSynonyms.get()).
        includeAbbreviations(searchAbbreviations.get()).
        includeAcronyms(searchAcronyms.get()).
        limit(limit.get());
    List<Concept> concepts = vocabulary.getConceptsFromTerm(builder.build());
    if (concepts.isEmpty()) {
      throw new WebApplicationException(404);
    } else {
      List<ConceptDTO> dtos = transform(concepts, conceptDtoTransformer);
      return dtos;
    }
  }

  @GET
  @Path("/search/{term}")
  @ApiOperation(value = "Find a concept from a term fragment",
  notes = "Searches the complete text of the term. "
      + "Individual tokens within multi-token labels are matched" + 
      " (ie: \"foo bar\" would be returned by a search for \"bar\"). Results are not guaranteed to be unique.",
      response = ConceptDTO.class,
      responseContainer= "List")
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with term could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public List<ConceptDTO> searchByTerm(
      @ApiParam( value = "Term to find", required = true )
      @PathParam("term") String term,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("20") IntParam limit,
      @ApiParam( value = DocumentationStrings.SEARCH_SYNONYMS, required = false )
      @QueryParam("searchSynonyms") @DefaultValue("true") BooleanParam searchSynonyms,
      @ApiParam( value = DocumentationStrings.SEARCH_ABBREVIATIONS, required = false )
      @QueryParam("searchAbbreviations") @DefaultValue("false") BooleanParam searchAbbreviations,
      @ApiParam( value = DocumentationStrings.SEARCH_ACRONYMS, required = false )
      @QueryParam("searchAcronyms") @DefaultValue("false") BooleanParam searchAcronyms,
      @ApiParam( value = "Categories to search (defaults to all)", required = false )
      @QueryParam("category") List<String> categories,
      @ApiParam( value = "CURIE prefixes to search (defaults to all)", required = false )
      @QueryParam("prefix") List<String> prefixes) {
    Vocabulary.Query.Builder builder = new Vocabulary.Query.Builder(term).
        categories(categories).
        prefixes(prefixes).
        includeSynonyms(searchSynonyms.get()).
        includeAbbreviations(searchAbbreviations.get()).
        includeAcronyms(searchAcronyms.get()).
        limit(limit.get());
    List<Concept> concepts = vocabulary.searchConcepts(builder.build());
    if (concepts.isEmpty()) {
      throw new WebApplicationException(404);
    } else {
      List<ConceptDTO> dtos = transform(concepts, conceptDtoTransformer);
      return dtos;
    }
  }

  @GET
  @Path("/suggestions/{term}")
  @ApiOperation(value = "Suggest terms",
  notes = "Suggests terms based on a mispelled or mistyped term.",
  response = String.class,
  responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm(
      @ApiParam( value = "Mispelled term", required = true )
      @PathParam("term") String term,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("1") IntParam limit) {
    List<String> suggestions = newArrayList(Iterables.limit(vocabulary.getSuggestions(term), limit.get()));
    return suggestions;
  }

  @GET
  @Path("/categories")
  @ApiOperation(value = "Get all categories",
  notes = "Categories can be used to limit results",
  response = String.class,
  responseContainer = "Set")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Set<String> getCategories() {
    Set<String> categories = vocabulary.getAllCategories();
    return categories;
  }

  @GET
  @Path("/prefixes")
  @ApiOperation(value = "Get all CURIE prefixes",
  notes = "CURIE prefixes can be used to limit results",
  response = String.class,
  responseContainer = "Set")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Set<String> getCuriePrefixes() {
    Set<String> prefixes = vocabulary.getAllCuriePrefixes();
    return prefixes;
  }

}
