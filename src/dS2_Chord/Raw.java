package dS2_Chord;

import java.math.BigInteger;

public class Raw {
	public BigInteger index;
	public Node successor;
	
	public Raw(BigInteger index, Node successor) {
		this.index = index;
		this.successor = successor;
	}
	
	public String toString() {
		return "[" + this.index.toString() + ", " + this.successor.getSuperNodeNameForMe() + "]";
	}
}
