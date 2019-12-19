package dS2_Chord;

public class Find_predecessor_reply {
	public boolean is_null;
	Node n;
	Node source;
	public Find_predecessor_reply(Node source, Node n, boolean is_null) {
		this.source = source;
		this.n = n;
		this.is_null = is_null;
	}
}
