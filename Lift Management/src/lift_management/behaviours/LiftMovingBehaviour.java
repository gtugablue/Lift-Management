package lift_management.behaviours;

import lift_management.agents.Lift;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.TickerBehaviour;

public class LiftMovingBehaviour extends Behaviour {
	private Lift lift;
	
	public LiftMovingBehaviour(Lift lift, long period) {
		super(lift);
		this.lift = lift;
	}
	
	@Override
	public void action() {
		double y = lift.getPosition().getY();
		int targetedFloor = lift.getTasks().get(0).getKey();

		if (targetedFloor > y)
			lift.ascend();
		else
			lift.descend();
	}
		
	@Override
	public int onEnd() {
		lift.handleTaskComplete();
		return LiftBehaviour.Transitions.ARRIVED.ordinal();
	}

	@Override
	public boolean done() {
		double delta = 0.01;
		double y = lift.getPosition().getY();
		int targetedFloor = lift.getTasks().get(0).getKey();
		return targetedFloor > y - delta && targetedFloor < y + delta;
	}
}