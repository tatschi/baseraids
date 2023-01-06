package may.baseraids.entities.ai;

import javax.annotation.Nullable;

import may.baseraids.RaidManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

/**
 * This class defines a custom arrow entity that interacts with blocks that it hits.
 * Blocks that are hit by these arrows during a raid add breaking progress for the block.
 * 
 * @author Natascha May
 */
public class RaidArrowEntity extends ArrowEntity {

	Entity shooter;
	RaidManager raidManager;
	private static final int DEFAULT_DAMAGE = 25;
	private int blockBreakDamage;

	public RaidArrowEntity(World world, LivingEntity shooter, RaidManager raidManager) {
		super(world, shooter);
		this.raidManager = raidManager;
		this.blockBreakDamage = DEFAULT_DAMAGE;
	}

	/**
	 * Called when the arrow entity collides with a block.
	 * Adds breaking progress to the block and reduces the future damage of the arrow.
	 */
	@Override
	protected void func_230299_a_(BlockRayTraceResult rayTraceResult) {
		super.func_230299_a_(rayTraceResult);
		if (!raidManager.isRaidActive()) {
			return;
		}
 		raidManager.globalBlockBreakProgressMng.addProgress(rayTraceResult.getPos(), blockBreakDamage);
		// the damage should decrease with every hit in order to disable infinite loops with falling arrows 
		blockBreakDamage /= 2;		
	}

	@Override
	public void setShooter(@Nullable Entity entityIn) {
		super.setShooter(entityIn);
		shooter = entityIn;
	}
	
	@Override
	public boolean equals(Object object) {
		if(!super.equals(object)) {
			return false;
		}
		if(!(object instanceof RaidArrowEntity)){
			return false;
		}
		RaidArrowEntity entity = (RaidArrowEntity) object;		
		if(!shooter.equals(entity.shooter)) {
			return false;
		}
		if(!raidManager.equals(entity.raidManager)) {
			return false;
		}
		return blockBreakDamage == entity.blockBreakDamage;
	}
	
	@Override
	  public int hashCode() {
	    //TODO
		return 0;
	  }

}
