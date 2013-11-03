package sma;

import sma.ontology.AuxInfo;
import sma.ontology.InfoGame;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class ScoutCoordinatorAgent extends Agent{
	
	private AuxInfo info;

	private AID coordinatorAgent;

	public ScoutCoordinatorAgent(){}
	
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
	    sd1.setType(UtilsAgents.SCOUT_COORDINATOR_AGENT);
	    sd1.setName(getLocalName());
	    sd1.setOwnership(UtilsAgents.OWNER);
	    DFAgentDescription dfd = new DFAgentDescription();
	    dfd.addServices(sd1);
	    dfd.setName(getAID());
	    try {
	      DFService.register(this, dfd);
	      showMessage("Registered to the DF");
	    }
	    catch (FIPAException e) {
	      System.err.println(getLocalName() + " registration with DF " + "unsucceeded. Reason: " + e.getMessage());
	      doDelete();
	    }
	    
	    // search CoordinatorAgent
	    ServiceDescription searchCriterion = new ServiceDescription();
	    searchCriterion.setType(UtilsAgents.COORDINATOR_AGENT);
	    this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
	    
	    
	    ACLMessage requestInicial = new ACLMessage(ACLMessage.REQUEST);
	    requestInicial.clearAllReceiver();
	    requestInicial.addReceiver(this.coordinatorAgent);
	    requestInicial.setProtocol(InteractionProtocol.FIPA_REQUEST);
	    showMessage("Message OK");
	    try {
	      requestInicial.setContent("Initial request");
	      showMessage("Content OK" + requestInicial.getContent());
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    
	    // Add behavior to request game info
	    this.addBehaviour(new requestGameInfo(this, coordinatorAgent));
	    
	}
	
	/**
	 * 
	 * @author Albert Busqué
	 *
	 * Class that implements behavior for requesting game info (map)
	 * NOT TESTED YET!!!
	 */
	private class requestGameInfo extends SimpleBehaviour 
	{
		private AID receptor;
		
		public requestGameInfo (Agent a, AID r)
		{
			super(a);
			this.receptor = r;
		}

		@Override
		public void action() {
			// Make the request
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.clearAllReceiver();
		    request.addReceiver(receptor);
		    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
		    showMessage("Requesting game info");
		    try {
			      request.setContent("get game info");
			      //showMessage("Content OK" + request.getContent());
			      send(request);
			    } catch (Exception e) {
			      e.printStackTrace();
			    }
		    
		    //Reception of game info
		    showMessage("Receiving game info");
		    ACLMessage reply = myAgent.blockingReceive();
		    if (reply != null) {    	
		    	try {
					AuxInfo myInfo = (AuxInfo) reply.getContentObject();	// Getting object with the information about the game
					showMessage(myInfo.getMap().toString());
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  	
		    }
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
}