package Client;

import javafx.scene.input.KeyCode;
import java.util.*;

public class InputManager {
    private static InputManager instance;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private Map<String, Map<String, KeyCode>> playerBindings = new HashMap<>();

    // Network input state for remote players
    private Map<String, Map<String, Boolean>> networkInputState = new HashMap<>();
    private Map<String, Boolean> isNetworkPlayer = new HashMap<>();

    InputManager() {
        setupDefaultBindings();
    }

    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    private void setupDefaultBindings() {
        // Player 1 bindings
        Map<String, KeyCode> p1Bindings = new HashMap<>();
        p1Bindings.put("left", KeyCode.A);
        p1Bindings.put("right", KeyCode.D);
        p1Bindings.put("up", KeyCode.W);
        p1Bindings.put("down", KeyCode.S);
        p1Bindings.put("light_punch", KeyCode.F);
        p1Bindings.put("heavy_punch", KeyCode.G);
        p1Bindings.put("light_kick", KeyCode.H);
        p1Bindings.put("heavy_kick", KeyCode.R);
        p1Bindings.put("block", KeyCode.T);
        playerBindings.put("P1", p1Bindings);

        // Player 2 bindings
        Map<String, KeyCode> p2Bindings = new HashMap<>();
        p2Bindings.put("left", KeyCode.LEFT);
        p2Bindings.put("right", KeyCode.RIGHT);
        p2Bindings.put("up", KeyCode.UP);
        p2Bindings.put("down", KeyCode.DOWN);
        p2Bindings.put("light_punch", KeyCode.J);
        p2Bindings.put("heavy_punch", KeyCode.K);
        p2Bindings.put("light_kick", KeyCode.L);
        p2Bindings.put("heavy_kick", KeyCode.U);
        p2Bindings.put("block", KeyCode.I);
        playerBindings.put("P2", p2Bindings);

        // Initialize network state maps
        networkInputState.put("P1", new HashMap<>());
        networkInputState.put("P2", new HashMap<>());
        isNetworkPlayer.put("P1", false);
        isNetworkPlayer.put("P2", false);
    }

    public void handleKeyPressed(KeyCode key) {
        pressedKeys.add(key);
    }

    public void handleKeyReleased(KeyCode key) {
        pressedKeys.remove(key);
    }

    public boolean isActionPressed(String playerId, String action) {
        // Check if this player is controlled by network
        if (Boolean.TRUE.equals(isNetworkPlayer.get(playerId))) {
            Map<String, Boolean> networkState = networkInputState.get(playerId);
            return networkState != null && Boolean.TRUE.equals(networkState.get(action));
        }

        // Local player - check keyboard
        Map<String, KeyCode> bindings = playerBindings.get(playerId);
        if (bindings == null) return false;

        KeyCode key = bindings.get(action);
        return key != null && pressedKeys.contains(key);
    }

    // Network input methods
    public void setNetworkInput(String playerId, String action, boolean pressed) {
        Map<String, Boolean> state = networkInputState.get(playerId);
        if (state == null) {
            state = new HashMap<>();
            networkInputState.put(playerId, state);
        }
        state.put(action, pressed);
    }

    public void setPlayerNetworkControlled(String playerId, boolean isNetwork) {
        isNetworkPlayer.put(playerId, isNetwork);
        if (isNetwork) {
            // Clear network input state when switching to network control
            Map<String, Boolean> state = networkInputState.get(playerId);
            if (state != null) {
                state.clear();
            }
        }
    }

    public boolean isPlayerNetworkControlled(String playerId) {
        return Boolean.TRUE.equals(isNetworkPlayer.get(playerId));
    }

    public void clearAllInput() {
        pressedKeys.clear();
        for (Map<String, Boolean> state : networkInputState.values()) {
            state.clear();
        }
    }

    public void clearBuffer(String playerId) {
        pressedKeys.clear();
        Map<String, Boolean> state = networkInputState.get(playerId);
        if (state != null) {
            state.clear();
        }
    }

    // Debug method
    public Set<KeyCode> getPressedKeys() {
        return new HashSet<>(pressedKeys);
    }
}