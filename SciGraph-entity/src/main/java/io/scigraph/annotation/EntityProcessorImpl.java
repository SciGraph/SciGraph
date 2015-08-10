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
package io.scigraph.annotation;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import io.scigraph.lucene.LuceneUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.inject.Inject;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.StreamedSource;

import org.apache.lucene.analysis.Analyzer;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

class EntityProcessorImpl implements EntityProcessor {

  private static final Logger logger = Logger.getLogger(EntityProcessorImpl.class.getName());

  private final Analyzer analyzer;
  private final EntityRecognizer recognizer;

  @Inject
  protected EntityProcessorImpl(EntityRecognizer recognizer) {
    this.recognizer = recognizer;
    analyzer = new EntityAnalyzer();
  }

  BlockingQueue<List<Token<String>>> startShingleProducer(String content) {
    BlockingQueue<List<Token<String>>> queue = new LinkedBlockingQueue<List<Token<String>>>();
    ShingleProducer producer = new ShingleProducer(analyzer, new StringReader(content), queue);
    Thread t = new Thread(producer, "Shingle Producer Thread");
    t.start();
    return queue;
  }

  String combineTokens(List<Token<String>> tokens) {
    return on(' ').join(transform(tokens, new Function<Token<String>, String>() {
      @Override
      public String apply(Token<String> input) {
        return input.getToken();
      }
    }));
  }

  protected List<EntityAnnotation> getAnnotations(String content, EntityFormatConfiguration config)
      throws InterruptedException {
    checkNotNull(content);
    BlockingQueue<List<Token<String>>> queue = startShingleProducer(content);
    List<EntityAnnotation> annotations = new ArrayList<>();
    while (true) {
      List<Token<String>> tokens = queue.take();
      if (tokens.equals(ShingleProducer.END_TOKEN)) {
        break;
      }
      if (LuceneUtils.isStopword(getFirst(tokens, null).getToken())
          || LuceneUtils.isStopword(getLast(tokens).getToken())) {
        continue;
      }

      String candidate = combineTokens(tokens);
      if (candidate.length() < config.getMinLength()) {
        continue;
      }
      int start = tokens.get(0).getStart();
      int end = tokens.get(tokens.size() - 1).getEnd();
      for (Entity entity : recognizer.getEntities(candidate, config)) {
        annotations.add(new EntityAnnotation(entity, start, end));
      }
    }

    List<EntityAnnotation> ret = newArrayList();
    for (EntityAnnotationGroup group : getAnnotationGroups(annotations, config.isLongestOnly())) {
      ret.addAll(group);
    }
    Collections.sort(ret);
    return ret;
  }

  /***
   * Convert a list of annotations into annotation groups
   * 
   * @param annotationList
   *          Annotations
   * @param longestOnly
   *          If shorter entities from annotation groups should be removed
   * @return annotation groups
   */
  static List<EntityAnnotationGroup> getAnnotationGroups(List<EntityAnnotation> annotationList,
      boolean longestOnly) {
    List<EntityAnnotationGroup> groups = new ArrayList<>();
    Collections.sort(annotationList, Collections.reverseOrder());
    PeekingIterator<EntityAnnotation> iter = Iterators.peekingIterator(annotationList.iterator());
    while (iter.hasNext()) {
      EntityAnnotationGroup group = new EntityAnnotationGroup();
      group.add(iter.next());
      Set<Entity> entitiesInGroup = new HashSet<>();
      while (iter.hasNext() && group.intersects(iter.peek())) {
        if (!entitiesInGroup.contains(iter.peek().getToken())) {
          entitiesInGroup.add(iter.peek().getToken());
          group.add(iter.next());
        } else {
          iter.next();
        }
      }

      if (longestOnly) {
        // Remove any entries that aren't as long as the first one
        Iterator<EntityAnnotation> groupIter = group.iterator();
        int longest = group.peek().length();
        while (groupIter.hasNext()) {
          EntityAnnotation annot = groupIter.next();
          if (annot.length() < longest) {
            groupIter.remove();
          }
        }
      }

      groups.add(group);
    }

    return groups;
  }

  /***
   * Add markup based on the annotations...<br />
   * TODO: make the markup configurable so as not to limit ourselves to html
   * 
   * @param content
   * @return A String with embedded markup
   * @throws IOException
   */
  protected final String insertSpans(List<EntityAnnotation> annotationList, String content,
      EntityFormatConfiguration config) throws IOException {
    StringBuilder buffer = new StringBuilder(content);

    Set<String> terms = newHashSet();
    Set<String> attrs = newHashSet();
    Set<String> cssClasses = newHashSet();
    for (EntityAnnotationGroup group : getAnnotationGroups(annotationList, config.isLongestOnly())) {
      cssClasses.clear();
      attrs.clear();
      terms.clear();
      for (Entity entity : group.getAnnotations()) {
        String serialized = entity.serialize();
        if (!isNullOrEmpty(serialized)) {
          terms.add(serialized);
        }
        cssClasses.add(recognizer.getCssClass());
      }

      if (!terms.isEmpty()) {
        attrs.add(format("%s=\"%s\"", config.getDataAttrName(), on("|").join(terms)));
      }

      buffer.insert(group.getEnd(), "</span>");
      buffer.insert(group.getStart(),
          format("<span class=\"%s\" %s>", on(" ").join(cssClasses), on(" ").join(attrs)));
    }
    return buffer.toString();
  }

  /***
   * @param url
   * @return The appropriate base s.t. relative URLs can resolve
   */
  static String getBase(URL url) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(url.getProtocol());
    buffer.append("://");
    buffer.append(url.getHost());
    if (url.getPort() > 0 && 80 != url.getPort()) {
      buffer.append(':');
      buffer.append(url.getPort());
    }
    if (!isNullOrEmpty(url.getPath()) && url.getPath().contains("/")) {
      String path = url.getPath();
      buffer.append(path.substring(0, path.lastIndexOf("/")));
    }
    buffer.append('/');
    return buffer.toString();
  }

  private static void injectStyles(Writer writer, List<String> styles) throws IOException {
    for (String style : styles) {
      writer
          .write(format("<link rel=\"stylesheet\" style=\"text/css\" href=\"%s\"></link>", style));
    }
  }

  private static void injectScripts(Writer writer, List<String> scripts) throws IOException {
    for (String script : scripts) {
      writer.write(format(
          "<script type=\"text/javascript\" language=\"javascript\" src=\"%s\"></script>", script));
    }
  }

  boolean shouldAnnotate(LinkedList<Element> eltStack, EntityFormatConfiguration config) {
    boolean shouldAnnotate = false;
    // If the user hasn't asked for anything - default to annotate everything
    if (config.getTargetClasses().isEmpty() && config.getTargetIds().isEmpty()) {
      shouldAnnotate = true;
    }
    for (Element tag : eltStack) {
      if (config.getIgnoreTags().contains(tag.getName().toLowerCase())) {
        // If an ignore tag is encountered in the stack stop...
        shouldAnnotate = false;
        break;
      }

      if (tag.containsKey("id") && config.getTargetIds().contains(tag.get("id"))) {
        shouldAnnotate = true;
      }

      if (tag.containsKey("class")) {
        for (String clazz : tag.get("class").split("\\s+")) {
          if (!clazz.isEmpty() && config.getTargetClasses().contains(clazz)) {
            shouldAnnotate = true;
          }
        }
      }

    }
    return shouldAnnotate;
  }

  @Override
  public List<EntityAnnotation> annotateEntities(EntityFormatConfiguration config)
      throws IOException {
    checkNotNull(config);

    try (StreamedSource source = new StreamedSource(config.getReader())) {
      LinkedList<Element> eltStack = new LinkedList<Element>();

      List<EntityAnnotation> entities = newArrayList();

      for (Segment segment : source) {
        if (segment instanceof StartTag) {
          StartTag tag = (StartTag) segment;
          config.getWriter().write(segment.toString());

          if (tag.getTagType() == StartTagType.NORMAL &&
          // Jericho is not generating end events for minimized element - don't
          // add them to the stack
              !tag.toString().endsWith("/>") && !tag.toString().endsWith("/ >")) {
            eltStack.push(new Element(tag.getName(), tag.getAttributes()));
          }

          if (config.getUrl() != null && HTMLElementName.HEAD.equals(tag.getName())) {
            config.getWriter().write("<base href=\"" + getBase(config.getUrl()) + "\"></base>");
            injectStyles(config.getWriter(), config.getStylesheets());
            injectScripts(config.getWriter(), config.getScripts());
          }
        } else if (segment instanceof EndTag) {
          config.getWriter().write(segment.toString());
          eltStack.pop();
        } else if (segment.getClass().equals(Segment.class)) {
          if (shouldAnnotate(eltStack, config)) {
            try {
              List<EntityAnnotation> annotationList = getAnnotations(segment.toString(), config);

              entities.addAll(annotationList);
              config.getWriter().write(insertSpans(annotationList, segment.toString(), config));
            } catch (IOException e) {
              config.getWriter().write(segment.toString());
              logger.warning(e.getMessage());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          } else {
            config.getWriter().write(segment.toString());
          }
        }
      }

      List<EntityAnnotation> ret = newArrayList();
      for (EntityAnnotationGroup group : getAnnotationGroups(entities, config.isLongestOnly())) {
        ret.addAll(group);
      }
      Collections.sort(ret);
      return ret;
    }
  }

  private static class Element extends ForwardingMap<String, String> {

    String name;

    Map<String, String> attributes = newHashMap();

    Element(String name, Iterable<Attribute> attrs) {
      this.name = name;
      for (Attribute attr : attrs) {
        put(attr.getName(), attr.getValue());
      }
    }

    String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name + " {" + attributes + "}";
    }

    @Override
    protected Map<String, String> delegate() {
      return attributes;
    }

  }

}
