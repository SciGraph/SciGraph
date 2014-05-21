package edu.sdsc.scigraph.lucene;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class LuceneUtilsTest {

  @Test
  public void testIsAllStopwords() {
    assertThat(LuceneUtils.isAllStopwords(newArrayList("the", "a")), is(true));
    assertThat(LuceneUtils.isAllStopwords(newArrayList("the", "cat")), is(false));
  }

}
