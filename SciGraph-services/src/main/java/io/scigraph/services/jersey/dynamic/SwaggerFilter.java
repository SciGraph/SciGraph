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
package io.scigraph.services.jersey.dynamic;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;

public class SwaggerFilter implements Filter {
  ApplicationConfiguration configuration;
  @Inject
  public SwaggerFilter(ApplicationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  private byte[] writeDynamicResource(InputStream is) throws IOException {
    String str = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
    Swagger swagger = new SwaggerParser().parse(str);
    // set the resource listing tag
    Tag dynamic = new Tag();
    dynamic.setName("dynamic");
    dynamic.setDescription("Dynamic Cypher resources");
    swagger.addTag(dynamic);
    // add resources to the path
    Map<String,Path> paths = swagger.getPaths();
    paths.putAll(configuration.getCypherResources());
    Map<String,Path> sorted = new LinkedHashMap<>();
    List<String> keys = new ArrayList<>();
    keys.addAll(paths.keySet());
    Collections.sort(keys);
    for (String key : keys) {
      sorted.put(key, paths.get(key));
    }
    swagger.setPaths(sorted);
    // return updated swagger JSON
    return Json.pretty(swagger).getBytes();
  }

  static boolean isGzip(ServletResponse response) {
    String encoding = ((HttpServletResponse) response).getHeader("Accept-Encoding");
    return null != encoding && encoding.contains("gzip");
  }
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // Capture the output of the filter chain
    ByteArrayResponseWrapper wrappedResp = new ByteArrayResponseWrapper((HttpServletResponse) response);
    chain.doFilter(request, wrappedResp);
    if (isGzip(response)) {
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
