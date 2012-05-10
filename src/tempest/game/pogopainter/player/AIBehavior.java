package tempest.game.pogopainter.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tempest.game.pogopainter.bonuses.Arrow;
import tempest.game.pogopainter.bonuses.BonusObject;
import tempest.game.pogopainter.bonuses.Bonuses;
import tempest.game.pogopainter.bonuses.Checkpoint;
import tempest.game.pogopainter.bonuses.Teleport;
import tempest.game.pogopainter.gametypes.Game;
import tempest.game.pogopainter.system.Board;
import tempest.game.pogopainter.system.Cell;
import tempest.game.pogopainter.system.Direction;
import tempest.game.pogopainter.system.Score;

/**
 * 
 * @author Asen Lekov <asenlekoff@gmail.com>
 * @version 3
 * @since 29/03/2012
 * 
 *        Basic artificial intelligence (AI)
 * 
 *        Simple implementation of AI that can get points by getting different
 *        bonuses and move around the game board.
 * 
 */

public class AIBehavior implements Behavior {
	private Difficulty AIdifficult;
	private Game actions;
	private Direction currentDir = Direction.NONE;
	private Checkpoint random = null;
	private Arrow arrow = null;
	private Teleport tp = null;
	private BonusObject followCP = null;

	private Random rnd = new Random();
	private boolean following = false;
	private Score score = null;
	private Player AI;

	private List<Arrow> arrows;
	private List<Teleport> teleports;

	/**
	 * Use {@link #AIBehavior(Difficulty, Game)} to attach behavior to AI
	 * 
	 * @param AIdifficult
	 *            behavior difficulty (easy, normal, hard)
	 * @param game
	 *            game that provides access to all game components such players,
	 *            bonuses, board e.g.
	 */

	public AIBehavior(Difficulty AIdifficult, Game game) {
		this.AIdifficult = AIdifficult;
		this.actions = game;
	}

	public void setPlayer(Player pl) {
		this.AI = pl;
	}

	/**
	 * Use {@link #easy(Board, Player, int)} to define attached behavior to EASY
	 * 
	 * @param b
	 *            board AI play on
	 * @param AI
	 *            player
	 * @param randomNumber
	 *            random number, chance to take some bonuses
	 */

	public void easy(Board b, Player AI, int randomNumber) {
		Direction nextDir = Direction.NONE;
		score = new Score(b, AI);

		int pointsToFollow = 5;

		List<Checkpoint> checkpoints = actions.getBonusHandler()
				.getCheckpoints();
		arrows = new ArrayList<Arrow>();
		teleports = new ArrayList<Teleport>();

		fillBonusLitstByTypes();

		getFreshCheckpointTarget(checkpoints);
		getFreshArrowTarget();
		getFreshTeleportTarget();

		nextDir = Direction.values()[rnd.nextInt(4) + 1];

		if (!actions.checkDir(nextDir, AI) && !following) {
			getNewFreshDirection(b, AI, nextDir);
		}
		if (random != null && checkPlayerOnBonus(AI, random)) {
			following = false;
		}

		if (teleports.size() > 0 && tp != null && AI.getBonus() == null) {
			nextDir = getTeleportDirectionForClosestPlayer(AI, nextDir);
		} else

		// CHECKPOINTS
		if (checkpoints.size() > 1 && !following
				&& score.Calculate() >= pointsToFollow) {
			nextDir = setClosestCheckpointDirection(AI, checkpoints);
		} else if (checkpoints.size() > 0 && !checkPlayerOnBonus(AI, random)
				&& score.Calculate() >= pointsToFollow) {
			nextDir = useTeleportOrFollowCheckpoint(b, AI, nextDir);

			// ARROWS
		} else if (arrows.size() > 1) {
			nextDir = getArrowDirectionFromClosestPlayer(AI, nextDir);
		} else if (arrows.size() > 0) {
			if (shouldAIGetArrow(AI, arrow)) {
				nextDir = getNextDirectionToBonus(AI, arrow);
			}
		} else {
			getNewFreshDirection(b, AI, nextDir);
		}
		setDirection(nextDir);
	}

	public boolean checkPlayerOnBonus(Player player, BonusObject bonus) {
		boolean sure = false;
		if (player.getX() == bonus.getX() && player.getY() == bonus.getY()) {
			sure = true;
		}
		return sure;
	}

	public boolean checkArrowForGivingPoints(Arrow arrow) {
		boolean sure = true;
		int x = arrow.getX();
		int y = arrow.getY();
		switch (arrow.getState()) {
		case 1:
			if (x == 7) {
				sure = false;
			}
			break;
		case 2:
			if (y == 7) {
				sure = false;
			}
			break;
		case 3:
			if (x == 0) {
				sure = false;
			}
			break;
		case 4:
			if (y == 0) {
				sure = false;
			}
			break;
		}
		return sure;
	}

	private Direction setClosestCheckpointDirection(Player AI,
			List<Checkpoint> checkpoints) {
		Direction nextDir;
		for (Player p : actions.getPlayers()) {
			if (calcDistance(p, random.getX(), random.getY()) < calcDistance(
					AI, random.getX(), random.getX())) {
				while (random == followCP) {
					followCP = checkpoints.get(rnd.nextInt(checkpoints.size()));
				}
				random = (Checkpoint) followCP;
			}
		}
		nextDir = getNextDirectionToBonus(AI, random);
		return nextDir;
	}

	private Direction useTeleportOrFollowCheckpoint(Board b, Player AI,
			Direction nextDir) {
		boolean teleport2cp = false;
		for (Player p : actions.getPlayers()) {
			if (calcDistance(p, random.getX(), random.getY()) < calcDistance(
					AI, random.getX(), random.getY())) {
				teleport2cp = true;
			}
		}
		if (teleport2cp && AI.getBonus() != null && score.Calculate() >= 10) {
			actions.triggerBonus(AI, AI.getBonus());
			nextDir = getNewFreshDirection(b, AI, nextDir);
		}
		nextDir = getNextDirectionToBonus(AI, random);
		following = true;
		return nextDir;
	}

	private void getFreshTeleportTarget() {
		if (teleports.size() > 0 && !teleports.contains(tp)) {
			tp = teleports.get(rnd.nextInt(teleports.size()));
		}
	}

	private void getFreshArrowTarget() {
		if (arrows.size() > 0 && !arrows.contains(arrow)) {
			arrow = arrows.get(rnd.nextInt(arrows.size()));
		}
	}

	private void getFreshCheckpointTarget(List<Checkpoint> checkpoints) {
		if (checkpoints.size() > 0 && !checkpoints.contains(random)) {
			random = checkpoints.get(rnd.nextInt(checkpoints.size()));
			followCP = random;
		}
	}

	private void fillBonusLitstByTypes() {
		for (BonusObject bo : actions.getBonusHandler().getOtherBonuses()) {
			if (bo.getType() == Bonuses.ARROW) {
				arrows.add((Arrow) bo);
			} else if (bo.getType() == Bonuses.TELEPORT) {
				teleports.add((Teleport) bo);
			}
		}
	}

	private Direction getArrowDirectionFromClosestPlayer(Player AI,
			Direction nextDir) {
		for (Arrow arrow : arrows) {
			if (calcDistance(AI, this.arrow.getX(), this.arrow.getY()) > calcDistance(
					AI, arrow.getX(), arrow.getY())) {
				this.arrow = arrow;
			}
		}
		if (shouldAIGetArrow(AI, arrow)) {
			nextDir = getNextDirectionToBonus(AI, arrow);
		}
		return nextDir;
	}

	private Direction getTeleportDirectionForClosestPlayer(Player AI,
			Direction nextDir) {
		for (Player p : actions.getPlayers()) {
			if (calcDistance(AI, tp.getX(), tp.getY()) < calcDistance(p,
					tp.getX(), tp.getY())) {
				nextDir = getNextDirectionToBonus(AI, tp);
			}
		}
		return nextDir;
	}

	private void setDirection(Direction dir) {
		currentDir = dir;
	}

	/**
	 * @author Asen Lekov <asenlekoff@gmail.com>
	 * @version 1
	 * @since 04/14/2012
	 * 
	 *        Use {@link #shouldAIGetArrow(Player, Arrow)} to get result it`s
	 *        okay AI getting Arrow bonus
	 * 
	 * @param AI
	 *            who will take arrow bonus
	 * @param arrow
	 *            which arrow bonus will be taken
	 * @return true if player when get Arrow bonus will take new cells,
	 *         otherwise false
	 */

	private boolean shouldAIGetArrow(Player AI, Arrow arrow) {
		boolean isReached = false;
		for (Direction dir : Direction.values()) {
			boolean checkDir = actions.checkDir(dir, AI);
			switch (dir) {
			case LEFT:
				if (checkDir && checkArrowForGivingPoints(arrow)) {
					isReached = true;
				}
				break;
			case RIGHT:
				if (checkDir && checkArrowForGivingPoints(arrow)) {
					isReached = true;
				}
				break;
			case UP:
				if (checkDir && checkArrowForGivingPoints(arrow)) {
					isReached = true;
				}
				break;
			case DOWN:
				if (checkDir && checkArrowForGivingPoints(arrow)) {
					isReached = true;
				}
				break;
			}
		}
		return isReached;
	}

	/**
	 * @author Asen Lekov <asenlekoff@gmail.com>
	 * @version 1
	 * @since 05/04/2012
	 * 
	 *        Getting new fresh direction, different from last direction
	 * 
	 * @param b
	 *            Board that player play on
	 * @param p
	 *            Target player
	 * @param l
	 *            Last direction of palyer
	 * @return new direction different from last & available
	 */

	private Direction getNewFreshDirection(Board b, Player p, Direction l) {
		Direction newDir = Direction.NONE;
		while (!actions.checkDir(l, p)
				&& (l == newDir || newDir == Direction.NONE)) {
			newDir = Direction.values()[rnd.nextInt(4) + 1];
			setDirection(newDir);
		}
		return newDir;
	}

	/**
	 * @author Asen Lekov <asenlekoff@gmail.com>
	 * @version 1
	 * @since 29/03/2012
	 * 
	 *        Simple implementation of A* pathfinding
	 * 
	 * @param x
	 *            x cord of player
	 * @param y
	 *            y cord of player
	 * @param destination
	 *            Destination cell
	 * @return A* distance from player to target cell
	 */

	private double calcDistance(int x, int y, Cell destination) {
		double distance = Math.sqrt(Math.pow((x - destination.getX()), 2)
				+ Math.pow((y - destination.getY()), 2));
		return distance;
	}

	/**
	 * @author Asen Lekov <asenlekoff@gmail.com>
	 * @version 1
	 * @since 29/03/2012
	 * 
	 *        Simple implementation of A* pathfinding
	 * 
	 * @param AI
	 *            Player distance we want FROM
	 * @param x
	 *            Target x cord
	 * @param y
	 *            Target y cord
	 * @return A* distance from player to target cell
	 */

	private double calcDistance(Player AI, int x, int y) {
		double distance = Math.sqrt(Math.pow((AI.getX() - x), 2)
				+ Math.pow((AI.getY() - y), 2));
		return distance;
	}

	/**
	 * @author Asen Lekov <asenlekoff@gmail.com>
	 * @version 1
	 * @since 14/04/2012
	 * 
	 *        Calculating what is shortest direction from player to bonus
	 * 
	 * @param AI
	 *            Player who will get bonus
	 * @param bonus
	 *            Target bonus
	 * @return Shortest direction based on A*
	 * 
	 *         Uses {@link #calcDistance(int, int, Cell)} &
	 *         {@link #calcDistance(Player, int, int)}
	 */

	private Direction getNextDirectionToBonus(Player AI, BonusObject bonus) {
		Direction nextDirection = Direction.NONE;
		int x = AI.getX();
		int y = AI.getY();
		Cell destination = new Cell(bonus.getX(), bonus.getY());
		double distance = calcDistance(x, y, destination);

		for (Direction dir : Direction.values()) {
			boolean checkDir = actions.checkDir(dir, AI);

			switch (dir) {
			case LEFT:
				if (checkDir && distance > calcDistance(x - 1, y, destination)) {
					distance = calcDistance(x - 1, y, destination);
					nextDirection = dir;
				}
				break;
			case RIGHT:
				if (checkDir && distance > calcDistance(x + 1, y, destination)) {
					distance = calcDistance(x + 1, y, destination);
					nextDirection = dir;
				}
				break;
			case DOWN:
				if (checkDir && distance > calcDistance(x, y + 1, destination)) {
					distance = calcDistance(x, y + 1, destination);
					nextDirection = dir;
				}
				break;
			case UP:
				if (checkDir && distance > calcDistance(x, y - 1, destination)) {
					distance = calcDistance(x, y - 1, destination);
					nextDirection = dir;
				}
				break;
			}
		}

		return nextDirection;
	}

	public Direction getNextDirection() {
		easy(actions.getBoard(), AI, rnd.nextInt(2) + 1);
		return currentDir;
	}
}