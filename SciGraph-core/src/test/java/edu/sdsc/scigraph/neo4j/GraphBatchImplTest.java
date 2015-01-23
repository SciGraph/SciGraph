package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import edu.sdsc.scigraph.frames.CommonProperties;

public class GraphBatchImplTest extends GraphTestBase<GraphBatchImpl> {

  @Override
  protected GraphBatchImpl createInstance() throws IOException {
    Path path = Files.createTempDirectory("SciGraph-BatchTest");
    BatchInserter inserter = BatchInserters.inserter(path.toFile().getAbsolutePath());
    return new GraphBatchImpl(inserter, CommonProperties.URI, newHashSet("prop1", "prop2"),
            newHashSet("prop1"), new IdMap(), new RelationshipMap());
  }

}
