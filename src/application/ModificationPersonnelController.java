package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

// Imports pour la base de données
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Imports pour le PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ModificationPersonnelController {
    
    @FXML private Button accueil;
    @FXML private Button addAirplane_btn;
    @FXML private Button addPersonnel_btn;
    @FXML private Button addVehicule_btn;
    @FXML private Button dashbord;
    @FXML private Button fouiller;
    @FXML private VBox mainContent;
    @FXML private Label politique_conf;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> formationFilter;
    @FXML private ComboBox<String> gradeFilter;
    @FXML private TableView<Personnel> personnelTable;
    @FXML private TableColumn<Personnel, String> matriculeColumn;
    @FXML private TableColumn<Personnel, String> nomColumn;
    @FXML private TableColumn<Personnel, String> prenomColumn;
    @FXML private TableColumn<Personnel, String> formationColumn;
    @FXML private TableColumn<Personnel, String> gradeColumn;

    // Nouveaux filtres (créés dynamiquement)
    private ComboBox<String> positionAdministrativeFilter;
    private ComboBox<String> statutFilter;
    private ComboBox<String> specialiteFilter;
    private ComboBox<String> regimeMatrimonialFilter;
    
    // Boutons pour les filtres
    private Button clearFiltersButton;
    private Button downloadPdfButton;
    
    private ObservableList<Personnel> personnelList = FXCollections.observableArrayList();
    private ObservableList<Personnel> filteredPersonnelList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            // Configurer les colonnes de la table
            matriculeColumn.setCellValueFactory(new PropertyValueFactory<>("matricule"));
            nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
            prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
            formationColumn.setCellValueFactory(new PropertyValueFactory<>("formation"));
            gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

            // IMPORTANT: Configurer la politique de redimensionnement ici
            personnelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            // Configurer les largeurs proportionnelles des colonnes
            setupColumnWidths();
            
            // Configurer la colonne d'action
            configureActionColumn();
            
            // Initialiser les filtres
            initializeFilters();
            
            // Charger les données depuis la base de données
            loadPersonnelFromDatabase();
            
            // Initialiser la liste filtrée
            filteredPersonnelList.addAll(personnelList);
            personnelTable.setItems(filteredPersonnelList);
            
            // Créer l'interface des filtres
            createFilterInterface();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur d'initialisation", "Erreur lors de l'initialisation: " + e.getMessage());
        }
    }
    
    /**
     * Configure les largeurs des colonnes pour qu'elles s'adaptent à la largeur du tableau
     */
    private void setupColumnWidths() {
        // Utiliser la politique de redimensionnement contrainte
        personnelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Définir les largeurs proportionnelles (total = 100%)
        matriculeColumn.prefWidthProperty().bind(personnelTable.widthProperty().multiply(0.15)); // 15%
        nomColumn.prefWidthProperty().bind(personnelTable.widthProperty().multiply(0.20));       // 20%
        prenomColumn.prefWidthProperty().bind(personnelTable.widthProperty().multiply(0.20));    // 20%
        formationColumn.prefWidthProperty().bind(personnelTable.widthProperty().multiply(0.20)); // 20%
        gradeColumn.prefWidthProperty().bind(personnelTable.widthProperty().multiply(0.25));     // 25%
        
        // Les largeurs minimales sont déjà définies dans le FXML
    }

    
    /**
     * Configure la colonne d'action avec les boutons Modifier et Télécharger.
     */
    private void configureActionColumn() {
        // Créer une nouvelle colonne d'action puisqu'elle n'est pas dans le FXML
        TableColumn<Personnel, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button modifyButton = new Button("Modifier");
            private final Button downloadButton = new Button("Télécharger");

            {
                modifyButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                downloadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px;");

                modifyButton.setOnAction(event -> {
                    Personnel personnel = getTableView().getItems().get(getIndex());
                    handleModify(personnel);
                });

                downloadButton.setOnAction(event -> {
                    Personnel personnel = getTableView().getItems().get(getIndex());
                    handleDownload(personnel);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, modifyButton, downloadButton);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
        
        // Ajouter la colonne d'action au tableau
        personnelTable.getColumns().add(actionCol);
    }
    
    /**
     * Initialise les ComboBox de filtres avec les options disponibles.
     */
    private void initializeFilters() {
        // Filtres existants du FXML
        if (formationFilter != null) {
            formationFilter.getItems().addAll("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        }
        
        if (gradeFilter != null) {
            gradeFilter.getItems().addAll("Général d'Armée", "Général de Corps d'Armée", "Général de Division Aérienne",
                "Général de Brigade Aérienne", "Colonel", "Lieutenant-colonel", 
                "Commandant", "Capitaine", "Lieutenant", "Sous-lieutenant", 
                "Adjudant-chef-major", "Adjudant-chef", "Adjudant", 
                "Sergent-chef", "Sergent", "Caporal-chef", 
                "Caporal", "Soldat de 1ère classe", "Soldat de 2e classe");
        }
        
        // Nouveaux filtres (créés dynamiquement)
        positionAdministrativeFilter = new ComboBox<>();
        positionAdministrativeFilter.getItems().addAll("Absent", "ASM", "Absence irrégulière", "Présent", "Stage", "Mission", "Subsistant", "Désertion", "Détention", "Poursuite Judiciaire", "Permission", "Hors cadre", "Détaché", "Retraite", "Décédé", "Evasan");
        positionAdministrativeFilter.setPromptText("Position Administrative");
        positionAdministrativeFilter.setPrefWidth(180);
        
        statutFilter = new ComboBox<>();
        statutFilter.getItems().addAll("Officier Active", "Sous-Officier de Carrière", "Rengagé", "1ère période");
        statutFilter.setPromptText("Statut");
        statutFilter.setPrefWidth(180);
        
        specialiteFilter = new ComboBox<>();
        specialiteFilter.getItems().addAll("Pilote/ Méca Nav", "Ingénieur sol", "Autre spécialité", "PNNSG");
        specialiteFilter.setPromptText("Spécialité");
        specialiteFilter.setPrefWidth(180);
        
        regimeMatrimonialFilter = new ComboBox<>();
        regimeMatrimonialFilter.getItems().addAll("Célibataire", "Marié(e)", "Divorcé(e)", "Veuf/Veuve", "Union libre", "Séparé(e)");
        regimeMatrimonialFilter.setPromptText("Régime Matrimonial");
        regimeMatrimonialFilter.setPrefWidth(180);
    }
    
    /**
     * Crée l'interface des filtres et l'ajoute à la scene.
     */
    private void createFilterInterface() {
        try {
            // Obtenir le parent principal depuis le TableView
            VBox parentContainer = findParentVBox();
            
            if (parentContainer != null) {
                // Créer la section des filtres améliorée
                VBox filterSection = createEnhancedFilterSection();
                
                // Insérer la section des filtres après la zone de recherche existante
                int searchBoxIndex = findSearchBoxIndex(parentContainer);
                if (searchBoxIndex >= 0) {
                    parentContainer.getChildren().add(searchBoxIndex + 1, filterSection);
                } else {
                    parentContainer.getChildren().add(0, filterSection);
                }
            } else {
                System.err.println("Impossible de trouver le conteneur parent pour ajouter les filtres");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la création de l'interface des filtres: " + e.getMessage());
        }
    }
    
    /**
     * Trouve l'index de la zone de recherche dans le conteneur parent
     */
    private int findSearchBoxIndex(VBox parentContainer) {
        for (int i = 0; i < parentContainer.getChildren().size(); i++) {
            Node child = parentContainer.getChildren().get(i);
            if (child.getStyleClass().contains("search-box")) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Crée une section de filtres améliorée avec un meilleur espacement
     */
    private VBox createEnhancedFilterSection() {
        VBox filterSection = new VBox(10);
        filterSection.setPadding(new Insets(10, 0, 10, 0)); // Padding vertical seulement
        filterSection.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5;");
        
        Label filterTitle = new Label("Filtres Avancés");
        filterTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");
        
        // Ligne de filtres supplémentaires avec espacement optimisé
        HBox filterRow = new HBox(15);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(5, 15, 5, 15));
        
        // Créer les filtres avec des largeurs appropriées
        Label positionLabel = createFilterLabel("Position Admin.:");
        positionAdministrativeFilter.setPrefWidth(180);
        positionAdministrativeFilter.setMaxWidth(180);
        
        Label statutLabel = createFilterLabel("Statut:");
        statutFilter.setPrefWidth(180);
        statutFilter.setMaxWidth(180);
        
        Label specialiteLabel = createFilterLabel("Spécialité:");
        specialiteFilter.setPrefWidth(150);
        specialiteFilter.setMaxWidth(150);
        
        filterRow.getChildren().addAll(
            positionLabel, positionAdministrativeFilter,
            statutLabel, statutFilter,
            specialiteLabel, specialiteFilter
        );
        
        // Ligne pour le régime matrimonial
        HBox matrimonialRow = new HBox(15);
        matrimonialRow.setAlignment(Pos.CENTER_LEFT);
        matrimonialRow.setPadding(new Insets(5, 15, 5, 15));
        
        Label matrimonialLabel = createFilterLabel("Régime Matrimonial:");
        regimeMatrimonialFilter.setPrefWidth(180);
        regimeMatrimonialFilter.setMaxWidth(180);
        
        matrimonialRow.getChildren().addAll(matrimonialLabel, regimeMatrimonialFilter);
        
        // Boutons d'action avec espacement
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(10, 0, 5, 0));
        
        Button searchButton = new Button("Appliquer");
        searchButton.setOnAction(e -> handleSearch());
        searchButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        searchButton.setPrefWidth(150);
        
        clearFiltersButton = new Button("Effacer");
        clearFiltersButton.setOnAction(e -> clearAllFilters());
        clearFiltersButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        clearFiltersButton.setPrefWidth(150);
        
        downloadPdfButton = new Button("Télécharger");
        downloadPdfButton.setOnAction(e -> downloadFilteredListAsPdf());
        downloadPdfButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        downloadPdfButton.setPrefWidth(150);
        
        actionButtons.getChildren().addAll(searchButton, clearFiltersButton, downloadPdfButton);
        
        filterSection.getChildren().addAll(filterTitle, filterRow, matrimonialRow, actionButtons);
        
        return filterSection;
    }

    
    
    /**
     * Trouve le VBox parent contenant le TableView.
     */
    private VBox findParentVBox() {
        if (personnelTable == null) return null;
        
        // Remonter dans la hiérarchie pour trouver un VBox parent
        javafx.scene.Node current = personnelTable.getParent();
        while (current != null) {
            if (current instanceof VBox) {
                return (VBox) current;
            }
            current = current.getParent();
        }
        return null;
    }
    
    /**
     * Crée la section des filtres.
     */
    private VBox createFilterSection() {
        VBox filterSection = new VBox(15);
        filterSection.setPadding(new Insets(15));
        filterSection.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5;");
        
        Label filterTitle = new Label("Filtres de Recherche");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // Première ligne de filtres
        HBox filterRow1 = new HBox(10);
        filterRow1.setAlignment(Pos.CENTER_LEFT);
        
        // Vérifier si les éléments FXML existent avant de les utiliser
        if (searchField != null) {
            searchField.setPromptText("Rechercher par nom, prénom ou matricule");
            searchField.setPrefWidth(200);
        }
        if (formationFilter != null) {
            formationFilter.setPrefWidth(150);
        }
        if (gradeFilter != null) {
            gradeFilter.setPrefWidth(150);
        }
        
        filterRow1.getChildren().addAll(
            createFilterLabel("Recherche:"), searchField != null ? searchField : new TextField(),
            createFilterLabel("Formation:"), formationFilter != null ? formationFilter : new ComboBox<>(),
            createFilterLabel("Grade:"), gradeFilter != null ? gradeFilter : new ComboBox<>()
        );
        
        // Deuxième ligne de filtres
        HBox filterRow2 = new HBox(10);
        filterRow2.setAlignment(Pos.CENTER_LEFT);
        filterRow2.getChildren().addAll(
            createFilterLabel("Position Admin.:"), positionAdministrativeFilter,
            createFilterLabel("Statut:"), statutFilter,
            createFilterLabel("Spécialité:"), specialiteFilter
        );
        
        // Troisième ligne de filtres
        HBox filterRow3 = new HBox(10);
        filterRow3.setAlignment(Pos.CENTER_LEFT);
        filterRow3.getChildren().addAll(
            createFilterLabel("Régime Matrimonial:"), regimeMatrimonialFilter
        );
        
        // Boutons d'action
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button searchButton = new Button("Appliquer les Filtres");
        searchButton.setOnAction(e -> handleSearch());
        searchButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        
        clearFiltersButton = new Button("Effacer les Filtres");
        clearFiltersButton.setOnAction(e -> clearAllFilters());
        clearFiltersButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        
        downloadPdfButton = new Button("Télécharger PDF");
        downloadPdfButton.setOnAction(e -> downloadFilteredListAsPdf());
        downloadPdfButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        
        actionButtons.getChildren().addAll(searchButton, clearFiltersButton, downloadPdfButton);
        
        filterSection.getChildren().addAll(filterTitle, filterRow1, filterRow2, filterRow3, actionButtons);
        
        return filterSection;
    }
    
    /**
     * Affiche une boîte de dialogue d'authentification avant la modification.
     */
    private void showAuthenticationDialog(Personnel personnel) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Authentification requise");
        dialog.setHeaderText("Veuillez vous authentifier pour modifier les données du personnel");
        
        // Définir la taille de la fenêtre
        dialog.getDialogPane().setPrefSize(400, 250);
        
        // Ajouter les boutons
        ButtonType loginButtonType = new ButtonType("Se connecter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // Créer les champs de saisie
        TextField identifiantField = new TextField();
        identifiantField.setPromptText("Identifiant");
        identifiantField.setPrefWidth(250);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setPrefWidth(250);
        
        // Organiser les éléments
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        
        Label instructionLabel = new Label("Saisissez vos identifiants pour continuer :");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057;");
        
        HBox identifiantBox = new HBox(10);
        identifiantBox.setAlignment(Pos.CENTER_LEFT);
        Label identifiantLabel = new Label("Identifiant :");
        identifiantLabel.setMinWidth(80);
        identifiantBox.getChildren().addAll(identifiantLabel, identifiantField);
        
        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Mot de passe :");
        passwordLabel.setMinWidth(80);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        vbox.getChildren().addAll(instructionLabel, identifiantBox, passwordBox);
        dialog.getDialogPane().setContent(vbox);
        
        // Activer/désactiver le bouton de connexion
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        
        // Listener pour activer le bouton quand les champs sont remplis
        identifiantField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || passwordField.getText().trim().isEmpty());
        });
        
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || identifiantField.getText().trim().isEmpty());
        });
        
        // Définir le focus initial
        Platform.runLater(() -> identifiantField.requestFocus());
        
        // Convertir le résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new String[]{identifiantField.getText(), passwordField.getText()};
            }
            return null;
        });
        
        // Afficher la boîte de dialogue et traiter le résultat
        Optional<String[]> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            String[] credentials = result.get();
            if (authenticateUser(credentials[0], credentials[1])) {
                // Authentification réussie, procéder à la modification
                proceedWithModification(personnel);
            } else {
                // Authentification échouée
                showErrorAlert("Authentification échouée", 
                             "Identifiants incorrects. Veuillez vérifier vos informations et réessayer.");
            }
        }
    }
    
    /**
     * Procède à la modification après authentification réussie.
     */
    private void proceedWithModification(Personnel personnel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ajoutPersonnel.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur et charger les données du personnel
            AjoutPersonnelController controller = loader.getController();
            controller.loadPersonnelForModification(personnel.getMatricule());
            
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("personnel.css").toExternalForm());
            
            Stage stage = (Stage) personnelTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Modification du personnel - " + personnel.getNom() + " " + personnel.getPrenom());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Impossible d'ouvrir l'interface de modification: " + e.getMessage());
        }
    }
    
    /**
     * Authentifie l'utilisateur avec les identifiants fournis.
     */
    private boolean authenticateUser(String identifiant, String motDePasse) {
        try (Connection connection = getConnection()) {
            String sql = "SELECT password FROM users WHERE identifiant = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, identifiant);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        // Ici vous pouvez ajouter un hashage du mot de passe si nécessaire
                        return motDePasse.equals(storedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // En cas d'erreur de base de données, utiliser une authentification par défaut
            return "admin".equals(identifiant) && "admin123".equals(motDePasse);
        }
        return false;
    }

    
    /**
     * Crée un label pour les filtres.
     */
    private Label createFilterLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(120);
        label.setPrefWidth(120);
        label.setMaxWidth(120);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057; -fx-font-size: 12px;");
        return label;
    }
    /**
     * Charge les données du personnel depuis la base de données.
     */
    private void loadPersonnelFromDatabase() {
        personnelList.clear();
        
        String sql = """
            SELECT DISTINCT 
                ip.matricule,
                ip.nom,
                ip.prenom,
                fa.formation,
                ga.rang as grade,
                COALESCE(ga.statut, '') as statut,
                COALESCE(isg.position_administrative, '') as position_administrative,
                COALESCE(s.type_specialite, '') as type_specialite,
                COALESCE(iss.regime_matrimonial, '') as regime_matrimonial
            FROM identite_personnelle ip
            LEFT JOIN formation_actuelle fa ON ip.matricule = fa.matricule
            LEFT JOIN grade_actuel ga ON ip.matricule = ga.matricule
            LEFT JOIN infos_specifiques_general isg ON ip.matricule = isg.matricule
            LEFT JOIN specialite s ON ip.matricule = s.matricule
            LEFT JOIN identite_sociale iss ON ip.matricule = iss.matricule
            ORDER BY ip.nom, ip.prenom
            """;
        
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Personnel personnel = new Personnel(
                    rs.getString("matricule") != null ? rs.getString("matricule") : "",
                    rs.getString("nom") != null ? rs.getString("nom") : "",
                    rs.getString("prenom") != null ? rs.getString("prenom") : "",
                    rs.getString("formation") != null ? rs.getString("formation") : "",
                    rs.getString("grade") != null ? rs.getString("grade") : "",
                    rs.getString("statut") != null ? rs.getString("statut") : "",
                    rs.getString("position_administrative") != null ? rs.getString("position_administrative") : "",
                    rs.getString("type_specialite") != null ? rs.getString("type_specialite") : "",
                    rs.getString("regime_matrimonial") != null ? rs.getString("regime_matrimonial") : ""
                );
                personnelList.add(personnel);
            }
            
            System.out.println("Chargé " + personnelList.size() + " personnes depuis la base de données.");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Erreur de base de données", "Impossible de charger les données du personnel: " + e.getMessage());
        }
    }

    
    /**
     * Obtient une connexion à la base de données.
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/exploit";
            String username = "marco";
            String password = "29Papa278.";
            return DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC non trouvé", e);
        }
    }
    
    /**
     * Gère la recherche avec filtres multiples.
     */
    @FXML
    private void handleSearch() {
        try {
            String searchTerm = "";
            if (searchField != null && searchField.getText() != null) {
                searchTerm = searchField.getText().toLowerCase().trim();
            }
            
            String formation = formationFilter != null ? formationFilter.getValue() : null;
            String grade = gradeFilter != null ? gradeFilter.getValue() : null;
            String positionAdmin = positionAdministrativeFilter != null ? positionAdministrativeFilter.getValue() : null;
            String statut = statutFilter != null ? statutFilter.getValue() : null;
            String specialite = specialiteFilter != null ? specialiteFilter.getValue() : null;
            String regimeMatrimonial = regimeMatrimonialFilter != null ? regimeMatrimonialFilter.getValue() : null;
            
            filteredPersonnelList.clear();
            
            for (Personnel personnel : personnelList) {
                boolean matches = true;
                
                // Filtre par nom/matricule/prénom
                if (!searchTerm.isEmpty()) {
                    boolean nameMatches = personnel.getNom().toLowerCase().contains(searchTerm) ||
                                        personnel.getPrenom().toLowerCase().contains(searchTerm) ||
                                        personnel.getMatricule().toLowerCase().contains(searchTerm);
                    if (!nameMatches) matches = false;
                }
                
                // Filtre par formation
                if (formation != null && !formation.isEmpty()) {
                    if (!formation.equals(personnel.getFormation())) matches = false;
                }
                
                // Filtre par grade
                if (grade != null && !grade.isEmpty()) {
                    if (!grade.equals(personnel.getGrade())) matches = false;
                }
                
                // Filtre par position administrative
                if (positionAdmin != null && !positionAdmin.isEmpty()) {
                    if (!positionAdmin.equals(personnel.getPositionAdministrative())) matches = false;
                }
                
                // Filtre par statut
                if (statut != null && !statut.isEmpty()) {
                    if (!statut.equals(personnel.getStatut())) matches = false;
                }
                
                // Filtre par spécialité
                if (specialite != null && !specialite.isEmpty()) {
                    if (!specialite.equals(personnel.getSpecialite())) matches = false;
                }
                
                // Filtre par régime matrimonial
                if (regimeMatrimonial != null && !regimeMatrimonial.isEmpty()) {
                    if (!regimeMatrimonial.equals(personnel.getRegimeMatrimonial())) matches = false;
                }
                
                if (matches) {
                    filteredPersonnelList.add(personnel);
                }
            }
            
            personnelTable.setItems(filteredPersonnelList);
            
            // Afficher le nombre de résultats
            showInfoAlert("Résultats de recherche", "Trouvé " + filteredPersonnelList.size() + " personne(s) correspondant aux critères.");
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur de recherche", "Erreur lors de la recherche: " + e.getMessage());
        }
    }
    
    /**
     * Efface tous les filtres.
     */
    private void clearAllFilters() {
        try {
            if (searchField != null) searchField.clear();
            if (formationFilter != null) formationFilter.getSelectionModel().clearSelection();
            if (gradeFilter != null) gradeFilter.getSelectionModel().clearSelection();
            if (positionAdministrativeFilter != null) positionAdministrativeFilter.getSelectionModel().clearSelection();
            if (statutFilter != null) statutFilter.getSelectionModel().clearSelection();
            if (specialiteFilter != null) specialiteFilter.getSelectionModel().clearSelection();
            if (regimeMatrimonialFilter != null) regimeMatrimonialFilter.getSelectionModel().clearSelection();
            
            // Réinitialiser la liste
            filteredPersonnelList.clear();
            filteredPersonnelList.addAll(personnelList);
            personnelTable.setItems(filteredPersonnelList);
            
            showInfoAlert("Filtres effacés", "Tous les filtres ont été effacés. Affichage de " + personnelList.size() + " personne(s).");
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Erreur lors de l'effacement des filtres: " + e.getMessage());
        }
    }
    
    /**
     * Télécharge la liste filtrée en PDF.
     */
    private void downloadFilteredListAsPdf() {
        if (filteredPersonnelList.isEmpty()) {
            showErrorAlert("Liste vide", "Aucun personnel à exporter. Veuillez ajuster vos filtres.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder la liste du personnel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("liste_personnel_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        
        Stage stage = (Stage) personnelTable.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                generatePdfReport(file);
                showInfoAlert("Export réussi", "La liste a été exportée avec succès vers: " + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Erreur d'export", "Impossible d'exporter le fichier PDF: " + e.getMessage());
            }
        }
    }
    
    /**
     * Génère le rapport PDF.
     */
    private void generatePdfReport(java.io.File file) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        
        document.open();
        
        // En-tête du document
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
        
        // Titre
        Paragraph title = new Paragraph("LISTE DU PERSONNEL MILITAIRE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Informations sur les filtres appliqués
        StringBuilder filterInfo = new StringBuilder("Filtres appliqués: ");
        List<String> appliedFilters = new ArrayList<>();
        
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            appliedFilters.add("Recherche: " + searchField.getText().trim());
        }
        if (formationFilter != null && formationFilter.getValue() != null) appliedFilters.add("Formation: " + formationFilter.getValue());
        if (gradeFilter != null && gradeFilter.getValue() != null) appliedFilters.add("Grade: " + gradeFilter.getValue());
        if (positionAdministrativeFilter != null && positionAdministrativeFilter.getValue() != null) appliedFilters.add("Position Admin.: " + positionAdministrativeFilter.getValue());
        if (statutFilter != null && statutFilter.getValue() != null) appliedFilters.add("Statut: " + statutFilter.getValue());
        if (specialiteFilter != null && specialiteFilter.getValue() != null) appliedFilters.add("Spécialité: " + specialiteFilter.getValue());
        if (regimeMatrimonialFilter != null && regimeMatrimonialFilter.getValue() != null) appliedFilters.add("Régime Matrimonial: " + regimeMatrimonialFilter.getValue());
        
        if (appliedFilters.isEmpty()) {
            filterInfo.append("Aucun filtre (liste complète)");
        } else {
            filterInfo.append(String.join(", ", appliedFilters));
        }
        
        Paragraph filterParagraph = new Paragraph(filterInfo.toString(), normalFont);
        filterParagraph.setSpacingAfter(15);
        document.add(filterParagraph);
        
        // Date et nombre de résultats
        Paragraph infoParagraph = new Paragraph(
            "Date d'extraction: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            " | Nombre de personnes: " + filteredPersonnelList.size(), normalFont);
        infoParagraph.setSpacingAfter(20);
        document.add(infoParagraph);
        
        // Tableau des données
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{10, 15, 15, 12, 12, 12, 15, 15, 14});
        
        // En-têtes de colonnes
        String[] headers = {"Matricule", "Nom", "Prénom", "Formation", "Grade", "Statut", "Position Admin.", "Spécialité", "Régime Matrim."};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }
        
        // Données
        for (Personnel personnel : filteredPersonnelList) {
            table.addCell(new PdfPCell(new Phrase(personnel.getMatricule(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getNom(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getPrenom(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getFormation(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getGrade(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getStatut(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getPositionAdministrative(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getSpecialite(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(personnel.getRegimeMatrimonial(), normalFont)));
        }
        
        document.add(table);
        
        // Pied de page
        Paragraph footer = new Paragraph("\n\nDocument généré automatiquement par le système de gestion du personnel militaire.", 
                                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
        
        document.close();
    }
    
    /**
     * Gère la modification d'un personnel.
     */
    private void handleModify(Personnel personnel) {
        showAuthenticationDialog(personnel);
    }
    
    
    /**
     * Gère le téléchargement des données d'un personnel.
     */
    private void handleDownload(Personnel personnel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder les informations du personnel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("personnel_" + personnel.getMatricule() + "_" + 
                                       LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        
        Stage stage = (Stage) personnelTable.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                generateIndividualPersonnelReport(personnel, file);
                showInfoAlert("Téléchargement réussi", "Les informations de " + personnel.getNom() + " " + 
                            personnel.getPrenom() + " ont été exportées vers: " + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Erreur de téléchargement", "Impossible de générer le rapport: " + e.getMessage());
            }
        }
    }
    
    /**
     * Génère un rapport individuel pour un personnel.
     */
    private void generateIndividualPersonnelReport(Personnel personnel, java.io.File file) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        
        document.open();
        
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
        
        // Titre
        Paragraph title = new Paragraph("FICHE PERSONNEL MILITAIRE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);
        
        // Informations personnelles
        document.add(new Paragraph("INFORMATIONS GÉNÉRALES", labelFont));
        document.add(new Paragraph("Matricule: " + personnel.getMatricule(), normalFont));
        document.add(new Paragraph("Nom: " + personnel.getNom(), normalFont));
        document.add(new Paragraph("Prénom: " + personnel.getPrenom(), normalFont));
        document.add(new Paragraph("Formation: " + personnel.getFormation(), normalFont));
        document.add(new Paragraph("Grade: " + personnel.getGrade(), normalFont));
        document.add(new Paragraph("Statut: " + personnel.getStatut(), normalFont));
        document.add(new Paragraph("Position Administrative: " + personnel.getPositionAdministrative(), normalFont));
        document.add(new Paragraph("Spécialité: " + personnel.getSpecialite(), normalFont));
        document.add(new Paragraph("Régime Matrimonial: " + personnel.getRegimeMatrimonial(), normalFont));
        
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Date d'extraction: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
        
        document.close();
    }
    
    /**
     * Classe Personnel étendue avec les nouveaux champs.
     */
    /**
     * Classe Personnel étendue avec les nouveaux champs.
     */
    public static class Personnel {
        private SimpleStringProperty matricule;
        private SimpleStringProperty nom;
        private SimpleStringProperty prenom;
        private SimpleStringProperty formation;
        private SimpleStringProperty grade;
        private SimpleStringProperty statut;
        private SimpleStringProperty positionAdministrative;
        private SimpleStringProperty specialite;
        private SimpleStringProperty regimeMatrimonial;

        public Personnel(String matricule, String nom, String prenom, String formation, String grade,
                        String statut, String positionAdministrative, String specialite, String regimeMatrimonial) {
            this.matricule = new SimpleStringProperty(matricule != null ? matricule : "");
            this.nom = new SimpleStringProperty(nom != null ? nom : "");
            this.prenom = new SimpleStringProperty(prenom != null ? prenom : "");
            this.formation = new SimpleStringProperty(formation != null ? formation : "");
            this.grade = new SimpleStringProperty(grade != null ? grade : "");
            this.statut = new SimpleStringProperty(statut != null ? statut : "");
            this.positionAdministrative = new SimpleStringProperty(positionAdministrative != null ? positionAdministrative : "");
            this.specialite = new SimpleStringProperty(specialite != null ? specialite : "");
            this.regimeMatrimonial = new SimpleStringProperty(regimeMatrimonial != null ? regimeMatrimonial : "");
        }

        // Getters
        public String getMatricule() { return matricule.get(); }
        public String getNom() { return nom.get(); }
        public String getPrenom() { return prenom.get(); }
        public String getFormation() { return formation.get(); }
        public String getGrade() { return grade.get(); }
        public String getStatut() { return statut.get(); }
        public String getPositionAdministrative() { return positionAdministrative.get(); }
        public String getSpecialite() { return specialite.get(); }
        public String getRegimeMatrimonial() { return regimeMatrimonial.get(); }
        
        // Setters si nécessaires
        public void setStatut(String statut) { this.statut.set(statut != null ? statut : ""); }
        public void setPositionAdministrative(String positionAdministrative) { 
            this.positionAdministrative.set(positionAdministrative != null ? positionAdministrative : ""); 
        }
        public void setSpecialite(String specialite) { this.specialite.set(specialite != null ? specialite : ""); }
        public void setRegimeMatrimonial(String regimeMatrimonial) { 
            this.regimeMatrimonial.set(regimeMatrimonial != null ? regimeMatrimonial : ""); 
        }
    }    
    // Méthodes pour les événements FXML existants
    @FXML
    void handleNewAircraft(ActionEvent event) {
        System.out.println("Ajout d'un nouvel aéronef");
    }

    @FXML
    private void handleNewPersonnel(ActionEvent event) {
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
            showErrorAlert("Erreur", "Impossible d'ouvrir l'interface d'ajout de personnel: " + e.getMessage());
        }
    }

    @FXML
    private void handleNewWeapon(ActionEvent event) {
        System.out.println("Ajout d'un nouveau véhicule");
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
    void showDashboard(ActionEvent event) {
        System.out.println("Affichage du dashboard");
    }

    @FXML
    void showHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
            
            Stage stage = (Stage) accueil.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accueil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    void showSearch(ActionEvent event) {
        System.out.println("Affichage de la page de recherche");
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        showHome(event);
    }
    
    @FXML
    private void handleModify(ActionEvent event) {
        Personnel selectedPersonnel = personnelTable.getSelectionModel().getSelectedItem();
        if (selectedPersonnel != null) {
            handleModify(selectedPersonnel);
        } else {
            showErrorAlert("Aucune sélection", "Veuillez sélectionner un personnel dans le tableau.");
        }
    }
    
    /**
     * Affiche une alerte d'erreur.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Affiche une alerte d'information.
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}