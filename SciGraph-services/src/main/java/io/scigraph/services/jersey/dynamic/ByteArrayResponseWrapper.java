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
package io.scigraph.services.jersey.dynamic;

import java.io.ByteArrayOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class ByteArrayResponseWrapper extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  ByteArrayResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  byte[] getBytes() {
    return baos.toByteArray();
  }

  @Override
  public ServletOutputStream getOutputStream() {
    return new ServletOutputStream() {
      @Override
      public void write(int b) {
        baos.write(b);
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener writeListener) {}
    };
  }

}
