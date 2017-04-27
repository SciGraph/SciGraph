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
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.IntParam;
import io.scigraph.annotation.EntityAnnotation;
import io.scigraph.annotation.EntityFormatConfiguration;
import io.scigraph.annotation.EntityProcessor;
import io.scigraph.services.jersey.BaseResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.collect.ForwardingList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Path("/annotations")
@Api(value = "/annotations", description = "Annotation services")
@SwaggerDefinition(tags = {@Tag(name="annotations", description="Annotation services")})
@Produces({ MediaType.APPLICATION_JSON })
public class AnnotateService extends BaseResource {

  private static final Logger logger = Logger.getLogger(AnnotateService.class.getName());

  @Inject
  private EntityProcessor processor;

  @GET
  @Produces("text/plain; charset=utf-8")
  @ApiOperation(value = "Annotate text", response = String.class,
  notes="This service is designed to annotate shorter fragments of text. Use the POST version if your content is too long.")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotate(
      @ApiParam( value = DocumentationStrings.CONTENT_DOC, required = true)
      final @QueryParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @QueryParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @QueryParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @QueryParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @QueryParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @QueryParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers) {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
      WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));

        EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content)).writeTo(writer);
        configBuilder.includeCategories(includeCategories);
        configBuilder.excludeCategories(excludeCategories);
        configBuilder.longestOnly(longestOnly.get());
        configBuilder.includeAbbreviations(includeAbbrev.get());
        configBuilder.includeAncronyms(includeAcronym.get());
        configBuilder.includeNumbers(includeNumbers.get());
        configBuilder.minLength(minLength.get());

        try {
          processor.annotateEntities(configBuilder.get());
        } catch (Exception e) {
          logger.log(Level.WARNING, e.getMessage(), e);
          throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        writer.flush();
      }
    };
    return Response.ok(stream).build();
  }

  @GET
  @Path("/url")
  @Produces(MediaType.TEXT_HTML)
  @ApiOperation(value = "Annotate a URL", response = String.class, 
  notes="Annotate the content of a URL (presumably an HTML page). <em>NOTE:</em> this method will add a &lt;base&gt; element to the head.")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotateUrl(
      @ApiParam( value = "", required = true)
      final @QueryParam("url") @DefaultValue("") String url,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @QueryParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @QueryParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @QueryParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @QueryParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @QueryParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers,
      @ApiParam( value = DocumentationStrings.IGNORE_TAGS_DOC, required = false)
      final @QueryParam("ignoreTag") Set<String> ignoreTags,
      @ApiParam( value = DocumentationStrings.STYLESHEETS_DOC, required = false)
      final @QueryParam("stylesheet") List<String> stylesheets,
      @ApiParam( value = DocumentationStrings.SCRIPTS_DOC, required = false)
      final @QueryParam("scripts") List<String> scripts,
      @ApiParam( value = DocumentationStrings.TARGET_IDS_DOC, required = false)
      final @QueryParam("targetId") Set<String> targetIds,
      @ApiParam( value = DocumentationStrings.CSS_CLASS_DOCS, required = false)
      final @QueryParam("targetClass") Set<String> targetClasses) {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
      WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8));

        URL source = new URL(url);
        try (Reader reader = new BufferedReader(new InputStreamReader(source.openStream(), Charsets.UTF_8))) {
          EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(reader).writeTo(writer);
          configBuilder.includeCategories(includeCategories);
          configBuilder.excludeCategories(excludeCategories);
          configBuilder.longestOnly(longestOnly.get());
          configBuilder.includeAbbreviations(includeAbbrev.get());
          configBuilder.includeAncronyms(includeAcronym.get());
          configBuilder.includeNumbers(includeNumbers.get());
          configBuilder.minLength(minLength.get());
          if (!ignoreTags.isEmpty()) {
            configBuilder.ignoreTags(ignoreTags);
          }
          configBuilder.stylesheets(stylesheets);
          configBuilder.scripts(scripts);
          configBuilder.url(source);
          configBuilder.targetIds(targetIds);
          configBuilder.targetClasses(targetClasses);

          try {
            processor.annotateEntities(configBuilder.get());
            reader.close();
          } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
          }
          writer.flush();
        }
      }
    };
    return Response.ok(stream).build();
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  @ApiOperation(value = "Annotate text", response = String.class, 
  notes = "A POST resource for API clients wishing to annotate longer content. This is most likely the method of choice for most clients. "
      + DocumentationStrings.REST_ABUSE_DOC
      )
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotatePost(
      @ApiParam(value = DocumentationStrings.CONTENT_DOC, required = true)
      final @FormParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @FormParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @FormParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @FormParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @FormParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @FormParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers,
      @ApiParam( value = DocumentationStrings.IGNORE_TAGS_DOC, required = false)
      final @FormParam("ignoreTag") Set<String> ignoreTags,
      @ApiParam( value = DocumentationStrings.STYLESHEETS_DOC, required = false)
      final @FormParam("stylesheet") List<String> stylesheets,
      @ApiParam( value = DocumentationStrings.SCRIPTS_DOC, required = false)
      final @FormParam("scripts") List<String> scripts,
      @ApiParam( value = DocumentationStrings.TARGET_IDS_DOC, required = false)
      final @FormParam("targetId") Set<String> targetIds,
      @ApiParam( value = DocumentationStrings.CSS_CLASS_DOCS, required = false)
      final @FormParam("targetClass") Set<String> targetClasses) {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
      WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8));

        EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content)).writeTo(writer);
        configBuilder.includeCategories(includeCategories);
        configBuilder.excludeCategories(excludeCategories);
        configBuilder.longestOnly(longestOnly.get());
        configBuilder.includeAbbreviations(includeAbbrev.get());
        configBuilder.includeAncronyms(includeAcronym.get());
        configBuilder.includeNumbers(includeNumbers.get());
        configBuilder.minLength(minLength.get());
        if (!ignoreTags.isEmpty()) {
          configBuilder.ignoreTags(ignoreTags);
        }
        configBuilder.stylesheets(stylesheets);
        configBuilder.scripts(scripts);
        configBuilder.targetIds(targetIds);
        configBuilder.targetClasses(targetClasses);

        try {
          processor.annotateEntities(configBuilder.get());
        } catch (Exception e) {
          logger.log(Level.WARNING, e.getMessage(), e);
          throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        writer.flush();
      }
    };
    return Response.ok(stream).build();
  }

  @GET
  @Path("/entities")
  @ApiOperation(value = "Get entities from text",
  response = EntityAnnotation.class,
  responseContainer = "List",
  notes="Get entities from content without embedding them in the source.")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public List<EntityAnnotation> getEntities(
      @ApiParam( value = DocumentationStrings.CONTENT_DOC, required = true)
      final @QueryParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @QueryParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @QueryParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @QueryParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @QueryParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @QueryParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers) {
    List<EntityAnnotation> entities = newArrayList();
    try {
      EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content));
      configBuilder.includeCategories(includeCategories);
      configBuilder.excludeCategories(excludeCategories);
      configBuilder.includeAbbreviations(includeAbbrev.get());
      configBuilder.includeAncronyms(includeAcronym.get());
      configBuilder.includeNumbers(includeNumbers.get());
      configBuilder.minLength(minLength.get());
      configBuilder.longestOnly(longestOnly.get());
      entities = processor.annotateEntities(configBuilder.get());
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return entities;
  }

  @POST
  @Path("/entities")
  @Consumes("application/x-www-form-urlencoded")
  @ApiOperation(value = "Get entities from text", 
  response = EntityAnnotation.class, 
  responseContainer = "List",
  notes = "Get the entities from content without embedding them in the source - only the entities are returned. " +
      DocumentationStrings.REST_ABUSE_DOC)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object postEntities(
      @ApiParam( value = DocumentationStrings.CONTENT_DOC, required = true)
      final @FormParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @FormParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @FormParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @FormParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @FormParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @FormParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers) {
    return getEntities(content, includeCategories, excludeCategories, minLength, 
        longestOnly, includeAbbrev, includeAcronym, includeNumbers);
  }

  /*** 
   * @return A list of entities and the annotated content
   */
  @GET
  @Path("/complete")
  @Consumes("application/x-www-form-urlencoded")
  @ApiOperation(value = "Get embedded annotations as well as a separate list", 
  response = Annotations.class, 
  notes="A convenience resource for retrieving both a list of entities and annotated content")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Annotations getEntitiesAndContent(
      @ApiParam( value = DocumentationStrings.CONTENT_DOC, required = true)
      final @QueryParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @QueryParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @QueryParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @QueryParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @QueryParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @QueryParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers
      ) throws IOException {
    Annotations annotations = new Annotations();
    StringWriter writer = new StringWriter();
    EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content));
    configBuilder.includeCategories(includeCategories);
    configBuilder.excludeCategories(excludeCategories);
    configBuilder.includeAbbreviations(includeAbbrev.get());
    configBuilder.includeAncronyms(includeAcronym.get());
    configBuilder.includeNumbers(includeNumbers.get());
    configBuilder.minLength(minLength.get());
    configBuilder.longestOnly(longestOnly.get());
    configBuilder.writeTo(writer);
    annotations.delegate = processor.annotateEntities(configBuilder.get());
    annotations.content = writer.toString();

    return annotations;
  }


  /***
   * A convenience call for retrieving both a list of entities and annotated content
   * 
   * @param content The content to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @return A list of entities and the annotated content
   * @throws IOException 
   */
  @POST
  @Path("/complete")
  @Consumes("application/x-www-form-urlencoded")
  @ApiOperation(value = "Get embedded annotations as well as a separate list", 
  response = Annotations.class, 
  notes="A convenience resource for retrieving both a list of entities and annotated content. " + DocumentationStrings.REST_ABUSE_DOC)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Annotations postEntitiesAndContent(
      @ApiParam( value = DocumentationStrings.CONTENT_DOC, required = true)
      final @FormParam("content") @DefaultValue("") String content,
      @ApiParam( value = DocumentationStrings.INCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("includeCat") Set<String> includeCategories,
      @ApiParam( value = DocumentationStrings.EXCLUDE_CATEGORIES_DOC, required = false)
      final @FormParam("excludeCat") Set<String> excludeCategories,
      @ApiParam( value = DocumentationStrings.MINIMUM_LENGTH_DOC, required = false)
      final @FormParam("minLength") @DefaultValue("4") IntParam minLength,
      @ApiParam( value = DocumentationStrings.LONGEST_ENTITY_DOC, required = false)
      final @FormParam("longestOnly") @DefaultValue("false") BooleanParam longestOnly,
      @ApiParam( value = DocumentationStrings.INCLUDE_ABBREV_DOC, required = false)
      final @FormParam("includeAbbrev") @DefaultValue("false") BooleanParam includeAbbrev,
      @ApiParam( value = DocumentationStrings.INCLUDE_ACRONYMS_DOC, required = false)
      final @FormParam("includeAcronym") @DefaultValue("false") BooleanParam includeAcronym,
      @ApiParam( value = DocumentationStrings.INCLUDE_NUMBERS_DOC, required = false)
      final @FormParam("includeNumbers") @DefaultValue("false") BooleanParam includeNumbers) throws IOException {
    return this.getEntitiesAndContent(content, includeCategories, excludeCategories, minLength, longestOnly, includeAbbrev, includeAcronym, includeNumbers);
  }

  /***
   * A utility class for {@link AnnotateService#getEntitiesAndContent(String, Set, Set, int, boolean, boolean, boolean, boolean, String)}.
   */
  static class Annotations extends ForwardingList<EntityAnnotation> {

    List<EntityAnnotation> delegate;

    String content;

    @Override
    protected List<EntityAnnotation> delegate() {
      return delegate;
    }

    @Override
    public boolean equals(Object obj) {
      return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    @JsonProperty
    String getContent() {
      return content;
    }

  }

}
