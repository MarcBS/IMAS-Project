package sma;

import java.io.Serializable;

/**
 * Defines a movement on the map made by an agent.
 * 
 * @author Marc Bolaños
 *
 */
public class Movement implements Serializable{

	private String agent_id;
	private String agent_type; // see UtilsAgents
	private int x;
	private int y;
	
	public Movement(String id, int x, int y, String a_type){
		agent_id = id;
		agent_type = a_type;
		this.x = x;
		this.y = y;
	}
	
	public String getAgentId(){
		return this.agent_id;
	}
	
	public int[] getPosition(){
		int[] pos = {x, y};
		return pos;
	}
	
	public String getAgentType(){
		return agent_type;
	}
	
}
