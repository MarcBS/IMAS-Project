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
	private int x;
	private int y;
	
	public Movement(String id, int x, int y){
		agent_id = id;
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
	
}
