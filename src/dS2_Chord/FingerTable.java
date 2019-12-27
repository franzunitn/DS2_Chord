package dS2_Chord;

import dS2_Chord.Util;
import it.geosolutions.jaiext.stats.Max;

import java.math.BigInteger;
import java.util.ArrayList;

public class FingerTable {
	private int m;
	private ArrayList<Raw> finger;
	private BigInteger nodeId;
	private Node originatorNode;
	private int dimension;
	
	public FingerTable(int m, Node originator) {
		this.m = m;
		this.nodeId = originator.getId();
		this.finger = new ArrayList<Raw>();
		this.originatorNode = originator;
		this.dimension = 0;
		init_finghers();
	}
	
	/**
	 * Init all the entries of the table with the actual node like successor
	 * I don't know if it is a correct way
	 */
	private void init_finghers() {
		Raw r = new Raw(this.nodeId, this.originatorNode);
		this.finger.add(0, r);
		for(int i = 1; i < m; i++) {
			BigInteger index = this.nodeId.add(Util.two_exponential(i)).mod(BigInteger.ZERO.setBit(this.m).subtract(BigInteger.ONE));
			r = new Raw(index, this.originatorNode);
			this.finger.add(i, r);
		}
		//System.out.println("FingerTable has dimension: " + this.finger.size());
	}
	
	public int getM() {
		return this.m;
	}
	
	public BigInteger getIndex(int idx) {
		return this.finger.get(idx).index;
	}
	
	public Node getNode(int idx) {
		return this.finger.get(idx).successor;
	}
	
	public void setNewNode(int idx, Node n) {
		Raw r = new Raw(this.finger.get(idx).index, n);
		this.finger.set(idx, r);
		this.dimension = Math.min(this.dimension+1, this.m);
	}
	
	public String toString() {
		String s = "Finghertable (" + this.dimension + "): \n";
		s += "[index, successor]\n";
		for (int i = 0; i < this.dimension; i++) {
			Raw raw = this.finger.get(i);
			s += raw.toString() + "\n";
		}
		return s;
	}
	
	public int getSize() {
		return this.finger.size();
	}
	
	public int getDimension() {
		return this.dimension;
	}
}
