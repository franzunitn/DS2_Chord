package dS2_Chord;

import dS2_Chord.Node;

public class Check_predecessor_message {
	Node source;
	Node target;
	int response;
	int check_predecessor_counter;
	public Check_predecessor_message(Node source, Node target, int response, int check_predecessor_counter) {
		this.source = source;
		this.target = target;
		this.response = response;
		this.check_predecessor_counter = check_predecessor_counter;
	}
}
