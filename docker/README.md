### Loading and Deploying SciGraph in Docker

This example uses the configuration files in the conf directory, and requires
starting from the parent SciGraph directory

To test a different ontology edit the ontologies field in the load-configuration.yaml

From this directory:

    cd ..

#### Build SciGraph Image
    docker build -t scigraph -f docker/Dockerfile .

#### Run SciGraph Loader
Building the graph in /tmp/graph, note that this will error if another graph exists in this directory,
    
    docker run \
        -v /tmp/graph:/data \
        -v `pwd`/docker/conf:/scigraph/conf \
        scigraph load-scigraph load-configuration.yaml
    
#### Run SciGraph Service
    docker run \
        -v /tmp/graph:/data \
        -v `pwd`/docker/conf:/scigraph/conf \
        -d -p 9000:9000 \
        --name scigraph-services \
        scigraph start-scigraph-service service-configuration.yaml

Wait 60 seconds for the server to start, then:

Browse the [generated REST documentation](http://localhost:9000/scigraph/docs/)
to see some of the other resources.
  
Check out some of the REST endpoints:

 - [Get autocomplete candidates for a prefix](http://localhost:9000/scigraph/vocabulary/autocomplete/Sp)

 - [Find a class based on an "exactish" term match](http://localhost:9000/scigraph/vocabulary/search/Shrimps)

 - [Get the graph neighborhood of a class](http://localhost:9000/scigraph/graph/neighbors/pizza:PrawnsTopping)
 

#### Enabling the image writers (Ubuntu)

In the /etc/rc.local

    Xvfb :1 -screen 0 800x600x16 &

and in the /etc/environment

    export DISPLAY=:1

##### Test endpoints that output images

 - [Visualize the graph neighborhood of a class](http://localhost:9000/scigraph/graph/neighbors/pizza:AmericanHot.png)

 - [Visualize a custom, domain specific Cypher query](http://localhost:9000/scigraph/dynamic/toppings.png?pizza_id=pizza:FourSeasons)


#### Stop SciGraph
docker stop scigraph-services
