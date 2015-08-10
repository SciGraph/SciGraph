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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.scigraph.annotation.Token;
import io.scigraph.lexical.LexicalLibModule;
import io.scigraph.lexical.LexicalLibOpenNlpImpl;
import io.scigraph.lexical.pos.PosToken;
import io.scigraph.opennlp.OpenNlpModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.InvalidFormatException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class LexicalLibOpenNlpImplTest {

  static List<String> sentenceFragments = newArrayList("This is the first sentence.",
      "This is the second sentence.", "This is the third sentence.");

  static String joinedText = on("  ").join(sentenceFragments);

  static LexicalLibOpenNlpImpl lexLib;

  @BeforeClass
  public static void setup() throws InvalidFormatException, IOException {
    Injector i = Guice.createInjector(new LexicalLibModule(), new OpenNlpModule());
    lexLib = i.getInstance(LexicalLibOpenNlpImpl.class);
  }

  @Test
  public void testExtractSentences() {
    assertEquals("Should be three sentences.", 3, lexLib.extractSentences(joinedText).size());
    assertEquals("Sentences should match.", sentenceFragments, lexLib.extractSentences(joinedText));
  }

  @Test
  public void testTagPOS() {
    List<String> tags = new ArrayList<>();
    for (PosToken token : lexLib.tagPOS(sentenceFragments.get(0))) {
      tags.add(token.getPos());
    }
    assertEquals("POS should match.", newArrayList("DT", "VBZ", "DT", "JJ", "NN", "."), tags);
  }

  @Test
  public void testChunks() {
    String text = "The Neuroscience Information Framework is a website.";
    boolean found = false;
    for (Token<String> chunk : lexLib.getChunks(text)) {
      if ("The Neuroscience Information Framework".equals(chunk.getToken())) {
        found = true;
      }
    }
    assertTrue("Didn't find a noun phrase.", found);
  }

  @Test
  public void testEqualChunks() {
    String text = "The Neuroscience Information Framework is a dynamic inventory of Web-based neuroscience resources: data, materials, and tools accessible via any computer connected to the Internet.";
    for (Token<String> chunk : lexLib.getChunks(text)) {
      assertEquals(chunk.getToken(), text.substring(chunk.getStart(), chunk.getEnd()));
    }
  }

  @Test
  public void testMultiSentenceChunks() {
    for (Token<String> chunk : lexLib.getChunks(joinedText)) {
      assertEquals(chunk.getToken(), joinedText.substring(chunk.getStart(), chunk.getEnd()));
    }
  }

}
