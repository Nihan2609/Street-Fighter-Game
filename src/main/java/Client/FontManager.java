package Client;

import javafx.scene.text.Font;
import java.io.InputStream;

public class FontManager {
    private static FontManager instance;
    private Font pressStart2P;
    private boolean initialized = false;

    // Font sizes
    public static final double SMALL = 12;
    public static final double MEDIUM = 16;
    public static final double LARGE = 20;
    public static final double XLARGE = 24;
    public static final double TITLE = 32;

    private FontManager() {}

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) return;

        try {
            // Load PressStart2P font
            InputStream fontStream = getClass().getResourceAsStream("/fonts/PressStart2P-Regular.ttf");
            if (fontStream != null) {
                pressStart2P = Font.loadFont(fontStream, MEDIUM);
                fontStream.close();

                if (pressStart2P != null) {
                    System.out.println("✓ PressStart2P font loaded successfully");
                    initialized = true;
                } else {
                    System.err.println("✗ Failed to load PressStart2P font");
                    loadFallbackFont();
                }
            } else {
                System.err.println("✗ Font file not found: /fonts/PressStart2P-Regular.ttf");
                loadFallbackFont();
            }
        } catch (Exception e) {
            System.err.println("Error loading custom font: " + e.getMessage());
            e.printStackTrace();
            loadFallbackFont();
        }
    }

    private void loadFallbackFont() {
        pressStart2P = Font.font("Monospaced", MEDIUM);
        System.out.println("Using fallback font: Monospaced");
        initialized = true;
    }

    // Get font with default size
    public Font getPressStart2P() {
        if (!initialized) initialize();
        return pressStart2P;
    }

    // Get font with custom size
    public Font getPressStart2P(double size) {
        if (!initialized) initialize();
        return Font.font(pressStart2P.getFamily(), size);
    }

    // Convenient methods for different sizes
    public Font getSmall() {
        return getPressStart2P(SMALL);
    }

    public Font getMedium() {
        return getPressStart2P(MEDIUM);
    }

    public Font getLarge() {
        return getPressStart2P(LARGE);
    }

    public Font getXLarge() {
        return getPressStart2P(XLARGE);
    }

    public Font getTitle() {
        return getPressStart2P(TITLE);
    }

    // Apply font to a JavaFX Node with inline CSS
    public String getFontStyle(double size) {
        if (!initialized) initialize();
        return String.format(
                "-fx-font-family: '%s'; -fx-font-size: %.0fpx;",
                pressStart2P.getFamily(),
                size
        );
    }

    // Get CSS style string
    public String getStyleString(double size) {
        return getFontStyle(size);
    }

    // Get CSS style with color
    public String getStyleString(double size, String color) {
        return getFontStyle(size) + " -fx-text-fill: " + color + ";";
    }
}