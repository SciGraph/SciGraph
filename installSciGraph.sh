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

if [ ! -d "monarchGraph" ]; then
  wget http://nif-crawler.neuinfo.org/database/graphs/monarchGraph.zip
  unzip monarchGraph.zip
  rm monarchGraph.zip
fi  
CWD=$(pwd)
echo "
server:
  applicationConnectors:
  - type: http
    port: 9000
  adminConnectors:
  - type: http
    port: 9001

logging:
  level: INFO

applicationContextPath: scigraph

graphConfiguration:
  graphLocation: $CWD/monarchGraph
" > configuration.yml

echo "
java -Xmx4G -jar SciGraph/SciGraph-services/target/scigraph-services-1.0-SNAPSHOT.jar server configuration.yml
" > runServer.sh

if [ ! -d "SciGraph" ]; then
  git clone https://github.com/SciCrunch/SciGraph.git
fi  
cd SciGraph
mvn clean install package
cd ..

sh runServer.sh