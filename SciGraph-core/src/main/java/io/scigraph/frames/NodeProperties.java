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
package io.scigraph.frames;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NodeProperties extends CommonProperties {

  public static final String LABEL = "label";

  private Set<String> labels = new HashSet<>();
  private boolean anonymous;

  public NodeProperties(long id) {
    super(id);
  }

  public void addLabel(String label) {
    labels.add(label);
  }

  public Collection<String> getLabels() {
    return labels;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

}
