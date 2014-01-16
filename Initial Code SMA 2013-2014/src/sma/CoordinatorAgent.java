package sma;

import java.lang.*;
import java.io.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;




//import jade.util.leap.ArrayList;
import jade.util.leap.List;
import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.content.*;
import jade.content.onto.*;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import sma.ontology.*;
import sma.gui.*;

import java.util.*;
/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * <p><b>Copyright:</b> Copyright (c) 2009</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author not attributable
 * @version 2.0
 */
public class CoordinatorAgent extends Agent {
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = true;

  private AuxInfo info;
  private int[] countMapRequests = {0, 0};
  private int countMovementsReceived = 0;
  
  private AID centralAgent;
  private AID harvCoordAgent;
  private AID scoutCoordAgent;
  
  // initializes the array that will store the movement of each agent for each turn
  private ArrayList<Cell> newMovements;
  // stores the new discoveries found
  private ArrayList newDiscoveries = new ArrayList();
  
  // array storing the not handled messages
  private MessagesList messagesQueue = new MessagesList(this);

 
  public CoordinatorAgent() {
  }

  /**
   * A message is shown in the log area of the GUI
   * @param str String to show
   */
  private void showMessage(String str) {
	  if(debugging)
		  System.out.println(getLocalName() + ": " + str);
  }


  /**
   * Agent setup method - called when it first come on-line. Configuration
   * of language to use, ontology and initialization of behaviours.
   */
  protected void setup() {

    /**** Very Important Line (VIL) *********/
    this.setEnabledO2ACommunication(true, 1);
    /****************************************/

    // Register the agent to the DF
    ServiceDescription sd1 = new ServiceDescription();
    sd1.setType(UtilsAgents.COORDINATOR_AGENT);
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

    // search CentralAgent, HarvesterCoord and ScoutsCoord
    ServiceDescription searchCriterion = new ServiceDescription();
    searchCriterion.setType(UtilsAgents.CENTRAL_AGENT);
    this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
    searchCriterion = new ServiceDescription();
    searchCriterion.setType(UtilsAgents.HARVESTER_COORDINATOR_AGENT);
    this.harvCoordAgent = UtilsAgents.searchAgent(this, searchCriterion);
    searchCriterion = new ServiceDescription();
    searchCriterion.setType(UtilsAgents.SCOUT_COORDINATOR_AGENT);
    this.scoutCoordAgent = UtilsAgents.searchAgent(this, searchCriterion);
    // searchAgent is a blocking method, so we will obtain always a correct AID

   /**************************************************/
    ACLMessage requestInicial = new ACLMessage(ACLMessage.REQUEST);
    requestInicial.clearAllReceiver();
    requestInicial.addReceiver(this.centralAgent);
    requestInicial.setProtocol(InteractionProtocol.FIPA_REQUEST);
    showMessage("Message OK");
    try {
      requestInicial.setContent("Initial request");
      showMessage("Content OK " + requestInicial.getContent());
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
    // Behaviour initial communication with CentralAgent
    fsm.registerFirstState(new RequestMapInfo(this, requestInicial), "STATE_1");
    // Behaviour initial communication with CoordinatorHarvesters and CoordinatorScouts
    fsm.registerState(new ListenRequestMap(this), "STATE_2");
    // Waiting for movements from lower layer coordinators
    fsm.registerState(new ReceiveMovements(this), "STATE_3");
    // Send movements received to CentralAgent and wait for its map response
    fsm.registerState(new SendMovesBehaviour(this), "STATE_4");
    // Waiting for all new discoveries from ScoutsCoordinator
    fsm.registerState(new ReceiveNewDiscoveriesScouts(this), "STATE_5");
    // Sends all new discoveries to CentralAgent
    fsm.registerState(new SendNewDiscoveriesCentral(this), "STATE_6");
    // Waiting for only the really new discoveries from CentralAgent
    fsm.registerState(new ReceiveNewDiscoveriesCentral(this), "STATE_7");
    // Sends all new discoveries to HarvesterCoordinator
    fsm.registerState(new SendNewDiscoveriesHarvesters(this), "STATE_8");
    
    // FSM transitions
    fsm.registerDefaultTransition("STATE_1", "STATE_2");
    fsm.registerTransition("STATE_2", "STATE_2", 1);
    //fsm.registerTransition("STATE_2", "STATE_3", 2);
    
    fsm.registerTransition("STATE_2", "STATE_5", 2);
    fsm.registerDefaultTransition("STATE_5", "STATE_6");
    fsm.registerDefaultTransition("STATE_6", "STATE_7");
    fsm.registerDefaultTransition("STATE_7", "STATE_8");
    fsm.registerDefaultTransition("STATE_8", "STATE_3");
    
    fsm.registerTransition("STATE_3", "STATE_3", 1);
    fsm.registerTransition("STATE_3", "STATE_4", 2);
    fsm.registerDefaultTransition("STATE_4", "STATE_2");
    
    addBehaviour(fsm);

  } //endof setup
  
  
  /**
   * Manages the received moves updating the newMovements list.
   * 
   * @param moves Object containing 
   */
  private boolean processMovements(Object moves){
	  
	  try{
		  Cell m = (Cell)moves;
		  newMovements.add(countMovementsReceived, m);
		  countMovementsReceived++;
		  
		  showMessage("Saving MOVEMENTS");
		  return true;
	  }catch(Exception e){
		  return false;
	  }
  }
  
  /*************************************************************************/
  

  /**
	 * 
	 * @author Albert Busqu� and Marc Bola�os
	 *
	 * Class that implements behavior for requesting game info (map)
	 * STATE_1
	 * Initial request to the CentralAgent.
	 */
	protected class RequestMapInfo extends SimpleBehaviour 
	{
		private ACLMessage msg;
		
		public RequestMapInfo (Agent a, ACLMessage m)
		{
			super(a);
			this.msg = m;
		}

		@Override
		public void action() {
			showMessage("STATE_1");
			// Make the request
		    try {
			      send(msg);
			    } catch (Exception e) {
			      e.printStackTrace();
			    }

		    boolean okInfo = false;
		    while(!okInfo){
		    	
		    	ACLMessage reply = messagesQueue.getMessage();
		    		
		    	if (reply != null)
		    	{
		    		switch (reply.getPerformative())
		    		{
			    		case ACLMessage.AGREE:
			    			showMessage("Recieved AGREE from "+reply.getSender().getLocalName());
			    			break;
			    		case ACLMessage.INFORM:
							try {
								info = (AuxInfo) reply.getContentObject();	// Getting object with the information about the game
								okInfo = true;
								newMovements = new ArrayList<Cell>(info.getNumHarvesters()+info.getNumScouts());
								showMessage("Received game info from "+reply.getSender().getLocalName());
							} catch (UnreadableException e) {
								// Unexpected messages received must be added to the queue.
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
	    	showMessage("STATE_1 return OK");
	    	return 0;
	    }
	}

  
/*************************************************************************/
  
	/**
	 * 
	 * STATE_2
	 * Waiting for initial requests from lower layer coordinators.
	 */
	protected class ListenRequestMap extends SimpleBehaviour 
	{
		public ListenRequestMap (Agent a)
		{
			super(a);
		}

		@Override
		public void action() {
			
			showMessage("STATE_2");
			showMessage("Waiting for Map REQUESTs from authorized agents");
			
			boolean okGetMap = false;
		    while(!okGetMap){
		    	ACLMessage msg = messagesQueue.getMessage();
		    	if (msg != null)
		    	{
		    		// If "get map" request received
		    		Object contentRebut = (Object)msg.getContent();
			        if(contentRebut.equals("get map")) {
			        	
			        	boolean receive = true;
			        	// If is scout coordinator agent
			        	if(msg.getSender().getLocalName().equals(scoutCoordAgent.getLocalName()) && countMapRequests[0] == 0){
			        		countMapRequests[0] = 1;
			        	// If is harvester coordinator agent
			        	} else if(msg.getSender().getLocalName().equals(harvCoordAgent.getLocalName()) && countMapRequests[1] == 0){
			        		countMapRequests[1] = 1;
			        	} else {
			        		receive = false;
			        	}
			        	
			        	if(receive){
			        		okGetMap = true;
				        	showMessage("Map request from " + msg.getSender().getLocalName() + " received.");
				        	// Send agree
				        	ACLMessage reply1 = msg.createReply();
				        	reply1.setPerformative(ACLMessage.AGREE);
				        	
				        	send(reply1);
				        	
				        	// Send map
				        	ACLMessage reply2 = msg.createReply();
				  	      	reply2.setPerformative(ACLMessage.INFORM);
	
				  	      	try {
				  	      		reply2.setContentObject(info);
				  	      	} catch (Exception e) {
				  	      		reply2.setPerformative(ACLMessage.FAILURE);
				  	      		System.err.println(e.toString());
				  	      		e.printStackTrace();
				  	      	}
				  	      	send(reply2);
				  	      	
				  	    // We have already received a petition from this agent in this turn
			        	} else {
			        		messagesQueue.add(msg);
			        	}
			        } else {
			        	messagesQueue.add(msg);
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
	    	int thisC = countMapRequests[0] + countMapRequests[1];
	    	if(thisC == 2){
	    		countMapRequests[0] = 0;
	    		countMapRequests[1] = 0;
	    	}
	    	showMessage("onEnd ListenRequestMap " + String.valueOf(thisC));
	    	return thisC;
	    }
	}
  
  
/*************************************************************************/
  
  /**
   * 
   * STATE_3
   * Waiting for new movements from lower layer coordinators.
   * 
   * @author Marc Bola�os
   *
   */
	protected class ReceiveMovements extends SimpleBehaviour {
		
		public ReceiveMovements (Agent a)
		{
			super(a);
		}

		@Override
		public void action() {
			
			showMessage("STATE_3");
			showMessage("Waiting for movements from lower layers.");

			boolean okMovements = false;
			while(!okMovements){
				ACLMessage msg = messagesQueue.getMessage();
				
				try {
					Object contentRebut = msg.getContentObject();
			        if(processMovements(contentRebut)) {
			        	okMovements = true;
			        	showMessage("New movements from " + msg.getSender().getLocalName() + " received.");
			        } else {
			        	messagesQueue.add(msg);
			        }
				}catch (Exception e){
					messagesQueue.add(msg);
				}
			}
			
	        messagesQueue.endRetrieval();
	        
		}

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
			int numAgents = info.getNumHarvesters()+info.getNumScouts();
	    	showMessage("onEnd ReceivedMovements " + String.valueOf(countMovementsReceived) + "/" + String.valueOf(numAgents));
	    	if(countMovementsReceived < numAgents){
	    		return 1;
	    	} else{
	    		return 2;
	    	}
	    }
	}


  /*************************************************************************/
  
  /**
   * STATE_4
   * Sends the movements to the CentralAgent and reeds the new map information.
   * 
   * @author Marc Bola�os
   *
   */
	protected class SendMovesBehaviour extends SimpleBehaviour {
		
		public SendMovesBehaviour (Agent a)
		{
			super(a);
		}

		@Override
		public void action() {
			
			showMessage("STATE_4");
			
			// Requests the map again sending the list of movements
	        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.clearAllReceiver();
			request.addReceiver(CoordinatorAgent.this.centralAgent);
			request.setProtocol(InteractionProtocol.FIPA_REQUEST);

		    try {
		    	request.setContentObject(newMovements);
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		    
		    send(request);
		    
			// Reset movements list
			newMovements = new ArrayList<Cell>(info.getNumHarvesters()+info.getNumScouts());
			countMovementsReceived = 0;
			countMapRequests[0] = 0; countMapRequests[1] = 0;
			
			// Receiving the new AuxInfo from the CentralAgent
			boolean auxReceived = false;
			while(!auxReceived){
				
		    	ACLMessage reply = messagesQueue.getMessage();
		    	if (reply != null)
		    	{
		    		switch (reply.getPerformative())
		    		{
			    		case ACLMessage.AGREE:
			    			showMessage("Recieved AGREE from "+reply.getSender().getLocalName());
			    			break;
			    		case ACLMessage.INFORM:
							try {
								info = (AuxInfo) reply.getContentObject();	// Getting object with the information about the game
								auxReceived = true;
								showMessage("Recieved game info from "+reply.getSender().getLocalName());
							} catch (UnreadableException e) {
								messagesQueue.add(reply);
								System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: " + e.getMessage());
							}
			    			break;
			    		case ACLMessage.FAILURE:
			    			System.err.println(getLocalName() + " Recieved game info unsucceeded. Reason: Performative was FAILURE");
			    			break;
			    		default:
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
	    	showMessage("STATE_4 return 0");
	    	return 0;
	    }
	}

  
  /*************************************************************************/
	
	/**
	   * 
	   * STATE_5
	   * Waiting for new discoveries from the ScoutsCoordinator.
	   * 
	   * @author Marc Bola�os
	   *
	   */
		protected class ReceiveNewDiscoveriesScouts extends SimpleBehaviour {
			
			public ReceiveNewDiscoveriesScouts (Agent a)
			{
				super(a);
			}

			@Override
			public void action() {
				
				showMessage("STATE_5");
				showMessage("Waiting for new discoveries from ScoutsCoordinator.");

				boolean okDisc = false;
				while(!okDisc){
					ACLMessage msg = messagesQueue.getMessage();
					
					try {
						ArrayList contentRebut = (ArrayList)msg.getContentObject();
				        if(msg.getSender().getLocalName().equals(scoutCoordAgent.getLocalName())) {
				        	newDiscoveries = contentRebut;
				        	okDisc = true;
				        	showMessage("New discoveries from " + msg.getSender().getLocalName() + " received.");
				        } else {
				        	messagesQueue.add(msg);
				        }
					}catch (Exception e){
						messagesQueue.add(msg);
					}
				}
				
		        messagesQueue.endRetrieval();
		        
			}

			@Override
			public boolean done() {
				return true;
			}
			
			public int onEnd(){
				showMessage("STATE_5 return 0");
				return 0;
		    }
		}


	  /*************************************************************************/
		
		/**
		   * STATE_6
		   * Sends the new discoveries to the CentralAgent.
		   * 
		   * @author Marc Bola�os
		   *
		   */
			protected class SendNewDiscoveriesCentral extends SimpleBehaviour {
				
				public SendNewDiscoveriesCentral (Agent a)
				{
					super(a);
				}

				@Override
				public void action() {
					
					showMessage("STATE_6");
					
					// Requests the map again sending the list of movements
			        ACLMessage discoveriesMsg = new ACLMessage(ACLMessage.INFORM);
			        discoveriesMsg.clearAllReceiver();
			        discoveriesMsg.addReceiver(CoordinatorAgent.this.centralAgent);
			        discoveriesMsg.setProtocol(InteractionProtocol.FIPA_REQUEST);

				    try {
				    	discoveriesMsg.setContentObject(newDiscoveries);
				    } catch (Exception e) {
				    	e.printStackTrace();
				    }
				    
				    send(discoveriesMsg);
					
				}

				@Override
				public boolean done() {
					return true;
				}
				
				public int onEnd(){
			    	showMessage("STATE_6 return 0");
			    	return 0;
			    }
			}

		  
		/*************************************************************************/
		
		  /**
		   * 
		   * STATE_7
		   * Waiting for new discoveries from the CentralAgent.
		   * 
		   * @author Marc Bola�os
		   *
		   */
			protected class ReceiveNewDiscoveriesCentral extends SimpleBehaviour {
				
				public ReceiveNewDiscoveriesCentral (Agent a)
				{
					super(a);
				}

				@Override
				public void action() {
					
					showMessage("STATE_7");
					showMessage("Waiting for new discoveries from CentralAgent.");

					boolean okDisc = false;
					while(!okDisc){
						ACLMessage msg = messagesQueue.getMessage();
						
						try {
							ArrayList contentRebut = (ArrayList)msg.getContentObject();
					        if(msg.getSender().getLocalName().equals(centralAgent.getLocalName())) {
					        	newDiscoveries = contentRebut;
					        	okDisc = true;
					        	showMessage("New discoveries from " + msg.getSender().getLocalName() + " received.");
					        } else {
					        	messagesQueue.add(msg);
					        }
						}catch (Exception e){
							messagesQueue.add(msg);
						}
					}
					
			        messagesQueue.endRetrieval();
			        
				}

				@Override
				public boolean done() {
					return true;
				}
				
				public int onEnd(){
					showMessage("STATE_7 return 0");
					return 0;
			    }
			}


		  /*************************************************************************/
		  
		  /**
		   * STATE_8
		   * Sends the new discoveries to the HarvestersCoordinator.
		   * 
		   * @author Marc Bola�os
		   *
		   */
			protected class SendNewDiscoveriesHarvesters extends SimpleBehaviour {
				
				public SendNewDiscoveriesHarvesters (Agent a)
				{
					super(a);
				}

				@Override
				public void action() {
					
					showMessage("STATE_8");
					
					// Requests the map again sending the list of movements
			        ACLMessage discoveriesMsg = new ACLMessage(ACLMessage.INFORM);
			        discoveriesMsg.clearAllReceiver();
			        discoveriesMsg.addReceiver(CoordinatorAgent.this.harvCoordAgent);
			        discoveriesMsg.setProtocol(InteractionProtocol.FIPA_REQUEST);

				    try {
				    	discoveriesMsg.setContentObject(newDiscoveries);
				    } catch (Exception e) {
				    	e.printStackTrace();
				    }
				    
				    send(discoveriesMsg);
					
				}

				@Override
				public boolean done() {
					return true;
				}
				
				public int onEnd(){
			    	showMessage("STATE_8 return 0");
			    	return 0;
			    }
			}

		  
		/*************************************************************************/
  
  

} //endof class CoordinatorAgent