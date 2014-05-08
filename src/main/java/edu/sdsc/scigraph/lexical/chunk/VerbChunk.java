package edu.sdsc.scigraph.lexical.chunk;

import javax.xml.bind.annotation.XmlRootElement;

import edu.sdsc.scigraph.annotation.Token;

@XmlRootElement
public class VerbChunk extends Token<String> {

  public VerbChunk(String text, int start, int end) {
    super(text, start, end);
  }

}
