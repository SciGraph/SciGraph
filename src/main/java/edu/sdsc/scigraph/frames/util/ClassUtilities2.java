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
package edu.sdsc.scigraph.frames.util;

import java.lang.reflect.Method;

import com.tinkerpop.frames.ClassUtilities;

public class ClassUtilities2 extends ClassUtilities {

  private static final String HAS = "has";

  public static boolean isHasMethod(final Method method) {
    return method.getName().startsWith(HAS);
  }

  public static boolean isIterable(final Object object) {
    return Iterable.class.isAssignableFrom(object.getClass());
  }

}
