package sma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import sma.ontology.AuxInfo;
import sma.ontology.Cell;
import sma.ontology.InfoAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
 *	Harvester Coordinator Agent
 *
 */
public class HarvesterCoordinatorAgent extends Agent {

	private AID coordinatorAgent;
	private AuxInfo mapInfo;
	private int countMapRequests = 0;

	// array storing the not handled messages
	private MessagesList messagesQueue = new MessagesList(this);

	private LinkedList<Cell> movementList = new LinkedList<Cell>();

	public HarvesterCoordinatorAgent() {
	}

	protected void setup() {

		/**** Very Important Line (VIL) *********/
		this.setEnabledO2ACommunication(true, 1);
		/****************************************/

		// Register the agent to the DF
		ServiceDescription sd1 = new ServiceDescription();
		sd1.setType(UtilsAgents.HARVESTER_COORDINATOR_AGENT);
		sd1.setName(getLocalName());
		sd1.setOwnership(UtilsAgents.OWNER);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.addServices(sd1);
		dfd.setName(getAID());
		try {
			DFService.register(this, dfd);
			showMessage("Registered to the DF");
		} catch (FIPAException e) {
			System.err.println(getLocalName() + " registration with DF "
					+ "unsucceeded. Reason: " + e.getMessage());
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

		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
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
		// Behaviour to send game info and first random movement to harvester agents
		fsm.registerState(new InitialSendToHarvester(this), "STATE_2");
		// Behaviour to receive one movement from one harvester
		fsm.registerState(new ReceiveMovement(this), "STATE_3");
		// Behaviour to send one movement of harvester to coordinator agent
		fsm.registerState(new SendMovement(this), "STATE_4");
		// Behaviour to send game info to all harvester
		fsm.registerState(new SendGameInfo(this), "STATE_5");

		// FSM transitions
		fsm.registerTransition("STATE_1", "STATE_2", 1);
		fsm.registerTransition("STATE_1", "STATE_5", 2);
		fsm.registerDefaultTransition("STATE_2", "STATE_3");
		fsm.registerTransition("STATE_3", "STATE_3", 1);
		fsm.registerTransition("STATE_3", "STATE_4", 2);
		fsm.registerTransition("STATE_4", "STATE_4", 1);
		fsm.registerTransition("STATE_4", "STATE_1", 2);
		fsm.registerDefaultTransition("STATE_5", "STATE_3");

		// Add behavior of the FSM
		addBehaviour(fsm);
	}

	
	/**
	 * 
	 * @author Iosu Mendizabal
	 *
	 *	Class to request the game information to the coordinator agent.
	 *
	 */
	protected class RequestGameInfo extends SimpleBehaviour {
		private AID receptor;
		private boolean firstTime = true;

		public RequestGameInfo(Agent a, AID r) {
			super(a);
			this.receptor = r;
		}

		public void action() {
			showMessage("STATE_1");

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
									okInfo = true;
									showMessage("Recieved game info from "+ reply.getSender());
								} catch (UnreadableException e) {
									messagesQueue.add(reply);
									System.err.println(getLocalName()
												+ " Recieved game info unsucceeded. Reason: "
												+ e.getMessage());
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
			showMessage("STATE_1 return OK");
			if (firstTime) {
				firstTime = false;
				return 1;
			}
			return 2;
		}
	}

	/**
	 * 
	 * @author Iosu Mendizabal
	 *
	 *	Class that sends the game info to all the harvester agents with a initial random movement.
	 *
	 */
	protected class InitialSendToHarvester extends SimpleBehaviour {
		public InitialSendToHarvester(Agent a) {
			super(a);
		}

		public void action() {
			showMessage("STATE_2");

			Random rnd = new Random();
			/*
			 * Make a broadcast to all harvester agent sending the game info and
			 * movement
			 */
			for (int i = 0; i < mapInfo.getHarvesters_aids().size(); i++) {
				/* Sending game info */
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
				request.addReceiver(mapInfo.getHarvesters_aids().get(i));
				request.setProtocol(InteractionProtocol.FIPA_REQUEST);
				try {
					request.setContentObject(mapInfo);
				} catch (Exception e) {
					request.setPerformative(ACLMessage.FAILURE);
					e.printStackTrace();
				}
				send(request);
				showMessage("Sending game info to "+ mapInfo.getHarvesters_aids().get(i));

				/* Make a broadcast to all harvesters sending random movement */
				int mapSize = mapInfo.getMap().length;
				Cell c = mapInfo.getCell(rnd.nextInt(mapSize), rnd.nextInt(mapSize));
				try {
					request.setContentObject(c);
				} catch (Exception e) {
					request.setPerformative(ACLMessage.FAILURE);
					e.printStackTrace();
				}
				send(request);
				showMessage("Sending random movement to "
						+ mapInfo.getHarvesters_aids().get(i));
			}
		}

		public boolean done() {
			return true;
		}

		public int onEnd() {
			showMessage("STATE_2 return OK");
			return 0;
		}
	}

	/**
	 * 
	 * @author Iosu Mendizabal
	 * 
	 * 	Class to receive the movement that the harvesters have done.
	 *
	 */
	protected class ReceiveMovement extends SimpleBehaviour {
		// Counter for knowing how many movements have been received
		private int countMovesReceived = 0;

		public ReceiveMovement(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			showMessage("STATE_3");

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
							showMessage("Receiving movement of the harvester "+reply.getSender());

							Cell c = (Cell) reply.getContentObject(); // Getting
																		// object
																		// with
																		// the
																		// movement
							movementList.add(c);
							okInfo = true;
							countMovesReceived++;
							showMessage("Recieved movement from "
									+ reply.getSender());
						} catch (UnreadableException e) {
							messagesQueue.add(reply);
							System.err
									.println(getLocalName()
											+ " Recieved game info unsucceeded. Reason: "
											+ e.getMessage());
						}
						break;
					case ACLMessage.FAILURE:
						System.err
								.println(getLocalName()
										+ " Recieved game info unsucceeded. Reason: Performative was FAILURE");
						break;
					default:
						// Unexpected messages received must be added to the queue.
						//showMessage("Doing defautl state 3....");
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

		public int onEnd() {
			/*
			 * If we have not received all movements from all scouts, we return
			 * 1 so we will repeat the same behaviour(state))
			 */
			if (countMovesReceived < mapInfo.getNumHarvesters())
				return 1;
			/*
			 * When we have recieved all movements, we return 2 so we will go to
			 * next state
			 */
			countMovesReceived = 0;
			showMessage("STATE_3 return OK");
			return 2;
		}
	}

	/**
	 * 
	 * @author Iosu Mendizabal
	 * 
	 * 	Class to send the movement that the harvester have done to the coordinator agent.
	 *
	 */
	protected class SendMovement extends SimpleBehaviour {
		private int countMovesSended = 0;

		public SendMovement(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			showMessage("STATE_4");

			/* Send movement to Coordinator agent */
			ACLMessage request = new ACLMessage(ACLMessage.INFORM);
			request.clearAllReceiver();
			request.addReceiver(coordinatorAgent);
			request.setProtocol(InteractionProtocol.FIPA_REQUEST);
			try {
				request.setContentObject(movementList.remove());
			} catch (Exception e) {
				request.setPerformative(ACLMessage.FAILURE);
				e.printStackTrace();
			}
			send(request);
			countMovesSended++;
			showMessage("Sending movement to " + coordinatorAgent);

		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return true;
		}

		public int onEnd() {
			/*
			 * If we have not sended all movements from all scouts, we return 1
			 * so we will repeat the same behaviour(state))
			 */
			if (countMovesSended < mapInfo.getNumHarvesters())
				return 1;
			/*
			 * When we have sended all movements, we return 2 so we will go to
			 * next state
			 */
			countMovesSended = 0;
			showMessage("STATE_4 return OK");
			return 2;
		}
	}

	/**
	 * 
	 * @author Iosu Mendizabal
	 * 
	 * 	Class to send the game information to all the harvesters.
	 *
	 *
	 */
	protected class SendGameInfo extends SimpleBehaviour {
		public SendGameInfo(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			showMessage("STATE_5");
			/*
			 * Make a broadcast to all scouts agent sending the game info and
			 * movement
			 */
			for (int i = 0; i < mapInfo.getHarvesters_aids().size(); i++) {
				/* Sending game info */
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.clearAllReceiver();
				request.addReceiver(mapInfo.getHarvesters_aids().get(i));
				request.setProtocol(InteractionProtocol.FIPA_REQUEST);
				try {
					request.setContentObject(mapInfo);
				} catch (Exception e) {
					request.setPerformative(ACLMessage.FAILURE);
					e.printStackTrace();
				}
				send(request);
				showMessage("Sending game info to "
						+ mapInfo.getHarvesters_aids().get(i));
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return true;
		}

		public int onEnd() {
			showMessage("STATE_5 return OK");
			return 0;
		}
	}

	/**
	 * A message is shown in the log area of the GUI
	 * 
	 * @param str
	 *            String to show
	 */
	private void showMessage(String str) {
		System.out.println(getLocalName() + ": " + str);
	}

}
