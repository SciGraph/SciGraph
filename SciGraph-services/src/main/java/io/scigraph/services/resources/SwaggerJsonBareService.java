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
package io.scigraph.services.resources;

import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.listing.ApiListingResource;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class SwaggerJsonBareService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/swagger")
    @ApiOperation(value = "The swagger definition in JSON", hidden = true)
    public Response getListingJsonBare(
            @Context Application app,
            @Context ServletConfig sc,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @Context ResourceContext rc) {
        ApiListingResource apiListingResource = rc.getResource(ApiListingResource.class);
        return apiListingResource.getListingJson(app, sc, headers, uriInfo);
    }
}
