package sma;

import java.util.Random;

import sma.ontology.AuxInfo;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
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
	
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);

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
	      showMessage("Content OK " + requestInicial.getContent());
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    
	    //TODO Implement FSM
	    
	    // Add behavior to request game info
	    this.addBehaviour(new RequestGameInfo(this, coordinatorAgent));
	    
	    // Add behavior to send game info and first random movement to scout agents 
	    this.addBehaviour(new InitialSendToScout(this));
	}
	
	/**
	 * 
	 * @author Albert Busqu�
	 *
	 * Class that implements behavior for requesting game info (map)
	 * NOT TESTED YET!!!
	 */
	protected class RequestGameInfo extends OneShotBehaviour 
	{
		private AID receptor;
		
		public RequestGameInfo (Agent a, AID r)
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
		    try {
			      request.setContent("get map");
			      send(request);
			      showMessage("Requesting game info to "+receptor);
			    } catch (Exception e) {
			      e.printStackTrace();
			    }
		    
		  /*Reception of game info
		   * 
		   * The protocol is in two steps: 
		   * 	1. Sender sent an AGREE/FAILURE message
		   * 	2. Sender sent INFORM  message containing the AuxInfo object
		   */
		    boolean okInfo = false;
		    while(!okInfo)
		    {
		    	ACLMessage reply = messagesQueue.getMessage();
		    	if (reply != null)
		    	{
		    		switch (reply.getPerformative())
		    		{
			    		case ACLMessage.AGREE:
			    			showMessage("Recieved AGREE from "+reply.getSender());
			    			break;
			    		case ACLMessage.INFORM:
							try {
								info = (AuxInfo) reply.getContentObject();	// Getting object with the information about the game
								okInfo = true;
								showMessage("Recieved game info from "+reply.getSender());
							} catch (UnreadableException e) {
								messagesQueue.add(reply);
								System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: " + e.getMessage());
							}
			    			break;
			    		case ACLMessage.FAILURE:
			    			System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: Performative was FAILURE");
			    			break;
			    		default:
			    			// Unexpected messages received must be added to the queue.
			    			messagesQueue.add(reply);
			    			break;
		    		}
		    	}
		    }
		    messagesQueue.endRetrieval();
		}
	}
	
	/**
	 * 
	 * @author Albert
	 * 
	 * Class that implements behavior for sending game info (map) to all scout agents and a random movement
	 * NOT TESTED YET!!!
	 */
	protected class InitialSendToScout extends OneShotBehaviour 
	{

		public InitialSendToScout (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			Random rnd = new Random();
			
			/* Make a broadcast to all scouts agent sending the game info and movement*/
			for (int i=0; i<info.getScout_aids().size(); i++)
			{	
				/* Sending game info */
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
			    request.addReceiver(info.getScout_aids().get(i));
			    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			    try 
			    {
			    	request.setContentObject(info);
			    } catch (Exception e) {
				    	request.setPerformative(ACLMessage.FAILURE);
				    	e.printStackTrace();
				    	}
			    send(request);
			    showMessage("Sending game info to "+info.getScout_aids().get(i));
			    
			    /* Sending random movement*/
			    int mapSize = info.getMap().length;
			    Movement m = new Movement(null, rnd.nextInt(mapSize), rnd.nextInt(mapSize), UtilsAgents.SCOUT_AGENT);	// Random movement
			    try 
			    {
			    	request.setContentObject(m);
			    } catch (Exception e) {
				    	request.setPerformative(ACLMessage.FAILURE);
				    	e.printStackTrace();
				    	}
			    send(request);
			    showMessage("Sending movement to "+info.getScout_aids().get(i));
			}
		}	
	}
}