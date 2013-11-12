package sma.ontology;

import jade.core.AID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import sma.gui.UtilsGUI;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

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
 * @author David Isern & Joan Albert L���pez
 * @see sma.CoordinatorAgent
 * @see sma.CentralAgent
 */
public class AuxInfo implements java.io.Serializable {

	//Properties about the map
	protected Cell[][] map;
	private int mapRows;
	private int mapColumns;
	
	private int numScouts;
	private int numHarvesters;
	private String[] typeHarvesters;
	private int[] capacityHarvesters;
	private int seed = 120;
	private Random generator;
	
	public AuxInfo(){
		generator = new Random();
	}

	// For each InfoAgent it contains its initial cell
	private HashMap<InfoAgent, Cell> agentsPosition = new HashMap<InfoAgent, Cell>(); 
	
	//AID of the harversters
	private List<AID> harvesters_aids = new ArrayList<AID>();
	
	//AID of the scout
	private List<AID> scout_aids = new ArrayList<AID>();
	
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
			agentsPosition.put(c.getAgent(), c);
	}

	public HashMap<InfoAgent, Cell> getAgentsInitialPosition() {
		return agentsPosition;
	}

	public void setAgentsInitialPosition(
			HashMap<InfoAgent, Cell> agentsInitialPosition) {
		this.agentsPosition = agentsInitialPosition;
	}
	
	public void setAgentCell(InfoAgent a, Cell new_cell){
		agentsPosition.put(a, new_cell);
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

	public List<AID> getHarvesters_aids() {
		return harvesters_aids;
	}

	public void setHarvesters_aids(List<AID> harvesters_aids) {
		this.harvesters_aids = harvesters_aids;
	}
	
	public void addHarvester_aid(AID aid){
		this.harvesters_aids.add(aid);
	}
	
	public void removeHarvester_aid(AID aid){
		this.harvesters_aids.remove(aid);
	}

	public List<AID> getScout_aids() {
		return scout_aids;
	}

	public void setScout_aids(List<AID> scout_aids) {
		this.scout_aids = scout_aids;
	}
	
	public void addScout_aid(AID aid){
		this.scout_aids.add(aid);
	}
	
	public void removeScout_aid(AID aid){
		this.scout_aids.remove(aid);
	}
	
	public void addBuilding(Cell cell) {
		buildings.add(cell);	
	}
	
	public List<Cell> getAllBuildings(){
		return buildings;
	}
	
	/**
	 * Give the cell of the agent
	 * @param aid AID of the agent
	 * @return Return the cell of the agent
	 */
	public Cell getAgentCell(AID aid){
		Collection<Cell> cells = agentsPosition.values(); //Get all the cell of the all agents
		Iterator<Cell> it = cells.iterator();
		boolean trobat = false;
		Cell cell = null;
		while(it.hasNext() && !trobat) {
		    cell = it.next();
			InfoAgent agent = cell.getAgent();
			if (agent.getAID().equals(aid)){
				trobat = true;
			}			
		}
		return cell;
	}
	
	/**
	 * Give the info agent of the specific aid
	 * @param aid Aid of the agent
	 * @return Return the Info Agent
	 */
	public InfoAgent getInfoAgent(AID aid){
		InfoAgent agent = null;
		Collection<Cell> cells = agentsPosition.values(); //Get all the cell of the all agents
		Iterator<Cell> it = cells.iterator();
		boolean trobat = false;
		Cell cell = null;
		while(it.hasNext() && !trobat) {
		    cell = it.next();
			agent = cell.getAgent();
			if (agent.getAID().equals(aid)){
				trobat = true;
			}			
		}
		return agent;
	}
	
	/**
	 * Method to get a random seed number to select position in the map
	 * @return Return a position
	 */
	public int getRandomPosition(int size){
		generator = new Random();
	    int num = generator.nextInt(size);
	    return num;
	}

	public int getMapRows() {
		return mapRows;
	}

	public void setMapRows(int mapRows) {
		this.mapRows = mapRows;
	}

	public int getMapColumns() {
		return mapColumns;
	}

	public void setMapColumns(int mapColumns) {
		this.mapColumns = mapColumns;
	}
	
	
	
}
