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
package io.scigraph.neo4j;

import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.common.collect.ForwardingConcurrentMap;

/***
 * A map of external unique keys to internal Neo4j IDs for keeping track of these mappings during a
 * bulk load.
 */
@ThreadSafe
public class IdMap extends ForwardingConcurrentMap<String, Long> {

  private final ConcurrentMap<String, Long> delegate;

  public IdMap() {
    DB maker = DBMaker.newMemoryDB().make();
    delegate = maker.createHashMap(IdMap.class.getName()).make();
  }

  @Inject
  public IdMap(DB maker) {
    delegate = maker.getHashMap(IdMap.class.getName());
  }

  @Override
  protected ConcurrentMap<String, Long> delegate() {
    return delegate;
  }

}
