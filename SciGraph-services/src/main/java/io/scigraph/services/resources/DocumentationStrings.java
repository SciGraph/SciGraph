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
package io.scigraph.services.resources;

class DocumentationStrings {

  final static String REST_ABUSE_DOC = "<em>NOTE:</em> This is an abuse of REST principals. This POST operation doesn't create resources";

  final static String JSONP_DOC = 
      "Name of the JSONP callback ('fn' by default). "
          + "Supplying this parameter or requesting a javascript media type will cause a JSONP response to be rendered.";

  final static String CONTENT_DOC = "The content to annotate";

  final static String INCLUDE_CATEGORIES_DOC = "A set of categories to include";

  final static String EXCLUDE_CATEGORIES_DOC = "A set of categories to exclude";

  final static String MINIMUM_LENGTH_DOC = "The minimum number of characters in annotated entities";

  final static String LONGEST_ENTITY_DOC =
      "Should only the longest entity be returned for an overlapping group";

  final static String SEARCH_SYNONYMS = "Should synonyms be matched";

  final static String SEARCH_ABBREVIATIONS = "Should abbreviations be matched";

  final static String SEARCH_ACRONYMS = "Should acronyms be matched";

  final static String INCLUDE_DEPRECATED_CLASSES = "Should deprecated classes be included";

  final static String INCLUDE_ABBREV_DOC = "Should abbreviations be included";

  final static String INCLUDE_ACRONYMS_DOC = "Should acronyms be included";

  final static String INCLUDE_NUMBERS_DOC = "Should numbers be included";

  final static String IGNORE_TAGS_DOC = "HTML tags that should not be annotated";

  final static String STYLESHEETS_DOC = "CSS stylesheets to add to the HEAD";

  final static String SCRIPTS_DOC = "JavaScripts that should to add to the HEAD";

  final static String TARGET_IDS_DOC = "A set of element IDs to annotate";

  final static String CSS_CLASS_DOCS = "A set of CSS class names to annotate";

  final static String RESULT_LIMIT_DOC = "Maximum result count";

  final static String GRAPH_ID_DOC = "This ID should be either a CURIE or an IRI";

  final static String DIRECTION_DOC = "Which direction to traverse: INCOMING, OUTGOING, BOTH (default). Only used if relationshipType is specified.";

  final static String DIRECTION_ALLOWED = "BOTH,INCOMING,OUTGOING";

  final static String PROJECTION_DOC = "Which properties to project. Defaults to '*'.";

  /********** Reconcile documentation **************/
  final static String RECONCILE_NOTES = "An implementation of "
      + "<a href=\"https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-API\" target=\"_blank\">"
      + "OpenRefine reconciliation services</a> "
      + "supporting OpenRefine term resolution backed by a SciGraph instance. It is unlikely that a client will use these services "
      + "directly but would instead point an OpenRefine instance to <em>http://example.org/SciGraph/refine/reconcile<em>";

  final static String RECONCILE_QUERY_DOC = "A call to a reconciliation service API for a single query looks like either of these:"
      + "<ul><li>http://foo.com/bar/reconcile?query=...string...</li>"
      + "<li>http://foo.com/bar/reconcile?query={...json object literal...}</li></ul>"
      + "If the query parameter is a string, then it's an abbreviation of <em>query={\"query\":...string...}</em>."
      + "<em>NOTE:</em> We encourage all API consumers to consider the single query mode <b>DEPRECATED</b>."
      + "Refine currently only uses the multiple query mode, "
      + "but other consumers of the API may use the single query option since it was included in the spec.";

  final static String RECONCILE_QUERIES_DOC = "A call to a standard reconciliation service API for multiple queries looks like this:"
      + "<ul><li>http://foo.com/bar/reconcile?queries={...json object literal...}</li></ul>"
      + "The json object literal has zero or more key/value pairs with arbitrary keys where the value is in the same "
      + "format as a single query, e.g."
      + "<ul><li>http://foo.com/bar/reconcile?queries={ \"q0\" : { \"query\" : \"foo\" }, \"q1\" : { \"query\" : \"bar\" } }</li></ul>"
      + "\"q0\" and \"q1\" can be arbitrary strings.";

  final static String RECONCILE_LIMIT_DOC = "An integer to specify how many results to return.";

  final static String RECONCILE_TYPE_DOC = "A single string, or an array of strings, specifying the types of result e.g.,"
      + " person, product, ... The actual format of each type depends on the service "
      + "(e.g., \"/government/politician\" as a Freebase type). ";

  final static String RECONCILE_TYPE_STRICT_DOC = "A string, one of \"any\", \"all\", \"should\". "
      + "<em>NOTE:</em> This is unused at this time and defaults to \"any\"";

  final static String RECONCILE_PROPERTIES_DOC = "Array of json object literals.";
}
