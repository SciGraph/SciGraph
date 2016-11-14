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
package io.scigraph.owlapi.curies.trie;

import java.util.HashMap;

/**
 * Represents a Node in a Trie.
 *
 */
class TrieNode {
  private char value;
  private HashMap<Character, TrieNode> children;
  private boolean isLeaf;

  public TrieNode(char ch) {
    value = ch;
    children = new HashMap<>();
    isLeaf = false;
  }

  public HashMap<Character, TrieNode> getChildren() {
    return children;
  }

  public char getValue() {
    return value;
  }

  public void setIsLeaf(boolean val) {
    isLeaf = val;
  }

  public boolean isLeaf() {
    return isLeaf;
  }
}
