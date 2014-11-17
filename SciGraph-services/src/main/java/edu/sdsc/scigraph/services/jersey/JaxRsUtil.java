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

import static java.lang.String.format;

import java.util.List;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.util.JSONWrappedObject;

public class JaxRsUtil {

  final static List<Variant> VARIANTS = 
      Variant.VariantListBuilder.newInstance().mediaTypes(
          CustomMediaTypes.TEXT_CSV_TYPE, 
          CustomMediaTypes.TEXT_TSV_TYPE,
          MediaType.APPLICATION_XML_TYPE, 
          MediaType.APPLICATION_JSON_TYPE,
          CustomMediaTypes.APPLICATION_JSONP_TYPE,
          CustomMediaTypes.APPLICATION_RIS_TYPE,
          CustomMediaTypes.APPLICATION_GRAPHML_TYPE,
          CustomMediaTypes.APPLICATION_GRAPHSON_TYPE,
          CustomMediaTypes.TEXT_GML_TYPE).add().build();

  public static boolean isVariant(Request request, MediaType type) {
    return request.selectVariant(VARIANTS).getMediaType().equals(type);
  }

  public static Variant getVariant(Request request) {
    return request.selectVariant(VARIANTS);
  }

  public static Object wrapJsonp(Request request, GenericEntity<?> response, String callback) {
    if (JaxRsUtil.isVariant(request, CustomMediaTypes.APPLICATION_JSONP_TYPE)) {
      return new JSONWrappedObject(format("%s(", callback), ");", response.getEntity());
    } else {
      return Response.ok(response).build();
    }
  }

}
