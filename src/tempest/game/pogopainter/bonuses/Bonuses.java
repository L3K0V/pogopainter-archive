package tempest.game.pogopainter.bonuses;

public enum Bonuses {
	NONE(-1),   CHECKPOINT(0), ARROW(1), 
	SPEEDUP(2), SHOOT(3),      TELEPORT(4), 
	CLEANER(5), STUN(6);
	
	private int bonus;
	
	private Bonuses(int bonus) {
		this.bonus = bonus;
	}
	
	public int getBonusInt() {
		return bonus;
	}
}
