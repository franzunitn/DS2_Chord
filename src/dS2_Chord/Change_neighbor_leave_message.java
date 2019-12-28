package dS2_Chord;

import dS2_Chord.Node;

public class Change_neighbor_leave_message {
	Node source;
	Node new_successor;
	Node new_predecessor;
	
	boolean change_predecessor;
	boolean change_successor;
	boolean is_predecessor_null;
	
	public Change_neighbor_leave_message(Node source,
										Node new_successor, 
										Node new_predecessor, 
										boolean change_predecessor, 
										boolean change_successor, 
										boolean is_predecessor_null) {
		
		this.source = source;
		this.new_predecessor = new_predecessor;
		this.new_successor = new_successor;
		this.change_predecessor = change_predecessor;
		this.change_successor = change_successor;
		this.is_predecessor_null = is_predecessor_null;
	}
}
