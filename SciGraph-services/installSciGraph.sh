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