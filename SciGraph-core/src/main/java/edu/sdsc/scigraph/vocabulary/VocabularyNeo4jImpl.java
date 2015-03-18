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

import static com.google.common.collect.Iterables.filter;
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

import org.apache.commons.lang3.StringUtils;
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
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyQueryAnalyzer;
import edu.sdsc.scigraph.neo4j.NodeTransformer;
import edu.sdsc.scigraph.neo4j.bindings.IndicatesNeo4jGraphLocation;
import edu.sdsc.scigraph.owlapi.CurieUtil;

public class VocabularyNeo4jImpl implements Vocabulary {

  private static final Logger logger = Logger.getLogger(VocabularyNeo4jImpl.class.getName());

  private final GraphDatabaseService graph;
  private final ExecutionEngine engine;
  private final SpellChecker spellChecker;
  private final CurieUtil curieUtil;
  private final NodeTransformer transformer;
  
  @Inject
  public VocabularyNeo4jImpl(GraphDatabaseService graph, @Nullable @IndicatesNeo4jGraphLocation String neo4jLocation,
      CurieUtil curieUtil, NodeTransformer transformer) throws IOException {
    this.graph = graph;
    engine = new ExecutionEngine(graph);
    this.curieUtil = curieUtil;
    this.transformer = transformer;
    if (null != neo4jLocation) {
      Directory indexDirectory =
          FSDirectory.open(new File(new File(neo4jLocation), "index/lucene/node/node_auto_index"));
      Directory spellDirectory =
          FSDirectory.open(new File(new File(neo4jLocation), "index/lucene/spellchecker"));
      spellChecker = new SpellChecker(spellDirectory);
      try (IndexReader reader = IndexReader.open(indexDirectory)) {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer());
        spellChecker.indexDictionary(new LuceneDictionary(reader, NodeProperties.LABEL
            + LuceneUtils.EXACT_SUFFIX), config, true);
      }
    } else {
      spellChecker = null;
    }
  }

  static QueryParser getQueryParser() {
    return new AnalyzingQueryParser(Version.LUCENE_36, NodeProperties.LABEL,
        new VocabularyQueryAnalyzer());
  }

  static String formatQuery(String format, Object... args) {
    return format(format, transform(newArrayList(args), new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        return input instanceof String ? QueryParser.escape((String) input)
            .replaceAll(" ", "\\\\ ") : input;
      }
    }).toArray());
  }

  void addCommonConstraints(BooleanQuery indexQuery, Query query) {
    BooleanQuery categoryQueries = new BooleanQuery();
    for (String category : query.getCategories()) {
      categoryQueries.add(new TermQuery(new Term(Concept.CATEGORY, category)), Occur.SHOULD);
    }
    if (!query.getCategories().isEmpty()) {
      indexQuery.add(new BooleanClause(categoryQueries, Occur.MUST));
    }

    BooleanQuery prefixQueries = new BooleanQuery();
    for (String curie : query.getPrefixes()) {
      String prefix = curieUtil.getExpansion(curie);
      prefixQueries.add(new WildcardQuery(new Term(CommonProperties.URI, prefix + "*")), Occur.SHOULD);
    }
    if (!query.getPrefixes().isEmpty()) {
      indexQuery.add(new BooleanClause(prefixQueries, Occur.MUST));
    }
  }

  List<Concept> limitHits(IndexHits<Node> hits, Query query) {
    try (Transaction tx = graph.beginTx()) {
      Iterable<Concept> concepts = Iterables.transform(hits, transformer);
      if (!query.isIncludeDeprecated()) {
        concepts = filter(concepts, new Predicate<Concept>() {
          @Override
          public boolean apply(Concept concept) {
            return !concept.isDeprecated();
          }});
      }
      Iterable<Concept> limitedHits = limit(concepts, query.getLimit());
      List<Concept> ret = newArrayList(limitedHits);
      tx.success();
      return ret;
    }
  }

  @Override
  public Optional<Concept> getConceptFromUri(String uri) {
    try (Transaction tx = graph.beginTx()) {
      Node node = graph.index().getNodeAutoIndexer().getAutoIndex().get(CommonProperties.URI, uri.toString()).getSingle();
      tx.success();
      Concept concept = null;
      if (null != node) {
        concept = transformer.apply(node);
      }
      return Optional.fromNullable(concept);
    }
  }

  @Override
  public Collection<Concept> getConceptFromId(Query query) {
    QueryParser parser = getQueryParser();
    String idQuery = StringUtils.strip(query.getInput(), "\"");
    Optional<String> fullUri = curieUtil.getIri(idQuery);
    idQuery = QueryParser.escape(idQuery);

    String queryString = format("%s:%s", CommonProperties.FRAGMENT, idQuery);
    if (fullUri.isPresent()) {
      queryString +=
          String.format(" %s:%s", CommonProperties.URI, QueryParser.escape(fullUri.get()));
    }
    IndexHits<Node> hits;
    try (Transaction tx = graph.beginTx()) {
      hits = graph.index().getNodeAutoIndexer().getAutoIndex().query(parser.parse(queryString));
      tx.success();
      return limitHits(hits, query);
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parse an ID query", e);
      return Collections.emptySet();
    }

  }

  @Override
  public List<Concept> getConceptsFromPrefix(Query query) {
    QueryParser parser = getQueryParser();
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      BooleanQuery subQuery = new BooleanQuery();
      subQuery.add(parser.parse(formatQuery("%s%s:%s*", NodeProperties.LABEL,
          LuceneUtils.EXACT_SUFFIX, query.getInput())), Occur.SHOULD);
      Optional<String> fullUri = curieUtil.getIri(query.getInput());
      if (fullUri.isPresent()) {
        subQuery.add(parser.parse(formatQuery("%s:%s*", NodeProperties.URI, (fullUri.get()))),
            Occur.SHOULD);
      }
      subQuery.add(parser.parse(formatQuery("%s:%s*", NodeProperties.FRAGMENT, query.getInput())),
          Occur.SHOULD);

      if (query.isIncludeSynonyms()) {
        subQuery.add(
            parser.parse(formatQuery("%s%s:%s*", Concept.SYNONYM, LuceneUtils.EXACT_SUFFIX,
                query.getInput())), Occur.SHOULD);
      }
      if (query.isIncludeAbbreviations()) {
        subQuery.add(
            parser.parse(formatQuery("%s%s:%s*", Concept.ABREVIATION, LuceneUtils.EXACT_SUFFIX,
                query.getInput())), Occur.SHOULD);
      }
      if (query.isIncludeAcronyms()) {
        subQuery.add(
            parser.parse(formatQuery("%s%s:%s*", Concept.ACRONYM, LuceneUtils.EXACT_SUFFIX,
                query.getInput())), Occur.SHOULD);
      }
      
      finalQuery.add(subQuery, Occur.MUST);
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parse query", e);
    }
    addCommonConstraints(finalQuery, query);
    IndexHits<Node> hits = null;
    try (Transaction tx = graph.beginTx()) {
      hits = graph.index().getNodeAutoIndexer().getAutoIndex().query(finalQuery);
      tx.success();
    }
    return limitHits(hits, query);

  }

  @Override
  public List<Concept> searchConcepts(Query query) {
    QueryParser parser = getQueryParser();
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      if (query.isIncludeSynonyms() || query.isIncludeAbbreviations() || query.isIncludeAcronyms()) {
        BooleanQuery subQuery = new BooleanQuery();
        subQuery.add(LuceneUtils.getBoostedQuery(parser, query.getInput(), 10.0f), Occur.SHOULD);
        if (query.isIncludeSynonyms()) {
          subQuery.add(parser.parse(Concept.SYNONYM + ":" + query.getInput()), Occur.SHOULD);
        }
        if (query.isIncludeAbbreviations()) {
          subQuery.add(parser.parse(Concept.ABREVIATION + ":" + query.getInput()), Occur.SHOULD);
        }
        if (query.isIncludeAcronyms()) {
          subQuery.add(parser.parse(Concept.ACRONYM + ":" + query.getInput()), Occur.SHOULD);
        }
        finalQuery.add(subQuery, Occur.MUST);
      } else {
        finalQuery.add(parser.parse(query.getInput()), Occur.MUST);
      }
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parse query", e);
    }
    addCommonConstraints(finalQuery, query);
    IndexHits<Node> hits = null;
    try (Transaction tx = graph.beginTx()) {
      hits = graph.index().getNodeAutoIndexer().getAutoIndex().query(finalQuery);
      tx.success();
    }
    return limitHits(hits, query);
  }

  @Override
  public List<Concept> getConceptsFromTerm(Query query) {
    QueryParser parser = getQueryParser();
    String exactQuery = String.format("\"\\^ %s $\"", query.getInput());
    BooleanQuery finalQuery = new BooleanQuery();
    try {
      if (query.isIncludeSynonyms() || query.isIncludeAbbreviations() || query.isIncludeAcronyms()) {
        BooleanQuery subQuery = new BooleanQuery();
        subQuery.add(LuceneUtils.getBoostedQuery(parser, exactQuery, 10.0f), Occur.SHOULD);
        if (query.isIncludeSynonyms()) {
          subQuery.add(parser.parse(Concept.SYNONYM + ":" + exactQuery), Occur.SHOULD);
        }
        if (query.isIncludeAbbreviations()) {
          subQuery.add(parser.parse(Concept.ABREVIATION + ":" + query.getInput()), Occur.SHOULD);
        }
        if (query.isIncludeAcronyms()) {
          subQuery.add(parser.parse(Concept.ACRONYM + ":" + query.getInput()), Occur.SHOULD);
        }
        finalQuery.add(subQuery, Occur.MUST);
      } else {
        finalQuery.add(parser.parse(exactQuery), Occur.MUST);
      }
    } catch (ParseException e) {
      logger.log(Level.WARNING, "Failed to parse query", e);
    }
    addCommonConstraints(finalQuery, query);
    logger.finest(finalQuery.toString());
    try (Transaction tx = graph.beginTx()) {
      IndexHits<Node> hits = graph.index().getNodeAutoIndexer().getAutoIndex().query(finalQuery);
      tx.success();
      return limitHits(hits, query);
    }
  }

  @Override
  public Set<String> getAllCategories() {
    return Suppliers.memoize(new Supplier<Set<String>>() {
      @Override
      public Set<String> get() {
        ExecutionResult result =
            engine.execute(
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
  public Collection<String> getAllCuriePrefixes() {
    return curieUtil.getPrefixes();
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
