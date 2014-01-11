package sma;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Random;

import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.Path.Step;

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
import sma.ontology.AStar;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.DelimitingZone;
import sma.ontology.InfoAgent;
import sma.ontology.InfoGame;

public class ScoutAgent extends Agent {
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = false;
	private boolean debuggingPatrolingPath = false;
	
	private AID scoutCoordinatorAgent;
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);
	private AuxInfo auxInfo;
	private DelimitingZone patrolZone; // object describing the zone where the scout must patrol
	
	//Patrol path parameters
	private ArrayList<Boolean> visitedBuildings;
	private ArrayList<Point> buildings;
	private int noVisited;
	private ArrayList<Point> patrollPath;
	private int patrollPoint = 0;
	private boolean sign=true;
	private Point objectivePoint;
		
	AStar astar;

	public ScoutAgent(){
		 super();
	}
	/**
	   * A message is shown in the log area of the GUI 
	   * @param str String to show
	   */
	private void showMessage(String str) {
		if(debugging)
			System.out.println(getLocalName() + ": " + str);
	}
	
	private void showMessageWithBoolean(Boolean b, String str) {
		if(b)
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
		visitedBuildings = new ArrayList<Boolean>();
		
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
	    
	    // Behaviour to receive first AuxInfo and DelimitingZone where it will have to patrol.
	    fsm.registerFirstState(new InitialRecieve(this, scoutCoordinatorAgent), "STATE_1");
	    // Behaviour to send the list of garbage positions that is has found in this turn.
	    fsm.registerState(new SendGarbagePositions(this, scoutCoordinatorAgent), "STATE_2");
	    // Behaviour to receive AuxInfo for each turn.
	    fsm.registerState(new RecieveGameInfo(this, scoutCoordinatorAgent), "STATE_3");
	    
	    fsm.registerDefaultTransition("STATE_1", "STATE_2");
	    fsm.registerDefaultTransition("STATE_2", "STATE_3");
	    fsm.registerDefaultTransition("STATE_3", "STATE_2");
	    
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
									patrolZone = (DelimitingZone) reply.getContentObject();
									astar = new AStar(auxInfo);
									showMessage("Receiving patrol zone from "+receptor);
									//if(this.getAgent().getName().equals("s0@192.168.1.130:1099/JADE")){
									createPatrolPath();
									//}
									// Send the cell
						        	ACLMessage reply2 = reply.createReply();
						  	      	reply2.setPerformative(ACLMessage.INFORM);
						  	      	Cell c = null;
						  	      	try {
						  	      		AID agent_aid = this.myAgent.getAID();
						  	      		c = auxInfo.getAgentCell(agent_aid);
						  	      		
						  	      		/**
						  	      		 * Create the patrol zone with the bounds of the delimiting zone 
						  	      		 * of each agent. Add in arrayList of objective positions
						  	      		 */
						  	      		int x=(int) patrolZone.getUL().getX(); 
						  	      		int y=(int) patrolZone.getUL().getY();
						  	      		
						  	      		objectivePoint = checkInitialPosition(patrolZone.getUL());
						  	      	
						  	      		//First movement Following the best position to the path
					  	      			c = getBestPositionToObjective(auxInfo.getMap(), c, new Cell(objectivePoint.x, objectivePoint.y));
					  
						  	      		//Or random
						  	      		//c = getRandomPosition(auxInfo.getMap(), c);
					  	      			reply2.setContentObject(c); //Return a new cell to scout coordinator
						  	      	} catch (Exception e1) {
						  	      		reply2.setPerformative(ACLMessage.FAILURE);
						  	      		System.err.println(e1.toString());
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
	 * @author Marc Bola�os Sol�
	 * Looks for any position around it with garbage and sends them to the ScoutsCoordinator.
	 */
	protected class SendGarbagePositions extends SimpleBehaviour
	{
		private AID receptor;
		private Agent a;
		
		public SendGarbagePositions (Agent a, AID r)
		{
			super(a);
			this.receptor = r;
		}

		@Override
		public void action() {
			
			// Finds garbage positions
			ArrayList garbageDiscoveries = checkScoutDiscoveries();
			
			try {
				// Make the request
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
				request.setContentObject(garbageDiscoveries);
				request.addReceiver(receptor);
			    
			    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			    send(request);
			    showMessage("Connection STATE_2 between scout->scout coordinator: OK");
			} catch (IOException e) {
				showMessage("Problem found when sending new discoveries.");
			}
		    
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
		    showMessage("Connection STATE_3 between scout->scout coordinator: OK");
		   
		    
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
					  	      		int c_x = c.getRow();
					  	      		int c_y = c.getColumn();
					  	      							  	      		
					  	      		//Case when you don't have objective point
					  	      		if(objectivePoint==null){
					  	      			//If you have not got a objective you will follow the patrolling path.
					  	      			Point patrollMovement= patrollPath.get(patrollPoint);
					  	      			
						  	      		//newPositionCell.addAgent(c.getAgent()); //Save infoagent to the new position
						  	      		showMessageWithBoolean(debuggingPatrolingPath, "Patrolling path");
					  	      			c = checkIfPositionIsOccupied(auxInfo.getMap()[patrollMovement.x][patrollMovement.y],auxInfo.getMap(), c);
						  	      		
					  	      			//Update the pointer in the patrollPath
					  	      			if(sign){
						  	      			patrollPoint++;
					  	      			}else{
						  	      			patrollPoint--;
					  	      			}
						  	      		
						  	      		//Check if the agent arrives on the final o on the initially of the array of patroll path
						  	      		if((patrollPoint<0) || (patrollPoint==patrollPath.size())){
						  	      			sign = !sign; //
							  	      		if(sign){
							  	      			patrollPoint++;
							  	      			patrollPoint++;
						  	      			}else{
							  	      			patrollPoint--;
							  	      			patrollPoint--;
						  	      			}
						  	      		}
					  	      		//Case when you have objective point
					  	      		}else{
				  	      				Point UL = checkInitialPosition(patrolZone.getUL());
					  	      			if((objectivePoint.getX()==c_x) && (objectivePoint.getY()==c_y)){
					  	      				objectivePoint = null;
						  	      			Point patrollMovement = patrollPath.get(patrollPoint);
						  	      			c = checkIfPositionIsOccupied(auxInfo.getMap()[patrollMovement.x][patrollMovement.y],auxInfo.getMap(), c);
						  	      			
						  	      			//Check if the patroll movement is possible or not (if not occupated)
						  	      			if((patrollMovement.x != c.getRow()) && (patrollMovement.y != c.getColumn())){
						  	      				objectivePoint = patrollMovement; //Save the patroll movement as to the new objective because you do not follow the patrol path.
						  	      			}
						  	      			
						  	      			//Update the pointer in the patrollPath
						  	      			if(sign){
							  	      			patrollPoint++;
						  	      			}else{
							  	      			patrollPoint--;
						  	      			}
						  	      			if(patrollPoint<0 && patrollPoint>=patrollPath.size()){
							  	      			System.out.println("******Change direction*******");
							  	      			sign = !sign; //
								  	      		if(sign){
								  	      			patrollPoint++;
							  	      			}else{
								  	      			patrollPoint--;
							  	      			}
						  	      			}
					  	      			}else{
							  	      		c = getBestPositionToObjective(auxInfo.getMap(), c, new Cell(objectivePoint.x, objectivePoint.y));
					  	      			}
					  	      		}
					  	      		
					  	      		if(c == null){
						  	      		c = getRandomPosition(auxInfo.getMap(), c);
					  	      		}
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
	    	showMessage("STATE_3 return OK");
	    	return 0;
	    }
	}
	
	private Cell getBestPositionToObjective(Cell[][] cells, Cell actualPosition, Cell objectivePosition) {
		Cell newPosition = null;
		
		showMessage("I am in the cell = ["+actualPosition.getRow()+","+actualPosition.getColumn()+"]");
		showMessage("I wanna go to the cell = ["+objectivePosition.getRow()+","+objectivePosition.getColumn()+"]");
		
		newPosition = astar.shortestPath(cells, actualPosition, objectivePosition);
		
		newPosition = checkIfPositionIsOccupied(newPosition, cells, actualPosition);
		
		return newPosition;
	}

	
	private Cell checkIfPositionIsOccupied(Cell newPosition, Cell[][] cells, Cell actualPosition) {
		if(newPosition != null){
			//The new position is occupied by someone?
			if(newPosition.isThereAnAgent()){
				switch (newPosition.getAgent().getAgentType()){
					case InfoAgent.SCOUT:
						// Compare the ID's of each scout. The bigger one will be moved to desired position
						int s1 = auxInfo.getInfoAgent(this.scoutCoordinatorAgent).getAID().hashCode();
						int s2 = newPosition.getAgent().getAID().hashCode();
						if (s1 > s2){
							try {
								newPosition.addAgent(auxInfo.getInfoAgent(this.getAID())); 
							} catch (Exception e) {
								showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
							}
						}else{
							newPosition = moveToFreePlace(cells, actualPosition, newPosition, auxInfo.getInfoAgent(this.getAID()));
							try {
								newPosition.addAgent(auxInfo.getInfoAgent(this.getAID()));
							} catch (Exception e) {
								e.printStackTrace();
							} 
						}
						break;
					case InfoAgent.HARVESTER:
						newPosition = moveToFreePlace(cells, actualPosition, newPosition, auxInfo.getInfoAgent(this.getAID()));
						try {
							newPosition.addAgent(auxInfo.getInfoAgent(this.getAID()));
						} catch (Exception e) {
							e.printStackTrace();
						} 
						break;
					}
			}else{
				try {
					newPosition.addAgent(auxInfo.getInfoAgent(this.getAID()));
				} catch (Exception e) {
					showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
				}
			}
		}else{
			newPosition = moveToFreePlace(cells, actualPosition, newPosition, auxInfo.getInfoAgent(this.getAID()));
			try {
				newPosition.addAgent(auxInfo.getInfoAgent(this.getAID()));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}		
		return newPosition;
	}
	private Cell moveToFreePlace(Cell[][] cells, Cell actualPosition, Cell newPosition, InfoAgent infoAgent) {
			
		int x = actualPosition.getRow();
		int y = actualPosition.getColumn();
		
		int maxRows = auxInfo.getMapRows();
		int maxColumns = auxInfo.getMapColumns();
		
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
		return actualPosition;
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
		maxRows = auxInfo.getMapRows();
		maxColumns = auxInfo.getMapColumns();
		newPosition = actualPosition;
		int [][] posibleMovements = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(posibleMovements);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);

		int [] list = null;
		//Search a cell street
		while(arrayList.size()!=0 && !trobat)
		{
			z= auxInfo.getRandomPosition(arrayList.size());
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
						/* Compare the ID's of each scout. The bigger one will be moved to desired position*/
						int h1 = auxInfo.getInfoAgent(this.scoutCoordinatorAgent).getAID().hashCode();
						int h2 = newPosition.getAgent().getAID().hashCode();
						if (h1 > h2)
						{
							try {
								showMessage("Position before moving "+"["+x+","+y+"]");
								newPosition.addAgent(auxInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
								positionToReturn = newPosition;
								//actualPosition.removeAgent(actualPosition.getAgent());
							} catch (Exception e) {
								showMessage("ERROR: Failed to save the infoagent to the new position: "+e.getMessage());
							}
							trobat = true;
						}
					}
					/* If there is a harvester agent in front of */
					else if (newPosition.isThereAnAgent() && newPosition.getAgent().getAgentType() == InfoAgent.HARVESTER)
					{
						// Do nothing.
						
					}
					/* If there is not an agent */
					else
					{
						try {
							showMessage("Position before moving "+"["+x+","+y+"]");
							newPosition.addAgent(auxInfo.getInfoAgent(this.getAID())); //Save infoagent to the new position
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
	
	/**
	 * Checks if any of the surrounding buildings there is any unit of garbage.
	 * 
	 * @return ArrayList with the information about the garbage found with the following format 
	 * 					for each garbage cell (ArrayList):
	 * 						- int row in the map
	 * 						- int column in the map
	 * 						- Cell datatype storing information about the content of the cell.
	 */
	private ArrayList checkScoutDiscoveries(){
		showMessage("Checking New Discoveries...");
		ArrayList garbageFound = new ArrayList();
		
		Cell a = auxInfo.getAgentCell(this.getAID());
		
		Cell b; // cell possibly with garbage
		Cell[][] map = auxInfo.getMap();
		for (int i = 0; i < auxInfo.getMapRows(); i++){
			for (int j = 0; j < auxInfo.getMapColumns(); j++){
				b = map[i][j];
				try {
					if(b.getCellType() == Cell.BUILDING && b.getGarbageUnits() > 0){
						int x=a.getRow(); int y=a.getColumn();
						int xb=b.getRow(); int yb=b.getColumn();
						if (x>0){
							if ((xb==x-1) && (yb==y)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
							if ((y>0) && (xb==x-1) && (yb==y-1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);} 
							if ((y<auxInfo.getMap()[x].length-1) && (xb==x-1) && (yb==y+1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
						}
						if (x<auxInfo.getMap().length-1){
							if ((xb==x+1) && (yb==y)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
							if ((y>0) && (xb==x+1) && (yb==y-1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
							if ((y<auxInfo.getMap()[x].length-1) && (xb==x+1) && (yb==y+1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
						}
						if ((y>0) && (xb==x) && (yb==y-1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
						if ((y<auxInfo.getMap()[x].length-1) && (xb==x) && (yb==y+1)) {garbageFound.add(createGarbage(xb, yb, b));}// remove.add(b);}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
				
		//for (Cell r : remove)  game.getBuildingsGarbage().remove(r);
		return garbageFound;
	}
	
	private ArrayList createGarbage(int x, int y, Cell b){
		ArrayList garbage = new ArrayList();
		garbage.add(x);
		garbage.add(y);
		garbage.add(b);
		return garbage;
	}
	
	private void createPatrolPath(){
		showMessageWithBoolean(this.debuggingPatrolingPath,"Creating the patrolling path!");
		Point UL = patrolZone.getUL(); //We will use the UL as initial point on the patroll path.
		buildings = patrolZone.getBuildingsPositions();
		
		for(int i=0;i<buildings.size();i++){
			visitedBuildings.add(false);
		}
		
		this.noVisited = visitedBuildings.size();
		
		//Initial point of the path it is possible or not
		Point currentPos = checkInitialPosition(UL);
		showMessageWithBoolean(this.debuggingPatrolingPath,"Initial point:"+currentPos);
		
		patrollPath = new ArrayList<Point>(); 		//Points of patrol path
		Stack<Point> stack =new Stack<Point>();
		Set<Point> cellsVisited = new HashSet<Point>(); 		
		stack.add(currentPos);
		
		//While not visit all the buildings
		while(this.noVisited!=0 && !stack.isEmpty()){
			currentPos = stack.pop();
			
			if(!cellsVisited.contains(currentPos)){
				cellsVisited.add(currentPos);
				patrollPath.add(currentPos);
				
				int num = 0;
				for(Point p : getChilds(currentPos)){
					if(!cellsVisited.contains(p)){
						stack.push(p);
						num++;
					}
				}
				
				//Need backtracking because you already visited all of your neighbours
				if(num==0 && !stack.isEmpty()){
					showMessageWithBoolean(this.debuggingPatrolingPath,"\nBacktracking is neeeded!");
					Point objectivePos;
					objectivePos = stack.pop();
					while(cellsVisited.contains(objectivePos)&&!stack.isEmpty()){
						showMessageWithBoolean(this.debuggingPatrolingPath,"Pos->"+objectivePos);
						objectivePos =  stack.pop();
					}
					showMessageWithBoolean(this.debuggingPatrolingPath,"Current->"+currentPos);
					showMessageWithBoolean(this.debuggingPatrolingPath,"Objective->"+objectivePos);

					//Compute A* to the current position to the objective
					AStarPathFinder pathFinder = new AStarPathFinder(astar.map, 1000,false);// Diagonal movement not allowed false, true allowed
					Path path = pathFinder.findPath(null, currentPos.y, currentPos.x, objectivePos.y, objectivePos.x);
					
					for(int i=1; i<path.getLength()-1; i++){
						Step s = path.getStep(i);
						Point p = new Point(s.getY(),s.getX());
						patrollPath.add(p);
						showMessageWithBoolean(this.debuggingPatrolingPath,"Backtracking..."+p);
					}
					
					//Push the non visited position
					stack.add(objectivePos);
					showMessageWithBoolean(this.debuggingPatrolingPath,"Finish backtracking! \n");
				}
			}
			showMessageWithBoolean(this.debuggingPatrolingPath,"The patrolling path was finished!");
		}
		
		if(this.debuggingPatrolingPath){
			showMessageWithBoolean(this.debuggingPatrolingPath,"Patrol path have been created!");
			for(int i=0; i<this.visitedBuildings.size(); i++){
				if(!this.visitedBuildings.get(i)) System.out.println(this.buildings.get(i));
			}
		}
		
		if(this.debuggingPatrolingPath){
			showMessageWithBoolean(this.debuggingPatrolingPath,"Printing the patrol path");
			showMessageWithBoolean(this.debuggingPatrolingPath,"Path size"+patrollPath.size());
			for(Point p : patrollPath){
				showMessageWithBoolean(this.debuggingPatrolingPath,p.toString());
			}
			showMessageWithBoolean(this.debuggingPatrolingPath,"Process finished!");
		}
		patrollPath.remove(0);
	}
	
	
	/**
	 * Check and correct if the initial point is or not a building
	 * @param p Initial point
	 * @return Return a correct initial point
	 */
	private Point checkInitialPosition(Point p){
		Cell[][] map = auxInfo.getMap();
		Cell c = map[p.x][p.y];
		c.setColumn(p.y);
		c.setRow(p.x);
		int count = 0;
		while(c.getCellType()!=c.STREET){
			c = map[p.x][p.y+count];
			c.setColumn(p.y+count);
			c.setRow(p.x);
			count++;
		}
		return new Point(c.getRow(),c.getColumn());
	}
	
	/**
	 * Method to check the buildings around the current position
	 * @param movements All the possible movements
	 * @return Return the all the possible movements filtered
	 */
	private ArrayList<Point> checkBuilding(ArrayList<Point> movements){
		Point b;
		Point UL = patrolZone.getUL();
		Point BR = patrolZone.getBR();
		Cell[][] map = auxInfo.getMap();
		ArrayList<Point> possibleMovements = new ArrayList<Point>();
		for(Point p : movements){
			int i = 0;
			boolean found = false;

			while(i<buildings.size() && !found){
				b = buildings.get(i);
				//Check if the point p is a building
				if((b.getX()==p.getX()) &&(b.getY()==p.getY()))   found = true;
				else i++;
			}
			
			if(found){
				if(!visitedBuildings.get(i)){
					visitedBuildings.set(i, true);
					this.noVisited--;
					showMessageWithBoolean(this.debuggingPatrolingPath,"    Visiting->"+buildings.get(i)+"    Num non visites->"+this.noVisited);
				}
			}else{
				//Check if the movement it can be possible, it is in the delimited zone.
				if((p.x>=0)&&(p.y>=0)&&(p.x<auxInfo.getMapRows())&&(p.y<auxInfo.getMapColumns()) 
						&& (UL.getX()<=p.getX()) && (UL.getY()<=p.getY())  && (BR.getX()>=p.getX()) && (BR.getY()>=p.getY())){
					//Check the cell type of the new movement is a street
					int type = map[p.x][p.y].getCellType(); 
					if(Cell.STREET==type) possibleMovements.add(p);
				}
			}
		}
		return possibleMovements;
	}
	
	/**
	 * Get the child cells of the current position
	 * @param currentPos Current position
	 * @return Return the possible movements of the childs of the current position
	 */
	private ArrayList<Point> getChilds(Point currentPos){
		Point up, right, left, down;
		double x,y;
		ArrayList<Point> possibleMovements =  new ArrayList<Point>();
		
		x = currentPos.getX();
		y = currentPos.getY();
						
		//Move in the 4 possible directions
		up=new Point((int)currentPos.getX()-1,(int)currentPos.getY());
		right=new Point((int)currentPos.getX(),(int)currentPos.getY()+1);
		left=new Point((int)currentPos.getX(),(int)currentPos.getY()-1);
		down=new Point((int)currentPos.getX()+1,(int)currentPos.getY());
		
		//Remove the building in the list of no visited buildings.
		possibleMovements.add(up);
		possibleMovements.add(left);
		possibleMovements.add(right);
		possibleMovements.add(down);
		
		return checkBuilding(possibleMovements);
	}
}
