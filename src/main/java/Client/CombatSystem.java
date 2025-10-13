package Client;

import javafx.geometry.Rectangle2D;

public class CombatSystem {
    public static class AttackData {
        public int startupFrames;   // Frames before attack becomes active
        public int activeFrames;    // Frames where attack can hit
        public int recoveryFrames;  // Frames after attack before character can act
        public int damage;
        public int blockstun;       // Frames opponent is stuck blocking
        public int hitstun;         // Frames opponent is stuck in hit animation
        public boolean canCancel;   // Can this attack be canceled into another?
        public double knockback;    // How far the opponent gets pushed back

        public AttackData(int startup, int active, int recovery, int damage) {
            this.startupFrames = startup;
            this.activeFrames = active;
            this.recoveryFrames = recovery;
            this.damage = damage;
            this.blockstun = Math.max(3, active + 2);
            this.hitstun = Math.max(5, damage / 8);
            this.canCancel = false;
            this.knockback = damage / 40.0;
        }

        public AttackData(int startup, int active, int recovery, int damage, boolean canCancel) {
            this(startup, active, recovery, damage);
            this.canCancel = canCancel;
        }

        public int getTotalFrames() {
            return startupFrames + activeFrames + recoveryFrames;
        }
    }

    // Frame data
    private static final java.util.Map<AnimationStateMachine.AnimationType, AttackData> ATTACK_DATA =
            new java.util.HashMap<AnimationStateMachine.AnimationType, AttackData>() {{

                // Light attacks - fast startup, can cancel
                put(AnimationStateMachine.AnimationType.QUICK_PUNCH,
                        new AttackData(3, 2, 6, 12, true)); // Fast jab

                put(AnimationStateMachine.AnimationType.KICK_LOW,
                        new AttackData(5, 3, 8, 18, true)); // Low kick

                // Heavy attacks - slower, more damage
                put(AnimationStateMachine.AnimationType.PUNCH,
                        new AttackData(8, 4, 12, 25, false)); // Heavy punch

                put(AnimationStateMachine.AnimationType.UPPER_KICK,
                        new AttackData(12, 6, 15, 35, false)); // Uppercut kick

                // Air attacks
                put(AnimationStateMachine.AnimationType.AIR_PUNCH,
                        new AttackData(5, 3, 8, 20, false));

                put(AnimationStateMachine.AnimationType.AIR_KICK,
                        new AttackData(8, 5, 12, 28, false));

                put(AnimationStateMachine.AnimationType.PUNCH_DOWN,
                        new AttackData(6, 4, 10, 22, false));
            }};

    public static AttackData getAttackData(AnimationStateMachine.AnimationType attackType) {
        return ATTACK_DATA.get(attackType);
    }

    public static boolean isAttackActive(AnimationStateMachine.AnimationType attackType,
                                         long animationStartTime) {
        AttackData data = getAttackData(attackType);
        if (data == null) return false;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        long frameTime = elapsed / 16; // 60fps

        return frameTime >= data.startupFrames &&
                frameTime < (data.startupFrames + data.activeFrames);
    }

    public static Rectangle2D getAttackHitbox(Entity attacker,
                                              AnimationStateMachine.AnimationType attackType,
                                              boolean facingRight) {
        AttackData data = getAttackData(attackType);
        if (data == null) return new Rectangle2D(0, 0, 0, 0);

        // Base hitbox dimensions
        double hitboxWidth = 40;
        double hitboxHeight = 30;
        double hitboxX = facingRight ? attacker.x + 50 : attacker.x - hitboxWidth - 10;
        double hitboxY = attacker.y + 20;

        // Adjust hitbox based on attack type
        switch (attackType) {
            case QUICK_PUNCH:
                hitboxWidth = 35;
                hitboxHeight = 25;
                hitboxY = attacker.y + 15;
                break;

            case PUNCH:
                hitboxWidth = 50;
                hitboxHeight = 35;
                hitboxY = attacker.y + 10;
                hitboxX = facingRight ? attacker.x + 55 : attacker.x - hitboxWidth - 15;
                break;

            case KICK_LOW:
                hitboxWidth = 45;
                hitboxHeight = 30;
                hitboxY = attacker.y + 50;
                break;

            case UPPER_KICK:
                hitboxWidth = 60;
                hitboxHeight = 40;
                hitboxY = attacker.y + 45;
                hitboxX = facingRight ? attacker.x + 60 : attacker.x - hitboxWidth - 20;
                break;

            case AIR_PUNCH:
                hitboxWidth = 40;
                hitboxHeight = 35;
                hitboxY = attacker.y + 10;
                break;

            case AIR_KICK:
                hitboxWidth = 50;
                hitboxHeight = 30;
                hitboxY = attacker.y + 40;
                hitboxX = facingRight ? attacker.x + 45 : attacker.x - hitboxWidth - 5;
                break;

            case PUNCH_DOWN:
                hitboxWidth = 35;
                hitboxHeight = 40;
                hitboxY = attacker.y + 20;
                break;
        }

        return new Rectangle2D(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    // Attack can be canceled into another attack -> CHECK
    public static boolean canCancelInto(AnimationStateMachine.AnimationType currentAttack,
                                        AnimationStateMachine.AnimationType newAttack) {
        AttackData currentData = getAttackData(currentAttack);
        if (currentData == null || !currentData.canCancel) return false;

        if (currentAttack == AnimationStateMachine.AnimationType.QUICK_PUNCH ||
                currentAttack == AnimationStateMachine.AnimationType.KICK_LOW) {
            return newAttack == AnimationStateMachine.AnimationType.PUNCH ||
                    newAttack == AnimationStateMachine.AnimationType.UPPER_KICK;
        }

        return false;
    }

    // Calculate damage and combo
    public static int calculateScaledDamage(int baseDamage, int comboCount) {
        if (comboCount <= 1) return baseDamage;

        // Damage scaling
        double scalingFactor = Math.max(0.3, 1.0 - (comboCount - 1) * 0.15);
        return (int) (baseDamage * scalingFactor);
    }

    // Attack priority
    public static int getAttackPriority(AnimationStateMachine.AnimationType attackType) {
        switch (attackType) {
            case QUICK_PUNCH:
            case KICK_LOW:
                return 1; // low priority

            case PUNCH:
            case UPPER_KICK:
                return 2; // medium priority


            case AIR_PUNCH:
            case AIR_KICK:
            case PUNCH_DOWN:
                return 2; // Air attacks

            default:
                return 0;
        }
    }

    // Knockback
    public static double getKnockbackForce(AnimationStateMachine.AnimationType attackType) {
        AttackData data = getAttackData(attackType);
        return data != null ? data.knockback : 0;
    }

    // Hitstun
    public static int getHitstunFrames(AnimationStateMachine.AnimationType attackType) {
        AttackData data = getAttackData(attackType);
        return data != null ? data.hitstun : 10;
    }

    // Blockstun
    public static int getBlockstunFrames(AnimationStateMachine.AnimationType attackType) {
        AttackData data = getAttackData(attackType);
        return data != null ? data.blockstun : 5;
    }

    // Startup frames -> CHECK
    public static boolean isInStartup(AnimationStateMachine.AnimationType attackType,
                                      long animationStartTime) {
        AttackData data = getAttackData(attackType);
        if (data == null) return false;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        long frameTime = elapsed / 16;

        return frameTime < data.startupFrames;
    }

    // Recovery frames -> CHECK
    public static boolean isInRecovery(AnimationStateMachine.AnimationType attackType,
                                       long animationStartTime) {
        AttackData data = getAttackData(attackType);
        if (data == null) return false;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        long frameTime = elapsed / 16;

        return frameTime >= (data.startupFrames + data.activeFrames);
    }
}