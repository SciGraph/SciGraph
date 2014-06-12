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
package edu.sdsc.scigraph.vocabulary;

import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyQueryAnalyzer;
import edu.sdsc.scigraph.neo4j.Graph;

public class VocabularyNeo4jImpl<N extends NodeProperties> implements Vocabulary<N> {

  private static final Logger logger = Logger.getLogger(VocabularyNeo4jImpl.class.getName());

  private final Graph<N> graph;
  private SpellChecker spellChecker;
  private final QueryParser parser;

  @Inject
  public VocabularyNeo4jImpl(Graph<N> graph, @Nullable @Named("neo4j.location") String neo4jLocation)
      throws IOException {
    this.graph = graph;
    if (null != neo4jLocation) {
      Directory indexDirectory = FSDirectory.open(new File(new File(neo4jLocation),
          "index/lucene/node/node_auto_index"));
      Directory spellDirectory = FSDirectory.open(new File(FileUtils.getTempDirectory(),
          "spellchecker"));
      spellChecker = new SpellChecker(spellDirectory);
      try (IndexReader reader = IndexReader.open(indexDirectory)) {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer());
        spellChecker.indexDictionary(new LuceneDictionary(reader, NodeProperties.LABEL
            + LuceneUtils.EXACT_SUFFIX), config, true);
      }
    }
    parser = new AnalyzingQueryParser(Version.LUCENE_36, NodeProperties.LABEL,
        new VocabularyQueryAnalyzer());
  }

  static String formatQuery(String format, Object... args) {
    return format(format, transform(newArrayList(args), new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        return (input instanceof String) ? QueryParser.escape((String) input).replaceAll(" ",
            "\\\\ ") : input;
      }
    }).toArray());
  }

  static void addCommonConstraints(BooleanQuery indexQuery, Query query) {
    BooleanQuery categoryQueries = new BooleanQuery();
    for (String category : query.getCategories()) {
      categoryQueries.add(new TermQuery(new Term(Concept.CATEGORY, category)), Occur.SHOULD);
    }
    if (!query.getCategories().isEmpty()) {
      indexQuery.add(new BooleanClause(categoryQueries, Occur.MUST));
    }

    BooleanQuery ontoloogyQueries = new BooleanQuery();
    for (String ontology : query.getOntologies()) {
      ontoloogyQueries.add(new TermQuery(new Term(CommonProperties.ONTOLOGY, ontology)),
          Occur.SHOULD);
    }
    if (!query.getOntologies().isEmpty()) {
      indexQuery.add(new BooleanClause(ontoloogyQueries, Occur.MUST));
    }
  }

  // TODO: Can this be done in the query?
  List<N> limitHits(IndexHits<Node> hits, Query query) {
    return newArrayList(limit(graph.getOrCreateFramedNodes(hits), query.getLimit()));
  }

  @Override
  public Optional<N> getConceptFromUri(String uri) {
    return graph.getFramedNode(uri);
  }

  @Override
  public Collection<N> getConceptFromId(Query query) {
    String idQuery = StringUtils.strip(query.getInput(), "\"");
    TermQuery fragmentQuery = new TermQuery(new Term(CommonProperties.FRAGMENT, idQuery));
    TermQuery curieQuery = new TermQuery(new Term(CommonProperties.CURIE, idQuery));
    BooleanQuery finalQuery = new BooleanQuery();
    finalQuery.add(fragmentQuery, Occur.SHOULD);
    finalQuery.add(curieQuery, Occur.SHOULD);
    IndexHits<Node> hits = graph.getNodeAutoIndex().query(finalQuery);
    return limitHits(hits, query);
  }

  @Override
  public List<N> getConceptsFromPrefix(Query query) {
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      BooleanQuery subQuery = new BooleanQuery();
      subQuery.add(parser.parse(formatQuery("%s%s:%s*", NodeProperties.LABEL,
          LuceneUtils.EXACT_SUFFIX, query.getInput())), Occur.SHOULD);
      subQuery.add(parser.parse(formatQuery("%s:%s*", NodeProperties.CURIE, query.getInput())),
          Occur.SHOULD);
      subQuery.add(parser.parse(formatQuery("%s:%s*", NodeProperties.FRAGMENT, query.getInput())),
          Occur.SHOULD);

      if (query.isIncludeSynonyms()) {
        subQuery.add(
            parser.parse(formatQuery("%s%s:%s*", Concept.SYNONYM, LuceneUtils.EXACT_SUFFIX,
                query.getInput())), Occur.SHOULD);
      }
      finalQuery.add(subQuery, Occur.MUST);
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parser query", e);
    }
    addCommonConstraints(finalQuery, query);
    IndexHits<Node> hits = graph.getNodeAutoIndex().query(finalQuery);
    return limitHits(hits, query);
  }

  @Override
  public List<N> searchConcepts(Query query) {
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      if (query.isIncludeSynonyms()) {
        BooleanQuery subQuery = new BooleanQuery();
        subQuery.add(LuceneUtils.getBoostedQuery(parser, query.getInput(), 10.0f), Occur.SHOULD);
        subQuery.add(parser.parse(Concept.SYNONYM + ":" + query.getInput()), Occur.SHOULD);
        finalQuery.add(subQuery, Occur.MUST);
      } else {
        finalQuery.add(parser.parse(query.getInput()), Occur.MUST);
      }
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parser query", e);
    }
    addCommonConstraints(finalQuery, query);
    IndexHits<Node> hits = graph.getNodeAutoIndex().query(finalQuery);
    return limitHits(hits, query);
  }

  @Override
  public List<N> getConceptsFromTerm(Query query) {
    String exactQuery = String.format("\"\\^ %s $\"", query.getInput());
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      if (query.isIncludeSynonyms()) {
        BooleanQuery subQuery = new BooleanQuery();
        subQuery.add(LuceneUtils.getBoostedQuery(parser, exactQuery, 10.0f), Occur.SHOULD);
        subQuery.add(parser.parse(Concept.SYNONYM + ":" + exactQuery), Occur.SHOULD);
        finalQuery.add(subQuery, Occur.MUST);
      } else {
        finalQuery.add(parser.parse(exactQuery), Occur.MUST);
      }
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parser query", e);
    }
    addCommonConstraints(finalQuery, query);
    logger.finest(finalQuery.toString());
    IndexHits<Node> hits = graph.getNodeAutoIndex().query(finalQuery);
    return limitHits(hits, query);
  }

  @Override
  public Set<String> getAllCategories() {
    return Suppliers.memoize(new Supplier<Set<String>>() {
      @Override
      public Set<String> get() {
        ExecutionResult result = graph.getExecutionEngine().execute(
            "START n = node(*) WHERE has(n.category) RETURN distinct(n.category)");
        Set<String> categories = new HashSet<>();
        while (result.iterator().hasNext()) {
          Map<String, Object> col = result.iterator().next();
          Object category = col.get("(n.category)");
          if (category.getClass().isArray()) {
            for (String cat : (String[]) category) {
              categories.add(cat);
            }
          } else {
            categories.add((String) col.get("(n.category)"));
          }
        }
        return categories;
      }
    }).get();
  }

  @Override
  public Set<String> getAllOntologies() {
    return Suppliers.memoize(new Supplier<Set<String>>() {
      @Override
      public Set<String> get() {
        ExecutionResult result = graph.getExecutionEngine().execute(
            "START n = node(*) WHERE has(n.ontology) RETURN distinct(n.ontology)");
        Set<String> ontologies = new HashSet<>();
        while (result.iterator().hasNext()) {
          Map<String, Object> col = result.iterator().next();
          ontologies.add((String) col.get("(n.ontology)"));
        }
        return ontologies;
      }
    }).get();
  }

  @Override
  public List<String> getSuggestions(String query) {
    try {
      return newArrayList(spellChecker.suggestSimilar(query, 5));
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to get spelling suggestions", e);
      return Collections.emptyList();
    }
  }

}
