package dS2_Chord;
import dS2_Chord.Key;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import dS2_Chord.StdRandom;
import dS2_Chord.Node;
import dS2_Chord.Super_node;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;


public class Network_Builder implements ContextBuilder<Object> {

		@Override
	public Context build(Context<Object> context) {
		context.setId("DS2_Chord");
		
		//declaration of all the network
		
		//Network for lookup message
		NetworkBuilder<Object> pointer_net = new NetworkBuilder<Object>("pointer_net", context, true);
		pointer_net.buildNetwork();
		
		//network for lookup
		
		
		NetworkBuilder<Object> netBuilder_messages = new NetworkBuilder<Object>(
				"chord", context, true);
		netBuilder_messages.buildNetwork();
		
		
		//continuous space declaration used to visualize the nodes
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 40,
				40);

		//grid declaration used to visualize the nodes
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, 40, 40));

		Parameters params = RunEnvironment.getInstance().getParameters();
		
		//Get parameters from user
		
		//probability of Node join the net
		//probability of Node leave the net
		//probability of Node fail
		//probability of a lookup
		//probability of an new key insertion
		
		//max number of nodes
		int nodes = (Integer) params.getValue("nodes");		//max number of node in the network.
		
		//declaration and initialization of a super node to control the protocol
		//pass all the probability 
		Super_node s = new Super_node();
		
		//create a key instance to encrypt the id of the node
		Key k = new Key();
		
		//add the node to the context 
		for (int i = 0; i < nodes; i++) {
			//adding the nodes in the context
			context.add(new Node(k.encryptThisString(Integer.toString(i))));
		}
		

		
		
		//construct a ring topology to be displayed and align the first node 
		
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		
		//this is only for batch run
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(40);
		}

		return context;
	}
		
	
}
