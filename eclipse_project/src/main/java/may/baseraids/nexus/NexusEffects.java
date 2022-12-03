package may.baseraids.nexus;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

/**
 * This class holds and provides the effects for the nexus.
 * 
 * @author Natascha May
 */
public class NexusEffects {
	
	/**
	 * Gets a new EffectInstance for a given predefined NexusEffect.
	 * @param effect
	 * @return
	 */
	public static EffectInstance getEffectInstance(NexusEffect effect) {
		return new EffectInstance(effect.EFFECT, effect.DURATION, effect.AMPLIFIER);
	}
	
	public static class NexusEffect{
		final Effect EFFECT;
		final int DURATION;
		final int AMPLIFIER;
		NexusEffect(Effect effect, int duration, int amplifier) {
			this.EFFECT = effect;
			this.DURATION = duration;
			this.AMPLIFIER = amplifier;
		}
	}
	
	/**
	 * This class specifies the properties of the slowness debuff that will be added
	 * to all players, if there is no nexus placed in the world.
	 */
	public static final NexusEffect DEBUFF = new NexusEffect(Effects.SLOWNESS, 200, 0);
	
	public static final NexusEffect REGEN_EFFECT_AFTER_RAID_WIN = new NexusEffect(Effects.REGENERATION, 200, 0);
	
	// speed buffs with increasing value
	public static final NexusEffect SPEEDBUFF_1 = new NexusEffect(Effects.SPEED, 200, 0);
	
	public static final NexusEffect SPEEDBUFF_2 = new NexusEffect(Effects.SPEED, 400, 0);
	
	public static final NexusEffect SPEEDBUFF_3 = new NexusEffect(Effects.SPEED, 400, 1);
	
	public static final NexusEffect SPEEDBUFF_4 = new NexusEffect(Effects.SPEED, 600, 1);
	
	public static final NexusEffect HASTEBUFF_1 = new NexusEffect(Effects.HASTE, 600, 0);
	
	public static final NexusEffect HASTEBUFF_2 = new NexusEffect(Effects.HASTE, 1200, 0);
	
	public static final NexusEffect LUCKBUFF = new NexusEffect(Effects.LUCK, 1200, 0);
	
}
