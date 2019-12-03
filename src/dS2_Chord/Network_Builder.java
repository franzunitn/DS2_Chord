package dS2_Chord;
import java.util.ArrayList;

import dS2_Chord.StdRandom;
import dS2_Chord.Node;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;


public class Network_Builder implements ContextBuilder<Object> {

		@Override
	public Context build(Context<Object> context) {
		context.setId("DS2_Chord");
		
		//example network  
		NetworkBuilder<Object> netBuilder_messages = new NetworkBuilder<Object>(
				"chord", context, true);
		netBuilder_messages.buildNetwork();
		
		//create a space 
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 40,
				40);

		//create a grid
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, 40, 40));

		Parameters params = RunEnvironment.getInstance().getParameters();
		
		int nodes = (Integer) params.getValue("nodes");								
		
		//add the node to the context 
		for (int i = 0; i < nodes; i++) {
			//adding the nodes in the context
			context.add(new Node());
		}
		

	
		
		//move the node to a location 
		
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		//this is only for batch run
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(40);
		}

		return context;
	}

}
