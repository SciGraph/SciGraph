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
package io.scigraph.cache;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.cache.Cache;

public class CacheInterceptor implements MethodInterceptor {

  @Inject
  Cache<CacheableMethodInvocation, Object> methodCache;

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    CacheableMethodInvocation cacheableInvocation = new CacheableMethodInvocation(invocation);
    Object result = methodCache.getIfPresent(cacheableInvocation);
    if (null == result) {
      result = invocation.proceed();
      methodCache.put(cacheableInvocation, result);
    }
    return result;
  }

}
