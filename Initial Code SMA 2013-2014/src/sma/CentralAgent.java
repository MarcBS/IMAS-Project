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
 * <p><b>Copyright:</b> Copyright (c) 2011</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author not attributable
 * @version 2.0
 */
public class CentralAgent extends Agent {

  private sma.gui.GraphicInterface gui;
  private sma.ontology.InfoGame game;
  private java.util.List<Cell> agents = null;

  private AID coordinatorAgent;

  public CentralAgent() {
    super();
  }

  /**
   * A message is shown in the log area of the GUI
   * @param str String to show
   */
  private void showMessage(String str) {
    if (gui!=null) gui.showLog(str + "\n");
    System.out.println(getLocalName() + ": " + str);
  }

  private java.util.List<Cell> placeAgents(InfoGame currentGame) throws Exception {
	  //Temporal version. Agents must be randomly placed
      java.util.List<Cell> agents = new java.util.ArrayList<Cell>();
      for(int k=0; k<currentGame.getInfo().getNumScouts(); k++) {
        InfoAgent b = new InfoAgent(InfoAgent.SCOUT);
        ((currentGame.getInfo().getMap())[9+k][9]).addAgent(b);
        agents.add(currentGame.getInfo().getCell(9+k,9));
      }

      for(int k=0; k<currentGame.getInfo().getNumHarvesters(); k++) {
    	InfoAgent p = new InfoAgent(InfoAgent.HARVESTER);
        ((currentGame.getInfo().getMap())[k+2][0]).addAgent(p);
        agents.add( currentGame.getInfo().getCell(k+2,0));
      }
      return agents;
    }
  
  /**
   * Agent setup method - called when it first come on-line. Configuration
   * of language to use, ontology and initialization of behaviours.
   */
  protected void setup() {

    /**** Very Important Line (VIL) *********/
    this.setEnabledO2ACommunication(true, 1);
    /****************************************/

    // showMessage("Agent (" + getLocalName() + ") .... [OK]");

    // Register the agent to the DF
    ServiceDescription sd1 = new ServiceDescription();
    sd1.setType(UtilsAgents.CENTRAL_AGENT);
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
      System.err.println(getLocalName() + " registration with DF unsucceeded. Reason: " + e.getMessage());
      doDelete();
    }

   /**************************************************/

    try {
      this.game = new InfoGame(); //object with the game data
      this.game.readGameFile("game.txt");
      //game.writeGameResult("result.txt", game.getMap());
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println("Game NOT loaded ... [KO]");
    }
    try {
      this.gui = new GraphicInterface(game);
      gui.setVisible(true);
      showMessage("Game loaded ... [OK]");
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    showMessage("Buildings with garbage");
    for (Cell c : game.getBuildingsGarbage()) showMessage(c.toString());
    
    
   /****Agents are randomly placed****************/
   try{
	   agents = placeAgents(this.game);
   }catch(Exception e){}
   
   /**************************************************/
   this.game.getInfo().fillAgentsInitialPositions(agents);

   //If any scout is near a building with garbage, we show it in the public map
   checkScoutsDiscoveries();
        
   // search CoordinatorAgent
   ServiceDescription searchCriterion = new ServiceDescription();
   searchCriterion.setType(UtilsAgents.COORDINATOR_AGENT);
   this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
   // searchAgent is a blocking method, so we will obtain always a correct AID

   // add behaviours

   // we wait for the initialization of the game
    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
    
   this.addBehaviour(new RequestResponseBehaviour(this, null));
   this.addBehaviour(new MainLoopBehaviour(this, game.getTimeout()));
   

   // Setup finished. When the last inform is received, the agent itself will add
   // a behavious to send/receive actions

  } //endof setup

  
  private void checkScoutsDiscoveries(){
	  showMessage("Checking New Discoveries...");
	  java.util.List<Cell> remove = new LinkedList<Cell>();
	   for (Cell a : agents){
		    if (a.getAgent().getAgentType()==InfoAgent.SCOUT){
		    	for (Cell b : game.getBuildingsGarbage()){
		    		int x=a.getRow(); int y=a.getColumn();
		    		int xb=b.getRow(); int yb=b.getColumn();
		    		if (x>0){
		    			if ((xb==x-1) && (yb==y)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
			    		if ((y>0) && (xb==x-1) && (yb==y-1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);} 
			    		if ((y<game.getInfo().getMap()[x].length-1) && (xb==x-1) && (yb==y+1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
			    	}
			    	if (x<game.getInfo().getMap().length-1){
			    		if ((xb==x+1) && (yb==y)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
			    		if ((y>0) && (xb==x+1) && (yb==y-1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
			    		if ((y<game.getInfo().getMap()[x].length-1) && (xb==x+1) && (yb==y+1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
			    	}
			    	if ((y>0) && (xb==x) && (yb==y-1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
					if ((y<game.getInfo().getMap()[x].length-1) && (xb==x) && (yb==y+1)) {game.getInfo().setCell(xb, yb, b); remove.add(b);}
		    	}
		    }	   
	   }
	   for (Cell r : remove)  game.getBuildingsGarbage().remove(r);
  }
  
  /*************************************************************************/

  /**
   * <p><B>Title:</b> IA2-SMA</p>
   * <p><b>Description:</b> Practical exercise 2011-12. Recycle swarm.</p>
   * Class that receives the REQUESTs from any agent. Concretely it is used 
   * at the beginning of the game. The Coordinator Agent sends a REQUEST for all
   * the information of the game and the CentralAgent sends an AGREE and then
   * it sends all the required information.
   * <p><b>Copyright:</b> Copyright (c) 2009</p>
   * <p><b>Company:</b> Universitat Rovira i Virgili (<a
   * href="http://www.urv.cat">URV</a>)</p>
   * @author David Isern and Joan Albert López
   * @see sma.ontology.Cell
   * @see sma.ontology.InfoGame
   */
  private class RequestResponseBehaviour extends AchieveREResponder {

    /**
     * Constructor for the <code>RequestResponseBehaviour</code> class.
     * @param myAgent The agent owning this behaviour
     * @param mt Template to receive future responses in this conversation
     */
    public RequestResponseBehaviour(CentralAgent myAgent, MessageTemplate mt) {
      super(myAgent, mt);
      showMessage("Waiting REQUESTs from authorized agents");
    }

    protected ACLMessage prepareResponse(ACLMessage msg) {
      /* method called when the message has been received. If the message to send
       * is an AGREE the behaviour will continue with the method prepareResultNotification. */
      ACLMessage reply = msg.createReply();
      try {
        Object contentRebut = (Object)msg.getContent();
        if(contentRebut.equals("Initial request")) {
          showMessage("Initial request received");
          reply.setPerformative(ACLMessage.AGREE);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      showMessage("Answer sent"); //: \n"+reply.toString());
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
        reply.setContentObject(game.getInfo());
      } catch (Exception e) {
        reply.setPerformative(ACLMessage.FAILURE);
        System.err.println(e.toString());
        e.printStackTrace();
      }
      showMessage("Answer sent"); //+reply.toString());
      return reply;

    } //endof prepareResultNotification


    /**
     *  No need for any specific action to reset this behaviour
     */
    public void reset() {
    }

  } //end of RequestResponseBehaviour


  /*************************************************************************/
  
  /**
   * Performs the main loop of the application checking the moves of the agents and
   * updating the GUI until we reach the number of turns for this game.
   * 
   * @author Marc Bolaños
   *
   */
  private class MainLoopBehaviour extends TickerBehaviour {

	public MainLoopBehaviour(Agent a, long period) {
		super(a, period);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onTick() {		
		
		boolean game_finished = false;
		
		// Update Turn counter
		game.incrTurn();
		int turn = game.getTurn();
		if(turn <= game.getGameDuration()){
			showMessage(String.valueOf(turn));
		// Finishing game
		} else {
			try {
				game_finished = true;
				this.stop();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		if(!game_finished){
			
			// Updates garbage discoveries
			checkScoutsDiscoveries();
			
			// TODO get and show agents' movements
			
		} else {
			
			// TODO show final results
			
		}
		
	}

  }

} //endof class AgentCentral
