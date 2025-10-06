package Client;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private static AssetManager instance;
    private Map<String, Map<String, Image[]>> characterAnimations = new HashMap<>();
    private Map<String, Image> backgrounds = new HashMap<>();
    private boolean initialized = false;

    private AssetManager() {}

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }


    public void initialize() {
        if (initialized) return;

        // First initialize the Assets class (loads sprite sheets)
        Assets.init();

        // Then load character mappings
        loadCharacterAssets("RYU");
        loadCharacterAssets("KEN");

        initialized = true;
        System.out.println("AssetManager initialized with " +
                characterAnimations.size() + " characters loaded");
    }

    public void loadCharacterAssets(String characterName) {
        if (!Assets.isInitialized()) {
            Assets.init();
        }

        Map<String, Image[]> animations = new HashMap<>();
        String name = characterName.toUpperCase();

        //System.out.println("Mapping animations for: " + name);

        try {
            if (name.equals("RYU")) {
                mapRyuAnimations(animations);
            } else if (name.equals("KEN")) {
                mapKenAnimations(animations);
            } else {
               // System.err.println("Unknown character: " + characterName);
                createPlaceholderAnimations(name);
                return;
            }

            characterAnimations.put(name, animations);
            //System.out.println("Successfully mapped " + animations.size() + " animations for " + name);

        } catch (Exception e) {
           //System.err.println("Error mapping assets for " + characterName + ": " + e.getMessage());
            e.printStackTrace();
            createPlaceholderAnimations(name);
        }
    }

    private void mapRyuAnimations(Map<String, Image[]> animations) {
        // Basic movements
        if (Assets.idle != null && Assets.idle[0] != null) {
            animations.put("idle", Assets.idle);
        }
        if (Assets.parry_f != null && Assets.parry_f[0] != null) {
            animations.put("parry_f", Assets.parry_f);
            animations.put("parry_forward", Assets.parry_f);
        }
        if (Assets.parry_b != null && Assets.parry_b[0] != null) {
            animations.put("parry_b", Assets.parry_b);
            animations.put("parry_backward", Assets.parry_b);
        }
        if (Assets.crouch != null && Assets.crouch[0] != null) {
            animations.put("crouch", Assets.crouch);
        }
        if (Assets.jump != null && Assets.jump[0] != null) {
            animations.put("jump", Assets.jump);
        }
        if (Assets.front_flip != null && Assets.front_flip[0] != null) {
            animations.put("front_flip", Assets.front_flip);
        }
        if (Assets.back_flip != null && Assets.back_flip[0] != null) {
            animations.put("back_flip", Assets.back_flip);
        }

        // Ground attacks
        if (Assets.punch != null && Assets.punch[0] != null) {
            animations.put("punch", Assets.punch);
        }
        if (Assets.quick_punch != null && Assets.quick_punch[0] != null) {
            animations.put("quick_punch", Assets.quick_punch);
        }
        if (Assets.crouch_punch != null && Assets.crouch_punch[0] != null) {
            animations.put("crouch_punch", Assets.crouch_punch);
        }
        if (Assets.crouch_attack != null && Assets.crouch_attack[0] != null) {
            animations.put("crouch_attack", Assets.crouch_attack);
        }
        if (Assets.uppercut != null && Assets.uppercut[0] != null) {
            animations.put("uppercut", Assets.uppercut);
        }
        if (Assets.kick_low != null && Assets.kick_low[0] != null) {
            animations.put("kick_low", Assets.kick_low);
        }
        if (Assets.upper_kick != null && Assets.upper_kick[0] != null) {
            animations.put("upper_kick", Assets.upper_kick);
        }

        // Air attacks
        if (Assets.air_punch != null && Assets.air_punch[0] != null) {
            animations.put("air_punch", Assets.air_punch);
        }
        if (Assets.air_kick != null && Assets.air_kick[0] != null) {
            animations.put("air_kick", Assets.air_kick);
        }
        if (Assets.punch_down != null && Assets.punch_down[0] != null) {
            animations.put("punch_down", Assets.punch_down);
        }

        // Hit reactions
        if (Assets.hit_stand != null && Assets.hit_stand[0] != null) {
            animations.put("hit_stand", Assets.hit_stand);
        }
        if (Assets.hit_stand_back != null && Assets.hit_stand_back[0] != null) {
            animations.put("hit_stand_back", Assets.hit_stand_back);
        }
        if (Assets.crouch_hit != null && Assets.crouch_hit[0] != null) {
            animations.put("crouch_hit", Assets.crouch_hit);
        }
        if (Assets.crouch_hit_back != null && Assets.crouch_hit_back[0] != null) {
            animations.put("crouch_hit_back", Assets.crouch_hit_back);
        }
        if (Assets.knockback != null && Assets.knockback[0] != null) {
            animations.put("knockback", Assets.knockback);
        }
        if (Assets.recover != null && Assets.recover[0] != null) {
            animations.put("recover", Assets.recover);
        }

        // End states
        if (Assets.win != null && Assets.win[0] != null) {
            animations.put("win", Assets.win);
        }
        if (Assets.dead != null && Assets.dead[0] != null) {
            animations.put("dead", Assets.dead);
        }

        System.out.println("Mapped " + animations.size() + " Ryu animations");
    }

    private void mapKenAnimations(Map<String, Image[]> animations) {
        // Basic movements
        if (Assets.idle1 != null && Assets.idle1[0] != null) {
            animations.put("idle", Assets.idle1);
        }
        if (Assets.parry_f1 != null && Assets.parry_f1[0] != null) {
            animations.put("parry_f", Assets.parry_f1);
            animations.put("parry_forward", Assets.parry_f1);
        }
        if (Assets.parry_b1 != null && Assets.parry_b1[0] != null) {
            animations.put("parry_b", Assets.parry_b1);
            animations.put("parry_backward", Assets.parry_b1);
        }
        if (Assets.crouch1 != null && Assets.crouch1[0] != null) {
            animations.put("crouch", Assets.crouch1);
        }
        if (Assets.jump1 != null && Assets.jump1[0] != null) {
            animations.put("jump", Assets.jump1);
        }
        if (Assets.front_flip1 != null && Assets.front_flip1[0] != null) {
            animations.put("front_flip", Assets.front_flip1);
        }
        if (Assets.back_flip1 != null && Assets.back_flip1[0] != null) {
            animations.put("back_flip", Assets.back_flip1);
        }

        // Ground attacks
        if (Assets.punch1 != null && Assets.punch1[0] != null) {
            animations.put("punch", Assets.punch1);
        }
        if (Assets.quick_punch1 != null && Assets.quick_punch1[0] != null) {
            animations.put("quick_punch", Assets.quick_punch1);
        }
        if (Assets.crouch_punch1 != null && Assets.crouch_punch1[0] != null) {
            animations.put("crouch_punch", Assets.crouch_punch1);
        }
        if (Assets.crouch_attack1 != null && Assets.crouch_attack1[0] != null) {
            animations.put("crouch_attack", Assets.crouch_attack1);
        }
        if (Assets.uppercut1 != null && Assets.uppercut1[0] != null) {
            animations.put("uppercut", Assets.uppercut1);
        }
        if (Assets.kick_low1 != null && Assets.kick_low1[0] != null) {
            animations.put("kick_low", Assets.kick_low1);
        }
        if (Assets.upper_kick1 != null && Assets.upper_kick1[0] != null) {
            animations.put("upper_kick", Assets.upper_kick1);
        }

        // Air attacks
        if (Assets.air_punch1 != null && Assets.air_punch1[0] != null) {
            animations.put("air_punch", Assets.air_punch1);
        }
        if (Assets.air_kick1 != null && Assets.air_kick1[0] != null) {
            animations.put("air_kick", Assets.air_kick1);
        }
        if (Assets.punch_down1 != null && Assets.punch_down1[0] != null) {
            animations.put("punch_down", Assets.punch_down1);
        }

        // Hit reactions
        Image[] hitStand = (Assets.hit_stand1 != null && Assets.hit_stand1[0] != null) ?
                Assets.hit_stand1 : Assets.hit_stand;
        if (hitStand != null && hitStand[0] != null) {
            animations.put("hit_stand", hitStand);
        }

        Image[] hitStandBack = (Assets.hit_stand_back1 != null && Assets.hit_stand_back1[0] != null) ?
                Assets.hit_stand_back1 : Assets.hit_stand_back;
        if (hitStandBack != null && hitStandBack[0] != null) {
            animations.put("hit_stand_back", hitStandBack);
        }

        Image[] crouchHit = (Assets.crouch_hit1 != null && Assets.crouch_hit1[0] != null) ?
                Assets.crouch_hit1 : Assets.crouch_hit;
        if (crouchHit != null && crouchHit[0] != null) {
            animations.put("crouch_hit", crouchHit);
        }

        Image[] crouchHitBack = (Assets.crouch_hit_back1 != null && Assets.crouch_hit_back1[0] != null) ?
                Assets.crouch_hit_back1 : Assets.crouch_hit_back;
        if (crouchHitBack != null && crouchHitBack[0] != null) {
            animations.put("crouch_hit_back", crouchHitBack);
        }

        Image[] knockback = (Assets.knockback1 != null && Assets.knockback1[0] != null) ?
                Assets.knockback1 : Assets.knockback;
        if (knockback != null && knockback[0] != null) {
            animations.put("knockback", knockback);
        }

        Image[] recover = (Assets.recover1 != null && Assets.recover1[0] != null) ?
                Assets.recover1 : Assets.recover;
        if (recover != null && recover[0] != null) {
            animations.put("recover", recover);
        }

        // End states
        Image[] win = (Assets.win1 != null && Assets.win1[0] != null) ?
                Assets.win1 : Assets.win;
        if (win != null && win[0] != null) {
            animations.put("win", win);
        }

        Image[] dead = (Assets.dead1 != null && Assets.dead1[0] != null) ?
                Assets.dead1 : Assets.dead;
        if (dead != null && dead[0] != null) {
            animations.put("dead", dead);
        }

        System.out.println("Mapped " + animations.size() + " Ken animations");
    }

    private void createPlaceholderAnimations(String characterName) {
        Map<String, Image[]> animations = new HashMap<>();

        // Create placeholder animations with proper frame counts
        animations.put("idle", createPlaceholderFrames(6, 60, 100, Color.LIGHTBLUE));
        animations.put("parry_f", createPlaceholderFrames(8, 60, 100, Color.GREEN));
        animations.put("parry_b", createPlaceholderFrames(8, 60, 100, Color.DARKGREEN));
        animations.put("crouch", createPlaceholderFrames(1, 60, 80, Color.ORANGE));
        animations.put("jump", createPlaceholderFrames(11, 60, 120, Color.YELLOW));
        animations.put("front_flip", createPlaceholderFrames(8, 80, 120, Color.PURPLE));
        animations.put("back_flip", createPlaceholderFrames(8, 80, 120, Color.VIOLET));

        // Attack animations
        animations.put("punch", createPlaceholderFrames(6, 80, 100, Color.RED));
        animations.put("quick_punch", createPlaceholderFrames(3, 80, 100, Color.PINK));
        animations.put("crouch_punch", createPlaceholderFrames(3, 80, 80, Color.DARKRED));
        animations.put("crouch_attack", createPlaceholderFrames(8, 80, 80, Color.MAROON));
        animations.put("uppercut", createPlaceholderFrames(8, 80, 110, Color.CRIMSON));
        animations.put("kick_low", createPlaceholderFrames(5, 100, 100, Color.BLUE));
        animations.put("upper_kick", createPlaceholderFrames(9, 100, 120, Color.NAVY));

        // Air attacks
        animations.put("air_punch", createPlaceholderFrames(6, 80, 100, Color.LIGHTCORAL));
        animations.put("air_kick", createPlaceholderFrames(5, 80, 100, Color.LIGHTBLUE));
        animations.put("punch_down", createPlaceholderFrames(4, 80, 100, Color.LIGHTSALMON));

        // Hit reactions
        animations.put("hit_stand", createPlaceholderFrames(2, 60, 100, Color.GRAY));
        animations.put("hit_stand_back", createPlaceholderFrames(4, 60, 100, Color.DARKGRAY));
        animations.put("crouch_hit", createPlaceholderFrames(2, 60, 80, Color.LIGHTGRAY));
        animations.put("crouch_hit_back", createPlaceholderFrames(4, 60, 80, Color.SILVER));
        animations.put("knockback", createPlaceholderFrames(4, 100, 100, Color.BLACK));
        animations.put("recover", createPlaceholderFrames(5, 80, 100, Color.WHITE));

        // End states
        animations.put("win", createPlaceholderFrames(10, 80, 120, Color.GOLD));
        animations.put("dead", createPlaceholderFrames(1, 100, 50, Color.DARKRED));

        characterAnimations.put(characterName, animations);
        System.out.println("Created " + animations.size() + " placeholder animations for: " + characterName);
    }

    private Image[] createPlaceholderFrames(int count, int width, int height, Color baseColor) {
        Image[] frames = new Image[count];

        for (int i = 0; i < count; i++) {
            Canvas canvas = new Canvas(width, height);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // Create gradient effect based on frame
            double alpha = 0.5 + (0.5 * Math.sin((i * Math.PI) / count));
            gc.setFill(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));
            gc.fillRect(0, 0, width, height);

            // Add frame border
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, width-2, height-2);

            // Add frame number
            gc.setFill(Color.WHITE);
            gc.fillText("F" + (i + 1), width/2 - 10, height/2);

            frames[i] = canvas.snapshot(null, null);
        }

        return frames;
    }

    public Image[] getAnimation(String character, String animationName) {
        if (!initialized) {
            initialize();
        }

        Map<String, Image[]> characterAnims = characterAnimations.get(character.toUpperCase());
        if (characterAnims != null && characterAnims.containsKey(animationName)) {
            Image[] animation = characterAnims.get(animationName);
            if (animation != null && animation.length > 0 && animation[0] != null) {
                return animation;
            }
        }

        // Fallback to idle if animation doesn't exist
        if (characterAnims != null && characterAnims.containsKey("idle")) {
            Image[] idle = characterAnims.get("idle");
            if (idle != null && idle.length > 0 && idle[0] != null) {
                return idle;
            }
        }

        // Last resort - create placeholder
        System.err.println("Creating emergency placeholder for " + character + "." + animationName);
        return createPlaceholderFrames(1, 60, 100, Color.MAGENTA);
    }

    public void loadBackground(String mapName, String imagePath) {
        try {
            Image bg = ImageLoader.loadImage(imagePath);
            if (bg != null && !bg.isError()) {
                backgrounds.put(mapName, bg);
                System.out.println("Loaded background: " + mapName);
            } else {
                System.err.println("Failed to load background: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("Exception loading background " + imagePath + ": " + e.getMessage());
        }
    }

    public Image getBackground(String mapName) {
        return backgrounds.get(mapName);
    }

    public boolean isCharacterLoaded(String characterName) {
        return characterAnimations.containsKey(characterName.toUpperCase());
    }

    public String[] getAvailableAnimations(String characterName) {
        Map<String, Image[]> characterAnims = characterAnimations.get(characterName.toUpperCase());
        if (characterAnims != null) {
            return characterAnims.keySet().toArray(new String[0]);
        }
        return new String[0];
    }

    public void clearAssets() {
        characterAnimations.clear();
        backgrounds.clear();
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void validateAssets() {
        System.out.println("=== Asset Validation Report ===");
        for (String character : characterAnimations.keySet()) {
            System.out.println("Character: " + character);
            Map<String, Image[]> anims = characterAnimations.get(character);

            for (String animName : anims.keySet()) {
                Image[] frames = anims.get(animName);
                int validFrames = 0;
                for (Image frame : frames) {
                    if (frame != null && !frame.isError()) validFrames++;
                }
                System.out.println("  " + animName + ": " + validFrames + "/" + frames.length + " valid frames");
            }
        }
        System.out.println("=== End Validation Report ===");
    }
}