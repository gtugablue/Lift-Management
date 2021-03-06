package lift_management.algorithms;

import java.util.List;

import javafx.util.Pair;
import lift_management.agents.Lift.Direction;
import lift_management.models.Task;

public class ClosestAttendsAlgorithm extends LiftAlgorithm<Direction> {

	@Override
	public float evaluate(List<Task<Direction>> tasks, int requestedFloor, Direction requestedTask, int maxBuildingFloor, int currentPosition, int liftID) throws Exception {
		if (tasks.isEmpty())
			return Math.abs((int)Math.round(currentPosition) - requestedFloor);
		else {
			return Math.abs(tasks.get(tasks.size() - 1).getFloor() - requestedFloor);
		}
	}

	@Override
	public int addNewTask(List<Task<Direction>> tasks, int requestedFloor, Direction requestedDirection,
			int maxBuildingFloor, int currentPosition) throws Exception {
		tasks.add(new Task<Direction>(requestedFloor, requestedDirection));
		return tasks.size()-1;
	}

	@Override
	public int attendRequest(List<Task<Direction>> tasks, int requestedFloor, int maxBuildingFloor,
			int currentPosition) {
		tasks.add(new Task<Direction>(requestedFloor, Direction.STOP));
		return 0;
	}

	@Override
	protected Direction getDirection(List<Task<Direction>> tasks, int i, int previousStop) {
		// TODO Auto-generated method stub
		return null;
	}

}
