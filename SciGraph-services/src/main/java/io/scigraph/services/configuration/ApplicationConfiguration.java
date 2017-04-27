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
package io.scigraph.services.configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.Configuration;
import io.scigraph.neo4j.Neo4jConfiguration;
import io.scigraph.services.refine.ServiceMetadata;

import java.io.IOException;
import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Method;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.SwaggerVersion;
import io.swagger.models.apideclaration.Api;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.apideclaration.Operation;
import io.swagger.models.resourcelisting.ApiListingReference;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.parser.util.SwaggerDeserializer;
import io.swagger.report.MessageBuilder;
import io.swagger.transform.migrate.ApiDeclarationMigrator;
import io.swagger.util.Json;

public class ApplicationConfiguration extends Configuration {

  @Valid
  @JsonProperty
  private String applicationContextPath;

  @Valid
  @NotNull
  @JsonProperty
  private Neo4jConfiguration graphConfiguration = new Neo4jConfiguration();

  @Valid
  @JsonProperty(required=false)
  private Optional<ApiConfiguration> apiConfiguration = Optional.empty();

  @Valid
  @JsonProperty(required=false)
  private Optional<ServiceMetadata> serviceMetadata = Optional.empty();

  @Valid
  @JsonProperty(required=false)
  @JsonDeserialize(using=CypherResourcesDeserializer.class)
  private Map<String,Path> cypherResources = new HashMap<>();
  
  public String getApplicationContextPath() {
    return applicationContextPath;
  }

  public Neo4jConfiguration getGraphConfiguration() {
    return graphConfiguration;
  }

  public Optional<ApiConfiguration> getApiConfiguration() {
    return apiConfiguration;
  }

  public Optional<ServiceMetadata> getServiceMetadata() {
    return serviceMetadata;
  }
  
  public Map<String,Path> getCypherResources() {
    return cypherResources;
  }

  public static class CypherResourcesDeserializer extends StdDeserializer<Map<String,Path>> {
    public CypherResourcesDeserializer() {
      this(null);
    }
    public CypherResourcesDeserializer(Class<?> vc) {
      super(vc);
    }
    @Override
    public Map<String,Path> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      JsonNode paths = mapper.readTree(jp);

      // First try Swagger 2.0
      ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
      node.put("swagger", "2.0");
      ObjectNode info = new ObjectNode(JsonNodeFactory.instance);
      info.put("title", "cypherResources");
      info.put("version", "1.0");
      node.set("info", info);
      node.set("paths", paths);
      SwaggerDeserializationResult result = new SwaggerDeserializer().deserialize(node);
      if (result.getMessages().isEmpty()) {
        // add dynamic tag
        Map<String,Path> pathsMap = result.getSwagger().getPaths();
        for (Map.Entry<String,Path> entry : pathsMap.entrySet()) {
          for (io.swagger.models.Operation op : entry.getValue().getOperations()) {
            op.addTag("dynamic");
          }
        }
        return pathsMap;
      }

      // Then try to convert Swagger 1.0 to 2.0
      ResourceListing resourceListing = new ResourceListing();
      resourceListing.setSwaggerVersion(SwaggerVersion.V1_2);
      ApiListingReference alr = new ApiListingReference();
      alr.setDescription("foo");
      alr.setPath("/foo");
      List<ApiListingReference> alrs = new ArrayList<>();
      alrs.add(alr);
      resourceListing.setApis(alrs);
      ObjectNode apis = new ObjectNode(JsonNodeFactory.instance);
      apis.put("swaggerVersion", "1.2");
      apis.put("basePath", "/");
      apis.set("apis", paths);
      ApiDeclarationMigrator migrator = new ApiDeclarationMigrator();
      MessageBuilder messageBuilder = new MessageBuilder();
      JsonNode transformed = migrator.migrate(messageBuilder, apis);
      if (messageBuilder.toString().isEmpty()) {
        Map<String,ObjectNode> extraFields = new HashMap<>();
        ApiDeclaration output = Json.mapper().convertValue(transformed, ApiDeclaration.class);
        // make sure the operation method is set
        for (Api api : output.getApis()) {
          for (Operation op : api.getOperations()) {
            if (op.getMethod() == null) {
              op.setMethod(Method.GET);
            }
          }
          extraFields.put(api.getPath(), api.getExtraFields());
        }
        List<ApiDeclaration> apiList = new ArrayList<>();
        apiList.add(output);
        try {
          Swagger swagger = new SwaggerCompatConverter().convert(resourceListing, apiList);
          Map<String,Path> pathMap = swagger.getPaths();
          // transfer the extraFields to vendor extensions
          for (Map.Entry<String,Path> entry : pathMap.entrySet()) {
            String pathName = entry.getKey();
            Path path = entry.getValue();
            ObjectNode extraField = extraFields.get(pathName);
            if (extraField != null) {
                for (Iterator<String> i = extraField.fieldNames(); i.hasNext();) {
                    String fieldName = i.next();
                    JsonNode field = extraField.get(fieldName);
                    if (field.isValueNode()) {
                      path.setVendorExtension("x-"+fieldName, field.asText());
                    }
                    else {
                      path.setVendorExtension("x-"+fieldName, field);
                    }
                }
            }
          }
          // add dynamic tag
          for (Map.Entry<String,Path> entry : pathMap.entrySet()) {
            for (io.swagger.models.Operation op : entry.getValue().getOperations()) {
              op.addTag("dynamic");
            }
          }
          return pathMap;
        } catch (Throwable e) {
          e.printStackTrace();
          throw e;
        }
      }
      throw new IOException(String.format("Could not parse cypherResources in configuration: '%s': %s%s",
              paths.toString(), result.getMessages(), messageBuilder.toString()));
    }
  }
}
