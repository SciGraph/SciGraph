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
package io.scigraph.annotation;

import java.io.IOException;
import java.util.List;

/***
 * EntityProcessor processes content looking for text that can be annotated.
 */
public interface EntityProcessor {

  /***
   * Identify annotations within the provided plain text.
   *
   * @param content
   *    The string to identify annotations within.
   * @param config
   *    Configuration settings for identifying annotations. Note that the `config.reader`
   *    setting will be involved.
   * @return A list of entity annotations from this text.
   */
  List<EntityAnnotation> getAnnotations(String content, EntityFormatConfiguration config);

  /***
   * Identify (and possibly tag) annotations within the provided HTML text.
   *
   * @param configuration Configuration settings for identifying annotations.
   * @return A list of entities found in the content
   * @throws IOException
   */
  List<EntityAnnotation> annotateEntities(EntityFormatConfiguration configuration)
      throws IOException;

}