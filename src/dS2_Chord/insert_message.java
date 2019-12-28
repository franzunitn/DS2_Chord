package dS2_Chord;

import java.math.BigInteger;

public class insert_message {
	public Node source;
	public BigInteger key;
	
	public insert_message(Node source, BigInteger key) {
		this.source = source;
		this.key = key;
	}
}
