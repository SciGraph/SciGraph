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
package io.scigraph.services.jersey;

import javax.ws.rs.core.MediaType;

/***
 * Custom media types that Jersey doesn't provide
 */
public class CustomMediaTypes {

  public final static String APPLICATION_JSONP = "application/javascript";
  public final static MediaType APPLICATION_JSONP_TYPE = new MediaType("application", "javascript");

  public final static String TEXT_CSV = "text/csv";
  public final static MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

  public final static String TEXT_TSV = "text/tab-separated-values";
  public final static MediaType TEXT_TSV_TYPE = new MediaType("text", "tab-separated-values");

  public final static String APPLICATION_RIS = "application/x-research-info-systems";
  public final static MediaType APPLICATION_RIS_TYPE = new MediaType("application", "x-research-info-systems");

  // Graph Media Types
  public final static String APPLICATION_GRAPHML = "application/graphml+xml";
  public final static MediaType APPLICATION_GRAPHML_TYPE = new MediaType("application", "graphml+xml");

  public final static String APPLICATION_GRAPHSON = "application/graphson";
  public final static MediaType APPLICATION_GRAPHSON_TYPE = new MediaType("application", "graphson");

  public final static String APPLICATION_XGMML = "application/xgmml";
  public final static MediaType APPLICATION_XGMML_TYPE = new MediaType("application", "xgmml");
  
  public final static String TEXT_GML = "text/gml";
  public final static MediaType TEXT_GML_TYPE = new MediaType("text", "gml");

  public final static String IMAGE_JPEG = "image/jpeg";
  public final static MediaType IMAGE_JPEG_TYPE = new MediaType("image", "jpeg");

  public final static String IMAGE_PNG = "image/png";
  public final static MediaType IMAGE_PNG_TYPE = new MediaType("image", "png");

}
