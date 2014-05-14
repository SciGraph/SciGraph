/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.lexical;

import java.util.List;

import edu.sdsc.scigraph.annotation.Token;
import edu.sdsc.scigraph.lexical.pos.PosToken;

public interface LexicalLib {

  /***
   * @param text A text fragment with multiple sentences
   * @return A list of the sentences found in the text
   */
  public abstract List<String> extractSentences(String text);

  /***
   * @param sentence A single sentence.
   * @return A string with POS annotations
   */
  public abstract List<PosToken> tagPOS(String sentence);

  /***
   * @param text The text to chunk
   * @return a list of phrase chunks recognized in the sentence
   */
  public abstract List<Token<String>> getChunks(String text);

  /***
   * Attempt to recognize entities (not chunks) in text.
   * @param text A section of text
   * @return A list of recognized entities
   */
  public abstract List<Token<String>> getEntities(String text);

}
