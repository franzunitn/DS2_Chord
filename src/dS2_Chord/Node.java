package dS2_Chord;
import dS2_Chord.Find_successor_message;
import dS2_Chord.Raw;

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


public class Node {

	private BigInteger id;
	private Node successor;
	private Node predecessor;
	private ArrayList fingertable;
	private boolean is_join;
	private static final BigInteger MAX_VALUE = BigInteger.ZERO.setBit(160).subtract(BigInteger.ONE);
	
	//Node Constructor
	public Node(BigInteger id) {
		this.id = id;
		this.successor = null;
		this.predecessor = null;
		this.fingertable = null;
		this.is_join = false;
	}
	
	
	/*
	 * Method used to join the chord ring. 
	 * In order to join the ring MUST know a node already in the ring
	 * @param n a node already in the ring
	 * */ 
	private void join(Node n, boolean is_first) {
		//check if node n is still in the ring or has fail or leave
		if(is_first) {
			//special case to join
			this.predecessor = null;
			this.successor = this;
		}else {
			this.predecessor = null;
			//send join message
			Find_successor_message m = new Find_successor_message(this);
			
			//schedule the receive of a message
			schedule_message(n, "on_find_successor_receive", m);
		}
		
	}
	
	//method to correct successors and predecessors for concurrent operation of join 
	private void stabilize() {
		
	}
	
	private void notification() {
		
	}
	
	private void fixFingers() {
		
	}
	
	public void find_successor() {
		
	}
	
	private void find_predecessor() {
		
	}
	
	private void check_predecessor() {
		
	}
	
	public void leave() {
		
	}
	
	public void fail() {
		
	}
	
	public void lookup() {
		
	}
	
	/**
	 * find the correct successor for a message join.
	 * if the successor is this node schedule a message with me as successor
	 * if the target is in my interval return a message with my successor as successor
	 * else forward the message to the highest preceding node of my finger table
	 * @param m the message that arrive
	 */
	private void on_find_successor_receive(Find_successor_message m) {
		
		BigInteger target_id = m.source.getId();
		
		//if i'm the successor of target 
		if(equal_than(this.id, target_id)) {
			//schedule the receive of a join reply with me as parameter
			schedule_message(m.source, "on_receive_join_reply", this);
			return;
			
		}
		//if the target is in my interval 
		if(check_interval(this.id, this.successor.getId(), target_id)){
			//schedule the receive of a join reply with my successor as parameter
			schedule_message(m.source, "on_receive_join_reply", this.successor);
			return;
		}
		
		//case if i have to forward the message to a closer node
		Node closest = closest_preceding_node(m.source);
		
		//forward message to closest preceding node by schedule a message Find_successor_message
		schedule_message(closest, "on_find_successor_receive", m);
	}
	
	/**
	 * Find the closest preceding node of a node by look in this node fingertable
	 * 
	 * @param target the node of which we want to find the successor
	 * @return the closest preceding node
	 */
	private Node closest_preceding_node(Node target) {
		//creation of a reverse finger table to find the highest predecessor of target ID
		ArrayList<Raw> reverse_fingertable = new ArrayList<Raw>(this.fingertable);
		Collections.reverse(reverse_fingertable);
		
		for(Raw e: reverse_fingertable) {
			if(check_interval(e.index, this.id, target.getId())) {
				//if the target is in this interval i found the highest preceeding node
				return e.successor;
			}
		}
		return this;
	}
	
	/**
	 * After i have done the join, my correct successor notify me that he is my successor
	 * @param successor the node i have to set as successor
	 */
	private void on_receive_join_reply(Node successor) {
		this.successor = successor;
	}
	
	/**
	 * this method given start and end of an interval return if the target is in the interval
	 * @param start the starting point in the chord ring interval
	 * @param finish the end point in the chord ring interval
	 * @param target the target id to be checked
	 * @return true if target is between start and finish
	 */
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
	 */
	private static void schedule_message(Node target, String method, Object message) {
		//schedule receive of a fins successor message in the next tick
		double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + 1); 
		RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, message);
	}


	public boolean is_join() {
		return this.is_join;
	}

	public BigInteger getId() {
		return id;
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