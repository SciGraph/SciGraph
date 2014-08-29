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
package edu.sdsc.scigraph.owlapi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

public class OwlApiUtilsTest {

  OWLDataFactory factory;

  @Before
  public void setup() {
    factory = OWLManager.getOWLDataFactory();
  }

  @Test
  public void testGetBooleanTypedLiteral() {
    OWLLiteral bool = factory.getOWLLiteral(false);
    assertThat(OwlApiUtils.getTypedLiteralValue(bool).get(), IsInstanceOf.instanceOf(Boolean.class));
    assertThat((Boolean)OwlApiUtils.getTypedLiteralValue(bool).get(), is(false));
  }

  @Test
  public void testGetNumericTypedLiterals() {
    OWLLiteral doub = factory.getOWLLiteral(3.14);
    assertThat((Double) OwlApiUtils.getTypedLiteralValue(doub).get(), is(3.14));
    OWLLiteral flt = factory.getOWLLiteral(3.14f);
    assertThat((Float) OwlApiUtils.getTypedLiteralValue(flt).get(), is(3.14f));
    OWLLiteral i = factory.getOWLLiteral(3);
    assertThat((Integer) OwlApiUtils.getTypedLiteralValue(i).get(), is(3));
  }

  @Test
  public void testGetStringTypedLiterals() {
    OWLLiteral literal = factory.getOWLLiteral("hello", "en");
    assertThat((String) OwlApiUtils.getTypedLiteralValue(literal).get(), is("hello"));
  }

  @Test
  public void testLiteralLanguages() {
    OWLLiteral literalEnLang = factory.getOWLLiteral("hello", "en");
    OWLLiteral literalEsLang = factory.getOWLLiteral("hello", "es");
    assertThat((String) OwlApiUtils.getTypedLiteralValue(literalEnLang).get(), is("hello"));
    assertThat(OwlApiUtils.getTypedLiteralValue(literalEsLang).isPresent(), is(false));
  }

  @Test
  public void getUri() throws URISyntaxException {
    OWLClassExpression expression = mock(OWLClassExpression.class, Mockito.RETURNS_DEEP_STUBS);
    when(expression.isAnonymous()).thenReturn(false);
    when(expression.asOWLClass().getIRI().toURI()).thenReturn(new URI("http://example.org/Thing"));
    assertThat(new URI("http://example.org/Thing"), is(OwlApiUtils.getUri(expression)));
    when(expression.isAnonymous()).thenReturn(true);
    assertThat(new URI("http://ontology.neuinfo.org/anon/" + expression.hashCode()),
        is(OwlApiUtils.getUri(expression)));
  }

}
