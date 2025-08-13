package Client;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


public class LoginUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        Button loginBtn = new Button("Login");
        Button signupBtn = new Button("Sign Up");
        Hyperlink leaderboardLink = new Hyperlink("View Leaderboard");

        // Build a form grid
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        usernameField.setPrefWidth(220);
        passwordField.setPrefWidth(220);

        // Action buttons
        HBox actions = new HBox(10, loginBtn, signupBtn);
        actions.setAlignment(Pos.CENTER);
        loginBtn.setPrefWidth(120);
        signupBtn.setPrefWidth(120);

        // Title
        Label title = new Label("Street Fighter");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        // Card container
        VBox card = new VBox(16, title, form, actions, leaderboardLink);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(320);
        card.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.22),
                new CornerRadii(16),
                Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
                Color.rgb(255, 255, 255, 0.25),
                BorderStrokeStyle.SOLID,
                new CornerRadii(16),
                new BorderWidths(1)
        )));
        DropShadow shadow = new DropShadow();
        shadow.setRadius(16);
        shadow.setOffsetY(6);
        shadow.setColor(Color.rgb(0, 0, 0, 0.45));
        card.setEffect(shadow);

        // Root with background image and subtle overlay
        StackPane root = new StackPane();

        ImageView bgView = new ImageView(new Image(getClass().getResource("/images/LogBack.jpeg").toExternalForm()));
        bgView.setPreserveRatio(true);
        bgView.setSmooth(true);
        bgView.fitWidthProperty().bind(root.widthProperty());
        bgView.fitHeightProperty().bind(root.heightProperty());

        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.45));
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());

        root.getChildren().addAll(bgView, overlay, card);
        StackPane.setAlignment(card, Pos.CENTER);

        // Layout padding
        StackPane.setMargin(card, new Insets(24));

        // UX tweaks
        loginBtn.setDefaultButton(true);
        loginBtn.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        // --- Login Handler ---
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.loginPlayer(user, pass)) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Login Success!");
                a.showAndWait();
                showGameModeDialog(user);
                primaryStage.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid login!").showAndWait();
            }
        });

        // --- Signup Handler ---
        signupBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.registerPlayer(user, pass)) {
                new Alert(Alert.AlertType.INFORMATION, "Signup Success! You can now login.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Username already exists!").showAndWait();
            }
        });

        // --- Leaderboard Link ---
        leaderboardLink.setOnAction(e -> LeaderboardUI.display());

        Scene scene = new Scene(root, 420, 520);

        primaryStage.setTitle("Street Fighter Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(getClass().getResource("/images/cover.jpg").toExternalForm()));
        primaryStage.show();
    }

    private void showGameModeDialog(String username) {
        Stage dialog = new Stage();
        dialog.setTitle("Select Game Mode");

        Button playPcBtn = new Button("Play vs PC");
        Button playMultiBtn = new Button("Play Multiplayer");

        playPcBtn.setMaxWidth(Double.MAX_VALUE);
        playMultiBtn.setMaxWidth(Double.MAX_VALUE);

        VBox dialogLayout = new VBox(15,
                new Label("Choose a game mode:"),
                playPcBtn,
                playMultiBtn
        );
        dialogLayout.setAlignment(Pos.CENTER);
        dialogLayout.setPadding(new Insets(20));

        playPcBtn.setOnAction(ev -> {
            dialog.close();
            SinglePlayerGame.username = username;
            SinglePlayerGame.openSinglePlayer(username);

        });

        playMultiBtn.setOnAction(ev -> {
            dialog.close();
            GameClient.openMultiplayer(username);
        });


        Scene scene = new Scene(dialogLayout, 250, 180);
        dialog.setScene(scene);
        dialog.show();
    }

    public static void main(String[] args) {
        launch(args);


    }
}
