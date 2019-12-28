package dS2_Chord;

public class look_up_reply_message {
	public Node source;
	public boolean is_present;
	
	public look_up_reply_message(Node source, boolean is_present) {
		this.source = source;
		this.is_present = is_present;
	}
}
