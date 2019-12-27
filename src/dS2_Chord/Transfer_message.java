package dS2_Chord;

import java.math.BigInteger;
import java.util.ArrayList;

public class Transfer_message {
	public Node source;
	public Node target;
	public ArrayList<BigInteger> keys;
	//add is_insert to know if the message is for the insertion of a new key 
	public Boolean is_insert;
	public Transfer_message(Node source, Node target, ArrayList<BigInteger> keys) {
		this.source = source;
		this.target = target;
		this.keys = keys;
		this.is_insert = false;
	}
	public Transfer_message(Node source, Node target, ArrayList<BigInteger> keys, Boolean is_insert) {
		this.source = source;
		this.target = target;
		this.keys = keys;
		this.is_insert = true;
	}
}
