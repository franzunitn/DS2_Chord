package dS2_Chord;

import java.math.BigInteger;

public class look_up_message {
	public Node source;
	public BigInteger key;
	public int pathlengh;
	
	public look_up_message(Node source, BigInteger key) {
		this.source = source;
		this.key = key;
		this.pathlengh = 0;
	}
}
