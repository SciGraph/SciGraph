FROM maven:3.6.0-jdk-8-alpine

VOLUME /data
WORKDIR /scigraph

ADD . /scigraph
ADD docker/scripts /scigraph/

# Directory for configuration files
RUN mkdir -p /scigraph/conf

# Build scigraph
RUN cd /scigraph && mvn install -DskipTests -DskipITs

ENV PATH="/scigraph/scripts/:$PATH"

ENTRYPOINT ["/bin/sh"]
