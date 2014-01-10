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
import sma.ontology.AStar;
import sma.ontology.AuxGarbage;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.InfoAgent;
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
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = true;

	private AuxInfo mapInfo;

	private AID harvesterCoordinatorAgent;

	private Cell objectivePosition;
	
	private AStar astar;

	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);

	public HarvesterAgent(){}
	
	/**
	   * A message is shown in the log area of the GUI
	   * @param str String to show
	   */
	private void showMessage(String str) {
		if(debugging)
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
	 		// Behaviour to perform a FPSB auction
	 		fsm.registerState(new FPSBAuction(this), "STATE_3");

	 		// FSM transitions
	 		fsm.registerDefaultTransition("STATE_1", "STATE_2");
	 		fsm.registerDefaultTransition("STATE_2", "STATE_3");
	 		fsm.registerDefaultTransition("STATE_3", "STATE_2");

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
						  	      		//TODO
						  	      		//If the agents have some objective position to go use AStar if not random movement.
						  	      		if(objectivePosition.getRow() != -1){
						  	      			
						  	      			Cell newC = getBestPositionToObjective(mapInfo.getMap(), c, objectivePosition);
						  	      			if(newC == null || newC == c){
							  	      			c = getRandomPosition(mapInfo.getMap(), c);
						  	      			}else{
						  	      				c = newC;						  	      			
						  	      			}
						  	      		}else{
						  	      			c = getRandomPosition(mapInfo.getMap(), c);
						  	      		}
					  	      			//c = getRandomPosition(mapInfo.getMap(), c);

					  	      			showMessage("New cell to move "+c);

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
								} catch (NullPointerException e){
									// empty informaion sent from coord (auction)
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
			showMessage("STATE_2 return OK");
			return true;
		}

		public int onEnd() {
			showMessage("STATE_2 return OK");
			return 0;
		}
	}
	
	
	protected class FPSBAuction extends SimpleBehaviour{

		public FPSBAuction(Agent a){
			super(a);
		}
		@Override
		public void action() {
			showMessage("STATE_3");
			boolean end = false;
			// Receive info (adapted from STATE 2)
			ACLMessage originalInfo = null;
			AuxGarbage auctionInfo = null;
			boolean okInfo = false;
			
			while (!end && !okInfo) {
				ACLMessage reply = messagesQueue.getMessage();
				if (reply != null) {
					switch (reply.getPerformative()) {
						case ACLMessage.AGREE:
								showMessage("Recieved AGREE from " + reply.getSender());
						break;
						
						case ACLMessage.INFORM:
								try {
									
									showMessage("Receiving garbage info from "+ reply.getSender());
									
									auctionInfo = (AuxGarbage) reply.getContentObject();
									
									if(auctionInfo == null){
										showMessage("There is no garbage!");
										end = true;
										break;
									}
									showMessage("Receiving garbage info from "+ reply.getSender());
									
									showMessage("Information: " + auctionInfo.getInfo().getGarbageString());
									okInfo = true;
									originalInfo = reply;
								} catch (UnreadableException e) {
									messagesQueue.add(reply);
									System.err.println(getLocalName()
												+ " Recieved auction info unsucceeded. Reason: "
												+ e.getMessage());
								} catch (Exception e) {
									e.printStackTrace();
								}
						break;
						
						case ACLMessage.FAILURE:
								System.err.println(getLocalName()
											+ " Recieved auction info unsucceeded. Reason: Performative was FAILURE");
						break;
						
						default:
							// Unexpected messages received must be added to the queue.
							messagesQueue.add(reply);
						break;
					}
				}
			}
			messagesQueue.endRetrieval();
			
			if(!end){
			// Evaluate the auction
			
			AID agent_aid = this.myAgent.getAID();
			Cell c = mapInfo.getAgentCell(agent_aid);
			
			float distance1 = Math.abs(c.getColumn()-auctionInfo.getColumn()) + Math.abs(c.getRow()-auctionInfo.getRow());
			List<Cell> recyclingCenters = mapInfo.getRecyclingCenters();
			int points = 0;
			float distance2 = Float.POSITIVE_INFINITY;
			Cell destination = null;
			for(Cell tmp : recyclingCenters){
				try {
					if(tmp.getGarbagePoints(auctionInfo.getGarbageType()) >= points){
						points = tmp.getGarbagePoints(auctionInfo.getGarbageType());
						float tmp_distance = Math.abs(auctionInfo.getColumn()-tmp.getColumn()) + Math.abs(auctionInfo.getRow()-tmp.getRow());
						if(tmp_distance < distance2){
							distance2 = tmp_distance;
							destination = tmp;
						}
					}
				} catch (Exception e) {
					
				}
				
			}
			float capacity = mapInfo.getInfoAgent(agent_aid).getMaxUnits() - mapInfo.getInfoAgent(agent_aid).getUnits();
			try {
				points = destination.getGarbagePoints(auctionInfo.getGarbageType());
			} catch (Exception e2) {
				points = 0;
			}
			float bid = -1;
			try {
				float tmp_capacity = capacity - auctionInfo.getInfo().getGarbageUnits();
				if(tmp_capacity == 0){ tmp_capacity = 1;} // if the harvester is full, optimizes the garbage collection
				//else{tmp_capacity = 1/tmp_capacity;} 
				bid = 1/distance1 + 1/tmp_capacity + 1/distance2 + points;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Send the bid
			
			ACLMessage reply2 = originalInfo.createReply();
  	      	reply2.setPerformative(ACLMessage.INFORM);
  	      	try {
  	      		reply2.setContentObject(bid); //Return the bid to the Harvester coordinator agent
  	      	} catch (Exception e1) {
  	      		reply2.setPerformative(ACLMessage.FAILURE);
  	      		System.err.println(e1.toString());
  	      	}
  	      	send(reply2);
			showMessage("Sending the bid to "+originalInfo.getSender());
			
			// Receive the winner
			
			okInfo = false;
			
			while (!okInfo) {
				ACLMessage reply = messagesQueue.getMessage();
				if (reply != null) {
					switch (reply.getPerformative()) {
						case ACLMessage.AGREE:
								showMessage("Recieved AGREE from " + reply.getSender());
						break;
						
						case ACLMessage.INFORM:
								try {
									AID winner = (AID) reply.getContentObject();
									
									showMessage("Receiving auction winner from "+ reply.getSender());
									
									showMessage("Winner: " + winner);
									// update the objective position
									if(agent_aid.equals(winner)){
										objectivePosition = auctionInfo.getInfo();
										showMessage("////////////////////Objective position updated!!");
									}
									okInfo = true;
									originalInfo = reply;
								} catch (UnreadableException e) {
									messagesQueue.add(reply);
									System.err.println(getLocalName()
												+ " Recieved game info unsucceeded. Reason: "
												+ e.getMessage());
								} catch (Exception e) {
									e.printStackTrace();
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
		}

		@Override
		public boolean done() {
			showMessage("STATE_3 return OK");
			return true;
		}
		
	}
	
	
	
	private Cell getBestPositionToObjective(Cell[][] cells, Cell actualPosition, Cell objectivePosition) {
		
		Cell newPosition = null;
		
		showMessage("I am in the cell = ["+actualPosition.getRow()+","+actualPosition.getColumn()+"]");
		showMessage("I wanna go to the cell = ["+objectivePosition.getRow()+","+objectivePosition.getColumn()+"]");
		astar = new AStar(mapInfo);
		newPosition = astar.shortestPath(cells, actualPosition, objectivePosition);
		
		
		
		if(newPosition != null){
			//The new position is occupied by someone?
			if(newPosition.isThereAnAgent()){
				switch (newPosition.getAgent().getAgentType()){
					case InfoAgent.SCOUT:
						try {
							newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); 
						} catch (Exception e) {
							showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
						}
						break;
					case InfoAgent.HARVESTER:
						InfoAgent infoagent = mapInfo.getInfoAgent(this.harvesterCoordinatorAgent);
						InfoAgent other_infoagent = newPosition.getAgent();
						int garbageAmount = infoagent.getGarbageUnits()[InfoAgent.GLASS] + infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + infoagent.getGarbageUnits()[InfoAgent.METAL] + infoagent.getGarbageUnits()[InfoAgent.PAPER];
						int other_garbageAmount = other_infoagent.getGarbageUnits()[InfoAgent.GLASS] + other_infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + other_infoagent.getGarbageUnits()[InfoAgent.METAL] + other_infoagent.getGarbageUnits()[InfoAgent.PAPER];
						
						/* The harvester carrying more garbage is the one that is going to move */
						if (garbageAmount >= other_garbageAmount){
							try {
								newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); 
							} catch (Exception e) {
								showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
							}
						}else{
							newPosition = moveToFreePlace(cells, actualPosition, newPosition, mapInfo.getInfoAgent(this.getAID()));
							try {
								newPosition.addAgent(mapInfo.getInfoAgent(this.getAID()));
							} catch (Exception e) {
								e.printStackTrace();
							} 
						}
						
						break;
				}
			}else{
				try {
					newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); 
				} catch (Exception e) {
					showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
				}
			}
		}

		return newPosition;
	}

	private Cell moveToFreePlace(Cell[][] cells, Cell actualPosition, Cell newPosition, InfoAgent infoAgent) {
		
		int x = actualPosition.getRow();
		int y = actualPosition.getColumn();
		
		int maxRows = mapInfo.getMapRows();
		int maxColumns = mapInfo.getMapColumns();
		
		int [][] nearPlaces = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(nearPlaces);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);
		
		int[] list = null;
		// Search a cell street
		while (arrayList.size() != 0) {
			list = arrayList.remove(0);

			int xi = list[0];
			int yi = list[1];

			if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns) {
				Cell position = cells[xi][yi];
				if (position.getCellType() == Cell.STREET) {
					if(!position.isThereAnAgent()){
						return position;
					}
				}
			}
		}
		return null;
	}
	/**
	 * Method to send a movement (A cell)
	 * @param reply Recieve message
	 * @param c Cell to send
	 * @throws IOException Error of sending message
	 */
	public Cell getRandomPosition(Cell[][] map, Cell actualPosition) throws IOException{
		Cell newPosition = null, positionToReturn = actualPosition;
		boolean trobat = false;
		showMessage("Checking random movement...");
		int x=actualPosition.getRow(), y=actualPosition.getColumn(), z = 0, xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		int [][] posibleMovements = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(posibleMovements);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);

		int [] list = null;
		//Search a cell street
		while(arrayList.size()!=0 && !trobat)
		{
			z= mapInfo.getRandomPosition(arrayList.size());
			list = arrayList.remove(z);
			
			xi = list[0];
			yi = list[1];
			if(xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns)	//Check if the position it's in the range of the map
			{ 
				newPosition = map[xi][yi];
				if(Cell.STREET == newPosition.getCellType() )	//Check the limits of the map
				{ 
					 /* If there is a scout agent in front of */ 
					if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.SCOUT)
					{
						try {
							showMessage("Position before moving "+"["+x+","+y+"]");
							newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
							positionToReturn = newPosition;
							//actualPosition.removeAgent(actualPosition.getAgent());
						} catch (Exception e) {
							showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
						}
						trobat = true;
					}
					 /* If there is a harvester agent in front of */
					else if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.HARVESTER)
					{
						// TODO
						InfoAgent infoagent = mapInfo.getInfoAgent(this.harvesterCoordinatorAgent);
						InfoAgent other_infoagent = newPosition.getAgent();
						int garbageAmount = infoagent.getGarbageUnits()[InfoAgent.GLASS] + infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + infoagent.getGarbageUnits()[InfoAgent.METAL] + infoagent.getGarbageUnits()[InfoAgent.PAPER];
						int other_garbageAmount = other_infoagent.getGarbageUnits()[InfoAgent.GLASS] + other_infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + other_infoagent.getGarbageUnits()[InfoAgent.METAL] + other_infoagent.getGarbageUnits()[InfoAgent.PAPER];
						
						/* The harvester carrying more garbage is the one that is going to move */
						if (garbageAmount > other_garbageAmount)
						{
							try {
								showMessage("Position before moving "+"["+x+","+y+"]");
								newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
								positionToReturn = newPosition;
								//actualPosition.removeAgent(actualPosition.getAgent());
							} catch (Exception e) {
								showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
							}
							trobat = true;
						}
						
						else if (garbageAmount < other_garbageAmount)
						{
							// Do nothing
						}
						
						else	/* Random decision by ID in case of draw */
						{
							int h1 = mapInfo.getInfoAgent(this.harvesterCoordinatorAgent).getAID().hashCode();
							int h2 = newPosition.getAgent().getAID().hashCode();
							if (h1 > h2)
							{
								try {
									showMessage("Position before moving "+"["+x+","+y+"]");
									newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
									positionToReturn = newPosition;
									//actualPosition.removeAgent(actualPosition.getAgent());
								} catch (Exception e) {
									showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
								}
								trobat = true;
							}
						}
					}
					 /* If there is not an agent */ 
					else
					{
						try {
							showMessage("Position before moving "+"["+x+","+y+"]");
							newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
							positionToReturn = newPosition;
							//actualPosition.removeAgent(actualPosition.getAgent());
						} catch (Exception e) {
							showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
						}
						trobat = true;		
					}
				}
			}
		}
		
		return positionToReturn;
	}
}
