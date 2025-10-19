package Client;

import javafx.scene.image.Image;

public class Assets {

    //SPRITE SHEETS: RYU

    // Basic movement
    public static Image[] idle = new Image[6],
            parry_f = new Image[8],
            parry_b = new Image[8];

    // Ground attacks
    public static Image[] punch = new Image[6],
            quick_punch = new Image[3],
            uppercut = new Image[8];

    // Kicks
    public static Image[] kick_low = new Image[5],
            upper_kick = new Image[9];

    // Air attacks
    public static Image[] air_punch = new Image[6],
            air_kick = new Image[5],
            punch_down = new Image[4];

    // Movement
    public static Image[] back_flip = new Image[8],
            front_flip = new Image[8],
            jump = new Image[11];

    // Hit reactions and recovery
    public static Image[] hit_stand = new Image[2],
            hit_stand_back = new Image[4],
            knockback = new Image[4],
            recover = new Image[5];

    // Win and Dead
    public static Image[] win = new Image[10],
            dead = new Image[1];


    // SPRITE SHEETS: KEN

    // Basic movement
    public static Image[] idle1 = new Image[6],
            parry_f1 = new Image[8],
            parry_b1 = new Image[8];

    // Ground attacks
    public static Image[] punch1 = new Image[8],
            quick_punch1 = new Image[4],
            uppercut1 = new Image[8];

    // Kicks
    public static Image[] kick_low1 = new Image[5],
            upper_kick1 = new Image[10];

    // Air attacks
    public static Image[] air_punch1 = new Image[7],
            air_kick1 = new Image[6],
            punch_down1 = new Image[4];

    // Movement
    public static Image[] back_flip1 = new Image[8],
            front_flip1 = new Image[8],
            jump1 = new Image[11];

    // Hit reactions and recovery
    public static Image[] hit_stand1 = new Image[2],
            hit_stand_back1 = new Image[4],
            knockback1 = new Image[4],
            recover1 = new Image[5];

    // Win and Dead
    public static Image[] win1 = new Image[10],
            dead1 = new Image[1];

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        try {
            loadRyuAssets();
            loadKenAssets();
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            initialized = true;
        }
    }

    private static void loadRyuAssets() {


        try {
            SpriteSheet ss_idle = loadSpriteSheet("/images/ryu/idle.png");
            SpriteSheet ss_parry_f = loadSpriteSheet("/images/ryu/parry_f.png");
            SpriteSheet ss_parry_b = loadSpriteSheet("/images/ryu/parry_b.png");
            SpriteSheet ss_jump = loadSpriteSheet("/images/ryu/jump.png");
            SpriteSheet ss_front_flip = loadSpriteSheet("/images/ryu/front_flip.png");
            SpriteSheet ss_back_flip = loadSpriteSheet("/images/ryu/back_flip.png");

            SpriteSheet ss_punch = loadSpriteSheet("/images/ryu/punch.png");
            SpriteSheet ss_quick_punch = loadSpriteSheet("/images/ryu/quick_punch.png");
            SpriteSheet ss_uppercut = loadSpriteSheet("/images/ryu/uppercut.png");
            SpriteSheet ss_kick_low = loadSpriteSheet("/images/ryu/kick_low.png");
            SpriteSheet ss_upper_kick = loadSpriteSheet("/images/ryu/upper_kick.png");

            SpriteSheet ss_air_punch = loadSpriteSheet("/images/ryu/air_punch.png");
            SpriteSheet ss_air_kick = loadSpriteSheet("/images/ryu/air_kick.png");
            SpriteSheet ss_punch_down = loadSpriteSheet("/images/ryu/punch_down.png");

            SpriteSheet ss_hit_stand = loadSpriteSheet("/images/ryu/hit_stand.png");
            SpriteSheet ss_hit_stand_back = loadSpriteSheet("/images/ryu/hit_stand_b.png");
            SpriteSheet ss_knockback = loadSpriteSheet("/images/ryu/knockback.png");
            SpriteSheet ss_recover = loadSpriteSheet("/images/ryu/recover.png");

            SpriteSheet ss_win = loadSpriteSheet("/images/ryu/win.png");
            SpriteSheet ss_dead = loadSpriteSheet("/images/ryu/dead.png");

            // Basic movement
            loadSpriteFrames(ss_idle, idle, 57, 106);
            loadSpriteFrames(ss_parry_f, parry_f, 70, 110);
            loadSpriteFrames(ss_parry_b, parry_b, 70, 108);

            // Movement
            loadRyuJumpFrames(ss_jump, jump, 70, 154);
            loadSpriteFrames(ss_front_flip, front_flip, 88, 129);
            loadSpriteFrames(ss_back_flip, back_flip, 88, 129);

            // Ground attacks
            loadSpriteFrames(ss_punch, punch, 101, 102);
            loadSpriteFrames(ss_quick_punch, quick_punch, 94, 102);
            loadSpriteFrames(ss_uppercut, uppercut, 82, 111);

            // Kicks
            loadSpriteFrames(ss_kick_low, kick_low, 115, 111);
            loadSpriteFrames(ss_upper_kick, upper_kick, 110, 111);

            // Air attacks
            loadSpriteFrames(ss_air_punch, air_punch, 83, 95);
            loadSpriteFrames(ss_air_kick, air_kick, 99, 94);
            loadSpriteFrames(ss_punch_down, punch_down, 75, 90);

            // Hit reactions and recovery
            loadSpriteFrames(ss_hit_stand, hit_stand, 62, 103);
            loadSpriteFrames(ss_hit_stand_back, hit_stand_back, 77, 104);
            loadSpriteFrames(ss_knockback, knockback, 86, 104);
            loadSpriteFrames(ss_recover, recover, 77, 104);

            // Win and Dead
            loadSpriteFrames(ss_win, win, 73, 148);
            if (ss_dead != null) {
                dead[0] = ss_dead.crop(116, 46, 0, 0);
            }


        } catch (Exception e) {

        }
    }

    private static void loadKenAssets() {

        try {
            SpriteSheet ss_idle1 = loadSpriteSheet("/images/ken/idle.png");
            SpriteSheet ss_parry_f1 = loadSpriteSheet("/images/ken/parry_f.png");
            SpriteSheet ss_parry_b1 = loadSpriteSheet("/images/ken/parry_b.png");
            SpriteSheet ss_jump1 = loadSpriteSheet("/images/ken/jump.png");
            SpriteSheet ss_front_flip1 = loadSpriteSheet("/images/ken/front_flip.png");
            SpriteSheet ss_back_flip1 = loadSpriteSheet("/images/ken/back_flip.png");

            SpriteSheet ss_punch1 = loadSpriteSheet("/images/ken/punch.png");
            SpriteSheet ss_quick_punch1 = loadSpriteSheet("/images/ken/quick_punch.png");
            SpriteSheet ss_uppercut1 = loadSpriteSheet("/images/ken/uppercut.png");
            SpriteSheet ss_kick_low1 = loadSpriteSheet("/images/ken/kick_low.png");
            SpriteSheet ss_upper_kick1 = loadSpriteSheet("/images/ken/upper_kick.png");

            SpriteSheet ss_air_punch1 = loadSpriteSheet("/images/ken/air_punch.png");
            SpriteSheet ss_air_kick1 = loadSpriteSheet("/images/ken/air_kick.png");
            SpriteSheet ss_punch_down1 = loadSpriteSheet("/images/ken/punch_down.png");

            SpriteSheet ss_hit_stand1 = loadSpriteSheet("/images/ken/hit_stand.png");
            SpriteSheet ss_hit_stand_back1 = loadSpriteSheet("/images/ken/hit_stand_b.png");
            SpriteSheet ss_knockback1 = loadSpriteSheet("/images/ken/knockback.png");
            SpriteSheet ss_recover1 = loadSpriteSheet("/images/ken/recover.png");

            SpriteSheet ss_win1 = loadSpriteSheet("/images/ken/win.png");
            SpriteSheet ss_dead1 = loadSpriteSheet("/images/ken/dead.png");

            // Basic movement and stances
            loadSpriteFrames(ss_idle1, idle1, 57, 106);
            loadSpriteFrames(ss_parry_f1, parry_f1, 70, 110);
            loadSpriteFrames(ss_parry_b1, parry_b1, 70, 110);

            // Movement
            loadKenJumpFrames(ss_jump1, jump1, 61, 124);
            loadSpriteFrames(ss_front_flip1, front_flip1, 83, 125);
            loadSpriteFrames(ss_back_flip1, back_flip1, 83, 125);

            // Ground attacks
            loadSpriteFrames(ss_punch1, punch1, 103, 103);
            loadSpriteFrames(ss_quick_punch1, quick_punch1, 95, 102);
            loadSpriteFrames(ss_uppercut1, uppercut1, 91, 108);

            // Kicks
            loadSpriteFrames(ss_kick_low1, kick_low1, 118, 105);
            loadSpriteFrames(ss_upper_kick1, upper_kick1, 135, 108);

            // Air attacks
            loadSpriteFrames(ss_air_punch1, air_punch1, 84, 95);
            loadSpriteFrames(ss_air_kick1, air_kick1, 106, 83);
            loadSpriteFrames(ss_punch_down1, punch_down1, 68, 88);

            // Hit reactions and recovery
            loadSpriteFrames(ss_hit_stand1, hit_stand1, 57, 104);
            loadSpriteFrames(ss_hit_stand_back1, hit_stand_back1, 79, 104);
            loadSpriteFrames(ss_knockback1, knockback1, 86, 104);
            loadSpriteFrames(ss_recover1, recover1, 79, 104);

            // Win and Dead
            loadSpriteFrames(ss_win1, win1, 70, 144);
            if (ss_dead1 != null) {
                dead1[0] = ss_dead1.crop(116, 46, 0, 0);
            }

        } catch (Exception e) {

        }
    }

    private static SpriteSheet loadSpriteSheet(String path) {
        try {
            return new SpriteSheet(path);
        } catch (Exception e) {
            return null;
        }
    }

    private static void loadSpriteFrames(SpriteSheet spriteSheet, Image[] frames, int frameWidth, int frameHeight) {
        if (spriteSheet == null) return;

        for (int i = 0; i < frames.length; i++) {
            try {
                frames[i] = spriteSheet.crop(frameWidth, frameHeight, frameWidth * i, 0);
            } catch (Exception e) {
            }
        }
    }

    private static void loadRyuJumpFrames(SpriteSheet spriteSheet, Image[] frames, int frameWidth, int frameHeight) {
        if (spriteSheet == null) return;

        try {
            for (int i = 0; i < 3; i++) {
                frames[i] = spriteSheet.crop(frameWidth, frameHeight, 0, 0);
            }
            for (int i = 3; i < frames.length; i++) {
                frames[i] = spriteSheet.crop(frameWidth, frameHeight, frameWidth * (i - 2), 0);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void loadKenJumpFrames(SpriteSheet spriteSheet, Image[] frames, int frameWidth, int frameHeight) {
        if (spriteSheet == null) return;

        try {
            for (int i = 0; i < 3; i++) {
                frames[i] = spriteSheet.crop(frameWidth, frameHeight, 0, 0);
            }
            for (int i = 3; i < frames.length; i++) {
                frames[i] = spriteSheet.crop(frameWidth, frameHeight, frameWidth * (i - 2), 0);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }


    public static Image[] getIdleFrames(boolean isRyu) {
        return isRyu ? idle : idle1;
    }

    public static Image[] getPunchFrames(boolean isRyu) {
        return isRyu ? punch : punch1;
    }

    public static Image[] getKickFrames(boolean isRyu) {
        return isRyu ? kick_low : kick_low1;
    }

    public static Image[] getJumpFrames(boolean isRyu) {
        return isRyu ? jump : jump1;
    }

    public static Image[] getHitFrames(boolean isRyu) {
        return isRyu ? hit_stand : hit_stand1;
    }

    public static Image[] getWinFrames(boolean isRyu) {
        return isRyu ? win : win1;
    }

    public static Image getDeadFrame(boolean isRyu) {
        return isRyu ? (dead[0] != null ? dead[0] : null) : (dead1[0] != null ? dead1[0] : null);
    }


    private static int getRyuAnimationCount() {
        int count = 0;
        if (idle != null && idle[0] != null) count++;
        if (parry_f != null && parry_f[0] != null) count++;
        if (parry_b != null && parry_b[0] != null) count++;
        if (jump != null && jump[0] != null) count++;
        if (front_flip != null && front_flip[0] != null) count++;
        if (back_flip != null && back_flip[0] != null) count++;
        if (punch != null && punch[0] != null) count++;
        if (quick_punch != null && quick_punch[0] != null) count++;
        if (uppercut != null && uppercut[0] != null) count++;
        if (kick_low != null && kick_low[0] != null) count++;
        if (upper_kick != null && upper_kick[0] != null) count++;
        if (air_punch != null && air_punch[0] != null) count++;
        if (air_kick != null && air_kick[0] != null) count++;
        if (punch_down != null && punch_down[0] != null) count++;
        if (hit_stand != null && hit_stand[0] != null) count++;
        if (hit_stand_back != null && hit_stand_back[0] != null) count++;
        if (knockback != null && knockback[0] != null) count++;
        if (recover != null && recover[0] != null) count++;
        if (win != null && win[0] != null) count++;
        if (dead != null && dead[0] != null) count++;
        return count;
    }

    private static int getKenAnimationCount() {
        int count = 0;
        if (idle1 != null && idle1[0] != null) count++;
        if (parry_f1 != null && parry_f1[0] != null) count++;
        if (parry_b1 != null && parry_b1[0] != null) count++;
        if (jump1 != null && jump1[0] != null) count++;
        if (front_flip1 != null && front_flip1[0] != null) count++;
        if (back_flip1 != null && back_flip1[0] != null) count++;
        if (punch1 != null && punch1[0] != null) count++;
        if (quick_punch1 != null && quick_punch1[0] != null) count++;
        if (uppercut1 != null && uppercut1[0] != null) count++;
        if (kick_low1 != null && kick_low1[0] != null) count++;
        if (upper_kick1 != null && upper_kick1[0] != null) count++;
        if (air_punch1 != null && air_punch1[0] != null) count++;
        if (air_kick1 != null && air_kick1[0] != null) count++;
        if (punch_down1 != null && punch_down1[0] != null) count++;
        if (hit_stand1 != null && hit_stand1[0] != null) count++;
        if (hit_stand_back1 != null && hit_stand_back1[0] != null) count++;
        if (knockback1 != null && knockback1[0] != null) count++;
        if (recover1 != null && recover1[0] != null) count++;
        if (win1 != null && win1[0] != null) count++;
        if (dead1 != null && dead1[0] != null) count++;
        return count;
    }
}