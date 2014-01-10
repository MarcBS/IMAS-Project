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
import sma.ontology.AStar;
import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.DelimitingZone;
import sma.ontology.InfoAgent;
import sma.ontology.InfoGame;

public class ScoutAgent extends Agent {
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = false;

	private AID scoutCoordinatorAgent;
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);
	private AuxInfo auxInfo;
	private DelimitingZone patrolZone; // object describing the zone where the scout must patrol
		
	private ArrayList<Cell> objectivePosition = new ArrayList<Cell>();
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
									showMessage("Receiving patrol zone from "+receptor);
									// Send the cell
						        	ACLMessage reply2 = reply.createReply();
						  	      	reply2.setPerformative(ACLMessage.INFORM);
						  	      	Cell c = null;
						  	      	try {
						  	      		AID agent_aid = this.myAgent.getAID();
						  	      		c = auxInfo.getAgentCell(agent_aid);
						  	      		
						  	      		astar = new AStar(auxInfo);
						  	      		
						  	      		int x=(int) patrolZone.getUL().getX(); 
						  	      		int y=(int) patrolZone.getUL().getY();
						  	      		objectivePosition.add(auxInfo.getMap()[x][y]);
						  	      			
						  	      		x=(int) patrolZone.getBR().getX();
						  	      		y=(int) patrolZone.getBR().getY();
						  	      		objectivePosition.add(auxInfo.getMap()[x][y]);
						  	      		
							  	      	x=(int) patrolZone.getBL().getX();
						  	      		y=(int) patrolZone.getBL().getY();
						  	      		objectivePosition.add(auxInfo.getMap()[x][y]);
						  	      		
						  	      		x=(int) patrolZone.getUR().getX();
						  	      		y=(int) patrolZone.getUR().getY();
						  	      		objectivePosition.add(auxInfo.getMap()[x][y]);
						  	      		
						  	      		//Change the objective position if not a street to the nearest one
						  	      		for(int i = 0 ; i < objectivePosition.size() ; i++){
						  	      			if(objectivePosition.get(i).getCellType()!=Cell.STREET){
						  	      				Cell pos = astar.getNearObjectStreetPosition(auxInfo.getMap(), objectivePosition.get(i));
						  	      				objectivePosition.remove(i);
						  	      				objectivePosition.add(i,pos);
						  	      			}
						  	      		}
						  	      		
					  	      			//Cell newC = getBestPositionToObjective(auxInfo.getMap(), c, objectivePosition.get(0));
					  	      			//c = newC;
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
					  	      		if(c.getRow() == objectivePosition.get(0).getRow() && c.getColumn() == objectivePosition.get(0).getColumn()){
					  	      			Cell corner = objectivePosition.get(0);
					  	      			objectivePosition.remove(0);
					  	      			objectivePosition.add(corner);
					  	      		}
					  	      		Cell newC = getBestPositionToObjective(auxInfo.getMap(), c, objectivePosition.get(0));
					  	      		System.out.println("actual pos = "+c);

					  	      		System.out.println("move to = "+newC);
					  	      		if(newC == null){
						  	      		c = getRandomPosition(auxInfo.getMap(), c);
					  	      		}else{
					  	      			c = newC;
					  	      		}
					  	      		//c = getRandomPosition(auxInfo.getMap(), c);

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
	
}
