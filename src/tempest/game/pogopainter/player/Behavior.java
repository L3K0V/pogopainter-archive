package tempest.game.pogopainter.player;

import java.util.Vector;

import tempest.app.neurons.base.NeuronNetwork;
import tempest.game.pogopainter.system.Direction;

public interface Behavior {
	public void setPlayer(Player pl);
	public Direction getNextDirection();
	public void refreshInputData(Vector<Integer> input);
	public NeuronNetwork getBrain();
}
