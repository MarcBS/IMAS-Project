package sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames.InteractionProtocol;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import sma.gui.LogPanel;
import sma.ontology.AStar;
import sma.ontology.AuxGarbage;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.InfoAgent;

/**
 * 
 * @author Iosu Mendizabal
 *
 *	Harvester agents class.
 */
public class HarvesterAgent extends Agent implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Indicates if we want to show the debugging messages
	private boolean debugging = true;

	private boolean cantMove = false;
	
	private AuxInfo mapInfo;
	
	private AID preference_over = null;

	private AID harvesterCoordinatorAgent;

	private Cell objectivePosition = new Cell(-1, -1); // initialize objective position until we received a garbage position
	
	private AStar astar;
	
	private HashMap<Integer, int[]> moviment = new HashMap<Integer, int[]>();
	
	boolean has_collision = false;

	private final int NORT = 1, SUD = 2, EST = 3, WEST = 4;
	
	private int contrari_colisio = 0;
	
	private AID myAID;
	
	private int carriedGarbage = 0;
	
	private boolean randomMovement = true;
	
	private ArrayList<Cell> objectives = new ArrayList<Cell>();
	
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);
	
	private boolean followingOptimalPath = true;
	private Cell lastOptimalPoint = null;

	public HarvesterAgent()
	{
		int[] N = {1,0}, E={0,1}, S = {-1,0}, W= {0,-1};
		moviment.put(NORT, N);
		moviment.put(SUD, S);
		moviment.put(EST, E);
		moviment.put(WEST, W);
		
	}

	
	/**
	   * A message is shown in the log area of the GUI
	   * @param str String to show
	   */
	private void showMessage(String str) {
		LogPanel.showMessage(getLocalName() + ": " + str+"\n");
		if(debugging)
			System.out.println(getLocalName() + ": " + str);
	}
	
	protected void setup(){
		
		 this.myAID = getAID();
		
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
	 		// Behaviour to decide where to move and send that information to the HarvesterCoordinator
	 		fsm.registerState(new MoveAgent(this, harvesterCoordinatorAgent), "STATE_4");

	 		// FSM transitions
	 		/*fsm.registerDefaultTransition("STATE_1", "STATE_2");
            fsm.registerDefaultTransition("STATE_2", "STATE_4");
            fsm.registerDefaultTransition("STATE_4", "STATE_3");
            fsm.registerDefaultTransition("STATE_3", "STATE_2");*/

            fsm.registerDefaultTransition("STATE_1", "STATE_3");
     		fsm.registerDefaultTransition("STATE_3", "STATE_4");
     		fsm.registerDefaultTransition("STATE_4", "STATE_2");
     		fsm.registerDefaultTransition("STATE_2", "STATE_3");
            
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
							astar = new AStar(mapInfo);
							showMessage("Recieved game info from "+ reply.getSender());
							okInfo = true;
						} catch (Exception e) {
							messagesQueue.add(reply);
							System.err
									.println(getLocalName()
											+ " Recieved game info unsucceeded. Reason: "
											+ e.getMessage());
						} 
						// USELESS!!!!!!
						/*catch (ClassCastException cce){
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
							}*/
						
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
	
	
	protected class MoveAgent extends SimpleBehaviour {
		private AID receptor;
		
		public MoveAgent(Agent a, AID r){
			super(a);
			this.receptor = r;
		}
		
		private Cell moveNormally(){
            
            AID agent_aid = this.myAgent.getAID();
            Cell c = mapInfo.getAgentCell(agent_aid);
           
            int avail_capacity = mapInfo.getInfoAgent(agent_aid).getMaxUnits() - mapInfo.getInfoAgent(agent_aid).getUnits();
      		// since the objective is a building, the agent must be at distance 1
      		if((objectivePosition.getCellType() == Cell.BUILDING || objectivePosition.getCellType() == Cell.RECYCLING_CENTER) && 
      				(Math.abs(objectivePosition.getRow() - c.getRow()) + Math.abs(objectivePosition.getColumn() - c.getColumn()) == 1 ||
      				(Math.abs(objectivePosition.getRow() - c.getRow()) == 1) && (Math.abs(objectivePosition.getColumn() - c.getColumn()) == 1))){
      			showMessage("Currently at objective");
      			switch (objectivePosition.getCellType()){
      			case Cell.BUILDING:
      				
      				
      				try {
						if(avail_capacity > 0 && mapInfo.getCell(objectivePosition.getRow(), objectivePosition.getColumn()).getGarbageUnits() > 0 &&
								mapInfo.getAgentCell(agent_aid).getAgent().getMaxUnits() > mapInfo.getAgentCell(agent_aid).getAgent().getUnits()){
							
							ServiceDescription searchCriterion = new ServiceDescription();
						searchCriterion.setType(UtilsAgents.CENTRAL_AGENT);
						AID centralAgent = UtilsAgents.searchAgent(this.myAgent, searchCriterion);
						
						showMessage("Sending Garbage Info to central.");
						ACLMessage info = new ACLMessage(ACLMessage.INFORM);
						info.clearAllReceiver();
						info.addReceiver(centralAgent);
						info.setProtocol(InteractionProtocol.FIPA_REQUEST);
						try {
							info.setContentObject((Cell)objectivePosition);
							send(info);
						} catch (Exception e) {
							e.printStackTrace();
						}

						
						} else if (mapInfo.getAgentCell(this.myAgent.getAID()).getAgent().getUnits() > 0){
							int points = 0;
							Cell recyclingCenter = null;
							for(Cell tmp : mapInfo.getRecyclingCenters()){
								try {
									
									if(tmp.getGarbagePoints(String.valueOf(mapInfo.getAgentCell(this.myAgent.getAID()).getAgent().getCurrentTypeChar())) > points){
										points = tmp.getGarbagePoints(String.valueOf(mapInfo.getAgentCell(this.myAgent.getAID()).getAgent().getCurrentTypeChar()));
										recyclingCenter = tmp;
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						
							if(avail_capacity == 0 && objectivePosition.getGarbageUnits() > 0){
								objectives.add(0, objectivePosition);
							}
							showMessage("ADDING A RECYCLING CENTER");
							objectivePosition = recyclingCenter;
						}
						c = objectivePosition;
					} catch (Exception e1) {
						
					}
      				break;
      				
      			case Cell.RECYCLING_CENTER:
      				if(mapInfo.getInfoAgent(agent_aid).getUnits() > 0){
      					
      					ServiceDescription searchCriterion = new ServiceDescription();
  	      			searchCriterion.setType(UtilsAgents.CENTRAL_AGENT);
  	      			AID centralAgent = UtilsAgents.searchAgent(this.myAgent, searchCriterion);
  	      			
  	      			showMessage("Sending Garbage Info to central.");
  	      			
  	      			ACLMessage info = new ACLMessage(ACLMessage.INFORM);
  	      			info.clearAllReceiver();
  	      			info.addReceiver(centralAgent);
  	      			info.setProtocol(InteractionProtocol.FIPA_REQUEST);

  	      		    try {
  	      		    	info.setContentObject((Cell)objectivePosition);
  	      		    	send(info);
  	      		    } catch (Exception e) {
  	      		    	e.printStackTrace();
  	      		    }

  	      		    
      				} else{
      					if(objectives.size() > 0){
      						findNextObjective(); // get the next movement, using the Manhattan distance and the position in the stack
      						//objectivePosition = objectives.remove(0); // get the first movement of the stack
      					}else{
      						objectivePosition = new Cell(-1, -1);
      					}
      					
      				}
      				c = objectivePosition;
      				break;
      			}
            }else{
                    //If the agents have some objective position to go use AStar if not random movement.
                    if(objectivePosition.getRow() != -1){
                           
                            Cell newC = getBestPositionToObjective(mapInfo.getMap(), c, objectivePosition);
                            if(newC == null || newC == c){
                            		showMessage("RANDOM MOVEMENT");
                                    randomMovement = true;
                                    try {
										c = getRandomPosition(mapInfo.getMap(), c);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
                            }else{
                                    c = newC;                                                                              
                            }
                    }else{
                            randomMovement = true;
                            try {
								c = getRandomPosition(mapInfo.getMap(), c);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                    }
                    //c = getRandomPosition(mapInfo.getMap(), c);

                    showMessage("New cell to move "+c);
            }
   
            return c;
		}
		
		public void action()
		{
			Cell actualPosition = mapInfo.getAgentCell(myAID);
		    
			// Collision avoidance module
            Cell c = checkCollisions(mapInfo.getMap(), actualPosition);

            // Move normally
            if(c == null){
                    if(followingOptimalPath){
                            c = moveNormally();
                    } else { // we have recently been avoiding a collision
                            followingOptimalPath = true;
                            // modify the optimal path adding the steps to go back to the lastOptimalPoint!!
                            //objectives.add(0, objectivePosition);
                            //objectivePosition = lastOptimalPoint;
                            
                            c = moveNormally();
                    }
            } else if(followingOptimalPath) { // We are avoiding a collision and we where recently following the optimal path
                    followingOptimalPath = false;
                    lastOptimalPoint = mapInfo.getAgentCell(this.myAgent.getAID());
                    
            }
				
           
            // Send the cell
            
            ACLMessage reply2 = new ACLMessage(ACLMessage.INFORM);
            reply2.clearAllReceiver();
            reply2.addReceiver(receptor);
            //reply2.setProtocol(InteractionProtocol.FIPA_REQUEST);
            //reply2.setPerformative(ACLMessage.INFORM);
            try {
				reply2.setContentObject(c);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //Return a new cell to harvester coordinator
            send(reply2);
            showMessage("Sending the cell position to "+receptor);


			
			/*showMessage("STATE_4");
			
			
			AID myAID = this.myAgent.getAID();
			Cell c = mapInfo.getAgentCell(myAID);
			// Send the cell
        	ACLMessage reply2 = reply.createReply();
  	      	reply2.setPerformative(ACLMessage.INFORM);
  	      	try {	
  	      		int avail_capacity = mapInfo.getInfoAgent(myAID).getMaxUnits() - mapInfo.getInfoAgent(myAID).getUnits();
  	      		// since the objective is a building, the agent must be at distance 1
  	      		if(avail_capacity > 0 && (objectivePosition.getCellType() == Cell.BUILDING || objectivePosition.getCellType() == Cell.RECYCLING_CENTER) && 
  	      				Math.abs(objectivePosition.getRow() - c.getRow()) + Math.abs(objectivePosition.getColumn() - c.getColumn()) == 1){
  	      			showMessage("Curently at objective");
  	      			switch (objectivePosition.getCellType()){
  	      			case Cell.BUILDING:
  	      				
  	      				if(objectivePosition.getGarbageUnits() > 0){
  	      					c.getAgent().setUnits(c.getAgent().getUnits() + 1);
  	      					//mapInfo.getInfoAgent(myAID).setUnits(mapInfo.getInfoAgent(myAID).getUnits() + 1);
  	      					objectivePosition.setGarbageUnits(objectivePosition.getGarbageUnits()-1);
  	      				}
  	      				break;
  	      				
  	      			case Cell.RECYCLING_CENTER:
  	      				showMessage("TODO: recycle GARBAGE");
  	      				break;
  	      			}
  	      		}else{
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
  	      		}
  	      		reply2.setContentObject(c); //Return a new cell to harvester coordinator
  	      	} catch (Exception e1) {
  	      		reply2.setPerformative(ACLMessage.FAILURE);
  	      		System.err.println(e1.toString());
  	      	}
  	      	send(reply2);
			showMessage("Sending the cell position to "+receptor);	*/			
		}

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
			showMessage("STATE_4: Position sent.");
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
									auctionInfo = (AuxGarbage) reply.getContentObject();
									
									showMessage("Receiving garbage info from "+ reply.getSender());
									
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
									messagesQueue.add(reply);
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
			
			AID myAID = this.myAgent.getAID();
			Cell c = mapInfo.getAgentCell(myAID);
			
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
			float capacity = mapInfo.getInfoAgent(myAID).getMaxUnits() - mapInfo.getInfoAgent(myAID).getUnits();
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
				float num_objectives = 10;
				if(objectives.size() > 0) num_objectives = 1/objectives.size();
				bid = 1/distance1 + 1/tmp_capacity + 1/distance2 + points + num_objectives;
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
									if(myAID.equals(winner)){
										if(objectives.size() == 0 && randomMovement == true){
											randomMovement = false;
											objectivePosition = auctionInfo.getInfo();
											showMessage("Objective position updated!!");
										} else{
											objectives.add(auctionInfo.getInfo());
										}
										

									}
									okInfo = true;
									originalInfo = reply;
								} catch (UnreadableException e) {
									messagesQueue.add(reply);
									System.err.println(getLocalName()
												+ " Recieved game info unsucceeded. Reason: "
												+ e.getMessage());
								} catch (Exception e) {
									messagesQueue.add(reply);
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
	
	private int mapPosWithDirection(int[] pos, Cell actualPos)
	{
		int x = pos[0], y = pos[1], actual_x = actualPos.getRow(), actual_y = actualPos.getColumn();
		
		int x_dif = actual_x - x;
		int y_dif = actual_y - y;
		
		if (x_dif == 1 && y_dif == 0)
			return this.NORT;
		if (x_dif == 0 && y_dif == 1)
			return this.EST;
		if (x_dif == -1 && y_dif == 0)
			return this.SUD;
		if ( x_dif == 0 && y_dif == -1)
			return this.WEST;
		
		System.err.println("Position not correct");
		return -1;
	}
	
	private int getOppositeDirection (int direction)
	{
		if (direction == this.NORT)
			return this.SUD;
		if (direction == this.EST)
			return this.WEST;
		if (direction == this.SUD)
			return this.NORT;
		if (direction == this.WEST)
			return this.EST;
		
		System.err.println("Direction not correct");
		return -1;
	}
	
	/*
	 * Return a Cell to move in order to avoid collitions. Return null when there not exist collisions.
	 */
	private Cell checkCollisions(Cell[][] map, Cell actualPosition)
	{
		Cell positionToReturn = null;
		
		if (!this.has_collision)
		{
			ArrayList<int[]> listColisions = detectCollision(map, actualPosition);
			if ( !listColisions.isEmpty())
			{
				for (int[] col : listColisions)
				{
					int xi = col[0], yi = col[1];
					InfoAgent other_agent = map[xi][yi].getAgent();
					
					if (other_agent.hasCollision() && other_agent.getAID() != this.preference_over)
					{
						this.has_collision = true;
						this.preference_over = null;
						int other_direction = mapPosWithDirection(col, actualPosition);
						this.contrari_colisio = getOppositeDirection(other_direction);
					}
					else if (!applyPolitic(col, map) && !this.cantMove)
					{
						this.has_collision = true;
						this.preference_over = null;
						int other_direction = mapPosWithDirection(col, actualPosition);
						this.contrari_colisio = getOppositeDirection(other_direction);
					}
					else if (applyPolitic(col, map))
					{
						this.preference_over = other_agent.getAID();
						if (other_agent.cantMove())
						{
							this.has_collision = true;
							this.preference_over = null;
							int other_direction = mapPosWithDirection(col, actualPosition);
							this.contrari_colisio = getOppositeDirection(other_direction);
						}
					}
				}
			}
			else 
			{
				this.has_collision = false;
				this.contrari_colisio = -1;
				this.cantMove = false;
			}
		}
		else
		{
			ArrayList<int[]> listColisions = detectCollision(map, actualPosition);
			if ( listColisions.isEmpty())
			{
				this.has_collision = false;
				this.contrari_colisio = -1;
				//this.cantMove = false; ???
			}
			else if (!listColisions.isEmpty() && this.cantMove)
			{
				for (int[] col : listColisions)	// Aquest for esta be????
				{
					int xi = col[0], yi = col[1];
					InfoAgent other_agent = map[xi][yi].getAgent();
					
					this.has_collision = false;
					this.contrari_colisio = -1;
					this.preference_over = other_agent.getAID();
				}
			}
			else if(!listColisions.isEmpty()) // if there is any collision
			{
				for (int[] col : listColisions) // for each collisions
				{
					int xi = col[0], yi = col[1];
					InfoAgent other_agent = map[xi][yi].getAgent();
					if (other_agent.cantMove()) // if the other agent can't move, then we have to give him priority
					{
						this.has_collision = true;
						this.preference_over = null;
						int other_direction = mapPosWithDirection(col, actualPosition);
						this.contrari_colisio = getOppositeDirection(other_direction);
						this.cantMove = true;
					}
				}		
			}	
		}
		
		// Let's decide where to move
		if (this.has_collision)
		{
			if (this.contrari_colisio == this.NORT || this.contrari_colisio == this.SUD)
			{
				int directionToMove;
				
				// Random movement to contrari direction
				Random r = new Random();
				if ( r.nextInt(1) == 1 )
					directionToMove = this.EST;	
				else
					directionToMove = this.WEST;
				
				if (canMoveToCell(actualPosition, directionToMove, map))
				{
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				else if ( canMoveToCell(actualPosition, getOppositeDirection(directionToMove), map) )
				{
					directionToMove = getOppositeDirection(directionToMove);
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				// MOVE TO CONTRARI COLISIO
				else if ( canMoveToCell(actualPosition, contrari_colisio, map) )
				{
					directionToMove = contrari_colisio;
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				
				// CAS EN QUE HI HA PARETS
				else if ( isSurroundedByWalls(actualPosition,map) )
				{
					this.cantMove = true;
					positionToReturn = actualPosition;
				}
			}
			
			
			else if (this.contrari_colisio == this.EST || this.contrari_colisio == this.WEST)
			{
				int directionToMove;
				
				// Random movement to contrari direction
				Random r = new Random();
				if ( r.nextInt(1) == 1 )
					directionToMove = this.NORT;	
				else
					directionToMove = this.SUD;
				
				if (canMoveToCell(actualPosition, directionToMove, map))
				{
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				else if ( canMoveToCell(actualPosition, getOppositeDirection(directionToMove), map) )
				{
					directionToMove = getOppositeDirection(directionToMove);
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				// MOVE TO CONTRARI COLISIO
				else if ( canMoveToCell(actualPosition, contrari_colisio, map) )
				{
					directionToMove = contrari_colisio;
					positionToReturn = map[ actualPosition.getRow() + moviment.get(directionToMove)[0] ] [actualPosition.getColumn() + moviment.get(directionToMove)[1]];
				}
				
				// CAS EN QUE HI HA PARETS
				else if ( isSurroundedByWalls(actualPosition,map) )
				{
					this.cantMove = true;
					positionToReturn = actualPosition;
				} else {
					positionToReturn = actualPosition;
				}
			}
			
			// Update info agent in the new cell
			try {
				positionToReturn.removeAgent(this.myAID);
				positionToReturn.addAgent(mapInfo.getInfoAgent(this.getAID()));
			} catch (Exception e) {
				try {
					positionToReturn.addAgent(actualPosition.getAgent());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
			} //Save infoagent to the new position
			
		}
		
		else // WE ARE GONNA MOVE OPTIMAL WAY
		{
			positionToReturn =  null; // MEANS THAT THERE IS NO COLLISION
		}
		
		// Update INFOAGENT
		InfoAgent thisInfoAgent = mapInfo.getInfoAgent(this.myAID);
		thisInfoAgent.setHasCollision(this.has_collision); 
		thisInfoAgent.setCantMove(this.cantMove);
		
		return positionToReturn;
	}
	
	private boolean isSurroundedByWalls (Cell actualPosition, Cell[][] map)
	{
		int x=actualPosition.getRow(), y=actualPosition.getColumn(), z = 0, xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		
		int [][] nearPlaces = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(nearPlaces);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);
		
		int valid_position = 0, not_street_cells = 0;
		
		int[] list = null;
		// Search a cell street
		while (arrayList.size() != 0) {
			Random a = new Random();
			list = arrayList.remove(a.nextInt(arrayList.size()));

			xi = list[0];
			yi = list[1];

			if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns) {
				Cell position = map[xi][yi];
				valid_position++;
				if (position.getCellType() != Cell.STREET) {
					not_street_cells++;
				}
			}
		}
		// If the agent has only 1 factible movement and the other non valid position are street cells, then the agent is surrounded by walls
		if (valid_position - not_street_cells == 1)
			return true;
		else return false;
	}
	
	private boolean canMoveToCell (Cell actualPosition, int DIRECTION, Cell[][] map)
	{
		int x=actualPosition.getRow(), y=actualPosition.getColumn(), z = 0, xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		
		int[] list = moviment.get(DIRECTION);
		xi = list[0] + x;
		yi = list[1] + y;
			
		if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns) {
			Cell dest = map[xi][yi];
			if (dest.getCellType() == Cell.STREET) {
				if(!dest.isThereAnAgent())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private ArrayList<int[]> detectCollision (Cell[][] map, Cell actualPosition)
	{
		int x=actualPosition.getRow(), y=actualPosition.getColumn(), z = 0, xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		
		ArrayList<int[]> list_colisions = new ArrayList<int[]>();
		
		int [][] nearPlaces = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(nearPlaces);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);
		
		int[] list = null;
		// Search a cell street
		while (arrayList.size() != 0) {
			Random a = new Random();
			list = arrayList.remove(a.nextInt(arrayList.size()));

			xi = list[0];
			yi = list[1];

			if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns) {
				Cell position = map[xi][yi];
				if (position.getCellType() == Cell.STREET) {
					if(position.isThereAnAgent())
					{
						int[] idx_colision = {xi,yi};
						list_colisions.add(idx_colision);
					}
				}
			}
		}
		
		return list_colisions;
	}
	
	private boolean applyPolitic(int[] colision_pos, Cell[][] map)
	{
		int xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		Cell newPosition;
		
		xi = colision_pos[0]; 
		yi = colision_pos[1];
		
		if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns)
		{
			newPosition = map[xi][yi];
			if(Cell.STREET == newPosition.getCellType() )	//Check the limits of the map
			{ 
				/* If there is a scout agent in front of */ 
				if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.SCOUT)
					return true;
				
				  /*If there is a harvester agent in front of */
				else if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.HARVESTER)
				{
					// TODO
					InfoAgent infoagent = mapInfo.getInfoAgent(this.myAID);
					InfoAgent other_infoagent = newPosition.getAgent();
					int garbageAmount = infoagent.getGarbageUnits()[InfoAgent.GLASS] + infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + infoagent.getGarbageUnits()[InfoAgent.METAL] + infoagent.getGarbageUnits()[InfoAgent.PAPER];
					int other_garbageAmount = other_infoagent.getGarbageUnits()[InfoAgent.GLASS] + other_infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + other_infoagent.getGarbageUnits()[InfoAgent.METAL] + other_infoagent.getGarbageUnits()[InfoAgent.PAPER];
					
					 /* The harvester carrying more garbage is the one that is going to move */ 
					if (garbageAmount > other_garbageAmount)
						return true;
					
					else if (garbageAmount < other_garbageAmount)
						return false;
					
					else	 /*Random decision by ID in case of draw*/ 
					{
						int h1 =  mapInfo.getInfoAgent(this.myAID).getAID().hashCode();
						int h2 = newPosition.getAgent().getAID().hashCode();
						if (h1 > h2)
						{
							return true;
						}
						else
							return false;
					}
				}
				  /* If there is not an agent */  
				else
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private void findNextObjective(){
		float best = Float.POSITIVE_INFINITY;
		int position = 0;
		int final_pos = 0;
		Cell curr_pos = mapInfo.getAgentCell(this.getAID());
		for(Cell o : objectives){
			float distance = Math.abs(curr_pos.getRow() - o.getRow()) + Math.abs(curr_pos.getColumn() - o.getColumn());
			distance += position/objectives.size();
			if(best > distance){
				best = distance;
				final_pos = position;
			}
			position++;
		}
		
		objectivePosition = objectives.remove(final_pos);
		System.err.println("NEW OBJECTIVE: "+objectivePosition + "; pos = " + curr_pos);
	}
	
	private Cell getBestPositionToObjective(Cell[][] cells, Cell actualPosition, Cell objectivePosition) {
		
		Cell newPosition = null;
		
		showMessage("I am in the cell = ["+actualPosition.getRow()+","+actualPosition.getColumn()+"]");
		showMessage("I wanna go to the cell = ["+objectivePosition.getRow()+","+objectivePosition.getColumn()+"]");
		
		newPosition = astar.shortestPath(cells, actualPosition, objectivePosition);
		
		if (newPosition == null)
			return null;
		
		int xi = newPosition.getRow();
		int yi = newPosition.getColumn();
		int maxRows = mapInfo.getMapRows();
		int maxColumns = mapInfo.getMapColumns();
		
		if (xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns)
		{
				Cell position = cells[xi][yi];
				if (position.getCellType() == Cell.STREET) {
					try {
						if (!newPosition.equals(actualPosition))
						{
							newPosition.removeAgent(this.myAID);
							newPosition.addAgent(mapInfo.getInfoAgent(this.getAID()));
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 
					
				}
		}
		
		
		//newPosition = checkIfPositionIsOccupied(newPosition, cells, actualPosition);
		
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
			Random a = new Random();
			list = arrayList.remove(a.nextInt(arrayList.size()));

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
					try {
						showMessage("Position before moving "+"["+x+","+y+"]");
						newPosition.removeAgent(this.getAID());
						newPosition.addAgent(mapInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
						positionToReturn = newPosition;
						//actualPosition.removeAgent(actualPosition.getAgent());
					} catch (Exception e) {
						showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
					}
					trobat = true;
					
					 /* If there is a scout agent in front of */ 
					/*if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.SCOUT)
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
					  If there is a harvester agent in front of 
					else if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.HARVESTER)
					{
						// TODO
						InfoAgent infoagent = mapInfo.getInfoAgent(this.harvesterCoordinatorAgent);
						InfoAgent other_infoagent = newPosition.getAgent();
						int garbageAmount = infoagent.getGarbageUnits()[InfoAgent.GLASS] + infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + infoagent.getGarbageUnits()[InfoAgent.METAL] + infoagent.getGarbageUnits()[InfoAgent.PAPER];
						int other_garbageAmount = other_infoagent.getGarbageUnits()[InfoAgent.GLASS] + other_infoagent.getGarbageUnits()[InfoAgent.PLASTIC] + other_infoagent.getGarbageUnits()[InfoAgent.METAL] + other_infoagent.getGarbageUnits()[InfoAgent.PAPER];
						
						 The harvester carrying more garbage is the one that is going to move 
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
						
						else	 Random decision by ID in case of draw 
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
					  If there is not an agent  
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
					}*/
				}
			}
		}
		
		return positionToReturn;
	}
}
