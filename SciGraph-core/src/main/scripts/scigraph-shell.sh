#!/bin/sh
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
echo $DIR
CLASSPATH_PREFIX=$DIR/../../../target/* neo4j-shell -path $1