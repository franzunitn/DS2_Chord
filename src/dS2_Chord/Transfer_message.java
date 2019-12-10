package dS2_Chord;

import java.math.BigInteger;
import java.util.ArrayList;

public class Transfer_message {
	public Node source;
	public Node target;
	public ArrayList<BigInteger> keys;
	
	public Transfer_message(Node source, Node target, ArrayList<BigInteger> keys) {
		this.source = source;
		this.target = target;
		this.keys = keys;
	}
}
