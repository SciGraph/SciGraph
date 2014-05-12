package edu.sdsc.scigraph.internal.reachability;

import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.helpers.Pair;

public class ReachabilityEvaluator implements Evaluator {

	private TreeMap <Long, Pair<TreeSet<Long>,TreeSet<Long>>> inMemoryIdx;
	private Direction direction ;
	private long thingId;   // node id for owl:Thing.
	
	public ReachabilityEvaluator (TreeMap <Long, Pair<TreeSet<Long>,TreeSet<Long>>> inMemoryIdx, Direction direction,
			                    long thingId) {
		this.inMemoryIdx = inMemoryIdx;
		this.direction = direction;
		this.thingId = thingId;
	}
	
	@Override
	public Evaluation evaluate(Path path) {

		Node cur = path.endNode();    // u or w
		long curId = cur.getId();
		
		// don't traverse owl:Thing node.
		if ( curId == thingId)	return Evaluation.EXCLUDE_AND_PRUNE;

		Node secondLast= path.startNode();   // Vi
		long secId = secondLast.getId();

		if ( curId == secId) {    // first node in the traverse
			// add itself to the in-out list
			Pair <TreeSet<Long>,TreeSet<Long>> listPair = inMemoryIdx.get(curId);
			
			if ( listPair == null) {  // create the list pair 
				// first is in-list, second is out-list
				listPair = Pair.of(new TreeSet<Long>(),new TreeSet<Long>());
				inMemoryIdx.put(curId, listPair);
			} 
			listPair.first().add(cur.getId());
			listPair.other().add(cur.getId());
			
			return Evaluation.INCLUDE_AND_CONTINUE;
		}
		
        if ( direction == Direction.INCOMING ) { // doing reverse BFS
        	if ( nodesAreConnected(curId, secId) ) {
        		return Evaluation.EXCLUDE_AND_PRUNE;
        	} else {
        		Pair <TreeSet<Long>,TreeSet<Long>> listPair = inMemoryIdx.get(curId);
        		if ( listPair == null) {
        			listPair = Pair.of(new TreeSet<Long>(),new TreeSet<Long>());
        			inMemoryIdx.put(curId, listPair);
        		}
        		listPair.other().add(secId);
  //      		System.out.println("add " +  secId + " to L_out of " + curId);
        		return Evaluation.INCLUDE_AND_CONTINUE;
        	}
        	
        } else {   //doing BFS
        	if ( nodesAreConnected(secId,curId) ) {   // cur is w
        		return Evaluation.EXCLUDE_AND_PRUNE;
        	} else {
        		Pair <TreeSet<Long>,TreeSet<Long>> listPair = inMemoryIdx.get(curId);
        		if ( listPair == null) {
        			listPair = Pair.of(new TreeSet<Long>(),new TreeSet<Long>());
        			inMemoryIdx.put(curId,listPair);
        		}
        		listPair.first().add(secondLast.getId());
//        		System.out.println("add " + secId + " to L_in of " + curId );
        		return Evaluation.INCLUDE_AND_CONTINUE;
        	}
        }
	}

	private boolean nodesAreConnected(long nodeIdOut, long nodeIdIn) {
		
		if ( inMemoryIdx.get(nodeIdOut) == null || inMemoryIdx.get(nodeIdIn) == null )
			return false;
		
		TreeSet<Long> outList = inMemoryIdx.get(nodeIdOut).other();
		TreeSet<Long> inList = inMemoryIdx.get(nodeIdIn).first();
		
		if ( outList == null || inList == null) return false;
		
		return !Collections.disjoint(outList, inList);
	}
	
}
