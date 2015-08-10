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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingMap;

public class RefineResults extends ForwardingMap<String, List<RefineResult>> {

  Map<String, List<RefineResult>> delegate = new HashMap<>();
  
  public RefineResults() {
    delegate.put("result", new ArrayList<RefineResult>());
  }
  
  @Override
  protected Map<String, List<RefineResult>> delegate() {
    return delegate;
  }
  
  public void addResult(RefineResult result) {
    delegate.get("result").add(result);
  }

}
