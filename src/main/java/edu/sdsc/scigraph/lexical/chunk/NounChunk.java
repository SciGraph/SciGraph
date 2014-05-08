package edu.sdsc.scigraph.lexical.chunk;

import javax.xml.bind.annotation.XmlRootElement;

import edu.sdsc.scigraph.annotation.Token;

@XmlRootElement
public class NounChunk extends Token<String> {

  public NounChunk(String text, int start, int end) {
    super(text, start, end);
  }

}
