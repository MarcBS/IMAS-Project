package sma;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sma.CoordinatorAgent.ListenRequestMap;
import sma.HarvesterCoordinatorAgent.InitialSendToHarvester;
import sma.HarvesterCoordinatorAgent.ReceiveMovement;
import sma.HarvesterCoordinatorAgent.RequestGameInfo;
import sma.HarvesterCoordinatorAgent.SendGameInfo;
import sma.HarvesterCoordinatorAgent.SendMovement;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.InfoGame;
import jade.core.*;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

/**
 * 
 * @author Iosu Mendizabal
 *
 *	Harvester agents class.
 */
public class HarvesterAgent extends Agent {

	private AuxInfo mapInfo;

	private AID harvesterCoordinatorAgent;

	private Cell objectivePosition;

	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);

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
	    
	    	// Finite State Machine
	 		FSMBehaviour fsm = new FSMBehaviour(this) {
	 			public int onEnd() {
	 				System.out.println("FSM behaviour completed.");
	 				myAgent.doDelete();
	 				return super.onEnd();
	 			}
	 		};

	 		// Behaviour to request the initial reception.
	 		fsm.registerFirstState(new InitialRequest(this, harvesterCoordinatorAgent), "STATE_1");
	 		// Behaviour to request the game info.
	 		fsm.registerState(new RequestGameInfo(this, harvesterCoordinatorAgent), "STATE_2");

	 		// FSM transitions
	 		fsm.registerDefaultTransition("STATE_1", "STATE_2");
	 		fsm.registerDefaultTransition("STATE_2", "STATE_2");

	 		// Add behavior of the FSM
	 		addBehaviour(fsm);
	}
	
	protected class InitialRequest extends SimpleBehaviour {
		
		private AID receptor;
		
		public InitialRequest (Agent a, AID r){
			super(a);
			this.receptor = r;
		}

		@Override
		public void action() {
//			showMessage("Requesting game info harvester coord");
//
//			// Make the request
//			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
//			request.clearAllReceiver();
//			request.addReceiver(receptor);
//			request.setProtocol(InteractionProtocol.FIPA_REQUEST);
//			try {
//				request.setContent("get map");
//				send(request);
//				showMessage("Requesting game info to " + receptor);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			/*
			 * Reception of game info
			 * 
			 * The protocol is in two steps: 
			 * 		1. Sender sent an AGREE/FAILURE
			 * 		message 
			 * 		2. Sender sent INFORM message containing the AuxInfo
			 * 		object
			 */
			boolean okInfo = false;
			while (!okInfo) {
				ACLMessage reply = messagesQueue.getMessage();
				if (reply != null) {
					switch (reply.getPerformative()) {
					case ACLMessage.AGREE:
						showMessage("Recieved AGREE from " + reply.getSender());
						break;
					case ACLMessage.INFORM:
						try {
							mapInfo = (AuxInfo) reply.getContentObject(); // Getting
																			// object
																			// with
																			// the
																			// information
																			// about
																			// the
																			// game
							showMessage("Recieved game info from "+ reply.getSender());
						} catch (UnreadableException e) {
							messagesQueue.add(reply);
							System.err
									.println(getLocalName()
											+ " Recieved game info unsucceeded. Reason: "
											+ e.getMessage());
						} catch (ClassCastException cce){
							try {
								objectivePosition = (Cell) reply.getContentObject();
								showMessage("Receiving objective position from "+receptor);
								// Send the cell
					        	ACLMessage reply2 = reply.createReply();
					  	      	reply2.setPerformative(ACLMessage.INFORM);
					  	      	try {
					  	      		reply2.setContentObject(objectivePosition); //Return the cell to harvester coordinator
					  	      	} catch (Exception e1) {
					  	      		reply2.setPerformative(ACLMessage.FAILURE);
					  	      		System.err.println(e1.toString());
					  	      	}
					  	      	send(reply2);
								showMessage("Sending the cell position to "+receptor);
								okInfo = true;
	
							} catch (UnreadableException ue) {
							messagesQueue.add(reply);
							System.err
									.println(getLocalName()
											+ " Recieved object position unsucceeded. Reason: "
											+ ue.getMessage());
							}
						}
						break;
					case ACLMessage.FAILURE:
						System.err
								.println(getLocalName()
										+ " Recieved game info unsucceeded. Reason: Performative was FAILURE");
						break;
					default:
						// Unexpected messages received must be added to the
						// queue.
						messagesQueue.add(reply);
						break;
					}
				}
			}
			messagesQueue.endRetrieval();
	
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return true;
		}
		
		public int onEnd(){
			return 0;
	    }
	}
	
	
	protected class RequestGameInfo extends SimpleBehaviour {
		private AID receptor;
		private boolean firstTime = true;

		public RequestGameInfo(Agent a, AID r) {
			super(a);
			this.receptor = r;
		}

		public void action() {
			showMessage("STATE_2");

			// Make the request
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.clearAllReceiver();
			request.addReceiver(receptor);
			request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			try {
				request.setContent("get map");
				send(request);
				showMessage("Requesting game info to " + receptor);
			} catch (Exception e) {
				e.printStackTrace();
			}

			/*
			 * Reception of game info
			 * 
			 * The protocol is in two steps: 
			 * 		1. Sender sent an AGREE/FAILURE
			 *		message 
			 * 		2. Sender sent INFORM message containing the AuxInfo
			 * 		object
			 */
			
			boolean okInfo = false;
			
			while (!okInfo) {
				ACLMessage reply = messagesQueue.getMessage();
				if (reply != null) {
					switch (reply.getPerformative()) {
						case ACLMessage.AGREE:
								showMessage("Recieved AGREE from " + reply.getSender());
						break;
						
						case ACLMessage.INFORM:
								try {
									mapInfo = (AuxInfo) reply.getContentObject(); // Getting
																				// object
																				// with
																				// the
																				// information
																				// about
																				// the
																				// game
									
									showMessage("Receiving game info from "+receptor);
								    AID agent_aid = this.myAgent.getAID();
									Cell c = mapInfo.getAgentCell(agent_aid);
									// Send the cell
						        	ACLMessage reply2 = reply.createReply();
						  	      	reply2.setPerformative(ACLMessage.INFORM);
						  	      	try {
						  	      		c = getRandomPosition(mapInfo.getMap(), c);
						  	      		reply2.setContentObject(c); //Return a new cell to harvester coordinator
						  	      	} catch (Exception e1) {
						  	      		reply2.setPerformative(ACLMessage.FAILURE);
						  	      		System.err.println(e1.toString());
						  	      	}
						  	      	send(reply2);
									showMessage("Sending the cell position to "+receptor);
									
									
									okInfo = true;
									showMessage("Recieved game info from "+ reply.getSender());
								} catch (UnreadableException e) {
									messagesQueue.add(reply);
									System.err.println(getLocalName()
												+ " Recieved game info unsucceeded. Reason: "
												+ e.getMessage());
								}
						break;
						
						case ACLMessage.FAILURE:
								System.err.println(getLocalName()
											+ " Recieved game info unsucceeded. Reason: Performative was FAILURE");
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

		public boolean done() {
			return true;
		}

		public int onEnd() {
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
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		newPosition = actualPosition;
		int [][] posibleMovements = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(posibleMovements);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);

		int [] list = null;
		//Search a cell street
		while(arrayList.size()!=0 && !trobat){
			z= mapInfo.getRandomPosition(arrayList.size());
			list = arrayList.remove(z);
			
			xi = list[0];
			yi = list[1];
			if(xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns){ //Check if the position it's in the range of the map
				newPosition = map[xi][yi];
				if(!newPosition.isThereAnAgent() && Cell.STREET == newPosition.getCellType() ){ //Check the limits of the map
					try {
						showMessage("Position before moving "+"["+x+","+y+"]");
						newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
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
