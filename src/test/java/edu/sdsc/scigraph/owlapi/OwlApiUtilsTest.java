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
package edu.sdsc.scigraph.owlapi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDatatype;

import edu.sdsc.scigraph.owlapi.OwlApiUtils;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplBoolean;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplDouble;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplFloat;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplInteger;

public class OwlApiUtilsTest {

  OWLDatatype type;

  @Before
  public void setup() {
    type = mock(OWLDatatype.class);
  }

  @Test
  public void testGetBooleanTypedLiteral() {
    OWLLiteralImplBoolean bool = new OWLLiteralImplBoolean(false);
    assertThat(OwlApiUtils.getTypedLiteralValue(bool).get(), IsInstanceOf.instanceOf(Boolean.class));
    assertThat((Boolean)OwlApiUtils.getTypedLiteralValue(bool).get(), is(false));
  }

  @Test
  public void testGetNumericTypedLiterals() {
    OWLLiteralImplDouble doub = new OWLLiteralImplDouble(3.14, type);
    assertThat((Double)OwlApiUtils.getTypedLiteralValue(doub).get(), is(equalTo(3.14)));
    OWLLiteralImplFloat flt = new OWLLiteralImplFloat(3.14f, type);
    assertThat((Float)OwlApiUtils.getTypedLiteralValue(flt).get(), is(equalTo(3.14f)));
    OWLLiteralImplInteger i = new OWLLiteralImplInteger(3, type);
    assertThat((Integer)OwlApiUtils.getTypedLiteralValue(i).get(), is(equalTo(3)));
  }

  @Test
  public void testGetStringTypedLiterals() {
    OWLLiteralImpl literal = new OWLLiteralImpl("hello", null, type);
    assertThat((String)OwlApiUtils.getTypedLiteralValue(literal).get(), is(equalTo("hello")));
  }

  @Test
  public void testLiteralLanguages() {
    OWLLiteralImpl literalEnLang = new OWLLiteralImpl("hello", "en", null);
    OWLLiteralImpl literalEsLang = new OWLLiteralImpl("hola", "es", null);
    assertThat((String)OwlApiUtils.getTypedLiteralValue(literalEnLang).get(), is(equalTo("hello")));
    assertThat(OwlApiUtils.getTypedLiteralValue(literalEsLang).isPresent(), is(false));
  }

  @Test
  public void getUri() throws URISyntaxException {
    OWLClassExpression expression = mock(OWLClassExpression.class, Mockito.RETURNS_DEEP_STUBS);
    when(expression.isAnonymous()).thenReturn(false);
    when(expression.asOWLClass().getIRI().toURI()).thenReturn(new URI("http://example.org/Thing"));
    assertThat(new URI("http://example.org/Thing"), is(equalTo(OwlApiUtils.getUri(expression))));
    when(expression.isAnonymous()).thenReturn(true);
    assertThat(new URI("http://ontology.neuinfo.org/anon/" + expression.hashCode()), is(equalTo(OwlApiUtils.getUri(expression))));
  }

}
