package dS2_Chord;
import dS2_Chord.Find_successor_message;
import dS2_Chord.Find_predecessor_message;
import dS2_Chord.Change_neighbor_leave_message;
import dS2_Chord.Key;
import dS2_Chord.Raw;
import dS2_Chord.FingerTable;
import dS2_Chord.Util;

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
	private static enum logs_types {
		MINIMAL(0), VERBOSE(1), VERYVERBOSE(2);
		
		private final int value;
		private logs_types(int value) {
			this.value = value;
		}
		private int getValue() {
			return this.value;
		}
	};
	
	public static enum Node_state{
		ACTIVE(0), INACTIVE(1), FAILED(2);
		
		private final int value;
		private Node_state(int value) {
			this.value = value;
		}
		private int getValue() {
			return this.value;
		}
	};
	
	private logs_types log_level;
	private int next;
	private boolean predecessor_has_reply;

	private Node_state state;
	
	private ArrayList<BigInteger> mykeys;

	public Super_node snode;
	
	private boolean new_key_added = false;
	
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
		this.log_level = logs_types.VERYVERBOSE;
	}
	
	public String getSuperNodeNameForMe() {
		return Integer.toString(snode.get_mapped_id(this.id));
	}
	
	public void printActualStateMinimal() {
		print("Node: " + getSuperNodeNameForMe() + " id: " + this.id.toString(), logs_types.MINIMAL);
	}
	
	private void changeActualLogLevel(logs_types logLevel) {
		this.log_level = logLevel;
	}
	
	public void logLevelToVeryVerbose() {
		changeActualLogLevel(logs_types.VERYVERBOSE);
	}
	
	/**
	 * Method used to print the node state
	 */
	public void printActualState() {
		print("PRINTING STATE");
		print("Node: " + getSuperNodeNameForMe() + " id: " + this.id.toString(), logs_types.MINIMAL);
		if (this.successor != null) {
			print("Successor: " + this.successor.getSuperNodeNameForMe() + " id: " + this.successor.getId().toString(), logs_types.MINIMAL);
		} else {
			print("Successor: NULL id: NULL", logs_types.MINIMAL);
		}
		if (this.predecessor != null) {
			print("Predecessor: " + this.predecessor.getSuperNodeNameForMe() + " id: " + this.predecessor.getId().toString(), logs_types.MINIMAL);
		} else {
			print("Predecessor: NULL id: NULL", logs_types.MINIMAL);
		}
		print(this.fingertable.toString(), logs_types.MINIMAL);
		String myKeys_str = "MyKeys: \n";
		for (BigInteger big : this.mykeys) {
			myKeys_str += "[key: " + big.toString() + "]\n";
		}
		print(myKeys_str, logs_types.MINIMAL);
	}
	
	/*
	 * Method used to join the chord ring. 
	 * In order to join the ring MUST know a node already in the ring
	 * @param n a node already in the ring
	 * */ 
	public void join(Node n, boolean is_first) {
		if(this.state != Node_state.FAILED) {
			//check if node n is still in the ring or has fail or leave
			if(is_first) {
				
				//print("Node: " + snode.get_mapped_id(this.id) + " has JOIN for FIRST ", logs_types.MINIMAL);
				print("\t Node: " + snode.get_mapped_id(this.id) + " sets his predecessor to NULL and successor to himself ", logs_types.VERBOSE);
				
				//special case to join
				this.predecessor = null;
				this.successor = this;
				//set the state of the node to active
				this.state = Node_state.ACTIVE;
			}else {
				
				//print("Node: " + snode.get_mapped_id(this.id) + " start JOIN procedure", logs_types.MINIMAL);
				
				this.predecessor = null;
				//send join message
				Find_successor_message m = new Find_successor_message(this);
				print("Node: " + snode.get_mapped_id(this.id) + " has asked to " + 
				snode.get_mapped_id(n.getId()) + " to find his successor",
						logs_types.VERBOSE);
				
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
		if(this.state == Node_state.ACTIVE) {
			print("Node: " + snode.get_mapped_id(this.id) + " has received a request of join from: " + 
					snode.get_mapped_id(m.source.getId()),
					logs_types.VERBOSE);

			BigInteger target_id = m.source.getId();
			
			Node closest = find_successor(target_id);
			
			if (closest != null) {
				
				print("Node: " + snode.get_mapped_id(this.id) + " know the successor of   " + 
						snode.get_mapped_id(m.source.getId()) +
						" so reply to him with " + snode.get_mapped_id(closest.getId()), logs_types.VERYVERBOSE);
				
				schedule_message(m.source, "on_receive_join_reply", this.successor, 1);
				return;
			}
			
			print("Node: " + snode.get_mapped_id(this.id) + " doesn't know the successor so foreward the request to: " + 
					snode.get_mapped_id(this.successor.getId()), logs_types.VERYVERBOSE);
			
			//forward message to closest preceding node by schedule a message Find_successor_message
			schedule_message(closest_preceding_node(target_id), "on_find_successor_receive", m, 1);
		}
	}
	
	
	/**
	 * After i have done the join, my correct successor notify me that he is my successor
	 * @param successor the node i have to set as successor
	 */
	public void on_receive_join_reply(Node successor) {
		
			
			print("Node: " + snode.get_mapped_id(this.id) + " has received a join REPLY from: " + 
					snode.get_mapped_id(successor.getId()) + " set his successor to: " +
					snode.get_mapped_id(successor.getId()),
					logs_types.VERBOSE);
			
			//set my successor
			this.successor = successor;
			//set my state to active
			this.state = Node_state.ACTIVE;
		
	}
	
	
	/**
	 * Every stabilize send a message to the successor asking for his predecessor.
	 * if the predecessor is between this node and his successor than i set my successor
	 * to the predecessor and that notify the node that i become his predecessor.
	 */
	public void stabilize() {
		if(this.state == Node_state.ACTIVE) {
			//print("Node: " + this.getSuperNodeNameForMe() + " is in stabilize procedure", logs_types.MINIMAL);
			
			print("Node: " + snode.get_mapped_id(this.id) + " has enter the stabilize " + " send a message to: " +
					snode.get_mapped_id(successor.getId()) + " wants to know " + snode.get_mapped_id(successor.getId()) +
					" predecessor", logs_types.VERBOSE);
				
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
		if(this.state == Node_state.ACTIVE) {
			//reply with my predecessor
			Find_predecessor_reply rply;
			if(this.predecessor != null) {
				
					print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor message from " +
				snode.get_mapped_id(m.source.getId()) + " that wants to know my predecessor that is: "
							+ snode.get_mapped_id(this.predecessor.getId()) + " and i tell him scheduling a reply ",
				logs_types.VERYVERBOSE);
					
					//schedule the reception of the reply
					rply = new Find_predecessor_reply(this, this.predecessor, false);
			}
			else {
				
				print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor message from " +
						snode.get_mapped_id(m.source.getId()) 
				+ " that wants to know my predecessor that is: NULL and i tell him scheduling a reply ",
						logs_types.VERYVERBOSE);
				rply = new Find_predecessor_reply(this, null, true);
			}
			
			
			schedule_message(m.source, "on_find_predecessor_reply", rply, 1);
		}
	}
		
	/**
	 * A reply to a find predecessor request. 
	 * if my successor has changed i set my successor predecessor
	 * to my successor and than notify him.
	 * @param x the reply message
	 */
	public void on_find_predecessor_reply(Find_predecessor_reply x) {
		if(this.state == Node_state.ACTIVE) {
			//if the predecessor of my successor is between me and my successor 
			//i set my successor to him and than notify him.
			if(!x.is_null) {																	
				if(check_interval(this.id, this.successor.getId(), x.n.getId(), false, false)) {
					
					print("Node: " + snode.get_mapped_id(this.id) + " has received a find predecessor REPLY from: " +
							snode.get_mapped_id(x.source.getId()) + 
							" my successor is telling me that his predecessor is between me and him \n\t so set my successor to : " + 
							snode.get_mapped_id(x.n.getId()),
							logs_types.VERYVERBOSE);
					
					this.successor = x.n;
				}else {
					//if my successor i'm me than set my successor to the predecessor
					if(this.id.compareTo(this.successor.getId()) == 0) {
						print("Node: " + snode.get_mapped_id(this.id) + " has received a find predecessor REPLY from: " +
								snode.get_mapped_id(x.source.getId()) + 
								" i am my successor so i SET my successor to  " + 
								snode.get_mapped_id(x.n.getId()),
								logs_types.VERYVERBOSE);
						
						this.successor = x.n;
					}
				}
			}else {
				
				print("Node: " + snode.get_mapped_id(this.id) + " receive a find prdecessor REPLY from: " +
				snode.get_mapped_id(x.source.getId()) + " telling me that his predecessor is NULL ",
						logs_types.VERYVERBOSE);
			}
			//if i'm not my successor
			if(this.successor.getId().compareTo(this.id) != 0) {
				
				print("Node: " + snode.get_mapped_id(this.id) + " send a NOTIFICATION to " +
						snode.get_mapped_id(x.source.getId()) + " that sould be equal to my successor that is: "+
						snode.get_mapped_id(this.successor.getId()) 
						+ " telling that his predecessor is me ",
								logs_types.VERYVERBOSE);
				
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
		if(this.state == Node_state.ACTIVE) {
			if(this.predecessor == null || check_interval(this.predecessor.getId(), this.id, n.getId(), false ,false)) {
				//set my predecessor
				this.predecessor = n;
				
				print("Node: " + snode.get_mapped_id(this.id) + " receive a notification from: " +
						snode.get_mapped_id(n.getId()) + " telling me that he is my new predecessor so i set my predevessor to: " +
						snode.get_mapped_id(this.predecessor.getId()), logs_types.VERBOSE);
				
				
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
		if(this.state == Node_state.ACTIVE && 
				this.getId().compareTo(this.successor.getId()) != 0) {
			 
			this.next = this.next + 1;
			
			if(next == Node.bigIntegerBits){
				this.next = 1; 
			}
			
			print("FIXFINGERS : Node: " + snode.get_mapped_id(this.id) + " start a fixFinger with next :  " +
					this.next, logs_types.VERBOSE);
			
			BigInteger index = this.fingertable.getIndex(next);
			
			Node n = find_successor(index);
			
			//case that the successor of index is my successor
			if(n != null) {
				print("FIXFINGER: Node: " + snode.get_mapped_id(this.id) + " the successor of indx is my successor", logs_types.VERBOSE);
				this.fingertable.setNewNode(this.next, n);
				update_fingers_graphic();
			}else {
				
				//create and schedule a message to the closest preceding node 
				Fix_finger_find_successor_message m = new Fix_finger_find_successor_message(this, index, this.next);
				
				//get the target node
				Node target = closest_preceding_node(index);
				
				print("FIXFINGER: Node: " + snode.get_mapped_id(this.id) + 
						" send a message to the node: " + 
						snode.get_mapped_id(target.getId()), logs_types.VERBOSE);
				
				schedule_message(target, "on_fixfinger_find_successor_message", m, 1);

			}
		}
	}
	
	
	/**
	 * When a Node is not the successor for a given key call this function with the key which he want to find the successor
	 * if this node successors is the responsible for the key than reply to this message, if not that forward the message to
	 * the nearest neighbor that he knows consulting the finger table.
	 * @param m the message received containing the source of the message and the key to be found
	 */
	public void on_fixfinger_find_successor_message(Fix_finger_find_successor_message m) {
		//see if the id contained in the message is in the range (me, my_successor]
		Node n = find_successor(m.index);
		
		//if i found the successor than create and schedule a reply message
		if(n != null) {
			//find the successor send reply message
			Fix_finger_find_successor_reply_message rm = new Fix_finger_find_successor_reply_message(n, m.next);
			
			print("Node: " + snode.get_mapped_id(this.id) + " found the successor for for the finger table index:  " +
					m.next + " for the node: "
					+ snode.get_mapped_id(m.source.getId()) + " so we send to : " +
					snode.get_mapped_id(m.source.getId()) + " that the successor is: " +
					snode.get_mapped_id(n.getId()), logs_types.VERBOSE);
			print("FIXFINGER on message find successor before sending we chek if is true :" + 
					"\n\t by test if index < successor id" + 
					m.index.compareTo(n.getId()) + " (has to be -1)", logs_types.VERBOSE);
			//scheduling
			schedule_message(m.source, "on_fixfinger_find_successor_reply_message", rm, 1);
			
		//if not than forward the message	
		}else {
			//not found successor
			print("Node: " + snode.get_mapped_id(this.id) + " dind't find the successor for fix finger of  " +
					snode.get_mapped_id(m.source.getId()) + " so we ask to : " +
					snode.get_mapped_id(this.successor.getId()) + " to find a successor for the fixfinger ", logs_types.VERBOSE);
			
			//find the closest preceding node and forward him the request
			Node target = closest_preceding_node(m.index);
			
			//schedule the receive of a message

			schedule_message(target, "on_fixfinger_find_successor_message", m, 1);
		}
	}
	
	/**
	 * This method is the handler for the receiving of a replay to find successor message.
	 * the message contains the index of my finger table to update and the value of the nearest neighbor to contact for that index.
	 * @param m 
	 */
	public void on_fixfinger_find_successor_reply_message(Fix_finger_find_successor_reply_message m) {
		//found the successor and than update the finger table
		print("Node: " + snode.get_mapped_id(this.id) + " receive the successor for the fingertable index : " +
				m.next + " whith successor: " +
				snode.get_mapped_id(m.source.getId()), logs_types.VERBOSE);
		
		//update the row 
		this.fingertable.setNewNode(m.next, m.source);
		
		update_fingers_graphic();
	}
	
	
	/**
	 * Find the successor of the key 
	 * @param i the key which we would find the successor
	 * @return the nearest known node to the id
	 */
	public Node find_successor(BigInteger i){
		
		print(" FIND SUCCESSOR: Node: " + snode.get_mapped_id(this.id) +
				" has to find the successor of: " + i, logs_types.VERBOSE);
		
		//check if i'm the only one in the net (my suc = me)
		if(this.id.compareTo(this.successor.getId()) == 0) {
			print("Node: " + snode.get_mapped_id(this.id) +
					" FIND_SUCCESSOR: case succ = to me ", logs_types.VERBOSE);
			return this.successor;
		}
		//check if the id that i have to find is in the interval (me, my_successor) 
		//if it is than my successor is responsible for that id
		if(check_interval(this.getId(), this.successor.getId(), i, false, true)) {
			print("Node: " + snode.get_mapped_id(this.id) + 
					" FIND SUCCESSOR: case the id is between my successor and I so return my successor " +
					"\n\t that is : " + snode.get_mapped_id(this.successor.getId()), logs_types.VERBOSE);
			
			return this.successor;
		}else {
			
			//a message has to be sent to the closest node that precede the id to be found
			//and that node is in charge to reply if satisfy the two previous condition.
			
			print("Node: " + snode.get_mapped_id(this.id) + 
				" FIND SUCCESSOR: case the id is NOT in the interval so i search the nearest", logs_types.VERBOSE);
			
			Node n_prime = closest_preceding_node(i);
			
			//if i found that i'm the node responsible to this id i return myself
			if(n_prime.getId().compareTo(this.id) == 0) {
				print("Node: " + snode.get_mapped_id(this.id) + " Has found that the closest is me. " +
						snode.get_mapped_id(this.successor.getId()), logs_types.VERBOSE);
				
				return this;
			}
			
			//if not return null and the Fixfinger will schedule a message to the closest preceding node
			print("Node: " + snode.get_mapped_id(this.id) + 
					" a message to che closest has to be sent " + snode.get_mapped_id(n_prime.getId()), logs_types.VERBOSE);
			
			return null;
		}
	}
	
	/**
	 * function that find the closest preceding node looking into the finger table
	 * @param id id searched
	 * @return the closest node known
	 */
	private Node closest_preceding_node(BigInteger id) {
		print("CLOSEST PRECEDING NODE: Node: " + snode.get_mapped_id(this.id) +
				" search in his finger table the successor of : " + 
				id, logs_types.VERBOSE);
			
		for(int i = this.fingertable.getDimension()-1; i > 0; i--) {
			if(check_interval(this.id, id, this.fingertable.getIndex(i), false, false)) {
				
				print("\n\t case when the index " + this.fingertable.getIndex(i) + " is in range : (" + snode.get_mapped_id(this.id) + ", " + id + 
						") so return the node: " + snode.get_mapped_id(this.fingertable.getNode(i).getId()), logs_types.VERBOSE);
				
				return this.fingertable.getNode(i);
			}
		}
		return this;
	}
	
	/**
 	* function to check if the predecessor ha failed or not.
 	*/
	public void check_predecessor() {
		if(this.state == Node_state.ACTIVE) {
			print("CHECK_PREDECESSOR: Node: " + this.getSuperNodeNameForMe() +
					" check the predecessor", logs_types.VERBOSE);
			if(this.predecessor != null) {
				print("CHECK_PREDECESSOR: Node: " + this.getSuperNodeNameForMe() +
						" predecessor is: " + this.predecessor.getSuperNodeNameForMe() +
						" schedule a message to him and wait for response, or timeout", logs_types.VERYVERBOSE);
				
				//set the predecessor check variable to inactive
				this.predecessor_has_reply = false;
				//than schedule a message to that node if it responds the state will change 
				//if not that means it is failed
				Check_predecessor_message m = new Check_predecessor_message(this, this.predecessor, this.state.getValue());
				schedule_message(this.predecessor, "on_check_predecessor_receive", m, 1);
				
				//also schedule to myself a timeout in order to set the node to failed if i don't receive a reply
				schedule_message(this, "timeout_predecessor_failed", m,  4);
			}else {
				print("CHECK_PREDECESSOR: Node: " + this.getSuperNodeNameForMe() +
						" has predecessor NULL", logs_types.VERYVERBOSE);
			}
		}
	}
	
	/**
	 * Handler of a message of type check predecessor.
	 * if this node is active has to reply with another message
	 * @param m The message received
	 */
	public void on_check_predecessor_receive(Check_predecessor_message m) {
		if(this.state == Node_state.ACTIVE) {
			print("ON_CHECK_PREDECESSOR_RECEIVE: Node: " + this.getSuperNodeNameForMe() +
					" has received a request from: " +
					m.source.getSuperNodeNameForMe() +
					"\n and schedule a reply", logs_types.VERBOSE);
			
			//construct a message with my current state in the reply
			Check_predecessor_message reply = new Check_predecessor_message(this, m.source, this.state.getValue());
			//schedule the reply to the source of the request
			schedule_message(m.source, "on_check_predecessor_reply", reply, 1);
		}
	}
	
	/**
	 * Handler of a check predecessor reply, deactivate the timeout 
	 * @param m the message received
	 */
	public void on_check_predecessor_reply(Check_predecessor_message m) {
		if(this.state == Node_state.ACTIVE) {
			
			print("ON_CHECK_PREDECESSOR_REPLY: Node: " + this.getSuperNodeNameForMe() +
					" get a reply form: " + m.source.getSuperNodeNameForMe(), logs_types.VERBOSE);
			
			//set that the predecessor has replied 
			this.predecessor_has_reply = true;
		}
	}
	
	/**
	 * Timeout for the predecessor if this method is called that means that the predecessor
	 * has failed so i can safely set my predecessor to NULL
	 * @param m the message send from myself to 
	 */
	public void timeout_predecessor_failed(Check_predecessor_message m) {
		if(this.state == Node_state.ACTIVE) {
			if(!this.predecessor_has_reply) {
				//my predecessor has not reply in 10 tick so i can consider him dead
				this.predecessor = null;
			}
			//after that it will be fixed by stabilize
		}
	}
	
	/**
	 * Method called when a node wants to leave the network
	 */
	public void leave() {
		//check if is already inactive because could occur that two or more leave are schedule
		if(this.state == Node_state.ACTIVE) {
			print("LEAVE: Node: " + this.getSuperNodeNameForMe() + " LEAVE the ring", logs_types.VERBOSE);
			//check if i'm the last one
			if(this.successor.getId().compareTo(this.id) == 0) {
				this.mykeys.clear();
				print("LEAVE: Node: " + this.getSuperNodeNameForMe() + 
						" is the only one in the ring, just clear the key", logs_types.VERYVERBOSE);
				this.state = Node_state.INACTIVE;
				
			}else {
				print("LEAVE: Node: " + this.getSuperNodeNameForMe() + 
						" schedule a transfer of keys to my successor: " +
						this.successor.getSuperNodeNameForMe(), logs_types.VERYVERBOSE);
				
				//transfer the keys with a message of type : transfer_message
				Transfer_message m = new Transfer_message(this, this.successor, this.mykeys);
				//schedule the message
				schedule_message(this.successor, "on_transfer_message", m, 1);
				
				//than schedule a message to my SUCCESSOR telling him to set his predecessor to my predecessor
				Change_neighbor_leave_message cp;
				if(this.predecessor != null) {
					cp = new Change_neighbor_leave_message(this,
																						this.successor, 
																						this.predecessor, 
																						true,
																						false, 
																						false);
				}else {
					//only tell my successor that i'm leaving and set his predecessor to null
					cp = new Change_neighbor_leave_message(this, 
																						this, 
																						this, 
																						true, 
																						false, 
																						true);
				}
				
				schedule_message(this.successor, "on_change_neigbour_leave", cp, 1);
				
				//than schedule a message to my predecessor (if not null) telling him to set his successor to my successor
				if(this.predecessor != null) {
					Change_neighbor_leave_message cs = new Change_neighbor_leave_message(this,
																						this.successor,
																						this.predecessor, 
																						false, 
																						true, 
																						false);
					schedule_message(this.predecessor, "on_change_neigbour_leave", cs, 1);
				}
				
				
				
				//than leave the network
				this.state = Node_state.INACTIVE;
			}
		}
	}
	
	public void on_transfer_message(Transfer_message m){
		if(this.state == Node_state.ACTIVE) {
			print("ON TRANSFER: Node: " + this.getSuperNodeNameForMe() +
					" receive a TRANSFER message from: " + 
					m.source.getSuperNodeNameForMe(), logs_types.VERYVERBOSE);
			//acquire the keys
			this.mykeys.addAll(m.keys);
		}else {
			//case where the node is inactive but can forward the message maybe
		}
	}
	
	public void on_change_neigbour_leave(Change_neighbor_leave_message m) {
		print("ON_CHANGE: Node: " + this.getSuperNodeNameForMe() + " has received a change message form: " +
				m.source.getSuperNodeNameForMe(), logs_types.VERBOSE);
		
		if(m.is_predecessor_null) {
			print("ON_CHANGE: " + "\t telling me that his predecessor is null so set mine to null", logs_types.VERBOSE);
			this.predecessor = null;
		}else {
			if(m.change_predecessor) {
				print("ON_CHANGE: " + "\t telling me that his PREDECESSOR is: " +
						m.new_predecessor.getSuperNodeNameForMe() + 
						" so change mine", logs_types.VERBOSE);
				this.predecessor = m.new_predecessor;
			}else if(m.change_successor) {
				print("ON_CHANGE: " + "\t telling me that his SUCCESSOR is: " +
						m.new_successor.getSuperNodeNameForMe() + 
						" so change mine", logs_types.VERBOSE);
				this.successor = m.new_successor;
			}
		}
	}
	
	/**
	 * Method used to simulate a node failure
	 */
	public void fail() {
		print("FAIL: Node: " + this.getSuperNodeNameForMe() + 
				" has failed", logs_types.MINIMAL);
		//lose all the key stored in the node
		this.mykeys.clear();
		//simply set the state to FAILED and stop participating in the protocol
		this.state = Node_state.FAILED;
	}

	/**
	 * Function for a key lookup, it will print the result on standard output
	 * @param key requested for the lookup
	 */
	public void lookup(BigInteger key) {
		//Check if the node is active
		if(this.state == Node_state.ACTIVE) {
			// Check if the key is in my competence range
			if (this.predecessor != null && check_interval(this.predecessor.getId(), this.getId(), key, false, false)) {
				// Check if I know the key
				if (check_element(key) != null) {
					//Print a message for the object if it's found
					print("LOOKUP, Node: " + this.getSuperNodeNameForMe() + ", Object [" + key + "] FOUND", logs_types.MINIMAL);
					return;
				}
				else {
					//Print the message for the object if it's not found
					print("LOOKUP, Node: " + this.getSuperNodeNameForMe() + ", Object [" + key + "] NOT FOUND", logs_types.MINIMAL);
					return;
				}
			}
			
			//The target is not in my range, check if I can use the successor
			Node target = find_successor(key);
			//Prepare the message
			look_up_message lum = new look_up_message(this, key);
			if(target != null) {
				//If the successor is available use it and send the message to it
				print("LOOKUP, Node: " + this.getSuperNodeNameForMe()
						+ ", I send a look up message to node " + target.getSuperNodeNameForMe()
						+ " to look for the key requested", logs_types.VERBOSE);
				schedule_message(target, "on_look_up_message_receive", lum, 1);
			}
			//My successor is not the right node that will handle the key
			else {
				//Search for the correct node in the closest preceding nodes
				Node closest = closest_preceding_node(key);
				//If I'm choosen like closest preceding node there is a problem
				if(this.getId().equals(closest.getId())) {
					print("LOOKUP, Node: " + this.getSuperNodeNameForMe() + " I'm the closest preceding node, but I don't have the object, so:"
							+ "\n\tObject [" + key + "] NOT FOUND, this is clearly a strange situation :muble:", logs_types.MINIMAL);
					return;
				}
				print("LOOKUP, Node: " + this.getSuperNodeNameForMe()
					+ ", I send a look up message to node " + closest.getSuperNodeNameForMe()
					+ " to handle the key requested", logs_types.VERBOSE);
				//Send the message
				schedule_message(closest, "on_look_up_message_receive", lum, 1);
			}
		}
		else {
			//Message for the user
			print("Node: " + this.getSuperNodeNameForMe() + ", I'm not active, I cannot lookup", log_level.VERBOSE);
		}
	}
	
	/**
	 * Function that handle a lookup message
	 * @param m message containing the key for which we are looking for
	 */
	public void on_look_up_message_receive(look_up_message m) {
		if(this.state == Node_state.ACTIVE) {
			//Check if the key is in my interval
			if (this.predecessor != null && check_interval(this.predecessor.getId(), this.getId(), m.key, false, false)) {
				print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
						+ " the key we are looking for is in my interval", logs_types.VERYVERBOSE);
				if (check_element(m.key) != null) {
					print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
						+ " I have the key we are looking for, I send a message to the source ("
						+ m.source.getSuperNodeNameForMe() + ") to comuncate the positeve handling", logs_types.VERBOSE);
					look_up_reply_message lurm = new look_up_reply_message(this, true);
					schedule_message(m.source, "on_look_up_reply_message_receive", lurm, 1);
					return;
				} else {
					print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
						+ " I don't have the key we are looking for, I send a message to the source ("
						+ m.source.getSuperNodeNameForMe() + ") to comuncate the negative handling", logs_types.VERBOSE);
					look_up_reply_message lurm = new look_up_reply_message(this, false);
					schedule_message(m.source, "on_look_up_reply_message_receive", lurm, 1);
					return;
				}
			}
			print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
				+ " the key we are looking for is not in my interval", logs_types.VERYVERBOSE);
			Node target = find_successor(m.key);
			if(target != null) {
				//If the successor is available use it and send the message to it
				print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
					+ ", I send a look up message to node " + target.getSuperNodeNameForMe()
					+ " to look for the key requested", logs_types.VERBOSE);
				schedule_message(target, "on_look_up_message_receive", m, 1);
			}
			else {
				Node closest = closest_preceding_node(m.key);
				if(this.getId().equals(closest.getId())) {
					print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe() + " I'm the closest preceding node, but I don't have the object, so:"
							+ "\n\tObject [" + m.key + "] NOT FOUND, this is clearly a strange situation :muble:", logs_types.MINIMAL);
					look_up_reply_message lurm = new look_up_reply_message(this, false);
					schedule_message(m.source, "on_look_up_reply_message_receive", lurm, 1);
					return;
				}
				print("ON_LOOKUP_MSG, Node: " + this.getSuperNodeNameForMe()
					+ ", I send a look up message to node " + closest.getSuperNodeNameForMe()
					+ " to handle the key requested", logs_types.VERBOSE);
				schedule_message(closest, "on_look_up_message_receive", m, 1);
			}
		}
	}
	
	/**
	 * Function to handle the reply to a lookup message
	 * @param m reply message, it can contains positive or negative informations
	 */
	public void on_look_up_reply_message_receive(look_up_reply_message m) {
		if(this.state == Node_state.ACTIVE) {
			if (m.is_present) {
				print("ON_LOOKUP_RPLY_MSG, Node: " + this.getSuperNodeNameForMe() + ", receive a look_up_reply by: "
						+ m.source.getSuperNodeNameForMe() + " and it FOUND the recource I was looking for :)", logs_types.MINIMAL);
			} else {
				print("ON_LOOKUP_RPLY_MSG, Node: " + this.getSuperNodeNameForMe() + ", receive a look_up_reply by: "
						+ m.source.getSuperNodeNameForMe() + " and it DID NOT FOUND the recource I was looking for :(", logs_types.MINIMAL);
			}
		}
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
	
	/**
	 * Function to insert a new key-object in the ring
	 * @param new_key key to store
	 */
	public void insert(BigInteger new_key) {
		if(this.state == Node_state.ACTIVE) {
			// Check if the key belongs to this interval
			if (this.predecessor != null && check_interval(this.predecessor.getId(), this.getId(), new_key, false, false)) {
				// Check if the key is already in the node keys list
				if (check_element(new_key) == null) {
					print("INSERT, Node: " + this.getSuperNodeNameForMe() 
							+ ", Key added to the keys controlled by this node", logs_types.MINIMAL);
					this.mykeys.add(new_key);
					//become green for 10 ticks 
					this.new_key_added = true;
					schedule_message(this, "remove_graphic", null, 3);
					return;
				} else {
					print("INSERT, Node: " + this.getSuperNodeNameForMe() 
						+ ", ERROR, key already in the key list", logs_types.MINIMAL);
				}
				return;
			}
			
			//Find the correct node which the key belongs
			Node target = find_successor(new_key);
			//Create the message
			insert_message m = new insert_message(this, new_key);
			if (target != null) {
				//If the successor exist and is the right node send the insert message to him
				print("INSERT, Node: " + this.getSuperNodeNameForMe()
					+ ", I send an insert message to the node " + target.getSuperNodeNameForMe()
					+ " to insert the key", logs_types.VERBOSE);
				schedule_message(target, "on_insert_message", m, 1);
				addEdge("insertNetwork", this, target);
			}
			else {
				Node closest = closest_preceding_node(m.key);
				if(this.getId().equals(closest.getId())) {
					print("INSERT, Node: " + this.getSuperNodeNameForMe() + " I'm the closest preceding node, but the object is not in my range:"
							+ "\n\tTHIS IS CLEARLY AND ERROR", logs_types.MINIMAL);
					return;
				}
				print("INSERT, Node: " + this.getSuperNodeNameForMe()
					+ ", I send an insert message to node " + closest.getSuperNodeNameForMe()
					+ " to handle the key", logs_types.VERBOSE);
				schedule_message(closest, "on_insert_message", m, 1);
				addEdge("insertNetwork", this, closest);
			}
		}
	}
	
	public void on_insert_message(insert_message message) {
		if(this.state == Node_state.ACTIVE) {
			BigInteger new_key = message.key;
			// Check if the key belongs to this interval
			if (this.predecessor != null && check_interval(this.predecessor.getId(), this.getId(), new_key, false, false)) {
				// Check if the key is already in the node keys list
				if (check_element(new_key) == null) {
					print("ON_INSERT_MESSAGE, Node: " + this.getSuperNodeNameForMe() 
							+ ", Key added to the keys controlled by this node", logs_types.MINIMAL);
					this.mykeys.add(new_key);
					this.new_key_added = true;
					schedule_message(this, "remove_graphic", null, 3);
				} else {
					print("ON_INSERT_MESSAGE, Node: " + this.getSuperNodeNameForMe() 
						+ ", ERROR, key already in the key list", logs_types.MINIMAL);
				}
				return;
			}
			
			//Find the correct node which the key belongs
			Node target = find_successor(new_key);
			//Create the message
			insert_message m = new insert_message(this, new_key);
			if (target != null) {
				//If the successor exist and is the right node send the insert message to him
				print("ON_INSERT_MESSAGE, Node: " + this.getSuperNodeNameForMe()
					+ ", I send an insert message to the node " + target.getSuperNodeNameForMe()
					+ " to insert the key", logs_types.VERBOSE);
				schedule_message(target, "on_insert_message", m, 1);
				addEdge("insertNetwork", this, target);
			}
			else {
				Node closest = closest_preceding_node(m.key);
				if(this.getId().equals(closest.getId())) {
					print("ON_INSERT_MESSAGE, Node: " + this.getSuperNodeNameForMe() + " I'm the closest preceding node, but the object is not in my range:"
							+ "\n\tTHIS IS CLEARLY AND ERROR", logs_types.MINIMAL);
					return;
				}
				print("ON_INSERT_MESSAGE, Node: " + this.getSuperNodeNameForMe()
					+ ", I send an insert message to node " + closest.getSuperNodeNameForMe()
					+ " to handle the key", logs_types.VERBOSE);
				schedule_message(closest, "on_insert_message", m, 1);
				addEdge("insertNetwork", this, closest);
			}
		}
	}
	
	//used to become again blue after recive a new key 
		public void remove_graphic () {
			this.new_key_added = false;
		}
	
	/**
	 * This method is used to check if a target id is in the interval between [start, finish]
	 * the boundaries can be also excluded specify the two boolean start_included and finish_included.
	 * so it's possible to check if target is in [start, finish] or (start, finish) or any combination of these case.
	 * 
	 * @param start the starting point in the chord ring interval
	 * @param finish the end point in the chord ring interval
	 * @param target the target id to be checked
	 * @param start_included specify if the start value is included in the interval to check
	 * @param finish_included specify if the finish value is included in the interval to check
	 * @return true if target is between start and finish (included or excluded depends on parameters)
	 */
	private boolean check_interval(BigInteger start, BigInteger finish, BigInteger target, boolean start_included, boolean finish_included) {
		//check if the start is included and if is equal to target
		if(start_included && start.compareTo(target) == 0) {
			return true;
		}
		//check if finish is included and if is equal to target
		if(finish_included && finish.compareTo(target) == 0) {
			return true;
		}
		//check if start is equal to finish
		if(start.compareTo(finish) == 0) {
			return false;
		}
		
		//check if start is less than finish
		if(start.compareTo(finish) < 0) {
			//base case
			return (start.compareTo(target) < 0) && (finish.compareTo(target) > 0);
		}else {
			//if finish is less than start than call the function with the boundaries inverted
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
		if (message != null) {
			double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + delay); 
			RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method, message);
		} else {
			double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + delay); 
			RunEnvironment.getInstance().getCurrentSchedule().schedule(params, target, method);
		}
	}

	public boolean is_join() {
		return this.is_join;
	}

	public BigInteger getId() {
		return id;
	}

	public int get_state() {
		return this.state.getValue();
	}

	public void setId(BigInteger id) {
		this.id = id;
	}
	
	private void print(String s) {
		System.out.println(s);
	}
	
	private void print(String s, logs_types log) {
		if(log.getValue() <= this.log_level.getValue())
			print(s);
	}
	
	public Boolean get_new_key_added () {
		return this.new_key_added;
	}
	
	//update the graphic of the fingers have to be called on each modification of the fingertable 
		private void update_fingers_graphic () {
			ArrayList<Node> myfingers = this.fingertable.getAllSucc();
			Context <Object> context = ContextUtils.getContext(this);
			Network<Object> fingerNetwork = (Network<Object>)context.getProjection("fingersNetwork");
			//remove all previous edges 
			ArrayList<RepastEdge<Object>> edges = new ArrayList<RepastEdge<Object>>();
			Iterable<RepastEdge<Object>>  edgesiter = fingerNetwork.getOutEdges(this);	
			for (RepastEdge<Object> edge : edgesiter) {
				edges.add(edge);
			}
			for (RepastEdge<Object> edge : edges) {
				fingerNetwork.removeEdge(edge);
			}
			
			//add edges from this to all the fingers 
			for (Node node : myfingers) {
				fingerNetwork.addEdge(this, node);
			}
			
		}
		//add an edge and remove all previous 
		private void addEdge (String network_name, Node source, Node target) {
			Context <Object> context = ContextUtils.getContext(this);
			Network<Object> network = (Network<Object>)context.getProjection(network_name);
			network.addEdge(source, target);
			schedule_message(source, "removeEdge", "insertNetwork", 1);
		}
		
		//remove all exit edges from this node
		public void removeEdge (String network_name) {
			Context <Object> context = ContextUtils.getContext(this);
			Network<Object> network = (Network<Object>)context.getProjection(network_name);
			//remove all previous edges 
			ArrayList<RepastEdge<Object>> edges = new ArrayList<RepastEdge<Object>>();
			Iterable<RepastEdge<Object>>  edgesiter = network.getOutEdges(this);	
			for (RepastEdge<Object> edge : edgesiter) {
				edges.add(edge);
			}
			for (RepastEdge<Object> edge : edges) {
				network.removeEdge(edge);
			}
			
			
		}
}
