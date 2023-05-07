package may.baseraids;


import java.util.Comparator;
import java.util.HashMap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public class RaidSpawnCountPerWaveRegistry extends HashMap<Integer, HashMap<EntityType<? extends Mob>, Integer>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void put(int wave, EntityType<? extends Mob> type, int count) {
		var existingInnerMap = this.get(wave);
		if(existingInnerMap == null) {
			HashMap<EntityType<? extends Mob>, Integer> newInnerMap = new HashMap<>();
			newInnerMap.put(type, count);
			this.put(wave, newInnerMap);
		}else {
			existingInnerMap.put(type, count);
		}
	}
	
	public int getMaxWave() {
		return this.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
	}
}
