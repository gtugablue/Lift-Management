package lift_management.agents;

import sajas.domain.DFService;
import sajas.proto.ContractNetResponder;
import sajas.proto.SSContractNetResponder;
import sajas.proto.SSResponderDispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lift_management.God;
import lift_management.Human;
import lift_management.agents.behaviours.LiftBehaviour;
import lift_management.algorithms.LiftAlgorithm;
import lift_management.calls.Call;
import lift_management.models.Task;
import lift_management.onto.ServiceOntology;
import lift_management.onto.ServiceProposal;
import lift_management.onto.ServiceProposalRequest;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;

/**
 * Created by Gustavo on 06/10/2016.
 */
public class Lift extends Agent {
	public static final float VELOCITY = 0.005f;
	public static final float DELTA = 0.001f;
	private Codec codec;
	private Ontology serviceOntology;
	private ContinuousSpace<Object> space;
	private final int maxWeight;
	private int currentWeight;
	private double totalCountLoad, sumLoad;
	private long operatingTime;
	private int numFloors;
	private AID buildingAID;
	private God god;
	private int id;
	private LiftAlgorithm algorithm;
	private List<Task<Object>> tasks = new ArrayList<Task<Object>>();
	private Map<Integer, ACLMessage> accepts = new HashMap<Integer, ACLMessage>();
	private List<Human> humans = new ArrayList<Human>();

	public enum DoorState {
		OPEN,
		CLOSED
	};
	public enum Direction {UP, DOWN, STOP};
	private DoorState doorState = DoorState.CLOSED;

	public Lift(int id, God god, ContinuousSpace<Object> space, int numFloors, int maxWeight, LiftAlgorithm algorithm) {
		this.id = id;
		this.god = god;
		this.space = space;
		this.numFloors = numFloors;
		this.maxWeight = maxWeight;
		totalCountLoad = 0;
		sumLoad = 0;
		operatingTime = 0;
		this.algorithm = algorithm;
	}

	@Override
	protected void setup() {
		register();

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addProtocols(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getName());
		sd.setType("lift");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// Behaviours
		addBehaviour(new CNetResponderDispatcher(this));
		addBehaviour(new LiftBehaviour(this));
	}

	/**
	 * Register language and ontology
	 */
	private void register() {
		codec = new SLCodec();
		serviceOntology = ServiceOntology.getInstance();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(serviceOntology);

	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private class CNetResponderDispatcher extends SSResponderDispatcher {

		private static final long serialVersionUID = 1L;

		public CNetResponderDispatcher(Lift agent) {
			super(agent, 
					MessageTemplate.and(
							ContractNetResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
							MessageTemplate.MatchOntology(ServiceOntology.ONTOLOGY_NAME)));
		}

		@Override
		protected Behaviour createResponder(ACLMessage cfp) {
			return new CNetResp((Lift)myAgent, cfp);
		}

	}


	private class CNetResp extends SSContractNetResponder {

		private static final long serialVersionUID = 1L;

		private boolean expectedSuccessfulExecution;

		public CNetResp(Lift a, ACLMessage cfp) {
			super(a, cfp);
		}

		@Override
		protected ACLMessage handleCfp(ACLMessage cfp) {
			//System.out.println(getLocalName() + ": Got cfp.");
			Lift lift = (Lift)myAgent;
			lift.buildingAID = cfp.getSender();
			ACLMessage reply = cfp.createReply();
			reply.setPerformative(ACLMessage.PROPOSE);
			try {
				Call call = (Call) ((ServiceProposalRequest)getContentManager().extractContent(cfp)).getCall();
				float price = lift.algorithm.evaluate(lift.tasks, call.getOrigin(), call.getDestiny(), numFloors, (int) Math.round(lift.getPosition().getY()), lift.getId());
				System.out.println("EVALUATION call: ("+call.getOrigin()+"->"+call.getDestiny() + ") lift: "+lift.getId()+" price: "+ price );
				getContentManager().fillContent(reply, new ServiceProposal("attend-request", price));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return reply;
		}

		@Override
		protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
			//System.out.println(getLocalName() + ": Got accept proposal.");
			ACLMessage result = accept.createReply();

			Call call;
			try {
				call = (Call) ((ServiceProposalRequest) getContentManager().extractContent(cfp)).getCall();
				System.out.println(getLocalName() + ": Got accept proposal (" + call + ").");
				addRequest(call, accept);
				return null; // We'll send the response manually later
			} catch (CodecException | OntologyException e) {
				e.printStackTrace();
				result.setPerformative(ACLMessage.FAILURE);
			}

			return result;
		}

		private void addRequest(Call call, ACLMessage accept) {
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).getFloor() == call.getOrigin() && tasks.get(i).getDestiny().equals(call.getDestiny()))
					return;
			}
			assignTask(call.getOrigin(), call.getDestiny(), accept);
		}

		@Override
		protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
			//System.out.println(getLocalName() + ": Got reject proposal.");
		}

	}

	public void handleTaskComplete() {
		if (tasks.isEmpty())
			return;

		Task task = tasks.get(0);
		tasks.remove(0);

		boolean success = passengersInOut();
		
		ACLMessage accept = accepts.get(task.getId());
		if (accept != null) {
			ACLMessage inform = accept.createReply();
			accepts.remove(task.getId());
			inform.setPerformative(success ? ACLMessage.INFORM : ACLMessage.FAILURE);
			send(inform);
			//System.out.println(getLocalName() + ": INFORM " + task.getFloor());
		}
	}

	public DoorState getDoorState() {
		return doorState;
	}

	public NdPoint getPosition() {
		return space.getLocation(this);
	}

	public float getMaxWeight() {
		return this.maxWeight;
	}

	/**
	 * Adds a new task by placing it in the correct spot in the task list.
	 * @param floor
	 * @param destiny
	 * @param accept The Accept Proposal message that originated this task.
	 */
	public void assignTask(int floor, Object destiny, ACLMessage accept) {
		try {
			int pos = this.algorithm.addNewTask(this.tasks, floor, destiny, this.numFloors, (int)Math.round(this.getPosition().getY()));
			if (accept != null)
				accepts.put(tasks.get(pos).getId(), accept);
			System.out.println(getLocalName() + ": new task " + tasks.get(pos).getId() + ".");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void assignTask(int originFloor, int destinyFloor, ACLMessage accept) {
		// TODO
	}

	private void ascend() {
		space.moveByDisplacement(this, 0, VELOCITY);
	}

	private void descend() {
		space.moveByDisplacement(this, 0, -VELOCITY);
	}
	
	public void headTo(int floor) {
		double y = getPosition().getY();
		
		if (Math.abs(floor - y) <= DELTA)
			return;

		if (floor > y)
			ascend();
		else
			descend();
	}

	public List<Task<Object>> getTasks() {
		return tasks;
	}

	public int getCurrentFloor() {
		return (int)Math.round(getPosition().getY());
	}

	/**
	 * This method should be called when the lift opens its doors, for people to leave and enter.
	 * @return False if the lift didn't attend all humans because it was full, true otherwise.
	 */
	public boolean passengersInOut() {
		this.humans.removeAll(god.dropoffHumans(getId(), getCurrentFloor()));
		List<Human> newHumans = new ArrayList<Human>();
		
		boolean full = god.attendWaitingHumans(getCurrentFloor(), this.maxWeight - this.currentWeight, getId(), possibleDestinies(getCurrentFloor(), this.numFloors), newHumans);
		this.humans.addAll(newHumans);
		this.currentWeight = calculateHumansWeight(this.humans);
		
		totalCountLoad++;
		sumLoad += this.currentWeight;

		for (Human human : humans) {
			Task task = new Task(getCurrentFloor(), human.getDestinyFloor());
			if (!tasks.contains(task)) {
				int pos = this.algorithm.attendRequest(tasks, human.getDestinyFloor(), this.numFloors, getCurrentFloor());
				for (int i = pos + 1; i < tasks.size(); i++) {
					int taskID = tasks.get(i).getId();
					tasks.remove(i);
					if (accepts.containsKey(taskID)) {
						ACLMessage response = accepts.remove(taskID).createReply();
						response.setPerformative(ACLMessage.FAILURE);
						send(response);
					}
					i--;
				}
			}
		}
		System.out.println(getLocalName() + ": tasks: " + this.tasks);
		if (tasks.isEmpty()) 
			System.out.println(getLocalName() + ": Closing doors. Idling");
		else
			System.out.println(getLocalName() + ": Closing doors. Heading to floor " + tasks.get(0).getFloor());
		return !full;
	}

	public boolean[] possibleDestinies(int currFloor, int numFloors) {
		boolean[] possibleDestinies = new boolean[numFloors];
		System.out.print("Lift: "+this.getId()+" ");
		System.out.print("currFloor: " + currFloor + " numFloors: "+numFloors+" ");
		if (tasks.isEmpty()) {
			for (int i = 0; i < numFloors; i++) {
				possibleDestinies[i] = true;
			}
		} else {
			int dest = tasks.get(0).getFloor();
			System.out.print("dest: "+dest+" ");
			Direction liftDirection = LiftAlgorithm.getDirection(currFloor, dest);
			for (int i = 0; i < numFloors; i++) {
				if (liftDirection.equals(Direction.STOP))
					possibleDestinies[i] = true;
				else
					possibleDestinies[i] = liftDirection.equals(LiftAlgorithm.getDirection(currFloor, i));
			}
		}
		
		for (int i = 0; i < numFloors; i++) {
			System.out.print(possibleDestinies[i]);
		}
		System.out.println();
		return possibleDestinies;
	}

	private static int calculateHumansWeight(Collection<Human> humans) {
		int weight = 0;
		for (Human human : humans) {
			weight += human.getWeight();
		}
		return weight;
	}

	public int getId() {
		return id;
	}

	public int getNumHumansInside() {
		return this.humans.size();
		//return god.getNumHumansInLift(getId());
	}
	
	public void openDoor() {
		this.doorState = DoorState.OPEN;
	}
	
	public void closeDoor() {
		this.doorState = DoorState.CLOSED;
	}
	
	public int getCurrentWeight() {
		return currentWeight;
	}

	public double getAvgLoad() {
		if (totalCountLoad == 0)
			return 0;
		return sumLoad / totalCountLoad;
	}
	
	public long getOperatingTime() {
		return operatingTime;
	}
	
	public void updateOperatingTime() {
		if (!tasks.isEmpty())
			operatingTime++;
	}
}