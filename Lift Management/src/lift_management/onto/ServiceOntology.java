package lift_management.onto;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import lift_management.agents.Lift;
import lift_management.calls.DestinationDispatchCall;
import lift_management.calls.DirectionalCall;

public class ServiceOntology extends BeanOntology {
	
	private static final long serialVersionUID = 1L;

	public static final String ONTOLOGY_NAME = "alter-ego";
	
	// Singleton instance of this ontology
	private static Ontology theInstance = new ServiceOntology();
	
	// Method to access the singleton ontology object
	public static Ontology getInstance() {
		return theInstance;
	}
	
	// Private constructor
	private ServiceOntology() {
		super(ONTOLOGY_NAME);
		
		try {
			// add all Concept, Predicate and AgentAction
			add(ServiceProposal.class);
			add(ServiceProposalRequest.class);
			add(ServiceExecutionRequest.class);
			add(DirectionalCall.class);
			add(DestinationDispatchCall.class);
			add(Lift.Direction.class);
			
		} catch(BeanOntologyException boe) {
			boe.printStackTrace();
		}
	}
	
}
