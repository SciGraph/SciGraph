package edu.sdsc.scigraph.annotation;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

public class EntityFormatConfigurationTest {

  @Test
  public void testGetTargetClasses() {
    EntityFormatConfiguration config = new EntityFormatConfiguration.Builder(new StringReader(""))
        .targetClasses(newHashSet("arg", "foo")).get();
    assertEquals(2, config.getTargetClasses().size());
  }

}
