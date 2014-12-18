@Grab('edu.sdsc:scigraph-core:1.1-SNAPSHOT')
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;
import groovy.util.logging.Slf4j;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

@Slf4j
class Graph {

    def graphDb
    def engine

    def init(graphLocation) {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(graphLocation)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() { graphDb.shutdown() }
        })
        engine = new ExecutionEngine(graphDb)
    }

    def executeQuery(query) {
        engine.execute(query).iterator()
    }

    def shutdown() {
        graphDb.shutdown()
    }

    def executeQuery(query, Closure func) {
        def result = executeQuery query
        result.each { func(it) }
    }

    def getSuperclasses(nodeId) {
        def superClasses = [] as Set
        def superClassLabels = [] as Set
        Transaction tx = graphDb.beginTx()
        try {
            for (Path path: graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(OwlRelationships.RDF_SUBCLASS_OF, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_GLOBAL)
                    .traverse(graphDb.getNodeById(nodeId))) {
                superClasses.add((String)path.endNode().getProperty("fragment"))
                if (path.endNode().hasProperty('label'))
                    superClassLabels.add((String)path.endNode().getProperty("label"))
            }
            tx.success()
        } finally {
            tx.close()
        }
        return [superClasses, superClassLabels]
    }

}
