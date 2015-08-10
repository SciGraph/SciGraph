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

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import io.scigraph.annotation.Token;
import io.scigraph.lexical.chunk.NounChunk;
import io.scigraph.lexical.chunk.VerbChunk;
import io.scigraph.lexical.pos.PosToken;
import io.scigraph.opennlp.OpenNlpModule.ChunkerProvider;
import io.scigraph.opennlp.OpenNlpModule.PosTaggerProvider;
import io.scigraph.opennlp.OpenNlpModule.SentenceDetectorProvider;
import io.scigraph.opennlp.OpenNlpModule.TokenizerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import com.google.common.annotations.Beta;

public class LexicalLibOpenNlpImpl implements LexicalLib {

  Tokenizer tokenizer;
  SentenceDetectorME sentenceDetector;
  POSTaggerME tagger;
  ChunkerME chunker;

  @Inject
  protected LexicalLibOpenNlpImpl(
      TokenizerProvider tokenizerProvider,
      SentenceDetectorProvider sentenceProvider,
      PosTaggerProvider posProvider,
      ChunkerProvider chunkerProvider) throws IOException {
    tokenizer = tokenizerProvider.get();
    sentenceDetector = sentenceProvider.get();
    tagger = posProvider.get();
    chunker = chunkerProvider.get();
  }

  @Override
  public List<String> extractSentences(String text) {
    String[] sentences = sentenceDetector.sentDetect(text);
    return newArrayList(sentences);
  }

  @Override
  public List<PosToken> tagPOS(String sentence) {
    String[] tokens = tokenizer.tokenize(sentence);
    Span[] spans = tokenizer.tokenizePos(sentence);
    String[] tags = tagger.tag(tokens);
    List<PosToken> poss = new ArrayList<>();
    for (int i = 0; i < tokens.length; i++) {
      poss.add(new PosToken(tokens[i], tags[i], spans[i].getStart(), spans[i].getEnd()));
    }
    return poss;
  }

  @Override
  public List<Token<String>> getChunks(String text) {
    int sentenceStart = 0;
    List<Token<String>> retChunks = new LinkedList<>();
    for (String sentence: extractSentences(text)) {
      String[] tokens = tokenizer.tokenize(sentence);
      Span[] spans = tokenizer.tokenizePos(sentence);
      String[] tags = tagger.tag(tokens);
      String[] chunks = chunker.chunk(tokens, tags);

      for (int i = 0; i < chunks.length; i++) {
        List<String> chunk = new LinkedList<>();
        int start = i;
        if ("B-NP".equals(chunks[i])) {
          chunk.add(tokens[i]);
          while (i + 1 < chunks.length && "I-NP".equals(chunks[i + 1])) {
            chunk.add(tokens[i+1]);
            i++;
          }
          retChunks.add(new NounChunk(on(' ').join(chunk).replace(" ,", ","), 
              sentenceStart + spans[start].getStart(), sentenceStart + spans[i].getEnd()));
        } else if ("B-VP".equals(chunks[i])) {
          chunk.add(tokens[i]);
          while (i + 1 < chunks.length && "I-VP".equals(chunks[i + 1])) {
            chunk.add(tokens[i+1]);
            i++;
          }
          retChunks.add(new VerbChunk(on(' ').join(chunk).replace(" ,", ","), 
              sentenceStart + spans[start].getStart(), sentenceStart + spans[i].getEnd()));
        }
      }
      sentenceStart += spans[spans.length - 1].getEnd() + 2;
    }
    return retChunks;
  }

  @Override
  @Beta
  public List<Token<String>> getEntities(String text) {
    int sentenceStart = 0;
    List<Token<String>> retChunks = new LinkedList<>();
    for (String sentence: extractSentences(text)) {
      String[] tokens = tokenizer.tokenize(sentence);
      Span[] spans = tokenizer.tokenizePos(sentence);
      String[] tags = tagger.tag(tokens);

      for (int i = 0; i < tags.length; i++) {
        List<String> chunk = new LinkedList<>();
        int start = i;
        
        if (PhraseChunker.START_NOUN_TAGS.contains(tags[i])) {
          chunk.add(tokens[i]);
          while (i + 1 < tags.length && PhraseChunker.CONTINUE_NOUN_TAGS.contains(tags[i + 1])) {
            chunk.add(tokens[i+1]);
            i++;
          }
          retChunks.add(new NounChunk(on(' ').join(chunk).replace(" ,", ","), 
              sentenceStart + spans[start].getStart(), sentenceStart + spans[i].getEnd()));
        } else if (PhraseChunker.START_VERB_TAGS.contains(tags[i])) {
          chunk.add(tokens[i]);
          while (i + 1 < tags.length && PhraseChunker.CONTINUE_VERB_TAGS.contains(tags[i + 1])) {
            chunk.add(tokens[i+1]);
            i++;
          }
          retChunks.add(new VerbChunk(on(' ').join(chunk).replace(" ,", ","), 
              sentenceStart + spans[start].getStart(), sentenceStart + spans[i].getEnd()));
        }
      }
      sentenceStart += spans[spans.length - 1].getEnd() + 2;
    }
    return retChunks;
  }

}
