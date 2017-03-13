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
package io.scigraph.owlapi.loader;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import io.scigraph.owlapi.loader.OwlLoadConfiguration;
import io.scigraph.owlapi.loader.OwlLoadConfigurationLoader;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.prefixcommons.CurieUtil;

public class OwlLoadConfigurationLoaderTest {

	OwlLoadConfiguration rawConfig;
	OwlLoadConfiguration loaderConfig;
	CurieUtil curieUtil;

	@Before
	public void setup() throws URISyntaxException, JsonParseException,
			JsonMappingException, IOException {
		URL url = this.getClass().getResource("/pizzaExample.yaml");
		File pizzaFile = new File(url.getFile());

		assertThat(pizzaFile.exists(), is(true));
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		rawConfig = mapper.readValue(pizzaFile, OwlLoadConfiguration.class);

		OwlLoadConfigurationLoader owlLoadConfigurationLoader = new OwlLoadConfigurationLoader(
				pizzaFile);
		loaderConfig = owlLoadConfigurationLoader.loadConfig();
		curieUtil = new CurieUtil(rawConfig.getGraphConfiguration().getCuries());
	}

	@Test
	public void supportCuriesForCategories() {
		// assert that there's a curie to resolve
		String unresolvedCurie = rawConfig.getCategories().get(
				"pizza:NamedPizza");
		assertThat(unresolvedCurie, equalTo("pizza"));

		// assert that categories are full IRIs
		String resolvedCurie = loaderConfig.getCategories().get(
				"http://www.co-ode.org/ontologies/pizza/pizza.owl#NamedPizza");
		assertThat(resolvedCurie, equalTo("pizza"));
	}

	@Test
	public void supportCuriesForMappedProperties() {
		Boolean rawConfigContainsCurie = false;
		List<MappedProperty> unresolvedMappedProperties = rawConfig
				.getMappedProperties();
		for (MappedProperty mappedProperty : unresolvedMappedProperties) {
			for (String property : mappedProperty.getProperties()) {
				rawConfigContainsCurie = rawConfigContainsCurie
						|| curieUtil.getIri(property).isPresent();
			}
		}
		assertThat("Test file does not contain any curies",
				rawConfigContainsCurie, equalTo(true));

		Boolean loaderConfigContainsCurie = false;
		List<MappedProperty> resolvedMappedProperties = loaderConfig
				.getMappedProperties();
		for (MappedProperty mappedProperty : resolvedMappedProperties) {
			for (String property : mappedProperty.getProperties()) {
				loaderConfigContainsCurie = loaderConfigContainsCurie
						|| curieUtil.getIri(property).isPresent();
			}
		}
		assertThat("MappedProperty curies have not been resolved properly",
				loaderConfigContainsCurie, equalTo(false));
	}
}
