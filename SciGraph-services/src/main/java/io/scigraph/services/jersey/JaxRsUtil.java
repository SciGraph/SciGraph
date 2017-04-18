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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.util.JSONWrappedObject;

public class JaxRsUtil {

  public static final String DEFAULT_JSONP_CALLBACK = "fn";
  
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
          CustomMediaTypes.APPLICATION_XGMML_TYPE,
          CustomMediaTypes.TEXT_GML_TYPE,
          CustomMediaTypes.IMAGE_JPEG_TYPE,
          CustomMediaTypes.IMAGE_PNG_TYPE).add().build();

  public static boolean isVariant(Request request, MediaType type) {
    return request.selectVariant(VARIANTS).getMediaType().equals(type);
  }

  public static Variant getVariant(Request request) {
    return request.selectVariant(VARIANTS);
  }

  /***
   * Build a Response, optionally wrapped in a JSONP callback.
   * <p>The response will be wrapped in a JSONP callback if any of the following apply:
   * <ul>
   * <li> The requested media type is <em>application/javascript</em>
   * <li> The callback is not null or empty
   * </ul>
   * @param request The request
   * @param response The response to wrap
   * @param callback The callback
   * @return A Response object, wrapped if necessary.
   */
  public static Object wrapJsonp(Request request, GenericEntity<?> response, @Nullable String callback) {
    if (JaxRsUtil.isVariant(request, CustomMediaTypes.APPLICATION_JSONP_TYPE) || !isNullOrEmpty(callback)) {
      callback = Optional.ofNullable(callback).orElse(DEFAULT_JSONP_CALLBACK);
      
      return new JSONWrappedObject(format("%s(", callback), ");", response.getEntity());
    } else {
      return Response.ok(response).build();
    }
  }

}
