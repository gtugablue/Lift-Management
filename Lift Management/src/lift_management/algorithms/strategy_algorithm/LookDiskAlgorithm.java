package lift_management.algorithms.strategy_algorithm;

import java.util.List;

import javafx.util.Pair;

public class LookDiskAlgorithm {
	public enum Task {UP, DOWN, STOP}
	
	/**
	 * Dada a lista de tarafas de um elevador, a sua posi��o actual, o n�mero de pisos do edif�cio e um pedido , analisa qual � o custo de atender o pedido.
	 * 
	 * @param tasks
	 * @param requestedFloor
	 * @param requestedTask
	 * @param maxBuildingFloor
	 * @param currentPosition
	 * @return Retorna a quantidade de pisos percorridos at� parar o requestedFloor e atender o pedido do Human
	 * @throws Exception 
	 */
	public static int evaluate(List<Pair<Integer, Task>> tasks, int requestedFloor, Task requestedTask, int maxBuildingFloor, int currentPosition) throws Exception{
		if(requestedTask.equals(Task.STOP)){
			throw new Exception("requestedTask cannot be STOP. Only UP or DOWN");
		}
		if(tasks.isEmpty()){
			return Math.abs(currentPosition-requestedFloor);
		}
		
		int previousStop = currentPosition;
		Task previousTask = Task.STOP;
		int floorsTraveled=0;
		boolean assigned = false;
		
		int nextStop;
		Task nextTask;
		Task direction;
		
		//Percorrer todo o caminho do elevador e tentar encaixar a paragem no caminho
		for(Pair<Integer, Task> task : tasks){
			nextStop = task.getKey().intValue();
			nextTask = task.getValue();
			direction = getDirection(tasks,tasks.indexOf(task), previousStop);
			
			if( floorInBetween(previousStop, nextStop, requestedFloor) && direction.equals(requestedTask) ){
				
				if(!previousTask.equals(direction)){	//Se a dire��o do elevador � diferente da dire��o de pr�xima tarefa, quer dizer que o elevador vai fazer uma paragem entre esta tarefa e a pr�xima tarefa que involve andar no sentido oposto.													
					int estimatedDestiny = getEstimatedDestiny(previousStop, previousTask, maxBuildingFloor);
					floorsTraveled += Math.abs( estimatedDestiny - previousStop) + Math.abs(estimatedDestiny + requestedFloor); //O custo ser� ent�o igual a fazer uma viagem que satisfa�a o pedido anterior mais o de ir dessa paragem para o requestedFloor
				}
				else{
					floorsTraveled += Math.abs(previousStop - requestedFloor);
				}
				assigned = true;
				break;
			}
			
			floorsTraveled += Math.abs(nextStop-previousStop);
			previousStop = nextStop;
			nextTask = previousTask;
		}
		
		
		//Percorreu toda a lista de tasks e ainda assim n�o foi atribuido a uma posi��o
		if(assigned == false){		
			int estimatedDestiny = getEstimatedDestiny(previousStop, previousTask, maxBuildingFloor);
			if(previousTask.equals(requestedTask) && floorInBetween(previousStop, estimatedDestiny, requestedFloor)){	//Se a dire��o do elevador � igual ao requestedTask e o requestedFloor est� entre a �ltima paragem e o destino quer dizer que o elevador pode parar no caminho para apanhar.	
				floorsTraveled += Math.abs(previousStop - requestedFloor);
			}else{
				estimatedDestiny = getEstimatedDestiny(previousStop, previousTask, maxBuildingFloor);
				floorsTraveled += Math.abs( estimatedDestiny - previousStop) + Math.abs(estimatedDestiny + requestedFloor); //O custo ser� ent�o igual a fazer uma viagem que satisfa�a o pedido anterior mais o de ir dessa paragem para o requestedFloor
			}	
		}
		
		return floorsTraveled; 
	}
	
	protected static int getEstimatedDestiny(int previousStop, Task previousTask, int maxBuildingFloor) {
		if(previousTask.equals(Task.DOWN)){
			return 0;
		}else{
			return (int) Math.ceil((maxBuildingFloor+1 - previousStop)/2);
		}
		
	}

	protected static boolean floorInBetween(int previous, int next, int n){
		return (previous < n && n <= next) || (previous > n && n >= next);
	}
	
	protected static Task getDirection(List<Pair<Integer, Task>> tasks,int i, int previousStop){		
		Task direction;
		while(previousStop == tasks.get(i).getKey().intValue() && i < tasks.size()){
			i++;
		}
		int nextStop = tasks.get(i).getKey().intValue();
		if(previousStop > nextStop) {
			direction = Task.UP;
		}else if(previousStop < nextStop){
			direction = Task.DOWN;
		}else{
			direction = tasks.get(i).getValue();
		}
		
		return direction;
	}
}

