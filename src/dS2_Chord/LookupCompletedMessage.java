package dS2_Chord;

import java.math.BigInteger;
import java.util.ArrayList;

public class LookupCompletedMessage {
	public BigInteger key;
	public ArrayList<BigInteger> chain;
	public int pathLengh;
	
	public LookupCompletedMessage(BigInteger key) {
		this.key = key;
		this.chain = new ArrayList<BigInteger>();
		this.pathLengh = 0;
	}
}
