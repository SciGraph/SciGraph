package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Sets.newHashSet;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphBatchImpl;
import edu.sdsc.scigraph.neo4j.IdMap;
import edu.sdsc.scigraph.neo4j.RelationshipMap;

public class OwlVisitorBatchGraphTest extends OwlVisitorTestBase<GraphBatchImpl> {

  @Override
  protected GraphBatchImpl createInstance() throws Exception {
    BatchInserter inserter = BatchInserters.inserter(path.toFile().getAbsolutePath());
    return new GraphBatchImpl(inserter, CommonProperties.URI, newHashSet("fragment"),
            newHashSet("fragment"), new IdMap(), new RelationshipMap());
  }

}
