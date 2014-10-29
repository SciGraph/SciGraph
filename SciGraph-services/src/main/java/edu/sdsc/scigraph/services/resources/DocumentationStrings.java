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
package edu.sdsc.scigraph.services.resources;


class DocumentationStrings {

  final static String REST_ABUSE_DOC = "<em>NOTE:</em> This is an abuse of REST principals. This POST operation doesn't create resources";

  final static String JSONP_DOC = "JSONP callback";

  final static String CONTENT_DOC = "The content to annotate";

  final static String INCLUDE_CATEGORIES_DOC = "A set of categories to include";

  final static String EXCLUDE_CATEGORIES_DOC = "A set of categories to exclude";

  final static String MINIMUM_LENGTH_DOC = "The minimum number of characters in annotated entities";

  final static String LONGEST_ENTITY_DOC =
      "Should only the longest entity be returned for an overlapping group";

  final static String INCLUDE_ABBREV_DOC = "Should abbreviations be included";

  final static String INCLUDE_ACRONYMS_DOC = "Should acronyms be included";

  final static String INCLUDE_NUMBERS_DOC = "Should numbers be included";

  final static String IGNORE_TAGS_DOC = "HTML tags that should not be annotated";

  final static String STYLESHEETS_DOC = "CSS stylesheets to add to the HEAD";

  final static String SCRIPTS_DOC = "JavaScripts that should to add to the HEAD";

  final static String TARGET_IDS_DOC = "A set of element IDs to annotate";

  final static String CSS_CLASS_DOCS = "A set of CSS class names to annotate";

  final static String RESULT_LIMIT_DOC = "Maximum result count";

  final static String GRAPH_ID_DOC = "This ID should be either a CURIE or a URL fragment";

}
