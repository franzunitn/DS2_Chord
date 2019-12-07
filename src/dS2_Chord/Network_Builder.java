package dS2_Chord;
import dS2_Chord.Key;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;

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
		
		float join_prob = (Float) params.getValue("join_prob");				//probability of Node join the net
		float leave_prob = (Float) params.getValue("leave_prob");			//probability of Node leave the net
		float fail_prob = (Float) params.getValue("fail_prob");				//probability of Node fail
		float lookup_prob = (Float) params.getValue("lookup_prob");			//probability of a lookup
		float insertkey_prob = (Float) params.getValue("insertkey_prob");	//probability of an new key insertion
		
		
		int nodes = (Integer) params.getValue("nodes");			//max number of node in the network.
		
		
		int stabilize_tick = (Integer) params.getValue("stabilize_tick");		//how many tick to wait before stabilize
		int fixfinger_tick = (Integer) params.getValue("fixfinger_tick");		//how many tick to wait before fixfinger
		
		
		//create a key instance to encrypt the id of the node
		Key k = new Key();
		//list of nodes to pass to the super_node constructor
		ArrayList<Node> current_nodes = new ArrayList<Node>();
		//Dictionary to map hash id into a more visualize integer id
		Dictionary d = new Hashtable();
		
		for (int i = 0; i < nodes; i++) {
			//create the new key
			BigInteger new_key = k.encryptThisString(Integer.toString(i)); 
			//create a new node
			Node n = new Node(new_key);
			//add the node to the list
			current_nodes.add(n);
		}
		
		//sort the node by key to construct the ring topology
		Collections.sort(current_nodes, new Comparator<Node>() {
			@Override
			public int compare(Node o1, Node o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		
		int count = 0;
		for(Node o : current_nodes) {
			//map the key into integer in order to be better visualizable
			d.put(o.getId(), count);
			count++;
			
			//adding the nodes in the context
			context.add(o);
		}
		
		//construct a ring topology to be displayed and align the first node 
		//the node should be ordered from the lowest to the biggest !!
		
		
		// Place nodes in a circle in space (after adding to context)
        double spaceSize = space.getDimensions().getHeight();
        double center = spaceSize / 2;
        double radius = center - 2;
        int nodeCount = current_nodes.size();
        for (int i = 0; i < nodeCount; i++) {
            double theta = 2 * Math.PI * i / nodeCount;
            double x = center + radius * Math.cos(theta);
            double y = center + radius * Math.sin(theta);
            space.moveTo(current_nodes.get(i), x, y);
        }
		
		
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		//declaration and initialization of a super node to control the protocol
		//pass all the probability 
		Super_node s = new Super_node(join_prob, 
									leave_prob, 
									fail_prob, 
									lookup_prob, 
									insertkey_prob, 
									nodes, 
									current_nodes,  
									stabilize_tick, 
									fixfinger_tick,
									d);
		
		context.add(s);
		
		//this is only for batch run
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(40);
		}

		return context;
	}
		
	
}
