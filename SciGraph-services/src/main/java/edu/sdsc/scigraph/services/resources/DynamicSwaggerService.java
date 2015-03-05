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
package edu.sdsc.scigraph.services.resources;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;
import edu.sdsc.scigraph.services.swagger.beans.resource.Resource;

@Path("/api-docs//dynamic")
@Produces(MediaType.APPLICATION_JSON)
public class DynamicSwaggerService {

  private final List<Apis> apis;

  @Inject
  public DynamicSwaggerService(List<Apis> apis) {
    this.apis = apis;
  }

  @GET
  @Timed
  public Resource getDocumentation() {
    Resource resource = new Resource();
    resource.setApiVersion("1.0.1");
    resource.setSwaggerVersion("1.2");
    resource.setBasePath("../../scigraph");
    resource.setResourcePath("/dynamic");
    resource.setProduces(newArrayList(MediaType.APPLICATION_JSON));

    for (Apis api: apis) {
      resource.getApis().add(api);
    }
    return resource;
  }

}
