package may.baseraids.sounds;

import may.baseraids.NexusEffectsTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

public class NexusRaidSound extends NexusSound {

	public NexusRaidSound(NexusEffectsTileEntity entity) {
		super(SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 2.0F, 0.1F, entity);
	    this.repeatDelay = 0;
	}
	
	@Override
	protected boolean shouldSwitchSound() {
		hasSwitchedSound = true;
		return !raidManager.isRaidActive();
	}
}
