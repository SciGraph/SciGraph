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
package edu.sdsc.scigraph.lucene;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.util.Version;

import com.google.common.collect.Lists;

public class LuceneUtils {

  public static final String EXACT_SUFFIX = "_EXACT";

  /***
   * @return The current Lucene version in use across the application
   */
  public static Version getVersion() {
    return Version.LUCENE_36;
  }
  
  public static Set<?> caseSensitiveStopSet;

  static {
    List<String> stopWords = Lists.newArrayList();
    for (Iterator<?> stopWord = StopAnalyzer.ENGLISH_STOP_WORDS_SET.iterator(); stopWord.hasNext(); ) {
      String word = new String((char[])stopWord.next());
      stopWords.add(word);
      stopWords.add(Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase());
    }
    caseSensitiveStopSet = StopFilter.makeStopSet(getVersion(), stopWords, false);
  }

}
