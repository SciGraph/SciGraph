package edu.sdsc.scigraph.internal.reachability;


import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;

public class ReachabilityIndex {
	
	public static final String dataDictionaryURI = "http://scigraph.scichruch.com/metadata";
	
	public static final String thingURI = "http://www.w3.org/2002/07/owl#Thing";

	
	private static final String indexExists ="ReachablilityIndexExists";
	protected static final String propInList  = "ReachablilityIndexInList";
	protected static final String propOutList = "ReachablilityIndexOutList";
	
	private Graph<Concept> graph;
	private GraphDatabaseService graphDb;
	private boolean opened;   // flag if there an index to use.
	private long thingId;     // node id for entity "http://www.w3.org/2002/07/owl#Thing"
	
    private static final Logger logger = Logger.getLogger(ReachabilityIndex.class.getName());

    /**
     * Initialize a reachability index object on graph. 
     * @param graph
     */
    public ReachabilityIndex(Graph<Concept> graph) {
		this.graph = graph;
		graphDb = graph.getGraphDb();

		Optional<Node> opt = graph.getNode(dataDictionaryURI);
		
		if (!opt.isPresent()) {
			opened= false;
	    } else {
	    	opened = (boolean)opt.get().getProperty(indexExists, false);
	    }
		
        opt = graph.getNode(thingURI);
		
		if (opt.isPresent()) {
			thingId = opt.get().getId();
	    } else {
	    	thingId = -1;
	    }
	}

    
    /**
     * Initialize a reachability index object on graphDb. 
     * @param graph
     */
    public ReachabilityIndex(GraphDatabaseService graphdb) {
		graphDb = graphdb;
		graph = null;

		Node n = graphDb.getNodeById(0);
		
    	opened = (boolean)n.getProperty(indexExists, false);
	    
    	thingId = -1;
	}

    
    /**
     * Create a reachability index on a graph.
     * 
     * @throws ReachabilityIndexException if a reachability index already exists.
     */
	public void creatIndex() throws ReachabilityIndexException {
		
	    if ( opened) {
	    	logger.info("Index already exists. Drop it first then create again.");
		    throw new ReachabilityIndexException("Index already exists.");
	    }
	    
	    // create the index

	    // calculate the
	    
	    long stime = System.currentTimeMillis();
	    
	    TreeSet<AbstractMap.SimpleEntry<Long,Integer>> l = getHopCoverages();
	    
	    long etime = System.currentTimeMillis();
	    
	    logger.info("Takes " + (etime - stime)/1000 + " secend(s) to calculate HopCoverage" );
	    
	    // Hold the partially built index in memory. Key is the Node id. Value is a Pair. 
	    // First element in the pair is inList, second element is outList.
	    TreeMap <Long, Pair<TreeSet<Long>,TreeSet<Long>>> inMemoryIdx = 
	    		new TreeMap <Long, Pair<TreeSet<Long>,TreeSet<Long>>>(); 
	    
	    TraversalDescription tdi = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
	    		               .expand(new DirectionalPathExpender(Direction.INCOMING))
	    		               .evaluator(new ReachabilityEvaluator(inMemoryIdx, Direction.INCOMING, thingId));

	    TraversalDescription tdo = Traversal.description().breadthFirst().uniqueness(Uniqueness.NODE_GLOBAL)
	               .expand(new DirectionalPathExpender(Direction.OUTGOING))
	               .evaluator(new ReachabilityEvaluator(inMemoryIdx, Direction.OUTGOING, thingId));

	    long bfst = 0, rbfst=0;
	    for (AbstractMap.SimpleEntry<Long,Integer> e : l ) {
	    	Node workingNode = graphDb.getNodeById(e.getKey());
	    	stime = System.currentTimeMillis();
	    	for ( Path p :tdi.traverse(workingNode) );
	    	etime = System.currentTimeMillis() ;
	    	rbfst += etime - stime;
	    	
	    	for ( Path p : tdo.traverse(workingNode) );
	    	bfst += System.currentTimeMillis() - etime;
	    }
	    
	    logger.info("BFS index building time: " + (bfst/1000) + " sec(s), RBFS index building time: " + (rbfst/1000) + " sec(s).");
	    // store the inMemoryIdx into db.
	    Transaction tx = graphDb.beginTx();
	    
	    int counter=0;
	    for(Map.Entry<Long, Pair<TreeSet<Long>,TreeSet<Long>>> e: inMemoryIdx.entrySet() ) {
	    	Node node = graphDb.getNodeById(e.getKey());
	    	counter++;
	    	if ( counter % 500000 == 0 ) {
	    		logger.info("commit transaction when populating in-out list.");
				tx.success();
				tx.finish();
				tx = graphDb.beginTx();	
	    	}
	    	
	    	node.setProperty(propInList, getPrimitiveArray(e.getValue().first()));
	    	node.setProperty(propOutList, getPrimitiveArray(e.getValue().other()));
	    }
	    
        //set the flag.
		Node n = graph == null ? graphDb.getNodeById(0): graph.getOrCreateNode(dataDictionaryURI);
	    n.setProperty(indexExists, true);
	    tx.success();
	    tx.finish();
	    
        opened = true;
        logger.info("Reachability index created.");
	}
	
	
	public void dropIndex() throws ReachabilityIndexException {
		if ( opened) {

	      Transaction tx = graphDb.beginTx();
		  Node n0;	
		   if ( graph != null ) {	
		      n0 = graph.getNode(dataDictionaryURI).get();
		   } else {
			  n0 = graphDb.getNodeById(0);   
		   }
     		  // ... cleanup the index.
   	   	   for ( Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
			    n.removeProperty(propInList);
			    n.removeProperty(propOutList);
   		   };
   		    
   		   // reset the flag.
   		   n0.setProperty(indexExists, false);
   		   
	       tx.success();
	       tx.finish();
	        
	        opened = false;
	        logger.info("Reachability index dropped.");
	    } else {
	    	logger.info("Index doesn't exist.");
	    	throw new ReachabilityIndexException("Index does't exist.");
	    }
	}
	
	/**
	 * Calculate the hopCoverage for each node and sort them in descendant order.
	 * @return
	 */
	private TreeSet<AbstractMap.SimpleEntry<Long,Integer>> getHopCoverages(){

		TreeSet<AbstractMap.SimpleEntry<Long,Integer>> nodeSet = new TreeSet<AbstractMap.SimpleEntry<Long,Integer>>(
				new Comparator<AbstractMap.SimpleEntry<Long,Integer>>() {
							public int compare(AbstractMap.SimpleEntry<Long,Integer> a, AbstractMap.SimpleEntry<Long,Integer> b) {
								int r = b.getValue() - a.getValue();
								if ( r != 0)
									return r;
								else {
									long r0 = a.getKey() - b.getKey();
								    return (int)r0;
								}
							}
						}
		);
		
		for ( Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
			int i = 0;
			for ( Relationship r : n.getRelationships()) {
				i++;
			}
			nodeSet.add(new AbstractMap.SimpleEntry<Long, Integer>(n.getId(), i));
		}
		
		return nodeSet;
	}
	
	private long[] getPrimitiveArray(Set<Long> s) {
		long[] result = new long[s.size()];
		int i = 0;
		for ( Long l : s) { result[i++]=l.longValue();} 
		return result;
		
	}
	
	/**
	 * Check if there is a path which starts from node1 and can reach node2.
	 * @param node1
	 * @param node2
	 * @return
	 * @throws Exception 
	 */
	public boolean canReach(Node node1, Node node2) throws ReachabilityIndexException {
		if ( ! opened) throw new ReachabilityIndexException ("Reachability index not exists."); 
		long[] l1 = (long[])node1.getProperty(propOutList);
		long[] l2 = (long[])node2.getProperty(propInList);
		int i=0,j=0;
		int m=l1.length;
		int n=l2.length;
		
        while(i < m && j < n)  {

          if(l1[i] < l2[j]) 
            i++;
          else if(l2[j] < l1[i])
            j++;
          else /* if l1[i] == l2[j] */
            return true;
        }
		return false;
	}
}
