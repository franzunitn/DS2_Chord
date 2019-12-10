package dS2_Chord;

import dS2_Chord.Node;

public class Check_predecessor_message {
	Node source;
	Node target;
	Node_state response;
	public Check_predecessor_message(Node source, Node target, Node_state response) {
		this.source = source;
		this.target = target;
		this.response = response;
	}
}
