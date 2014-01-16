package sma;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.DelimitingZone;
import sma.ontology.InfoAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class ScoutCoordinatorAgent extends Agent{
	
	// Indicates if we want to show the debugging messages
	private boolean debugging = false;
	
	private AuxInfo info;

	private AID coordinatorAgent;
	
	private LinkedList<Cell> movementList = new LinkedList<Cell>();
	private ArrayList discoveriesList = new ArrayList();
	
	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);

	public ScoutCoordinatorAgent(){}
	
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
	    sd1.setType(UtilsAgents.SCOUT_COORDINATOR_AGENT);
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
	    
	    // search CoordinatorAgent
	    ServiceDescription searchCriterion = new ServiceDescription();
	    searchCriterion.setType(UtilsAgents.COORDINATOR_AGENT);
	    this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
	    
	    
	    ACLMessage requestInicial = new ACLMessage(ACLMessage.REQUEST);
	    requestInicial.clearAllReceiver();
	    requestInicial.addReceiver(this.coordinatorAgent);
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
	    
	    // Behaviour to request game info
	    fsm.registerFirstState(new RequestGameInfo(this, coordinatorAgent), "STATE_1");
	    // Behaviour to send game info, first random movement and delimiting zone 
	    // (buildings to explore) to scout agents 
	    fsm.registerState(new InitialSendToScout(this), "STATE_2");
	    // Behaviour to receive one movement from one scout 
	    fsm.registerState(new ReceiveMovement(this), "STATE_3");
	    // Behaviour to send one movement of scout to coordinator agent 
	    fsm.registerState(new SendMovement(this), "STATE_4");
	    // Behaviour to send game info to all scout 
	    fsm.registerState(new SendGameInfo(this), "STATE_5");
	    // Behaviour to receive new discoveries from Scouts
	    fsm.registerState(new ReceiveNewDiscoveries(this), "STATE_6");
	    // Behaviour to send new discoveries to CoordinatorAgent
	    fsm.registerState(new SendNewDiscoveries(this), "STATE_7");
	    
	    // FSM transitions
	    fsm.registerTransition("STATE_1", "STATE_2", 1);
	    fsm.registerTransition("STATE_1", "STATE_5", 2);
	    //fsm.registerDefaultTransition("STATE_2", "STATE_3");
	    fsm.registerDefaultTransition("STATE_2", "STATE_6");
	    fsm.registerTransition("STATE_3", "STATE_3", 1);
	    fsm.registerTransition("STATE_3", "STATE_4", 2);
	    fsm.registerTransition("STATE_4", "STATE_4", 1);
	    fsm.registerTransition("STATE_4", "STATE_1", 2);
	    //fsm.registerDefaultTransition("STATE_5", "STATE_3");
	    fsm.registerDefaultTransition("STATE_5", "STATE_6");
	    fsm.registerTransition("STATE_6", "STATE_6", 1);
	    fsm.registerTransition("STATE_6", "STATE_7", 2);
	    fsm.registerDefaultTransition("STATE_7", "STATE_3");
	    
	    addBehaviour(fsm);
	    
	}
	
	/**
	 * 
	 * @author Albert Busqué
	 *
	 * Class that implements behavior for requesting game info (map)
	 */
	protected class RequestGameInfo extends SimpleBehaviour 
	{
		private AID receptor;
		private boolean firstTime = true;
		
		public RequestGameInfo (Agent a, AID r)
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
		    try {
			      request.setContent("get map");
			      send(request);
			      showMessage("Requesting game info to "+receptor);
			    } catch (Exception e) {
			      e.printStackTrace();
			    }
		    
		  /* Reception of game info
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
								info = (AuxInfo) reply.getContentObject();	// Getting object with the information about the game
								okInfo = true;
								showMessage("Recieved game info from "+reply.getSender());
							} catch (Exception e) {
								messagesQueue.add(reply);
								//System.err.println(getLocalName() + " Recieved game info unsucceeded from "+ reply.getSender().getLocalName() +". Reason: " + e.getMessage());
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
	    	if (firstTime)
	    	{
	    		firstTime = false;
	    		return 1;
	    	}
	    	return 2;
	    }
	}
	
	/**
	 * 
	 * @author Albert
	 * 
	 * Class that implements behavior for sending game info (map) to all scout agents and a random movement
	 */
	protected class InitialSendToScout extends SimpleBehaviour 
	{
		public InitialSendToScout (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			Random rnd = new Random();
			
			// Get delimiting zones for all the scouts
			ArrayList<DelimitingZone> zones = new ArrayList<DelimitingZone>();
			zones = getDelimitingZones(zones, info.getScout_aids().size());
			// Assigns the closest zone to each agent.
			int[] orderZones = sortZones(zones, info.getAgentsInitialPosition(), info.getScout_aids());
			
			
			/* Make a broadcast to all scouts agent sending the game info and movement*/
			for (int i=0; i<info.getScout_aids().size(); i++)
			{	
				/* Sending game info */
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
			    request.addReceiver(info.getScout_aids().get(i));
			    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			    try {
			    	request.setContentObject(info);
			    } catch (Exception e) {
				    request.setPerformative(ACLMessage.FAILURE);
				    e.printStackTrace();
				}
			    send(request);
			    showMessage("Sending game info to "+info.getScout_aids().get(i));
			    
			    /* Make a broadcast to all scouts sending the assigned DelimitingZone*/
			    //int mapSize = info.getMap().length;
			    //Cell c = info.getCell(rnd.nextInt(mapSize), rnd.nextInt(mapSize));
			    DelimitingZone dz = zones.get(orderZones[i]);
			    try {
			    	request.setContentObject(dz);
			    } catch (Exception e) {
				    request.setPerformative(ACLMessage.FAILURE);
				    e.printStackTrace();
				}
			    send(request);
			    showMessage("Sending random movement to "+ info.getScout_aids().get(i));
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
	 * @author Albert
	 * 
	 * Class that implements behavior of receiving movements from scouts
	 */
	protected class ReceiveMovement extends SimpleBehaviour
	{
		// Counter for knowing how many movements have been received
		private int countMovesReceived = 0;
		
		public ReceiveMovement (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			/*
			 * Reception of the movement
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
								Cell c = (Cell) reply.getContentObject();	// Getting object with the movement
								movementList.add(c);
								okInfo = true;
								countMovesReceived++;
								showMessage("Recieved movement from "+reply.getSender());
							} catch (UnreadableException e) {
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
			/* If we have not received all movements from all scouts, we return 1 so we will repeat the same behaviour(state)) */
	    	if (countMovesReceived < info.getNumScouts())
	    		return 1;
	    	/* When we have recieved all movements, we return 2 so we will go to next state */
	    	countMovesReceived = 0;
	    	showMessage("STATE_3 return OK");
	    	return 2;
	    }
	}
	
	/**
	 * 
	 * @author Albert
	 * 
	 * Class that implements behavior of sending one movement to one scout
	 */
	protected class SendMovement extends SimpleBehaviour
	{	
		private int countMovesSended = 0;
		
		public SendMovement (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			/* Send movement to Coordinator agent */
			ACLMessage request = new ACLMessage(ACLMessage.INFORM);
			request.clearAllReceiver();
		    request.addReceiver(coordinatorAgent);
		    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
		    try 
		    {
		    	request.setContentObject(movementList.remove());
		    } catch (Exception e) {
			    	request.setPerformative(ACLMessage.FAILURE);
			    	e.printStackTrace();
			    	}
		    send(request);
		    countMovesSended++;
		    showMessage("Sending movement to "+coordinatorAgent);
			
		}

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
			/* If we have not sended all movements from all scouts, we return 1 so we will repeat the same behaviour(state)) */
	    	if (countMovesSended < info.getNumScouts())
	    		return 1;
	    	/* When we have sended all movements, we return 2 so we will go to next state */
	    	countMovesSended = 0;
	    	showMessage("STATE_4 return OK");
	    	return 2;
	    }
	}
	
	/**
	 * 
	 * @author Albert
	 * 
	 * Class that implements behavior of sending game info to all scouts
	 */
	protected class SendGameInfo extends SimpleBehaviour
	{
		public SendGameInfo (Agent a)
		{
			super(a);
		}

		@Override
		public void action() {
			/* Make a broadcast to all scouts agent sending the game info and movement */
			for (int i=0; i<info.getScout_aids().size(); i++)
			{	
				/* Sending game info */
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
			    request.addReceiver(info.getScout_aids().get(i));
			    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			    try 
			    {
			    	request.setContentObject(info);
			    } catch (Exception e) {
				    	request.setPerformative(ACLMessage.FAILURE);
				    	e.printStackTrace();
				    	}
			    send(request);
			    showMessage("Sending game info to "+info.getScout_aids().get(i));
			}
		}

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
	    	showMessage("STATE_5 return OK");
	    	return 0;
	    }
	}
	
	
	/**
	 * 
	 * @author Marc Bolaños Solà
	 * 
	 * Class that implements behavior of receiving new discoveries from scouts
	 */
	protected class ReceiveNewDiscoveries extends SimpleBehaviour
	{
		// Counter for knowing how many discoveries have been received
		private int countDiscoveriesReceived = 0;
		
		public ReceiveNewDiscoveries (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			/*
			 * Reception of the new discoveries ArrayList
			 */
			showMessage("STATE_6: waiting for new discoveries from Scouts.");
			
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
								ArrayList discoveries = (ArrayList) reply.getContentObject();	// Getting object with ArrayList of discoveries
								for(int i = 0; i < discoveries.size(); i++){
									discoveriesList.add((ArrayList)discoveries.get(i));
								}
								okInfo = true;
								countDiscoveriesReceived++;
								showMessage("Recieved new discovery from "+reply.getSender());
							} catch (Exception e) {
								messagesQueue.add(reply);
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
			/* If we have not received all movements from all scouts, we return 1 so we will repeat the same behaviour(state)) */
	    	if (countDiscoveriesReceived < info.getNumScouts())
	    		return 1;
	    	/* When we have recieved all the new discoveries, we return 2 so we will go to next state */
	    	countDiscoveriesReceived = 0;
	    	showMessage("STATE_6 return OK");
	    	return 2;
	    }
	}
	
	/**
	 * 
	 * @author Marc Bolaños Solà
	 * 
	 * Class that implements behavior of sending all the new discoveries to the coordinator agent.
	 */
	protected class SendNewDiscoveries extends SimpleBehaviour
	{	
		
		public SendNewDiscoveries (Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() {
			/* Send movement to Coordinator agent */
			ACLMessage request = new ACLMessage(ACLMessage.INFORM);
			request.clearAllReceiver();
		    request.addReceiver(coordinatorAgent);
		    request.setProtocol(InteractionProtocol.FIPA_REQUEST);
		    try 
		    {
		    	request.setContentObject(discoveriesList);
		    	send(request);
			    showMessage("Sending movement to "+coordinatorAgent);
			    discoveriesList = new ArrayList();
		    } catch (Exception e) {
			    request.setPerformative(ACLMessage.FAILURE);
			    e.printStackTrace();
			}
		    
			
		}

		@Override
		public boolean done() {
			return true;
		}
		
		public int onEnd(){
	    	showMessage("STATE_7 return OK");
	    	return 0;
	    }
	}

	/**
	 * Creates each of the zones for the available ScoutAgents dividing 
	 * all the map by half until it reaches the desired number.
	 * 
	 * @param zones ArrayList<DelimitingZone> empty.
	 * @param nScouts int number of scouts in our charge.
	 * @return zones ArrayList<DelimitingZone> with all the zones created.
	 */
	public ArrayList<DelimitingZone> getDelimitingZones(ArrayList<DelimitingZone> zones, int nScouts) {
		
		// Initialize first zone containing all the map.
		DelimitingZone dz = new DelimitingZone(0, 0, this.info.getMapRows()-1, this.info.getMapColumns()-1);
		try {
			
			dz.setBuildings(this.info);
			zones.add(dz);
			
			// Divides each of the zones by half until we have a zone for each scout
			for(int i = 1; i < nScouts; i++){
				dz = zones.get(0);
				zones.remove(0);
				DelimitingZone[] dzs = dz.divideZone();
				zones.add(dzs[0]);
				zones.add(dzs[1]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return zones;
	}

	/**
	 * Assigns the closest DelimitingZone in "zones" to each scout defined by its AID in "scouts_aids"
	 * and taking into account its position stored in "agentsInitialPosition".
	 * 
	 * @param zones
	 * @param agentsInitialPosition
	 * @param scout_aids
	 * @return int[] sorted list with the id of each zone (position) for each AID in scouts_aids.
	 */
	public int[] sortZones(ArrayList<DelimitingZone> zones, HashMap<InfoAgent, Cell> agentsInitialPosition, List<AID> scout_aids) {

		int[] orderZones = new int[zones.size()];
		for(int i = 0; i < zones.size(); i++){
			orderZones[i] = -1;
		}
		
		// Matrix of distances between each agent (rows) and each zone (columns).
		int[][] matDist = new int[scout_aids.size()][zones.size()];
		
		// For each agent
		int count = 0;
		for(AID a_id : scout_aids){
			try {
				
				//InfoAgent a_info = new InfoAgent(InfoAgent.SCOUT, scout_aids.get(count));
				//Cell a = agentsInitialPosition.get(a_info);
				
				// Looks for the current agent's position
				Cell a = null;
				boolean found = false;
				Set<Entry<InfoAgent, Cell>> set = agentsInitialPosition.entrySet();
				Iterator<Entry<InfoAgent, Cell>> iter = set.iterator();
				while(iter.hasNext() && !found){
					Entry<InfoAgent, Cell> entry = iter.next();
					if(entry.getKey().getAID().equals( scout_aids.get(count) )){
						a = entry.getValue();
						found = true;
					}
				}
				
				// Checks for each agent the distance to each DelimitingZone
				for(int i = 0; i < zones.size(); i++){
					Point BR = zones.get(i).getBR();
					Point UL = zones.get(i).getUL();
					int y = a.getRow();
					int x = a.getColumn();
					
					if(y <= BR.y && y >= UL.y && x <= BR.x && x >= UL.x){
						// is inside the zone
						matDist[count][i] = 0;
					} else if(y <= BR.y && y >= UL.y){
						if(x > BR.x){
							// is at the right
							matDist[count][i] = x - BR.x;
						} else if(x < UL.x){
							// is at the left
							matDist[count][i] = UL.x - x;
						}
					} else if(x <= BR.x && x >= UL.x){
						if(y < UL.y){
							// is at the top
							matDist[count][i] = UL.y - y;
						} else if(y > BR.y){
							// is at the bottom
							matDist[count][i] = y - BR.y;
						}
					} else {
						// is in diagonal
						if(y < UL.y && x > BR.x){
							// top right
							matDist[count][i] = (UL.y - y) + (x - BR.x);
						} else if(y < UL.y && x < UL.x){
							// top left
							matDist[count][i] = (UL.y - y) + (UL.x - x);
						} else if(y > BR.y && x > BR.x){
							// bottom right
							matDist[count][i] = (y - BR.y) + (x - BR.x);
						} else if(y > BR.y && x < UL.x){
							// bottom left
							matDist[count][i] = (y - BR.y) + (UL.x - x);
						}
					}
				} // end for
				
			} catch (Exception e) {
				e.printStackTrace();
			}

			count++;
		} // end for
		
		
		// Now we select the best zone for each agent
		for(int i = 0; i < zones.size(); i++){
			// gets the column (zone) values
			double[] distsZone = new double[zones.size()];
			for(int j = 0; j < zones.size(); j++){
				distsZone[j] = (double)matDist[j][i];
			}
			
			// sorts the distances
			int[] orderedIDs = getIndicesInOrder((double[])distsZone);
			boolean found = false;
			int j = zones.size()-1;
			while(!found){
				if(orderZones[orderedIDs[j]] == -1){
					// if the current agent has not been assigned to any zones yet
					orderZones[orderedIDs[j]] = i;
					found = true;
				}
				j--;
			}
		}
		
		return orderZones;
	}
	
	public static int[] getIndicesInOrder(double[] array) {
	    Map<Integer, Double> map = new HashMap<Integer, Double>(array.length);
	    for (int i = 0; i < array.length; i++)
	        map.put(i, array[i]);

	    List<Entry<Integer, Double>> l = 
	                           new ArrayList<Entry<Integer, Double>>(map.entrySet());

	    Collections.sort(l, new Comparator<Entry<?, Double>>() {
	            @Override
	            public int compare(Entry<?, Double> e1, Entry<?, Double> e2) {
	                return e2.getValue().compareTo(e1.getValue());
	            }
	        });

	    int[] result = new int[array.length];
	    for (int i = 0; i < result.length; i++)
	        result[i] = l.get(i).getKey();

	    return result;
	}
	
}