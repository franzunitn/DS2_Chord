package dS2_Chord;

import dS2_Chord.Util;

import java.math.BigInteger;
import java.util.ArrayList;

public class FingerTable {
	private int m;
	private ArrayList<Raw> fingher;
	private BigInteger nodeId;
	private Node originatorNode;
	
	public FingerTable(int m, Node originator) {
		this.m = m;
		this.nodeId = originator.getId();
		this.fingher = new ArrayList<Raw>(this.m+1);
		this.originatorNode = originator;
		init_finghers();
	}
	
	/**
	 * Init all the entries of the table with the actual node like succesor
	 * I don't know if it is a correct way
	 */
	private void init_finghers() {
		Raw r = new Raw(this.nodeId, this.originatorNode);
		this.fingher.add(0, r);
		for(int i = 1; i <= m; i++) {
			//TODO insert mod
			BigInteger index = this.nodeId.add(Util.two_exponential(i));
			r = new Raw(index, this.originatorNode);
			this.fingher.add(i, r);
		}
		System.out.println(this.fingher.size());
	}
	
	public int getM() {
		return this.m;
	}
	
	public BigInteger getIndex(int idx) {
		return this.fingher.get(idx).index;
	}
	
	public Node getNode(int idx) {
		return this.fingher.get(idx).successor;
	}
	
	public void setNewNode(int idx, Node n) {
		Raw r = new Raw(this.fingher.get(idx).index, n);
		this.fingher.set(idx, r);
	}
}
