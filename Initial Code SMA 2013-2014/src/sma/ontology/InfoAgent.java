package sma.ontology;

import jade.core.AID;

/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * <p><b>Copyright:</b> Copyright (c) 2013</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert López
 */
public class InfoAgent extends Object implements java.io.Serializable {

	static public int SCOUT = 0;
	static public int HARVESTER = 1;

	private int typeAgent = -1;
	private AID aid;

	private boolean[] garbageType = { false, false, false, false }; //(Glass, Plastic, Metal, Paper)

	private int maxUnits = 10;
	private int units = 0;

	static public int GLASS = 0;
	static public int PLASTIC = 1;
	static public int METAL = 2;
	static public int PAPER = 3;

	private int currentType = -1;

	public boolean[] getGarbageType() {
		return garbageType;
	}

	public void setGarbageType(boolean[] garbageType) {
		this.garbageType = garbageType;
	}

	public int getMaxUnits() {
		return maxUnits;
	}

	public void setMaxUnits(int maxUnits) {
		this.maxUnits = maxUnits;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public int getCurrentType() {
		return currentType;
	}

	public void setCurrentType(int type) throws Exception {
		if ((type != GLASS) && (type != PLASTIC) && (type != METAL)
				&& (type != PAPER))
			throw new Exception("Unkown type");
		this.currentType = type;
	}

	public boolean equals(InfoAgent a) {
		return a.getAID().equals(this.aid);
	}

	public AID getAID() {
		return this.aid;
	}

	public void setAID(AID aid) {
		this.aid = aid;
	}

	public int getAgentType() {
		return this.typeAgent;
	}

	public void setAgentType(int type) throws Exception {
		if ((type != SCOUT) && (type != HARVESTER))
			throw new Exception("Unkown type");
		this.typeAgent = type;
	}

	public String getAgent() throws Exception {
		if (typeAgent == SCOUT)
			return "S";
		if (typeAgent == HARVESTER)
			return "H";
		throw new Exception("Unkown type");
	}

	public String toString() {
		String str = "";
		str = "(info-agent (agent-type " + this.getAgentType() + ")";
		//str=str+"(aid "+ this.getAID().getLocalName()+")";
		if (this.getAgentType() == InfoAgent.HARVESTER) {
			if (this.getCurrentType() > -1)
				str += "type" + "(" + this.getCurrentType() + ")";
			str += "units" + "(" + this.getUnits() + "/" + this.getMaxUnits()
					+ ")";
		} else
			str += ")";
		return str;
	}

	/**
	 * Default constructor
	 * @param agentType int Type of the agent we want to save
	 * @param aid AID Agent identifier
	 * @throws Exception Errors in the assignation
	 */
	public InfoAgent(int agentType, AID aid) throws Exception {
		this.setAgentType(agentType);
		this.setAID(aid);
	}

	/**
	 * Constructor for the information of the agent, without its AID
	 * @param agentType int
	 * @throws Exception Errors in the assignation
	 */
	public InfoAgent(int agentType) throws Exception {
		this.setAgentType(agentType);
	}

} //endof class InfoAgent
