package Client;

import javafx.scene.image.Image;

public class Assets {

    // ============================================ SPRITE SHEETS: RYU ============================================

    public static Image[] idle = new Image[6],
            parry_f = new Image[8],
            parry_b = new Image[8],
            crouch = new Image[1];

    public static Image[] punch = new Image[6],
            quick_punch = new Image[3],
            crouch_punch = new Image[3],
            crouch_attack = new Image[8],
            uppercut = new Image[8];

    public static Image[] kick_low = new Image[5],
            upper_kick = new Image[9];

    public static Image[] air_punch = new Image[6],
            air_kick = new Image[5],
            punch_down = new Image[4];

    public static Image[] back_flip = new Image[8],
            front_flip = new Image[8],
            jump = new Image[11];

    public static Image[] crouch_hit = new Image[2],
            crouch_hit_back = new Image[4],
            hit_stand = new Image[2],
            hit_stand_back = new Image[4],
            knockback = new Image[4],
            recover = new Image[5];

    public static Image[] win = new Image[10],
            dead = new Image[1];

    // ============================================ SPRITE SHEETS: KEN ============================================

    public static Image[] idle1 = new Image[6],
            parry_f1 = new Image[8],
            parry_b1 = new Image[8],
            crouch1 = new Image[1];

    public static Image[] punch1 = new Image[8],
            quick_punch1 = new Image[4],
            crouch_punch1 = new Image[3],
            crouch_attack1 = new Image[5],
            uppercut1 = new Image[8];

    public static Image[] kick_low1 = new Image[5],
            upper_kick1 = new Image[10];

    public static Image[] air_punch1 = new Image[7],
            air_kick1 = new Image[6],
            punch_down1 = new Image[4];

    public static Image[] back_flip1 = new Image[8],
            front_flip1 = new Image[8],
            jump1 = new Image[11];

    public static Image[] crouch_hit1 = new Image[2],
            crouch_hit_back1 = new Image[4],
            hit_stand1 = new Image[2],
            hit_stand_back1 = new Image[4],
            knockback1 = new Image[4],
            recover1 = new Image[5];

    public static Image[] win1 = new Image[10],
            dead1 = new Image[1];


    public static void init() {

        // ============================================ SPRITE SHEETS: RYU ============================================

        SpriteSheet ss_idle = new SpriteSheet("/images/ryu/idle.png");
        SpriteSheet ss_parry_front = new SpriteSheet("/images/ryu/parry_f.png");
        SpriteSheet ss_parry_back = new SpriteSheet("/images/ryu/parry_b.png");
        SpriteSheet ss_crouch = new SpriteSheet("/images/ryu/crouch.png");
        SpriteSheet ss_jump = new SpriteSheet("/images/ryu/jump.png");
        SpriteSheet ss_front_flip = new SpriteSheet("/images/ryu/front_flip.png");
        SpriteSheet ss_back_flip = new SpriteSheet("/images/ryu/back_flip.png");
        SpriteSheet ss_punch = new SpriteSheet("/images/ryu/punch.png");
        SpriteSheet ss_quick_punch = new SpriteSheet("/images/ryu/quick_punch.png");
        SpriteSheet ss_crouch_punch = new SpriteSheet("/images/ryu/crouch_punch.png");
        SpriteSheet ss_kick_low = new SpriteSheet("/images/ryu/kick_low.png");
        SpriteSheet ss_upper_kick = new SpriteSheet("/images/ryu/upper_kick.png");
        SpriteSheet ss_air_punch = new SpriteSheet("/images/ryu/air_punch.png");
        SpriteSheet ss_air_kick = new SpriteSheet("/images/ryu/air_kick.png");
        SpriteSheet ss_punch_down = new SpriteSheet("/images/ryu/punch_down.png");

        // Basic movement
        for (int i = 0; i < 6; i++) idle[i] = ss_idle.crop(57, 106, 57 * i, 0);
        for (int i = 0; i < 8; i++) parry_f[i] = ss_parry_front.crop(70, 110, 70 * i, 0);
        for (int i = 0; i < 8; i++) parry_b[i] = ss_parry_back.crop(70, 108, 70 * i, 0);
        crouch[0] = ss_crouch.crop(54, 73, 0, 0);

        // Jumping
        jump[0] = ss_jump.crop(70, 154, 0, 0);
        jump[1] = ss_jump.crop(70, 154, 0, 0);
        jump[2] = ss_jump.crop(70, 154, 0, 0);
        for (int i = 3; i < 11; i++) jump[i] = ss_jump.crop(70, 154, 70 * (i - 2), 0);

        for (int i = 0; i < 8; i++) front_flip[i] = ss_front_flip.crop(88, 129, 88 * i, 0);
        for (int i = 0; i < 8; i++) back_flip[i] = ss_back_flip.crop(88, 129, 88 * i, 0);

        // Ground attacks
        for (int i = 0; i < 6; i++) punch[i] = ss_punch.crop(101, 102, 101 * i, 0);
        for (int i = 0; i < 3; i++) quick_punch[i] = ss_quick_punch.crop(94, 102, 94 * i, 0);
        for (int i = 0; i < 3; i++) crouch_punch[i] = ss_crouch_punch.crop(86, 72, 86 * i, 0);

        // Air attacks
        for (int i = 0; i < 6; i++) air_punch[i] = ss_air_punch.crop(83, 95, 83 * i, 0);
        for (int i = 0; i < 4; i++) punch_down[i] = ss_punch_down.crop(75, 90, 75 * i, 0);
        for (int i = 0; i < 5; i++) air_kick[i] = ss_air_kick.crop(99, 94, 99 * i, 0);

        // Ground kicks
        for (int i = 0; i < 5; i++) kick_low[i] = ss_kick_low.crop(115, 111, 115 * i, 0);
        for (int i = 0; i < 9; i++) upper_kick[i] = ss_upper_kick.crop(110, 111, 110 * i, 0);

        // ============================================ KEN ============================================
        SpriteSheet ss_idle1 = new SpriteSheet("/images/ken/idle.png");
        SpriteSheet ss_jump1 = new SpriteSheet("/images/ken/jump.png");
        SpriteSheet ss_front_flip1 = new SpriteSheet("/images/ken/front_flip.png");
        SpriteSheet ss_back_flip1 = new SpriteSheet("/images/ken/back_flip.png");
        SpriteSheet ss_punch1 = new SpriteSheet("/images/ken/punch.png");
        SpriteSheet ss_quick_punch1 = new SpriteSheet("/images/ken/quick_punch.png");
        SpriteSheet ss_crouch_punch1 = new SpriteSheet("/images/ken/crouch_punch.png");
        SpriteSheet ss_kick_low1 = new SpriteSheet("/images/ken/kick_low.png");
        SpriteSheet ss_upper_kick1 = new SpriteSheet("/images/ken/upper_kick.png");
        SpriteSheet ss_air_punch1 = new SpriteSheet("/images/ken/air_punch.png");
        SpriteSheet ss_air_kick1 = new SpriteSheet("/images/ken/air_kick.png");
        SpriteSheet ss_punch_down1 = new SpriteSheet("/images/ken/punch_down.png");

        for (int i = 0; i < 6; i++) idle1[i] = ss_idle1.crop(57, 106, 57 * i, 0);
        for (int i = 0; i < 11; i++) jump1[i] = ss_jump1.crop(61, 124, (i < 3 ? 0 : 61 * (i - 2)), 0);
        for (int i = 0; i < 8; i++) front_flip1[i] = ss_front_flip1.crop(83, 125, 83 * i, 0);
        for (int i = 0; i < 8; i++) back_flip1[i] = ss_back_flip1.crop(83, 125, 83 * i, 0);
        for (int i = 0; i < 8; i++) punch1[i] = ss_punch1.crop(103, 103, 103 * i, 0);
        for (int i = 0; i < 4; i++) quick_punch1[i] = ss_quick_punch1.crop(95, 102, 95 * i, 0);
        for (int i = 0; i < 3; i++) crouch_punch1[i] = ss_crouch_punch1.crop(89, 72, 89 * i, 0);
        for (int i = 0; i < 5; i++) kick_low1[i] = ss_kick_low1.crop(118, 105, 118 * i, 0);
        for (int i = 0; i < 10; i++) upper_kick1[i] = ss_upper_kick1.crop(135, 108, 135 * i, 0);
        for (int i = 0; i < 7; i++) air_punch1[i] = ss_air_punch1.crop(84, 95, 84 * i, 0);
        for (int i = 0; i < 4; i++) punch_down1[i] = ss_punch_down1.crop(68, 88, 68 * i, 0);
        for (int i = 0; i < 6; i++) air_kick1[i] = ss_air_kick1.crop(106, 83, 106 * i, 0);
    }
}