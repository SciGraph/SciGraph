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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.prefixcommons.CurieUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;

public class OwlLoadConfigurationLoader {

	private File configurationFile;

	public OwlLoadConfigurationLoader(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	public OwlLoadConfiguration loadConfig() throws JsonParseException,
			JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		OwlLoadConfiguration config = mapper.readValue(configurationFile,
				OwlLoadConfiguration.class);

		CurieUtil curieUtil = new CurieUtil(config.getGraphConfiguration()
				.getCuries());

		// resolve categories
		Map<String, String> resolvedCategories = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : config.getCategories()
				.entrySet()) {
			Optional<String> iriOpt = curieUtil.getIri(entry.getKey());
			resolvedCategories.put(iriOpt.orElse(entry.getKey()), entry.getValue());
		}
		config.setCategories(resolvedCategories);

		// resolve MappedProperties
		List<MappedProperty> resolvedMappedProperties = new ArrayList<MappedProperty>();
		for (MappedProperty mappedProperty : config.getMappedProperties()) {
			MappedProperty resolvedMappedProperty = new MappedProperty(
					mappedProperty.name);
			List<String> resolvedProperties = new ArrayList<String>();
			for (String property : mappedProperty.getProperties()) {
				resolvedProperties.add(curieUtil.getIri(property).orElse(property));
			}
			resolvedMappedProperty.setProperties(resolvedProperties);
			resolvedMappedProperties.add(resolvedMappedProperty);
		}
		config.setMappedProperties(resolvedMappedProperties);

		return config;
	}
}
