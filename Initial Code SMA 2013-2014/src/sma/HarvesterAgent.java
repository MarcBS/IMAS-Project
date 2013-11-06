package sma;

import sma.ontology.AuxInfo;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;

public class HarvesterAgent extends Agent {

	private AuxInfo info;

	private AID harvesterCoordinatorAgent;

	public HarvesterAgent(){}
	
	/**
	   * A message is shown in the log area of the GUI
	   * @param str String to show
	   */
	private void showMessage(String str) {
		System.out.println(getLocalName() + ": " + str);
	}
	
	protected void setup(){
		
		/**** Very Important Line (VIL) *********/
	    this.setEnabledO2ACommunication(true, 1);
	    /****************************************/

	    // Register the agent to the DF
	    ServiceDescription sd1 = new ServiceDescription();
	    sd1.setType(UtilsAgents.HARVESTER_AGENT);
	    sd1.setName(getLocalName());
	    sd1.setOwnership(UtilsAgents.OWNER);
	    DFAgentDescription dfd = new DFAgentDescription();
	    dfd.addServices(sd1);
	    dfd.setName(getAID());
	    try {
	      DFService.register(this, dfd);
	      showMessage("Registered harvester to the DF");
	    }
	    catch (FIPAException e) {
	      System.err.println(getLocalName() + " registration with DF " + "unsucceeded. Reason: " + e.getMessage());
	      doDelete();
	    }
	    
	    // search HarvesterCoordinatorAgent
	    ServiceDescription searchCriterion = new ServiceDescription();
	    searchCriterion.setType(UtilsAgents.HARVESTER_COORDINATOR_AGENT);
	    this.harvesterCoordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
	    
	    ACLMessage requestInicial = new ACLMessage(ACLMessage.REQUEST);
	    requestInicial.clearAllReceiver();
	    requestInicial.addReceiver(this.harvesterCoordinatorAgent);
	    requestInicial.setProtocol(InteractionProtocol.FIPA_REQUEST);
	    showMessage("Message OK");
	    try {
	      requestInicial.setContent("Initial request");
	      showMessage("Content OK" + requestInicial.getContent());
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    
	}
	
	
}
