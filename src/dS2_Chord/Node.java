package dS2_Chord;
import dS2_Chord.Find_successor_message;
import dS2_Chord.Find_predecessor_message;
import dS2_Chord.Key;
import dS2_Chord.Raw;
import dS2_Chord.FingerTable;
import dS2_Chord.Util;
import dS2_Chord.Node_state;

import java.awt.Color;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduleParameters;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import repast.simphony.context.Context;


public class Node{

	private BigInteger id;
	private static final int bigIntegerBits = 160; //AKA m in the paper
	private Node successor;
	private Node predecessor;
	private FingerTable fingertable;
	private boolean is_join;
	private static final BigInteger MAX_VALUE = BigInteger.ZERO.setBit(Node.bigIntegerBits).subtract(BigInteger.ONE);
	private int next;

	private Node_state state;
	

	private ArrayList<BigInteger> mykeys;

	//Node Constructor
	public Node(BigInteger id) {
		this.id = id;
		this.successor = null;
		this.predecessor = null;
		this.fingertable = new FingerTable(Node.bigIntegerBits, this);
		this.is_join = false;
		this.next = 0;
		this.state = Node_state.INACTIVE;
		this.mykeys = new ArrayList<BigInteger>();
	}
	
	
	/*
	 * Method used to join the chord ring. 
	 * In order to join the ring MUST know a node already in the ring
	 * @param n a node already in the ring
	 * */ 
	public void join(Node n, boolean is_first) {
		if(!(this.state == Node_state.FAILED)) {
			//check if node n is still in the ring or has fail or leave
			if(is_first) {
				//special case to join
				this.predecessor = null;
				this.successor = this;
				//set the state of the node to active
				this.state = Node_state.ACTIVE;
			}else {
				this.predecessor = null;
				//send join message
				Find_successor_message m = new Find_successor_message(this);
				
				//schedule the receive of a message
				schedule_message(n, "on_find_successor_receive", m, 1);
			}	
		}
	}
	
	/**
	 * Every stabilize send a message to the successor asking for his predecessor.
	 * if the predecessor is between this node and his successor than i set my successor
	 * to the predecessor and that notify the node that i become his predecessor.
	 */
	public void stabilize() {
		if(!(this.state == Node_state.FAILED)) {
			//find my successor predecessor
			Find_predecessor_message m = new Find_predecessor_message(this);
			
			//schedule the arrival of the message in my successor node
			schedule_message(this.successor, "on_find_predecessor_receive", m, 1);
		}
	}
	
	/**
	 * When receive a request from a node that want to know my predecessor 
	 * just reply with my predecessor if is not null.
	 * @param m the message that I receive
	 */
	public void on_find_predecessor_receive(Find_predecessor_message m) {
		if(!(this.state == Node_state.FAILED)) {
			//reply with my predecessor (if not null)
			if(this.predecessor != null) {
				//schedule the reception of the reply
				schedule_message(m.source, "on_find_predecessor_reply", this, 1);
			
				//maybe i can set my predecessor to the node asking for that
				this.predecessor = m.source;
			}else {
				//WAIT CAN BE NULL ? AND IN THIS CASE ?
				//find predecessor of me-1 ?
			}
		}
	}
		
	/**
	 * A reply to a find predecessor request. 
	 * if my successor has changed i set my successor predecessor
	 * to my successor and than notify him.
	 * @param x
	 */
	public void on_find_predecessor_reply(Node x) {
		if(!(this.state == Node_state.FAILED)) {
			//if the predecessor of my successor is between me and my successor 
			//i set my successor to him and than notify him.
			if(check_interval(this.id, this.successor.getId(), x.getId())) {
				this.successor = x;
			}
			
			//schedule the receiving of the notification
			schedule_message(this.successor, "notification", this, 1);
		}
	}

	/**
	* The receiving of a notification means that the node before me has
	* discovered be so i set my predecessor to him.
	* @param n The node that discover that i'm his successor
	*/
	private void notification(Node n) {
		if(!(this.state == Node_state.FAILED)) {
			if(this.predecessor == null || check_interval(this.predecessor.getId(), this.id, n.getId())) {
				this.predecessor = n;
				//select the keys to send to my predecessor
				BigInteger node_id = n.getId();
				ArrayList<BigInteger> to_transfer = new ArrayList<BigInteger>();
				for (BigInteger k : mykeys) {
					if (k.compareTo(node_id) <= 0) {
						to_transfer.add(k);
					}
				}
				//remove all the keys to transfer from me
				this.mykeys.removeAll(to_transfer);
				//create a new Transfer_keys message
				Transfer_message m = new Transfer_message(this, n, to_transfer);
				//schedule the receiving of the transfer message
				schedule_message(n, "on_transfer_message", m, 1);
			}
		}
	}
	
	
	
	
	/**
	 * Periodic function used to update the finger table
	 */
	public void fixFingers() {
		if(!(this.state == Node_state.FAILED)) {
			this.next = this.next + 1;
			if(next > this.bigIntegerBits){
				this.next = 1; // Il primo elemento della fingertable è il nodo stesso e quello non deve essere modificato (credo)
			}
			
			//Find the closest node to this id plus two ^ next-1, I applied the module to respect the circle
			Node n = find_successor(this.id.add(Util.two_exponential(next-1)).mod(Node.MAX_VALUE));
			this.fingertable.setNewNode(this.next, n);
		}
	}
	
	/**
	 * Find the successor of the passed id
	 * @param i id
	 * @return the nearest known node to the id
	 */
	public Node find_successor(BigInteger i){
		if (check_interval(this.getId(), this.successor.getId(), i)) {
			return this.successor;
		} else {
			Node n_prime = closest_preceding_node(i);
			return n_prime;
		}
	}
	
	/**
	 * function that find the closest preceding node looking into the finger table
	 * @param id id searched
	 * @return the closest node known
	 */
	private Node closest_preceding_node(BigInteger id) {
		for(int i = this.bigIntegerBits; i > 0; i--) {
			if(check_interval(this.id, id, this.fingertable.getIndex(i))) {
				return this.fingertable.getNode(i);
			}
		}
		return this;
	}
	

	private void check_predecessor() {
		//TODO determine how to check if a predecessor has failed
		//send a message and check if doesn't reply ? 
		//check a predecessors variable ?
		if(!(this.state == Node_state.FAILED)) {
			if(this.predecessor != null) {
				//some message to check the state of my predecessor
			}
		}
	}
	
	public void leave() {
		if(!(this.state == Node_state.FAILED)) {
			//check if i'm the last one
			if(this.successor.id.compareTo(this.id) == 0) {
				this.mykeys.clear();
			}
			else {
				//transfer the keys with a message of type : transfer_message
				Transfer_message m = new Transfer_message(this, this.successor, this.mykeys);
				//schedule the message in THIS tick
				schedule_message(this.successor, "on_transfer_message", m, 1);
			}
		}
	}
	
	public void on_transfer_message(Transfer_message m){
		if(!(this.state == Node_state.FAILED)) {
			//acquire the keys
			this.mykeys.addAll(m.keys);
		}

	}
	/**
	 * Method used to simulate a node failure
	 */
	public void fail() {
		//lose all the key stored in the node
		this.mykeys.clear();
		//simply set the state to FAILED and stop participating in the protocol
		this.state = Node_state.FAILED;
	}
	
	//lookup send a message to the right node 
	public void lookup(BigInteger key) {
		Node target = find_successor(key);
		schedule_message(target, "check_element", key, 1);
		
	}
	//check if the key is present in the node 
	public BigInteger check_element(BigInteger key) {
		if (this.mykeys.contains(key)) {
			//for simulation we return only the key if present in real case will be the object associated to that key 
			return key;
		} else {
			return null;
		}
	}
	
	//find the node reliable of the key 
	public void insert(BigInteger new_key) {
		Node target = find_successor(new_key);
		schedule_message(target, "addKey", new_key, 1);
		//SOULD BE USED A TRANSFER MESSAGE ?
	}
	
	public void addKey (BigInteger key) {
		this.mykeys.add(key);
	}
	
	
	/**
	 * find the correct successor for a message join.
	 * if the successor is this node schedule a message with me as successor
	 * if the target is in my interval return a message with my successor as successor
	 * else forward the message to the highest preceding node of my finger table
	 * @param m the message that arrive
	 */
	public void on_find_successor_receive(Find_successor_message m) {
		if(!(this.state == Node_state.FAILED)) {
			BigInteger target_id = m.source.getId();
			
			Node closest = find_successor(target_id);
			
			if (equal_than(this.successor.getId(), closest.getId())) {
				schedule_message(m.source, "on_receive_join_reply", this.successor, 1);
				return;
			}
			
			//forward message to closest preceding node by schedule a message Find_successor_message
			schedule_message(closest, "on_find_successor_receive", m, 1);
		}
	}
	
	
	/**
	 * After i have done the join, my correct successor notify me that he is my successor
	 * @param successor the node i have to set as successor
	 */
	private void on_receive_join_reply(Node successor) {
		if(!(this.state == Node_state.FAILED)) {
			//set my successor
			this.successor = successor;
			//set my state to active
			this.state = Node_state.ACTIVE;
		}
	}
	
	/**
	 * this method given start and end of an interval return if the target is in the interval
	 * @param start the starting point in the chord ring interval
	 * @param finish the end point in the chord ring interval
	 * @param target the target id to be checked
	 * @return true if target is between start and finish
	 */
	//TODO inserire la possibilità di considerare o meno gli estrmi
	private boolean check_interval(BigInteger start, BigInteger finish, BigInteger target) {
		
		//start has to be different from finish
		assert(!equal_than(start, finish));
		
		if(bigger_than(finish, start)) {
			//base case finish is > than start
			return bigger_than(start, target) && less_than_equal(target, finish);
		}else if(less_than(finish, start)) {
			//case in which the interval contain the start of the 
			return (bigger_than(target, finish) && less_than_equal(target, this.MAX_VALUE) || 
					(bigger_than_equal(target, BigInteger.ZERO) && less_than_equal(target, finish)));
		}
		return false;
	}
	
	/**
	 * Method used to schedule a message in the next tick
	 * @param target the node in which i want to call the method
	 * @param method the method of node target to be called in the next tick 
	 * @param message the parameters of the method
	 * @param delay how many tick in advance schedule the event
	 */
	private static void schedule_message(Node target, String method, Object message, int delay) {
		//schedule receive of a fins successor message in the next tick
		double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + delay); 
		RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, message);
	}

	public boolean is_join() {
		return this.is_join;
	}

	public BigInteger getId() {
		return id;
	}

	public Node_state get_state() {
		return this.state;
	}

	public void setId(BigInteger id) {
		this.id = id;
	}
	
	private boolean less_than(BigInteger start, BigInteger finish) {
		if(start.compareTo(finish) == -1) {
			return true;
		}else {
			return false;
		}
	}
	
	private boolean equal_than(BigInteger start, BigInteger finish) {
		if(start.compareTo(finish) == 0) {
			return true;
		}else {
			return false;
		}
	}
	
	private boolean bigger_than(BigInteger start, BigInteger finish) {
		if(start.compareTo(finish) == 1) {
			return true;
		}else {
			return false;
		}
	}
	
	private boolean less_than_equal(BigInteger start, BigInteger finish) {
		return less_than(start, finish) || equal_than(start, finish);
	}
	
	private boolean bigger_than_equal(BigInteger start, BigInteger finish) {
		return bigger_than(start, finish) || equal_than(start, finish);
	}
}
