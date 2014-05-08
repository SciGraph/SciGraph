package edu.sdsc.scigraph.lexical.pos;

import static java.lang.String.format;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import edu.sdsc.scigraph.annotation.Token;

@XmlRootElement
public class PosToken extends Token<String> {

  private String pos;

  public PosToken(String token, String pos, int start, int end) {
    super(token, start, end);
    this.pos = pos;
  }

  @XmlAttribute
  public String getPos() {
    return pos;
  }

  @Override
  public String toString() {
    return format("%s [%s]", getToken(), pos);
  }

}
