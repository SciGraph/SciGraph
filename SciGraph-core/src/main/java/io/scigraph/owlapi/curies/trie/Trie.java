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
 * Data structure specific for characters, to efficiently resolve IRI prefixes to CURIEs prefixes.
 *
 */
public class Trie {
  private final TrieNode root;

  public Trie() {
    root = new TrieNode((char) 0);
  }

  
  /**
   * @param word to insert into the Trie
   */
  public void insert(String word) {

    // Find length of the given word
    int length = word.length();
    TrieNode crawl = root;

    // Traverse through all characters of given word
    for (int level = 0; level < length; level++) {
      HashMap<Character, TrieNode> child = crawl.getChildren();
      char ch = word.charAt(level);

      // If there is already a child for current character of given word
      if (child.containsKey(ch))
        crawl = child.get(ch);
      else // Else create a child
      {
        TrieNode temp = new TrieNode(ch);
        child.put(ch, temp);
        crawl = temp;
      }
    }

    // Set isLeaf true for last character
    crawl.setIsLeaf(true);
  }

  /**
   * @param input
   * @return the longest matching prefix
   */
  public String getMatchingPrefix(String input) {
    String result = ""; // Initialize resultant string
    int length = input.length(); // Find length of the input string

    // Initialize reference to traverse through Trie
    TrieNode crawl = root;

    // Iterate through all characters of input string 'str' and traverse
    // down the Trie
    int level, prevMatch = 0;
    for (level = 0; level < length; level++) {
      // Find current character of str
      char ch = input.charAt(level);

      // HashMap of current Trie node to traverse down
      HashMap<Character, TrieNode> child = crawl.getChildren();

      // See if there is a Trie edge for the current character
      if (child.containsKey(ch)) {
        result += ch; // Update result
        crawl = child.get(ch); // Update crawl to move down in Trie

        // If this is end of a word, then update prevMatch
        if (crawl.isLeaf())
          prevMatch = level + 1;
      } else
        break;
    }

    // If the last processed character did not match end of a word,
    // return the previously matching prefix
    if (!crawl.isLeaf())
      return result.substring(0, prevMatch);

    else
      return result;
  }

}
