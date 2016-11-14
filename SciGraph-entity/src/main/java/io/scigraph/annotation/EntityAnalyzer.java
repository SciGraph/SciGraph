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

import io.scigraph.lucene.LuceneUtils;
import io.scigraph.lucene.PatternReplaceFilter;

import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

final class EntityAnalyzer extends Analyzer {

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tokenizer = new WhitespaceTokenizer();
    TokenStream result =
        new PatternReplaceFilter(tokenizer,
            Pattern.compile("^([\\.!\\?,:;\"'\\(\\)]*)(.*?)([\\.!\\?,:;\"'\\(\\)]*)$"), "$2", true);
    result = new PatternReplaceFilter(result, Pattern.compile("'s"), "s", true);
    return new TokenStreamComponents(tokenizer, result);
  }

}
