#!/bin/sh
#
# Copyright (C) 2014 Christopher Condit (condit@gmail.com)
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

mvn clean test-compile

declare -a opts=("-Xmx2G -Xms2G" "-Xmx4G -Xms4G" "-Xmx6G -Xms6G")

i=1
for maven_opt in "${opts[@]}"
do
  export MAVEN_OPTS="$maven_opt"
  mvn -Djub.consumers=CONSOLE,XML -Djub.xml.file=target/$i.xml failsafe:integration-test -Dit.test=io.scigraph.internal.reachability.ReachabilityIndexPerfIT
  ((i++))
done
