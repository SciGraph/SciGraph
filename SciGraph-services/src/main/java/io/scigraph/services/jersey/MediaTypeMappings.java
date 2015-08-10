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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.google.common.collect.ForwardingMap;

public final class MediaTypeMappings extends ForwardingMap<String, MediaType> {

  Map<String, MediaType> delegate = new HashMap<>();

  public MediaTypeMappings() {
    delegate.put("xml", MediaType.APPLICATION_XML_TYPE);
    delegate.put("json", MediaType.APPLICATION_JSON_TYPE);
    delegate.put("jsonp", CustomMediaTypes.APPLICATION_JSONP_TYPE);
    delegate.put("csv", CustomMediaTypes.TEXT_CSV_TYPE);
    delegate.put("tsv", CustomMediaTypes.TEXT_TSV_TYPE);
    delegate.put("ris", CustomMediaTypes.APPLICATION_RIS_TYPE);
    delegate.put("graphson", CustomMediaTypes.APPLICATION_GRAPHSON_TYPE);
    delegate.put("graphml", CustomMediaTypes.APPLICATION_GRAPHML_TYPE);
    delegate.put("gml", CustomMediaTypes.TEXT_GML_TYPE);
    delegate.put("gr", CustomMediaTypes.APPLICATION_XGMML_TYPE);
    delegate.put("jpg", CustomMediaTypes.IMAGE_JPEG_TYPE);
    delegate.put("jpeg", CustomMediaTypes.IMAGE_JPEG_TYPE);
    delegate.put("png", CustomMediaTypes.IMAGE_PNG_TYPE);
  }

  @Override
  protected Map<String, MediaType> delegate() {
    return delegate;
  }

}
