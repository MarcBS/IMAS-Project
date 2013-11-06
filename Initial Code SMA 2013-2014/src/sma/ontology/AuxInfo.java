package sma.ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sma.gui.UtilsGUI;
import java.util.*;

/**
 * <p>
 * <B>Title:</b> IA2-SMA
 * </p>
 * <p>
 * <b>Description:</b> Practical exercise 2013-14. Recycle swarm.
 * </p>
 * Main information about the game which is sent to the coordinator agent during the
 * initialization. This object is initialized from a file.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)
 * </p>
 * 
 * @author David Isern & Joan Albert L�pez
 * @see sma.CoordinatorAgent
 * @see sma.CentralAgent
 */
public class AuxInfo implements java.io.Serializable {

	protected Cell[][] map;

	private int numScouts;
	private int numHarvesters;
	private String[] typeHarvesters;
	private int[] capacityHarvesters;

	private HashMap<InfoAgent, Cell> agentsInitialPosition = new HashMap<InfoAgent, Cell>(); 
	// For each InfoAgent it contains its initial cell
	
	private List<Cell> recyclingCenters = new ArrayList<Cell>(); 
	// It contains the list of cells with recycling centers
	
	// a list containing all the buildings
	private List<Cell> buildings = new ArrayList<Cell>();
	
	
	
	
	public Cell[][] getMap() {
		return this.map;
	}

	public void setMap(Cell[][] map) {
		this.map = map;
	}

	public Cell getCell(int x, int y) {
		return this.map[x][y];
	}

	public void setCell(int x, int y, Cell c) {
		this.map[x][y] = c;
	}

	public int getNumScouts() {
		return this.numScouts;
	}

	public int getNumHarvesters() {
		return this.numHarvesters;
	}

	protected void setNumScouts(int numScouts) {
		this.numScouts = numScouts;
	}

	protected void setNumHarvesters(int numHarvesters) {
		this.numHarvesters = numHarvesters;
	}

	protected void setTypeHarvesters(String[] typeHarvesters) {
		this.typeHarvesters = typeHarvesters;
	}

	public String[] getTypeHarvesters() {
		return typeHarvesters;
	}

	protected void setCapacityHarvesters(int[] capacityHarvesters) {
		this.capacityHarvesters = capacityHarvesters;
	}

	public int[] getCapacityHarvesters() {
		return capacityHarvesters;
	}

	public void fillAgentsInitialPositions(List<Cell> agents) {
		for (Cell c : agents)
			agentsInitialPosition.put(c.getAgent(), c);
	}

	public HashMap<InfoAgent, Cell> getAgentsInitialPosition() {
		return agentsInitialPosition;
	}

	public void setAgentsInitialPosition(
			HashMap<InfoAgent, Cell> agentsInitialPosition) {
		this.agentsInitialPosition = agentsInitialPosition;
	}

	public List<Cell> getRecyclingCenters() {
		return recyclingCenters;
	}

	public void setRecyclingCenters(List<Cell> recyclingCenters) {
		this.recyclingCenters = recyclingCenters;
	}

	public void addRecyclingCenter(Cell c) {
		recyclingCenters.add(c);
	}

	public void addBuilding(Cell cell) {
		buildings.add(cell);
		
	}
	
	public List<Cell> getAllBuildings(){
		return buildings;
	}
}
