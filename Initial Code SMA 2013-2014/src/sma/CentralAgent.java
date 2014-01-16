package sma;

import java.lang.*;
import java.io.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;







// import jade.util.leap.ArrayList;
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
import jade.wrapper.*;
import jade.wrapper.AgentContainer;

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
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = true;

	private sma.gui.GraphicInterface gui;
	private sma.ontology.InfoGame game;
	private java.util.List<Cell> agents = null;

	private static String harvesterName = "h";
	private static String scoutName = "s";

	private AID coordinatorAgent;
	private int turnLastMap = 0;
	
	//Global variables for the final stats
	private int newDiscover = 0;
	private int totalGarbage = 0;
	private int totalUnitsGarbage = 0;
	private int haversterUnits = 0;
	private ArrayList<Integer> gCenters;
	private HashMap<AID, Integer> harvester_points = new HashMap<AID, Integer>();
	private int maxPossiblePoints=0;


	
	// stores the new discoveries found
	private ArrayList newDiscoveries = new ArrayList();
	
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);	
	
	private boolean movements_updated = false;

	
	public CentralAgent() {
		super();
	}

	/**
	 * A message is shown in the log area of the GUI
	 * @param str String to show
	 */
	private void showMessage(String str) {
		if (gui!=null) gui.showLog(str + "\n");
	}

	private java.util.List<Cell> placeAgents(InfoGame currentGame) throws Exception {
	  Cell c = null;
	  int x=0, y=0;
	  int numScouts=0, maxScouts = currentGame.getInfo().getNumScouts();
	  int numHarvesters=0, maxHarvesters = currentGame.getInfo().getNumHarvesters();
      java.util.List<Cell> agents = new java.util.ArrayList<Cell>();
      
      //Create the object to create the agents and get the main controller
      UtilsAgents utils = new UtilsAgents();
      AgentContainer container = this.getContainerController();
      Object [] parameters = null;
      String agentName = "";
      AID aid = null;
      
      ServiceDescription searchCriterion = new ServiceDescription();
      searchCriterion.setType(UtilsAgents.SCOUT_AGENT);
      showMessage("Placing agents...");
      while(numScouts<maxScouts){
    	  x = game.getInfo().getRandomPosition(20);
    	  y = game.getInfo().getRandomPosition(20);
    	  c = currentGame.getInfo().getCell(x,y);
    	  if(c.getCellType()==c.STREET && !c.isThereAnAgent()){
    		  agentName = this.scoutName+String.valueOf(numScouts);
        	  //Create the scout agent
              utils.createAgent(container, agentName, "sma.ScoutAgent",parameters);
              //Add other criteria to search aid agent
              searchCriterion.setName(agentName);
              //Search AID of the scout agent
      	      aid = UtilsAgents.searchAgent(this, searchCriterion);
      	      //Create the InfoAgent
        	  InfoAgent b = new InfoAgent(InfoAgent.SCOUT, aid);
      	      //Add agent in the map
        	  ((currentGame.getInfo().getMap())[x][y]).addAgent(b);
        	  agents.add(currentGame.getInfo().getCell(x,y));
      	      //Add the s aid into the harvester aid list
      	      this.game.getInfo().addScout_aid(aid);
    		  numScouts++;
    	  }
      }
      
	  searchCriterion = new ServiceDescription();
      searchCriterion.setType(UtilsAgents.HARVESTER_AGENT);
      while(numHarvesters<maxHarvesters){
    	  x = game.getInfo().getRandomPosition(20);
    	  y = game.getInfo().getRandomPosition(20);
    	  c = currentGame.getInfo().getCell(x,y);
    	  if(c.getCellType()==c.STREET && !c.isThereAnAgent()){
    		  agentName = this.harvesterName+String.valueOf(numHarvesters);
        	  //Create the scout agent
        	  utils.createAgent(container, agentName, "sma.HarvesterAgent",parameters);
              //Add other criteria to search aid agent
              searchCriterion.setName(agentName);
              //Search AID of the scout agent
      	      aid = UtilsAgents.searchAgent(this, searchCriterion);
      	      //Create the InfoAgent
        	  InfoAgent p = new InfoAgent(InfoAgent.HARVESTER, aid);
      	      //Add agent in the map
        	  ((currentGame.getInfo().getMap())[x][y]).addAgent(p);
        	  agents.add( currentGame.getInfo().getCell(x,y));
      	      //Add the harvester aid into the harvester aid list
      	      this.game.getInfo().addHarvester_aid(aid);
      	      harvester_points.put(aid, 0);
      	      numHarvesters++;
    	  }
      }
      showMessage(numScouts+" of the scout agents have been placed!");
      showMessage(numHarvesters+" of the harverters agents have been placed!");
      showMessage("All agents placed");
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
		//checkScoutsDiscoveries();

		// search CoordinatorAgent
		ServiceDescription searchCriterion = new ServiceDescription();
		searchCriterion.setType(UtilsAgents.COORDINATOR_AGENT);
		this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
		// searchAgent is a blocking method, so we will obtain always a correct AID

		//Obtain the max gain at each type of garbage
		gCenters = new ArrayList<Integer>();
		computeTheMaxScore();
		
		// add behaviours

		// we wait for the initialization of the game
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		showMessage("Starting InitialRequestResponseBehaviour");
		this.addBehaviour(new InitialRequestResponseBehaviour());


		// Setup finished. When the last inform is received, the agent itself will add
		// a behaviour to send/receive actions
		showMessage("MainLoopBehaviour added");
		this.addBehaviour(new MainLoopBehaviour(this, game.getTimeout()));
		
		showMessage("Setup finished\n");

	} //endof setup



	/**
	 * Manages the input moves updating the map with respect to the information
	 * provided.
	 * 
	 * @param moves Object containing 
	 */
	private boolean processMovements(Object cells){
		showMessage("Checking received movements");
		
		ArrayList<Cell> mo_list = (ArrayList)cells;
		ArrayList<Integer> row_cell_processed = new ArrayList<Integer>();
		ArrayList<Integer> col_cell_processed = new ArrayList<Integer>();
		ArrayList<Cell> newAgentPos = new ArrayList<Cell>();
		ArrayList<Cell> oldAgentPos = new ArrayList<Cell>();
		ArrayList<Cell> cell_processed = new ArrayList<Cell>();
		ArrayList<InfoAgent> old = new ArrayList<InfoAgent>();
		ArrayList<Cell> original_cell = new ArrayList<Cell>();
		 
		ArrayList<Integer> addPositions = new ArrayList<Integer>(); // order in which the agents must be added to the map
		ArrayList<Integer> movingPos = new ArrayList<Integer>(); // indices of the agents that are moving
		ArrayList<Integer> standPos = new ArrayList<Integer>(); // indices of the agents that are standing still
		
		ArrayList<ArrayList> checked = new ArrayList<ArrayList>();// crossed positions checked for crashes
		
		int k = 0;
		boolean mov_processed = false;
		
		for(Cell c : mo_list){
			if(!c.isThereAnAgent()){ continue;}
	
			String agent_id = c.getAgent().getAID().getName();
			AID agent_aid = c.getAgent().getAID();
			
			int pos_col = c.getColumn();
			int pos_row = c.getRow();
			// find the current Cell of the agent
			Cell[][] map = game.getInfo().getMap();
			//(int i = 0; i < map.length; i++){
			int i = 0; int j = 0;boolean found = false;
			Cell tmp = game.getInfo().getAgentCell(agent_aid);
			if(tmp != null){
				oldAgentPos.add(tmp);
				found = true;
			}
			/*
			while( i < map.length && !found){
				j = 0;
				while(j < map[i].length && !found){
					try{
						if(map[i][j].getAgent().getAID().getName().equals(agent_id)){
							found = true;
							oldAgentPos.add(map[i][j]);
						}
					} catch(Exception e3){ }
					j++;
				}
				i++;
			}
			*/
			if(found){
				original_cell.add(game.getInfo().getAgentCell(agent_aid));
				//int addPos;
				// We only check if this is any agent that does not want to move
				/*if(oldAgentPos.get(k).getRow() == pos_row && oldAgentPos.get(k).getColumn() == pos_col){
					addPos = 0;
					standPos.add(k);
				} else {
					addPos = k;
					movingPos.add(k);
				}*/
				movingPos.add(k);
				old.add(oldAgentPos.get(k).getAgent());
	
			
				// set the InfoAgent to null
				try {
					System.err.println(oldAgentPos.get(k));
					oldAgentPos.get(k).removeAgent(old.get(k));
				} catch (Exception e1) {
					
					e1.printStackTrace();
				}
				// set the InfoAgent of the new Cell
				i = 0; j = 0; found = false;
				while( i < map.length && !found){
					j = 0;
					while(j < map[i].length && !found){
						try{
							if(map[i][j].getRow()==pos_row && map[i][j].getColumn()==pos_col){
								found = true;
								newAgentPos.add(map[i][j]);
								k++;
							}
						}catch(Exception e){}
						j++;
					}
					i++;
				}
				
			} else{
				System.err.println("Agent not found");
			}
	
		}		
		
		//addPositions.addAll(standPos);
		addPositions.addAll(movingPos);
		
		// Check exchanging positions (crashes)
		for(int i = 0; i < oldAgentPos.size(); i++){
			for(int j = 0; j < oldAgentPos.size(); j++){
				boolean trobat = false; int lenChecked = 0; 
				while(!trobat && lenChecked < checked.size()){
					ArrayList pair = checked.get(lenChecked);
					if(((int)pair.get(0) == i && (int)pair.get(1) == j) || ((int)pair.get(0) == j && (int)pair.get(1) == i)){
						trobat = true; // we have already checked this pair, we do not have to repeat it
					}
					lenChecked++;
				}
				if(j != i && !trobat){ // they are not the same agent
					
					AID aid_j = old.get(addPositions.get(j)).getAID();
					AID aid_i = old.get(addPositions.get(i)).getAID();
					Cell old_pos_j = oldAgentPos.get(addPositions.get(j));
					Cell old_pos_i = oldAgentPos.get(addPositions.get(i));
					Cell new_pos_j = newAgentPos.get(j);
					Cell new_pos_i = newAgentPos.get(i);
					
					// if old_pos_j == new_pos i
					// and old_pos i == new_pos j
					// CRASH!!!
					if(old_pos_j.getRow() == new_pos_i.getRow() && old_pos_j.getColumn() == new_pos_i.getColumn() &&
							old_pos_i.getRow() == new_pos_j.getRow() && old_pos_i.getColumn() == new_pos_j.getColumn()){
						// We correct to their old positions
						int old_i_r = old_pos_i.getRow();
						int old_i_c = old_pos_i.getColumn();
						int old_j_r = old_pos_j.getRow();
						int old_j_c = old_pos_j.getColumn();
						newAgentPos.get(i).setRow(old_i_r);
						newAgentPos.get(i).setColumn(old_i_c);
						newAgentPos.get(j).setRow(old_j_r);
						newAgentPos.get(j).setColumn(old_j_c);
						System.err.println("Crash between " + aid_j.getLocalName() + " and " + aid_i.getLocalName() +"!");
						ArrayList pair = new ArrayList(); pair.add(i); pair.add(j);
						checked.add(pair);
					}
					
				}
			}
		}
		
		/* Up to this point, all the cells don't have any InfoAgent inside. In the following loop, we are gonna put the InfoAgents into new positions */
		for (int i=0; i<k; i++)
		{
			mov_processed = false;
			for (Cell c: cell_processed)
			{
				if ( (c.getRow() == newAgentPos.get(i).getRow()) && (c.getColumn() == newAgentPos.get(i).getColumn()) )
					mov_processed = true;				
			}
			
			if( !mov_processed ){
				try {
					
					newAgentPos.get(i).addAgent(old.get(addPositions.get(i)));
					game.getInfo().setAgentCell(old.get(addPositions.get(i)), newAgentPos.get(i));

					
				} catch (Exception e) {
					
					try {
						oldAgentPos.get(addPositions.get(i)).removeAgent(old.get(addPositions.get(i)).getAID());
						oldAgentPos.get(addPositions.get(i)).addAgent(old.get(addPositions.get(i)));
						game.getInfo().setAgentCell(old.get(addPositions.get(i)), oldAgentPos.get(addPositions.get(i)));
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				cell_processed.add(newAgentPos.get(i));
				
			/* If we have already put a InfoAgent in this cell in a previous movement already processed, then the Agent that wants to move to this cell is not allowed. */
			} else{ 
				System.err.println("There is an agent in this cell");
				try {
					// We do not move it from its old position
					oldAgentPos.get(addPositions.get(i)).removeAgent(old.get(addPositions.get(i)).getAID());
					oldAgentPos.get(addPositions.get(i)).addAgent(old.get(addPositions.get(i)));
					game.getInfo().setAgentCell(old.get(addPositions.get(i)), oldAgentPos.get(addPositions.get(i)));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		showMessage("Movements have been processed.");
		movements_updated = true;
		return true;
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
	 * @author David Isern and Joan Albert L������������������������������������������������������������������������������������������������������������������������������������������������������������������pez
	 * @see sma.ontology.Cell
	 * @see sma.ontology.InfoGame
	 */
	private class InitialRequestResponseBehaviour extends OneShotBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			
			
			boolean okInit = false;
			ACLMessage reply = null;
			while(!okInit){
				ACLMessage msg = messagesQueue.getMessage();
				try {
					Object contentRebut = (Object)msg.getContent();
					if(contentRebut.equals("Initial request")) {
						reply = msg.createReply();
						showMessage("Initial request received");
						reply.setPerformative(ACLMessage.AGREE);
						send(reply);
						okInit = true;
					} else {
			        	messagesQueue.add(msg);
			        }
				}catch (Exception e){
					messagesQueue.add(msg);
				}
			}
			
	        messagesQueue.endRetrieval();
			
			if( reply != null){
				reply.setPerformative(ACLMessage.INFORM);

				try {
					reply.setContentObject(game.getInfo());
					
				} catch (Exception e) {
					reply.setPerformative(ACLMessage.FAILURE);
					System.err.println(e.toString());
					e.printStackTrace();
				}
				send(reply);
			} else{
				showMessage("No Initial request message found!!");
			}
			
		}
		
		
		
	}


	/**
	 * Reads the movements, applies them and sends an agree reply
	 * @author Alex Pardo Fernandez
	 *
	 */
	private class RequestResponseBehaviour extends OneShotBehaviour{
		
		public RequestResponseBehaviour(CentralAgent myAgent) {
			super(myAgent);
			showMessage("Waiting REQUESTs from authorized agents");
		}
		@Override
		public void action() {

			
			boolean okRR = false;
			ACLMessage reply = null;
			showMessage("Waiting REQUESTs from authorized agents");
			while(!okRR){
				ACLMessage msg = messagesQueue.getMessage();
				
				try {
//<<<<<<< HEAD
					Object contentRebut = (Object)msg.getContent();

					if(msg.getContentObject() instanceof ArrayList<?> && processMovements(msg.getContentObject())) {
						turnLastMap = game.getTurn();
						showMessage("Movements applied.");
						reply = msg.createReply();
						reply.setPerformative(ACLMessage.AGREE);
						okRR = true;
//=======
//					Object contentRebut = (Object)msg.getContentObject();
//					if(contentRebut instanceof ArrayList<?> && msg.getSender().getLocalName().equals()){
//						boolean result = processMovements(msg.getContentObject());
//						if(result) {
//							turnLastMap = game.getTurn();
//							showMessage("Movements applied.");
//							reply = msg.createReply();
//							reply.setPerformative(ACLMessage.AGREE);
//							okRR = true;
//						}
//>>>>>>> origin/debug
					} else {
			        	messagesQueue.add(msg);
			        }
				}catch (Exception e){
					e.printStackTrace();
					messagesQueue.add(msg);
				}
			}
			
	        messagesQueue.endRetrieval();
	        
			if( reply != null){
				send(reply);
			}else{
				showMessage("No movements message found!!");
			}
			
			
		}
	

		
	}
//		/**
//		 * Constructor for the <code>RequestResponseBehaviour</code> class.
//		 * @param myAgent The agent owning this behaviour
//		 * @param mt Template to receive future responses in this conversation
//		 */
//		public RequestResponseBehaviour(CentralAgent myAgent, MessageTemplate mt) {
//			super(myAgent, mt);
//			showMessage("Waiting REQUESTs from authorized agents");
//		}
//
//		protected ACLMessage prepareResponse(ACLMessage msg) {
//			/* method called when the message has been received. If the message to send
//			 * is an AGREE the behaviour will continue with the method prepareResultNotification. */
//			
//			showMessage("Message received from" + msg.getSender().getName());
//			ACLMessage reply = msg.createReply();
//			try {
//				
//				Object contentRebut = (Object)msg.getContent();
//				if(processMovements(msg.getContentObject())) {
//					turnLastMap = game.getTurn();
//					showMessage("Movements applied.");
//					reply.setPerformative(ACLMessage.AGREE);
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			//showMessage("Answer sent"); //: \n"+reply.toString());
//			return reply;
//		} //endof prepareResponse   
//
//		/**
//		 * This method is called after the response has been sent and only when
//		 * one of the following two cases arise: the response was an agree message
//		 * OR no response message was sent. This default implementation return null
//		 * which has the effect of sending no result notification. Programmers
//		 * should override the method in case they need to react to this event.
//		 * @param msg ACLMessage the received message
//		 * @param response ACLMessage the previously sent response message
//		 * @return ACLMessage to be sent as a result notification (i.e. one of
//		 * inform, failure).
//		 */
//		@Override
//		protected ACLMessage prepareResultNotification(ACLMessage request,
//				ACLMessage response) throws FailureException {
//			// TODO Auto-generated method stub
//			return super.prepareResultNotification(request, response);
//		}
//		
////		protected ACLMessage prepareResultNotification(ACLMessage msg, ACLMessage response) {
////
////			// it is important to make the createReply in order to keep the same context of
////			// the conversation
////			ACLMessage reply = msg.createReply();
////			reply.setPerformative(ACLMessage.INFORM);
////
////			try {
////				reply.setContentObject(game.getInfo());
////			} catch (Exception e) {
////				reply.setPerformative(ACLMessage.FAILURE);
////				System.err.println(e.toString());
////				e.printStackTrace();
////			}
////			//showMessage("Answer sent"); //+reply.toString());
////			return reply;
////
////		} //endof prepareResultNotification
//
//
//		/**
//		 *  No need for any specific action to reset this behaviour
//		 */
//		public void reset() {
//		}
//
//		
//
//	} //end of RequestResponseBehaviour


	private class SendInfoBehaviour extends OneShotBehaviour{

		@Override
		public void action() {
			showMessage("Sending Game Info to coord.");
			ACLMessage info = new ACLMessage(ACLMessage.INFORM);
			info.clearAllReceiver();
			info.addReceiver(CentralAgent.this.coordinatorAgent);
			info.setProtocol(InteractionProtocol.FIPA_REQUEST);

		    try {
		    	info.setContentObject(game.getInfo());
		    	send(info);
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		
		    
			
		}

		
	}
	
	/**
	 * Reads the movements, applies them and sends an agree reply
	 * @author Alex Pardo Fernandez
	 *
	 */
	private class UpdateGarbageHarvester extends OneShotBehaviour{
		
		public UpdateGarbageHarvester(CentralAgent myAgent) {
			super(myAgent);
			
		}
		@Override
		public void action() {

			showMessage("Reading updates from Harvesters.");
			
			boolean okRR = false;
			ACLMessage reply = null;
			int counter = 0;
			while(!okRR && counter < messagesQueue.size()){
				ACLMessage msg = messagesQueue.getMessage();
				counter++;
				
				try {
					Object contentRebut = (Object)msg.getContentObject();
					
					if(msg.getSender().getLocalName().startsWith(harvesterName)){
						AID senderName = msg.getSender();
						showMessage("Message from " + msg.getSender().getLocalName());
						Cell c = game.getInfo().getAgentCell(senderName);
							
						Cell b = (Cell) contentRebut;
						int tmp;
						switch(b.getCellType()){
						case Cell.BUILDING:
							showMessage(Integer.toString(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getCurrentType()));
							if(c.getAgent().getCurrentTypeChar() == b.getGarbageType() || c.getAgent().getCurrentType() == -1){
								if(game.getInfo().getCell(b.getRow(), b.getColumn()).getGarbageUnits() > 0){
									// set harvester garbage type
									game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setCurrentType(b.getGarbageType());
									// increase the counter for the harvester
									tmp = game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits();
									game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setUnits(tmp + 1);
									// decrease the counter for the building
									tmp = game.getInfo().getCell(b.getRow(), b.getColumn()).getGarbageUnits();
									game.getInfo().getCell(b.getRow(), b.getColumn()).setGarbageUnits(tmp - 1);
								}
							} else{
								System.err.println("Harvester " + senderName + " performing wrong garbage operation");
							}
							
							reply = msg.createReply();
							reply.setPerformative(ACLMessage.AGREE);
							
							break;
							
						case Cell.RECYCLING_CENTER:
							if(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits() > 0){
								// decrease the counter for the harvester
								tmp = game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits();
								game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setUnits(tmp - 1);
								String garbage = String.valueOf(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getCurrentTypeChar());
								if(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits() == 0){
								game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setCurrentType(-1);
								}
								// add the points
								tmp = harvester_points.remove(senderName);
								harvester_points.put(senderName, tmp+b.getGarbagePoints(garbage));
								haversterUnits++;
								
								showMessage(senderName+" is recycling 1 unit of the garbage type "+garbage);
								showMessage(senderName+" obtains:"+tmp+b.getGarbagePoints(garbage) +" points by recycling the garbage");
								reply = msg.createReply();
								reply.setPerformative(ACLMessage.AGREE);		
							}
							break;
							
						default: 
								break;
						}
						
					
				
					} else {
			        	messagesQueue.add(msg);
			        }
				}catch (Exception e){
					e.printStackTrace();
					messagesQueue.add(msg);
				}
			}
			
	        messagesQueue.endRetrieval();
	        
			if( reply != null){
				send(reply);
			}else{
				showMessage("No garbage interaction found.");
			}
			
			
		}
	

		
	}

	/*************************************************************************/

	/**
	 * Performs the main loop of the application checking the moves of the agents and
	 * updating the GUI until we reach the number of turns for this game.
	 * 
	 * @author Marc Bola��os
	 *
	 */
	private class MainLoopBehaviour extends TickerBehaviour {

		public MainLoopBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {		

			boolean game_finished = false;

			// Update Turn counter
			try {
			    Thread.sleep(400);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			game.incrTurn();
			int turn = game.getTurn();
			if(turn <= game.getGameDuration()){
				showMessage("\n\nTurn " + String.valueOf(turn));
				// Finishing game
			} else {
				try {
					game_finished = true;
					//Show the stats of the execution
					showStats();
					this.stop();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			if(!game_finished){
				// send game info
				if(movements_updated){
					this.myAgent.addBehaviour(new SendInfoBehaviour());
					movements_updated = false;
				} 
				//get a random number to decide whether to add or not garbage
				double d = Math.random();
				if(d <= game.getProbGarbage()){
					try {
						showMessage("Adding new garbage...");
						addGarbage();

					} catch (Exception e) {
						// do nothing
					}
				}
				// wait for the new garbage discoveries 
				this.myAgent.addBehaviour(new ReceiveNewDiscoveries((CentralAgent) this.myAgent));
				
				// send only the really new garbage discoveries to the CoordinatorAgent
				this.myAgent.addBehaviour(new SendNewDiscoveries((CentralAgent) this.myAgent));
				
				// wait for the new positions 
				this.myAgent.addBehaviour(new RequestResponseBehaviour((CentralAgent) this.myAgent));
				
				// look for garbage messages from harvesters
				this.myAgent.addBehaviour(new UpdateGarbageHarvester((CentralAgent) this.myAgent));
				
			} else {

				// TODO: show final result

			}

		}
		
		/*************************************************************************/
		
		 /**
		   * 
		   * Waiting for new discoveries from the CoordinatorAgent
		   * 
		   * @author Marc Bola��os
		   *
		   */
			protected class ReceiveNewDiscoveries extends SimpleBehaviour {
				
				
				public ReceiveNewDiscoveries (Agent a)
				{
					super(a);
				}

				@Override
				public void action() {
					
					showMessage("Waiting for new discoveries from CoordinatorAgent.");

					boolean okDisc = false;
					while(!okDisc){
						ACLMessage msg = messagesQueue.getMessage();
						
						try {
							ArrayList contentRebut = (ArrayList)msg.getContentObject();
					        if(msg.getSender().getLocalName().equals(coordinatorAgent.getLocalName())) {
					        	newDiscoveries = checkIfNewDiscoveries(contentRebut);
					        	okDisc = true;
					        	showMessage("New discoveries from " + msg.getSender().getLocalName() + " received.");
					        } else {
					        	messagesQueue.add(msg);
					        }
						}catch (Exception e){
							System.err.println("The message received is from "+ msg.getSender().getLocalName());
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
					return 0;
			    }
			}

			/*************************************************************************/
			
			/**
			   * Sends the new discoveries to the CoordinatorAgent.
			   * 
			   * @author Marc Bola��os
			   *
			   */
				protected class SendNewDiscoveries extends SimpleBehaviour {
					
					public SendNewDiscoveries(Agent a)
					{
						super(a);
					}

					@Override
					public void action() {
						
						showMessage("Sending really new discoveries to coord agent\n");
						
						// Requests the map again sending the list of movements
				        ACLMessage discoveriesMsg = new ACLMessage(ACLMessage.INFORM);
				        discoveriesMsg.clearAllReceiver();
				        discoveriesMsg.addReceiver(coordinatorAgent);
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
				    	return 0;
				    }
				}

				
				
				/**
				 * Reads the movements, applies them and sends an agree reply
				 * @author Alex Pardo Fernandez
				 *
				 */
				private class UpdateGarbageHarvester extends OneShotBehaviour{
					
					public UpdateGarbageHarvester(CentralAgent myAgent) {
						super(myAgent);
						
					}
					@Override
					public void action() {

						showMessage("Reading updates garbafe from Harvesters.");
						
						boolean okRR = false;
						ACLMessage reply = null;
						int counter = 0;
						while(!okRR && counter < messagesQueue.size()){
							ACLMessage msg = messagesQueue.getMessage();
							counter++;
							
							try {
								Object contentRebut = (Object)msg.getContentObject();
								
								if(msg.getSender().getLocalName().startsWith(harvesterName)){
									AID senderName = msg.getSender();
									showMessage("Message from " + msg.getSender().getLocalName());
									Cell c = game.getInfo().getAgentCell(senderName);
										
									Cell b = (Cell) contentRebut;
									int tmp;
									switch(b.getCellType()){
									case Cell.BUILDING:
										showMessage(Integer.toString(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getCurrentType()));
										if(c.getAgent().getCurrentTypeChar() == b.getGarbageType() || c.getAgent().getCurrentType() == -1){
											if(game.getInfo().getCell(b.getRow(), b.getColumn()).getGarbageUnits() > 0){
												// set harvester garbage type
												game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setCurrentType(b.getGarbageType());
												// increase the counter for the harvester
												tmp = game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits();
												game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setUnits(tmp + 1);
												// decrease the counter for the building
												tmp = game.getInfo().getCell(b.getRow(), b.getColumn()).getGarbageUnits();
												game.getInfo().getCell(b.getRow(), b.getColumn()).setGarbageUnits(tmp - 1);
											}
										} else{
											System.err.println("Harvester " + senderName + " performing wrong garbage operation");
										}
										
										reply = msg.createReply();
										reply.setPerformative(ACLMessage.AGREE);
										
										break;
										
									case Cell.RECYCLING_CENTER:
										if(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits() > 0){
											// decrease the counter for the harvester
											tmp = game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits();
											game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setUnits(tmp - 1);
											String garbage = String.valueOf(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getCurrentTypeChar());
											if(game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().getUnits() == 0){
											game.getInfo().getCell(c.getRow(), c.getColumn()).getAgent().setCurrentType(-1);
											}
											// add the points
											tmp = harvester_points.remove(senderName);
											harvester_points.put(senderName, tmp+b.getGarbagePoints(garbage));
											haversterUnits++;
											
											showMessage(senderName+" is recycling 1 unit of the garbage type "+garbage);
											showMessage(senderName+" obtains:"+tmp+b.getGarbagePoints(garbage) +" points by recycling the garbage");
											
											reply = msg.createReply();
											reply.setPerformative(ACLMessage.AGREE);		
										}
										break;
										
									default: 
											break;
									}
									
								
							
								} else {
						        	messagesQueue.add(msg);
						        }
							}catch (Exception e){
								e.printStackTrace();
								messagesQueue.add(msg);
							}
						}
						
				        messagesQueue.endRetrieval();
				        
						if( reply != null){
							send(reply);
						}else{
							showMessage("No garbage interaction found.");
						}
						
						
					}
				

					
				}
			  
			/*************************************************************************/
		
		
		
		/**
		 * Adds garbage to the buildings in the map without garbage
		 * @throws Exception
		 */
		private void addGarbage() throws Exception{
			Random r = new Random();
			java.util.List<Cell> all_buildings = game.getAllBuildings();
			int index = 0;

			do{
				index = (int) (Math.random() * all_buildings.size());	

			} while(all_buildings.get(index).getGarbageUnits() != 0);

			Cell b = all_buildings.get(index);
			char type;
			int units;
			String[] types = {"G","P", "M", "A"};

			int v_type = (int) (Math.random() * 3);
			type = types[v_type].charAt(0); 


			units = (int) Math.abs(((r.nextGaussian()*5)+2));
			if (units == 0){
				units = 1;
			}

			b.setGarbageType(type);
			b.setGarbageUnits(units);
			
			//Save the units of the garbage and increment the number of the garbage buildings
			totalUnitsGarbage = totalUnitsGarbage + units;
			totalGarbage++;
			
			//Save the possible max points and the units of haverster
			int gain=1;
			switch(type){
				case 'G':
					gain = gCenters.get(0);
					break;
				case 'M':
					gain = gCenters.get(1);
					break;
				case 'P':
					gain = gCenters.get(2);
					break;
				case 'A':
					gain = gCenters.get(3);
					break;
			}
			
			//int gain =gCenters.get(type);
			maxPossiblePoints = maxPossiblePoints + gain*units;
			
			// set garbage into the map inside auxInfo
			Cell[][] map = game.getInfo().getMap();
			map[b.getRow()][b.getColumn()] = b;
			game.getInfo().setMap(map);
			
			// set garbage into the list of buildings with garbage
			java.util.List<Cell> tmp = game.getBuildingsGarbage();
			tmp.add(b);
			game.setBuildingsGarbage(tmp);
		}
		
		/**
		 * Checks for each received garbage position if it is really new or it has been found before.
		 * 
		 * @param disc ArrayList with all the garbage positions found.
		 * @return ArrayList with all the really new garbages.
		 */
		private ArrayList checkIfNewDiscoveries(ArrayList disc) throws Exception {
			
			try {
				ArrayList trueDisc = new ArrayList();
				ArrayList<Integer> toDelete = new ArrayList<Integer>();
				java.util.List<Cell> list = game.getBuildingsGarbage();
				// we go through all the garbages that have not been detected yet
				for(int count = 0; count < list.size(); count++){
					Cell g = list.get(count);
					boolean found = false;
					int i = 0;
					// and compare them with all the discovered garbages by the scouts
					while(!found && i < disc.size()){
						ArrayList g2 = (ArrayList)disc.get(i);
						if((int)g2.get(0) == g.getRow() && (int)g2.get(1) == g.getColumn()){
							found = true;
						} else {
							i++;
						}
					}
					// if we have found a true new position, then we add it to the 
					// list of true new discoveries (trueDisc) and delete it from
					// the rest of the lists.
					if(found){
						trueDisc.add((ArrayList)disc.get(i));
						newDiscover++;
						disc.remove(i);
						toDelete.add(count);
					}
				}
				
				// Deletes all the found garbages from the list in "game".
				for(int i = toDelete.size()-1; i >= 0; i--){
					list.remove((int)toDelete.get(i));
				}
				game.setBuildingsGarbage(list);
				
				return trueDisc;
			} catch (Exception e){
				throw new Exception("This ArrayList is not a list of discoveries!!!");
			}
		}

	}
	
	/**
	 * Method to compute the max score possible at each type of garbage.
	 */
	private void computeTheMaxScore(){
		int maxValue=0, newValue=0;
		String[] types = {"G","M","P","A"};
		java.util.List<Cell> centers = game.getInfo().getRecyclingCenters();
		for(String garbage : types){
			maxValue = 0;
			for(Cell c: centers){
				try {
					//Get the garbage point of one spefic type of garbage
					newValue = c.getGarbagePoints(garbage);
					if(newValue > maxValue){
						maxValue = newValue;
					}
				} catch (Exception e) {
					showMessage(e.getMessage());
				}
			}
			//Save in the hashmap as key the type of the garbage and the max benefist of this type.
			gCenters.add(maxValue);
		}		
	}
	
	private void showStats(){
		int obtainedPoints = 0;
		showMessage("\n\n-----------Final stats------------");
		Collection<Integer> points = harvester_points.values();
		for(Integer point : points){
			obtainedPoints = obtainedPoints + point;
		}
		double stat1, stat2, stat3;
		
		stat1 = ((double)obtainedPoints/(double)maxPossiblePoints)*100;
		showMessage("Percentage of the number of points obtained over the maximum number of points for this map:"+new DecimalFormat("###.##").format(stat1)+"%");
		stat2 = ((double)haversterUnits/(double)totalUnitsGarbage)*100;
		showMessage("Percentage of units of garbage harvested:"+new DecimalFormat("###.##").format(stat2)+"%");
		stat3 = ((double)newDiscover/(double)totalGarbage)*100;
		showMessage("Percentage of buildings with garbage discovered: "+new DecimalFormat("###.##").format(stat3)+"%");
		showMessage("---------------------------------------");
	}

} //endof class AgentCentral