package dS2_Chord;

import java.awt.Color;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import dS2_Chord.Node;
import dS2_Chord.Node.Node_state;
public class NodeStyle extends DefaultStyleOGL2D {

	@Override
	public Color getColor(Object agent) {
		Node ag = (Node) agent;
		int state = ag.get_state();
		//has recived a new key 
		if(ag.get_new_key_added()) {
			return Color.green;
		}
		//has find the key in the lookup 
		if (ag.get_key_finded()) {
			return Color.yellow;
		}
		//active 
		if(state == 0) {
			return Color.blue;
		}
		//inactive (leave)
		if(state == 1) {
			return Color.gray;
		}
		//fail 
		if(state == 2) {
			return Color.red;
		}
		return Color.blue;
		
	}

	@Override
	public float getScale(Object object) {
		// TODO Auto-generated method stub
		return 1;
	}

	
	
}