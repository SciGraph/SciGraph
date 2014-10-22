package edu.sdsc.scigraph.services.jersey;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.Responses;

public class UnknownClassException extends WebApplicationException {

  private static final long serialVersionUID = 1L;

  public UnknownClassException() {
    super(Responses.notFound().build());
  }

  public UnknownClassException(String id) {
    super(Response.status(Responses.NOT_FOUND).
        entity("Failed to find class with ID: " + id).type("text/plain").build());
  }

}
