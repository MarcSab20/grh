package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;

import application.historiqueController;
import java.util.prefs.Preferences;

public class impPersonnelController implements Initializable {
    
    @FXML private BorderPane borderPane;
    @FXML private FlowPane dropZonesContainer;
    @FXML private Label statusLabel;
    @FXML private Button importButton;
    @FXML private Button clearButton;
    
    @FXML private Button addAirplane_btn;
    @FXML private Button addPersonnel_btn;
    @FXML private Button addVehicule_btn;
    @FXML private Button acceuilBtn;
    @FXML private Button fouillerBtn;
    @FXML private Button dashboardBtn;
    
    // Liste des noms de tables dans la base de données
    private List<String> tableNames = Arrays.asList(
        "identite_personnelle",
        "identite_sociale",
        "identite_culturelle",
        "grade_actuel",
        "historique_grades",
        "formation_actuelle",
        "historique_postes",
        "ecole_formation_initiale",
        "ecole_civile",
        "ecole_militaire",
        "operation",
        "decoration",
        "medaille",
        "punition",
        "langue",
        "infos_specifiques_general",
        "personnel_naviguant",
        "maintenance",
        "specialite",
        "dotation_20_mai",
        "dotation_particuliere_config",
        "dotation_particuliere",
        "parametres_corporels"
    );
    
    // Map pour stocker les fichiers déposés pour chaque table
    private Map<String, File> droppedFiles = new HashMap<>();
    
    // Map pour stocker les VBox des zones de drop
    private Map<String, VBox> dropZones = new HashMap<>();
    
    // Map pour stocker les structures de colonnes des tables
    private Map<String, List<String>> tableColumns = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Créer les zones de drop pour chaque table
        createDropZones();
        
        // Charger les structures de colonnes des tables
        loadTableStructures();
        
        // Initialiser l'état de l'interface
        updateStatus("Prêt à recevoir les fichiers CSV. Veuillez déposer les 23 fichiers correspondants.");
    }
    
    /**
     * Crée les zones de drop pour chaque table
     */
    private void createDropZones() {
        for (String tableName : tableNames) {
            VBox dropZone = createDropZone(tableName);
            dropZones.put(tableName, dropZone);
            dropZonesContainer.getChildren().add(dropZone);
        }
    }
    
    /**
     * Crée une zone de drop pour une table spécifique
     */
    private VBox createDropZone(String tableName) {
        VBox dropZone = new VBox();
        dropZone.getStyleClass().add("table-drop-zone");
        dropZone.setPrefWidth(200);
        dropZone.setPrefHeight(120);
        
        // Ajouter un indicateur d'état (rouge par défaut)
        ImageView statusIcon = new ImageView(new Image(getClass().getResourceAsStream("/application/file-icon.png")));
        statusIcon.setFitHeight(32);
        statusIcon.setFitWidth(32);
        
        // Ajouter le nom de la table
        Label tableNameLabel = new Label(tableName);
        tableNameLabel.getStyleClass().add("table-name-label");
        
        // Ajouter un label pour le nom du fichier
        Label fileNameLabel = new Label("Aucun fichier");
        fileNameLabel.getStyleClass().add("file-name-label");
        
        // Ajouter les éléments à la zone de drop
        dropZone.getChildren().addAll(statusIcon, tableNameLabel, fileNameLabel);
        
        // Configurer les événements de drag and drop
        setupDropZoneEvents(dropZone, tableName, fileNameLabel);
        
        return dropZone;
    }
    
    /**
     * Configure les événements de drag and drop pour une zone
     */
    private void setupDropZoneEvents(VBox dropZone, String tableName, Label fileNameLabel) {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles() && db.getFiles().size() == 1) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    // Vérifier si le fichier correspond à la table attendue
                    String expectedFileName = tableName + ".csv";
                    if (file.getName().equalsIgnoreCase(expectedFileName)) {
                        // Fichier correct
                        droppedFiles.put(tableName, file);
                        fileNameLabel.setText(file.getName());
                        dropZone.getStyleClass().remove("table-drop-zone-error");
                        dropZone.getStyleClass().add("table-drop-zone-success");
                        success = true;
                    } else {
                        // Fichier incorrect
                        showErrorAlert("Fichier incorrect", "Le fichier doit s'appeler " + expectedFileName);
                        dropZone.getStyleClass().add("table-drop-zone-error");
                    }
                } else {
                    showErrorAlert("Format invalide", "Le fichier doit avoir une extension .csv");
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
            
            // Mettre à jour l'état du bouton d'importation
            updateImportButtonState();
        });
    }
    
    /**
     * Charge les structures de colonnes des tables depuis la base de données
     */
    private void loadTableStructures() {
        Connection connection = null;
        
        try {
            connection = getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            
            for (String tableName : tableNames) {
                List<String> columns = new ArrayList<>();
                
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        columns.add(columnName);
                    }
                }
                
                tableColumns.put(tableName, columns);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Erreur de base de données", "Impossible de charger les structures des tables: " + e.getMessage());
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
    
    /**
     * Met à jour l'état du bouton d'importation
     */
    private void updateImportButtonState() {
        // Activer le bouton d'importation uniquement si tous les fichiers sont présents
        boolean allFilesPresent = tableNames.size() == droppedFiles.size();
        importButton.setDisable(!allFilesPresent);
        
        // Mettre à jour le message d'état
        int filesCount = droppedFiles.size();
        if (filesCount == 0) {
            updateStatus("Prêt à recevoir les fichiers CSV. Veuillez déposer les 23 fichiers correspondants.");
        } else if (filesCount < tableNames.size()) {
            updateStatus(filesCount + " fichiers sur " + tableNames.size() + " ont été déposés. " +
                    "Il manque encore " + (tableNames.size() - filesCount) + " fichiers.");
        } else {
            updateStatus("Tous les fichiers ont été déposés. Cliquez sur 'Importer' pour continuer.");
        }
    }
    
    /**
     * Met à jour le message d'état
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * Gère l'action du bouton Effacer tout
     */
    @FXML
    private void handleClear() {
        // Réinitialiser les fichiers déposés
        droppedFiles.clear();
        
        // Réinitialiser l'apparence des zones de drop
        for (String tableName : tableNames) {
            VBox dropZone = dropZones.get(tableName);
            dropZone.getStyleClass().remove("table-drop-zone-success");
            dropZone.getStyleClass().remove("table-drop-zone-error");
            
            // Réinitialiser le label du nom de fichier
            Label fileNameLabel = (Label) dropZone.getChildren().get(2);
            fileNameLabel.setText("Aucun fichier");
        }
        
        // Mettre à jour l'état du bouton d'importation
        updateImportButtonState();
    }
    
    /**
     * Gère l'action du bouton Importer
     */
    @FXML
    private void handleImport() {
        // Vérifier si tous les fichiers nécessaires sont présents
        if (droppedFiles.size() < tableNames.size()) {
            showErrorAlert("Fichiers manquants", "Tous les fichiers CSV nécessaires n'ont pas été fournis.");
            return;
        }
        
        // NOUVELLE VÉRIFICATION D'IDENTITÉ
        if (!verifyUserIdentity()) {
            updateStatus("Importation annulée : authentification échouée.");
            return;
        }
        
        // Désactiver les boutons pendant l'importation
        importButton.setDisable(true);
        clearButton.setDisable(true);
        updateStatus("Importation en cours...");
        
        // Obtenir le nom d'utilisateur actuel depuis les préférences système
        Preferences prefs = Preferences.userNodeForPackage(impPersonnelController.class);
        String currentUser = prefs.get("username", "utilisateur");
        
        // Vérifier la validité des fichiers CSV
        try {
            for (String tableName : tableNames) {
                File file = droppedFiles.get(tableName);
                if (!validateCsvFile(file, tableName)) {
                    // Si un fichier est invalide, annuler l'importation
                    updateStatus("Importation annulée en raison d'erreurs de validation.");
                    importButton.setDisable(false);
                    clearButton.setDisable(false);
                    return;
                }
            }
            
            // Si tous les fichiers sont valides, importer les données
            String matricule = extractMatriculeFromPersonnelFile();
            if (matricule == null) {
                showErrorAlert("Données invalides", "Impossible d'extraire le matricule du personnel à partir du fichier identite_personnelle.csv");
                updateStatus("Importation annulée : matricule non trouvé dans le fichier identite_personnelle.csv");
                importButton.setDisable(false);
                clearButton.setDisable(false);
                return;
            }
            
            // Vérifier si le matricule existe déjà
            boolean matriculeExists = checkMatriculeExists(matricule);
            
            // Demander confirmation si le matricule existe déjà
            if (matriculeExists) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Matricule existant");
                confirmAlert.setHeaderText("Le matricule " + matricule + " existe déjà dans la base de données.");
                confirmAlert.setContentText("Voulez-vous écraser toutes les données existantes pour ce personnel ?");
                
                ButtonType buttonYes = new ButtonType("Oui, écraser");
                ButtonType buttonNo = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);
                
                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() == buttonNo) {
                    updateStatus("Importation annulée par l'utilisateur.");
                    importButton.setDisable(false);
                    clearButton.setDisable(false);
                    return;
                }
            }
            
            // Extraire le nom et prénom pour l'historique
            String nom = "";
            String prenom = "";
            try {
                File personnelFile = droppedFiles.get("identite_personnelle");
                if (personnelFile != null) {
                    try (BufferedReader br = new BufferedReader(new FileReader(personnelFile))) {
                        // Lire la première ligne (en-têtes)
                        String headerLine = br.readLine();
                        if (headerLine != null) {
                            String[] headers = headerLine.split(",");
                            int nomIndex = -1;
                            int prenomIndex = -1;
                            
                            // Trouver les index des colonnes nom et prenom
                            for (int i = 0; i < headers.length; i++) {
                                if (headers[i].trim().equalsIgnoreCase("nom")) {
                                    nomIndex = i;
                                } else if (headers[i].trim().equalsIgnoreCase("prenom")) {
                                    prenomIndex = i;
                                }
                            }
                            
                            // Lire la deuxième ligne (données)
                            String dataLine = br.readLine();
                            if (dataLine != null && nomIndex >= 0 && prenomIndex >= 0) {
                                String[] values = parseCSVLine(dataLine);
                                
                                if (nomIndex < values.length) {
                                    nom = values[nomIndex].trim();
                                }
                                
                                if (prenomIndex < values.length) {
                                    prenom = values[prenomIndex].trim();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Ignorer les erreurs lors de l'extraction du nom/prénom
                e.printStackTrace();
            }
            
            // Importer les données avec écrasement
            importDataWithOverwrite(matricule, matriculeExists);
            
            // Construire le détail de l'action pour l'historique
            String action = matriculeExists ? historiqueController.ACTION_MODIFICATION : historiqueController.ACTION_IMPORTATION;
            String actionPrefix = matriculeExists ? "Écrasement et mise à jour" : "Importation";
            String detailAction = String.format("%s du personnel %s (%s %s)", 
                               actionPrefix, matricule, nom.toUpperCase(), prenom);
            
            // Enregistrer l'action dans l'historique
            historiqueController.enregistrerAction(
                action,
                historiqueController.CIBLE_PERSONNEL,
                detailAction,
                currentUser
            );
            
            // Afficher un message de succès
            String successMessage = matriculeExists ? 
                    "Écrasement et mise à jour des données terminés avec succès pour le matricule " + matricule :
                    "Importation des données terminée avec succès pour le matricule " + matricule;
            
            showInfoAlert("Succès", successMessage);
            updateStatus(successMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur d'importation", "Une erreur s'est produite lors de l'importation : " + e.getMessage());
            updateStatus("Importation échouée : " + e.getMessage());
        } finally {
            // Réactiver les boutons
            importButton.setDisable(false);
            clearButton.setDisable(false);
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour vérifier l'identité de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    private boolean verifyUserIdentity() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification d'identité");
        dialog.setHeaderText("Veuillez vous authentifier pour continuer");
        
        // Définir la taille de la fenêtre de dialogue
        dialog.getDialogPane().setPrefSize(400, 250);
        dialog.setResizable(false);
        
        // Ajouter les boutons OK et Annuler
        ButtonType loginButtonType = new ButtonType("Se connecter", ButtonData.OK_DONE);
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
        
        Label instructionLabel = new Label("Entrez vos identifiants pour confirmer cette action :");
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
                showErrorAlert("Erreur", "Veuillez remplir tous les champs.");
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
                            showErrorAlert("Authentification échouée", 
                                "Identifiant ou mot de passe incorrect.");
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors de la vérification des identifiants : " + e.getMessage());
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
     * Importe les données avec écrasement complet des données existantes
     */
    private void importDataWithOverwrite(String matricule, boolean matriculeExists) throws SQLException {
        Connection connection = null;
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            
            // **DÉSACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Si le matricule existe, supprimer TOUTES les données existantes pour ce matricule
            if (matriculeExists) {
                deleteAllDataForMatriculeOptimized(connection, matricule);
            }
            
            // Ordre d'importation respectant les contraintes de clés étrangères
            String[] importOrder = {
                "identite_personnelle",        // Table parent - TOUJOURS EN PREMIER
                "identite_sociale",
                "identite_culturelle",
                "grade_actuel",
                "historique_grades",
                "formation_actuelle",
                "historique_postes",
                "ecole_formation_initiale",
                "ecole_civile",
                "ecole_militaire",
                "operation",
                "decoration",
                "medaille", 
                "punition",
                "langue",
                "infos_specifiques_general",
                "personnel_naviguant",
                "maintenance",
                "specialite",
                "dotation_20_mai",
                "dotation_particuliere_config",
                "dotation_particuliere",
                "parametres_corporels"
            };
            
            // Importer dans l'ordre correct avec insertion forcée
            for (String tableName : importOrder) {
                importTableDataWithInsertEnhanced(connection, tableName, matricule);
            }
            
            // **RÉACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            connection.commit();
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    // **RÉACTIVER LES CONTRAINTES EN CAS D'ERREUR AUSSI**
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    // **S'ASSURER QUE LES CONTRAINTES SONT RÉACTIVÉES**
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Supprime toutes les données existantes pour un matricule donné
     */
    private void deleteAllDataForMatriculeOptimized(Connection connection, String matricule) throws SQLException {
        // D'abord, identifier quelles tables contiennent effectivement des données pour ce matricule
        Map<String, Integer> tablesWithData = findTablesWithDataForMatricule(connection, matricule);
        
        if (tablesWithData.isEmpty()) {
            System.out.println("Aucune donnée trouvée pour le matricule " + matricule);
            return;
        }
        
        System.out.println("Données trouvées pour le matricule " + matricule + " dans " + tablesWithData.size() + " table(s):");
        for (Map.Entry<String, Integer> entry : tablesWithData.entrySet()) {
            System.out.println("  - " + entry.getKey() + " : " + entry.getValue() + " ligne(s)");
        }
        
        // Tables dans l'ordre de suppression (contraintes de clés étrangères)
        String[] deleteOrder = {
            "parametres_corporels", "dotation_particuliere", "dotation_particuliere_config", 
            "dotation_20_mai", "specialite", "maintenance", "personnel_naviguant",
            "infos_specifiques_general", "langue", "punition", "medaille", "decoration",
            "operation", "ecole_militaire", "ecole_civile", "ecole_formation_initiale",
            "historique_postes", "formation_actuelle", "historique_grades", "grade_actuel",
            "identite_culturelle", "identite_sociale", "identite_personnelle"
        };
        
        int totalDeleted = 0;
        
        // Supprimer seulement les tables qui contiennent des données
        for (String tableName : deleteOrder) {
            if (tablesWithData.containsKey(tableName)) {
                String deleteSql = "DELETE FROM " + tableName + " WHERE matricule = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, matricule);
                    int deletedRows = deleteStmt.executeUpdate();
                    System.out.println("✓ Supprimé " + deletedRows + " ligne(s) de la table " + tableName);
                    totalDeleted += deletedRows;
                }
            }
        }
        
        System.out.println("✓ Total supprimé : " + totalDeleted + " ligne(s) pour le matricule " + matricule);
    }

    /**
     * Trouve toutes les tables qui contiennent des données pour un matricule donné
     */
    private Map<String, Integer> findTablesWithDataForMatricule(Connection connection, String matricule) {
        Map<String, Integer> result = new HashMap<>();
        
        // Liste des tables à vérifier
        String[] tablesToCheck = {
            "identite_personnelle", "identite_sociale", "identite_culturelle",
            "grade_actuel", "historique_grades", "formation_actuelle", "historique_postes",
            "ecole_formation_initiale", "ecole_civile", "ecole_militaire",
            "operation", "decoration", "medaille", "punition", "langue",
            "infos_specifiques_general", "personnel_naviguant", "maintenance",
            "specialite", "dotation_20_mai", "dotation_particuliere_config",
            "dotation_particuliere", "parametres_corporels"
        };
        
        for (String tableName : tablesToCheck) {
            try {
                String countSql = "SELECT COUNT(*) FROM " + tableName + " WHERE matricule = ?";
                try (PreparedStatement stmt = connection.prepareStatement(countSql)) {
                    stmt.setString(1, matricule);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            if (count > 0) {
                                result.put(tableName, count);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                // Si la table n'a pas de colonne matricule, on l'ignore silencieusement
                // System.out.println("Table " + tableName + " ignorée : " + e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * Méthode pour afficher un rapport de suppression détaillé
     */
    private void showDeletionReport(String matricule, Map<String, Integer> deletedData) {
        if (deletedData.isEmpty()) {
            System.out.println("Aucune donnée à supprimer pour le matricule " + matricule);
            return;
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Rapport de suppression pour le matricule : ").append(matricule).append("\n\n");
        
        int totalDeleted = 0;
        for (Map.Entry<String, Integer> entry : deletedData.entrySet()) {
            report.append("• ").append(entry.getKey()).append(" : ")
                   .append(entry.getValue()).append(" ligne(s) supprimée(s)\n");
            totalDeleted += entry.getValue();
        }
        
        report.append("\nTotal : ").append(totalDeleted).append(" ligne(s) supprimée(s)");
        
        System.out.println(report.toString());
    }

    /**
     * Importe les données d'une table spécifique avec insertion forcée
     */
    private int importTableDataWithInsertEnhanced(Connection connection, String tableName, String matricule) throws SQLException {
        File file = droppedFiles.get(tableName);
        if (file == null) return 0;
        
        int insertedRows = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) return 0;
            
            String[] headers = headerLine.split(",");
            
            String dataLine;
            while ((dataLine = br.readLine()) != null) {
                if (dataLine.trim().isEmpty()) continue;
                
                String[] values = parseCSVLine(dataLine);
                
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
                sqlBuilder.append(String.join(", ", headers));
                sqlBuilder.append(") VALUES (");
                
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append("?");
                }
                sqlBuilder.append(")");
                
                try (PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString())) {
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        String value = values[i].trim();
                        String columnName = headers[i].trim();
                        
                        if (isDateColumn(columnName) && (value.isEmpty() || value.equalsIgnoreCase("null"))) {
                            stmt.setNull(i + 1, Types.DATE);
                        } else if (value.isEmpty()) {
                            stmt.setNull(i + 1, Types.VARCHAR);
                        } else {
                            stmt.setString(i + 1, value);
                        }
                    }
                    
                    for (int i = values.length; i < headers.length; i++) {
                        stmt.setNull(i + 1, Types.VARCHAR);
                    }
                    
                    stmt.executeUpdate();
                    insertedRows++;
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Erreur lors de la lecture du fichier " + file.getName() + ": " + e.getMessage());
        }
        
        return insertedRows;
    }

    
    /**
     * Extrait le matricule du fichier identite_personnelle.csv
     */
    private String extractMatriculeFromPersonnelFile() {
        File file = droppedFiles.get("identite_personnelle");
        if (file == null) return null;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Lire la première ligne (en-têtes)
            String headerLine = br.readLine();
            if (headerLine == null) return null;
            
            String[] headers = headerLine.split(",");
            int matriculeIndex = -1;
            
            // Trouver l'index de la colonne matricule
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("matricule")) {
                    matriculeIndex = i;
                    break;
                }
            }
            
            if (matriculeIndex == -1) return null;
            
            // Lire la deuxième ligne (données)
            String dataLine = br.readLine();
            if (dataLine == null) return null;
            
            String[] values = dataLine.split(",");
            
            if (matriculeIndex < values.length) {
                return values[matriculeIndex].trim();
            }
            
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Vérifie si un matricule existe déjà dans la base de données
     */
    private boolean checkMatriculeExists(String matricule) {
        Connection connection = null;
        
        try {
            connection = getConnection();
            String sql = "SELECT 1 FROM identite_personnelle WHERE matricule = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, matricule);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    }
    
    /**
     * Valide un fichier CSV par rapport à la structure attendue de la table
     */
    private boolean validateCsvFile(File file, String tableName) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Lire la première ligne (en-têtes)
            String headerLine = br.readLine();
            if (headerLine == null) {
                showErrorAlert("Fichier vide", "Le fichier " + file.getName() + " est vide.");
                return false;
            }
            
            String[] headers = headerLine.split(",");
            List<String> expectedColumns = tableColumns.get(tableName);
            
            // Vérifier que tous les en-têtes attendus sont présents
            boolean hasAllHeaders = true;
            List<String> missingHeaders = new ArrayList<>();
            
            for (String expectedColumn : expectedColumns) {
                boolean found = false;
                for (String header : headers) {
                    if (header.trim().equalsIgnoreCase(expectedColumn)) {
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    hasAllHeaders = false;
                    missingHeaders.add(expectedColumn);
                }
            }
            
            if (!hasAllHeaders) {
                showErrorAlert("Structure invalide", 
                        "Le fichier " + file.getName() + " ne contient pas toutes les colonnes attendues. " +
                        "Colonnes manquantes : " + String.join(", ", missingHeaders));
                return false;
            }
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur de lecture", "Impossible de lire le fichier " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Importe les données dans l'ordre correct des dépendances
     */
    private void importData(String matricule, boolean matriculeExists) throws SQLException {
        Connection connection = null;
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            
            // **DÉSACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Ordre d'importation respectant les contraintes de clés étrangères
            String[] importOrder = {
                "identite_personnelle",        // Table parent - TOUJOURS EN PREMIER
                "identite_sociale",
                "identite_culturelle",
                "grade_actuel",
                "historique_grades",
                "formation_actuelle",
                "historique_postes",
                "ecole_formation_initiale",
                "ecole_civile",
                "ecole_militaire",
                "operation",
                "decoration",
                "medaille", 
                "punition",
                "langue",
                "infos_specifiques_general",
                "personnel_naviguant",
                "maintenance",
                "specialite",
                "dotation_20_mai",
                "dotation_particuliere_config",
                "dotation_particuliere",
                "parametres_corporels"
            };
            
            // Importer dans l'ordre correct
            for (String tableName : importOrder) {
                importTableData(connection, tableName, matricule, matriculeExists);
            }
            
            // **RÉACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            connection.commit();
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    // **RÉACTIVER LES CONTRAINTES EN CAS D'ERREUR AUSSI**
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    // **S'ASSURER QUE LES CONTRAINTES SONT RÉACTIVÉES**
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Importe les données d'une table spécifique
     */
    private void importTableData(Connection connection, String tableName, String matricule, boolean matriculeExists) throws SQLException {
        File file = droppedFiles.get(tableName);
        if (file == null) return;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Lire la première ligne (en-têtes)
            String headerLine = br.readLine();
            if (headerLine == null) return;
            
            String[] headers = headerLine.split(",");
            
            // Si le matricule existe déjà et que la table a une colonne matricule, supprimer les données existantes
            if (matriculeExists && tableHasMatriculeColumn(connection, tableName)) {
                String deleteSql = "DELETE FROM " + tableName + " WHERE matricule = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, matricule);
                    deleteStmt.executeUpdate();
                }
            }
            
            // Lire les lignes de données
            String dataLine;
            while ((dataLine = br.readLine()) != null) {
                // Ignorer les lignes vides
                if (dataLine.trim().isEmpty()) continue;
                
                String[] values = parseCSVLine(dataLine);
                
                // Préparer la requête d'insertion
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("INSERT IGNORE INTO ").append(tableName).append(" (");
                
                // Ajouter les noms de colonnes
                sqlBuilder.append(String.join(", ", headers));
                
                sqlBuilder.append(") VALUES (");
                
                // Ajouter les paramètres
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append("?");
                }
                
                sqlBuilder.append(")");
                
                // Exécuter la requête
                try (PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString())) {
                    // Définir les valeurs des paramètres
                	for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                	    String value = values[i].trim();
                	    String columnName = headers[i].trim();
                	    
                	    // Vérifier si c'est une colonne de date et si la valeur est vide
                	    if (isDateColumn(columnName) && (value.isEmpty() || value.equalsIgnoreCase("null"))) {
                	        stmt.setNull(i + 1, Types.DATE);
                	    } else if (value.isEmpty()) {
                	        stmt.setNull(i + 1, Types.VARCHAR);
                	    } else {
                	        stmt.setString(i + 1, value);
                	    }
                	}
                    
                    // Si certains paramètres n'ont pas été définis (moins de valeurs que d'en-têtes)
                    for (int i = values.length; i < headers.length; i++) {
                        stmt.setNull(i + 1, Types.VARCHAR);
                    }
                    
                    stmt.executeUpdate();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Erreur lors de la lecture du fichier " + file.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si une colonne est de type date
     */
    private boolean isDateColumn(String columnName) {
        String lowerColumnName = columnName.toLowerCase();
        return lowerColumnName.contains("date") || 
               lowerColumnName.equals("cempn_validite") ||
               // Ajoutez d'autres noms de colonnes de date si nécessaire
               lowerColumnName.contains("naissance");
    }
    
    /**
     * Parse une ligne CSV en tenant compte des guillemets
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Si c'est un guillemet échappé ("""), traiter comme partie du champ
                if (i < line.length() - 1 && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // Sauter le prochain guillemet
                } else {
                    // Sinon, basculer l'état "inQuotes"
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Fin d'un champ
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                // Caractère normal, l'ajouter au champ courant
                currentField.append(c);
            }
        }
        
        // Ajouter le dernier champ
        result.add(currentField.toString());
        
        return result.toArray(new String[0]);
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
    
    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue d'information
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Méthodes existantes d'origine du impPersonnelController (gestion des menus, etc.)
    
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
            showErrorAlert("Erreur", "Erreur lors du chargement de la vue d'importation du personnel");
        }
    }
    
    @FXML
    private void handleImportAeronef(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper15.fxml"));
            Parent importArmementView = loader.load();
            borderPane.setCenter(importArmementView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors du chargement de la vue d'importation du personnel");
        }
    }
    
    @FXML
    private void handleImportArmement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper16.fxml"));
            Parent importArmementView = loader.load();
            borderPane.setCenter(importArmementView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors du chargement de la vue d'importation du personnel");
        }
    }
    
    @FXML
    private void handleImportMateriel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper17.fxml"));
            Parent importMaterielView = loader.load();
            borderPane.setCenter(importMaterielView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors du chargement de la vue d'importation du personnel");
        }
    }
    
    @FXML
    private void handleImportVehicule(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper18.fxml"));
            Parent importVehiculeView = loader.load();
            borderPane.setCenter(importVehiculeView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors du chargement de la vue d'importation du personnel");
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
            showErrorAlert("Erreur", "Erreur lors du chargement du tableau de bord");
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
            showErrorAlert("Erreur", "Erreur lors du chargement de l'accueil");
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
            showErrorAlert("Erreur", "Erreur lors du chargement de la page de recherche");
        }
    }
    
    /**
     * Affiche un rapport détaillé de l'importation
     */
    private void showImportReport(String matricule, boolean wasExisting, Map<String, Integer> tableStats) {
        StringBuilder report = new StringBuilder();
        report.append("Rapport d'importation pour le matricule : ").append(matricule).append("\n\n");
        
        if (wasExisting) {
            report.append("✓ Données existantes écrasées avec succès\n\n");
        } else {
            report.append("✓ Nouvelles données importées avec succès\n\n");
        }
        
        report.append("Détails par table :\n");
        for (Map.Entry<String, Integer> entry : tableStats.entrySet()) {
            report.append("• ").append(entry.getKey()).append(" : ")
                   .append(entry.getValue()).append(" ligne(s) importée(s)\n");
        }
        
        Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);
        reportAlert.setTitle("Rapport d'importation");
        reportAlert.setHeaderText("Importation terminée avec succès");
        reportAlert.setContentText(report.toString());
        
        // Rendre la zone de texte plus grande
        reportAlert.getDialogPane().setPrefWidth(500);
        reportAlert.getDialogPane().setPrefHeight(400);
        
        reportAlert.showAndWait();
    }

    /**
     * Vérifie l'intégrité des données après importation
     */
    private boolean verifyDataIntegrity(Connection connection, String matricule) {
        try {
            // Vérifier que l'identité personnelle existe
            String checkSql = "SELECT COUNT(*) FROM identite_personnelle WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                stmt.setString(1, matricule);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("✓ Vérification d'intégrité réussie pour le matricule " + matricule);
                        return true;
                    } else {
                        System.err.println("✗ Échec de la vérification d'intégrité : matricule non trouvé");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la vérification d'intégrité : " + e.getMessage());
            return false;
        }
    }

    /**
     * Version améliorée de la méthode d'importation avec rapport détaillé
     */
    private void importDataWithOverwriteEnhanced(String matricule, boolean matriculeExists) throws SQLException {
        Connection connection = null;
        Map<String, Integer> importStats = new HashMap<>();
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            
            // **DÉSACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Si le matricule existe, supprimer TOUTES les données existantes pour ce matricule
            if (matriculeExists) {
                updateStatus("Suppression des données existantes pour le matricule " + matricule + "...");
                deleteAllDataForMatriculeOptimized(connection, matricule);
            }
            
            // Ordre d'importation respectant les contraintes de clés étrangères
            String[] importOrder = {
                "identite_personnelle", "identite_sociale", "identite_culturelle",
                "grade_actuel", "historique_grades", "formation_actuelle", "historique_postes",
                "ecole_formation_initiale", "ecole_civile", "ecole_militaire",
                "operation", "decoration", "medaille", "punition", "langue",
                "infos_specifiques_general", "personnel_naviguant", "maintenance",
                "specialite", "dotation_20_mai", "dotation_particuliere_config",
                "dotation_particuliere", "parametres_corporels"
            };
            
            // Importer dans l'ordre correct avec suivi des statistiques
            for (int i = 0; i < importOrder.length; i++) {
                String tableName = importOrder[i];
                updateStatus("Importation de la table " + tableName + " (" + (i + 1) + "/" + importOrder.length + ")...");
                
                int rowsImported = importTableDataWithInsertEnhanced(connection, tableName, matricule);
                importStats.put(tableName, rowsImported);
            }
            
            // **RÉACTIVER LES CONTRAINTES DE CLÉS ÉTRANGÈRES**
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            // Vérifier l'intégrité des données
            updateStatus("Vérification de l'intégrité des données...");
            if (!verifyDataIntegrity(connection, matricule)) {
                throw new SQLException("Échec de la vérification d'intégrité des données");
            }
            
            connection.commit();
            
            // Afficher le rapport détaillé
            showImportReport(matricule, matriculeExists, importStats);
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}