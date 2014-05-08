package edu.sdsc.scigraph.lexical;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.InvalidFormatException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.sdsc.scigraph.annotation.Token;
import edu.sdsc.scigraph.lexical.pos.PosToken;
import edu.sdsc.scigraph.opennlp.OpenNlpModule;

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
