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
package edu.sdsc.scigraph.services.jersey;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.core.PackagesResourceConfig;

/***
 * Tell Jersey how to map URL extensions to media types
 */
public class CommonResourceConfig extends PackagesResourceConfig {

  public CommonResourceConfig(Map<String, Object> props) {
    super(props);
  }

  public CommonResourceConfig(String... packages) {
    super(packages);
  }

  @Override
  public Map<String, MediaType> getMediaTypeMappings() {
    Map<String, MediaType> map = newHashMap();
    map.put("xml", MediaType.APPLICATION_XML_TYPE);
    map.put("json", MediaType.APPLICATION_JSON_TYPE);
    map.put("jsonp", CustomMediaTypes.APPLICATION_JSONP_TYPE);
    map.put("csv", CustomMediaTypes.TEXT_CSV_TYPE);
    map.put("tsv", CustomMediaTypes.TEXT_TSV_TYPE);
    map.put("ris", CustomMediaTypes.APPLICATION_RIS_TYPE);
    return map;
  }

}
