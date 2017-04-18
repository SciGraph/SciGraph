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
package io.scigraph.services.refine;

import java.util.Optional;

import com.google.inject.AbstractModule;

public class RefineModule extends AbstractModule {

  private static final String DEFAULT_SERVICE_NAME = "SciGraph Reconciliation Service";
  private static final String ID_SPACE = "http://example.org";

  private final ServiceMetadata metadata;

  public RefineModule(Optional<ServiceMetadata> metadata) {
    if (metadata.isPresent()) {
      this.metadata = metadata.get();
    } else {
      this.metadata = new ServiceMetadata();
      this.metadata.setName(DEFAULT_SERVICE_NAME);
      this.metadata.setIdentifierSpace(ID_SPACE);
      this.metadata.setSchemaSpace(ID_SPACE);
    }
  }

  @Override
  protected void configure() {
    bind(ServiceMetadata.class).toInstance(metadata);
  }

}
