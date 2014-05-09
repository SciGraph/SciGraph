package edu.sdsc.scigraph.internal.reachability;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

public class DirectionalPathExpender implements PathExpander<Void> {

	private Direction direction;
	
	public DirectionalPathExpender (Direction direction) {
		this.direction =direction;
	}
	
	@Override
	public Iterable<Relationship> expand(Path path, BranchState<Void> state) {
          return path.endNode().getRelationships(direction);
	}

	@Override
	public PathExpander<Void> reverse() {
		// TODO Auto-generated method stub
		return new DirectionalPathExpender(direction.reverse());
	}

}
