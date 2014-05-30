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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;

@Path("/literature")
//@Api(value = "/pmid", description = "test")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, 
  CustomMediaTypes.APPLICATION_RIS, CustomMediaTypes.TEXT_CSV})
public class LiteratureService extends BaseResource {

  @XmlRootElement
  public static class Table {
    @XmlElement
    @JsonProperty
    List<Row> row = new ArrayList<>();
  }

  @XmlRootElement
  public static class Row {
    @XmlElement
    @JsonProperty
    List<Entry> entries = new ArrayList<>();
  }

  @XmlRootElement
  public static class Entry {
    
    @XmlAttribute
    @JsonProperty
    String key;

    @XmlElement
    @JsonProperty
    List<Object> value;

    Entry() {}
    
    public Entry(String key, List<Object> value) {
      this.key = key;
      this.value = value;
    }

  }

  @GET
  @Path("/test")
  public Object test() {
    Table table = new Table();
    Row row = new Row();
    List<Object> a = new ArrayList<>();
    a.add("foo");
    row.entries.add(new Entry("a", a));
    List<Object> b = new ArrayList<>();
    b.add(1);
    b.add(2);
    row.entries.add(new Entry("b", b));
    table.row.add(row);
    ObjectMapper mapper = new ObjectMapper();
    try {
      System.out.println(mapper.writeValueAsString(table)) ;
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
    
    GenericEntity<Table> response = new GenericEntity<Table>(table){};
    return JaxRsUtil.wrapJsonp(request, response, "fn");
  }

}
