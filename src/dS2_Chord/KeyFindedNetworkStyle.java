package dS2_Chord;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.DefaultEdgeStyleOGL2D;

public class KeyFindedNetworkStyle extends DefaultEdgeStyleOGL2D {

	@Override
	public Color getColor(RepastEdge<?> edge) {
		// TODO Auto-generated method stub
		return Color.CYAN;
	}

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		// TODO Auto-generated method stub
		return 2;
	}
	
	

}
