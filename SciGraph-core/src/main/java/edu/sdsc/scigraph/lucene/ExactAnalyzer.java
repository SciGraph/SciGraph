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

import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.Version;

public final class ExactAnalyzer extends Analyzer {

  private static final Pattern pattern = Pattern.compile("'");

  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    Tokenizer tokenizer = new KeywordTokenizer(reader);
    TokenStream result = new LowerCaseFilter(Version.LUCENE_36, tokenizer);
    result = new ASCIIFoldingFilter(result);
    result = new PatternReplaceFilter(result, pattern, "", true);
    return result;
  }

}