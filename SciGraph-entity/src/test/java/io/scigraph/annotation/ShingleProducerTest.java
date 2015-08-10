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
package io.scigraph.annotation;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import io.scigraph.annotation.EntityAnalyzer;
import io.scigraph.annotation.ShingleProducer;
import io.scigraph.annotation.Token;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;

public class ShingleProducerTest {

  ShingleProducer producer;
  BlockingQueue<List<Token<String>>> queue;

  @Before
  public void setUp() throws Exception {
    String text = "a b c";
    queue = new LinkedBlockingQueue<List<Token<String>>>();
    producer = new ShingleProducer(new EntityAnalyzer(), new StringReader(text), queue);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddBufferToQueue() throws InterruptedException {
    Token<String> a = mock(Token.class);
    Token<String> b = mock(Token.class);
    Token<String> c = mock(Token.class);
    producer.addBufferToQueue(newArrayList(a, b, c));
    assertThat(queue,
        contains(newArrayList(a), newArrayList(a, b), (List<Token<String>>) newArrayList(a, b, c)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testShingleProduction() throws InterruptedException {
    Thread t = new Thread(producer, "ShingeProducerTestThread");
    t.start();

    List<List<Token<String>>> expected = new ArrayList<>();
    Token<String> a = new Token<String>("a", 0, 1);
    Token<String> b = new Token<String>("b", 2, 3);
    Token<String> c = new Token<String>("c", 4, 5);
    expected.add(newArrayList(a));
    expected.add(newArrayList(a, b));
    expected.add(newArrayList(a, b, c));
    expected.add(newArrayList(b));
    expected.add(newArrayList(b, c));
    expected.add(newArrayList(c));
    List<List<Token<String>>> actual = new ArrayList<>();
    while (true) {
      List<Token<String>> tokens = queue.take();
      if (tokens.equals(ShingleProducer.END_TOKEN)) {
        break;
      }
      actual.add(tokens);
    }
    assertThat(actual, is(equalTo(expected)));
  }

}
