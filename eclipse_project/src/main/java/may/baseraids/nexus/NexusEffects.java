package may.baseraids.nexus;

import java.util.Arrays;
import java.util.List;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * This class holds and provides the effects for the nexus.
 * 
 * @author Natascha May
 */
public class NexusEffects {

	/**
	 * Gets a new EffectInstance for a given predefined NexusEffect.
	 * 
	 * @param effect
	 * @return
	 */
	public static MobEffectInstance getEffectInstance(NexusEffect effect) {
		return new MobEffectInstance(effect.effect, effect.duration, effect.amplifier);
	}

	public static class NexusEffect {
		final MobEffect effect;
		final int duration;
		final int amplifier;

		NexusEffect(MobEffect effect, int duration, int amplifier) {
			this.effect = effect;
			this.duration = duration;
			this.amplifier = amplifier;
		}
	}

	/**
	 * This class specifies the properties of the slowness debuff that will be added
	 * to all players, if there is no nexus placed in the world.
	 */
	public static final NexusEffect DEBUFF = new NexusEffect(MobEffects.MOVEMENT_SLOWDOWN, 200, 0);

	public static final NexusEffect REGEN_EFFECT_AFTER_RAID_WIN = new NexusEffect(MobEffects.REGENERATION, 200, 0);

	static final List<NexusEffects.NexusEffect> effects1 = Arrays.asList();
	static final List<NexusEffects.NexusEffect> effects2 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 200, 0));
	static final List<NexusEffects.NexusEffect> effects3 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 200, 0));
	static final List<NexusEffects.NexusEffect> effects4 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 400, 0));
	static final List<NexusEffects.NexusEffect> effects5 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 400, 1));
	static final List<NexusEffects.NexusEffect> effects6 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 400, 1));
	static final List<NexusEffects.NexusEffect> effects7 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 400, 1),
			new NexusEffect(MobEffects.DIG_SPEED, 600, 0));
	static final List<NexusEffects.NexusEffect> effects8 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 400, 1),
			new NexusEffect(MobEffects.DIG_SPEED, 1200, 0));
	static final List<NexusEffects.NexusEffect> effects9 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 600, 1),
			new NexusEffect(MobEffects.DIG_SPEED, 1200, 0), new NexusEffect(MobEffects.LUCK, 1200, 0),
			new NexusEffect(MobEffects.WATER_BREATHING, 200, 0));
	static final List<NexusEffects.NexusEffect> effects10 = Arrays.asList(new NexusEffect(MobEffects.MOVEMENT_SPEED, 600, 1),
			new NexusEffect(MobEffects.DIG_SPEED, 1200, 0), new NexusEffect(MobEffects.LUCK, 1200, 0),
			new NexusEffect(MobEffects.WATER_BREATHING, 200, 0), new NexusEffect(MobEffects.SATURATION, 200, 0));

	/**
	 * For each raid level, this list holds a list of effects that will be applied
	 * to nearby players.
	 */
	protected static final List<List<NexusEffects.NexusEffect>> effects = Arrays.asList(NexusEffects.effects1, NexusEffects.effects2,
			NexusEffects.effects3, NexusEffects.effects4, NexusEffects.effects5, NexusEffects.effects6,
			NexusEffects.effects7, NexusEffects.effects8, NexusEffects.effects9, NexusEffects.effects10);
	
	private NexusEffects() {
		throw new IllegalStateException("Utility class");
	}
}
