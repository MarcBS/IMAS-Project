package sma;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

/**
 * Messages list or queue that automatically returns the first unhandled message or asks for a new one
 * calling the blockingReceive() function. 
 * 
 * The procedure for using this class is the following:
 * 
 * instantiate and initialize the MessagesList list variable.
 * while communication not finished:
 * 		msg = list.getMessage()
 * 		useful = handle(msg)
 * 		if(not useful)
 * 			list.add(msg)
 * end communication
 * list.endRetrieval()
 * 
 * Synchronization not guaranteed!
 * 
 * @author Marc Bola�os
 *
 */
public class MessagesList {

	private ArrayList<ACLMessage> list;
	private int i_msg;
	private boolean in_list;
	private Agent agent;
	
	/**
	 * Constructor of the list of messages for the given particular agent.
	 * 
	 * @param ag Agent that possesses this list.
	 */
	public MessagesList(Agent ag){
		list = new ArrayList<ACLMessage>();
		agent = ag;
		this.endRetrieval();
	}
	
	/**
	 * Gets the next message needed to process, this method automatically checks if it has to return
	 * a previously stored message or if it has to apply a blockingReceive() for retrieving a new message.
	 * 
	 * @return ACLMessage needed to process.
	 */
	public ACLMessage getMessage(){
		i_msg++;
		// Until we have not checked all the messages in the queue, we don't receive new messages
    	ACLMessage reply;
    	in_list = false;
    	if(list.size() <= i_msg){
    		reply = agent.blockingReceive();
    	} else {
    		reply = list.get(i_msg);
    		list.remove(i_msg);
    		in_list = true;
    	}
    	return reply;
	}
	
	
	/**
	 * Adds the passed message to the list checking if it was retrieved from a particular 
	 * position or if it is a new message.
	 * 
	 * @param msg ACLMessage to add to the list.
	 */
	public void add(ACLMessage msg){
		// Unexpected messages received must be added to the queue.
		if(!in_list){
			list.add(msg);
		} else {
			list.add(i_msg, msg);;
		}
	}
	
	/**
	 * Ends the retrieval reseting the needed variables. This method must be called every time we have finished
	 * and have found the necessary messages.
	 */
	public void endRetrieval(){
		i_msg = -1;
		in_list = false;
	}
	
	public int size(){
		return list.size();
	}
	
	
}
