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
package io.scigraph.lexical;

import java.util.Set;

import com.google.common.collect.Sets;

public final class PhraseChunker {

  // Determiners & Numerals
  private static final Set<String> DETERMINER_TAGS = Sets.newHashSet(
      "DD", "DB", "DT");

  // Adjectives
  private static final Set<String> ADJECTIVE_TAGS = Sets.newHashSet(
      "JJ", "JJR", "JJT"); //* and QL?

  // Nouns
  private static final Set<String> NOUN_TAGS = Sets.newHashSet(
      "NN", "NNP", "NNS");

  // Pronoun
  private static final Set<String> PRONOUN_TAGS = Sets.newHashSet(
      "PN", "PND", "PPG", "PNR");

  // Adverbs
  private static final Set<String> ADVERB_TAGS = Sets.newHashSet(
      "RR", "RRR", "RRT");

  // Verbs
  private static final Set<String> VERB_TAGS = Sets.newHashSet(
      "VVB", "VVD", "VVG", "VVGJ", "VVGN", "VVI", "VVN", "VVNJ", "VVZ");

  // Auxiliary Verbs
  private static final Set<String> AUXILIARY_VERB_TAGS = Sets.newHashSet(
      "VBB", "VBD", "VBG", "VBI", "VBN", "VBZ");

  protected static final Set<String> PUNCTUATION_TAGS = Sets.newHashSet(
      "'", ".", "*", ":");

  public static final Set<String> START_VERB_TAGS = Sets.newHashSet();
  public static final Set<String> CONTINUE_VERB_TAGS = Sets.newHashSet();
  public static final Set<String> START_NOUN_TAGS = Sets.newHashSet();
  public static final Set<String> CONTINUE_NOUN_TAGS = Sets.newHashSet();

  static {
    START_NOUN_TAGS.addAll(DETERMINER_TAGS);
    START_NOUN_TAGS.addAll(ADJECTIVE_TAGS);
    START_NOUN_TAGS.addAll(NOUN_TAGS);
    START_NOUN_TAGS.addAll(PRONOUN_TAGS);

    CONTINUE_NOUN_TAGS.addAll(START_NOUN_TAGS);
    CONTINUE_NOUN_TAGS.addAll(ADVERB_TAGS);
    CONTINUE_NOUN_TAGS.addAll(PUNCTUATION_TAGS);
    CONTINUE_NOUN_TAGS.add("CS");
    CONTINUE_NOUN_TAGS.add("CS+");
    CONTINUE_NOUN_TAGS.add("SYM");
    CONTINUE_NOUN_TAGS.add("VVNJ");
    CONTINUE_NOUN_TAGS.add("MC");

    START_VERB_TAGS.addAll(VERB_TAGS);
    START_VERB_TAGS.addAll(AUXILIARY_VERB_TAGS);
    START_VERB_TAGS.addAll(ADVERB_TAGS);

    CONTINUE_VERB_TAGS.addAll(START_VERB_TAGS);
    CONTINUE_VERB_TAGS.addAll(PUNCTUATION_TAGS);
  }

}