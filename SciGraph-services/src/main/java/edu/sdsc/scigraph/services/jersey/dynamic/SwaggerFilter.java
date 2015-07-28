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
package edu.sdsc.scigraph.services.jersey.dynamic;

import io.dropwizard.jackson.Jackson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import edu.sdsc.scigraph.services.swagger.beans.api.Apis;
import edu.sdsc.scigraph.services.swagger.beans.api.Swagger;

public class SwaggerFilter implements Filter {

  private static final ObjectMapper YAML_MAPPER = Jackson.newObjectMapper(new YAMLFactory());
  private static final ObjectMapper JSON_MAPPER= Jackson.newObjectMapper();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  static byte[] writeDynamicResource(InputStream is) throws IOException {
    Swagger swagger = YAML_MAPPER.readValue(is, Swagger.class);
    Apis api = new Apis();
    api.setDescription("Dynamic Cypher resources");
    api.setPath("/dynamic");
    swagger.getApis().add(api);
    return JSON_MAPPER.writeValueAsBytes(swagger);
  }

  static boolean isGzip(ServletRequest request) {
    String encoding = ((HttpServletRequest) request).getHeader("Accept-Encoding");
    return null != encoding && encoding.contains("gzip");
  }
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // Capture the output of the filter chain
    ByteArrayResponseWrapper wrappedResp = new ByteArrayResponseWrapper((HttpServletResponse) response);
    chain.doFilter(request, wrappedResp);
    if (isGzip(request)) {
      try (InputStream is = new ByteArrayInputStream(wrappedResp.getBytes());
          GZIPInputStream gis = new GZIPInputStream(is);
          ByteArrayOutputStream bs = new ByteArrayOutputStream();
          GZIPOutputStream gzos = new GZIPOutputStream(bs)) {
        byte[] newApi = writeDynamicResource(gis);
        gzos.write(newApi);
        gzos.close();
        byte[] output = bs.toByteArray();
        response.setContentLength(output.length);
        response.getOutputStream().write(output);
      }
    } else {
      try (InputStream is = new ByteArrayInputStream(wrappedResp.getBytes());
          ByteArrayOutputStream bs = new ByteArrayOutputStream()) {
        byte[] newApi = writeDynamicResource(is);
        response.setContentLength(newApi.length);
        response.getOutputStream().write(newApi);
      }
      
    }
  }

  @Override
  public void destroy() {}

}
