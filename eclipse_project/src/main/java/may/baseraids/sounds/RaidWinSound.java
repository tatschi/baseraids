package may.baseraids.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

/**
 * This class controls the sound played upon winning a raid.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1.1
 */
public class RaidWinSound extends TickableSound {

	public RaidWinSound() {
		super(SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT);
		this.volume = 5.0F;
		this.pitch = 1.5F;
		this.repeatDelay = 10;
	}

	@Override
	public void tick() {
		Minecraft.getInstance().getSoundHandler().playOnNextTick(this);
		Minecraft.getInstance().getSoundHandler().playDelayed(this, repeatDelay);
		this.pitch += 0.5F;
		Minecraft.getInstance().getSoundHandler().playDelayed(this, repeatDelay);
		this.finishPlaying();
	}

}
