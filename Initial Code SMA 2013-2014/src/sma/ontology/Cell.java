package sma.ontology;

import java.util.*;
import java.io.Serializable;

/**
 * <p>
 * <B>Title:</b> IA2-SMA
 * </p> *
 * <p>
 * <b>Description:</b> Practical exercise 2013-14. Recycle swarm. This class
 * keeps all the information about a cell in the map.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p> *
 * <p>
 * <b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)
 * </p>
 * 
 * @author not attributable
 * @version 2.0
 */
public class Cell implements Serializable {

	static public int BUILDING = 1;
	static public int STREET = 2;
	static public int RECYCLING_CENTER = 3;

	private int type;

	private InfoAgent agent = null;

	// only for buildings
	private int garbageUnits = 0;
	private char garbageType = '-'; // G=Glass, P=Plastic, M=Metal, A=Paper

	// only for recycling centers
	private int[] garbagePoints = { 0, 0, 0, 0 }; // (Glass, Plastic, Metal, Paper)

	private int row = -1;
	private int column = -1;

	public Cell(int type) {
		this.type = type;
	}

	/** *********************************************************************** */

	public void setRow(int i) {
		this.row = i;
	}

	public int getRow() {
		return this.row;
	}

	public void setColumn(int i) {
		this.column = i;
	}

	public int getColumn() {
		return this.column;
	}

	public int getCellType() {
		return this.type;
	}

	public void setCellType(int newType) throws Exception {
		if ((newType != BUILDING) && (newType != STREET)
				&& (newType != RECYCLING_CENTER))
			throw new Exception("Unknown type");
		this.type = newType;
	}

	static public String getCellType(int tipus) {
		if (tipus == BUILDING)
			return "B";
		if (tipus == STREET)
			return "S";
		if (tipus == RECYCLING_CENTER)
			return "R";
		return "";
	}

	/** *********************************************************************** */

	public int getGarbageUnits() throws Exception {
		if (this.getCellType() != BUILDING)
			throw new Exception("Wrong operation");
		return this.garbageUnits;
	}

	public char getGarbageType() {
		return garbageType;
	}

	public String getGarbageString() throws Exception {
		if (this.getCellType() != BUILDING)
			throw new Exception("Wrong operation");
		if (garbageUnits == 0)
			return "";
		else {
			return "" + garbageUnits + garbageType;
		}
	}

	public void setGarbageUnits(int g) throws Exception {
		if (this.getCellType() != BUILDING)
			throw new Exception("Wrong operation");
		this.garbageUnits = g;
	}

	public void setGarbageType(char g) throws Exception {
		if (this.getCellType() != BUILDING)
			throw new Exception("Wrong operation");
		this.garbageType = g;
	}

	/** *********************************************************************** */

	public int[] getGarbagePoints() throws Exception {
		if (this.getCellType() != RECYCLING_CENTER)
			throw new Exception("Wrong operation");
		return garbagePoints;
	}
	
	public int getGarbagePoints(String garbage) throws Exception{
		if (this.getCellType() != RECYCLING_CENTER)
			throw new Exception("Wrong operation");
		if(garbage.equals("G")){
			return garbagePoints[0];
		}else if(garbage.equals("P")){
			return garbagePoints[1];
		}else if(garbage.equals("M")){
			return garbagePoints[2];
		}else if(garbage.equals("A")){
			return garbagePoints[3];
		}
		return 0;
	}

	public String getGarbagePointsString() throws Exception {
		if (this.getCellType() != RECYCLING_CENTER)
			throw new Exception("Wrong operation");
		String points = "";
		if (garbagePoints[0] > 0)
			points = points + garbagePoints[0] + "G";
		if (garbagePoints[1] > 0)
			points = points + garbagePoints[1] + "P";
		if (garbagePoints[2] > 0)
			points = points + garbagePoints[2] + "M";
		if (garbagePoints[3] > 0)
			points = points + garbagePoints[3] + "A";
		return points;
	}
	
	

	public void setGarbagePoints(int[] g) throws Exception {
		if (this.getCellType() != RECYCLING_CENTER)
			throw new Exception("Wrong operation");
		this.garbagePoints = g;
	}

	/** *********************************************************************** */

	public boolean isThereAnAgent() {
		return (agent != null);
	}

	public void addAgent(InfoAgent newAgent) throws Exception {
		System.out.println("Add "+newAgent.getAID()+" to " + this.toString());
		if ((this.getCellType() == BUILDING)
				|| (this.getCellType() == RECYCLING_CENTER))
			throw new Exception("Wrong operation");
		if ((this.getCellType() == STREET) && (this.isThereAnAgent()))
			throw new Exception("Full STREET cell");
		if (this.isAgent(newAgent))
			throw new Exception("Repeated InfoAgent");

		// if everything is OK, we add the new agent to the cell
		this.agent = newAgent;
	}

	private boolean isAgent(InfoAgent infoAgent) {
		if (infoAgent == null)
			return false;
		else {
			if (this.agent != null)
				return this.agent.equals(infoAgent);
			else
				return false;
		}
	}

	public void removeAgent(InfoAgent oldInfoAgent) throws Exception {
		if ((this.getCellType() == BUILDING)
				|| (this.getCellType() == RECYCLING_CENTER))
			throw new Exception("Wrong operation");
		if (this.agent == null)
			throw new Exception("No agents in this cell");
		if (!this.isAgent(oldInfoAgent))
			throw new Exception("InfoAgent not here");
		// if everything is OK, we remove the agent from the cell
		this.agent = null;
	}

	public InfoAgent getAgent() {
		return this.agent;
	}

		/** *********************************************************************** */

	public String toString() {
		String str = "";
		try {
			str = "(cell-type " + this.getCellType(this.getCellType()) + " "
					+ "(r " + this.getRow() + ")" + "(c "
					+ this.getColumn() + ")";
			if (this.type == this.STREET) {
				if (this.isThereAnAgent())
					str = str + "(agent ";
				else
					str = str + "(empty ";
				if (agent != null)
					str = str + this.agent.toString();
				str = str + ")";
			}
			if (this.type == this.BUILDING) {
				str = str + "(garbage " + this.getGarbageUnits()
						+ this.getGarbageType() + ")";
			}
			if (this.type == this.RECYCLING_CENTER) {
				str = str + "(garbage points " + this.getGarbagePointsString()
						+ ")";
			}
			str = str + ")";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}

} // endof class Cell
