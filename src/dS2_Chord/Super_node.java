package dS2_Chord;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;

public class Super_node {
	@ScheduledMethod (start = 1, interval = 1)
	public void step() {
		/*
		 * for every node that is not already in the network (BUT for a total number < than max_numbeor of node)extract
		 * a value with the probability of join and if success schedule the join
		 * */
		double current_tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Context<Object> context =  ContextUtils.getContext(Node.class);
		IndexedIterable <Object> nodes_to_join = context.getObjects(Node.class);
		for(Object o : nodes_to_join) {
			//we should have a counter to see if the current number of node in the net + the ones
			//that we are going to add exceed the max_numbero of nodes.
		}
		ScheduleParameters params = ScheduleParameters.createOneTime(current_tick + 1); 
		RunEnvironment.getInstance().getCurrentSchedule().scheduleIterable(params, nodes_to_join, "join", true);
		
		/*
		 * for every node in the network extract a value with the probability 
		 * (has to be low in order to prevent to get in a situation of an empty chord ring)
		 * of fail and if success schedule a fail 
		 * */
		
		/*
		 * for every node in the network extract a value with the probability of leave and if 
		 * success schedule a leave
		 * (considering also the node that have just join ? or already schedule a fail?)
		 * */
		
		/*
		 * extract an number of lookup with lookup probability
		 * for every lookup chose a node randomly and ask that node to lookup for some key
		 * that as to be in the network (or not ?)
		 * */
		
		/*
		 * extract a number of key based on probability of a new key creation
		 * and insert them in the correct nodes
		 * */
		
		/*
		 * for each node that is in the network in this moment schedule a stabilize
		 * */
	}
}
