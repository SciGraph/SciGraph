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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/***
 * Adds a beginning of line token and an ending of line token.
 * 
 * <p>Useful for supporting "exactish" matching with Lucene.
 */
public final class BolEolFilter extends TokenFilter {

  private final String bol;
  private final String eol;

  boolean addedBol = false;
  boolean addedEol = false;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public BolEolFilter(TokenStream input) {
    this(input, "^", "$");
  }

  public BolEolFilter(TokenStream input, String bol, String eol) {
    super(input);
    this.bol = bol;
    this.eol = eol;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (!addedBol) {
      termAtt.setEmpty().append(bol);
      addedBol = true;
      return true;
    }
    if (input.incrementToken()) {
      return true;
    }
    if (!addedEol) {
      termAtt.setEmpty().append(eol);
      addedEol = true;
      return true;
    }
    return false;
  }

}