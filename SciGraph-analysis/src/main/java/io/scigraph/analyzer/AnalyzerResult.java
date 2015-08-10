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
package io.scigraph.analyzer;

import java.util.Objects;

public final class AnalyzerResult {
  private final String iri;
  private final double pValue;
  private final String labels;

  public AnalyzerResult(String labels, String iri, double pValue) {
    this.labels = labels;
    this.iri = iri;
    this.pValue = pValue;
  }
  
  public String getLabels() {
    return labels;
  }

  public String getIri() {
    return iri;
  }

  public double getpValue() {
    return pValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(iri);
  }

  @Override
  public boolean equals(Object obj) {
    if (null == obj) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(getIri(), ((AnalyzerResult) obj).getIri());
  }

  @Override
  public String toString() {
    return labels + " " + iri + " (" + pValue + ")";
  }
}
