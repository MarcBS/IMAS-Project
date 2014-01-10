package sma.ontology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

/**
 * 
 * @author Iosu Mendizabal
 * 
 *         Class where the A* path finding algorithm is computed.
 * 
 */
public class AStar {

	private static final int MAX_PATH_LENGTH = 400;

	private SimpleMap map;
	private AuxInfo mapInfo;
	
	public AStar(AuxInfo mapInfo) {
		this.mapInfo = mapInfo;
		
		final int[][] MAP = new int[mapInfo.getMapRows()][mapInfo
				.getMapColumns()];

		for (int i = 0; i < mapInfo.getMapRows(); i++) {
			for (int j = 0; j < mapInfo.getMapColumns(); j++) {
				if (mapInfo.getCell(i, j).getCellType() == Cell.STREET) {
					MAP[i][j] = 1;
				} else {
					MAP[i][j] = 0;
				}
			}
		}
//		for (int i = 0; i < MAP.length; i++) {
//			for (int j = 0; j < MAP[0].length; j++) {
//				System.out.print(MAP[i][j] + " ");
//			}
//			System.out.println();
//		}
		map = new SimpleMap(MAP);
	}

	public Cell shortestPath(Cell[][] cells, Cell actualPosition, Cell objectivePosition) {
		
		if(objectivePosition.getCellType() != Cell.STREET){
			objectivePosition = getNearObjectStreetPosition(cells, objectivePosition);
		}
		
		int x_in = actualPosition.getRow(), y_in = actualPosition.getColumn();
		int x_togo = objectivePosition.getRow(), y_togo = objectivePosition
				.getColumn();

		AStarPathFinder pathFinder = new AStarPathFinder(map, MAX_PATH_LENGTH,
				false);// Diagonal movement not allowed false, true allowed
		Path path = pathFinder.findPath(null, y_in, x_in, y_togo, x_togo);

		if (path == null) {
			System.out
					.println("null path because the objective is a building or imposible to find.");
			return null;
		} else {
			int length = path.getLength();
			System.out.println("Found path of length: " + length + ".");

			for (int i = 0; i < length; i++) {
				System.out.println("Move to: " + path.getY(i) + ","
						+ path.getX(i) + ".");
			}
			Cell positionToReturn = cells[path.getY(1)][path.getX(1)];

			System.out.println("cell type is ="+positionToReturn);
			return positionToReturn;
		}
	}

	public Cell getNearObjectStreetPosition(Cell[][] cells,Cell objectivePosition) {
		int x= objectivePosition.getRow(), y=(int) objectivePosition.getColumn(), xi=0, yi=0;
		int maxRows=0, maxColumns=0;
		Cell newPosition = null;
		
		maxRows = mapInfo.getMapRows();
		maxColumns = mapInfo.getMapColumns();
		
		int [][] nearPlaces = {{x+1,y},{x,y+1},{x-1,y},{x,y-1}};
		List<int[]> intList = Arrays.asList(nearPlaces);
		ArrayList<int[]> arrayList = new ArrayList<int[]>(intList);
		
		objectivePosition = cells[x][y];
		
		if(objectivePosition.getCellType() != Cell.STREET){
			int [] list = null;
			//Search a cell street
			while(arrayList.size() != 0){
				list = arrayList.remove(0);
				
				xi = list[0];
				yi = list[1];
				if(xi < maxRows && xi >= 0 && yi >= 0 && yi < maxColumns)	//Check if the position it's in the range of the map
				{ 
					newPosition = cells[xi][yi];
					if(Cell.STREET == newPosition.getCellType() )	//Check the limits of the map
					{ 
						objectivePosition = newPosition;
						return objectivePosition;
					}
				}
			}
			objectivePosition = newPosition;
		}	
		return objectivePosition;
	}

}

class SimpleMap implements TileBasedMap {
	private static final int WIDTH = 20;
	private static final int HEIGHT = 20;

	private final int[][] MAP;

	public SimpleMap(int[][] MAP) {
		this.MAP = MAP;
	}

	@Override
	public boolean blocked(PathFindingContext ctx, int x, int y) {
		return MAP[y][x] != 1;
	}

	@Override
	public float getCost(PathFindingContext ctx, int x, int y) {
		return 1.0f;
	}

	@Override
	public int getHeightInTiles() {
		return HEIGHT;
	}

	@Override
	public int getWidthInTiles() {
		return WIDTH;
	}

	@Override
	public void pathFinderVisited(int x, int y) {
	}

}