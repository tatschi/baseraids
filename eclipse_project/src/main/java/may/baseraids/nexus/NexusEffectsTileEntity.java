package may.baseraids.nexus;

import java.util.Arrays;
import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.config.ConfigOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * This class handles sound and effects from the nexus.
 * 
 * @author Natascha May
 */
public class NexusEffectsTileEntity extends TileEntity implements ITickableTileEntity {

	NexusEffects.NexusEffect curEffect = null;
	double effectDistance = 30D;

	/**
	 * For each raid level, this list holds a list of effects that will be applied
	 * to nearby players.
	 */
	List<List<NexusEffects.NexusEffect>> effects = Arrays.asList(Arrays.asList(NexusEffects.SPEEDBUFF_1),
			Arrays.asList(NexusEffects.SPEEDBUFF_2), Arrays.asList(NexusEffects.SPEEDBUFF_3),
			Arrays.asList(NexusEffects.SPEEDBUFF_4), Arrays.asList(NexusEffects.SPEEDBUFF_4, NexusEffects.HASTEBUFF_1),
			Arrays.asList(NexusEffects.SPEEDBUFF_4, NexusEffects.HASTEBUFF_2),
			Arrays.asList(NexusEffects.SPEEDBUFF_4, NexusEffects.HASTEBUFF_2, NexusEffects.LUCKBUFF));

	public NexusEffectsTileEntity() {
		super(Baseraids.NEXUS_TILE_ENTITY_TYPE.get());
	}

	public void tick() {
		if (world.isRemote) {
			tickOnClient();
		}else {
			tickOnServer();
		}
	}
	
	private void tickOnServer() {
		if (this.world.getGameTime() % 40L == 0L) {
			List<NexusEffects.NexusEffect> curEffects = effects
					.get(Baseraids.baseraidsData.raidManager.getRaidLevel() - 1);
			curEffects.forEach(x -> this.addEffectsToPlayers(NexusEffects.getEffectInstance(x)));
		}		
	}
	
	private void tickOnClient() {
		if (this.world.getGameTime() % 40L == 0L) {
			
			Minecraft instance = Minecraft.getInstance();
			if (Baseraids.baseraidsData.raidManager.isRaidActive()) {
				if (ConfigOptions.enableSoundRaidHeartbeat.get()) {					
					this.world.playSound(instance.player, instance.player.getPosition(), Baseraids.SOUND_RAID_ACTIVE.get(), SoundCategory.BLOCKS, 0.5F, 1.0F);
				}
			} else {
				if (ConfigOptions.enableSoundNexusAmbient.get()) {
					this.world.playSound(instance.player, this.pos, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.25F, 0.5F);
				}
			}
		}

	}

	/**
	 * Adds the effect <code>curEffect</code> to all players in the distance
	 * <code>effectDistance</code>.
	 */
	public void addEffectsToPlayers(EffectInstance effect) {
		if (world.isRemote) {
			return;
		}
		if (effect == null) {
			return;
		}

		AxisAlignedBB axisalignedbb = (new AxisAlignedBB(pos)).grow(effectDistance).expand(0.0D,
				(double) world.getHeight(), 0.0D);

		List<PlayerEntity> list = world.getEntitiesWithinAABB(PlayerEntity.class, axisalignedbb);
		for (PlayerEntity playerentity : list) {
			playerentity.addPotionEffect(effect);
		}
	}

}
