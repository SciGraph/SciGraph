package edu.sdsc.scigraph.annotation;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

public class EntityRecognizerTest {

  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);
  Concept concept = mock(Concept.class);
  EntityRecognizer recognizer;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    Vocabulary<Concept> vocabulary = mock(Vocabulary.class);
    when(concept.getLabel()).thenReturn("foo");
    when(concept.getCategories()).thenReturn(Collections.<String> emptySet());
    when(vocabulary.getConceptsFromTerm(any(Query.class))).thenReturn(singletonList(concept));
    recognizer = new EntityRecognizer(vocabulary);
  }

  @Test
  public void testKnownEntity() {
    Collection<Entity> entities = recognizer.getEntities("foo", config);
    assertThat(entities, contains(new Entity(concept)));
  }

}
