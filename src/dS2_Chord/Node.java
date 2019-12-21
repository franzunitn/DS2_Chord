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
	private enum logs_types {
		MINIMAL(0), VERBOSE(1), VERYVERBOSE(2);
		
		private final int value;
		private logs_types(int value) {
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
		this.log_level = logs_types.MINIMAL;
	}
	
	public String getSuperNodeNameForMe() {
		return Integer.toString(snode.get_mapped_id(this.id));
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
		if(!(this.state == Node_state.FAILED)) {
			//check if node n is still in the ring or has fail or leave
			if(is_first) {
				
				print("Node: " + snode.get_mapped_id(this.id) + " has JOIN for FIRST ", logs_types.VERBOSE);
				print("\t Node: " + snode.get_mapped_id(this.id) + " sets his predecessor to NULL and successor to himself ", logs_types.VERBOSE);
				
				//special case to join
				this.predecessor = null;
				this.successor = this;
				//set the state of the node to active
				this.state = Node_state.ACTIVE;
			}else {
				
				print("Node: " + snode.get_mapped_id(this.id) + " start JOIN procedure", logs_types.VERBOSE);
				
				this.predecessor = null;
				//send join message
				Find_successor_message m = new Find_successor_message(this);
				print("Node: " + snode.get_mapped_id(this.id) + " has asked to " + 
				snode.get_mapped_id(n.getId()) + " to find his successor",
						logs_types.VERYVERBOSE);
				
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
			schedule_message(this.successor, "on_find_successor_receive", m, 1);
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
					snode.get_mapped_id(successor.getId()),
					logs_types.VERBOSE);
			
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
			//if(this.id.compareTo(this.successor.getId()) != 0) {
				
				print("Node: " + snode.get_mapped_id(this.id) + " has enter the stabilize " + " send a message to: " +
						snode.get_mapped_id(successor.getId()) + " wants to know " + snode.get_mapped_id(successor.getId()) +
						" predecessor", logs_types.VERBOSE);
				
				//find my successor predecessor
				Find_predecessor_message m = new Find_predecessor_message(this);
				
				//schedule the arrival of the message in my successor node
				schedule_message(this.successor, "on_find_predecessor_receive", m, 1);
			//}
			 
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
	 * @param x
	 */
	public void on_find_predecessor_reply(Find_predecessor_reply x) {
		if(!(this.state == Node_state.FAILED)) {
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
			}
			else {
				
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
		if(!(this.state == Node_state.FAILED)) {
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
		if(!(this.state == Node_state.FAILED)) {
			 
			this.next = this.next + 1;
			if(snode.get_mapped_id(this.id) == 0) {
				print("FIXFINGERS : Node: " + snode.get_mapped_id(this.id) + " start a fixFinger with next :  " +
						this.next, logs_types.MINIMAL);
			}
			
			
			if(next == this.bigIntegerBits){
				this.next = 1; 
			}
			
			//Find the closest node to this id plus two ^ next-1, I applied the module to respect the circle
			//BigInteger index = this.id.add(Util.two_exponential(next-1)).mod(Node.MAX_VALUE);
			BigInteger index = this.fingertable.getIndex(next);
			
			Node n = find_successor(index);
			
			if(n!= null) {
				print("Node: " + snode.get_mapped_id(this.id) + " in fixfinger after find successor, found: " +
						snode.get_mapped_id(n.getId()) + " as  his successor, at index : " + this.next ,
					logs_types.VERBOSE);
				
				//check if the node successor of that index is really the one that we found
				print(" \n\t FIXFINGER: Node: " + snode.get_mapped_id(this.id) + " has that the successor of this index is my succesor that is:  " +
						snode.get_mapped_id(this.successor.getId()) +
						" \n\t check that that really is by test: i < successor id " +
						" \n\t " + index.compareTo(this.successor.getId()) + " (if is -1 is ok)", logs_types.VERBOSE);
				
				//foud the successor that is me so update the finger table
				this.fingertable.setNewNode(this.next, n);
				
			}else{
				//i'm not the successor for this index so i froward the message
				Fix_finger_find_successor_message m = new Fix_finger_find_successor_message(this, index, this.next);
				
				print("Node: " + snode.get_mapped_id(this.id) + " has asked(send a message) to " + 
						snode.get_mapped_id(this.successor.getId()) + " to find a successor for the fixfinger ",
								logs_types.VERBOSE);
						
				//schedule the receive of a message
				schedule_message(this.successor, "on_fixfinger_find_successor_message", m, 1);
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
		//ask to find successor to find the node corresponding to m.index
		Node n = find_successor(m.index);
		
		if(n != null) {
			//find the successor send reply message
			Fix_finger_find_successor_reply_message rm = new Fix_finger_find_successor_reply_message(n, m.next);
			
			print("Node: " + snode.get_mapped_id(this.id) + " found the successor for for the finger table index:  " +
					m.next + " for the node: "
					+ snode.get_mapped_id(m.source.getId()) + " so we send to : " +
					snode.get_mapped_id(m.source.getId()) + " that the successor is: " +
					snode.get_mapped_id(n.getId()),
							logs_types.VERBOSE);
			print("FIXFINGER on message find successor before sending we chek if is true :" + 
					"\n\t by test if index < successor id" + 
					m.index.compareTo(n.getId()) + " (has to be -1)", logs_types.VERBOSE);
			//scheduling
			schedule_message(m.source, "on_fixfinger_find_successor_reply_message", rm, 1);
			
		}else {
			//not found successor
			print("Node: " + snode.get_mapped_id(this.id) + " dind't find the successor for fix finger of  " +
					snode.get_mapped_id(m.source.getId()) + " so we ask to : " +
					snode.get_mapped_id(this.successor.getId()) + " to find a successor for the fixfinger ",
							logs_types.VERBOSE);
					
			//schedule the receive of a message
			schedule_message(this.successor, "on_fixfinger_find_successor_message", m, 1);
		}
	}
	
	/**
	 * This method is the handler for the receiving of a replay to find successor message.
	 * the message contains the index of my finger table to update and the value of the nearest neighbor to contact for that index.
	 * @param m 
	 */
	public void on_fixfinger_find_successor_reply_message(Fix_finger_find_successor_reply_message m) {
		//found the succesor and than update the finger table
		print("Node: " + snode.get_mapped_id(this.id) + " receive the successor for the fingertable index : " +
				m.next + " whith successor: " +
				snode.get_mapped_id(m.source.getId()),
						logs_types.VERBOSE);
		//update the row 
		this.fingertable.setNewNode(m.next, m.source);
	}
	
	
	/**
	 * Find the successor of the key 
	 * @param i the key which we would find the successor
	 * @return the nearest known node to the id
	 */
	public Node find_successor(BigInteger i){
		if(snode.get_mapped_id(this.id) == 0) {
			print(" FIND SUCCESSOR: Node: " + snode.get_mapped_id(this.id) + " has to find the successor of: " + i , logs_types.MINIMAL);
		}
		
		/*
		if (check_interval(this.getId(), this.successor.getId(), i, false, true)) {
			return this.successor;
		} else {
			Node n_prime = closest_preceding_node(i);
			//è davvero quello che deve ritornare ? 
			return n_prime;
		}*/
		
		
		//printing information of which test i'm doing
		
		/*if(i.compareTo(this.id.add(BigInteger.ONE)) == 0) {
			print("Node: " + snode.get_mapped_id(this.id) +" FIND SUCCESSOR the next hash has to be in my successor domain: " + 
					"\n\t that is: " + snode.get_mapped_id(this.successor.getId()));
		}else if(i.compareTo(this.id.subtract(BigInteger.ONE)) == 0) {
			print("Node: " + snode.get_mapped_id(this.id) +" FIND SUCCESOR the previous hash has to be in my domain: " +
					"\n\t that is: " + snode.get_mapped_id(this.id));
		}*/
		
		
		if(this.id.compareTo(this.successor.getId()) == 0) {
			print("Node: " + snode.get_mapped_id(this.id) +" FIND_SUCCESSOR: case succ = to me ");
			return this.successor;
		}
		
		if(check_interval(this.getId(), this.successor.getId(), i, false, true)) {
			if(snode.get_mapped_id(this.id) == 0) {
				print("Node: " + snode.get_mapped_id(this.id) +" FIND SUCCESSOR: case the id is between my successor and I so return my successor " +
						"\n\t that is : " + snode.get_mapped_id(this.successor.getId()));
			}
			
			/*
			print("FIND SUCCESSOR : Node: " + snode.get_mapped_id(this.id) + " has check that: " +
				 i + " is in the interval :(" + this.getId() + ", " + this.successor.getId() + ")\n" +
				this.getId().toString(2) + " \n" +
				 this.successor.getId().toString(2) + " \n" + 
				i.toString(2) + " \n" +
					" so return my successor that is: " + snode.get_mapped_id(this.successor.getId())
				, logs_types.MINIMAL);
				*/
			
			return this.successor;
		}else {
			if(snode.get_mapped_id(this.id) == 0) {
				print("Node: " + snode.get_mapped_id(this.id) +" FIND SUCCESSOR: case the id is NOT in the interval so i search the nearest");
			}
			
			//Node n_prime = closest_preceding_node(i);
			
			//check if is the nearest neighbor that i know is the successor for this KEY 
			
			
			//return null;
			
			Node n_prime = closest_preceding_node(i);
			
			//case n_prime = me :
			if(n_prime.getId().compareTo(this.id) == 0) {
				if(snode.get_mapped_id(this.id) == 0) {
					print("Node: " + snode.get_mapped_id(this.id) + " Has found that the closest is me so return my successor: " +
							snode.get_mapped_id(this.successor.getId()));
				}
				
				return this.successor;
			}
			if(snode.get_mapped_id(this.id) == 0) {
				print("Node: " + snode.get_mapped_id(this.id) + " FOREWARD the request to : " + snode.get_mapped_id(n_prime.getId()));
			}
			
			Node target = n_prime.find_successor(i);
			if(snode.get_mapped_id(this.id) == 0) {
				print("\n\t Node: " + snode.get_mapped_id(this.id) + " has found that the successor for that index is: " + snode.get_mapped_id(target.getId()));
			}
			
			return target;
		}
	}
	
	/**
	 * function that find the closest preceding node looking into the finger table
	 * @param id id searched
	 * @return the closest node known
	 */
	private Node closest_preceding_node(BigInteger id) {
		
		if(snode.get_mapped_id(this.id) == 0) {
			print("CLOSEST PRECEDING NODE: Node: " + snode.get_mapped_id(this.id) +
					" search in his finger table the successor of : " + 
					id, logs_types.MINIMAL);
		}
		
		//changed to i>1 
		for(int i = this.fingertable.getSize()-1; i > 1; i--) {
			if(check_interval(this.id, id, this.fingertable.getIndex(i), false, false)) {
				if(snode.get_mapped_id(this.id) == 0) {
					print("\n\t case when the id is in range : (" + snode.get_mapped_id(this.id) + ", " + id + 
							") so return the node: " + snode.get_mapped_id(this.fingertable.getNode(i).getId()), logs_types.MINIMAL);
				}
				
				
				return this.fingertable.getNode(i);
			}
		}
		return this;
	}
	
	/**
 	* function to check if the predecessor ha failed or not.
 	*/
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
	
	private void print(String s) {
		System.out.println(s);
	}
	
	private void print(String s, logs_types log) {
		if(log.getValue() <= this.log_level.getValue())
			print(s);
	}
}
