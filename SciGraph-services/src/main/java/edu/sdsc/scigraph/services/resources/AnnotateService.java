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
import io.dropwizard.jersey.caching.CacheControl;

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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;
import edu.sdsc.scigraph.annotation.EntityProcessor;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;

@Path("/annotations")
@Api(value = "/annotations", description = "Annotation services")
@Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP,
    MediaType.APPLICATION_XML })
public class AnnotateService extends BaseResource {

  private static final Logger logger = Logger.getLogger(AnnotateService.class.getName());

  @Inject
  private EntityProcessor processor;

  /***
   * This service is designed to annotate shorter fragments of text and for use from 
   * a browser. It does not have the same options for handling markup that
   * {@link #annotateUrl(String, Set, Set, int, boolean, boolean, boolean, boolean, Set, List, List, Set, Set)}
   * and
   * {@link #annotatePost(String, Set, Set, int, boolean, boolean, boolean, boolean, Set, List, List, Set, Set)
   * have.
   * 
   * @param content The content to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @return The annotated text
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Annotate text", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotate(
      final @QueryParam("content") @DefaultValue("") String content,
      final @QueryParam("includeCat") Set<String> includeCategories,
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      final @QueryParam("minLength") @DefaultValue("4") int minLength,
      final @QueryParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @QueryParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @QueryParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @QueryParam("includeNumbers") @DefaultValue("false") boolean includeNumbers) {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
      WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));

        EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content)).writeTo(writer);
        configBuilder.includeCategories(includeCategories);
        configBuilder.excludeCategories(excludeCategories);
        configBuilder.longestOnly(longestOnly);
        configBuilder.includeAbbreviations(includeAbbrev);
        configBuilder.includeAncronyms(includeAcronym);
        configBuilder.includeNumbers(includeNumbers);
        configBuilder.minLength(minLength);

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

  /***
   * <p>Annotate the content of a URL (presumably a piece of HTML)</p>
   * <p><b>NOTE:</b> this method will add a &lt;base&gt; element to the head
   * 
   * @param url The URL to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @param ignoreTags HTML tags that should be ignored
   * @param stylesheets CSS stylesheets to add to the &lt;head&gt;
   * @param scripts Javascripts to add to the &lt;head&gt;
   * @param targetIds A set of element IDs to annotate
   * @param targetClasses A set of CSS class names to annotate
   * @return The annotated HTML
   */
  @GET
  @Path("/url")
  @Produces(MediaType.TEXT_HTML)
  @ApiOperation(value = "Annotate a URL", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotateUrl(
      final @QueryParam("url") @DefaultValue("") String url,
      final @QueryParam("includeCat") Set<String> includeCategories,
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      final @QueryParam("minLength") @DefaultValue("4") int minLength,
      final @QueryParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @QueryParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @QueryParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @QueryParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      final @QueryParam("ignoreTag") Set<String> ignoreTags,
      final @QueryParam("stylesheet") List<String> stylesheets, 
      final @QueryParam("scripts") List<String> scripts,
      final @QueryParam("targetId") Set<String> targetIds,
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
          configBuilder.longestOnly(longestOnly);
          configBuilder.includeAbbreviations(includeAbbrev);
          configBuilder.includeAncronyms(includeAcronym);
          configBuilder.includeNumbers(includeNumbers);
          configBuilder.minLength(minLength);
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

  /***
   * A POST method for API clients wishing to annotate longer blocks of content. This is most likely the method
   * of choice for most clients.
   * 
   * @param content The content to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @param ignoreTags HTML tags that should be ignored
   * @param stylesheets CSS stylesheets to add to the &lt;head&gt;
   * @param scripts Javascripts to add to the &lt;head&gt;
   * @param targetIds A set of element IDs to annotate
   * @param targetClasses A set of CSS class names to annotate
   * @return The annotated HTML
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes("application/x-www-form-urlencoded")
  @ApiOperation(value = "Annotate text", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Response annotatePost(
      final @FormParam("content") @DefaultValue("") String content,
      final @FormParam("includeCat") Set<String> includeCategories,
      final @FormParam("excludeCat") Set<String> excludeCategories,
      final @FormParam("minLength") @DefaultValue("4") int minLength,
      final @FormParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @FormParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @FormParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @FormParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      final @FormParam("ignoreTag") Set<String> ignoreTags,
      final @FormParam("stylesheet") List<String> stylesheets, 
      final @FormParam("scripts") List<String> scripts,
      final @FormParam("targetId") Set<String> targetIds,
      final @FormParam("targetClass") Set<String> targetClasses) {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
      WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));

        EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content)).writeTo(writer);
        configBuilder.includeCategories(includeCategories);
        configBuilder.excludeCategories(excludeCategories);
        configBuilder.longestOnly(longestOnly);
        configBuilder.includeAbbreviations(includeAbbrev);
        configBuilder.includeAncronyms(includeAcronym);
        configBuilder.includeNumbers(includeNumbers);
        configBuilder.minLength(minLength);
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

  /***
   * Get the entities from content without embedding them
   * in the source.
   * 
   * @param content The content to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @return A list of entities.
   */
  @GET
  @Path("/entities")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
  @ApiOperation(value = "Annotate text", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getEntities(
      @QueryParam("content") @DefaultValue("") String content,
      final @QueryParam("includeCat") Set<String> includeCategories,
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      final @QueryParam("minLength") @DefaultValue("4") int minLength,
      final @QueryParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @QueryParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @QueryParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @QueryParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    List<EntityAnnotation> entities = newArrayList();
    try {
      EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content));
      configBuilder.includeCategories(includeCategories);
      configBuilder.excludeCategories(excludeCategories);
      configBuilder.includeAbbreviations(includeAbbrev);
      configBuilder.includeAncronyms(includeAcronym);
      configBuilder.includeNumbers(includeNumbers);
      configBuilder.minLength(minLength);
      configBuilder.longestOnly(longestOnly);
      entities = processor.annotateEntities(configBuilder.get());
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    GenericEntity<List<EntityAnnotation>> response = new GenericEntity<List<EntityAnnotation>>(entities){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  /***
   * Get the entities from content without embedding them
   * in the source - only the entities are returned.
   * 
   * @param content The content to annotate
   * @param includeCategories A set of categories to include
   * @param excludeCategories A set of categories to exclude
   * @param minLength The minimum length of annotated entities
   * @param longestOnly Should only the longest entity be returned for an overlapping group
   * @param includeAbbrev Should abbreviations be included
   * @param includeAcronym Should acronyms be included
   * @param includeNumbers Should numbers be included
   * @return A list of entities.
   */
  @POST
  @Path("/entities")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
  @ApiOperation(value = "Annotate text", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object postEntities(
      @FormParam("content") @DefaultValue("") String content,
      final @FormParam("includeCat") Set<String> includeCategories,
      final @FormParam("excludeCat") Set<String> excludeCategories,
      final @FormParam("minLength") @DefaultValue("4") int minLength,
      final @FormParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @FormParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @FormParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @FormParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      @FormParam("callback") @DefaultValue("fn") String callback) {
    return getEntities(content, includeCategories, excludeCategories, minLength, 
        longestOnly, includeAbbrev, includeAcronym, includeNumbers, callback);
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
  @GET
  @Path("/complete")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
  @ApiOperation(value = "Annotate text", response = Annotations.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getEntitiesAndContent(
      @QueryParam("content") @DefaultValue("") String content,
      final @QueryParam("includeCat") Set<String> includeCategories,
      final @QueryParam("excludeCat") Set<String> excludeCategories,
      final @QueryParam("minLength") @DefaultValue("4") int minLength,
      final @QueryParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @QueryParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @QueryParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @QueryParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      @QueryParam("callback") @DefaultValue("fn") String callback) throws IOException {
    Annotations annotation = new Annotations();
    StringWriter writer = new StringWriter();
    EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content));
    configBuilder.includeCategories(includeCategories);
    configBuilder.excludeCategories(excludeCategories);
    configBuilder.includeAbbreviations(includeAbbrev);
    configBuilder.includeAncronyms(includeAcronym);
    configBuilder.includeNumbers(includeNumbers);
    configBuilder.minLength(minLength);
    configBuilder.longestOnly(longestOnly);
    configBuilder.writeTo(writer);
    annotation.delegate = processor.annotateEntities(configBuilder.get());
    annotation.content = writer.toString();

    GenericEntity<Annotations> response = new GenericEntity<Annotations>(annotation){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
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
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP })
  @ApiOperation(value = "Annotate text", response = Annotations.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object postEntitiesAndContent(
      @FormParam("content") @DefaultValue("") String content,
      final @FormParam("includeCat") Set<String> includeCategories,
      final @FormParam("excludeCat") Set<String> excludeCategories,
      final @FormParam("minLength") @DefaultValue("4") int minLength,
      final @FormParam("longestOnly") @DefaultValue("false") boolean longestOnly,
      final @FormParam("includeAbbrev") @DefaultValue("false") boolean includeAbbrev,
      final @FormParam("includeAcronym") @DefaultValue("false") boolean includeAcronym,
      final @FormParam("includeNumbers") @DefaultValue("false") boolean includeNumbers,
      @FormParam("callback") @DefaultValue("fn") String callback) throws IOException {
    Annotations annotation = new Annotations();
    StringWriter writer = new StringWriter();
    EntityFormatConfiguration.Builder configBuilder = new EntityFormatConfiguration.Builder(new StringReader(content));
    configBuilder.includeCategories(includeCategories);
    configBuilder.excludeCategories(excludeCategories);
    configBuilder.includeAbbreviations(includeAbbrev);
    configBuilder.includeAncronyms(includeAcronym);
    configBuilder.includeNumbers(includeNumbers);
    configBuilder.minLength(minLength);
    configBuilder.longestOnly(longestOnly);
    configBuilder.writeTo(writer);
    annotation.delegate = processor.annotateEntities(configBuilder.get());
    annotation.content = writer.toString();

    GenericEntity<Annotations> response = new GenericEntity<Annotations>(annotation){};
    return JaxRsUtil.wrapJsonp(request, response, callback); 
  }

  /***
   * A utility JAXB class for {@link AnnotateService#getEntitiesAndContent(String, Set, Set, int, boolean, boolean, boolean, boolean, String)}.
   */
  @XmlRootElement
  static class Annotations /*extends ForwardingList<EntityAnnotation>*/ {

    @XmlElement(name="entity")
    @XmlElementWrapper(name="entities")
    List<EntityAnnotation> delegate;

    @XmlElement
    String content;

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    /*@Override
    protected List<EntityAnnotation> delegate() {
      return delegate;
    }*/
    
    @JsonProperty
    String getContent() {
      return content;
    }

  }

}
