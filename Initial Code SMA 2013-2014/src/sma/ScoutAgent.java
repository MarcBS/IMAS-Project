package sma;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames.InteractionProtocol;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import sma.ScoutCoordinatorAgent.InitialSendToScout;
import sma.ScoutCoordinatorAgent.RequestGameInfo;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.InfoAgent;
import sma.ontology.InfoGame;

public class ScoutAgent extends Agent {

	private AID scoutCoordinatorAgent;
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);
	private AuxInfo auxInfo;
	private Cell objectivePosition; //Cell of the objective position of the agent
		
	public ScoutAgent(){
		 super();
	}
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
	    sd1.setType(UtilsAgents.SCOUT_AGENT);
	    sd1.setName(getLocalName());
	    sd1.setOwnership(UtilsAgents.OWNER);
	    DFAgentDescription dfd = new DFAgentDescription();
	    dfd.addServices(sd1);
	    dfd.setName(getAID());

	    try {
	      DFService.register(this, dfd);
	      showMessage("Scout Registered to the DF");
	    }
	    catch (FIPAException e) {
	      System.err.println(getLocalName() + " registration with DF " + "unsucceeded. Reason: " + e.getMessage());
	      doDelete();
	    }
	    
//	    move();
	    
	    // search ScoutsCoordinatorAgent
	    ServiceDescription searchCriterion = new ServiceDescription();
	    searchCriterion.setType(UtilsAgents.SCOUT_COORDINATOR_AGENT);
	    this.scoutCoordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
	    
	    // Finite State Machine
	    FSMBehaviour fsm = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;
			public int onEnd() {
				System.out.println("FSM behaviour completed.");
				myAgent.doDelete();
				return super.onEnd();
			}
	    };
	    fsm.registerFirstState(new InitialRecieve(this, scoutCoordinatorAgent), "STATE_1");
	    fsm.registerState(new RecieveGameInfo(this,scoutCoordinatorAgent), "STATE_2");
	    fsm.registerDefaultTransition("STATE_1", "STATE_2");
	    fsm.registerDefaultTransition("STATE_2", "STATE_2");
	    addBehaviour(fsm);
	}	 
	
	/**
	 * 
	 * @author David Sanchez Pinsach 
	 * Class that implements behavior for requesting game info (map)
	 */
	protected class InitialRecieve extends SimpleBehaviour
	{
		private AID receptor;
		private Agent a;
		
		public InitialRecieve (Agent a, AID r)
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
		    showMessage("Connection STATE_1 between scout->scout coordinator: OK");
		   
		    
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
								auxInfo = (AuxInfo) reply.getContentObject(); // Getting object with the information about the game
							    showMessage("Receiving game info from "+receptor);
							} catch (UnreadableException e) {
								messagesQueue.add(reply);
								System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: " + e.getMessage());
							} catch (ClassCastException e){
								try {
									objectivePosition = (Cell) reply.getContentObject();
									showMessage("Receiving objective position from "+receptor);
									// Send the cell
						        	ACLMessage reply2 = reply.createReply();
						  	      	reply2.setPerformative(ACLMessage.INFORM);
						  	      	Cell c = null;
						  	      	try {
						  	      		AID agent_aid = this.myAgent.getAID();
						  	      		c = auxInfo.getAgentCell(agent_aid);
						  	      		c = getRandomPosition(auxInfo.getMap(), c);
					  	      			reply2.setContentObject(c); //Return a new cell to scout coordinator
						  	      	} catch (Exception e1) {
						  	      		reply2.setPerformative(ACLMessage.FAILURE);
						  	      		System.err.println(e.toString());
						  	      	}
						  	      	send(reply2);
									showMessage("Sending the cell ["+c.getRow()+","+c.getColumn()+"] position to "+receptor);
									okInfo = true;
		
								} catch (UnreadableException e1) {
									messagesQueue.add(reply);
									System.err.println(getLocalName() + " Recieved objective postionunsucceeded. Reason: " + e.getMessage());
								} //Getting the objective position	
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

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
	    	showMessage("STATE_1 return OK");
	    	return 0;
	    }
	}
	
	/**
	 * 
	 * @author David Sanchez Pinsach 
	 * Class that implements behavior for requesting game info (map)
	 */
	protected class RecieveGameInfo extends SimpleBehaviour
	{
		private AID receptor;
		private Agent a;
		
		public RecieveGameInfo (Agent a, AID r)
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
		    showMessage("Connection STATE_2 between scout->scout coordinator: OK");
		   
		    
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
								auxInfo = (AuxInfo) reply.getContentObject(); // Getting object with the information about the game
							    showMessage("Receiving game info from "+receptor);
							    AID agent_aid = this.myAgent.getAID();
								Cell c = auxInfo.getAgentCell(agent_aid);
								// Send the cell
					        	ACLMessage reply2 = reply.createReply();
					  	      	reply2.setPerformative(ACLMessage.INFORM);
					  	      	try {
					  	      		c = getRandomPosition(auxInfo.getMap(), c);
					  	      		reply2.setContentObject(c); //Return a new cell to scout coordinator
					  	      	} catch (Exception e1) {
					  	      		reply2.setPerformative(ACLMessage.FAILURE);
					  	      		System.err.println(e1.toString());
					  	      	}
					  	      	send(reply2);
								showMessage("Sending the cell ["+c.getRow()+","+c.getColumn()+"] position to "+receptor);
								okInfo = true;
							} catch (UnreadableException e) {
								messagesQueue.add(reply);
								System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: " + e.getMessage());
							} catch(ClassCastException e){
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

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
	    	showMessage("STATE_2 return OK");
	    	return 0;
	    }
	}
	
	
	/**
	 * Method to send a movement (A cell)
	 * @param reply Recieve message
	 * @param c Cell to send
	 * @throws IOException Error of sending message
	 */
	public Cell getRandomPosition(Cell[][] map, Cell actualPosition) throws IOException{
		Cell newPosition = null;
		boolean trobat = false;
		showMessage("Checking random movement...");
		int x=actualPosition.getRow(), y=actualPosition.getColumn(), z = 0, xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = auxInfo.getMapRows();
		maxColumns = auxInfo.getMapColumns();
		newPosition = actualPosition;
		int [][] posibleMovements = {{x+1,y},{x,y+1},{x+1,y-1},{x-1,y+1},{x-1,y},{x,y-1},{x-1,y-1},{x-1,y}};
		List<int[]> intList = Arrays.asList(posibleMovements);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);

		int [] list = null;
		//Search a cell street
		while(arrayList.size()!=0 && !trobat){
			z= auxInfo.getRandomPosition(arrayList.size());
			list = arrayList.remove(z);
			
			xi = list[0];
			yi = list[1];
			if(xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns){ //Check if the position it's in the range of the map
				newPosition = map[xi][yi];
				if(!newPosition.isThereAnAgent() && Cell.STREET == newPosition.getCellType() ){ //Check the limits of the map
					try {
						showMessage("Position before moving "+"["+x+","+y+"]");
						newPosition.addAgent(auxInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
					} catch (Exception e) {
						showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
					}
					trobat = true;
				}
			}else{
				newPosition = actualPosition; //If you can move you return your same position
			}
		}
		
		return newPosition;
	}
}
