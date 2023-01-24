package com.gameoflife;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.Random;

public class GameOfLife extends Application {

    private boolean[][] grid;
    private int rows;
    private int cols;
    private GridPane gridPane;
    private volatile boolean isPlaying = true;
    private int playingSpeed = 3;
    private int aliveCells = 0;
    private int totalGenerations = 0;
    Label aliveCellsLabel = new Label("Alive cells: " + aliveCells);
    Label totalGenerationsLabel = new Label("Generations: " + totalGenerations);

    public GameOfLife() {
        this.rows = 30;
        this.cols = 30;
        this.grid = new boolean[this.rows][this.cols];
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void initializeGrid() {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                this.grid[i][j] = Math.random() < 0.5;
            }
        }
    }

    private void updateGrid() {
        aliveCells = 0;
        boolean[][] newGrid = new boolean[rows][cols];
        for (int i = 0; i < rows; i++)
            if (cols >= 0) System.arraycopy(grid[i], 0, newGrid[i], 0, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int neighbors = countNeighbors(i, j);
                // Une cellule vivante avec 2 ou 3 voisins survit, une cellule morte avec exactement 3 voisins devient vivante
                newGrid[i][j] = grid[i][j] ? (neighbors == 2 || neighbors == 3) : (neighbors == 3);
                Button button = (Button) gridPane.getChildren().get(i * cols + j);
                button.setStyle("-fx-background-color: " + (newGrid[i][j] ? "black" : "white"));
                int finalI = i; int finalJ = j;
                button.setOnAction(e -> {
                    newGrid[finalI][finalJ] = !newGrid[finalI][finalJ];
                    button.setStyle("-fx-background-color: " + (newGrid[finalI][finalJ] ? "black" : "white"));
                });
                if (newGrid[i][j]) aliveCells++;
            }
        }
        grid = newGrid;
        totalGenerations++;
        totalGenerationsLabel.setText("Generations: " + totalGenerations);
        aliveCellsLabel.setText("Alive cells: " + aliveCells);
    }

    private void updateGridPane() {
        gridPane.getChildren().clear();
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                Button button = new Button();
                button.setMinSize(10, 10);
                button.setMaxSize(10, 10);
                button.setStyle("-fx-background-color: " + (grid[i][j] ? "black" : "white"));
                int finalI = i;
                int finalJ = j;
                button.setOnAction(e -> {
                    grid[finalI][finalJ] = !grid[finalI][finalJ];
                    button.setStyle("-fx-background-color: " + (grid[finalI][finalJ] ? "black" : "white"));
                });
                gridPane.add(button, i, j);
            }
        }
    }

    private int countNeighbors(int x, int y) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int row = x + i;
                int col = y + j;
                if (row < 0 || row >= rows || col < 0 || col >= cols) continue;
                if (grid[row][col]) count++;
            }
        }
        return count;
    }

    public void start(Stage primaryStage) {
        // Initialisation de la grille
        this.initializeGrid();
        // Initialisation de l'interface graphique du jeu
        gridPane = new GridPane();
        gridPane.setStyle("-fx-background-color: #fff");
        this.updateGridPane();
        // Création d'un VBox à gauche pour y ajouter les boutons de control
        VBox leftVBox = new VBox();
        leftVBox.setSpacing(10);
        leftVBox.setPadding(new Insets(10, 10, 10, 10));
        leftVBox.setStyle("-fx-background-color: #395060");
        // Ajout des boutons
        this.addButtons(leftVBox, primaryStage);
        // Création d'un menu pour charger et sauvegarder des configurations de grille
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem loadConfig = new MenuItem("Load Configuration");
        loadConfig.setOnAction(event -> this.loadConfig(primaryStage));
        fileMenu.getItems().add(loadConfig);
        MenuItem saveConfig = new MenuItem("Save Configuration");
        saveConfig.setOnAction(event -> this.saveConfig(primaryStage));
        fileMenu.getItems().add(saveConfig);
        menuBar.getMenus().add(fileMenu);
        // Création d'un BorderPane qui regroupera les boutons, le menu et le jeu en lui même
        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(leftVBox);
        borderPane.setRight(gridPane);
        borderPane.setTop(menuBar);
        borderPane.setStyle("-fx-background-color: #395060");
        // Création de la scène et ajout du CSS
        Scene scene = new Scene(borderPane, 550, 550);
        URL url = GameOfLife.class.getResource("/interface.css");

        if (url == null) System.out.println("Fichier CSS introuvable");
        else scene.getStylesheets().add(url.toExternalForm());
        // Parametrage et affichage de la fenetre
        primaryStage.setScene(scene);
        primaryStage.setTitle("Game Of Life");
        primaryStage.show();
        primaryStage.setWidth(550);
        primaryStage.setHeight(550);
        primaryStage.setResizable(false);
        // Boucle de jeu dans un thread séparé
        Thread gameThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (isPlaying) {
                    Platform.runLater(this::updateGrid);
                    try {
                        Thread.sleep(1000 / playingSpeed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        // Ajout d'un bouton pour fermer le jeu.
        Button exitButton = new Button("Exit");
        exitButton.getStyleClass().add("sbutton15");
        leftVBox.getChildren().add(exitButton);
        exitButton.setOnAction(e -> {
            gameThread.interrupt();
            primaryStage.close();
        });
        // Démarrage de la boucle de jeu
        gameThread.setDaemon(false);
        gameThread.start();
    }

    private void loadConfig(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Configuration File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Game of Life Config Files", "*.cyb"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                String[] dimensions = reader.readLine().split(" ");
                this.rows = Integer.parseInt(dimensions[0]);
                this.cols = Integer.parseInt(dimensions[1]);
                this.grid = new boolean[rows][cols];
                for (int i = 0; i < rows; i++) {
                    char[] row = reader.readLine().toCharArray();
                    for (int j = 0; j < this.cols; j++) {
                        this.grid[i][j] = row[j] == '1';
                    }
                }
                reader.close();
                updateGridPane();
                primaryStage.setWidth(this.rows * 10 + 250);
                primaryStage.setHeight(Math.max(this.cols * 10, 350) + 50);
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred while loading configuration file");
                alert.setContentText("Please make sure the file is in the correct format and try again.");
                alert.showAndWait();
            }
        }
    }

    private void saveConfig(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Configuration File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Game of Life Config Files", ".cyb"),
                new FileChooser.ExtensionFilter("All Files", ".*")
        );
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));
                writer.write(rows + " " + cols);
                writer.newLine();
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        writer.write(grid[i][j] ? '1' : '0');
                    }
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred while saving configuration file");
                alert.setContentText("Please try again.");
                alert.showAndWait();
            }
        }
    }

    // Oui c'est moche et il y a probablement des centaines de facon plus élégante d'obtenir le même résultat
    // Ce projet est ma 1ere experience avec JavaFX donc on peux dire que c'est réalisé a l'aide de mes connaissances
    private void addButtons(VBox leftVBox, Stage primaryStage) {
        // Ajout des boutons Play/Pause
        HBox hbox = new HBox();
        Button playButton = new Button("Play");
        playButton.getStyleClass().add("sbutton");
        playButton.setOnAction(e -> isPlaying = true);

        Button stopButton = new Button("Pause");
        stopButton.getStyleClass().add("sbutton");
        stopButton.setOnAction(e -> isPlaying = false);

        hbox.getChildren().addAll(playButton, stopButton);
        hbox.setSpacing(10);
        leftVBox.getChildren().add(hbox);

        // Ajout de champs pour modifier la taille de la grille
        hbox = new HBox();
        Label rowsLabel = new Label("Rows: ");
        rowsLabel.getStyleClass().add("txt-label");
        rowsLabel.setTextFill(Color.WHITE);
        TextField rowsTextField = new TextField();
        rowsTextField.setText(String.valueOf(this.rows));
        rowsTextField.getStyleClass().add("text-field");

        Label colsLabel = new Label("Cols: ");
        colsLabel.getStyleClass().add("txt-label");
        colsLabel.setTextFill(Color.WHITE);
        TextField colsTextField = new TextField();
        colsTextField.setText(String.valueOf(this.cols));
        colsTextField.getStyleClass().add("text-field");

        hbox.getChildren().addAll(rowsLabel, rowsTextField, colsLabel, colsTextField);
        hbox.setSpacing(10);
        leftVBox.getChildren().add(hbox);

        // Ajout d'un bouton pour valider les modifications de la taille de la grille
        Button submitButton = new Button("Apply");
        submitButton.getStyleClass().add("sbutton15");
        leftVBox.getChildren().add(submitButton);

        submitButton.setOnAction(e -> {
            try {
                int newRows = Integer.parseInt(rowsTextField.getText());
                int newCols = Integer.parseInt(colsTextField.getText());
                // Vérifiez si les nouvelles valeurs sont valides
                if (newRows > 0 && newCols > 0 && newRows <= 1000 && newCols <= 1000) {
                    this.isPlaying = false;
                    this.rows = newRows;
                    this.cols = newCols;
                    this.grid = new boolean[newRows][newCols];
                    initializeGrid();
                    updateGridPane();
                    primaryStage.setWidth(this.rows * 10 + 250);
                    primaryStage.setHeight(Math.max(this.cols * 10, 350) + 50);
                    rowsTextField.setText(String.valueOf(this.rows));
                    colsTextField.setText(String.valueOf(this.cols));
                    this.isPlaying = true;
                } else {
                    // Affiche une erreur si les valeurs sont incorrectes
                    System.out.println("Invalid input: rows and cols must be greater than 0 and less than 1000.");
                }
            } catch (NumberFormatException ex) {
                // Affiche une erreur si les valeurs saisies ne sont pas des nombres entiers
                System.out.println("Invalid input: rows and cols must be integers.");
            }
        });

        // Ajout d'un champ pour saisir la vitesse de jeu
        Label speedLabel = new Label("Speed: ");
        speedLabel.getStyleClass().add("txt-label");
        speedLabel.setTextFill(Color.WHITE);
        TextField speedTextField = new TextField();
        speedTextField.setText(playingSpeed + "");
        speedTextField.getStyleClass().add("text-field");
        // Ajout d'un bouton pour diminuer la vitesse de jeu
        Button speedDownBtn = new Button("-");
        speedDownBtn.getStyleClass().add("sbutton2");
        speedDownBtn.setOnAction(e -> {
            playingSpeed--;
            if (playingSpeed < 1) playingSpeed = 1;
            speedTextField.setText(playingSpeed + "");
        });
        // Ajout d'un bouton pour augmenter la vitesse de jeu
        Button speedUpBtn = new Button("+");
        speedUpBtn.getStyleClass().add("sbutton2");
        speedUpBtn.setOnAction(e -> {
            playingSpeed++;
            if (playingSpeed > 1000) playingSpeed = 1000;
            speedTextField.setText(playingSpeed + "");
        });
        hbox = new HBox();
        hbox.getChildren().addAll(speedLabel, speedTextField, speedDownBtn, speedUpBtn);
        hbox.setSpacing(10);
        leftVBox.getChildren().add(hbox);
        //Ajout d'un écouteur pour le champ de saisie de vitesse pour s'assurer que la valeur saisie est valide
        speedTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int newSpeed = Integer.parseInt(newValue);
                if (newSpeed < 1) newSpeed = 1;
                if (newSpeed > 1000) newSpeed = 1000;
                playingSpeed = newSpeed;
            } catch (NumberFormatException e) {
                speedTextField.setText(oldValue);
            }
        });
        // Ajout d'un texte pour indiquer le nombre de cellules vivantes
        aliveCellsLabel.getStyleClass().add("txt-label");
        aliveCellsLabel.setTextFill(Color.WHITE);
        leftVBox.getChildren().add(aliveCellsLabel);
        // Ajout d'un texte pour indiquer le nombre de generations totales
        totalGenerationsLabel.getStyleClass().add("txt-label");
        totalGenerationsLabel.setTextFill(Color.WHITE);
        leftVBox.getChildren().add(totalGenerationsLabel);
        // Ajout d'un bouton Thanos pour supprimer la moitié des cellules vivantes
        Button thanosButton = new Button("Thanos");
        thanosButton.getStyleClass().add("sbutton15");
        thanosButton.setOnAction(e -> {
            this.isPlaying = false;
            int cellsToKill = aliveCells / 2;
            Random random = new Random();
            while (cellsToKill > 0) {
                int i = random.nextInt(rows);
                int j = random.nextInt(cols);
                if (grid[i][j]) {
                    grid[i][j] = false;
                    cellsToKill--;
                }
            }
            this.isPlaying = true;
            updateGridPane();
        });
        leftVBox.getChildren().add(thanosButton);
        // Ajout d'un bouton pour vider la grille
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("sbutton");
        clearButton.setOnAction(e -> {
            this.isPlaying = false;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    grid[i][j] = false;
                }
            }
            updateGridPane();
            this.isPlaying = true;
        });
        // Ajout d'un bouton pour remplir la grille aléatoirement
        Button randomButton = new Button("Fill");
        randomButton.getStyleClass().add("sbutton");
        randomButton.setOnAction(e -> {
            this.isPlaying = false;
            Random random = new Random();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    grid[i][j] = random.nextBoolean();
                }
            }
            updateGridPane();
            this.isPlaying = true;
        });
        hbox = new HBox();
        hbox.getChildren().addAll(clearButton, randomButton);
        hbox.setSpacing(10);
        leftVBox.getChildren().add(hbox);
    }

}