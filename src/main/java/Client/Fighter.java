package Client;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class Fighter extends Entity {
    // Core stats
    private int health;
    private int maxHealth = 1000;
    private String characterName;
    private boolean facingRight;

    // Physics
    private double velX, velY;
    private final double GRAVITY = 0.6;
    private final double JUMP_FORCE = -14;
    private final double MOVE_SPEED = 4;
    private final double AIR_MOVE_SPEED = 2.5;
    private final double GROUND_Y = 270;
    private boolean onGround = true;

    // Systems
    AnimationStateMachine animationSM;
    private InputManager inputManager;
    private String playerId;

    // Combat state
    private int blockstunTimer = 0;
    private int hitstunTimer = 0;
    private boolean invulnerable = false;
    private long invulnerabilityEnd = 0;
    private int comboCount = 0;

    // Special move
    private boolean canCancelAttack = false;
    private long lastAttackTime = 0;

    // Jump state
    private boolean jumpInitiated = false;
    private boolean canPerformAirAction = true;

    // Attack state tracking
    private boolean isPerformingGroundAttack = false;
    private long attackStartTime = 0;

    public Fighter(String characterName, float startX, float startY, String playerId, boolean facingRight) {
        super(startX, startY);
        this.characterName = characterName.toUpperCase();
        this.playerId = playerId;
        this.facingRight = facingRight;
        this.health = maxHealth;

        AssetManager assetManager = AssetManager.getInstance();
        if (!assetManager.isInitialized()) {
            assetManager.initialize();
        }

        this.animationSM = new AnimationStateMachine(this.characterName);
        this.inputManager = InputManager.getInstance();

        // Position on ground
        this.y = (float) GROUND_Y;
    }

    @Override
    public void tick() {
        if (isDead()) {
            y = (float) GROUND_Y;
            velX = 0;
            velY = 0;
            onGround = true;
        } else if (animationSM.getCurrentAnimationType() == AnimationStateMachine.AnimationType.WIN) {
            y = (float) GROUND_Y;
            velX = 0;
            velY = 0;
            onGround = true;
        }
        updatePhysics();
        updateCombatState();
        processInput();
        updateAnimations();
    }

    private void updatePhysics() {
        if (isDead() || animationSM.getCurrentAnimationType() == AnimationStateMachine.AnimationType.WIN) {
            y = (float) GROUND_Y;
            velX = 0;
            velY = 0;
            onGround = true;
            return;
        }
        // applying movement
        x += (float) velX;
        y += (float) velY;

        // Gravity
        if (y < GROUND_Y) {
            velY += GRAVITY;
            onGround = false;
        } else {
            y = (float) GROUND_Y;
            velY = 0;

            if (!onGround) {
                // now iam on ground
                onGround = true;
                jumpInitiated = false;
                canPerformAirAction = true;
                isPerformingGroundAttack = false;

                // Back to idle
                AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();
                if (currentAnim == AnimationStateMachine.AnimationType.JUMP ||
                        currentAnim == AnimationStateMachine.AnimationType.AIR_PUNCH ||
                        currentAnim == AnimationStateMachine.AnimationType.AIR_KICK ||
                        currentAnim == AnimationStateMachine.AnimationType.PUNCH_DOWN ||
                        currentAnim == AnimationStateMachine.AnimationType.FRONT_FLIP ||
                        currentAnim == AnimationStateMachine.AnimationType.BACK_FLIP) {
                    animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
                }
            }
        }

        // Air math
        if (!onGround) {
            velX *= 0.95;
        } else {
            // Ground friction
            if (!isMovementInputPressed() || isPerformingGroundAttack) {
                velX *= 0.85;
                if (Math.abs(velX) < 0.1) velX = 0;
            }
        }

        // Boundary checks
        if (x < 0) {
            x = 0;
            velX = Math.max(0, velX);
        }
        if (x > 740) {
            x = 740;
            velX = Math.min(0, velX);
        }
    }

    private void updateCombatState() {
        if (isPerformingGroundAttack && onGround) {
            AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

            if (animationSM.isAnimationFinished() ||
                    currentAnim == AnimationStateMachine.AnimationType.IDLE ||
                    currentAnim == AnimationStateMachine.AnimationType.PARRY_FORWARD ||
                    currentAnim == AnimationStateMachine.AnimationType.PARRY_BACKWARD) {
                isPerformingGroundAttack = false;
            }
        }

        if (blockstunTimer > 0) {
            blockstunTimer--;
            if (blockstunTimer == 0) {
                animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
            }
        }

        if (hitstunTimer > 0) {
            hitstunTimer--;
            if (hitstunTimer == 0) {
                animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
                comboCount = 0;
            }
        }

        if (invulnerable && System.currentTimeMillis() > invulnerabilityEnd) {
            invulnerable = false;
        }

        if (canCancelAttack && (System.currentTimeMillis() - lastAttackTime) > 300) {
            canCancelAttack = false;
        }
    }

    private void updateAnimations() {
        animationSM.getCurrentFrame();
    }

    private boolean isMovementInputPressed() {
        return inputManager.isActionPressed(playerId, "left") ||
                inputManager.isActionPressed(playerId, "right");
    }

    private void processInput() {
        if (hitstunTimer > 0 || blockstunTimer > 0) return;
        if (isDead()) return;

        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (!onGround) {
            handleAirborneInput();
        } else {
            handleGroundInput(currentAnim);
        }
    }

    private void handleAirborneInput() {
        boolean upPressed = inputManager.isActionPressed(playerId, "up");
        boolean forwardPressed = inputManager.isActionPressed(playerId, facingRight ? "right" : "left");
        boolean backwardPressed = inputManager.isActionPressed(playerId, facingRight ? "left" : "right");

        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // while in jump state I can do flip
        if (canPerformAirAction && jumpInitiated && currentAnim == AnimationStateMachine.AnimationType.JUMP) {
            if (forwardPressed) {
                performFrontFlip();
                return;
            } else if (backwardPressed) {
                performBackFlip();
                return;
            }
        }

        // Air attacks
        if (canPerformAirAction && (currentAnim == AnimationStateMachine.AnimationType.JUMP ||
                currentAnim == AnimationStateMachine.AnimationType.FRONT_FLIP ||
                currentAnim == AnimationStateMachine.AnimationType.BACK_FLIP)) {

            if (inputManager.isActionPressed(playerId, "light_punch") ||
                    inputManager.isActionPressed(playerId, "heavy_punch")) {

                if (inputManager.isActionPressed(playerId, "down")) {
                    performAirAttack(AnimationStateMachine.AnimationType.PUNCH_DOWN);
                } else {
                    performAirAttack(AnimationStateMachine.AnimationType.AIR_PUNCH);
                }
            } else if (inputManager.isActionPressed(playerId, "light_kick") ||
                    inputManager.isActionPressed(playerId, "heavy_kick")) {
                performAirAttack(AnimationStateMachine.AnimationType.AIR_KICK);
            }
        }

        // Air movement
        if (inputManager.isActionPressed(playerId, "left")) {
            velX -= 0.2;
            velX = Math.max(-AIR_MOVE_SPEED, velX);
        } else if (inputManager.isActionPressed(playerId, "right")) {
            velX += 0.2;
            velX = Math.min(AIR_MOVE_SPEED, velX);
        }
    }

    private void handleGroundInput(AnimationStateMachine.AnimationType currentAnim) {
        if (inputManager.isActionPressed(playerId, "up") && animationSM.canInterrupt()) {
            jump();
            return;
        }

        // Ground attacks
        handleGroundAttackInput();

        if (!isPerformingGroundAttack) {
            handleMovementInput(currentAnim);
        }

        // Block
        handleBlockInput(currentAnim);
    }

    private void handleMovementInput(AnimationStateMachine.AnimationType currentAnim) {
        boolean leftPressed = inputManager.isActionPressed(playerId, "left");
        boolean rightPressed = inputManager.isActionPressed(playerId, "right");

        if (leftPressed && !rightPressed) {
            moveLeft();
        } else if (rightPressed && !leftPressed) {
            moveRight();
        } else {
            stopMoving();
        }
    }

    private void handleBlockInput(AnimationStateMachine.AnimationType currentAnim) {
        boolean blockPressed = inputManager.isActionPressed(playerId, "block");

        if (blockPressed && animationSM.canInterrupt()) {
            block();
        } else if (!blockPressed && currentAnim == AnimationStateMachine.AnimationType.PARRY_B) {
            stand();
        }
    }

    private void handleGroundAttackInput() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (animationSM.canInterrupt()) {
            if (inputManager.isActionPressed(playerId, "light_punch")) {
                performAttack(AnimationStateMachine.AnimationType.QUICK_PUNCH);
            } else if (inputManager.isActionPressed(playerId, "heavy_punch")) {
                if (inputManager.isActionPressed(playerId, "down")) {
                    performAttack(AnimationStateMachine.AnimationType.UPPERCUT);
                } else {
                    performAttack(AnimationStateMachine.AnimationType.PUNCH);
                }
            } else if (inputManager.isActionPressed(playerId, "light_kick")) {
                performAttack(AnimationStateMachine.AnimationType.KICK_LOW);
            } else if (inputManager.isActionPressed(playerId, "heavy_kick")) {
                performAttack(AnimationStateMachine.AnimationType.UPPER_KICK);
            }
        }
    }

    private void moveLeft() {
        if (isPerformingGroundAttack) {
            velX = 0;
            return;
        }

        if (animationSM.canInterrupt() && onGround) {
            velX = -MOVE_SPEED;

            AnimationStateMachine.AnimationType desired = facingRight
                    ? AnimationStateMachine.AnimationType.PARRY_BACKWARD
                    : AnimationStateMachine.AnimationType.PARRY_FORWARD;

            if (animationSM.getCurrentAnimationType() != desired) {
                animationSM.transition(desired, false);
            }
        }
    }

    private void moveRight() {
        if (isPerformingGroundAttack) {
            velX = 0;
            return;
        }

        if (animationSM.canInterrupt() && onGround) {
            velX = MOVE_SPEED;

            AnimationStateMachine.AnimationType desired = facingRight
                    ? AnimationStateMachine.AnimationType.PARRY_FORWARD
                    : AnimationStateMachine.AnimationType.PARRY_BACKWARD;

            if (animationSM.getCurrentAnimationType() != desired) {
                animationSM.transition(desired, false);
            }
        }
    }

    private void stopMoving() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();
        if (currentAnim == AnimationStateMachine.AnimationType.PARRY_FORWARD ||
                currentAnim == AnimationStateMachine.AnimationType.PARRY_BACKWARD) {
            animationSM.transition(AnimationStateMachine.AnimationType.IDLE, false);
        }
    }

    private void jump() {
        if (onGround && animationSM.canInterrupt()) {
            velY = JUMP_FORCE;
            onGround = false;
            jumpInitiated = true;
            canPerformAirAction = true;

            if (inputManager.isActionPressed(playerId, "left")) {
                velX = -MOVE_SPEED * 0.6;
            } else if (inputManager.isActionPressed(playerId, "right")) {
                velX = MOVE_SPEED * 0.6;
            }

            animationSM.transition(AnimationStateMachine.AnimationType.JUMP, true);
            AudioManager.playJumpSound();
        }
    }

    private void performFrontFlip() {
        if (canPerformAirAction) {
            animationSM.transition(AnimationStateMachine.AnimationType.FRONT_FLIP, true);
            velX = facingRight ? 5 : -5;
            velY = Math.min(velY, -4);
            canPerformAirAction = false;
        }
    }

    private void performBackFlip() {
        if (canPerformAirAction) {
            animationSM.transition(AnimationStateMachine.AnimationType.BACK_FLIP, true);
            velX = facingRight ? -5 : 5;
            velY = Math.min(velY, -4);
            canPerformAirAction = false;
        }
    }

    private void performAirAttack(AnimationStateMachine.AnimationType attackType) {
        if (canPerformAirAction) {
            animationSM.transition(attackType, true);
            canPerformAirAction = false;
            lastAttackTime = System.currentTimeMillis();

            velX *= 0.5;

            if (attackType == AnimationStateMachine.AnimationType.PUNCH_DOWN) {
                velY = Math.max(velY, 2);
            } else {
                velY *= 0.7;
            }

            if (attackType == AnimationStateMachine.AnimationType.AIR_KICK) {
                AudioManager.playKickSound();
            } else {
                AudioManager.playPunchSound();
            }
        }
    }

    private void stand() {
        if (animationSM.canInterrupt()) {
            animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
        }
    }

    private void block() {
        animationSM.transition(AnimationStateMachine.AnimationType.PARRY_B, false);
        velX = 0;
    }

    private void performAttack(AnimationStateMachine.AnimationType attackType) {
        isPerformingGroundAttack = true;
        velX = 0;
        attackStartTime = System.currentTimeMillis();

        animationSM.transition(attackType, true);
        lastAttackTime = System.currentTimeMillis();

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        canCancelAttack = attackData != null && attackData.canCancel;

        switch (attackType) {
            case PUNCH:
            case QUICK_PUNCH:
                AudioManager.playPunchSound();
                break;
            case KICK_LOW:
            case UPPER_KICK:
                AudioManager.playKickSound();
                break;
            default:
                AudioManager.playPunchSound();
                break;
        }
    }

    @Override
    public void render(GraphicsContext g) {
        Image currentFrame = animationSM.getCurrentFrame();

        if (currentFrame == null) {
            return;
        }

        //shadow
        g.setFill(Color.rgb(0, 0, 0, 0.3));
        g.fillOval(x + 10, GROUND_Y + 90, 40, 8);

        // Draw character
        if (!facingRight) {
            g.save();
            g.scale(-1, 1);
            g.drawImage(currentFrame, -x - currentFrame.getWidth(), y);
            g.restore();
        } else {
            g.drawImage(currentFrame, x, y);
        }
    }

    public void takeDamage(int damage, AnimationStateMachine.AnimationType attackType) {
        if (invulnerable || isDead()) return;

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (currentAnim == AnimationStateMachine.AnimationType.PARRY_B) {
            damage = (int)(damage * 0.25);
            blockstunTimer = attackData != null ? attackData.blockstun : 10;
        } else {
            damage = CombatSystem.calculateScaledDamage(damage, comboCount);
            comboCount++;

            AnimationStateMachine.AnimationType hitAnim;
            if (damage >= 100) {
                hitAnim = AnimationStateMachine.AnimationType.HIT_STAND_BACK;
            } else {
                hitAnim = AnimationStateMachine.AnimationType.HIT_STAND;
            }

            animationSM.transition(hitAnim, true);
            hitstunTimer = attackData != null ? attackData.hitstun : 15;

            double knockback = CombatSystem.getKnockbackForce(attackType);
            velX += facingRight ? -knockback : knockback;

            if (!onGround) {
                canPerformAirAction = false;
            }

            isPerformingGroundAttack = false;
        }

        health -= damage;
        if (health <= 0) {
            health = 0;
            die();

            y = (float)  GROUND_Y;
            velX = 0;
            velY = 0;
            onGround = true;
        }
        AudioManager.playHitSound();
    }

    private void die() {
        animationSM.transition(AnimationStateMachine.AnimationType.DEAD, true);
        isPerformingGroundAttack = false;
        velX = 0;
        velY = 0;
        y = (float) GROUND_Y;
        onGround = true;

        jumpInitiated = false;
        canPerformAirAction = false;
        AudioManager.playDeathSound();
    }

    public boolean isAttacking() {
        AnimationStateMachine.AnimationType currentType = animationSM.getCurrentAnimationType();
        return CombatSystem.getAttackData(currentType) != null;
    }

    public Rectangle2D getAttackHitbox() {
        if (!isAttacking() || !animationSM.isInActiveFrames()) {
            return new Rectangle2D(0, 0, 0, 0);
        }

        AnimationStateMachine.AnimationType currentType = animationSM.getCurrentAnimationType();
        return CombatSystem.getAttackHitbox(this, currentType, facingRight);
    }

    public Rectangle2D getHurtbox() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (currentAnim == AnimationStateMachine.AnimationType.JUMP) {
            return new Rectangle2D(x + 10, y + 10, 40, 80);
        }

        return new Rectangle2D(x + 10, y + 10, 40, 90);
    }

    public boolean canBeHit(Fighter attacker) {
        if (invulnerable || hitstunTimer > 0 || isDead()) return false;
        if (attacker == this) return false;

        Rectangle2D attackHitbox = attacker.getAttackHitbox();
        Rectangle2D myHurtbox = getHurtbox();

        return attackHitbox.intersects(myHurtbox) &&
                attacker.animationSM.isInActiveFrames();
    }

    public void faceOpponent(Fighter opponent) {
        if (animationSM.canInterrupt() && opponent != null) {
            boolean shouldFaceRight = (opponent.x > this.x);
            if (facingRight != shouldFaceRight) {
                facingRight = shouldFaceRight;
            }
        }
    }

    public void performWin() {
        animationSM.transition(AnimationStateMachine.AnimationType.WIN, true);
        isPerformingGroundAttack = false;
        velX = 0;
        velY = 0;
        y = (float) GROUND_Y;
        onGround = true;

        if (playerId.equals("P1")) {
            AudioManager.playP1WinSound();
        } else {
            AudioManager.playP2WinSound();
        }
    }

    public void resetForNewRound() {
        health = maxHealth;
        comboCount = 0;
        blockstunTimer = 0;
        hitstunTimer = 0;
        invulnerable = false;
        canCancelAttack = false;
        velX = 0;
        velY = 0;
        y = (float) GROUND_Y;
        onGround = true;
        jumpInitiated = false;
        canPerformAirAction = true;
        isPerformingGroundAttack = false;
        animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
    }

    public void handleKeyPressed(KeyCode key) {
        inputManager.handleKeyPressed(key);
    }

    public void handleKeyReleased(KeyCode key) {
        inputManager.handleKeyReleased(key);
    }

    // Getters
    @Override
    public String getName() { return characterName; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isFacingRight() { return facingRight; }
    public boolean isDead() { return health <= 0; }
    public AnimationStateMachine.AnimationType getCurrentAnimation() {
        return animationSM.getCurrentAnimationType();
    }
    public boolean isOnGround() { return onGround; }
    public int getComboCount() { return comboCount; }
    public boolean isInvulnerable() { return invulnerable; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public String getPlayerId() { return playerId; }
    public boolean isPerformingAttack() { return isPerformingGroundAttack; }

    public void setInvulnerable(boolean invulnerable, long duration) {
        this.invulnerable = invulnerable;
        if (invulnerable && duration > 0) {
            this.invulnerabilityEnd = System.currentTimeMillis() + duration;
        }
    }

    @Override
    public String toString() {
        return characterName + " (HP: " + health + "/" + maxHealth +
                ", Anim: " + animationSM.getCurrentAnimationType() +
                ", Attacking: " + isPerformingGroundAttack +
                ", Facing: " + (facingRight ? "Right" : "Left") +
                ", Pos: " + (int)x + "," + (int)y + ")";
    }
}