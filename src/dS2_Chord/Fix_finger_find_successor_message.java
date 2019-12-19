package dS2_Chord;

import java.math.BigInteger;

public class Fix_finger_find_successor_message {
	public Node source;
	public BigInteger index;
	public int next;
	
	public Fix_finger_find_successor_message(Node source, BigInteger index, int next) {
		this.source = source;
		this.index = index;
		this.next = next;
	}
}
