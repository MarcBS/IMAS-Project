package sma;

import java.lang.*;
import java.io.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import jade.util.leap.ArrayList;
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

  private AID centralAgent;

 
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
      showMessage("Content OK" + requestInicial.getContent());
    } catch (Exception e) {
      e.printStackTrace();
    }

    //we add a behaviour that sends the message and waits for an answer
    this.addBehaviour(new RequesterBehaviour(this, requestInicial));

    // setup finished. When we receive the last inform, the agent itself will add
    // a behaviour to send/receive actions
    
   

  } //endof setup




  /*************************************************************************/

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
                     
            //@todo Add a new behaviour which initiates the turns of the game 
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
  } //Endof class RequesterBehaviour


  /*************************************************************************/


} //endof class CoordinatorAgent