#!/bin/bash
#
# Copyright (C) 2014 The SciGraph authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

CURIES=$(curl -s https://raw.githubusercontent.com/monarch-initiative/dipper/master/dipper/curie_map.yaml | sed 's/^/    /g')
TEMPLATE_QUERIES=$(more ~/Documents/configs/SciGraph/template_queries/cypher.yaml | sed 's/\$/\$/' | sed 's/^/    /g')
LOCATION=/temp/duckworthGraph

echo "
server:
  type: simple
  applicationContextPath: /scigraph
  adminContextPath: /admin
  connector:
    type: http
    port: 9000

applicationContextPath: scigraph

graphConfiguration:
  location: $LOCATION
  neo4jConfig:
      use_memory_mapped_buffers : true
      dump_configuration : true
      neostore.nodestore.db.mapped_memory : 1G
      neostore.relationshipstore.db.mapped_memory : 4G
      neostore.propertystore.db.mapped_memory : 500M
      neostore.propertystore.db.strings.mapped_memory : 500M
      neostore.propertystore.db.arrays.mapped_memory : 500M
  indexedNodeProperties:
      - label
      - synonym
      - fragment
  curies:
$CURIES
cypherResources:
$TEMPLATE_QUERIES
"
