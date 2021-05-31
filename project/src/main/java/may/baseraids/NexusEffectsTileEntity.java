package may.baseraids;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;



public class NexusEffectsTileEntity extends TileEntity implements ITickableTileEntity{

	
	
	Effect curEffect = null;
	
	public NexusEffectsTileEntity() {
		super(Baseraids.NEXUS_TILE_ENTITY_TYPE.get());
	}
	
	
	// the tick method is called each tick (approx. 20 ticks per second)
	public void tick() {
		if (this.world.getGameTime() % 80L == 0L) {
            this.addEffectsToPlayers();
            this.world.playSound((PlayerEntity)null, this.pos, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 2.0F, 0.5F);
      	}
		
	}
	
	private void addEffectsToPlayers() {
		if (!this.world.isRemote && curEffect != null) {
			double distance = (double)(50);
			int amplifier = 0;
	
			int duration = 100;
			AxisAlignedBB axisalignedbb = (new AxisAlignedBB(this.pos)).grow(distance).expand(0.0D, (double)this.world.getHeight(), 0.0D);
			List<PlayerEntity> list = this.world.getEntitiesWithinAABB(PlayerEntity.class, axisalignedbb);
	
			for(PlayerEntity playerentity : list) {
				playerentity.addPotionEffect(new EffectInstance(curEffect, duration, amplifier, true, true));
			}
		}
	}

}
