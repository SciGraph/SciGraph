SciGraph
========
[![Build Status](https://travis-ci.org/SciGraph/SciGraph.svg?branch=master)](https://travis-ci.org/SciGraph/SciGraph)
[![Coverage Status](https://coveralls.io/repos/SciGraph/SciGraph/badge.svg)](https://coveralls.io/r/SciGraph/SciGraph)
[![Stories in Ready](https://badge.waffle.io/SciGraph/SciGraph.svg?label=ready&title=Ready)](http://waffle.io/SciGraph/SciGraph)

Represent ontologies and ontology-encoded knowledge in a Neo4j graph.

Motivation
----------
SciGraph aims to represent ontologies and data described using ontologies as a Neo4j graph. SciGraph
reads ontologies with [owlapi](http://owlapi.sourceforge.net/) and ingests
ontology formats available to owlapi (OWL, RDF, OBO, TTL, etc).
Have a look at [how SciGraph translates some simple ontologies](https://github.com/SciGraph/SciGraph/wiki/Neo4jMapping).

Goals:
* OWL 2 Support
* Provide a simple, usable, Neo4j representation
* Efficient, parallel ontology ingestion
* Provide basic "vocabulary" support
* Stay domain agnostic

Non-goals:
* Create ontologies based on the graph
* Reasoning support

What's Included?
----------------
SciGraph can be used in a number of ways. After the graph is generated it could be used in an application with no SciGraph dependency.

It could be used in an application with the
`scigraph-core` dependency which adds some convenience methods and includes "vocabulary" support. "Vocabulary" support resolves
labels to graph nodes, auto-complete functionality, OpenRefine resolution services, and CURIE to
IRI resolution. Additional support for identifying these vocabulary entities
in free text can be found in the `scigraph-entity` module.

SciGraph can also be used as a stand-alone Dropwizard web service (via `scigraph-services`). SciGraph services support adding custom Cypher
queries during application configuration to keep the code base domain agnostic.

Note that SciGraph is "OWL-centric". If you have, for example, and arbitrary SKOS ontology that doesn't assert skos:Concept as an owl:Class these skos:Concepts will not be visible to the owlapi and not loaded in the resulting Neo4j graph.

Alternatives
------------
* [tinkerpop sail implementation](https://github.com/tinkerpop/blueprints/wiki/Sail-Implementation)
* [owlapi](https://owlcs.github.io/owlapi/)
* [jena tdb](https://jena.apache.org/documentation/tdb/)

Applications
------------
 * the [Monarch Initiative](http://monarchinitiative.org/) uses SciGraph for both ontologies and biological data modeling
 * [SciCrunch](http://scicrunch.org/) uses SciGraph for vocabulary and annotation services
 * [CINERGI](http://earthcube.org/group/cinergi) uses SciGraph for vocabulary and annotation services
 * the [Human Brain project](https://nip.humanbrainproject.eu/) uses SciGraph for vocabulary and annotation services

Additional Documentation
------------------------

 * [Overview of SciGraph in NIF and Monarch](https://github.com/SciGraph/SciGraph/raw/master/docs/presentation/20150801%20SciGraph.pptx)

Getting Started
---------------
A [Vagrant](https://www.vagrantup.com/) box is included if you don't want to modify your `localhost` (you'll also need [VirtualBox](https://www.virtualbox.org/)).
You can launch a provisioned box like this and then follow the steps below:

    curl https://raw.githubusercontent.com/SciGraph/SciGraph/master/src/test/resources/vagrant/Vagrantfile -o Vagrantfile
    vagrant up
    vagrant ssh

<em>Note that because Neo4j is using memory mapped IO the database cannot be stored in a Vagrant shared directory</em>

If you're not using the vagrant box make sure you have `git`, `maven`, and `java` available. Java should be version 7 or better.

Clone and compile the project:

    git clone https://github.com/SciGraph/SciGraph; cd SciGraph; mvn -DskipTests -DskipITs install

Build the graph:

    cd SciGraph-core
    mvn exec:java -Dexec.mainClass="io.scigraph.owlapi.loader.BatchOwlLoader" -Dexec.args="-c src/test/resources/pizzaExample.yaml"

Run the services:

	cd ../SciGraph-services
    mvn exec:java -Dexec.mainClass="io.scigraph.services.MainApplication" -Dexec.args="server src/test/resources/pizzaConfiguration.yaml"

Check out some of the REST endpoints (the Vagrant box has port 9000 mapped so you can use your host browser to check these out):

 - [Get autocomplete candidates for a prefix](http://localhost:9000/scigraph/vocabulary/autocomplete/Sp)

 - [Find a class based on an "exactish" term match](http://localhost:9000/scigraph/vocabulary/search/Shrimps)

 - [Get the graph neighborhood of a class](http://localhost:9000/scigraph/graph/neighbors/pizza:PrawnsTopping)

 - [Visualize the graph neighborhood of a class](http://localhost:9000/scigraph/graph/neighbors/pizza:AmericanHot.png)

 - [Visualize a custom, domain specific Cypher query](http://localhost:9000/scigraph/dynamic/toppings.png?pizza_id=pizza:FourSeasons)

Also browse the [generated REST documentation](http://localhost:9000/scigraph/docs/)
to see some of the other resources.

-------
<img src="http://github.nfsdb.org/images/yklogo.png" />
-------
Thanks to YourKit for providing an Open Source license.

YourKit supports open source projects with its full-featured Java Profiler.YourKit, LLC is the creator of <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>, innovative and intelligent tools for profiling Java and .NET applications.
