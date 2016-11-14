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
package io.scigraph.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class LuceneUtils {

  public static final String EXACT_SUFFIX = "_EXACT";

  /***
   * @return The current Lucene version in use across the application
   */
  public static Version getVersion() {
    return Version.LUCENE_5_5_0;
  }

  public static final CharArraySet caseSensitiveStopSet;

  static {
    List<String> stopWords = Lists.newArrayList();
    for (Iterator<?> stopWord = StopAnalyzer.ENGLISH_STOP_WORDS_SET.iterator(); stopWord.hasNext();) {
      String word = new String((char[]) stopWord.next());
      stopWords.add(word);
      stopWords.add(Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase());
    }
    caseSensitiveStopSet = StopFilter.makeStopSet(stopWords, false);
  }

  public static boolean isStopword(String word) {
    for (Iterator<?> stopWord = StopAnalyzer.ENGLISH_STOP_WORDS_SET.iterator(); stopWord.hasNext();) {
      String stopword = new String((char[]) stopWord.next());
      if (stopword.equalsIgnoreCase(word)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAllStopwords(List<String> words) {
    for (String word : words) {
      if (!isStopword(word)) {
        return false;
      }
    }
    return true;
  }

  public static Query getBoostedQuery(QueryParser parser, String queryString, float boost)
      throws ParseException {
    Query query = parser.parse(queryString);
    query.setBoost(boost);
    return query;
  }

  public static String getTokenization(Analyzer analyzer, String term) {
    List<String> ret = getTokenization(analyzer, (CharSequence) term);
    return Joiner.on(", ").join(ret);
  }

  public static List<String> getTokenization(Analyzer analyzer, CharSequence term) {
    List<String> ret = Lists.newArrayList();

    try {
      TokenStream stream = analyzer.tokenStream("", new StringReader(term.toString()));
      CharTermAttribute token = stream.getAttribute(CharTermAttribute.class);
      stream.reset();
      while (stream.incrementToken()) {
        ret.add(token.toString());
      }
      stream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ret;
  }

}
