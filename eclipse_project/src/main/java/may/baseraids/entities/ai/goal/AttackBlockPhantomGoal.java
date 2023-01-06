package may.baseraids.entities.ai.goal;

import java.util.Random;

import may.baseraids.RaidManager;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class AttackBlockPhantomGoal extends AttackBlockGoal<PhantomEntity> {

	private int tickDelay;
	private Random rand = new Random();
	protected static final int PHANTOM_DAMAGE = 10;

	public AttackBlockPhantomGoal(PhantomEntity entity, RaidManager raidManager) {
		super(entity, raidManager);
	}

	@Override
	public boolean shouldExecute() {
		if (!raidManager.isRaidActive()) {
			return false;
		}

		if (entity.getAttackTarget() != null) {
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
	public void startExecuting() {
		entity.attackPhase = PhantomEntity.AttackPhase.CIRCLE;
	}

	public void attackBlockPhantom() {
		if(entity.attackPhase == PhantomEntity.AttackPhase.CIRCLE) {
			pickAttack();
		}
		if(entity.attackPhase == PhantomEntity.AttackPhase.SWOOP) {
			sweepAttack();
		}		
	}
	
	private void pickAttack() {
		if (entity.attackPhase == PhantomEntity.AttackPhase.CIRCLE) {
			--this.tickDelay;
			if (this.tickDelay <= 0) {
				entity.attackPhase = PhantomEntity.AttackPhase.SWOOP;
				this.func_203143_f();
				this.tickDelay = 80 + rand.nextInt(1200);
				entity.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 10.0F, 0.95F + rand.nextFloat() * 0.1F);
			}
		}
	}
	
	private void sweepAttack() {		
        entity.orbitOffset = new Vector3d(target.getX(), target.getY(), target.getZ());
        AxisAlignedBB originalBB = entity.getBoundingBox();
        entity.setBoundingBox(originalBB.grow(0.2F));
        if (entity.collidedHorizontally && isBlockInMeleeRange(target)) {
           
           boolean wasBroken = raidManager.globalBlockBreakProgressMng.addProgress(target, PHANTOM_DAMAGE);		
			if(wasBroken) {
				target = null;
			}				
			entity.attackPhase = PhantomEntity.AttackPhase.CIRCLE;
           if (!entity.isSilent()) {
        	   entity.world.playEvent(1039, entity.getPosition(), 0);
           }
           resetAttack();
           return;           
        }
        entity.setBoundingBox(originalBB);
        if (entity.hurtTime > 0) {
        	entity.attackPhase = PhantomEntity.AttackPhase.CIRCLE;
        }
	}
	
	private void resetAttack() {
		entity.orbitPosition = entity.getHomePosition();
		entity.orbitOffset = Vector3d.ZERO;
	}
	
	@Override
	protected void attackTarget() {
		attackBlockPhantom();
	}

	private void func_203143_f() {
		entity.orbitPosition = target.up(20 + rand.nextInt(20));
		if (entity.orbitPosition.getY() < entity.world.getSeaLevel()) {
			entity.orbitPosition = new BlockPos(entity.orbitPosition.getX(), entity.world.getSeaLevel() + 1,
					entity.orbitPosition.getZ());
		}

	}

}
