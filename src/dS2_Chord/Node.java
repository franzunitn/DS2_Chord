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
	
	private boolean predecessor_has_reply;

	private Node_state state;
	
	private ArrayList<BigInteger> mykeys;

	public Super_node snode;
	
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
		this.predecessor_has_reply = false;
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
				print("Node: " + snode.get_mapped_id(this.id) + " has join for FIRST");
				//special case to join
				this.predecessor = null;
				this.successor = this;
				//set the state of the node to active
				this.state = Node_state.ACTIVE;
			}else {
				print("Node: " + snode.get_mapped_id(this.id) + " start join procedure");
				this.predecessor = null;
				//send join message
				Find_successor_message m = new Find_successor_message(this);
				print("Node: " + snode.get_mapped_id(this.id) + " has asked to " + snode.get_mapped_id(n.getId()) + " to find his successor");
				//schedule the receive of a message
				schedule_message(n, "on_find_successor_receive", m, 1);
			}	
		}
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
			print("Node: " + snode.get_mapped_id(this.id) + " has received a request of join from: " + 
		snode.get_mapped_id(m.source.getId()));

			BigInteger target_id = m.source.getId();
			
			Node closest = find_successor(target_id);
			
			if (equal_than(this.successor.getId(), closest.getId())) {
				print("Node: " + snode.get_mapped_id(this.id) + " is the successor for " + 
						snode.get_mapped_id(m.source.getId()) + " so reply to him");
				schedule_message(m.source, "on_receive_join_reply", this.successor, 1);
				return;
			}
			print("Node: " + snode.get_mapped_id(this.id) + " is not the successor and foreward the request to: " + 
					snode.get_mapped_id(closest.getId()));
			//forward message to closest preceding node by schedule a message Find_successor_message
			schedule_message(closest, "on_find_successor_receive", m, 1);
		}
	}
	
	
	/**
	 * After i have done the join, my correct successor notify me that he is my successor
	 * @param successor the node i have to set as successor
	 */
	public void on_receive_join_reply(Node successor) {
		if(!(this.state == Node_state.FAILED)) {
			print("Node: " + snode.get_mapped_id(this.id) + " has received a join REPLY from: " + 
					snode.get_mapped_id(successor.getId()) + " set his successor to: " +
					snode.get_mapped_id(successor.getId()));
			//set my successor
			this.successor = successor;
			//set my state to active
			this.state = Node_state.ACTIVE;
		}
	}
	
	
	/**
	 * Every stabilize send a message to the successor asking for his predecessor.
	 * if the predecessor is between this node and his successor than i set my successor
	 * to the predecessor and that notify the node that i become his predecessor.
	 */
	public void stabilize() {
		if(!(this.state == Node_state.FAILED)) {
			if(this.id.compareTo(this.successor.getId()) != 0) {
				print("Node: " + snode.get_mapped_id(this.id) + " has enter the stabilize " + " send a message to: " +
						snode.get_mapped_id(successor.getId()));
				
				//find my successor predecessor
				Find_predecessor_message m = new Find_predecessor_message(this);
				
				//schedule the arrival of the message in my successor node
				schedule_message(this.successor, "on_find_predecessor_receive", m, 1);
			}
			
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
			Find_predecessor_reply rply;
			if(this.predecessor != null) {
					print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor message from " +
				snode.get_mapped_id(m.source.getId()) + " with predecessor: " + snode.get_mapped_id(this.predecessor.getId()));
					//schedule the reception of the reply
					rply = new Find_predecessor_reply(this.predecessor, false);
			}
			else {
				print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor message from" +
						snode.get_mapped_id(m.source.getId()) + " with predecessor: NULL" );
				rply = new Find_predecessor_reply(null, true);
			}
			
			
			schedule_message(m.source, "on_find_predecessor_reply", rply, 1);
			
			
				//maybe i can set my predecessor to the node asking for that ?
				//this.predecessor = m.source;
			
		}
	}
		
	/**
	 * A reply to a find predecessor request. 
	 * if my successor has changed i set my successor predecessor
	 * to my successor and than notify him.
	 * @param x
	 */
	public void on_find_predecessor_reply(Find_predecessor_reply x) {
		if(!(this.state == Node_state.FAILED)) {
			//if the predecessor of my successor is between me and my successor 
			//i set my successor to him and than notify him.
			if(!x.is_null) {
				
				if(check_interval(this.id, this.successor.getId(), x.n.getId(), false, false)) {
					print("Node: " + snode.get_mapped_id(this.id) + " check interval and has set his successor to: " + snode.get_mapped_id(x.n.getId()));
					this.successor = x.n;
				}
				
				print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor REPLY with  predecessor : " +
						snode.get_mapped_id(x.n.getId()) + " my actual successor is: " + snode.get_mapped_id(this.successor.getId()));
			}
			else {
				print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor REPLY with  predecessor : " +
						" NULL " + " my actual successor is: " + snode.get_mapped_id(this.successor.getId()));
			}
			//notify my new successor that i'm his predecessor
			if(this.successor.getId().compareTo(this.id) != 0) {
				schedule_message(this.successor, "notification", this, 1);
			}
		}
	}

	/**
	* The receiving of a notification means that the node before me has
	* discovered be so i set my predecessor to him.
	* @param n The node that discover that i'm his successor
	*/
	public void notification(Node n) {
		if(!(this.state == Node_state.FAILED)) {
			if(this.predecessor == null || check_interval(this.predecessor.getId(), this.id, n.getId(), false ,false)) {
				//set my predecessor
				this.predecessor = n;
				
				print("Node: " + snode.get_mapped_id(this.id) + " receive a notification with a new  predecessor : " +
						snode.get_mapped_id(n.getId()) + " my actual predecessor is: " + snode.get_mapped_id(this.predecessor.getId()));
				
				
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
			print("Node: " + snode.get_mapped_id(this.id) + " start a fixfinger with next :  " +
					this.next );
			
			if(next > this.bigIntegerBits){
				this.next = 1; // Il primo elemento della fingertable è il nodo stesso e quello non deve essere modificato (credo)
			}
			
			//Find the closest node to this id plus two ^ next-1, I applied the module to respect the circle
			Node n = find_successor(this.id.add(Util.two_exponential(next-1)).mod(Node.MAX_VALUE));
			print("Node: " + snode.get_mapped_id(this.id) + "after find successor, found: " +
					snode.get_mapped_id(n.getId()) );
			
			this.fingertable.setNewNode(this.next, n);
		}
	}
	
	/**
	 * Find the successor of the passed id
	 * @param i id
	 * @return the nearest known node to the id
	 */
	public Node find_successor(BigInteger i){
		if (check_interval(this.getId(), this.successor.getId(), i, false, true)) {
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
			if(check_interval(this.id, id, this.fingertable.getIndex(i), false, false)) {
				return this.fingertable.getNode(i);
			}
		}
		return this;
	}
	

	public void check_predecessor() {
		if(!(this.state == Node_state.FAILED)) {
			if(this.predecessor != null) {
				//set the predecessor check variable to inactive
				this.predecessor_has_reply = false;
				//than schedule a message to that node if it responds the state will change 
				//if not that means it is failed
				Check_predecessor_message m = new Check_predecessor_message(this, this.predecessor, Node_state.INACTIVE);
				schedule_message(this.predecessor,"on_check_predecessor_receive", m, 1);
				
				//also schedule to myself a timeout in order to set the node to failed if i don't receive a reply
				schedule_message(this, "timeout_predecessor_failed", m, 4);
			}
		}
	}
	
	public void on_check_predecessor_receive(Check_predecessor_message m) {
		if(!(this.state == Node_state.FAILED)) {
			//construct a message with my current state in the reply
			Check_predecessor_message reply = new Check_predecessor_message(this, m.source, this.state);
			//schedule the reply to the source of the request
			schedule_message(m.source, "on_check_predecessor_reply", reply, 1);
		}
	}
	
	public void on_check_predecessor_reply(Check_predecessor_message m) {
		if(!(this.state == Node_state.FAILED)) {
			//set that the predecessor has replied 
			this.predecessor_has_reply = true;
			//the predecessor is not failed but could be inactive
			if(m.response == Node_state.INACTIVE) {
				//check if he is still my predecessor
				if(m.source.getId().compareTo(this.predecessor.getId()) == 0) {
					//the predecessor leave the network and so i set my predecessor to null
					//will be fixed by stabilize
					this.predecessor = null;
				}
			}
		}
	}
	
	public void timeout_predecessor_failed(Check_predecessor_message m) {
		if(!(this.state == Node_state.FAILED)) {
			//my predecessor has not reply in 4 tick so i can consider him dead
			this.predecessor = null;
			//after that it will be fixed by stabilize
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
		//create an arraylist containing only the new key
		ArrayList<BigInteger> k = new ArrayList<BigInteger>();
		k.add(new_key);
		//new Transfer message with only a key
		Transfer_message m = new Transfer_message(this, target, k);
		schedule_message(target, "on_transfer_message", m, 1);
	}
	
	
	/**
	 * this method given start and end of an interval return if the target is in the interval
	 * @param start the starting point in the chord ring interval
	 * @param finish the end point in the chord ring interval
	 * @param target the target id to be checked
	 * @return true if target is between start and finish
	 */
	private boolean check_interval(BigInteger start, BigInteger finish, BigInteger target, boolean start_included, boolean finish_included) {
		if(start_included && start.compareTo(target) == 0) {
			return true;
		}
		
		if(finish_included && finish.compareTo(target) == 0) {
			return true;
		}
		
		if(start.compareTo(finish) == 0) {
			return false;
		}
		
		//inizio fine target incluso inizio incluso fine
		if(start.compareTo(finish) < 0) {
			//caso base
			return (start.compareTo(target) < 0) && (finish.compareTo(target) > 0);
		}else {
			return !check_interval(finish, start, target, !start_included, !finish_included);
		}
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
	
	private void print(String s) {
		System.out.println(s);
	}
}
