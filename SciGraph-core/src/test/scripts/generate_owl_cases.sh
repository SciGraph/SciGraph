#!/bin/sh

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
`git clone git@github.com:SciCrunch/SciGraph.wiki.git`
cd SciGraph.wiki
`cp ../index.md Neo4jMapping.md`
`mkdir -p images`
`cp ../*.png images`
git add *
git commit -m "Automated update of owl_cases"
git push
