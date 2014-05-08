package edu.sdsc.scigraph.annotation;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class EntityAnnotationTest {

  EntityAnnotation annot1, annot2, annot3, annot4, annot5;

  @Before
  public void setUp() throws Exception {
    annot1 = new EntityAnnotation(null, -5, 6);
    annot2 = new EntityAnnotation(null, 1, 6);
    annot3 = new EntityAnnotation(null, 7, 20);
    annot4 = new EntityAnnotation(null, 0, 21);
    annot5 = new EntityAnnotation(null, 2, 3);
  }

  @Test
  public void testCompareTo() {
    List<EntityAnnotation> list = Lists.newArrayList(annot1, annot2, annot3, annot4, annot5);
    Collections.sort(list);
    List<EntityAnnotation> expected = newArrayList(annot5, annot2, annot1, annot3, annot4);
    assertThat(list, equalTo(expected));
  }

}
