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
package edu.sdsc.scigraph.lucene;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;

import com.google.common.base.Suppliers;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;

public final class VocabularyIndexAnalyzer extends Analyzer {

  private final Analyzer analyzer;

  public VocabularyIndexAnalyzer() throws IOException, URISyntaxException {
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

    static SynonymMap map = Suppliers.memoize(new SynonymMapSupplier()).get();

    @SuppressWarnings("deprecation")
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      Tokenizer tokenizer = new WhitespaceTokenizer(LuceneUtils.getVersion(), reader);
      TokenStream result = new PatternReplaceFilter(tokenizer, Pattern.compile("^([\\.!\\?,:;\"'\\(\\)]*)(.*?)([\\.!\\?,:;\"'\\(\\)]*)$"), "$2", true);
      result = new PatternReplaceFilter(result, Pattern.compile("'s"), "s", true);
      result = new BolEolFilter(result);
      result = new SynonymFilter(result, map, true);
      result = new StopFilter(false, result, LuceneUtils.caseSensitiveStopSet);
      result = new LowerCaseFilter(LuceneUtils.getVersion(), result);
      result = new ASCIIFoldingFilter(result);

      return result;
    }

  }

  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    return analyzer.tokenStream(fieldName, reader);
  }

}