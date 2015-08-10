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
package io.scigraph.lucene;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.scigraph.frames.NodeProperties;
import io.scigraph.lucene.VocabularyIndexAnalyzer;
import io.scigraph.lucene.VocabularyQueryAnalyzer;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

public class VocabularyAnalyzerTest {

  IndexSearcher searcher;
  QueryParser parser;

  static void addDoc(IndexWriter writer, String term) throws CorruptIndexException, IOException {
    Document doc = new Document();
    doc.add(new Field(NodeProperties.LABEL, term, Store.YES, Index.ANALYZED));
    writer.addDocument(doc);
  }

  @Before
  public void setupIndex() throws Exception {
    Directory dir = new RAMDirectory();
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new VocabularyIndexAnalyzer());
    try( IndexWriter writer = new IndexWriter(dir, conf)) {
      addDoc(writer, "hippocampus");
      addDoc(writer, "hippocampal structures");
      addDoc(writer, "structure of the hippocampus");
      addDoc(writer, "formation");
      writer.commit();
    }

    IndexReader reader = IndexReader.open(dir);
    searcher = new IndexSearcher(reader);
    parser = new QueryParser(Version.LUCENE_36, NodeProperties.LABEL, new VocabularyQueryAnalyzer());
  }

  @Test
  public void testStopWords() throws Exception {
    Query query = parser.parse("\"^ hippocampus structure $\"");
    TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
    assertThat(docs.totalHits, is(1));
  }

  @Test
  public void testAscii() throws Exception {
    Query query = parser.parse("formation");
    TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
    assertThat(docs.totalHits, is(1));
  }

  @Test
  public void testTermQuery() throws Exception {
    TermQuery query = new TermQuery(new Term(NodeProperties.LABEL, "formation"));
    TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
    assertThat(docs.totalHits, is(1));
  }

}
