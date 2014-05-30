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
package edu.sdsc.scigraph.internal.reachability;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Objects;

@ThreadSafe
class InOutList {

  Set<Long> inList = new ConcurrentSkipListSet<>();
  Set<Long> outList = new ConcurrentSkipListSet<>();

  Set<Long> getInList() {
    return inList;
  }

  Set<Long> getOutList() {
    return outList;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
    .add("inList", inList)
    .add("outList", outList)
    .toString();
  }

}