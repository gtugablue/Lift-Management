package lift_management.algorithms;

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import lift_management.TravelTimes;
import lift_management.agents.Lift.Direction;
import lift_management.models.Task;

public class LookDiskAlgorithm extends LiftAlgorithm<Direction>{
	
	@Override
	public float evaluate(List<Task<Direction>> tasks, int requestedFloor, Direction requestedDirection, int maxBuildingFloor, int currentPosition, int liftID) throws Exception{
		if(requestedDirection.equals(Direction.STOP)){
			throw new Exception("requestedTask cannot be STOP. Only UP or DOWN");
		}
		if(tasks.isEmpty()){
			return Math.abs(currentPosition-requestedFloor);
		}
		
		int previousStop = currentPosition;
		Direction previousDirection = Direction.STOP;
		int floorsTraveled=0;
		int numStops = 0;
		boolean assigned = false;
		
		int nextStop;
		Direction nextDirection;
		Direction direction;
		
		
		//Percorrer todo o caminho do elevador e tentar encaixar a paragem no caminho
		for(Task<Direction> task : tasks){
			nextStop = task.getFloor();
			nextDirection = task.getDestiny();
			direction = getDirection(tasks,tasks.indexOf(task), previousStop);
			
			if( floorInBetween(previousStop, nextStop, requestedFloor) && (direction.equals(requestedDirection))){
				
				if(!(previousDirection.equals(direction) || previousDirection.equals(Direction.STOP))){	//Se a dire��o do elevador � n�o for igual � dire��o de pr�xima tarefa e n�o for STOP, quer dizer que o elevador vai fazer uma paragem entre esta tarefa e a pr�xima tarefa que involve andar no sentido oposto.													
					int estimatedDestiny = getEstimatedDestiny(previousStop, previousDirection, maxBuildingFloor);
					numStops++;
					floorsTraveled += Math.abs( estimatedDestiny - previousStop) + Math.abs(estimatedDestiny - requestedFloor); //O custo ser� ent�o igual a fazer uma viagem que satisfa�a o pedido anterior mais o de ir dessa paragem para o requestedFloor
				}
				else{
					floorsTraveled += Math.abs(previousStop - requestedFloor);
				}
				assigned = true;
				break;
			}
			
			numStops++;
			floorsTraveled += Math.abs(nextStop-previousStop);
			previousStop = nextStop;
			previousDirection = nextDirection;
		}
				
		//Percorreu toda a lista de tasks e ainda assim n�o foi atribuido a uma posi��o
		if(assigned == false){		
			int estimatedDestiny = getEstimatedDestiny(previousStop, previousDirection, maxBuildingFloor);
			if((previousDirection.equals(requestedDirection) && floorInBetween(previousStop, estimatedDestiny, requestedFloor)) || previousDirection.equals(Direction.STOP)){	//Se a dire��o do elevador � igual ao requestedTask e o requestedFloor est� entre a �ltima paragem e o destino quer dizer que o elevador pode parar no caminho para apanhar.	
				floorsTraveled += Math.abs(previousStop - requestedFloor);
			}else{
				estimatedDestiny = getEstimatedDestiny(previousStop, previousDirection, maxBuildingFloor);
				numStops++;
				floorsTraveled += Math.abs( estimatedDestiny - previousStop) + Math.abs(estimatedDestiny - requestedFloor); //O custo ser� ent�o igual a fazer uma viagem que satisfa�a o pedido anterior mais o de ir dessa paragem para o requestedFloor
			}	
		}
		
		return floorsTraveled*TravelTimes.FLOOR+TravelTimes.getStopsExtraTime(numStops); 
	}
	
	protected static int getEstimatedDestiny(int previousStop, Direction previousTask, int maxBuildingFloor) {
		if(previousTask.equals(Direction.DOWN)){
			return 0;
		}else if(previousTask.equals(Direction.UP)){
			return (int) Math.ceil((maxBuildingFloor+1 - previousStop)/2);
		}else{
			return previousStop;
		}
		
	}
	
	@Override
	protected Direction getDirection(List<Task<Direction>> tasks, int i, int previousStop) {		
		Direction direction;
		
		while(i < tasks.size() - 1 && previousStop == tasks.get(i).getFloor()){
			i++;
		}
		
		int nextStop = tasks.get(i).getFloor();
		direction = getDirection(previousStop, nextStop);
		
		if(direction.equals(Direction.STOP)){
			direction = tasks.get(i).getDestiny();
		}
		
		return direction;
	}
	
	@Override
	public int addNewTask(List<Task<Direction>> tasks, int requestedFloor, Direction requestedDirection, int maxBuildingFloor, int currentPosition) throws Exception{
		if(requestedDirection.equals(Direction.STOP)){
			throw new Exception("requestedDirection cannot be STOP. Only UP or DOWN");
		}
		
		//System.out.println("ADD TASK Init, requestFloor: "+requestedFloor+" requestedDirection: "+requestedDirection+" \n" + tasks.toString());

		
		int previousStop = currentPosition;
		Direction previousDirection = Direction.STOP;
		
		int nextStop;
		Direction nextDirection;
		Direction direction;
		
		//Percorrer todo o caminho do elevador e tentar encaixar a paragem no caminho
		for(Task<Direction> task : tasks){
			nextStop = task.getFloor();
			nextDirection = task.getDestiny();
			direction = getDirection(tasks,tasks.indexOf(task), previousStop);
			
			if( floorInBetween(previousStop, nextStop, requestedFloor) && (direction.equals(requestedDirection))){
				int i = tasks.indexOf(task);
				tasks.add(i, new Task<Direction>(requestedFloor, requestedDirection));
				return i;
			}
			
			previousStop = nextStop;
			previousDirection = nextDirection;
		}
		

		//Percorreu toda a lista de tasks e ainda assim n�o foi atribuido a uma posi��o				
		tasks.add(new Task<Direction>(requestedFloor, requestedDirection));
		//System.out.println("ADD TASK END, requestFloor: "+requestedFloor+" requestedDirection: "+requestedDirection+" \n" + tasks.toString());
		return tasks.size()-1;
	}
	
	@Override
	public int attendRequest(List<Task<Direction>> tasks, int requestedFloor, int maxBuildingFloor, int currentPosition){
		//System.out.println("Attend request Init, requestFloor: "+requestedFloor+" \n" + tasks.toString());
		Task<Direction> newTask = new Task<Direction>(requestedFloor, Direction.STOP); 
		if (tasks.contains(newTask))
			return tasks.indexOf(newTask);
		if(tasks.size()==0){
			tasks.add(newTask);
			return 0;
		}
		
		int previousStop = currentPosition;
		Direction previousDirection = Direction.STOP;
		
		int nextStop;
		Direction nextDirection;
		Direction direction;
		
		//Percorrer todo o caminho do elevador e tentar encaixar o maior n�mero de paragens no caminho entre a posi��o inicial e nova paragem
		for(Task<Direction> task : tasks){
			nextStop = task.getFloor();
			nextDirection = task.getDestiny();
			direction = getDirection(previousStop, requestedFloor);
			
			if(!floorInBetween(previousStop, requestedFloor, nextStop) || !(direction.equals(nextDirection))){
				int i = tasks.indexOf(task);
				tasks.add(i, newTask);
				return i;
			}
			
			previousStop = nextStop;
			previousDirection = nextDirection;
		}
		
		//Percorreu toda a lista de tasks e ainda assim n�o foi atribuido a uma posi��o				
		tasks.add(newTask);
		//System.out.println("Attend request END, requestFloor: "+requestedFloor+" \n" + tasks.toString());

		return tasks.size()-1;
	}
}


