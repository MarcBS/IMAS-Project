package sma.ontology;

import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;
/**
 * 
 * @author Iosu Mendizabal
 * 
 * Class where the A* path finding algorithm is computed.
 *
 */
public class AStar {

	private static final int MAX_PATH_LENGTH = 400;

	private SimpleMap map;

	public AStar(AuxInfo mapInfo) {
		final int[][] MAP = new int[mapInfo.getMapRows()][mapInfo
				.getMapColumns()];

		for (int i = 0; i < mapInfo.getMapRows(); i++) {
			for (int j = 0; j < mapInfo.getMapColumns(); j++) {
				if (mapInfo.getCell(i, j).getCellType() == 2) {
					MAP[i][j] = 1;
				} else {
					MAP[i][j] = 0;
				}
			}
		}
		for(int i = 0; i <  MAP.length; i++){
			for(int j = 0; j <MAP[0].length; j++){
				System.out.print(MAP[i][j]+ " ");
			}
			System.out.println();
	}
		map = new SimpleMap(MAP);
	}

	public Cell shortestPath(Cell[][] cells, Cell actualPosition, Cell objectivePosition) {
		AStarPathFinder pathFinder = new AStarPathFinder(map, MAX_PATH_LENGTH, true);
		Path path = pathFinder.findPath(null, actualPosition.getRow(),
				actualPosition.getColumn(), objectivePosition.getRow(), objectivePosition.getColumn());
		if(path == null){
			System.out.println("null path because the objective is a building or imposible to find.");
			return null; 
		}else{
			int length = path.getLength();
			System.out.println("Found path of length: " + length + ".");

			for (int i = 0; i < length; i++) {
				System.out.println("Move to: " + path.getX(i) + "," + path.getY(i)
						+ ".");
			}
			Cell positionToReturn = cells[path.getX(1)][path.getY(1)];

			return positionToReturn;
		}
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
		return MAP[y][x] == 0;
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