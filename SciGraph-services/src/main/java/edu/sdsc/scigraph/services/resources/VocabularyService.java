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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singleton;
import static java.util.Collections.sort;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.IntParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.dozer.DozerBeanMapper;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.lucene.ExactAnalyzer;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;
import edu.sdsc.scigraph.services.api.graph.ConceptDTO;
import edu.sdsc.scigraph.services.api.graph.ConceptDTOLite;
import edu.sdsc.scigraph.services.api.vocabulary.Completion;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

@Path("/vocabulary") 
@Api(value = "/vocabulary", description = "Vocabulary services")
@Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP,
    MediaType.APPLICATION_XML, })
public class VocabularyService extends BaseResource {

  private final Vocabulary vocabulary;
  private final DozerBeanMapper mapper;
  private final CurieUtil curieUtil;

  private static final Analyzer analyzer = new ExactAnalyzer();

  private Function<Concept, ConceptDTO> conceptDtoTransformer = new Function<Concept, ConceptDTO>() {

    @Override
    public ConceptDTO apply(Concept input) {
      ConceptDTO dto =  mapper.map(input, ConceptDTO.class);
      Optional<String> curie = curieUtil.getCurie(dto.getUri());
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
      Optional<String> curie = curieUtil.getCurie(dto.getUri());
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
  notes = "Find concepts matching either an IRI or a CURIE.",
      response = Concept.class)
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with ID could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object findById(
      @ApiParam( value = "ID to find", required = true)
      @PathParam("id") String id,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) throws Exception {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Optional<Concept> concept = vocabulary.getConceptFromId(query);
    if (!concept.isPresent()) {
      throw new WebApplicationException(404);
    } else {
      ConceptWrapper wrapper = new ConceptWrapper(transform(singleton(concept.get()), conceptDtoTransformer));
      GenericEntity<ConceptWrapper> response = new GenericEntity<ConceptWrapper>(wrapper){};
      return JaxRsUtil.wrapJsonp(request.get(), response, callback);
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
  response = Concept.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object findByPrefix(
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
      @QueryParam("prefix") List<String> prefixes,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
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
    CompletionWrapper wrapper = new CompletionWrapper(completions);
    GenericEntity<CompletionWrapper> response = new GenericEntity<CompletionWrapper>(wrapper){};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @GET
  @Path("/term/{term}")
  @ApiOperation(value = "Find a concept from a term",
  notes = "Makes a best effort to provide \"exactish\" matching. " +
      "Individual tokens within multi-token labels are not matched" + 
      " (ie: \"foo bar\" would not be returned by a search for \"bar\")."
      + " Results are not guaranteed to be unique.",
      response = Concept.class)
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with term could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object findByTerm(
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
      @QueryParam("prefix") List<String> prefixes,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
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
      ConceptWrapper wrapper = new ConceptWrapper(transform(concepts, conceptDtoTransformer));
      GenericEntity<ConceptWrapper> response = new GenericEntity<ConceptWrapper>(wrapper){};
      return JaxRsUtil.wrapJsonp(request.get(), response, callback);
    }
  }

  @GET
  @Path("/search/{term}")
  @ApiOperation(value = "Find a concept from a term fragment",
  notes = "Searches the complete text of the term. "
      + "Individual tokens within multi-token labels are matched" + 
      " (ie: \"foo bar\" would be returned by a search for \"bar\"). Results are not guaranteed to be unique.",
      response = Concept.class)
  @ApiResponses({
    @ApiResponse(code = 404, message = "Concept with term could not be found")
  })
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object searchByTerm(
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
      @QueryParam("prefix") List<String> prefixes,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
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
      ConceptWrapper wrapper = new ConceptWrapper(transform(concepts, conceptDtoTransformer));
      GenericEntity<ConceptWrapper> response = new GenericEntity<ConceptWrapper>(wrapper){};
      return JaxRsUtil.wrapJsonp(request.get(), response, callback);
    }
  }

  @GET
  @Path("/suggestions/{term}")
  @ApiOperation(value = "Suggest terms",
  notes = "Suggests terms based on a mispelled or mistyped term.",
  response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object suggestFromTerm(
      @ApiParam( value = "Mispelled term", required = true )
      @PathParam("term") String term,
      @ApiParam( value = DocumentationStrings.RESULT_LIMIT_DOC, required = false )
      @QueryParam("limit") @DefaultValue("1") IntParam limit,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    List<String> suggestions = newArrayList(Iterables.limit(vocabulary.getSuggestions(term), limit.get()));
    SuggestionWrapper wrapper = new SuggestionWrapper(suggestions);
    GenericEntity<SuggestionWrapper> response = new GenericEntity<SuggestionWrapper>(wrapper){};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @GET
  @Path("/categories")
  @ApiOperation(value = "Get all categories",
  notes = "Categories can be used to limit results",
  response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getCategories(
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    CategoryWrapper categories = new CategoryWrapper(vocabulary.getAllCategories());
    GenericEntity<CategoryWrapper> response = new GenericEntity<CategoryWrapper>(categories){};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @GET
  @Path("/prefixes")
  @ApiOperation(value = "Get all CURIE prefixes",
  notes = "CURIE prefixes can be used to limit results",
  response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getCuriePrefixes(
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    OntologyWrapper ontologies = new OntologyWrapper(vocabulary.getAllCuriePrefixes());
    GenericEntity<OntologyWrapper> response = new GenericEntity<OntologyWrapper>(ontologies){};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @XmlRootElement(name="concepts")
  private static class ConceptWrapper {
    @XmlElement(name="concept")
    @JsonProperty
    List<ConceptDTO> concepts = new ArrayList<>();

    @SuppressWarnings("unused")
    ConceptWrapper() {}

    ConceptWrapper(Collection<ConceptDTO> items) {
      concepts.addAll(items);
    }
  }

  @XmlRootElement(name="completions")
  private static class CompletionWrapper {
    @XmlElement(name="completion")
    @JsonProperty
    List<Completion> list = new ArrayList<>();

    @SuppressWarnings("unused")
    CompletionWrapper() {}

    CompletionWrapper(Collection<Completion> items) {
      list.addAll(items);
    }
  }

  @XmlRootElement(name="suggestions")
  private static class SuggestionWrapper {
    @XmlElement(name="suggestion")
    @JsonProperty
    List<String> list = new ArrayList<>();

    @SuppressWarnings("unused")
    SuggestionWrapper() {}

    SuggestionWrapper(Collection<String> items) {
      list.addAll(items);
    }
  }

  @XmlRootElement(name="categories")
  private static class CategoryWrapper {
    @XmlElement(name="category")
    @JsonProperty
    List<String> list = new ArrayList<>();

    @SuppressWarnings("unused")
    CategoryWrapper() {}

    CategoryWrapper(Collection<String> items) {
      list.addAll(items);
    }
  }

  @XmlRootElement(name="ontologies")
  private static class OntologyWrapper {
    @XmlElement(name="ontology")
    @JsonProperty
    List<String> list = new ArrayList<>();

    @SuppressWarnings("unused")
    OntologyWrapper() {}

    OntologyWrapper(Collection<String> items) {
      list.addAll(items);
    }
  }

}
