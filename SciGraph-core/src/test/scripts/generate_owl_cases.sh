#!/bin/sh

out=target/owl_cases/index.html

function getOwlContent() {
  owlfile="src/test/resources/ontologies/cases/$1.owl"
  owl=`cat $owlfile | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&#39;/g'`
  echo "<pre>$owl</pre>\n" >> $out
}

function drawGraph() {
  png="target/owl_cases/$1.png"
  `dot -Tpng $2 > $png`
  echo "<img src='$1.png' />" >> $out
}

echo "<h1>OWL Graph Representations</h1>\n" > $out

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
