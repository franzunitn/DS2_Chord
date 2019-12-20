package dS2_Chord;

import dS2_Chord.Node;
import dS2_Chord.Key;
import dS2_Chord.Node_state;

import java.awt.print.Printable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
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
	private int new_keys;			//probability of an new key insertion
	
	private int stabilize_tick;
	private int fix_finger_tick;
	
	private static final BigInteger MAX_VALUE = BigInteger.ZERO.setBit(160).subtract(BigInteger.ONE);
	
	private int max_number_of_nodes;		//max number of nodes in the chord ring
	private ArrayList<BigInteger> keys = new ArrayList<BigInteger>();			//current keys in the ring
	private ArrayList<Node> current_nodes = new ArrayList<Node>();		//current nodes in the ring (initially empty)
	private ArrayList<Node> all_nodes;		//all the nodes added by the network builder
	
	private Dictionary<BigInteger, Integer> k;
	private Dictionary<BigInteger, Integer> d;
	
	private boolean test = false;

	public Super_node(float join_prob, float leave_prob, float fail_prob, float lookup_prob, int new_keys,
			int max_number_of_nodes, ArrayList<Node> current_nodes,  int stabilize_tick, int fix_finger_tick,
			Dictionary<BigInteger, Integer> d) {
		this.join_prob = join_prob;
		this.leave_prob = leave_prob;
		this.fail_prob = fail_prob;
		this.lookup_prob = lookup_prob;
		this.new_keys = new_keys;
		this.max_number_of_nodes = max_number_of_nodes;
		this.all_nodes = current_nodes;
		this.stabilize_tick = stabilize_tick;
		this.fix_finger_tick = fix_finger_tick;
		this.d = d;
		
		this.test = false;
	}

	//@ScheduledMethod (start = 1, interval = 1)
	/**
	 * Method to execute one step of the super node where all the behavior of the node are schedule, 
	 * for example fixfinger and stabilize method are schedule inside here.
	 */
	public void step() {
		print("---start step---");
		Random randomSource = new Random();
		Context<Object> context =  ContextUtils.getContext(this);
		
		/*
		 * for every node that is not already in the network (BUT for a total number < than max_numbeor of node)extract
		 * a value with the probability of join and if success schedule the join
		 * */
		//search all nodes that are or not in the network
		ArrayList<Node> nodes_to_join = new ArrayList<Node>();
		//all the nodes that are already active in this tick
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		
		for(Node o: this.all_nodes) {
			if(o.get_state() == Node_state.ACTIVE) {
				active_nodes.add(o);
			}
		}
		
		for(Node o: this.all_nodes) {
			if(o.get_state() == Node_state.INACTIVE && !current_nodes.contains(o)) {
				nodes_to_join.add(o);
			}
		}
		//scan all the nodes that aren't already in the network and for each see if can join
		for(Node o : nodes_to_join) {
			if(StdRandom.bernoulli(this.join_prob)) {
				//check if the we exceed the max number of node (should never be true)
				if(this.max_number_of_nodes > this.current_nodes.size()) {
					//case if the ring is empty
					if(this.current_nodes.size() == 0) {
						print("Node: " + d.get(o.getId()) + " has schedule the first join");
						//add the node in the current_nodes list
						this.current_nodes.add(o);
						//schedule the join of the node
						schedule_action(o, "join", o, true, 1);
					}else {
						if(active_nodes.size() > 0) {
							//there is enough space so i can add a node 
							//get a random node already in the network 
							int random = randomSource.nextInt(active_nodes.size());
							Node target = active_nodes.get(random);
							
							//add the node to the current nodes in the network list
							print("Node: " + d.get(o.getId()) + " has schedule a join");
							this.current_nodes.add(o);
							
							//schedule the join of the node in the next tick
							schedule_action(o, "join", target, false, 10);
						}
					}
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
				print("Node: " + d.get(o.getId()) + " has schedule a fail");
				//schedule the fail
				schedule_action(o, "fail", "", false, 1);
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
				print("Node: " + d.get(o.getId()) + " has schedule a leave");
				//schedule the leave
				schedule_action(o, "leave", "", false, 1);
			}
		}
		//remove all the nodes that are going to leave
		this.current_nodes.removeAll(going_to_leave);
		
		/*
		 * extract a number of lookup with lookup probability
		 * for every lookup chose a node randomly and ask that node to lookup for some key
		 * that as to be in the network (or not ?)
		 * */
		//in order to lookup a key there must be keys generated and also node in the ring
		if(this.current_nodes.size() > 0 && this.keys.size() > 0) {
			for(int i = 0; i < this.keys.size(); i++) {
				if(StdRandom.bernoulli(this.lookup_prob)) {
					//select a random node to ask for the lookup
					int random_node = randomSource.nextInt(this.current_nodes.size());
					Node target = this.current_nodes.get(random_node);
					
					//select a random keys
					int random_key = randomSource.nextInt(this.keys.size());
					BigInteger key = this.keys.get(random_key);
					
					print("Node: " + d.get(target.getId()) + " chose to lookup key: " + k.get(key));
					
					//schedule the lookup for the next tick
					schedule_action(target, "lookup", key, false, 1);
				}
			}
		}
		
		/*
		 * extract a number of key based on probability of a new key creation
		 * and insert them in the correct nodes
		 * */
		Key key_gen = new Key();
		//in order to insert a new keys there must be node in the ring
		if(this.current_nodes.size() > 0) {
			for(int i = 0; i < this.new_keys; i++) {
				//select a random node and ask him to insert the new key
				int random_node = randomSource.nextInt(this.current_nodes.size());
				Node target = this.current_nodes.get(random_node);
				
				//use the current time millis to generate a new key
				BigInteger new_key = key_gen.encryptThisString(Long.toString(System.currentTimeMillis()));
				
				//add the new key to the map dictionary
				this.k.put(new_key, this.k.size() + 1);
				
				//add the new kay in the array of keys
				this.keys.add(new_key);
				
				print("Node: " + d.get(target.getId()) + " chose to insert new_key: " + this.k.get(new_key));
				
				//schedule the insertion of a new key
				schedule_action(target, "insert", new_key, false, 1);
			}
		}
		
		/*
		 * for each node that is in the network in this moment schedule a stabilize
		 * */
		//get the current tick
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		for(Node o : active_nodes) {
			//check if it is the time to schedule a stabilize
			if(tick_count % this.stabilize_tick == 0) {
				//this is created because i dont'know why if i pass null throw an error
				Object a = new Object();
				
				schedule_action(o, "stabilize", a, false, 1);
				print("Node: " + d.get(o.getId()) + " schedule a stabilize");
			}
			//check if is the time to schedule a fixFingers
			if(tick_count % this.fix_finger_tick == 0) {
				schedule_action(o, "fixFingers", "", false, 1);
				print("Node: " + d.get(o.getId()) + " schedule a fixFingers");

			}
		}
		print("---Finish step---");
		
	}
	
	//@ScheduledMethod (start = 1, interval = 1)
	/**
	 * A simple test to run a simple configuration and check that all the nodes are good.
	 * make the join of 10 nodes and print the state of the node to check predecessor and successor
	 */
	public void simple_test() {
		/**
		 * test of join 10 tick delay one from another 
		 * all nodes make join
		 */
		
		//this is created because i dont'know why if i pass null throw an error
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
			schedule_action(this.all_nodes.get(4), "join", this.all_nodes.get(0), false, 10);
			schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(4), false, 20);
			schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(3), false, 30);
			schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(2), false, 40);
			schedule_action(this.all_nodes.get(5), "join", this.all_nodes.get(0), false, 50);
			schedule_action(this.all_nodes.get(6), "join", this.all_nodes.get(0), false, 60);
			schedule_action(this.all_nodes.get(7), "join", this.all_nodes.get(4), false, 70);
			schedule_action(this.all_nodes.get(8), "join", this.all_nodes.get(3), false, 80);
			schedule_action(this.all_nodes.get(9), "join", this.all_nodes.get(2), false, 90);
			
			
			schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
			schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
			schedule_action(this.all_nodes.get(2), "printActualState", a, false, 100);
			schedule_action(this.all_nodes.get(3), "printActualState", a, false, 100);
			schedule_action(this.all_nodes.get(4), "printActualState", a, false, 100);
			
			//fixFingers
			for(int i = 100; i<161 * 10;i+=10) {
				schedule_action(this.all_nodes.get(0), "fixFingers", this.all_nodes.get(2), false, i);
			}
			
			
			schedule_action(this.all_nodes.get(0), "printActualState", a, false, 1600);
			
			
			
			print("ALL NODES EVENTS SCHEDULED");
			/*schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 40);
			schedule_action(this.all_nodes.get(4), "join", this.all_nodes.get(1), false, 40);
			schedule_action(this.all_nodes.get(5), "join", this.all_nodes.get(1), false, 50);
			schedule_action(this.all_nodes.get(6), "join", this.all_nodes.get(2), false, 60);
			schedule_action(this.all_nodes.get(7), "join", this.all_nodes.get(2), false, 70);
			schedule_action(this.all_nodes.get(8), "join", this.all_nodes.get(3), false, 80);
			schedule_action(this.all_nodes.get(9), "join", this.all_nodes.get(3), false, 90);*/
		}
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			if(o.get_state() == Node_state.ACTIVE) {
				active_nodes.add(o);
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		for(Node o : active_nodes) {
			//check if it is the time to schedule a stabilize
			if(tick_count % this.stabilize_tick == 0) {
				
				schedule_action(o, "stabilize", a, false, 1);
				print("Node: " + d.get(o.getId()) + " schedule a stabilize");
			}
			//check if is the time to schedule a fixFingers
			/*
			if(tick_count % this.fix_finger_tick == 0) {
				schedule_action(o, "fixFingerss", "", false, 1);
				print("Node: " + d.get(o.getId()) + " schedule a fixFingers");

			}*/
		}
	}
	
	/**
	 * Another simple test to check if the find successor work good 
	 * and always returns the correct successor of a key.
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void test_find_successor() {
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			//schedule the join of three nodes
			schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
			schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
			schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 15);
			schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 20);
			
			//schedule print state for check the network 
			schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
			schedule_action(this.all_nodes.get(1), "printActualState", a, false, 50);
			schedule_action(this.all_nodes.get(2), "printActualState", a, false, 50);
			schedule_action(this.all_nodes.get(3), "printActualState", a, false, 50);
			
			//schedule the find successor to check if return the right value
			//for every node i check the immediate hash next and before every node
			schedule_action(this.all_nodes.get(0), "find_successor", this.all_nodes.get(0).getId().add(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(0), "find_successor", this.all_nodes.get(0).getId().subtract(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(1), "find_successor", this.all_nodes.get(1).getId().add(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(1), "find_successor", this.all_nodes.get(1).getId().subtract(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(2), "find_successor", this.all_nodes.get(2).getId().add(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(2), "find_successor", this.all_nodes.get(2).getId().subtract(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(3), "find_successor", this.all_nodes.get(3).getId().add(BigInteger.ONE), false, 60);
			schedule_action(this.all_nodes.get(3), "find_successor", this.all_nodes.get(3).getId().subtract(BigInteger.ONE), false, 60);
			
			//print again to see the finger table 
			//schedule print state for check the network 
			schedule_action(this.all_nodes.get(0), "printActualState", a, false, 500);
			schedule_action(this.all_nodes.get(1), "printActualState", a, false, 500);
			schedule_action(this.all_nodes.get(2), "printActualState", a, false, 500);
			schedule_action(this.all_nodes.get(3), "printActualState", a, false, 500);
			
			print("\n\n FINISH TEST FIND SUCCESSOR \n\n"); 
		}
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			if(o.get_state() == Node_state.ACTIVE) {
				active_nodes.add(o);
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		for(Node o : active_nodes) {
			//check if it is the time to schedule a stabilize
			if(tick_count % this.stabilize_tick == 0) {
				
				schedule_action(o, "stabilize", a, false, 1);
				print("Node: " + d.get(o.getId()) + " schedule a stabilize");
			}
			
			//check if is the time to schedule a fixFingers
			
			if(tick_count % this.fix_finger_tick == 0) {
				schedule_action(o, "fixFingers", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule a fixFingers");

			}
			
			//print the fingertable every 100 tick
			if(tick_count % 100 == 0) {
				schedule_action(o, "printActualState", a, false, 0);
			}
		}
		
	}
	
	/**
	 * A Test to fix finger function that every 100 steps print the finger table of node 0 to check if everythink is ok
	 */
	@ScheduledMethod (start = 1, interval = 1)
	public void test_fixfingers() {
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			for(int i = 0 ; i < this.max_number_of_nodes ; i++) {
				if(i == 0) {
					schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 1);
				}else {
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, i * 2);
				}
			}
		}
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			if(o.get_state() == Node_state.ACTIVE) {
				active_nodes.add(o);
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		for(Node o : active_nodes) {
			//check if it is the time to schedule a stabilize
			if(tick_count % this.stabilize_tick == 0) {
				
				schedule_action(o, "stabilize", a, false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule a stabilize");
			}
			
			//check if is the time to schedule a fixFingers
			
			if(tick_count % this.fix_finger_tick == 0) {
				schedule_action(o, "fixFingers", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule a fixFingers");

			}
			
			//print the fingertable every 100 tick
			if(tick_count % 10 == 0) {
				if(o.getId().compareTo(this.all_nodes.get(0).getId()) == 0) {
					schedule_action(o, "printActualState", a, false, 0);
				}
			}
		}
	}
	
	private static void schedule_action(Node target, String method, Object parameters , boolean is_first, int delay) {
		//schedule receive of a fins successor message in the next tick
		double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + delay); 
		
		switch(method) {
			case "join" : 
				if(is_first) {
					RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters, true);
					break;
				}else {
					RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters, false);
					break;
				}
			case "fail" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "leave" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "lookup" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters);
				break;
			case "insert" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters);
				break;
			case "stabilize" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "fixFingers" : 
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "printActualState":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "find_successor":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters);
				break;
			default : 
				System.out.println("In supernode schedule_action: Method not recognized " + method);
		}
	}
	
	private void print(String s) {
		System.out.println(s);
	}
	
	public int get_mapped_id(BigInteger id) {
		return this.d.get(id);
	}
	
	public int get_mapped_key(BigInteger k_id) {
		return this.k.get(k_id);
	}
	
	
}
