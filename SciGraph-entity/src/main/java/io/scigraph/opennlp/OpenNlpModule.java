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
package io.scigraph.opennlp;

import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import com.google.inject.AbstractModule;
import com.google.inject.throwingproviders.CheckedProvider;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class OpenNlpModule extends AbstractModule {

  @Override
  protected void configure() {
    install(ThrowingProviderBinder.forModule(this));
  }

  public interface TokenizerProvider extends CheckedProvider<Tokenizer> {
    @Override
    Tokenizer get() throws IOException;
  }

  @CheckedProvides(TokenizerProvider.class)
  Tokenizer getTokenizer() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-token.bin")) {
      TokenizerModel model = new TokenizerModel(is);
      return new TokenizerME(model);
    }
  }

  public interface SentenceDetectorProvider extends CheckedProvider<SentenceDetectorME> {
    @Override
    SentenceDetectorME get() throws IOException;
  }

  @CheckedProvides(SentenceDetectorProvider.class)
  SentenceDetectorME getSentenceDetector() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-sent.bin")) {
      SentenceModel model = new SentenceModel(is);
      return new SentenceDetectorME(model);
    }
  }

  public interface PosTaggerProvider extends CheckedProvider<POSTaggerME> {
    @Override
    POSTaggerME get() throws IOException;
  }

  @CheckedProvides(PosTaggerProvider.class)
  POSTaggerME getPosTagger() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-pos-maxent.bin")) {
      POSModel model = new POSModel(is);
      return new POSTaggerME(model);
    }
  }

  public interface ChunkerProvider extends CheckedProvider<ChunkerME> {
    @Override
    ChunkerME get() throws IOException;
  }

  @CheckedProvides(ChunkerProvider.class)
  ChunkerME getChunker() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-chunker.bin")) {
      ChunkerModel model = new ChunkerModel(is);
      return new ChunkerME(model);
    }
  }

}
