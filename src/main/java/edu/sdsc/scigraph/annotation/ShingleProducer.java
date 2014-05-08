package edu.sdsc.scigraph.annotation;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/***
 * Abstracts shingle functionality for an arbitrary Lucene Analyzer.
 * <p>
 * Queues the shingles into a blocking queue so that tokenization and processing
 * can happen concurrently.
 */
@NotThreadSafe
public class ShingleProducer implements Runnable {

  private final static Logger logger = Logger.getLogger(ShingleProducer.class.getName());

  final static int DEFAULT_SHINGLE_COUNT = 4;

  private final Analyzer analyzer;
  private final Reader reader;
  private final BlockingQueue<List<Token<String>>> queue;
  private final int shingleCount;

  /***
   * This is the token that indicates that processing has finished...
   */
  public final static List<Token<String>> END_TOKEN = Collections.emptyList();

  public ShingleProducer(Analyzer analyzer, Reader reader, BlockingQueue<List<Token<String>>> queue) {
    this(analyzer, reader, queue, DEFAULT_SHINGLE_COUNT);
  }

  public ShingleProducer(Analyzer analyzer, Reader reader,
      BlockingQueue<List<Token<String>>> queue, int shingleCount) {
    this.analyzer = analyzer;
    this.reader = reader;
    this.queue = queue;
    this.shingleCount = shingleCount;
  }

  /***
   * Add all subsequences of buffer to the queue. If the buffer is [A, B, C]
   * then add A, AB, and ABC to the queue.
   * 
   * @param buffer
   * @throws InterruptedException
   */
  void addBufferToQueue(Iterable<Token<String>> buffer) throws InterruptedException {
    List<Token<String>> tokenList = new ArrayList<>();

    for (Token<String> token : buffer) {
      tokenList.add(token);
      queue.put(new ArrayList<>(tokenList));
    }
  }

  @Override
  public void run() {
    Deque<Token<String>> buffer = new LinkedList<>();
    try {
      TokenStream stream = analyzer.tokenStream("", reader);
      OffsetAttribute offset = stream.getAttribute(OffsetAttribute.class);
      CharTermAttribute term = stream.getAttribute(CharTermAttribute.class);

      try {
        while (stream.incrementToken()) {
          Token<String> token = new Token<String>(term.toString(), offset.startOffset(),
              offset.endOffset());
          buffer.offer(token);
          if (buffer.size() < shingleCount) {
            // Fill the buffer first, before offering anything to the queue
            continue;
          }
          addBufferToQueue(buffer);
          if (shingleCount == buffer.size()) {
            buffer.pop();
          }
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to produces singles", e);
      }
      while (!buffer.isEmpty()) {
        addBufferToQueue(buffer);
        buffer.pop();
      }
      queue.put(END_TOKEN);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
