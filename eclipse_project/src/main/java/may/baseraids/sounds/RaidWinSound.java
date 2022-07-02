package may.baseraids.sounds;

import may.baseraids.Baseraids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

/**
 * This class controls the sound played upon winning a raid.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1.1
 */
public class RaidWinSound extends LocatableSound {

	public RaidWinSound() {
		
		
		super(SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.BLOCKS);
		this.volume = 5.0F;
		this.pitch = 1.5F;
		this.repeatDelay = 10;
		Baseraids.LOGGER.debug("Play Raid Win Sound");
		Minecraft.getInstance().getSoundHandler().play(this);
		Minecraft.getInstance().getSoundHandler().playDelayed(this, repeatDelay);
		this.pitch += 0.5F;
		Minecraft.getInstance().getSoundHandler().playDelayed(this, repeatDelay);
	}

}
