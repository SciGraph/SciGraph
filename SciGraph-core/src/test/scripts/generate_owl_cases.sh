#!/bin/sh
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


# This script can be run after mvn test to generate images of the graph translation
# test cases. It requires graphviz.

out=target/owl_cases/index.html

function getOwlContent() {
  owlfile="src/test/resources/ontologies/cases/$1.owl"
  owl=`cat $owlfile | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&#39;/g'`
  echo "<pre>$owl</pre>\n" >> $out
}

function drawGraph() {
  png="target/owl_cases/$1.png"
  `dot -Tpng $2 > $png`
  echo "<img src='images/$1.png' />" >> $out
}

echo "<h1>OWL -> Neo4j Translations</h1>\n" > $out

echo "<table border='1'>" >> $out
for f in target/owl_cases/*.dot
do
  filename=$(basename "$f")
  filename="${filename%.*}"
  echo "<tr><td colspan='2'><center><b>$filename</b></center></td></tr>" >> $out
  echo "<tr>" >> $out
  echo "<td>" >> $out
  getOwlContent $filename
  echo "</td>" >> $out
  echo "<td>" >> $out
  drawGraph $filename $f
  echo "</td>" >> $out
  echo "</tr>" >> $out
done
echo "</table>" >> $out

cd target/owl_cases

`rm -fr SciGraph.wiki`
`pandoc -f html -t markdown index.html > index.md`
`git clone git@github.com:SciGraph/SciGraph.wiki.git`
cd SciGraph.wiki
`cp ../index.md Neo4jMapping.md`
`mkdir -p images`
`cp ../*.png images`
git add *
git commit -m "Automated update of owl_cases"
git push
