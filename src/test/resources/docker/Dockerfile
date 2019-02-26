FROM maven:3.6.0-jdk-8-alpine

VOLUME /scigraph
WORKDIR /data

ADD resources/load-configuration.yaml /data/
ADD resources/service-configuration.yaml /data/

ADD scripts /data/

RUN apk --update add git

# Clone and build
RUN git clone https://github.com/SciGraph/SciGraph.git /data/scigraph
RUN cd /data/scigraph && mvn install -DskipTests -DskipITs

ENV PATH="/data/scripts/:$PATH"

ENTRYPOINT ["/bin/sh"]
