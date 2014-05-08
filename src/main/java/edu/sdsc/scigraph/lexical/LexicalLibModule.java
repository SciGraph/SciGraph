package edu.sdsc.scigraph.lexical;

import com.google.inject.AbstractModule;

public final class LexicalLibModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(LexicalLib.class).to(LexicalLibOpenNlpImpl.class);
  }

}
