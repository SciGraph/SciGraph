SciGraph
========
Represent ontologies in a Neo4j graph.

Motivation
----------
SciGraph aims to represent ontologies as a Neo4j graph. SciGraph
reads ontologies with [owlapi](http://owlapi.sourceforge.net/) and ingests
ontology formats available to owlapi (OWL, RDF, OBO, TTL, etc). 
Have a look at [how SciGraph translates some simple ontologies](https://github.com/SciCrunch/SciGraph/wiki/Neo4jMapping).

Goals:
* OWL 2 Support
* Provide a simple, usable, Neo4j representation
* Efficient loading
* Provide basic "vocabulary" support

Non-goals:
* Create ontologies based on the graph
* Reasoning support

What's Included?
----------------
SciGraph can be used in a number of ways. After the graph is generated
(depending on the index configuration) it could be used in an application with
no SciGraph dependency.

It could be used in an application with the
`scigraph-core` dependency which adds some convenience methods and includes "vocabulary" support. "Vocabulary" support includes searching a Lucene index
for ontology classes in the graph, auto-complete functionality, and CURIE to
class resolution. Additional support for identifying these vocabulary entities
in text can be found in the `scigraph-entity` module.

SciGraph can also be used as a stand-alone DropWizard web service (via `scigraph-services`).

Alternatives
------------
* [tinkerpop sail implementation](https://github.com/tinkerpop/blueprints/wiki/Sail-Implementation)
* [owlapi](http://owlapi.sourceforge.net/)
* [jena tdb](https://jena.apache.org/documentation/tdb/)

Getting Started
---------------
Make sure you have `git`, `maven`, and `java 7`.

Clone and compile the project:

    git clone https://github.com/SciCrunch/SciGraph; cd SciGraph; mvn compile

Build the graph:

    cd SciGraph-core
    mvn exec:java -Dexec.mainClass="edu.sdsc.scigraph.owlapi.BatchOwlLoader" -Dexec.args="-c src/test/resources/pizzaExample.yaml"

Run the service:

	cd SciGraph-services
    mvn exec:java -Dexec.mainClass="edu.sdsc.scigraph.services.MainApplication" -Dexec.args="server src/test/resources/pizzaConfiguration.yaml"

Check out some of the REST endpoints:
<dl>
<dt> [http://localhost:9000/scigraph/vocabulary/autocomplete/Sp](http://localhost:9000/scigraph/vocabulary/autocomplete/Sp) </dt>
<dd>Get autocomplete options for a prefix</dd>
<dt>[http://localhost:9000/scigraph/vocabulary/search/Shrimp](http://localhost:9000/scigraph/vocabulary/search/Shrimp)</dt>
<dd>Find a class based on an "exactish" term match
(try changing the term to "shrimps")</dd>
<dt>[http://localhost:9000/scigraph/graph/neighbors/PrawnsTopping](http://localhost:9000/scigraph/graph/neighbors/PrawnsTopping)</dt>
<dd>Get the graph neighborhood or a class</dd>
</dl>

Also browse the [generated REST documentation](http://localhost:9000/scigraph/docs/)
to see some of the other resources.
