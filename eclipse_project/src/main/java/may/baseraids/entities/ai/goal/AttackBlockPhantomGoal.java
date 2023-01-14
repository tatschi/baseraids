package may.baseraids.entities.ai.goal;

import java.util.Random;

import com.mojang.math.Vector3d;

import may.baseraids.RaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AttackBlockPhantomGoal extends AttackBlockGoal<Phantom> {

	private int tickDelay;
	private Random rand = new Random();
	protected static final int PHANTOM_DAMAGE = 10;

	public AttackBlockPhantomGoal(Phantom entity, RaidManager raidManager) {
		super(entity, raidManager);
	}

	@Override
	public boolean canUse() {
		if (!raidManager.isRaidActive()) {
			return false;
		}

		if (entity.getTarget() != null) {
			return false;
		}

		if (findTargetTicks > 2 * FIND_TARGET_MAX_TICKS) {
			findTargetTicks = 0;
			return false;
		}

		if (findTargetTicks > FIND_TARGET_MAX_TICKS) {
			findTargetTicks++;
			return false;
		}

		return true;
	}

	@Override
	public void start() {
		entity.attackPhase = Phantom.AttackPhase.CIRCLE;
	}

	public void attackBlockPhantom() {
		if(entity.attackPhase == Phantom.AttackPhase.CIRCLE) {
			pickAttack();
		}
		if(entity.attackPhase == Phantom.AttackPhase.SWOOP) {
			sweepAttack();
		}
	}
	
	private void pickAttack() {
		if (entity.attackPhase == Phantom.AttackPhase.CIRCLE) {
			--this.tickDelay;
			if (this.tickDelay <= 0) {
				entity.attackPhase = Phantom.AttackPhase.SWOOP;
				this.setOrbitPositionForSwoopPhase();
				this.tickDelay = 80 + rand.nextInt(1200);
				entity.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + rand.nextFloat() * 0.1F);
			}
		}
	}
	
	private void sweepAttack() {		
        entity.moveTargetPoint = new Vec3(target.getX(), target.getY(), target.getZ());
        AABB originalBB = entity.getBoundingBox();
        entity.setBoundingBox(originalBB.inflate(0.2F));
        if (entity.horizontalCollision && touchingTarget()) {
           
           boolean wasBroken = raidManager.globalBlockBreakProgressMng.addProgress(target, PHANTOM_DAMAGE);		
			if(wasBroken) {
				target = null;
			}				
			entity.attackPhase = Phantom.AttackPhase.CIRCLE;
           if (!entity.isSilent()) {
        	   entity.level.levelEvent(1039, entity.blockPosition(), 0);
           }
           resetAttack();
           return;           
        }
        entity.setBoundingBox(originalBB);
        if (entity.hurtTime > 0) {
        	entity.attackPhase = Phantom.AttackPhase.CIRCLE;
        }
	}
	
  
	
	private void resetAttack() {
	}
	
	@Override
	protected void attackTarget() {
		attackBlockPhantom();
	}

	// copied from PickAttackGoal#func_203143_f
	private void setOrbitPositionForSwoopPhase() {
		entity.anchorPoint = target.above(20 + rand.nextInt(20));
		if (entity.anchorPoint.getY() < entity.level.getSeaLevel()) {
			entity.anchorPoint = new BlockPos(entity.anchorPoint.getX(), entity.level.getSeaLevel() + 1,
					entity.anchorPoint.getZ());
		}

	}
	
	protected boolean touchingTarget() {
        return entity.moveTargetPoint.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4.0D;
     }

}

