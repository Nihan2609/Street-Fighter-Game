package Client;

import javafx.scene.image.Image;

public class AnimationStateMachine {
    public enum AnimationType {
        // Basic states
        IDLE, PARRY_FORWARD, PARRY_BACKWARD, PARRY_F, PARRY_B, CROUCH, JUMP,

        // Ground attacks
        PUNCH, QUICK_PUNCH, CROUCH_PUNCH, CROUCH_ATTACK, UPPERCUT, KICK_LOW, UPPER_KICK,

        // Air attacks
        AIR_PUNCH, AIR_KICK, PUNCH_DOWN,

        // Movement
        FRONT_FLIP, BACK_FLIP,

        // Defensive/Hurt states
        HIT_STAND, HIT_STAND_BACK, CROUCH_HIT, CROUCH_HIT_BACK,
        KNOCKBACK, RECOVER, WIN, DEAD
    }

    public static class AnimationState {
        public AnimationType type;
        public int frameCount;
        public int currentFrame;
        public long frameStartTime;
        public int frameDelay; // milliseconds per frame
        public boolean canBeInterrupted;
        public boolean loops;
        public AnimationType nextState;
        public boolean isFinished;

        public AnimationState(AnimationType type, int frameCount, int frameDelay,
                              boolean canBeInterrupted, boolean loops) {
            this.type = type;
            this.frameCount = frameCount;
            this.frameDelay = frameDelay;
            this.canBeInterrupted = canBeInterrupted;
            this.loops = loops;
            this.currentFrame = 0;
            this.frameStartTime = System.currentTimeMillis();
            this.nextState = AnimationType.IDLE;
            this.isFinished = false;
        }

        public void reset() {
            this.currentFrame = 0;
            this.frameStartTime = System.currentTimeMillis();
            this.isFinished = false;
        }
    }

    public AnimationState currentState;
    private final String characterName;
    private final AssetManager assetManager;
    private final boolean isRyu; // true for Ryu, false for Ken

    public AnimationStateMachine(String characterName) {
        this.characterName = characterName;
        this.assetManager = AssetManager.getInstance();
        this.isRyu = characterName.toUpperCase().equals("RYU");

        // Ensure AssetManager is initialized
        if (!assetManager.isInitialized()) {
            assetManager.initialize();
        }

        transition(AnimationType.IDLE, true);
    }

    /**
     * Transitions to a new animation state with improved logic
     */
    public void transition(AnimationType newType, boolean force) {
        // If we're already in the requested state and not forcing a restart, do nothing.
        if (currentState != null && currentState.type == newType && !force && !currentState.isFinished) {
            return;
        }

        // If current state cannot be interrupted and we're not forcing, block transition.
        if (!force && currentState != null && !currentState.canBeInterrupted && !currentState.isFinished) {
            return;
        }

        AnimationState newState = createAnimationState(newType);
        if (newState != null) {
            currentState = newState;
        }
    }

    private AnimationState createAnimationState(AnimationType type) {
        // Get the actual frame count from the asset manager to ensure accuracy
        int actualFrameCount = getActualFrameCount(type);

        switch (type) {
            // Basic movement - match actual sprite frame counts
            case IDLE:
                return new AnimationState(type, actualFrameCount, 150, true, true);

            case PARRY_FORWARD:
            case PARRY_F:
                return new AnimationState(type, actualFrameCount, 120, true, true);

            case PARRY_BACKWARD:
            case PARRY_B:
                return new AnimationState(type, actualFrameCount, 120, true, true);

            case CROUCH:
                return new AnimationState(type, 1, 100, true, true);

            case JUMP:
                AnimationState jump = new AnimationState(type, actualFrameCount, 80, false, false);
                jump.nextState = AnimationType.IDLE;
                return jump;

            // Flips
            case FRONT_FLIP:
            case BACK_FLIP:
                AnimationState flip = new AnimationState(type, actualFrameCount, 120, false, false);
                flip.nextState = AnimationType.IDLE;
                return flip;

            // Ground attacks - frame counts matching your sprites
            case PUNCH:
                AnimationState punch = new AnimationState(type, actualFrameCount, 100, false, false);
                punch.nextState = AnimationType.IDLE;
                return punch;

            case QUICK_PUNCH:
                AnimationState qPunch = new AnimationState(type, actualFrameCount, 80, false, false);
                qPunch.nextState = AnimationType.IDLE;
                return qPunch;

            case CROUCH_PUNCH:
                AnimationState cPunch = new AnimationState(type, actualFrameCount, 80, false, false);
                cPunch.nextState = AnimationType.CROUCH;
                return cPunch;

            case CROUCH_ATTACK:
                AnimationState cAttack = new AnimationState(type, actualFrameCount, 90, false, false);
                cAttack.nextState = AnimationType.CROUCH;
                return cAttack;

            case UPPERCUT:
                AnimationState uppercut = new AnimationState(type, actualFrameCount, 120, false, false);
                uppercut.nextState = AnimationType.IDLE;
                return uppercut;

            case KICK_LOW:
                AnimationState lowKick = new AnimationState(type, actualFrameCount, 120, false, false);
                lowKick.nextState = AnimationType.IDLE;
                return lowKick;

            case UPPER_KICK:
                AnimationState upperKick = new AnimationState(type, actualFrameCount, 100, false, false);
                upperKick.nextState = AnimationType.IDLE;
                return upperKick;

            // Air attacks
            case AIR_PUNCH:
                AnimationState airPunch = new AnimationState(type, actualFrameCount, 100, false, false);
                airPunch.nextState = AnimationType.JUMP;
                return airPunch;

            case AIR_KICK:
                AnimationState airKick = new AnimationState(type, actualFrameCount, 120, false, false);
                airKick.nextState = AnimationType.JUMP;
                return airKick;

            case PUNCH_DOWN:
                AnimationState punchDown = new AnimationState(type, actualFrameCount, 120, false, false);
                punchDown.nextState = AnimationType.JUMP;
                return punchDown;

            // Hit reactions
            case HIT_STAND:
                AnimationState hitStand = new AnimationState(type, actualFrameCount, 150, false, false);
                hitStand.nextState = AnimationType.IDLE;
                return hitStand;

            case HIT_STAND_BACK:
                AnimationState hitStandBack = new AnimationState(type, actualFrameCount, 120, false, false);
                hitStandBack.nextState = AnimationType.IDLE;
                return hitStandBack;

            case CROUCH_HIT:
                AnimationState crouchHit = new AnimationState(type, actualFrameCount, 150, false, false);
                crouchHit.nextState = AnimationType.CROUCH;
                return crouchHit;

            case CROUCH_HIT_BACK:
                AnimationState crouchHitBack = new AnimationState(type, actualFrameCount, 120, false, false);
                crouchHitBack.nextState = AnimationType.CROUCH;
                return crouchHitBack;

            case KNOCKBACK:
                AnimationState knockback = new AnimationState(type, actualFrameCount, 150, false, false);
                knockback.nextState = AnimationType.RECOVER;
                return knockback;

            case RECOVER:
                AnimationState recover = new AnimationState(type, actualFrameCount, 120, false, false);
                recover.nextState = AnimationType.IDLE;
                return recover;

            // End states
            case WIN:
                AnimationState win = new AnimationState(type, actualFrameCount, 150, false, false);
                win.nextState = null; // Stay in win state
                return win;

            case DEAD:
                return new AnimationState(type, 1, 100, false, true);

            default:
                return new AnimationState(AnimationType.IDLE, getActualFrameCount(AnimationType.IDLE), 150, true, true);
        }
    }

    /**
     * Gets the actual frame count from the loaded sprites
     */
    private int getActualFrameCount(AnimationType type) {
        String animName = getAnimationName(type);
        Image[] animation = assetManager.getAnimation(characterName, animName);

        if (animation != null && animation.length > 0) {
            return animation.length;
        }

        // Fallback default frame counts
        switch (type) {
            case IDLE: return 6;
            case PARRY_FORWARD:
            case PARRY_BACKWARD:
            case PARRY_F:
            case PARRY_B: return 8;
            case CROUCH: return 1;
            case JUMP: return 11;
            case FRONT_FLIP:
            case BACK_FLIP: return 8;
            case PUNCH: return isRyu ? 6 : 8;
            case QUICK_PUNCH: return isRyu ? 3 : 4;
            case CROUCH_PUNCH: return 3;
            case CROUCH_ATTACK: return isRyu ? 8 : 5;
            case UPPERCUT: return 8;
            case KICK_LOW: return 5;
            case UPPER_KICK: return isRyu ? 9 : 10;
            case AIR_PUNCH: return isRyu ? 6 : 7;
            case AIR_KICK: return isRyu ? 5 : 6;
            case PUNCH_DOWN: return 4;
            case HIT_STAND: return 2;
            case HIT_STAND_BACK: return 4;
            case CROUCH_HIT: return 2;
            case CROUCH_HIT_BACK: return 4;
            case KNOCKBACK: return 4;
            case RECOVER: return 5;
            case WIN: return 10;
            case DEAD: return 1;
            default: return 1;
        }
    }

    public Image getCurrentFrame() {
        if (currentState == null) return null;

        long currentTime = System.currentTimeMillis();
        long timeSinceStart = currentTime - currentState.frameStartTime;
        int targetFrame = (int) (timeSinceStart / currentState.frameDelay);

        if (currentState.loops) {
            currentState.currentFrame = targetFrame % currentState.frameCount;
        } else {
            if (targetFrame >= currentState.frameCount) {
                currentState.currentFrame = currentState.frameCount - 1;
                currentState.isFinished = true;

                // Auto-transition to next state if specified
                if (currentState.nextState != null && currentState.nextState != currentState.type) {
                    transition(currentState.nextState, true);
                    return getCurrentFrame();
                }
            } else {
                currentState.currentFrame = targetFrame;
            }
        }

        String animName = getAnimationName(currentState.type);
        Image[] animation = assetManager.getAnimation(characterName, animName);

        if (animation != null && animation.length > 0 && currentState.currentFrame < animation.length) {
            return animation[currentState.currentFrame];
        }

        if (animation != null && animation.length > 0) {
            return animation[0];
        }

        return null;
    }

    private String getAnimationName(AnimationType type) {
        switch (type) {
            case IDLE: return "idle";
            case PARRY_FORWARD:
            case PARRY_F: return "parry_f";
            case PARRY_BACKWARD:
            case PARRY_B: return "parry_b";
            case CROUCH: return "crouch";
            case JUMP: return "jump";
            case FRONT_FLIP: return "front_flip";
            case BACK_FLIP: return "back_flip";
            case PUNCH: return "punch";
            case QUICK_PUNCH: return "quick_punch";
            case CROUCH_PUNCH: return "crouch_punch";
            case CROUCH_ATTACK: return "crouch_attack";
            case UPPERCUT: return "uppercut";
            case KICK_LOW: return "kick_low";
            case UPPER_KICK: return "upper_kick";
            case AIR_PUNCH: return "air_punch";
            case AIR_KICK: return "air_kick";
            case PUNCH_DOWN: return "punch_down";
            case HIT_STAND: return "hit_stand";
            case HIT_STAND_BACK: return "hit_stand_back";
            case CROUCH_HIT: return "crouch_hit";
            case CROUCH_HIT_BACK: return "crouch_hit_back";
            case KNOCKBACK: return "knockback";
            case RECOVER: return "recover";
            case WIN: return "win";
            case DEAD: return "dead";
            default: return "idle";
        }
    }

    public AnimationType getCurrentAnimationType() {
        return currentState != null ? currentState.type : AnimationType.IDLE;
    }

    public boolean isAnimationFinished() {
        return currentState != null && currentState.isFinished;
    }

    public boolean canInterrupt() {
        return currentState == null || currentState.canBeInterrupted || currentState.isFinished;
    }

    public int getCurrentFrameIndex() {
        return currentState != null ? currentState.currentFrame : 0;
    }

    public boolean isInFrameRange(int startFrame, int endFrame) {
        if (currentState == null) return false;
        return currentState.currentFrame >= startFrame &&
                currentState.currentFrame <= endFrame;
    }

    /**
     * Check if the animation is in active attack frames (for combat system integration)
     */
    public boolean isInActiveFrames() {
        if (currentState == null) return false;

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(currentState.type);
        if (attackData == null) return false;

        return CombatSystem.isAttackActive(currentState.type, currentState.frameStartTime);
    }

    /**
     * Get the current animation's total duration in milliseconds
     */
    public long getAnimationDuration() {
        if (currentState == null) return 0;
        return (long) currentState.frameCount * currentState.frameDelay;
    }

    /**
     * Reset current animation to frame 0
     */
    public void resetCurrentAnimation() {
        if (currentState != null) {
            currentState.reset();
        }
    }

    /**
     * Force finish current animation
     */
    public void finishCurrentAnimation() {
        if (currentState != null && !currentState.loops) {
            currentState.currentFrame = currentState.frameCount - 1;
            currentState.isFinished = true;
        }
    }
}