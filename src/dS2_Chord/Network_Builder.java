package dS2_Chord;
import dS2_Chord.Key;

import java.awt.print.Printable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

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
		
		
		NetworkBuilder<Object> netBuilder_insert = new NetworkBuilder<Object>(
				"insertNetwork", context, true);
		netBuilder_insert.buildNetwork();
		
		NetworkBuilder<Object> netBuilder_lookup = new NetworkBuilder<Object>(
				"lookupNetwork", context, true);
		netBuilder_lookup.buildNetwork();
		
		NetworkBuilder<Object> netBuilder_key_finded = new NetworkBuilder<Object>(
				"keyFindedNetwork", context, true);
		netBuilder_key_finded.buildNetwork();
		
		NetworkBuilder<Object> netBuilder_key_join = new NetworkBuilder<Object>(
				"joinNetwork", context, true);
		netBuilder_key_join.buildNetwork();
		
		NetworkBuilder<Object> netBuilder_stabilize = new NetworkBuilder<Object>(
				"stabilizeNetwork", context, true);
		netBuilder_stabilize.buildNetwork();
		
		NetworkBuilder<Object> netBuilder_notify = new NetworkBuilder<Object>(
				"notifyNetwork", context, true);
		netBuilder_notify.buildNetwork();
		
		
		
		//network to show the fingers 
		NetworkBuilder<Object> fingersNetwork = new NetworkBuilder<Object>(
				"fingersNetwork", context, true);
		fingersNetwork.buildNetwork();
		
		
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
		
		int new_keys = (int) params.getValue("new_keys");	//probability of an new key insertion
		
		
		int nodes = (Integer) params.getValue("nodes");			//max number of node in the network.
		
		
		int stabilize_tick = (Integer) params.getValue("stabilize_tick");		//how many tick to wait before stabilize
		int fixfinger_tick = (Integer) params.getValue("fixfinger_tick");		//how many tick to wait before fixfinger
		
		int max_number_of_keys = (Integer) params.getValue("max_number_of_keys");
		//create a key instance to encrypt the id of the node
		Key k = new Key();
		//list of nodes to pass to the super_node constructor
		ArrayList<Node> current_nodes = new ArrayList<Node>();
		//Dictionary to map hash id into a more visualize integer id
		Dictionary<BigInteger, Integer> d = new Hashtable();
		
		Random rd = new Random();
		for (int i = 0; i < nodes; i++) {
			//create the new key
			BigInteger new_key = k.encryptThisString(Integer.toString(rd.nextInt())); 
			//create a new node
			//System.out.println(new_key.toString());
			Node n = new Node(new_key);
			//add the node to the list
			current_nodes.add(n);
		}
		/*
		//MANUALLY create some node with closer range of id to test fixfinger
		for (int i = 0; i < nodes; i++) {
			BigInteger next = new BigInteger("" + i);
			//create the new key
			BigInteger new_key = next.multiply(BigInteger.TEN);
			//create a new node
			System.out.println(new_key.toString());
			Node n = new Node(new_key);
			//add the node to the list
			current_nodes.add(n);
		}*/
		
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
		
		//check that all the node in current_node are in order 
		for(int i = 1; i < current_nodes.size(); i++) {
			assert(current_nodes.get(i - 1).getId().compareTo(current_nodes.get(i).getId()) < 0);
		}
		
		//construct a ring topology to be displayed and align the first node 
		//the node should be ordered from the lowest to the biggest !!
		
		
		// Place nodes in a circle in space (after adding to context)
        double spaceSize = space.getDimensions().getHeight();
        double center = spaceSize / 2;
        double radius = center - 2;
        int nodeCount = current_nodes.size();
        BigDecimal MAX_VALUE = new BigDecimal(BigInteger.ZERO.setBit(160).subtract(BigInteger.ONE));
        for (int i = 0; i < nodeCount; i++) {
        	BigInteger id = current_nodes.get(i).getId();
        	BigDecimal id_d = new BigDecimal(id);
        	double multiplier = id_d.divide(MAX_VALUE,3, RoundingMode.HALF_UP).doubleValue();
        	double theta = 2 * Math.PI * (multiplier);
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
									new_keys, 
									nodes, 
									current_nodes,  
									stabilize_tick, 
									fixfinger_tick,
									d, 
									max_number_of_keys);
		
		context.add(s);
		
		//give the reference of the super node to all the nodes
		for(Node o : current_nodes) {
			o.snode = s;
		}
		
		//this is only for batch run
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(40);
		}

		return context;
	}
		
	
}
