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
package io.scigraph.services.auth;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.enumeration;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.codec.binary.Base64;

import com.google.common.net.HttpHeaders;

/***
 * <p>A filter to set basic authentication headers from a request parameter.
 * <p>Allows a client to pass in an API key as a request parameter which gets
 * translated into the user name and password for basic authentication. The FilterConfig
 * can be populated with:
 * <ul>
 * <li><b>KEY_REQUEST_PARAM</b>: the name of the API key request parameter
 * <li><b>DEFAULT_API_KEY</b>: the default API key to use if not specified in the request
 * </ul>
 * <p>If DEFAULT_API_KEY is not set then authentication will be required.
 */
public class BasicAuthFilter implements Filter {

  public static final String KEY_REQUEST_PARAM = "keyParameter";
  public static final String DEFAULT_API_KEY = "defaultApiKey";

  private static String keyParam = "apikey";
  private static Optional<String> defaultApiKey = Optional.empty();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    keyParam = filterConfig.getInitParameter(KEY_REQUEST_PARAM);
    if (!isNullOrEmpty(filterConfig.getInitParameter(DEFAULT_API_KEY))) {
      defaultApiKey = Optional.of(filterConfig.getInitParameter(DEFAULT_API_KEY));
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    chain.doFilter(new FilteredRequest((HttpServletRequest) request), response);
  }

  @Override
  public void destroy() {}

  static class FilteredRequest extends HttpServletRequestWrapper {

    public FilteredRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getHeader(String name) {
      if (HttpHeaders.AUTHORIZATION.equals(name)) {
        String authKey = null;
        if (!isNullOrEmpty(getRequest().getParameter(keyParam))) {
          authKey = format("%1$s:%1$s", getRequest().getParameter(keyParam));
        } else if (defaultApiKey.isPresent()) {
          authKey = format("%1$s:%1$s", defaultApiKey.get());
        } else {
          return null;
        }
        return format("Basic %s", Base64.encodeBase64String(authKey.getBytes()));
      } else {
        return super.getHeader(name);
      }
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (HttpHeaders.AUTHORIZATION.equals(name)) {
        return enumeration(newHashSet(getHeader(name)));
      } else {
        return super.getHeaders(name);
      }
    }

  }

}
