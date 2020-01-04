package dS2_Chord;

import dS2_Chord.Node;
import dS2_Chord.Key;

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
	
	private boolean insert_done = false;
	
	private int max_number_of_keys;
	private int stable_counter;
	private boolean stable;
	private int finish;

	public Super_node(float join_prob, float leave_prob, float fail_prob, float lookup_prob, int new_keys,
			int max_number_of_nodes, ArrayList<Node> current_nodes,  int stabilize_tick, int fix_finger_tick,
			Dictionary<BigInteger, Integer> d, int max_number_of_keys) {
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
		this.k = new Hashtable();
		this.test = false;
		this.max_number_of_keys = max_number_of_keys;
		this.stable_counter = 0;
		this.stable = false;
		this.finish = 0;
	}

	/**
	 * Method to execute one step of the super node where all the behavior of the node are schedule, 
	 * for example fixfinger and stabilize method are schedule inside here.
	 */
	//@ScheduledMethod (start = 1, interval = 1)
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
		ArrayList<Node> failed_nodes = new ArrayList<Node>();
		
		for(Node o: this.all_nodes) {
			//if o is ACTIVE
			if(o.get_state() == 0) {
				active_nodes.add(o);
			}
			//if a node is INACTIVE
			if(o.get_state() == 1 /*&& !current_nodes.contains(o) */) {
				nodes_to_join.add(o);
			}
			if (o.get_state() == 2 && !failed_nodes.contains(o)) {
				failed_nodes.add(o);
			}
		}
		/*print("Active nodes: " + active_nodes.size() + 
				", nodes_to_join are: " + nodes_to_join.size() + 
				", failed nodes are: " + failed_nodes.size() + 
				", Total:" + this.all_nodes.size());*/
		

		ArrayList<Node> joined_nodes = new ArrayList<Node>();
		//scan all the nodes that aren't already in the network and for each see if can join
		for(Node o : nodes_to_join) {
			if(StdRandom.bernoulli(this.join_prob)) {
				//check if the we exceed the max number of node (should never be true)
				if(this.max_number_of_nodes > this.current_nodes.size()) {
					//case if the ring is empty
					if(this.current_nodes.size() == 0) {
						//print("Node: " + d.get(o.getId()) + " has schedule the first join");
						//add the node in the current_nodes list
						if(!this.current_nodes.contains(o))
							this.current_nodes.add(o);
						//schedule the join of the node
						schedule_action(o, "join", o, true, 1);
						joined_nodes.add(o);
					}else {
						if(active_nodes.size() > 0) {
							//there is enough space so i can add a node 
							//get a random node already in the network 
							int random = randomSource.nextInt(active_nodes.size());
							Node target = active_nodes.get(random);
							
							//add the node to the current nodes in the network list
							//print("Node: " + d.get(o.getId()) + " has schedule a join");
							if(!this.current_nodes.contains(o))
								this.current_nodes.add(o);
							
							//schedule the join of the node in the next tick
							schedule_action(o, "join", target, false, 10);
							joined_nodes.add(o);
						}
					}
				} else {
					print("REACHED MAX SIZE");
				}
			}
		}
		nodes_to_join.removeAll(joined_nodes);
		
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
				//print("Node: " + d.get(o.getId()) + " has schedule a fail");
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
				//print("Node: " + d.get(o.getId()) + " has schedule a leave");
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
					
					//print("Node: " + d.get(target.getId()) + " chose to lookup key: " + k.get(key));
					
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
				BigInteger new_key = key_gen.encryptThisString(Integer.toString(randomSource.nextInt()));
				
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
		Object a = new Object();
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
			//check predecessor procedure
			if(tick_count % this.stabilize_tick*4 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");
			}
			
			if(tick_count % 10 == 0) {
				schedule_action(o, "printActualState", o, false, 1);
			}
		}
		print("---Finish step---");
		
	}
	
	/**
	 * Simple test to check if the join function work good 
	 * and always returns the correct successor of a key, 
	 * and also if with the stabilize and the fix fingers the 
	 * ring stabilize after a while
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void testJoin() {
		Object a = new Object();
		
		if(!this.test) {
			this.test = true;
			if (true) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				for (Node n : this.all_nodes) {
					if (!n.equals(this.all_nodes.get(0))) {
						schedule_action(n, "join", this.all_nodes.get(0), false, 5);
					}
				}
			}
			//simple join test where the nodes join in order (0->1->2-> .. ) and join to the same node (0)
			if(false) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 20);
				
				//schedule a print state to check the correctness tick 20
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 30);
				//tick 40 
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 50);
				//add test for the insertion of a key 
				Key key_gen = new Key();
				BigInteger new_key = key_gen.encryptThisString("Hello");
				schedule_action(this.all_nodes.get(3), "insert", new_key, false, 55);
			}
			
			//join test where the node join in different order (0->3->2->1->...) but to the same node (0)
			if(false) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 20);
				
				//schedule a print state to check the correctness tick 30
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 30);
				//tick 40 
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 40);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 40);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 40);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 40);
				
				
				
				
			}
			
			//join test where the node join not in order and more than one in the same tick but all to the same node (0)
			if(false) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 10);
				
				//schedule a print state to check the correctness tick 30
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 30);
				//tick 60 
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 60);
			}
			
			//join test where the node join not in order and more than one in the same tick and not in order
			if(false) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 20);
				schedule_action(this.all_nodes.get(4), "join", this.all_nodes.get(0), false, 30);
				schedule_action(this.all_nodes.get(6), "join", this.all_nodes.get(1), false, 30);
				schedule_action(this.all_nodes.get(5), "join", this.all_nodes.get(3), false, 30);
				
				//schedule a print state to check the correctness tick 30
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(4), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(5), "printActualState", a, false, 30);
				schedule_action(this.all_nodes.get(6), "printActualState", a, false, 30);
				//tick 60 
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(4), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(5), "printActualState", a, false, 60);
				schedule_action(this.all_nodes.get(6), "printActualState", a, false, 60);
				
			}
			
			print("\n\n FINISH TEST FIND SUCCESSOR \n\n"); 
		}
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is ACTIVE
			if(o.get_state() == 0) {
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
		
		print("CURRENT ACTIVE NODES: " + active_nodes.size());
		
	}
	
	
	/**
	 * test if a node that leave the network behave well
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void test_leave() {
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			
			//test if there is only one node in the network and leave
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 10);
			}
			
			//test if some nodes leave in order
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 25);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 35);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(3), "printActualState", a, false, 50);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(3), "leave", this.all_nodes.get(0), false, 60);
				schedule_action(this.all_nodes.get(2), "leave", this.all_nodes.get(0), false, 70);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 80);
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 90);
			}
			
			//test if some node leave at the same tick
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 25);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 35);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 50);
				
				//schedule leave
				schedule_action(this.all_nodes.get(3), "leave", this.all_nodes.get(0), false, 60);
				schedule_action(this.all_nodes.get(2), "leave", this.all_nodes.get(0), false, 60);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 60);
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 60);
			}
			
			//test if the finger table is fixed from check predecessor after some ticks
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 15);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 25);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 35);
				schedule_action(this.all_nodes.get(4), "join", this.all_nodes.get(0), false, 45);
				schedule_action(this.all_nodes.get(5), "join", this.all_nodes.get(0), false, 55);
				
				//schedule leave
				schedule_action(this.all_nodes.get(2), "leave", this.all_nodes.get(0), false, 100);
				schedule_action(this.all_nodes.get(3), "leave", this.all_nodes.get(0), false, 100);
				schedule_action(this.all_nodes.get(5), "leave", this.all_nodes.get(0), false, 101);
				
			}
		}
		
		
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
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
				print("Node: " + d.get(o.getId()) + " schedule a fixFingers");

			}
			
			//check predecessor procedure
			if(tick_count % (this.stabilize_tick * 5)  == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				print("Node: " + d.get(o.getId()) + " schedule check_predecessor");
			}
			
			//check predecessor procedure
			if(tick_count % 5  == 0) {
				schedule_action(o, "printActualState", a, false, 1);
			}
		}
		
		print("CURRENT ACTIVE NODES: " + active_nodes.size());
	}
	
	
	/**
	 * test if a use correctly the fingertable
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void test_finger() {
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			
			//test if some nodes leave in order
			if(true) {
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 2);
				int tick = 3;
				
				//all others
				for (int i = 1; i < this.all_nodes.size(); i++) {
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, tick);
					tick = tick + 5;
				}
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(10), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(20), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(30), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(40), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(50), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(60), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(70), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(80), "printActualState", a, false, 10000);
				schedule_action(this.all_nodes.get(99), "printActualState", a, false, 10000);
				
			}
		}
		
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
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
			
			//check predecessor procedure
			if(tick_count % (this.stabilize_tick * 5) == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");

			}
		}
	}
	
	/**
	 * test if keys are handled correctly
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void test_keys() {
		Object a = new Object();
		if(!this.test) {
			this.test = true;
			
			//test if one key is inserted correctly
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 210);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 210);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test if three keys are inserted correctly with message passing to successor
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				BigInteger key_three = k.encryptThisString("key_three");
				schedule_action(this.all_nodes.get(1), "insert", key_one, true, 200);
				schedule_action(this.all_nodes.get(1), "insert", key_two, true, 210);
				schedule_action(this.all_nodes.get(1), "insert", key_three, true, 220);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test if three keys are inserted correctly with message passing using the fingertable
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 20);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				BigInteger key_three = k.encryptThisString("key_three");
				schedule_action(this.all_nodes.get(1), "insert", key_one, true, 200);
				schedule_action(this.all_nodes.get(1), "insert", key_two, true, 210);
				schedule_action(this.all_nodes.get(1), "insert", key_three, true, 220);
				
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(2), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test duplicate key on the same node
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 205);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 210);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 210);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test duplicate key on different nodes
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				schedule_action(this.all_nodes.get(1), "insert", key_one, true, 205);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 210);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 210);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test lookup on the handler node
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				
				//Schedule the key lookup
				schedule_action(this.all_nodes.get(0), "lookup", key_one, true, 210);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test lookup on the handler node of an unknown key
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				
				//Schedule the key lookup
				schedule_action(this.all_nodes.get(0), "lookup", key_two, true, 210);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test lookup on the successor of a known key and an unknown key
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				
				//Schedule the key lookup
				schedule_action(this.all_nodes.get(1), "lookup", key_one, true, 210);
				schedule_action(this.all_nodes.get(1), "lookup", key_two, true, 220);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test lookup on the successor of a known key and an unknown key
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 20);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				schedule_action(this.all_nodes.get(0), "insert", key_one, true, 200);
				
				//Schedule the key lookup
				schedule_action(this.all_nodes.get(1), "lookup", key_one, true, 210);
				schedule_action(this.all_nodes.get(1), "lookup", key_two, true, 220);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 250);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 250);
				
				//schedule the leave
				schedule_action(this.all_nodes.get(0), "leave", this.all_nodes.get(0), false, 300);
				schedule_action(this.all_nodes.get(1), "leave", this.all_nodes.get(0), false, 300);
			}
			
			//test lookup 
			if(false) {
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 20);
				schedule_action(this.all_nodes.get(3), "join", this.all_nodes.get(0), false, 30);
				schedule_action(this.all_nodes.get(4), "join", this.all_nodes.get(0), false, 31);
				schedule_action(this.all_nodes.get(5), "join", this.all_nodes.get(0), false, 32);
				schedule_action(this.all_nodes.get(6), "join", this.all_nodes.get(0), false, 33);
				schedule_action(this.all_nodes.get(7), "join", this.all_nodes.get(0), false, 34);
				schedule_action(this.all_nodes.get(8), "join", this.all_nodes.get(0), false, 35);
				schedule_action(this.all_nodes.get(9), "join", this.all_nodes.get(0), false, 36);
				
				//print status
				schedule_action(this.all_nodes.get(0), "printActualState", a, false, 100);
				schedule_action(this.all_nodes.get(1), "printActualState", a, false, 100);
				
				//Schedule the key insertion
				Key k = new Key();
				BigInteger key_one = k.encryptThisString("key_one");
				BigInteger key_two = k.encryptThisString("key_two");
				schedule_action(this.all_nodes.get(3), "insert", key_one, true, 50);
				schedule_action(this.all_nodes.get(2), "insert", key_two, true, 51);
				
				//Schedule the key lookup
				schedule_action(this.all_nodes.get(1), "lookup", key_one, true, 60);
				schedule_action(this.all_nodes.get(1), "lookup", key_two, true, 65);
			}
			
		}
		
		
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
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
			
			//check predecessor procedure
			if(tick_count % 20 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");

			}
		}
		
		print("CURRENT ACTIVE NODES: " + active_nodes.size());
	}
	
	
	
	
	/**
	 * test the networks graphic 
	 */
	//@ScheduledMethod (start = 1, interval = 1)
	public void test_networks() {
		Object a = new Object();
		Random randomSource = new Random();
	
		if(!this.test) {
			this.test = true;
			//test graphic 
			if(true) {
				//all nodes join the network 
				//first special case 
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 2);
				int tick = 3;
				
				//all others
				for (int i = 1; i < this.all_nodes.size(); i++) {
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, tick);
					tick = tick + 3;
				}
				//wait some time before the insertion of keys 
				tick = tick + 15;
				
				//Schedule the key insertion of 5 keys by random nodes in the netrowk
				final int NUMBER_OF_INSERION = 5;
				Key k = new Key();
				ArrayList<BigInteger> inserted_keys = new ArrayList<BigInteger>();
				for (int j = 0; j < NUMBER_OF_INSERION; j++) {
					//random for the keys
					int random = randomSource.nextInt(100);
					//random for choose a random node 
					int random2 = randomSource.nextInt(this.all_nodes.size());
					//creation of the key 
					BigInteger key = k.encryptThisString("key_" + random);
					//schedule the insertion of the key 
					schedule_action(this.all_nodes.get(random2), "insert", key, true, tick);
					//add the key to a list for schedule lookups later 
					inserted_keys.add(key);
					//insertion every 3 ticks 
					tick = tick + 3;
				}
				
				
				//wait some time before try to lookup the inserted keys 
				tick = tick +15;
				//lookups for all the keys that were inserted before 
				for (BigInteger key : inserted_keys) {
					//choose a random node to lookup for a key 
					int random = randomSource.nextInt(this.all_nodes.size());
					schedule_action(this.all_nodes.get(random), "lookup", key, true, tick);
					tick = tick + 3;
				}
				
			}
			
			if(true) {
				int number_of_key = 100000;
				
				//schedule all the join
				schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 5);
				schedule_action(this.all_nodes.get(1), "join", this.all_nodes.get(0), false, 10);
				schedule_action(this.all_nodes.get(2), "join", this.all_nodes.get(0), false, 20);
				
				Key k = new Key();
				for(int i =0; i< number_of_key;i++){	
					BigInteger rand_key = k.encryptThisString("key_one" + i);
					schedule_action(this.all_nodes.get(2), "insert",rand_key, true, 50 +i);
				}
				
				//print status
				schedule_action(this.all_nodes.get(0), "print_key_size", a, false, number_of_key + 100);
				schedule_action(this.all_nodes.get(1), "print_key_size", a, false, number_of_key + 100);
				schedule_action(this.all_nodes.get(2), "print_key_size", a, false, number_of_key + 100);
			}
			
		}
		
		
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
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
			
			//check predecessor procedure
			if(tick_count % 20 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");

			}
		}
		
		print("CURRENT ACTIVE NODES: " + active_nodes.size());
	}
	
	
	//@ScheduledMethod(start = 1, interval = 1)
	public void num_key_per_node_test() {
		if(!this.test) {
			this.test = true;
			
			//first of all schedule the join of the nodes and reach a stable state
			for(int i = 0; i < this.max_number_of_nodes; i++) {
				if(i == 0) {
					schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 1);
				}else {
					
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, i + 3);
				}
			}
		}
		
		Random rand_generator = new Random();
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
				active_nodes.add(o);
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Object a = new Object();
		
		//schedule_action(this.all_nodes.get(1), "printActualState", a, false, 0);
		//schedule_action(this.all_nodes.get(0), "printActualState", a, false, 0);
		if(active_nodes.size() == this.max_number_of_nodes && !insert_done) {
			if(this.stable) {
				print(" ----------Entered the insert procedure----------- ");
				insert_done = true;
				int keys = 0;
				Key key_generator = new Key();
				
				while(keys < this.max_number_of_keys) {
					BigInteger random_key = key_generator.encryptThisString("" + keys + rand_generator.nextInt());
					if(!this.keys.contains(random_key)) {
						this.keys.add(random_key);
						Node random_node = this.all_nodes.get(rand_generator.nextInt(active_nodes.size()));
						schedule_action(random_node, "insert", random_key, false, rand_generator.nextInt(100));
						keys++;
						if(keys % 1000 == 0) {
							print("scheduled: " + keys/1000 + "/" + this.max_number_of_keys/1000 + "K");
						}
					}
				}
				print(" ----------Exit the insert procedure----------- ");
				this.finish = tick_count + 300;
				RunEnvironment.getInstance().endAt(this.finish);
			}else {
				print("waiting: " + this.stable_counter + "/3000");
				this.stable_counter++;
				if(this.stable_counter >= 3000) {
					this.stable = true;
				}
				
			}
		}else {
			if(active_nodes.size() == this.max_number_of_nodes) {
				print("Mancano: " + (this.finish - tick_count) + " Tick");
			}
		}
		
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
			
			//check predecessor procedure
			if(tick_count % this.stabilize_tick * 4 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");

			}
		}
		
		if(tick_count % 50 == 0) {
			schedule_action(this.all_nodes.get(0), "printActualStateMinimal", a, false, 1);
		}
		
		if(active_nodes.size() < this.max_number_of_nodes) {
			print("ACTIVE NODES: " + active_nodes.size());
		}
		
		if(tick_count % 2999 == 0) {
			for(Node n : active_nodes) {
				schedule_action(n, "printActualState", a, false, 1);
			}
		}
	}
	
	//@ScheduledMethod(start = 1, interval = 1)
	public void path_lengh_test() {
		if(!this.test) {
			this.test = true;
			
			//first of all schedule the join of the nodes and reach a stable state
			for(int i = 0; i < this.max_number_of_nodes; i++) {
				if(i == 0) {
					schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 1);
				}else {
					
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, i + 3);
				}
			}
		}
			
		Random rand_generator = new Random();
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
				active_nodes.add(o);
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Object a = new Object();
		
		//schedule_action(this.all_nodes.get(1), "printActualState", a, false, 0);
		//schedule_action(this.all_nodes.get(0), "printActualState", a, false, 0);
		if(active_nodes.size() == this.max_number_of_nodes && !insert_done) {
			if(this.stable) {
				print(" ----------Entered the insert procedure----------- ");
				insert_done = true;
				int keys = 0;
				Key key_generator = new Key();
				
				while(keys < this.max_number_of_keys) {
					BigInteger random_key = key_generator.encryptThisString("" + keys + rand_generator.nextInt());
					if(!this.keys.contains(random_key)) {
						this.keys.add(random_key);
						Node random_node = this.all_nodes.get(rand_generator.nextInt(active_nodes.size()));
						schedule_action(random_node, "insert", random_key, false, rand_generator.nextInt(100));
						keys++;
						if(keys % 1000 == 0) {
							print("scheduled: " + keys/1000 + "/" + this.max_number_of_keys/1000 + "K");
						}
					}
				}
				print(" ----------Exit the insert procedure----------- ");
				this.finish = tick_count + 300;
				RunEnvironment.getInstance().endAt(this.finish);
			}else {
				print("waiting: " + this.stable_counter + "/3000");
				this.stable_counter++;
				if(this.stable_counter >= 3000) {
					this.stable = true;
				}
				
			}
		}else {
			if(active_nodes.size() == this.max_number_of_nodes) {
				print("Mancano: " + (this.finish - tick_count) + " Tick");
			}
		}
		
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
			
			//check predecessor procedure
			if(tick_count % this.stabilize_tick * 4 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");
				}
		}
		
		if(tick_count % 50 == 0) {
			schedule_action(this.all_nodes.get(0), "printActualStateMinimal", a, false, 1);
		}
		
		if(active_nodes.size() < this.max_number_of_nodes) {
			print("ACTIVE NODES: " + active_nodes.size());
		}
		
		if(tick_count % 2999 == 0) {
			for(Node n : active_nodes) {
				schedule_action(n, "printActualState", a, false, 1);
			}
		}
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void test_equal_range() {
		if(!this.test) {
			this.test = true;
			
			//first of all schedule the join of the nodes and reach a stable state
			for(int i = 0; i < this.max_number_of_nodes; i++) {
				if(i == 0) {
					schedule_action(this.all_nodes.get(0), "join", this.all_nodes.get(0), true, 1);
				}else {
					schedule_action(this.all_nodes.get(i), "join", this.all_nodes.get(0), false, i + 3);
				}
			}
		}
		
		ArrayList<Node> active_nodes = new ArrayList<Node>();
		for(Node o: this.all_nodes) {
			//if a node is active
			if(o.get_state() == 0) {
				active_nodes.add(o);
			}else {
				print("Node : not active--> " + o.getSuperNodeNameForMe());
			}
		}
		
		int tick_count = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Object a = new Object();
		
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
			
			//check predecessor procedure
			if(tick_count % this.stabilize_tick * 4 == 0) {
				schedule_action(o, "check_predecessor", "", false, 1);
				//print("Node: " + d.get(o.getId()) + " schedule check_predecessor");
				}
			
			if(tick_count % 100 == 0) {
				if(o.getId().compareTo(this.all_nodes.get(0).getId()) == 0) {
					schedule_action(o, "calculate_range_id", a, true, 1);
				}else {
					schedule_action(o, "calculate_range_id", a, false, 1);
				}
				
			}
		}
		
		
		print("ACTIVE NODES: " + active_nodes.size());
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
			case "printActualStateMinimal":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "find_successor":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, parameters);
				break;
			case "check_predecessor":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "check_finger_table":
				RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
				break;
			case "calculate_range_id":
				if(is_first) {
					RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, true);
					break;
				}else {
					RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, false);
					break;
				}
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
