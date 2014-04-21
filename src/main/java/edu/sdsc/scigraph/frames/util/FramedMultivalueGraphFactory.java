/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.frames.util;

import com.tinkerpop.frames.FramedGraphConfiguration;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.AbstractModule;

/***
 * A {@link FramedGraphFactory} with support for generic, multi-valued properties.
 * 
 * <p>See {@link MultiPropertyMethodHandler} for more information about how multi-valued
 * properties are handled.
 */
public class FramedMultivalueGraphFactory extends FramedGraphFactory {

  public FramedMultivalueGraphFactory() {
    super(new AbstractModule() {
      @Override
      protected void doConfigure(FramedGraphConfiguration config) {
        config.addMethodHandler(new MultiPropertyMethodHandler());
      }
    });
  }

}
