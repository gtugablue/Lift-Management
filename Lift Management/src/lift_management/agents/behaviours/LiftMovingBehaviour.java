package lift_management.agents.behaviours;

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
		int targetedFloor = lift.getTasks().get(0).getFloor();

		lift.headTo(targetedFloor);
	}
		
	@Override
	public int onEnd() {
		lift.handleTaskComplete();
		lift.openDoor();
		return LiftBehaviour.Transitions.ARRIVED.ordinal();
	}

	@Override
	public boolean done() {
		double y = lift.getPosition().getY();
		int targetedFloor = lift.getTasks().get(0).getFloor();
		return targetedFloor > y - Lift.DELTA && targetedFloor < y + Lift.DELTA;
	}
}
