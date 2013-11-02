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

  private AuxInfo info;
  private int countMapRequests = 0;
  private int countMovementsReceived = 0;
  
  private AID centralAgent;
  
  // initializes the array that will store the movement of each agent for each turn
  private ArrayList<Movement> newMovements;
  private int movReceivedScouts = 0;
  private int movReceivedHarvesters = 0;

 
  public CoordinatorAgent() {
  }

  /**
   * A message is shown in the log area of the GUI
   * @param str String to show
   */
  private void showMessage(String str) {
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

    // search CentralAgent
    ServiceDescription searchCriterion = new ServiceDescription();
    searchCriterion.setType(UtilsAgents.CENTRAL_AGENT);
    this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
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

    // Behaviour initial communication with CentralAgent
    //this.addBehaviour(new RequesterBehaviour(this, requestInicial));
    // Behaviour initial communication with CoordinatorHarvesters and CoordinatorScouts
    //this.addBehaviour(new RequestResponseBehaviour(this, null));

    // setup finished. When we receive the last inform, the agent itself will add
    // a behaviour to send/receive actions
    
    
    // Finite State Machine
    FSMBehaviour fsm = new FSMBehaviour(this);
    // Behaviour initial communication with CentralAgent
    fsm.registerFirstState(new RequesterBehaviour(this, requestInicial), "STATE_1");
    // Behaviour initial communication with CoordinatorHarvesters and CoordinatorScouts
    fsm.registerState(new ListenRequestMap(this, null), "STATE_2");
    // Waiting for movements from lower layer coordinators
    fsm.registerState(new ReceiveMovements(this, null), "STATE_3");
    // Send movements received to CentralAgent and wait for its map response
    fsm.registerLastState(new SendMovesBehaviour(this, null), "STATE_4");
    
    // FSM transitions
    fsm.registerDefaultTransition("STATE_1", "STATE_2");
    fsm.registerTransition("STATE_2", "STATE_2", 1);
    fsm.registerTransition("STATE_2", "STATE_3", 2);
    fsm.registerTransition("STATE_3", "STATE_3", 1);
    fsm.registerTransition("STATE_3", "STATE_4", 2);
    fsm.registerDefaultTransition("STATE_4", "STATE_2");
    

  } //endof setup
  
  
  /**
   * Manages the received moves updating the newMovements list.
   * 
   * @param moves Object containing 
   */
  private boolean processMovements(Object moves){
	  ArrayList<Movement> mo_list = (ArrayList)moves;
	  for(Movement m : mo_list){
		  String agent_id = m.getAgentId();
		  String agent_type = m.getAgentType();
		  int[] pos = m.getPosition();
		  // TODO: update list of movements
		  if(agent_type.equals(UtilsAgents.SCOUT_AGENT)){ // scout
			  newMovements.add(movReceivedScouts, m);
			  movReceivedScouts++;
		  } else if(agent_type.equals(UtilsAgents.HARVESTER_AGENT)){ // harvester
			  newMovements.add(info.getNumScouts()+movReceivedHarvesters, m);
			  movReceivedHarvesters++;
		  }
	  }
	  
	  showMessage("Saving MOVEMENTS");
	  return true;
  }
  
  /*************************************************************************/
  

  /*
   * 
   * STATE_1
   * Initial request to the CentralAgent.
   * 
   * @author Marc Bolaños
   *
   */
  /**
    * <p><B>Title:</b> IA2-SMA</p>
    * <p><b>Description:</b> Practical exercise 2011-12. Recycle swarm.</p>
    * This class lets send the REQUESTs for any agent. Concretely it is used in the 
    * initialization of the game. The Coordinator Agent sends a REQUEST for the information
    * of the game (instance of <code>AuxInfo</code> containing parameters, coordenates of 
    * the agents and the recycling centers and visual range of each agent). The Central Agent
    * sends an AGREE and then it informs of this information which is stored by the Coordinator
    * Agent. The game is processed by another behaviour that we add after the INFORM has been 
    * processed.
    * <p><b>Copyright:</b> Copyright (c) 2011</p>
    * <p><b>Company:</b> Universitat Rovira i Virgili (<a
    * href="http://www.urv.cat">URV</a>)</p>
    * @author David Isern and Joan Albert López
    * @see sma.ontology.Cell
    * @see sma.ontology.InfoGame
   */
  class RequesterBehaviour extends AchieveREInitiator {
	  
	private ACLMessage msgSent = null;
    
    public RequesterBehaviour(Agent myAgent, ACLMessage requestMsg) {
      super(myAgent, requestMsg);
      showMessage("AchieveREInitiator starts...");
      msgSent = requestMsg;
    }

    /**
     * Handle AGREE messages
     * @param msg Message to handle
     */
    protected void handleAgree(ACLMessage msg) {
      showMessage("AGREE received from "+ ( (AID)msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     * @param msg Message
     */
    protected void handleInform(ACLMessage msg) {
    	showMessage("INFORM received from "+ ( (AID)msg.getSender()).getLocalName()+" ... [OK]");
        try {
          info = (AuxInfo)msg.getContentObject();
          if (info instanceof AuxInfo) {
            showMessage("Agents initial position: ");
            for (InfoAgent ia : info.getAgentsInitialPosition().keySet()){  
          	  showMessage(ia.toString());
              Cell pos = (Cell)info.getAgentsInitialPosition().get(ia);
              showMessage("c: " + pos);  	
            }
            showMessage("Garbage discovered: ");
            for (int i=0; i<info.getMap().length; i++){
            	for (int j=0; j<info.getMap()[0].length; j++){
            		if (info.getCell(j,i).getCellType()==Cell.BUILDING)
            			if (info.getCell(j,i).getGarbageUnits()>0) showMessage(info.getCell(j,i).toString());
                }  
            }
            showMessage("Cells with recycling centers: ");
            for (Cell c : info.getRecyclingCenters()) showMessage(c.toString()); 
                   
            
            // Initialize movements list
  		  	newMovements = new ArrayList<Movement>(info.getNumHarvesters()+info.getNumScouts());
  		  	movReceivedScouts = 0;
  		  	movReceivedHarvesters = 0;
            
          }
        } catch (Exception e) {
          showMessage("Incorrect content: "+e.toString());
        }
    }

    /**
     * Handle NOT-UNDERSTOOD messages
     * @param msg Message
     */
    protected void handleNotUnderstood(ACLMessage msg) {
      showMessage("This message NOT UNDERSTOOD. \n");
    }

    /**
     * Handle FAILURE messages
     * @param msg Message
     */
    protected void handleFailure(ACLMessage msg) {
      showMessage("The action has failed.");

    } //End of handleFailure

    /**
     * Handle REFUSE messages
     * @param msg Message
     */
    protected void handleRefuse(ACLMessage msg) {
      showMessage("Action refused.");
    }
    
    public int onEnd(){
    	return 0;
    }
    
  } //Endof class RequesterBehaviour

  
/*************************************************************************/
  
  /**
   * 
   * STATE_2
   * Waiting for initial requests from lower layer coordinators.
   * 
   * @author Marc Bolaños
   *
   */
  private class ListenRequestMap extends AchieveREResponder {

	    /**
	     * Constructor for the <code>RequestResponseBehaviour</code> class.
	     * @param coordinatorAgent The agent owning this behaviour
	     * @param mt Template to receive future responses in this conversation
	     */
	    public ListenRequestMap(CoordinatorAgent coordinatorAgent, MessageTemplate mt) {
	      super(coordinatorAgent, mt);
	      showMessage("Waiting for Map REQUESTs from authorized agents");
	    }

	    protected ACLMessage prepareResponse(ACLMessage msg) {
	      /* method called when the message has been received. If the message to send
	       * is an AGREE the behaviour will continue with the method prepareResultNotification. */
	      ACLMessage reply = msg.createReply();
	      try {
	        Object contentRebut = (Object)msg.getContent();
	        if(contentRebut.equals("get map")) {
	        	showMessage("Map request from " + msg.getSender() + " received.");
	        	reply.setPerformative(ACLMessage.AGREE);
	        }
	      } catch (Exception e) {
	        e.printStackTrace();
	      }
	      //showMessage("Answer sent"); //: \n"+reply.toString());
	      return reply;
	    } //endof prepareResponse   

	    /**
	     * This method is called after the response has been sent and only when
	     * one of the following two cases arise: the response was an agree message
	     * OR no response message was sent. This default implementation return null
	     * which has the effect of sending no result notification. Programmers
	     * should override the method in case they need to react to this event.
	     * @param msg ACLMessage the received message
	     * @param response ACLMessage the previously sent response message
	     * @return ACLMessage to be sent as a result notification (i.e. one of
	     * inform, failure).
	     */
	    protected ACLMessage prepareResultNotification(ACLMessage msg, ACLMessage response) {

	      // it is important to make the createReply in order to keep the same context of
	      // the conversation
	      ACLMessage reply = msg.createReply();
	      reply.setPerformative(ACLMessage.INFORM);

	      try {
	        reply.setContentObject(info);
	      } catch (Exception e) {
	        reply.setPerformative(ACLMessage.FAILURE);
	        System.err.println(e.toString());
	        e.printStackTrace();
	      }
	      //showMessage("Answer sent"); //+reply.toString());
	      return reply;

	    } //endof prepareResultNotification


	    /**
	     *  No need for any specific action to reset this behaviour
	     */
	    public void reset() {
	    }
	    
	    public int onEnd(){
	    	countMapRequests++;
	    	int thisC = countMapRequests;
	    	countMapRequests = countMapRequests%2;
	    	showMessage("onEnd ListenRequestMap " + String.valueOf(thisC));
	    	return thisC;
	    }

	  } //end of RequestResponseBehaviour
  
  
/*************************************************************************/
  
  /**
   * 
   * STATE_3
   * Waiting for new movements from lower layer coordinators.
   * 
   * @author Marc Bolaños
   *
   */
  private class ReceiveMovements extends AchieveREResponder {

	    /**
	     * Constructor for the <code>RequestResponseBehaviour</code> class.
	     * @param coordinatorAgent The agent owning this behaviour
	     * @param mt Template to receive future responses in this conversation
	     */
	    public ReceiveMovements(CoordinatorAgent coordinatorAgent, MessageTemplate mt) {
	      super(coordinatorAgent, mt);
	      showMessage("Waiting for movements from lower layers.");
	    }

	    protected ACLMessage prepareResponse(ACLMessage msg) {
	      /* method called when the message has been received. If the message to send
	       * is an AGREE the behaviour will continue with the method prepareResultNotification. */
	      ACLMessage reply = msg.createReply();
	      try {
	        Object contentRebut = (Object)msg.getContent();
	        if(processMovements(contentRebut)) {
	        	showMessage("New movements from " + msg.getSender() + " received.");
	        	reply.setPerformative(ACLMessage.AGREE);
	        }
	      } catch (Exception e) {
	        e.printStackTrace();
	      }
	      //showMessage("Answer sent"); //: \n"+reply.toString());
	      return reply;
	    } //endof prepareResponse   

	    /**
	     * This method is called after the response has been sent and only when
	     * one of the following two cases arise: the response was an agree message
	     * OR no response message was sent. This default implementation return null
	     * which has the effect of sending no result notification. Programmers
	     * should override the method in case they need to react to this event.
	     * @param msg ACLMessage the received message
	     * @param response ACLMessage the previously sent response message
	     * @return ACLMessage to be sent as a result notification (i.e. one of
	     * inform, failure).
	     */
	    protected ACLMessage prepareResultNotification(ACLMessage msg, ACLMessage response) {

	      // it is important to make the createReply in order to keep the same context of
	      // the conversation
	      ACLMessage reply = msg.createReply();
	      reply.setPerformative(ACLMessage.INFORM);

	      try {
	    	  reply.setContent("ok movements");
	      } catch (Exception e) {
	        reply.setPerformative(ACLMessage.FAILURE);
	        System.err.println(e.toString());
	        e.printStackTrace();
	      }
	      //showMessage("Answer sent"); //+reply.toString());
	      return reply;

	    } //endof prepareResultNotification


	    /**
	     *  No need for any specific action to reset this behaviour
	     */
	    public void reset() {
	    }
	    
	    public int onEnd(){
	    	countMovementsReceived++;
	    	int thisC = countMovementsReceived;
	    	countMovementsReceived = countMovementsReceived%2;
	    	showMessage("onEnd ReceiveMovements " + String.valueOf(thisC));
	    	return thisC;
	    }

	  } //end of RequestResponseBehaviour

  /*************************************************************************/
  
  /**
   * STATE_4
   * Sends the movements to the CentralAgent and reeds the new map information.
   * 
   * @author Marc Bolaños
   *
   */
  class SendMovesBehaviour extends AchieveREInitiator {
	  
		private ACLMessage msgSent = null;
	    
	    public SendMovesBehaviour(Agent myAgent, ACLMessage requestMsg) {
	      super(myAgent, requestMsg);
	      //showMessage("Checking moves to send...");
	      
	      	// Requests the map again
	        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.clearAllReceiver();
			request.addReceiver(CoordinatorAgent.this.centralAgent);
			request.setProtocol(InteractionProtocol.FIPA_REQUEST);

		    try {
		    	request.setContentObject(newMovements);
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		    
		  // Reset movements list
		  newMovements = new ArrayList<Movement>(info.getNumHarvesters()+info.getNumScouts());
		  movReceivedScouts = 0;
		  movReceivedHarvesters = 0;
		    
	      msgSent = request;
	    }

	    /**
	     * Handle AGREE messages
	     * @param msg Message to handle
	     */
	    protected void handleAgree(ACLMessage msg) {
	      //showMessage("AGREE received from "+ ( (AID)msg.getSender()).getLocalName());
	    }

	    /**
	     * Handle INFORM messages
	     * @param msg Message
	     */
	    protected void handleInform(ACLMessage msg) {
	    	//showMessage("INFORM received from "+ ( (AID)msg.getSender()).getLocalName()+" ... [OK]");
	        try {
	          info = (AuxInfo)msg.getContentObject();
	          if (info instanceof AuxInfo) {
	            //showMessage("Agents initial position: ");
	            for (InfoAgent ia : info.getAgentsInitialPosition().keySet()){  
	          	  //showMessage(ia.toString());
	              Cell pos = (Cell)info.getAgentsInitialPosition().get(ia);
	              //showMessage("c: " + pos);  	
	            }
	            //showMessage("Garbage discovered: ");
	            for (int i=0; i<info.getMap().length; i++){
	            	for (int j=0; j<info.getMap()[0].length; j++){
	            		if (info.getCell(j,i).getCellType()==Cell.BUILDING)
	            			if (info.getCell(j,i).getGarbageUnits()>0) showMessage(info.getCell(j,i).toString());
	                }  
	            }
	            //showMessage("Cells with recycling centers: ");
	            //for (Cell c : info.getRecyclingCenters()) //showMessage(c.toString()); 
	                     

	          }
	        } catch (Exception e) {
	          showMessage("Incorrect content: "+e.toString());
	        }
	    }

	    /**
	     * Handle NOT-UNDERSTOOD messages
	     * @param msg Message
	     */
	    protected void handleNotUnderstood(ACLMessage msg) {
	      showMessage("This message NOT UNDERSTOOD. \n");
	    }

	    /**
	     * Handle FAILURE messages
	     * @param msg Message
	     */
	    protected void handleFailure(ACLMessage msg) {
	      showMessage("The action has failed.");

	    } //End of handleFailure

	    /**
	     * Handle REFUSE messages
	     * @param msg Message
	     */
	    protected void handleRefuse(ACLMessage msg) {
	      showMessage("Action refused.");
	    }
	    
	    public int onEnd(){
	    	return 0;
	    }
	    
	  } //Endof class SendMovesBehaviour

  
  /*************************************************************************/
  
  

} //endof class CoordinatorAgent