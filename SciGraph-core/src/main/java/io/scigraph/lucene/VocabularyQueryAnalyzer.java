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

import io.scigraph.frames.Concept;
import io.scigraph.frames.NodeProperties;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.Lucene43StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

public final class VocabularyQueryAnalyzer extends Analyzer {

  private final Analyzer analyzer;

  public VocabularyQueryAnalyzer() {
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put(NodeProperties.LABEL, new TermAnalyzer());
    fieldAnalyzers.put(NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, new ExactAnalyzer());
    fieldAnalyzers.put(Concept.SYNONYM, new TermAnalyzer());
    fieldAnalyzers.put(Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX, new ExactAnalyzer());
    fieldAnalyzers.put(Concept.ABREVIATION, new TermAnalyzer());
    fieldAnalyzers.put(Concept.ABREVIATION + LuceneUtils.EXACT_SUFFIX, new ExactAnalyzer());
    fieldAnalyzers.put(Concept.ACRONYM, new TermAnalyzer());
    fieldAnalyzers.put(Concept.ACRONYM + LuceneUtils.EXACT_SUFFIX, new ExactAnalyzer());
    analyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), fieldAnalyzers);
  }

  final static class TermAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
      Tokenizer tokenizer = new WhitespaceTokenizer();
      TokenStream result =
          new PatternReplaceFilter(tokenizer,
              Pattern.compile("^([\\.!\\?,:;\"'\\(\\)]*)(.*?)([\\.!\\?,:;\"'\\(\\)]*)$"), "$2",
              true);
      result = new PatternReplaceFilter(result, Pattern.compile("'s"), "s", true);
      // result = new StopFilter(result, LuceneUtils.caseSensitiveStopSet);
      result = new Lucene43StopFilter(false, result, LuceneUtils.caseSensitiveStopSet);
      result = new LowerCaseFilter(result);
      result = new ASCIIFoldingFilter(result);

      return new TokenStreamComponents(tokenizer, result);
    }

  }

  // TODO Not sure that using reflection is the right way to handle that
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    try {
      Class<? extends Analyzer> clazz = analyzer.getClass();
      Method getWrappedAnalyzer = clazz.getDeclaredMethod("getWrappedAnalyzer", String.class);
      getWrappedAnalyzer.setAccessible(true);
      Analyzer currentAnalyzer = (Analyzer) getWrappedAnalyzer.invoke(analyzer, fieldName);
      Class<? extends Analyzer> clazz2 = currentAnalyzer.getClass();
      Method cc = clazz2.getDeclaredMethod("createComponents", String.class);
      cc.setAccessible(true);
      return (TokenStreamComponents) cc.invoke(currentAnalyzer, fieldName);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
