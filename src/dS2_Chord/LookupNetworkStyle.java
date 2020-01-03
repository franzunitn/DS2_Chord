package dS2_Chord;
import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.DefaultEdgeStyleOGL2D;

public class LookupNetworkStyle extends DefaultEdgeStyleOGL2D {

	@Override
	public Color getColor(RepastEdge<?> edge) {
		// TODO Auto-generated method stub
		return Color.red;
	}

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		// TODO Auto-generated method stub
		return 2;
	}
	
}
	
