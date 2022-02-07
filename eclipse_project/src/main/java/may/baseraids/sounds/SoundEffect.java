package may.baseraids.sounds;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This class characterizes a sound effect with it's properties like volume,
 * pitch and for repeating sound effects the interval between the sound. It can
 * be used to easily define fixed sound effects.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1.1
 */
public class SoundEffect {

	SoundEvent soundEvent;
	SoundCategory soundCategory;
	float volume;
	float pitch;
	public int intervalInTicks;

	public SoundEffect(SoundEvent soundEvent, SoundCategory soundCategory, float volume, float pitch,
			int intervalInTicks) {
		this.soundEvent = soundEvent;
		this.soundCategory = soundCategory;
		this.volume = volume;
		this.pitch = pitch;
		this.intervalInTicks = intervalInTicks;
	}

	public void playSound(World world, @Nullable PlayerEntity player, BlockPos pos) {
		world.playSound(player, pos, soundEvent, soundCategory, volume, pitch);
	}
}
