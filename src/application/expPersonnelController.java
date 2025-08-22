package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javafx.scene.control.PasswordField;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import application.historiqueController;
import java.util.prefs.Preferences;

public class expPersonnelController implements Initializable {

    @FXML private TextField directoryField;
    @FXML private BorderPane borderPane;
    @FXML private Button addAirplane_btn;
    @FXML private Button addPersonnel_btn;
    @FXML private Button addVehicule_btn;
    @FXML private Button acceuilBtn;
    @FXML private Button fouillerBtn;
    @FXML private Button dashboardBtn;
    
    // Nouveaux contrôles pour l'exportation
    @FXML private RadioButton singlePersonnelRadio;
    @FXML private RadioButton allPersonnelRadio;
    @FXML private ComboBox<Personnel> personnelComboBox;
    @FXML private VBox personnelSelectionBox;
    @FXML private ToggleGroup exportType;
    
    private List<String> tableNames;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation des tables de la base de données
        tableNames = new ArrayList<>();
        tableNames.add("identite_personnelle");
        tableNames.add("identite_sociale");
        tableNames.add("identite_culturelle");
        tableNames.add("grade_actuel");
        tableNames.add("historique_grades");
        tableNames.add("formation_actuelle");
        tableNames.add("historique_postes");
        tableNames.add("ecole_formation_initiale");
        tableNames.add("ecole_civile");
        tableNames.add("ecole_militaire");
        tableNames.add("operation");
        tableNames.add("decoration");
        tableNames.add("medaille");
        tableNames.add("punition");
        tableNames.add("langue");
        tableNames.add("infos_specifiques_general");
        tableNames.add("personnel_naviguant");
        tableNames.add("maintenance");
        tableNames.add("specialite");
        tableNames.add("dotation_20_mai");
        tableNames.add("dotation_particuliere_config");
        tableNames.add("dotation_particuliere");
        tableNames.add("parametres_corporels");
        
        // Charger les personnels dans le ComboBox
        loadPersonnels();
    }
    
    /**
     * Charge tous les personnels depuis la base de données dans le ComboBox
     */
    private void loadPersonnels() {
        ObservableList<Personnel> personnels = FXCollections.observableArrayList();
        Connection connection = null;
        
        try {
            connection = getConnection();
            String sql = "SELECT matricule, nom, prenom FROM identite_personnelle ORDER BY nom, prenom";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String matricule = rs.getString("matricule");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    
                    personnels.add(new Personnel(matricule, nom, prenom));
                }
            }
            
            personnelComboBox.setItems(personnels);
            
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erreur", "Impossible de charger la liste des personnels: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void handleExportTypeChange() {
        // Afficher ou masquer la sélection de personnel en fonction du type d'exportation choisi
        personnelSelectionBox.setVisible(singlePersonnelRadio.isSelected());
        personnelSelectionBox.setManaged(singlePersonnelRadio.isSelected());
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir le répertoire d'exportation");
        File selectedDirectory = directoryChooser.showDialog(directoryField.getScene().getWindow());

        if (selectedDirectory != null) {
            directoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void handleExport() {
        String directory = directoryField.getText();
        if (directory.isEmpty()) {
            showAlert(AlertType.ERROR, "Erreur", "Veuillez choisir un répertoire d'exportation.");
            return;
        }
        
        // Vérifier si un personnel est sélectionné (si nécessaire)
        if (singlePersonnelRadio.isSelected() && personnelComboBox.getValue() == null) {
            showAlert(AlertType.ERROR, "Erreur", "Veuillez sélectionner un personnel à exporter.");
            return;
        }
        
        // NOUVELLE VÉRIFICATION D'IDENTITÉ
        if (!verifyUserIdentity()) {
            return; // Arrêter si l'authentification échoue
        }
        
     // Obtenir le nom d'utilisateur actuel depuis les préférences système
        Preferences prefs = Preferences.userNodeForPackage(expPersonnelController.class);
        String currentUser = prefs.get("username", "utilisateur");
        
        try {
            if (singlePersonnelRadio.isSelected()) {
                // Export d'un personnel spécifique
                Personnel selectedPersonnel = personnelComboBox.getValue();
                exportSinglePersonnel(directory, selectedPersonnel);
                
             // Construire le détail de l'action pour l'historique
                String detailAction = String.format("Exportation du personnel %s (%s %s) vers %s", 
                                   selectedPersonnel.getMatricule(), 
                                   selectedPersonnel.getNom(), 
                                   selectedPersonnel.getPrenom(),
                                   directory);
                
                // Enregistrer l'action dans l'historique
                historiqueController.enregistrerAction(
                    historiqueController.ACTION_EXPORTATION,
                    historiqueController.CIBLE_PERSONNEL,
                    detailAction,
                    currentUser
                );
            } else {
                // Export de tous les personnels
                exportAllPersonnel(directory);
                
             // Construire le détail de l'action pour l'historique
                String detailAction = String.format("Exportation de tous les personnels vers %s", directory);
                
                // Enregistrer l'action dans l'historique
                historiqueController.enregistrerAction(
                    historiqueController.ACTION_EXPORTATION,
                    historiqueController.CIBLE_ENSEMBLE,
                    detailAction,
                    currentUser
                );
            }
            
            showAlert(AlertType.INFORMATION, "Succès", "Exportation terminée avec succès.");
        } catch (IOException | SQLException e) {
            showAlert(AlertType.ERROR, "Erreur", "Une erreur s'est produite lors de l'exportation : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exporte les données d'un personnel spécifique
     */
    private void exportSinglePersonnel(String baseDirectory, Personnel personnel) throws IOException, SQLException {
        // Créer un dossier pour ce personnel
        String personnelDirName = personnel.getMatricule() + "_" + personnel.getNom() + "_" + personnel.getPrenom();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dirName = "export_" + personnelDirName + "_" + timestamp;
        
        File exportDir = new File(baseDirectory, dirName);
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Impossible de créer le répertoire d'exportation: " + exportDir.getAbsolutePath());
        }
        
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Pour chaque table, exporter les données liées à ce personnel
            for (String tableName : tableNames) {
                exportTableForPersonnel(connection, exportDir, tableName, personnel.getMatricule());
            }
            
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour vérifier l'identité de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    private boolean verifyUserIdentity() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification d'identité");
        dialog.setHeaderText("Veuillez vous authentifier pour exporter les données");
        
        // Définir la taille de la fenêtre de dialogue
        dialog.getDialogPane().setPrefSize(400, 250);
        dialog.setResizable(false);
        
        // Ajouter les boutons OK et Annuler
        ButtonType loginButtonType = new ButtonType("Se connecter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField identifiantField = new TextField();
        identifiantField.setPromptText("Identifiant");
        identifiantField.setPrefWidth(250);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setPrefWidth(250);
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        
        Label instructionLabel = new Label("Authentification requise pour l'exportation :");
        instructionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        
        HBox identifiantBox = new HBox(10);
        identifiantBox.setAlignment(Pos.CENTER_LEFT);
        Label identifiantLabel = new Label("Identifiant:");
        identifiantLabel.setPrefWidth(80);
        identifiantBox.getChildren().addAll(identifiantLabel, identifiantField);
        
        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Mot de passe:");
        passwordLabel.setPrefWidth(80);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        content.getChildren().addAll(instructionLabel, identifiantBox, passwordBox);
        
        dialog.getDialogPane().setContent(content);
        
        // Mettre le focus sur le champ identifiant
        Platform.runLater(() -> identifiantField.requestFocus());
        
        // Gérer la touche Entrée
        identifiantField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> {
            if (dialog.getDialogPane().lookupButton(loginButtonType) != null) {
                ((Button) dialog.getDialogPane().lookupButton(loginButtonType)).fire();
            }
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == loginButtonType) {
            String identifiant = identifiantField.getText().trim();
            String password = passwordField.getText();
            
            if (identifiant.isEmpty() || password.isEmpty()) {
                showAlert(AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
                return false;
            }
            
            // Vérifier les identifiants dans la base de données
            return authenticateUser(identifiant, password);
        }
        
        return false;
    }

    /**
     * Authentifie un utilisateur en vérifiant ses identifiants dans la base de données
     * @param identifiant L'identifiant de l'utilisateur
     * @param password Le mot de passe de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    private boolean authenticateUser(String identifiant, String password) {
        Connection connection = null;
        
        try {
            connection = getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE identifiant = ? AND password = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, identifiant);
                stmt.setString(2, password);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            return true;
                        } else {
                            showAlert(AlertType.ERROR, "Authentification échouée", 
                                "Identifiant ou mot de passe incorrect.");
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur", 
                "Erreur lors de la vérification des identifiants : " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Exporte les données de tous les personnels
     */
    private void exportAllPersonnel(String baseDirectory) throws IOException, SQLException {
        // Créer un dossier pour tous les personnels
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dirName = "export_all_personnel_" + timestamp;
        
        File exportDir = new File(baseDirectory, dirName);
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Impossible de créer le répertoire d'exportation: " + exportDir.getAbsolutePath());
        }
        
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Pour chaque table, exporter toutes les données
            for (String tableName : tableNames) {
                exportAllFromTable(connection, exportDir, tableName);
            }
            
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    /**
     * Exporte les données d'une table pour un personnel spécifique
     */
    private void exportTableForPersonnel(Connection connection, File exportDir, String tableName, String matricule) throws IOException, SQLException {
        // Vérifier si la table a une colonne matricule
        if (!tableHasMatriculeColumn(connection, tableName)) {
            // Si la table n'a pas de colonne matricule, elle n'est pas liée directement au personnel
            return;
        }
        
        File csvFile = new File(exportDir, tableName + ".csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            // Obtenir les colonnes de la table
            List<String> columns = getTableColumns(connection, tableName);
            
            // Écrire l'en-tête CSV
            writer.write(String.join(",", columns) + "\n");
            
            // Préparer la requête SQL
            String sql = "SELECT * FROM " + tableName + " WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, matricule);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> rowValues = new ArrayList<>();
                        
                        for (String column : columns) {
                            Object value = rs.getObject(column);
                            rowValues.add(value != null ? escapeCSV(value.toString()) : "");
                        }
                        
                        writer.write(String.join(",", rowValues) + "\n");
                    }
                }
            }
        }
    }
    
    /**
     * Exporte toutes les données d'une table
     */
    private void exportAllFromTable(Connection connection, File exportDir, String tableName) throws IOException, SQLException {
        File csvFile = new File(exportDir, tableName + ".csv");
        
        try (FileWriter writer = new FileWriter(csvFile)) {
            // Obtenir les colonnes de la table
            List<String> columns = getTableColumns(connection, tableName);
            
            // Écrire l'en-tête CSV
            writer.write(String.join(",", columns) + "\n");
            
            // Préparer la requête SQL
            String sql = "SELECT * FROM " + tableName;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    List<String> rowValues = new ArrayList<>();
                    
                    for (String column : columns) {
                        Object value = rs.getObject(column);
                        rowValues.add(value != null ? escapeCSV(value.toString()) : "");
                    }
                    
                    writer.write(String.join(",", rowValues) + "\n");
                }
            }
        }
    }
    
    /**
     * Vérifie si une table a une colonne 'matricule'
     */
    private boolean tableHasMatriculeColumn(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet columns = meta.getColumns(null, null, tableName, "matricule");
        
        boolean hasMatricule = columns.next();
        columns.close();
        
        return hasMatricule;
    }
    
    /**
     * Récupère les noms de colonnes d'une table
     */
    private List<String> getTableColumns(Connection connection, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return columns;
    }
    
    /**
     * Échappe une valeur pour le format CSV
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        
        // Si la valeur contient une virgule, des guillemets ou des sauts de ligne, l'entourer de guillemets
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Remplacer les guillemets par deux guillemets
            value = value.replace("\"", "\"\"");
            // Entourer de guillemets
            value = "\"" + value + "\"";
        }
        
        return value;
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
            
            Stage stage = (Stage) directoryField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accueil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du retour à l'accueil");
        }
    }

    private void showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showErrorAlert(String message) {
        showAlert(AlertType.ERROR, "Erreur", message);
    }
    
    /**
     * Obtient une connexion à la base de données
     */
    private Connection getConnection() throws SQLException {
        try {
            // Charger le pilote JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Paramètres de connexion
            String url = "jdbc:mysql://localhost:3306/exploit";
            String username = "marco";
            String password = "29Papa278.";
            
            // Établir la connexion
            return DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC non trouvé", e);
        }
    }
    
    // Méthodes existantes d'origine de expPersonnelController (gestion des menus, etc.)
    
    @FXML
    private void handlePersonnelModDown(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper4.fxml"));
            Parent personnelModifyDownloadView = loader.load();
            borderPane.setCenter(personnelModifyDownloadView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleImportPersonnel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper7.fxml"));
            Parent importPersonnelView = loader.load();
            borderPane.setCenter(importPersonnelView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la vue d'importation du personnel");
        }
    }
    
    @FXML
    private void handleExportPersonnel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper8.fxml"));
            Parent importPersonnelView = loader.load();
            borderPane.setCenter(importPersonnelView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur Exportation du personnel");
        }
    }
    
    @FXML
    void handleNewAircraft(ActionEvent event) {
        // Logique pour ajouter un nouvel aéronef
        System.out.println("Ajout d'un nouvel aéronef");
    }
    
    @FXML
    private void handleNewPersonnel(ActionEvent event) {
        // Logique pour ajouter un nouveau personnel
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper3.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("personnel.css").toExternalForm());
            
            Stage stage = (Stage) addPersonnel_btn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ajout d'un nouveau personnel");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        System.out.println("Ajout d'un nouveau personnel");
    }
    
    @FXML
    private void handleNewWeapon(ActionEvent event) {
        // Logique pour ajouter un nouveau véhicule
        
    }
    
    @FXML
    private void showAboutDialog(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText(null);
        alert.setContentText("Application de gestion militaire pour le personnel, les véhicules, l'armement, le matériel et les aéronefs.");
        alert.showAndWait();
    }
    
    @FXML
    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper6.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("dashboard.css").toExternalForm());
            
            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement du tableau de bord");
        }
    }
    
    @FXML
    private void showHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
            
            Stage stage = (Stage) acceuilBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accueil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de l'accueil");
        }
    }
    
    @FXML
    private void showSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper5.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("fouiller.css").toExternalForm());
            
            Stage stage = (Stage) fouillerBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Fouille");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la page de recherche");
        }
    }
    
    /**
     * Classe interne représentant un personnel pour le ComboBox
     */
    public static class Personnel {
        private String matricule;
        private String nom;
        private String prenom;
        
        public Personnel(String matricule, String nom, String prenom) {
            this.matricule = matricule;
            this.nom = nom;
            this.prenom = prenom;
        }
        
        public String getMatricule() { return matricule; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        
        @Override
        public String toString() {
            return nom + " " + prenom + " (" + matricule + ")";
        }
    }
}