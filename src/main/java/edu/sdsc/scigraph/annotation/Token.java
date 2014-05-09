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
package edu.sdsc.scigraph.annotation;

import static java.lang.String.format;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Range;

@XmlRootElement
public class Token<T> {

  protected final T token;
  protected final Range<Integer> range;

  Token() {
    this(null, 0, 0);
  }

  public Token(T token, int start, int end) {
    this.token = token;
    this.range = Range.closed(start, end);
  }

  public T getToken() {
    return token;
  }

  @XmlAttribute
  public int getStart() {
    return range.lowerEndpoint();
  }

  @XmlAttribute
  public int getEnd() {
    return range.upperEndpoint();
  }

  @Override
  public String toString() {
    return format("%s %s", getToken().toString(), range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, range);
  }

  @Override
  public boolean equals(Object obj) {
    if (null == obj) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Token<?> other = (Token<?>) obj;
    return Objects.equals(this.token, other.token)
        && Objects.equals(this.range, other.range);
  }

}
