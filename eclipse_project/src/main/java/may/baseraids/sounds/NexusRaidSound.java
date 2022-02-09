package may.baseraids.sounds;

import may.baseraids.NexusEffectsTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

public class NexusRaidSound extends NexusSound {

	public NexusRaidSound(NexusEffectsTileEntity entity) {
		super(SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F, entity);
		this.repeat = true;
	    this.repeatDelay = 60;
	}
	
	@Override
	protected boolean shouldSwitchSound() {
		return !raidManager.isRaidActive();
	}
}
