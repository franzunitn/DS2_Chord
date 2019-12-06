package dS2_Chord;

import dS2_Chord.Node;
import dS2_Chord.Key;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;

public class Super_node {
	//declaration of the variable of the class
	
	private float join_prob;		//probability of Node join the net
	private float leave_prob;		//probability of Node leave the net
	private float fail_prob;		//probability of Node fail
	private float lookup_prob;		//probability of a lookup
	private float insertkey_prob;	//probability of an new key insertion
	
	private int stabilize_tick;
	private int fix_finger_tick;
	
	private static final BigInteger MAX_VALUE = BigInteger.ZERO.setBit(160).subtract(BigInteger.ONE);
	
	private int max_number_of_nodes;		//max number of nodes in the chord ring
	private ArrayList<BigInteger> keys;			//current keys in the ring
	private ArrayList<Node> current_nodes;		//current nodes in the ring (initially empty)

	public Super_node(float join_prob, float leave_prob, float fail_prob, float lookup_prob, float insertkey_prob,
			int max_number_of_nodes, ArrayList<Node> current_nodes, ArrayList<BigInteger>current_keys, int stabilize_tick, int fix_finger_tick) {
		this.join_prob = join_prob;
		this.leave_prob = leave_prob;
		this.fail_prob = fail_prob;
		this.lookup_prob = lookup_prob;
		this.insertkey_prob = insertkey_prob;
		this.max_number_of_nodes = max_number_of_nodes;
		this.current_nodes = current_nodes;
		this.keys = current_keys;
		this.stabilize_tick = stabilize_tick;
		this.fix_finger_tick = fix_finger_tick;
	}

	@ScheduledMethod (start = 1, interval = 1)
	public void step() {
		
		Random randomSource = new Random();
		Context<Object> context =  ContextUtils.getContext(Node.class);
		IndexedIterable <Object> nodes = context.getObjects(Node.class);
		
		/*
		 * for every node that is not already in the network (BUT for a total number < than max_numbeor of node)extract
		 * a value with the probability of join and if success schedule the join
		 * */
		//search all nodes that are or not in the network
		ArrayList<Node> nodes_to_join = new ArrayList<Node>();
		
		for(Object o: nodes) {
			Node a = (Node) o;
			if(!a.is_join()) {
				nodes_to_join.add(a);
			}
		}
		//scan all the nodes that aren't already in the network and for each see if can join
		for(Node o : nodes_to_join) {
			if(StdRandom.bernoulli(this.join_prob)) {
				//check if the we exceed the max number of node (should never be true)
				if(this.max_number_of_nodes > this.current_nodes.size()) {
					//there is enough space so i can add a node 
					//get a random node already in the network 
					int random = randomSource.nextInt(this.current_nodes.size());
					Node target = this.current_nodes.get(random);
					
					//add the node to the current nodes in the network list
					this.current_nodes.add(o);
					
					//schedule the join of the node in the next tick
					schedule_action(o, "join", target);
				}
			}
		}
		
		
		/*
		 * for every node in the network extract a value with the probability 
		 * of fail and if success schedule a fail 
		 * (has to be low in order to prevent to get in a situation of an empty chord ring)
		 * */
		//list of nodes set to be fail in order to not schedule also the leave
		ArrayList<Node> going_to_fail = new ArrayList<Node>();
		for (Node o : current_nodes) {
			if(StdRandom.bernoulli(this.fail_prob)) {
				//add the node to the going to fail list 
				going_to_fail.add(o);
				//schedule the fail
				schedule_action(o, "fail", null);
			}
		}
		//remove from current nodes all the nodes that are going to fail
		this.current_nodes.removeAll(going_to_fail);
		
		/*
		 * for every node in the network extract a value with the probability of leave and if 
		 * success, schedule a leave
		 * */
		ArrayList<Node> going_to_leave = new ArrayList<Node>();
		for(Node o : current_nodes) {
			if(StdRandom.bernoulli(this.leave_prob)) {
				//add the node to the list going to leave
				going_to_leave.add(o);
				//schedule the leave
				schedule_action(o, "leave", null);
			}
		}
		//remove all the nodes that are going to leave
		this.current_nodes.removeAll(going_to_leave);
		
		/*
		 * extract a number of lookup with lookup probability
		 * for every lookup chose a node randomly and ask that node to lookup for some key
		 * that as to be in the network (or not ?)
		 * */
		int number_of_lookup = Math.round(this.keys.size() * this.lookup_prob);
		
		for(int i = 0; i < number_of_lookup; i++) {
			//select a random node to ask for the lookup
			int random_node = randomSource.nextInt(this.current_nodes.size());
			Node target = this.current_nodes.get(random_node);
			
			//select a random keys
			int random_key = randomSource.nextInt(this.keys.size());
			BigInteger key = this.keys.get(random_key);
			
			//schedule the lookup for the next tick
			schedule_action(target, "lookup", key);
		}
		
		/*
		 * extract a number of key based on probability of a new key creation
		 * and insert them in the correct nodes
		 * */
		//the more keys are in the ring the less are inserted
		int number_new_key = Math.round((Integer.MAX_VALUE - this.keys.size()) * this.insertkey_prob);
		Key key_gen = new Key();
		for(int i = 0; i < number_new_key; i++) {
			//select a random node and ask him to insert the new key
			int random_node = randomSource.nextInt(this.current_nodes.size());
			Node target = this.current_nodes.get(random_node);
			
			//use the current time millis to generate a new key
			BigInteger new_key = key_gen.encryptThisString(Long.toString(System.currentTimeMillis()));
			
			//schedule the insertion of a new key
			schedule_action(target, "insert", new_key);
		}
		
		/*
		 * for each node that is in the network in this moment schedule a stabilize
		 * */
		//get the current tick
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		for(Node n : this.current_nodes) {
			//check if it is the time to schedule a stabilize
			if(tick_count % this.stabilize_tick == 0) {
				schedule_action(n, "stabilize", null);
			}
			//check if is the time to schedule a fixfinger
			if(tick_count % this.fix_finger_tick == 0) {
				schedule_action(n, "fixFinger", null);
			}
		}
	}
	
	private static void schedule_action(Node target, String method, Object parameters) {
		//schedule receive of a fins successor message in the next tick
		double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + 1); 
		RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters);
	}
}
