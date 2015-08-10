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
package io.scigraph.internal.reachability;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.MoreObjects;

@ThreadSafe
class InOutList {

  Set<Long> inList = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
  Set<Long> outList = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

  Set<Long> getInList() {
    return inList;
  }

  Set<Long> getOutList() {
    return outList;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
    .add("inList", inList)
    .add("outList", outList)
    .toString();
  }

}