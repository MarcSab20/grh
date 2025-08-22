package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import application.AjoutPersonnelController.Decoration;
import application.AjoutPersonnelController.Dotation;
import application.AjoutPersonnelController.DotationParticuliere;
import application.AjoutPersonnelController.EcoleCivile;
import application.AjoutPersonnelController.EcoleMilitaire;
import application.AjoutPersonnelController.Grade;
import application.AjoutPersonnelController.Langue;
import application.AjoutPersonnelController.Maintenance;
import application.AjoutPersonnelController.Medal;
import application.AjoutPersonnelController.Operation;
import application.AjoutPersonnelController.PosteHistorique;
import application.AjoutPersonnelController.Punishment;
import application.ModificationPersonnelController.Personnel;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ModificationDetailsPersonnelController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox mainVBox;
    @FXML private Label personnelInfoLabel;
    @FXML private Button saveAllButton;
    @FXML private Button cancelButton;

    private Personnel personnel;
    private List<String> selectedTables;
    private Connection connection;
    
    // Map pour stocker les données modifiées
    private Map<String, Map<String, Object>> modifiedData = new HashMap<>();
    
    // Map pour stocker les sections modifiables
    private Map<String, VBox> sectionContainers = new HashMap<>();

    public void initData(Personnel personnel, List<String> selectedTables, Connection connection) {
        this.personnel = personnel;
        this.selectedTables = selectedTables;
        this.connection = connection;
        
        personnelInfoLabel.setText("Modification des données de " + personnel.getNom() + " " + personnel.getPrenom() + 
                                   " (Matricule: " + personnel.getMatricule() + ")");
        
        // Initialiser les sections pour chaque table sélectionnée
        initializeSections();
        
        // Ajouter les gestionnaires d'événements pour les boutons
        saveAllButton.setOnAction(e -> handleSaveAll());
        cancelButton.setOnAction(e -> handleCancel());
    }

    private void initializeSections() {
        // Créer une section pour chaque table sélectionnée
        for (String table : selectedTables) {
            TitledPane section = createSection(table);
            mainVBox.getChildren().add(section);
        }
    }

    private TitledPane createSection(String tableName) {
        String friendlyName = getFriendlyTableName(tableName);
        TitledPane titledPane = new TitledPane();
        titledPane.setText(friendlyName);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Stocker la référence du contenu pour un accès ultérieur
        sectionContainers.put(tableName, content);
        
        // Charger les données spécifiques à cette table
        try {
            loadTableData(tableName, content);
        } catch (SQLException e) {
            showError("Erreur de chargement des données", "Impossible de charger les données de " + friendlyName + "\n" + e.getMessage());
            e.printStackTrace();
        }
        
        titledPane.setContent(content);
        return titledPane;
    }

    private void loadTableData(String tableName, VBox container) throws SQLException {
        switch (tableName) {
            case "identite_personnelle":
                loadIdentitePersonnelle(container);
                break;
            case "identite_sociale":
                loadIdentiteSociale(container);
                break;
            case "identite_culturelle":
                loadIdentiteCulturelle(container);
                break;
            case "grade_actuel":
                loadGradeActuel(container);
                break;
            case "historique_grades":
                loadHistoriqueGrades(container);
                break;
            case "formation_actuelle":
                loadFormationActuelle(container);
                break;
            case "historique_postes":
                loadHistoriquePostes(container);
                break;
            case "ecole_formation_initiale":
                loadEcoleFormationInitiale(container);
                break;
            case "ecole_civile":
                loadEcoleCivile(container);
                break;
            case "ecole_militaire":
                loadEcoleMilitaire(container);
                break;
            case "operation":
                loadOperations(container);
                break;
            case "decoration":
                loadDecorations(container);
                break;
            case "medaille":
                loadMedailles(container);
                break;
            case "punition":
                loadPunitions(container);
                break;
            case "langue":
                loadLangues(container);
                break;
            case "infos_specifiques_general":
                loadInfosSpecifiquesGeneral(container);
                break;
            case "personnel_naviguant":
                loadPersonnelNaviguant(container);
                break;
            case "maintenance":
                loadMaintenance(container);
                break;
            case "specialite":
                loadSpecialite(container);
                break;
            case "dotation_20_mai":
                loadDotation20Mai(container);
                break;
            case "dotation_particuliere":
                loadDotationParticuliere(container);
                break;
            case "parametres_corporels":
                loadParametresCorporels(container);
                break;
            default:
                container.getChildren().add(new Label("Données non disponibles pour cette section"));
                break;
        }
    }

    // Méthodes pour charger les données spécifiques à chaque table
    private void loadIdentitePersonnelle(VBox container) throws SQLException {
        String query = "SELECT * FROM identite_personnelle WHERE matricule = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, personnel.getMatricule());
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            // Matricule (non modifiable)
            TextField matriculeField = new TextField(rs.getString("matricule"));
            matriculeField.setEditable(false);
            grid.add(new Label("Matricule:"), 0, 0);
            grid.add(matriculeField, 1, 0);

            // Nom
            TextField nomField = new TextField(rs.getString("nom"));
            grid.add(new Label("Nom:"), 0, 1);
            grid.add(nomField, 1, 1);

            // Prénom
            TextField prenomField = new TextField(rs.getString("prenom"));
            grid.add(new Label("Prénom:"), 0, 2);
            grid.add(prenomField, 1, 2);

            // Lieu de naissance
            TextField lieuNaissanceField = new TextField(rs.getString("lieu_naissance"));
            grid.add(new Label("Lieu de naissance:"), 0, 3);
            grid.add(lieuNaissanceField, 1, 3);

            // Date de naissance
            DatePicker dateNaissanceField = new DatePicker();
            if (rs.getDate("date_naissance") != null) {
                dateNaissanceField.setValue(rs.getDate("date_naissance").toLocalDate());
            }
            grid.add(new Label("Date de naissance:"), 0, 4);
            grid.add(dateNaissanceField, 1, 4);

            // Téléphone
            TextField telephoneField = new TextField(rs.getString("telephone"));
            grid.add(new Label("Téléphone:"), 0, 5);
            grid.add(telephoneField, 1, 5);

            // Sexe
            ComboBox<String> sexeField = new ComboBox<>();
            sexeField.getItems().addAll("Masculin", "Féminin");
            sexeField.setValue(rs.getString("sexe"));
            grid.add(new Label("Sexe:"), 0, 6);
            grid.add(sexeField, 1, 6);

            // Groupe sanguin
            ComboBox<String> groupeSanguinField = new ComboBox<>();
            groupeSanguinField.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
            groupeSanguinField.setValue(rs.getString("groupe_sanguin"));
            grid.add(new Label("Groupe sanguin:"), 0, 7);
            grid.add(groupeSanguinField, 1, 7);

            // Ajouter un bouton pour sauvegarder cette section
            Button saveButton = new Button("Enregistrer cette section");
            saveButton.setOnAction(e -> {
                // Créer la map de données modifiées
                Map<String, Object> data = new HashMap<>();
                data.put("matricule", matriculeField.getText());
                data.put("nom", nomField.getText());
                data.put("prenom", prenomField.getText());
                data.put("lieu_naissance", lieuNaissanceField.getText());
                data.put("date_naissance", dateNaissanceField.getValue());
                data.put("telephone", telephoneField.getText());
                data.put("sexe", sexeField.getValue());
                data.put("groupe_sanguin", groupeSanguinField.getValue());

                // Stocker les données modifiées
                modifiedData.put("identite_personnelle", data);

                showSuccess("Les informations d'identité personnelle ont été enregistrées.");
            });

            container.getChildren().addAll(grid, saveButton);
        }
    }

    private void loadIdentiteSociale(VBox container) throws SQLException {
        String query = "SELECT * FROM identite_sociale WHERE matricule = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, personnel.getMatricule());
        ResultSet rs = pstmt.executeQuery();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Créer les champs avec valeurs par défaut vides
        TextField nomPereField = new TextField();
        TextField nomMereField = new TextField();
        Spinner<Integer> nombreConjointsSpinner = new Spinner<>(0, 10, 0);
        Spinner<Integer> nombreEnfantsSpinner = new Spinner<>(0, 20, 0);

        // Remplir avec les données existantes si disponibles
        if (rs.next()) {
            nomPereField.setText(rs.getString("nom_pere"));
            nomMereField.setText(rs.getString("nom_mere"));
            nombreConjointsSpinner.getValueFactory().setValue(rs.getInt("nombre_conjoints"));
            nombreEnfantsSpinner.getValueFactory().setValue(rs.getInt("nombre_enfants"));
        }

        // Ajouter les champs au grid
        grid.add(new Label("Nom du père:"), 0, 0);
        grid.add(nomPereField, 1, 0);
        grid.add(new Label("Nom de la mère:"), 0, 1);
        grid.add(nomMereField, 1, 1);
        grid.add(new Label("Nombre de conjoints:"), 0, 2);
        grid.add(nombreConjointsSpinner, 1, 2);
        grid.add(new Label("Nombre d'enfants:"), 0, 3);
        grid.add(nombreEnfantsSpinner, 1, 3);

        // Ajouter un bouton pour sauvegarder cette section
        Button saveButton = new Button("Enregistrer cette section");
        saveButton.setOnAction(e -> {
            // Créer la map de données modifiées
            Map<String, Object> data = new HashMap<>();
            data.put("matricule", personnel.getMatricule());
            data.put("nom_pere", nomPereField.getText());
            data.put("nom_mere", nomMereField.getText());
            data.put("nombre_conjoints", nombreConjointsSpinner.getValue());
            data.put("nombre_enfants", nombreEnfantsSpinner.getValue());

            // Stocker les données modifiées
            modifiedData.put("identite_sociale", data);

            showSuccess("Les informations d'identité sociale ont été enregistrées.");
        });

        container.getChildren().addAll(grid, saveButton);
    }

    private void loadIdentiteCulturelle(VBox container) throws SQLException {
        String query = "SELECT * FROM identite_culturelle WHERE matricule = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, personnel.getMatricule());
        ResultSet rs = pstmt.executeQuery();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Créer les champs avec valeurs par défaut vides
        ComboBox<String> regionOrigineField = new ComboBox<>();
        regionOrigineField.getItems().addAll("Région 1", "Région 2", "Région 3");
        ComboBox<String> departementOrigineField = new ComboBox<>();
        departementOrigineField.getItems().addAll("Département 1", "Département 2", "Département 3");
        TextField arrondissementOrigineField = new TextField();
        TextField villageField = new TextField();
        ComboBox<String> ethnieField = new ComboBox<>();
        ethnieField.getItems().addAll("Ethnie 1", "Ethnie 2", "Ethnie 3");
        ComboBox<String> religionField = new ComboBox<>();
        religionField.getItems().addAll("Religion 1", "Religion 2", "Religion 3");

        // Remplir avec les données existantes si disponibles
        if (rs.next()) {
            regionOrigineField.setValue(rs.getString("region_origine"));
            departementOrigineField.setValue(rs.getString("departement_origine"));
            arrondissementOrigineField.setText(rs.getString("arrondissement_origine"));
            villageField.setText(rs.getString("village"));
            ethnieField.setValue(rs.getString("ethnie"));
            religionField.setValue(rs.getString("religion"));
        }

        // Ajouter les champs au grid
        grid.add(new Label("Région d'origine:"), 0, 0);
        grid.add(regionOrigineField, 1, 0);
        grid.add(new Label("Département d'origine:"), 0, 1);
        grid.add(departementOrigineField, 1, 1);
        grid.add(new Label("Arrondissement d'origine:"), 0, 2);
        grid.add(arrondissementOrigineField, 1, 2);
        grid.add(new Label("Village:"), 0, 3);
        grid.add(villageField, 1, 3);
        grid.add(new Label("Ethnie:"), 0, 4);
        grid.add(ethnieField, 1, 4);
        grid.add(new Label("Religion:"), 0, 5);
        grid.add(religionField, 1, 5);

        // Ajouter un bouton pour sauvegarder cette section
        Button saveButton = new Button("Enregistrer cette section");
        saveButton.setOnAction(e -> {
            // Créer la map de données modifiées
            Map<String, Object> data = new HashMap<>();
            data.put("matricule", personnel.getMatricule());
            data.put("region_origine", regionOrigineField.getValue());
            data.put("departement_origine", departementOrigineField.getValue());
            data.put("arrondissement_origine", arrondissementOrigineField.getText());
            data.put("village", villageField.getText());
            data.put("ethnie", ethnieField.getValue());
            data.put("religion", religionField.getValue());

            // Stocker les données modifiées
            modifiedData.put("identite_culturelle", data);

            showSuccess("Les informations d'identité culturelle ont été enregistrées.");
        });

        container.getChildren().addAll(grid, saveButton);
    }

    private void loadGradeActuel(VBox container) throws SQLException {
        String query = "SELECT * FROM grade_actuel WHERE matricule = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, personnel.getMatricule());
        ResultSet rs = pstmt.executeQuery();

        // Créer un tableau pour afficher le grade actuel
        TableView<Grade> gradeTable = new TableView<>();
        
        // Configurer les colonnes
        TableColumn<Grade, String> rangCol = new TableColumn<>("Rang");
        rangCol.setCellValueFactory(new PropertyValueFactory<>("rang"));
        
        TableColumn<Grade, String> echelonCol = new TableColumn<>("Échelon");
        echelonCol.setCellValueFactory(new PropertyValueFactory<>("echelon"));
        
        TableColumn<Grade, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Grade, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<Grade, String> echelonGradeCol = new TableColumn<>("Échelon Grade");
        echelonGradeCol.setCellValueFactory(new PropertyValueFactory<>("echelonGrade"));
        
        TableColumn<Grade, String> referenceEchelonCol = new TableColumn<>("Référence Échelon");
        referenceEchelonCol.setCellValueFactory(new PropertyValueFactory<>("referenceEchelon"));
        
        TableColumn<Grade, LocalDate> dateEchelonCol = new TableColumn<>("Date Échelon");
        dateEchelonCol.setCellValueFactory(new PropertyValueFactory<>("dateEchelon"));
        
        // Ajouter les colonnes au tableau
        gradeTable.getColumns().addAll(rangCol, echelonCol, dateCol, referenceCol, 
                                     echelonGradeCol, referenceEchelonCol, dateEchelonCol);
        
        // Configurer la hauteur du tableau
        gradeTable.setPrefHeight(200);
        
        // Créer la liste des grades
        ObservableList<Grade> grades = FXCollections.observableArrayList();
        
        // Ajouter le grade actuel s'il existe
        if (rs.next()) {
            Grade grade = new Grade(
                rs.getString("rang"),
                rs.getString("echelon"),
                rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null,
                rs.getString("reference"),
                rs.getString("echelon_grade"),
                rs.getString("reference_echelon"),
                rs.getDate("date_echelon") != null ? rs.getDate("date_echelon").toLocalDate() : null
            );
            grades.add(grade);
        }
        
        // Définir les items du tableau
        gradeTable.setItems(grades);
        
        // Ajouter un bouton pour modifier le grade
        Button modifyButton = new Button("Modifier le grade actuel");
        modifyButton.setOnAction(e -> {
            // Récupérer le grade sélectionné
            Grade selectedGrade = gradeTable.getSelectionModel().getSelectedItem();
            if (selectedGrade != null) {
                // Créer une boîte de dialogue pour modifier le grade
                // Implémenter cette méthode pour ouvrir une boîte de dialogue de modification
                showGradeEditDialog(selectedGrade, gradeTable);
            } else {
                showAlert("Aucune sélection", "Veuillez sélectionner un grade à modifier.", AlertType.WARNING);
            }
        });
        
        // Ajouter un bouton pour sauvegarder cette section
        Button saveButton = new Button("Enregistrer cette section");
        saveButton.setOnAction(e -> {
            // Créer la map de données modifiées
            Map<String, Object> data = new HashMap<>();
            data.put("matricule", personnel.getMatricule());
            data.put("grade", gradeTable.getItems().get(0)); // Le seul grade dans le tableau
            
            // Stocker les données modifiées
            modifiedData.put("grade_actuel", data);
            
            showSuccess("Les informations du grade actuel ont été enregistrées.");
        });
        
        container.getChildren().addAll(gradeTable, modifyButton, saveButton);
    }

    // Méthode pour afficher la boîte de dialogue de modification d'un grade
    private void showGradeEditDialog(Grade grade, TableView<Grade> gradeTable) {
        // Cette méthode serait similaire à celle déjà implémentée dans AjoutPersonnelController
        // Elle ouvrirait une boîte de dialogue pour modifier les attributs du grade
        // Pour le moment, nous allons juste simuler une modification
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Modification");
        alert.setHeaderText("Fonction de modification du grade");
        alert.setContentText("Cette fonctionnalité serait implémentée de manière similaire à celle dans AjoutPersonnelController.");
        alert.showAndWait();
    }

    // Implémentez des méthodes similaires pour toutes les autres tables
    // Ces méthodes suivraient le même modèle que celles ci-dessus
    // mais adaptées à la structure spécifique de chaque table

    private void loadHistoriqueGrades(VBox container) {
        // Similaire à loadGradeActuel mais pour l'historique des grades
        Label placeholderLabel = new Label("Historique des grades - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadFormationActuelle(VBox container) {
        // Similaire aux autres mais pour formation actuelle
        Label placeholderLabel = new Label("Formation actuelle - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadHistoriquePostes(VBox container) {
        // Similaire aux autres mais pour l'historique des postes
        Label placeholderLabel = new Label("Historique des postes - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadEcoleFormationInitiale(VBox container) {
        // Similaire aux autres mais pour école de formation initiale
        Label placeholderLabel = new Label("École de formation initiale - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadEcoleCivile(VBox container) {
        // Similaire aux autres mais pour écoles civiles
        Label placeholderLabel = new Label("Écoles civiles - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadEcoleMilitaire(VBox container) {
        // Similaire aux autres mais pour écoles militaires
        Label placeholderLabel = new Label("Écoles militaires - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadOperations(VBox container) {
        // Similaire aux autres mais pour opérations
        Label placeholderLabel = new Label("Opérations - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadDecorations(VBox container) {
        // Similaire aux autres mais pour décorations
        Label placeholderLabel = new Label("Décorations - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadMedailles(VBox container) {
        // Similaire aux autres mais pour médailles
        Label placeholderLabel = new Label("Médailles - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadPunitions(VBox container) {
        // Similaire aux autres mais pour punitions
        Label placeholderLabel = new Label("Punitions - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadLangues(VBox container) {
        // Similaire aux autres mais pour langues
        Label placeholderLabel = new Label("Langues - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadInfosSpecifiquesGeneral(VBox container) {
        // Similaire aux autres mais pour infos spécifiques générales
        Label placeholderLabel = new Label("Infos spécifiques générales - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadPersonnelNaviguant(VBox container) {
        // Similaire aux autres mais pour personnel naviguant
        Label placeholderLabel = new Label("Personnel naviguant - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadMaintenance(VBox container) {
        // Similaire aux autres mais pour maintenance
        Label placeholderLabel = new Label("Maintenance - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadSpecialite(VBox container) {
        // Similaire aux autres mais pour spécialité
        Label placeholderLabel = new Label("Spécialité - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadDotation20Mai(VBox container) {
        // Similaire aux autres mais pour dotation 20 Mai
        Label placeholderLabel = new Label("Dotation 20 Mai - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadDotationParticuliere(VBox container) {
        // Similaire aux autres mais pour dotation particulière
        Label placeholderLabel = new Label("Dotation particulière - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    private void loadParametresCorporels(VBox container) {
        // Similaire aux autres mais pour paramètres corporels
        Label placeholderLabel = new Label("Paramètres corporels - Cette section serait implémentée comme les autres");
        container.getChildren().add(placeholderLabel);
    }

    // Méthode pour obtenir le nom convivial d'une table
    private String getFriendlyTableName(String tableName) {
        switch (tableName) {
            case "identite_personnelle": return "Identité Personnelle";
            case "identite_sociale": return "Identité Sociale";
            case "identite_culturelle": return "Identité Culturelle";
            case "grade_actuel": return "Grade Actuel";
            case "historique_grades": return "Historique des Grades";
            case "formation_actuelle": return "Formation Actuelle";
            case "historique_postes": return "Historique des Postes";
            case "ecole_formation_initiale": return "École de Formation Initiale";
            case "ecole_civile": return "École Civile";
            case "ecole_militaire": return "École Militaire";
            case "operation": return "Opérations";
            case "decoration": return "Décorations";
            case "medaille": return "Médailles";
            case "punition": return "Punitions";
            case "langue": return "Langues";
            case "infos_specifiques_general": return "Informations Spécifiques Générales";
            case "personnel_naviguant": return "Personnel Naviguant";
            case "maintenance": return "Maintenance";
            case "specialite": return "Spécialité";
            case "dotation_20_mai": return "Dotation 20 Mai";
            case "dotation_particuliere": return "Dotation Particulière";
            case "parametres_corporels": return "Paramètres Corporels";
            default: return tableName;
        }
    }

    @FXML
    private void handleSaveAll() {
        try {
            connection.setAutoCommit(false);
            
            boolean success = true;
            
            for (Map.Entry<String, Map<String, Object>> entry : modifiedData.entrySet()) {
                String tableName = entry.getKey();
                Map<String, Object> data = entry.getValue();
                
                try {
                    saveTableData(tableName, data);
                } catch (SQLException e) {
                    success = false;
                    showError("Erreur d'enregistrement", "Erreur lors de l'enregistrement de " + getFriendlyTableName(tableName) + ": " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
            
            if (success) {
                connection.commit();
                showSuccess("Toutes les modifications ont été enregistrées avec succès.");
                
                // Fermer la fenêtre
                Stage stage = (Stage) saveAllButton.getScene().getWindow();
                stage.close();
            } else {
                connection.rollback();
                showError("Transaction annulée", "Les modifications n'ont pas été enregistrées en raison d'erreurs.");
            }
            
        } catch (SQLException e) {
            showError("Erreur de transaction", e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveTableData(String tableName, Map<String, Object> data) throws SQLException {
        switch (tableName) {
            case "identite_personnelle":
                saveIdentitePersonnelle(data);
                break;
            case "identite_sociale":
                saveIdentiteSociale(data);
                break;
            case "identite_culturelle":
                saveIdentiteCulturelle(data);
                break;
            case "grade_actuel":
                saveGradeActuel(data);
                break;
            // Implémentez des méthodes similaires pour toutes les autres tables
            default:
                throw new SQLException("Table non prise en charge: " + tableName);
        }
    }

    private void saveIdentitePersonnelle(Map<String, Object> data) throws SQLException {
        String sql = "UPDATE identite_personnelle SET " +
                     "nom = ?, prenom = ?, lieu_naissance = ?, date_naissance = ?, " +
                     "telephone = ?, sexe = ?, groupe_sanguin = ? " +
                     "WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, (String) data.get("nom"));
            stmt.setString(2, (String) data.get("prenom"));
            stmt.setString(3, (String) data.get("lieu_naissance"));
            stmt.setObject(4, data.get("date_naissance"));
            stmt.setString(5, (String) data.get("telephone"));
            stmt.setString(6, (String) data.get("sexe"));
            stmt.setString(7, (String) data.get("groupe_sanguin"));
            stmt.setString(8, (String) data.get("matricule"));
            
            stmt.executeUpdate();
        }
    }

    private void saveIdentiteSociale(Map<String, Object> data) throws SQLException {
        // Vérifier si une entrée existe déjà
        String checkSql = "SELECT COUNT(*) FROM identite_sociale WHERE matricule = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, (String) data.get("matricule"));
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            if (count > 0) {
                // Mise à jour
                String sql = "UPDATE identite_sociale SET " +
                             "nom_pere = ?, nom_mere = ?, nombre_conjoints = ?, nombre_enfants = ? " +
                             "WHERE matricule = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, (String) data.get("nom_pere"));
                    stmt.setString(2, (String) data.get("nom_mere"));
                    stmt.setInt(3, (Integer) data.get("nombre_conjoints"));
                    stmt.setInt(4, (Integer) data.get("nombre_enfants"));
                    stmt.setString(5, (String) data.get("matricule"));
                    
                    stmt.executeUpdate();
                }
            } else {
                // Insertion
                String sql = "INSERT INTO identite_sociale " +
                             "(matricule, nom_pere, nom_mere, nombre_conjoints, nombre_enfants) " +
                             "VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, (String) data.get("matricule"));
                    stmt.setString(2, (String) data.get("nom_pere"));
                    stmt.setString(3, (String) data.get("nom_mere"));
                    stmt.setInt(4, (Integer) data.get("nombre_conjoints"));
                    stmt.setInt(5, (Integer) data.get("nombre_enfants"));
                    
                    stmt.executeUpdate();
                }
            }
        }
    }

    private void saveIdentiteCulturelle(Map<String, Object> data) throws SQLException {
        // Similaire à saveIdentiteSociale
        // Vérifier si une entrée existe déjà
        String checkSql = "SELECT COUNT(*) FROM identite_culturelle WHERE matricule = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, (String) data.get("matricule"));
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            if (count > 0) {
                // Mise à jour
                String sql = "UPDATE identite_culturelle SET " +
                             "region_origine = ?, departement_origine = ?, arrondissement_origine = ?, " +
                             "village = ?, ethnie = ?, religion = ? " +
                             "WHERE matricule = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, (String) data.get("region_origine"));
                    stmt.setString(2, (String) data.get("departement_origine"));
                    stmt.setString(3, (String) data.get("arrondissement_origine"));
                    stmt.setString(4, (String) data.get("village"));
                    stmt.setString(5, (String) data.get("ethnie"));
                    stmt.setString(6, (String) data.get("religion"));
                    stmt.setString(7, (String) data.get("matricule"));
                    
                    stmt.executeUpdate();
                }
            } else {
                // Insertion
                String sql = "INSERT INTO identite_culturelle " +
                             "(matricule, region_origine, departement_origine, arrondissement_origine, village, ethnie, religion) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, (String) data.get("matricule"));
                    stmt.setString(2, (String) data.get("region_origine"));
                    stmt.setString(3, (String) data.get("departement_origine"));
                    stmt.setString(4, (String) data.get("arrondissement_origine"));
                    stmt.setString(5, (String) data.get("village"));
                    stmt.setString(6, (String) data.get("ethnie"));
                    stmt.setString(7, (String) data.get("religion"));
                    
                    stmt.executeUpdate();
                }
            }
        }
    }

    private void saveGradeActuel(Map<String, Object> data) throws SQLException {
        // Similaire aux autres, mais pour le grade actuel
        // D'abord, supprimer l'ancien grade
        String deleteSql = "DELETE FROM grade_actuel WHERE matricule = ?";
        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, (String) data.get("matricule"));
            deleteStmt.executeUpdate();
        }
        
        // Ensuite, insérer le nouveau grade
        Grade grade = (Grade) data.get("grade");
        String insertSql = "INSERT INTO grade_actuel " +
                         "(matricule, rang, echelon, date, reference, echelon_grade, reference_echelon, date_echelon) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, (String) data.get("matricule"));
            insertStmt.setString(2, grade.getRang());
            insertStmt.setString(3, grade.getEchelon());
            insertStmt.setObject(4, grade.getDate());
            insertStmt.setString(5, grade.getReference());
            insertStmt.setString(6, grade.getEchelonGrade());
            insertStmt.setString(7, grade.getReferenceEchelon());
            insertStmt.setObject(8, grade.getDateEchelon());
            
            insertStmt.executeUpdate();
        }
    }

    // Implémentez des méthodes similaires pour toutes les autres tables
    // Ces méthodes suivraient le même modèle que celles ci-dessus
    // mais adaptées à la structure spécifique de chaque table

    @FXML
    private void handleCancel() {
        // Demander confirmation
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation d'annulation");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir annuler les modifications ?");
        confirmDialog.setContentText("Toutes les modifications non enregistrées seront perdues.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Fermer la fenêtre
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    private void showSuccess(String message) {
        showAlert("Succès", message, AlertType.INFORMATION);
    }

    private void showError(String title, String message) {
        showAlert(title, message, AlertType.ERROR);
    }

    private void showAlert(String title, String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Classe définissant un grade (copiée de AjoutPersonnelController pour référence)
    public static class Grade {
        private final SimpleStringProperty rang;
        private final SimpleStringProperty echelon;
        private final SimpleStringProperty reference;
        private final SimpleStringProperty echelonGrade;
        private final SimpleStringProperty referenceEchelon;
        private LocalDate date;
        private LocalDate dateEchelon;
        
        public Grade(String rang, String echelon, LocalDate date, String reference) {
            this.rang = new SimpleStringProperty(rang);
            this.echelon = new SimpleStringProperty(echelon);
            this.date = date;
            this.reference = new SimpleStringProperty(reference);
            this.echelonGrade = new SimpleStringProperty("1");
            this.referenceEchelon = new SimpleStringProperty("");
            this.dateEchelon = LocalDate.now();
        }
        
        public Grade(String rang, String echelon, LocalDate date, String reference, 
                    String echelonGrade, String referenceEchelon, LocalDate dateEchelon) {
            this.rang = new SimpleStringProperty(rang);
            this.echelon = new SimpleStringProperty(echelon);
            this.date = date;
            this.reference = new SimpleStringProperty(reference);
            this.echelonGrade = new SimpleStringProperty(echelonGrade);
            this.referenceEchelon = new SimpleStringProperty(referenceEchelon);
            this.dateEchelon = dateEchelon;
        }
        
        // Getters
        public String getRang() { return rang.get(); }
        public String getEchelon() { return echelon.get(); }
        public LocalDate getDate() { return date; }
        public String getReference() { return reference.get(); }
        public String getEchelonGrade() { return echelonGrade.get(); }
        public String getReferenceEchelon() { return referenceEchelon.get(); }
        public LocalDate getDateEchelon() { return dateEchelon; }
        
        // Setters
        public void setEchelon(String echelon) { this.echelon.set(echelon); }
        public void setDate(LocalDate date) { this.date = date; }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setEchelonGrade(String echelonGrade) { this.echelonGrade.set(echelonGrade); }
        public void setReferenceEchelon(String referenceEchelon) { this.referenceEchelon.set(referenceEchelon); }
        public void setDateEchelon(LocalDate dateEchelon) { this.dateEchelon = dateEchelon; }
    }
}