package dS2_Chord;

import java.awt.Color;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import dS2_Chord.Node_state;
import dS2_Chord.Node;
public class NodeStyle extends DefaultStyleOGL2D {

	@Override
	public Color getColor(Object agent) {
		Node ag = (Node) agent;
		Node_state state = ag.get_state();
		
		if(state == Node_state.ACTIVE) {
			return Color.blue;
		}
		if(state == Node_state.INACTIVE) {
			return Color.gray;
		}
		if(state == Node_state.FAILED) {
			return Color.red;
		}
		return Color.blue;
		
	}

	@Override
	public float getScale(Object object) {
		// TODO Auto-generated method stub
		return 5;
	}

	
	
}