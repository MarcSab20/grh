package application;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.prefs.Preferences;
import application.historiqueController;

/**
 * Contrôleur pour la gestion du personnel militaire.
 * Ce contrôleur gère l'interface utilisateur et les interactions avec la base de données
 * pour l'ajout, la modification et la suppression des données du personnel.
 * 
 * @author [Votre nom]
 * @version 1.0
 */
public class AjoutPersonnelController {
    
    // ******************** Attributs FXML ********************
    
    @FXML private BorderPane borderPane;
    @FXML private VBox sidebar;
    @FXML private VBox contentArea;
    
    // Boutons principaux de la sidebar
    @FXML private Button personelBtn;
    @FXML private Button gradeBtn;
    @FXML private Button formationBtn;
    @FXML private Button ecoleBtn;
    @FXML private Button operationBtn;
    @FXML private Button decorationBtn;
    @FXML private Button punishmentBtn;
    @FXML private Button languesBtn;
    @FXML private Button specialBtn;
    @FXML private Button dotationBtn;
    
    // Sous-menus de la sidebar
    @FXML private VBox personalSubMenu;
    @FXML private Button personalIdentityBtn;
    @FXML private Button socialIdentityBtn;
    @FXML private Button culturalIdentityBtn;
    
    @FXML private VBox operationsSubMenu;
    @FXML private Button interiorOperationsBtn;
    @FXML private Button exteriorOperationsBtn;
    
    @FXML private VBox specialSubMenu;
    @FXML private Button generalInfoBtn;
    @FXML private Button navigatingCrewBtn;
    @FXML private Button maintenanceBtn;
    @FXML private Button specialtyBtn;
    
    @FXML private VBox dotationSubMenu;
    @FXML private Button dotation20MaiBtn;
    @FXML private Button dotationParticuliereBtn;
    @FXML private Button parametresCorporelsBtn;
    
    // Boutons de navigation
    @FXML private Button previousBtn;
    @FXML private Button nextBtn;
    @FXML private Button submitBtn;
    @FXML private Button backBtn;
    
    // Éléments pour afficher le matricule
    @FXML private HBox matriculeContainer;
    @FXML private Label matriculeLabel;
    
    // ******************** Attributs privés ********************
    
    // Constantes pour les dimensions des éléments d'interface
    private final double LABEL_WIDTH = 200;
    private final double FIELD_WIDTH = 250;
    
    // Variable pour le suivi de la navigation
    private int currentMainSection = 0;
    private int currentSubSection = 0;
    
    // Structure de navigation pour les sections principales et sous-sections
    private final String[] mainSections = {
        "Informations Personnelles", "Grade", "Formation et Poste", "Écoles et Diplômes",
        "Opérations et Déploiements", "Décorations", "Punitions", "Langues",
        "Informations Spécifiques", "Dotations Militaires"
    };
    
    // Map pour stocker les sous-sections par section principale
    private Map<String, String[]> subSections;
    
    // Map pour stocker les VBox des sous-menus
    private Map<String, VBox> subMenus;
    
    // Map pour stocker les boutons des sous-menus
    private Map<String, List<Button>> subMenuButtons;
    
    // Variable pour stocker le matricule d'incorporation actuel
    private String currentMatricule;
    
    // Map pour stocker les données saisies
    private Map<String, Map<String, Object>> formData;
    
    // Variables pour les tables d'interface utilisateur
    private TableView<Grade> gradeTable;
    private TableView<Decoration> decorationTable;
    private GridPane languagesGrid;
    
    private List<VBox> allSubMenus = new ArrayList<>();
    
    // Historique de navigation pour le bouton "Précédent"
    private List<NavigationHistoryItem> navigationHistory;
    
    // ******************** Constructeur ********************
    
    /**
     * Constructeur par défaut.
     */
    public AjoutPersonnelController() {
        subSections = new HashMap<>();
        subMenus = new HashMap<>();
        subMenuButtons = new HashMap<>();
        formData = new HashMap<>();
        navigationHistory = new ArrayList<>();
        
        // Initialisation des sous-sections
        subSections.put("Informations Personnelles", new String[]{"Identité Personnelle", "Identité Sociale", "Identité Culturelle"});
        subSections.put("Opérations et Déploiements", new String[]{"Opérations Intérieures", "Opérations Extérieures"});
        subSections.put("Informations Spécifiques", new String[]{"Général", "Personnel Naviguant", "Maintenance", "Spécialité"});
        subSections.put("Dotations Militaires", new String[]{"Dotation 20 Mai", "Dotation Particulière", "Paramètres Corporels"});
    }
    
    // Classe interne pour l'historique de navigation
    private class NavigationHistoryItem {
        private int mainSection;
        private int subSection;
        
        public NavigationHistoryItem(int mainSection, int subSection) {
            this.mainSection = mainSection;
            this.subSection = subSection;
        }
        
        public int getMainSection() {
            return mainSection;
        }
        
        public int getSubSection() {
            return subSection;
        }
    }
    
// ******************** Méthodes d'initialisation ********************
    
    /**
     * Méthode d'initialisation appelée automatiquement après le chargement du FXML.
     * Configure les gestionnaires d'événements et prépare l'interface utilisateur.
     */
    @FXML
    private void initialize() {
        // Initialisation du matricule
    	if (matriculeLabel != null) {
    		currentMatricule = "";
            matriculeLabel.setText("À saisir");
        } else {
            System.out.println("Attention: matriculeLabel est null");
            
        }
        
        // Initialisation des maps pour les sous-menus
        subMenus.put("Informations Personnelles", personalSubMenu);
        subMenus.put("Opérations et Déploiements", operationsSubMenu);
        subMenus.put("Informations Spécifiques", specialSubMenu);
        subMenus.put("Dotations Militaires", dotationSubMenu);
        
        // Initialisation des listes de boutons pour les sous-menus
        List<Button> personalButtons = new ArrayList<>();
        personalButtons.add(personalIdentityBtn);
        personalButtons.add(socialIdentityBtn);
        personalButtons.add(culturalIdentityBtn);
        subMenuButtons.put("Informations Personnelles", personalButtons);
        
        List<Button> operationsButtons = new ArrayList<>();
        operationsButtons.add(interiorOperationsBtn);
        operationsButtons.add(exteriorOperationsBtn);
        subMenuButtons.put("Opérations et Déploiements", operationsButtons);
        
        List<Button> specialButtons = new ArrayList<>();
        specialButtons.add(generalInfoBtn);
        specialButtons.add(navigatingCrewBtn);
        specialButtons.add(maintenanceBtn);
        specialButtons.add(specialtyBtn);
        subMenuButtons.put("Informations Spécifiques", specialButtons);
        
        List<Button> dotationButtons = new ArrayList<>();
        dotationButtons.add(dotation20MaiBtn);
        dotationButtons.add(dotationParticuliereBtn);
        dotationButtons.add(parametresCorporelsBtn);
        subMenuButtons.put("Dotations Militaires", dotationButtons);
        
        // Configuration des événements pour les boutons principaux de la sidebar
        setupMainButtonHandlers();
        
        // Configuration des événements pour les boutons des sous-menus
        setupSubMenuButtonHandlers();
        
        // Initialiser l'interface avec la première section (Informations Personnelles)
        navigateToSection(0, 0); // Premier menu principal, première sous-section
    }
    
    
    @FXML
    private void handleAddPersonnel() {}
    
    @FXML
    private void handleExportPersonnel() {}
    
    @FXML
    private void handleImportPersonnel() {}
    
    @FXML
    private void handlePersonnelModDown() {}
    
    @FXML
    private void handlePersonnelVisual() {}
    
    @FXML
    private void showHistory() {}
    
    @FXML
    private void showAboutDialog() {}
    
    @FXML
    private void handleNewPersonnel() {}
   
    
    
    /**
     * Configure les gestionnaires d'événements pour les boutons principaux de la sidebar.
     */
    private void setupMainButtonHandlers() {
        // Configurer uniquement les boutons sans sous-menus pour naviguer directement
        gradeBtn.setOnAction(event -> navigateToSection(1, -1));
        formationBtn.setOnAction(event -> navigateToSection(2, -1));
        ecoleBtn.setOnAction(event -> navigateToSection(3, -1));
        decorationBtn.setOnAction(event -> navigateToSection(5, -1));
        punishmentBtn.setOnAction(event -> navigateToSection(6, -1));
        languesBtn.setOnAction(event -> navigateToSection(7, -1));
        
        // Configurer les boutons avec sous-menus pour baculer leurs sous-menus respectifs
        personelBtn.setOnAction(e -> {
            toggleSubMenu(personalSubMenu);
            // Si on ouvre le sous-menu, naviguer vers la première sous-section
            if (personalSubMenu.isVisible()) {
                navigateToSection(0, 0);
            }
        });
        
        operationBtn.setOnAction(e -> {
            toggleSubMenu(operationsSubMenu);
            // Si on ouvre le sous-menu, naviguer vers la première sous-section
            if (operationsSubMenu.isVisible()) {
                navigateToSection(4, 0);
            }
        });
        
        specialBtn.setOnAction(e -> {
            toggleSubMenu(specialSubMenu);
            // Si on ouvre le sous-menu, naviguer vers la première sous-section
            if (specialSubMenu.isVisible()) {
                navigateToSection(8, 0);
            }
        });
        
        dotationBtn.setOnAction(e -> {
            toggleSubMenu(dotationSubMenu);
            // Si on ouvre le sous-menu, naviguer vers la première sous-section
            if (dotationSubMenu.isVisible()) {
                navigateToSection(9, 0);
            }
        });
    }
    
    /**
     * Configure les gestionnaires d'événements pour les boutons des sous-menus.
     */
    private void setupSubMenuButtonHandlers() {
    	
    	

        // Informations Personnelles
        personalIdentityBtn.setOnAction(event -> handleSubButtonClick(event, 0, 0));
        socialIdentityBtn.setOnAction(event -> handleSubButtonClick(event, 0, 1));
        culturalIdentityBtn.setOnAction(event -> handleSubButtonClick(event, 0, 2));
        
        // Opérations et Déploiements
        interiorOperationsBtn.setOnAction(event -> handleSubButtonClick(event, 4, 0));
        exteriorOperationsBtn.setOnAction(event -> handleSubButtonClick(event, 4, 1));
        
        // Informations Spécifiques
        generalInfoBtn.setOnAction(event -> handleSubButtonClick(event, 8, 0));
        navigatingCrewBtn.setOnAction(event -> handleSubButtonClick(event, 8, 1));
        maintenanceBtn.setOnAction(event -> handleSubButtonClick(event, 8, 2));
        specialtyBtn.setOnAction(event -> handleSubButtonClick(event, 8, 3));
        
        // Dotations Militaires
        dotation20MaiBtn.setOnAction(event -> handleSubButtonClick(event, 9, 0));
        dotationParticuliereBtn.setOnAction(event -> handleSubButtonClick(event, 9, 1));
        parametresCorporelsBtn.setOnAction(event -> handleSubButtonClick(event, 9, 2));
    }
    
    // ******************** Méthodes de navigation ********************
    
    /**
     * Gère le clic sur un bouton principal de la sidebar.
     * 
     * @param event L'événement de clic
     * @param sectionIndex L'indice de la section principale
     */
    private void handleMainButtonClick(ActionEvent event, int sectionIndex) {
        String sectionName = mainSections[sectionIndex];
        
        // Fermer tous les sous-menus d'abord
        closeAllSubMenus();
        
        // Si cette section a des sous-sections, afficher le sous-menu
        if (subSections.containsKey(sectionName)) {
            VBox subMenu = subMenus.get(sectionName);
            toggleSubMenu(subMenu);
            navigateToSection(sectionIndex, 0); // Aller à la première sous-section par défaut
        } else {
            // Sinon, naviguer directement à cette section
            navigateToSection(sectionIndex, -1);
        }
    }
    
    /**
     * Gère le clic sur un bouton de sous-menu.
     * 
     * @param event L'événement de clic
     * @param mainSectionIndex L'indice de la section principale
     * @param subSectionIndex L'indice de la sous-section
     */
    private void handleSubButtonClick(ActionEvent event, int mainSectionIndex, int subSectionIndex) {
        navigateToSection(mainSectionIndex, subSectionIndex);
    }
    
    /**
     * Ferme tous les sous-menus de la sidebar.
     */
    private void closeAllSubMenus() {
        for (VBox subMenu : subMenus.values()) {
            toggleSubMenu(subMenu);
        }
    }
    
    // Méthode pour enregistrer les sous-menus lors de l'initialisation
    private void registerSubMenu(VBox subMenu) {
        if (!allSubMenus.contains(subMenu)) {
            allSubMenus.add(subMenu);
            // S'assurer que les sous-menus sont initialement cachés
            subMenu.setVisible(false);
            subMenu.setManaged(false);
        }
    }
    
    private void initializeSubMenus() {
        // Ajoutez tous vos sous-menus à la liste
        allSubMenus.add(personalSubMenu);
        allSubMenus.add(operationsSubMenu);
        allSubMenus.add(specialSubMenu);
        allSubMenus.add(dotationSubMenu);
        
        // Assurez-vous que tous les sous-menus sont initialement cachés
        for (VBox menu : allSubMenus) {
            menu.setVisible(false);
            menu.setManaged(false);
        }
    }

    
    /**
     * Bascule l'affichage d'un sous-menu tout en fermant les autres
     * Cette méthode conserve la signature d'origine
     * @param subMenu Le sous-menu à basculer
     */
    private void toggleSubMenu(VBox subMenu) {
        // Vérifier si c'est le premier appel (initialiser la liste si nécessaire)
        if (allSubMenus.isEmpty()) {
            initializeSubMenus();
        }
        
        boolean isCurrentlyVisible = subMenu.isVisible();
        
        // Fermer tous les sous-menus d'abord
        for (VBox menu : allSubMenus) {
            if (menu.isVisible()) {
                // Rétablir l'état du bouton parent du menu qu'on ferme
                Button parentButton = findParentButton(menu);
                if (parentButton != null) {
                    String text = parentButton.getText();
                    if (text.contains("▲")) {
                        parentButton.setText(text.replace("▲", "▼"));
                    }
                }
                
                // Fermer le menu avec animation
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), menu);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    menu.setVisible(false);
                    menu.setManaged(false);
                });
                fadeOut.play();
            }
        }
        
        // Si nous avons cliqué sur le sous-menu actuellement visible,
        // on le ferme simplement et on sort (déjà fait ci-dessus)
        if (isCurrentlyVisible) {
            return;
        }
        
        // Sinon, on ouvre le sous-menu demandé
        subMenu.setManaged(true);
        subMenu.setVisible(true);
        
        // Animation pour afficher le sous-menu
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), subMenu);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Mettre à jour l'icône du bouton parent
        Button button = findParentButton(subMenu);
        if (button != null) {
            String text = button.getText();
            if (text.contains("▼")) {
                button.setText(text.replace("▼", "▲"));
            }
        }
    }
    
    /**
     * Trouve le bouton parent d'un sous-menu
     * Implémentation basée sur votre structure FXML
     */
    private Button findParentButton(VBox subMenu) {
        if (subMenu == personalSubMenu) {
            return personelBtn;
        } else if (subMenu == operationsSubMenu) {
            return operationBtn;
        } else if (subMenu == specialSubMenu) {
            return specialBtn;
        } else if (subMenu == dotationSubMenu) {
            return dotationBtn;
        }
        return null;
    }
    
    /**
     * Navigue vers une section spécifique (principale ou sous-section).
     * 
     * @param mainSectionIndex L'indice de la section principale
     * @param subSectionIndex L'indice de la sous-section (-1 si pas de sous-section)
     */
    private void navigateToSection(int mainSectionIndex, int subSectionIndex) {
        // Ajouter l'état actuel à l'historique de navigation
        if (currentMainSection != mainSectionIndex || currentSubSection != subSectionIndex) {
            navigationHistory.add(new NavigationHistoryItem(currentMainSection, currentSubSection));
        }
        
        // Mettre à jour les indices de section actuelle
        currentMainSection = mainSectionIndex;
        currentSubSection = subSectionIndex;
        
        // Mettre à jour l'état actif des boutons
        updateActiveButtons();
        
        // Nettoyer le contenu actuel
        contentArea.getChildren().clear();
        
        // Afficher le contenu approprié en fonction de la section
        String sectionName = mainSections[mainSectionIndex];
        
        switch (sectionName) {
            case "Informations Personnelles":
                if (subSectionIndex == 0) showPersonalIdentity();
                else if (subSectionIndex == 1) showSocialIdentity();
                else if (subSectionIndex == 2) showCulturalIdentity();
                break;
            case "Grade":
                showGradeInfo();
                break;
            case "Formation et Poste":
                showFormationInfo();
                break;
            case "Écoles et Diplômes":
                showEcolesEtDiplomesInfo();
                break;
            case "Opérations et Déploiements":
                if (subSectionIndex == 0) showInteriorOperations();
                else if (subSectionIndex == 1) showExteriorOperations();
                break;
            case "Décorations":
                showDecorationInfo();
                break;
            case "Punitions":
                showPunishmentInfo();
                break;
            case "Langues":
                showLanguesInfo();
                break;
            case "Informations Spécifiques":
                if (subSectionIndex == 0) showGeneralSpecialInfo();
                else if (subSectionIndex == 1) showNavigatingCrewInfo();
                else if (subSectionIndex == 2) showMaintenanceInfo();
                else if (subSectionIndex == 3) showSpecialtyInfo();
                break;
            case "Dotations Militaires":
                if (subSectionIndex == 0) showDotation20Mai();
                else if (subSectionIndex == 1) showDotationParticuliere();
                else if (subSectionIndex == 2) showParametresCorporels();
                break;
        }
        
        // Mettre à jour l'état des boutons de navigation
        updateNavigationButtons();
    }
    
    /**
     * Met à jour l'état actif des boutons de la sidebar.
     */
    private void updateActiveButtons() {
        // Réinitialiser tous les boutons principaux
        personelBtn.getStyleClass().remove("active");
        gradeBtn.getStyleClass().remove("active");
        formationBtn.getStyleClass().remove("active");
        ecoleBtn.getStyleClass().remove("active");
        operationBtn.getStyleClass().remove("active");
        decorationBtn.getStyleClass().remove("active");
        punishmentBtn.getStyleClass().remove("active");
        languesBtn.getStyleClass().remove("active");
        specialBtn.getStyleClass().remove("active");
        dotationBtn.getStyleClass().remove("active");
        
        // Réinitialiser tous les boutons de sous-menu
        for (List<Button> buttons : subMenuButtons.values()) {
            for (Button button : buttons) {
                button.getStyleClass().remove("active");
            }
        }
        
        // Définir le bouton principal actif
        Button mainButton = getMainButtonByIndex(currentMainSection);
        if (mainButton != null) {
            mainButton.getStyleClass().add("active");
        }
        
        // Si une sous-section est sélectionnée, définir son bouton comme actif
        if (currentSubSection >= 0) {
            String sectionName = mainSections[currentMainSection];
            if (subMenuButtons.containsKey(sectionName)) {
                List<Button> buttons = subMenuButtons.get(sectionName);
                if (currentSubSection < buttons.size()) {
                    buttons.get(currentSubSection).getStyleClass().add("active");
                }
            }
        }
    }
    
    /**
     * Retourne le bouton principal correspondant à l'indice donné.
     * 
     * @param index L'indice de la section principale
     * @return Le bouton correspondant
     */
    private Button getMainButtonByIndex(int index) {
        switch (index) {
            case 0: return personelBtn;
            case 1: return gradeBtn;
            case 2: return formationBtn;
            case 3: return ecoleBtn;
            case 4: return operationBtn;
            case 5: return decorationBtn;
            case 6: return punishmentBtn;
            case 7: return languesBtn;
            case 8: return specialBtn;
            case 9: return dotationBtn;
            default: return null;
        }
    }
    
    /**
     * Met à jour l'état des boutons de navigation (Précédent, Suivant, Soumettre).
     */
    private void updateNavigationButtons() {
        // Activer/désactiver le bouton Précédent
        previousBtn.setDisable(navigationHistory.isEmpty());
        
        // Activer/désactiver le bouton Suivant
        boolean isLastSection = currentMainSection == mainSections.length - 1;
        boolean isLastSubSection = true;
        
        if (currentSubSection >= 0) {
            String sectionName = mainSections[currentMainSection];
            if (subSections.containsKey(sectionName)) {
                String[] subSectionNames = subSections.get(sectionName);
                isLastSubSection = currentSubSection == subSectionNames.length - 1;
            }
        }
        
        nextBtn.setDisable(isLastSection && isLastSubSection);
        
        // Activer le bouton Soumettre uniquement sur la dernière section
        submitBtn.setDisable(!isLastSection);
    }
    
    
    /**
     * Crée un groupe pour les sections de formulaire avec un titre.
     * 
     * @param title Le titre de la section
     * @return Un VBox contenant le titre et les champs de formulaire
     */
    private VBox createFormSection(String title) {
        VBox section = new VBox(10);
        section.getStyleClass().add("form-section");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("form-title");
        
        section.getChildren().add(titleLabel);
        
        return section;
    }
    
    /**
     * Crée une ligne de formulaire avec un label et un champ de saisie.
     * 
     * @param labelText Le texte du label
     * @param field Le champ de saisie
     * @return Un HBox contenant le label et le champ
     */
    private HBox createFormField(String labelText, Control field) {
        HBox hbox = new HBox(10);
        hbox.getStyleClass().add("form-field");
        
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        label.setPrefWidth(LABEL_WIDTH);
        
        field.getStyleClass().add("form-input");
        if (field instanceof TextField || field instanceof ComboBox || field instanceof DatePicker) {
            ((Control) field).setPrefWidth(FIELD_WIDTH);
        }
        
        hbox.getChildren().addAll(label, field);
        
        return hbox;
    }
    
    /**
     * Crée un ComboBox avec les éléments fournis.
     * 
     * @param items Les éléments à ajouter au ComboBox
     * @return Le ComboBox créé
     */
    private ComboBox<String> createComboBox(String... items) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(items);
        return comboBox;
    }
    
// ******************** Informations Personnelles ********************
    
    /**
     * Méthode appelée lorsque l'utilisateur clique sur le bouton "Informations Personnelles".
     * Affiche la première sous-section (Identité Personnelle).
     */
    @FXML
    private void showPersonalInfo() {
        // Cette méthode est liée au bouton et gérée par handleMainButtonClick
        // Elle ne fait rien ici car la logique est dans navigateToSection
    }
    
    /**
     * Affiche le formulaire d'identité personnelle.
     */
    @FXML
    private void showPersonalIdentity() {
        VBox formContainer = createFormSection("Identité Personnelle");
        
        // Récupérer les données existantes
        Map<String, Object> personalData = formData.getOrDefault("identite_personnelle", new HashMap<>());
        
        // Champ pour le matricule d'incorporation - MAINTENANT MODIFIABLE
        TextField matriculeIncorporationField = new TextField(currentMatricule != null ? currentMatricule : "");
        matriculeIncorporationField.setPromptText("Matricule d'incorporation");
        // SUPPRESSION de setEditable(false) et du style grisé
        
        formContainer.getChildren().add(createFormField("Matricule d'incorporation", matriculeIncorporationField));
        
        // Ajouter un listener pour mettre à jour currentMatricule quand l'utilisateur tape
        matriculeIncorporationField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentMatricule = newVal;
            if (matriculeLabel != null) {
                matriculeLabel.setText(newVal.isEmpty() ? "À saisir" : newVal);
            }
        });
        
        // Ajouter un champ pour la photo
        HBox photoBox = new HBox(10);
        photoBox.setAlignment(Pos.CENTER_LEFT);
        
        Button photoButton = new Button("Choisir une photo");
        ImageView photoView = new ImageView();
        photoView.setFitHeight(100);
        photoView.setFitWidth(100);
        photoView.setPreserveRatio(true);
        
        // Pré-charger l'image si elle existe
        String photoPath = (String) personalData.get("photo_path");
        if (photoPath != null && !photoPath.isEmpty()) {
            try {
                File photoFile = new File(photoPath);
                if (photoFile.exists()) {
                    Image image = new Image(photoFile.toURI().toString());
                    photoView.setImage(image);
                }
            } catch (Exception e) {
                System.err.println("Impossible de charger l'image: " + photoPath);
            }
        }
        
        photoButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                Image image = new Image(selectedFile.toURI().toString());
                photoView.setImage(image);
                
                // Stocker le chemin de l'image dans les données du formulaire
                Map<String, Object> data = formData.getOrDefault("identite_personnelle", new HashMap<>());
                data.put("photo_path", selectedFile.getAbsolutePath());
                formData.put("identite_personnelle", data);
            }
        });
        
        photoBox.getChildren().addAll(photoButton, photoView);
        formContainer.getChildren().add(photoBox);
        
        // Champs du formulaire d'identité personnelle - PRÉ-REMPLIS
        TextField nomField = new TextField((String) personalData.getOrDefault("nom", ""));
        TextField prenomField = new TextField((String) personalData.getOrDefault("prenom", ""));
        TextField lieuNaissanceField = new TextField((String) personalData.getOrDefault("lieu_naissance", ""));
        
        DatePicker dateNaissanceField = new DatePicker();
        if (personalData.containsKey("date_naissance")) {
            dateNaissanceField.setValue((LocalDate) personalData.get("date_naissance"));
        }
        
        TextField numeroTelephoneField = new TextField((String) personalData.getOrDefault("telephone", ""));
        
        ComboBox<String> sexeField = createComboBox("Masculin", "Féminin");
        if (personalData.containsKey("sexe")) {
            sexeField.setValue((String) personalData.get("sexe"));
        }
        
        ComboBox<String> groupeSanguinField = createComboBox("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        if (personalData.containsKey("groupe_sanguin")) {
            groupeSanguinField.setValue((String) personalData.get("groupe_sanguin"));
        }
        
        formContainer.getChildren().addAll(
            createFormField("Nom", nomField),
            createFormField("Prénom", prenomField),
            createFormField("Lieu de naissance", lieuNaissanceField),
            createFormField("Date de naissance", dateNaissanceField),
            createFormField("Numéro de téléphone", numeroTelephoneField),
            createFormField("Sexe", sexeField),
            createFormField("Groupe sanguin", groupeSanguinField)
        );
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> data = formData.getOrDefault("identite_personnelle", new HashMap<>());
            
            // Mettre à jour currentMatricule avec la valeur du champ
            currentMatricule = matriculeIncorporationField.getText();
            if (matriculeLabel != null) {
                matriculeLabel.setText(currentMatricule.isEmpty() ? "À saisir" : currentMatricule);
            }
            
            // Stocker les valeurs
            data.put("matricule", currentMatricule);
            data.put("nom", nomField.getText());
            data.put("prenom", prenomField.getText());
            data.put("lieu_naissance", lieuNaissanceField.getText());
            data.put("date_naissance", dateNaissanceField.getValue());
            data.put("telephone", numeroTelephoneField.getText());
            data.put("sexe", sexeField.getValue());
            data.put("groupe_sanguin", groupeSanguinField.getValue());
            
            // Mettre à jour la map globale
            formData.put("identite_personnelle", data);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations d'identité personnelle ont été mises à jour.");
        });
        
        formContainer.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(formContainer);
    }
    
    /**
     * Affiche le formulaire d'identité sociale.
     */
    @FXML
    private void showSocialIdentity() {
        VBox formContainer = createFormSection("Identité Sociale");
        
        // Récupérer les données existantes si disponibles
        Map<String, Object> socialData = formData.getOrDefault("identite_sociale", new HashMap<>());
        
        // Champs du formulaire d'identité sociale - PRÉ-REMPLIS
        TextField nomPereField = new TextField((String) socialData.getOrDefault("nom_pere", ""));
        TextField nomMereField = new TextField((String) socialData.getOrDefault("nom_mere", ""));
        
        Spinner<Integer> nombreConjointsSpinner = new Spinner<>(0, 10, 0);
        if (socialData.containsKey("nombre_conjoints")) {
            nombreConjointsSpinner.getValueFactory().setValue((Integer) socialData.get("nombre_conjoints"));
        }
        
        Spinner<Integer> nombreEnfantsSpinner = new Spinner<>(0, 20, 0);
        if (socialData.containsKey("nombre_enfants")) {
            nombreEnfantsSpinner.getValueFactory().setValue((Integer) socialData.get("nombre_enfants"));
        }
        
        // Autres champs pré-remplis
        TextField personneContactField = new TextField((String) socialData.getOrDefault("personne_contact", ""));
        TextField lienPersonneContactField = new TextField((String) socialData.getOrDefault("lien_personne_contact", ""));
        TextField telephonePersonneContactField = new TextField((String) socialData.getOrDefault("telephone_personne_contact", ""));
        TextField lieuResidencePersonneContactField = new TextField((String) socialData.getOrDefault("lieu_residence_personne_contact", ""));
        TextField remarqueParticuliereField = new TextField((String) socialData.getOrDefault("remarque_particuliere", ""));
        
        ComboBox<String> regimeMatrimonialCombo = createComboBox("Célibataire", "Marié(e)", "Divorcé(e)", "Veuf/Veuve", "Union libre", "Séparé(e)");
        if (socialData.containsKey("regime_matrimonial")) {
            regimeMatrimonialCombo.setValue((String) socialData.get("regime_matrimonial"));
        }
        
        TextField nomConjointField = new TextField((String) socialData.getOrDefault("nom_conjoint", ""));
        
        formContainer.getChildren().addAll(
            createFormField("Nom du père", nomPereField),
            createFormField("Nom de la mère", nomMereField),
            createFormField("Nombre de conjoints", nombreConjointsSpinner),
            createFormField("Nombre d'enfants", nombreEnfantsSpinner),
            createFormField("Personne à contacter", personneContactField),
            createFormField("Lien avec la personne à contacter", lienPersonneContactField),
            createFormField("Téléphone de la personne à contacter", telephonePersonneContactField),
            createFormField("Lieu de résidence de la personne à contacter", lieuResidencePersonneContactField),
            createFormField("Remarque particulière", remarqueParticuliereField),
            createFormField("Régime matrimonial", regimeMatrimonialCombo),
            createFormField("Nom du conjoint", nomConjointField)
        );
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> data = formData.getOrDefault("identite_sociale", new HashMap<>());
            
            // Stocker les valeurs
            data.put("matricule", currentMatricule);
            data.put("nom_pere", nomPereField.getText());
            data.put("nom_mere", nomMereField.getText());
            data.put("nombre_conjoints", nombreConjointsSpinner.getValue());
            data.put("nombre_enfants", nombreEnfantsSpinner.getValue());
            data.put("personne_contact", personneContactField.getText());
            data.put("lien_personne_contact", lienPersonneContactField.getText());
            data.put("telephone_personne_contact", telephonePersonneContactField.getText());
            data.put("lieu_residence_personne_contact", lieuResidencePersonneContactField.getText());
            data.put("remarque_particuliere", remarqueParticuliereField.getText());
            data.put("regime_matrimonial", regimeMatrimonialCombo.getValue());
            data.put("nom_conjoint", nomConjointField.getText());
            
            // Mettre à jour la map globale
            formData.put("identite_sociale", data);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations d'identité sociale ont été mises à jour.");
        });
        
        formContainer.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(formContainer);
    }

    
    /**
     * Affiche le formulaire d'identité culturelle.
     */
    @FXML
    private void showCulturalIdentity() {
        VBox formContainer = createFormSection("Identité Culturelle");
        
        // Récupérer les données existantes si disponibles
        Map<String, Object> culturalData = formData.getOrDefault("identite_culturelle", new HashMap<>());
        
        // Champs du formulaire d'identité culturelle
        ComboBox<String> regionOriginField = createComboBox("Adamaoua", "Centre", "Est", "Extrême-Nord", "Littoral", "Nord", "Nord-Ouest", "Ouest", "Sud", "Sud-Ouest");
        if (culturalData.containsKey("region_origine")) {
            regionOriginField.setValue((String) culturalData.get("region_origine"));
        }
        
        ComboBox<String> departementOriginField = createComboBox("Boyo", "Bui", "Donga-Mantung", "Menchum", "Mezam", "Momo", "Ngo-Ketunjia", "Bamboutos", "Haut-Nkam", "Hauts-Plateaux", "Koung-Khi", "Menoua", "Mifi", "Ndé", "Noun", "Boumba-et-Ngoko", "Kadey", "Lom-et-Djérem", "Diamaré", "Logone-et-Chari", "Mayo-Danay", "Mayo-Kani", "Mayo-Sava", "Mayo-Tsanaga", "Djérem", "Faro-et-Déo", "Mayo-Banyo", "Mbéré", "Vina", "Bénoué", "Faro", "Mayo-Louti", "Mayo-Rey", "Lekié", "Mbam-et-Inoubou", "Mbam-et-Kim", "Méfou-et-Afamba", "Méfou-et-Akono", "Mfoundi", "Nyong-et-Kéllé", "Nyong-et-Mfoumou", "Nyong-et-So'o", "Fako", "Koupé-Manengouba", "Lebialem", "Manyu", "Meme", "Ndian", "Dja-et-Lobo", "Mvila", "Océan", "Vallée-du-Ntem", "Moungo", "Nkam", "Sanaga-Maritime", "Wouri");
        if (culturalData.containsKey("departement_origine")) {
            departementOriginField.setValue((String) culturalData.get("departement_origine"));
        }
        
        TextField arrondissementOriginField = new TextField((String) culturalData.getOrDefault("arrondissement_origine", ""));
        TextField villageField = new TextField((String) culturalData.getOrDefault("village", ""));
        
        ComboBox<String> ethnieField = createComboBox("Bamiléké", "Bassa", "Duala", "Ewondo", "Beti", "Eton", "Bakoko", "Bafia", "Fulani", "Peul", "Bororo", "Kirdi", "Tupuri", "Mundang", "Massa", "Mousgoum", "Toupouri", "Kanuri", "Kotoko", "Arabes Choa", "Baka", "Pygmées", "Maka", "Bulu", "Fang", "Bamoun", "Tikar", "Bafut", "Kom", "Banso", "Bakweri", "Widekum", "Batanga", "Balong", "Bakossi", "Banyang", "Ejagham", "Mbo", "Bangwa", "Ngemba", "Bayangi", "Metta", "Wimbum", "Nso", "Bali", "Yamba", "Mbembe", "Mambila", "Vute", "Banen", "Bafia", "Banen", "Bafaw", "Oroko", "Bakweri", "Balue", "Baneka", "Bakundu", "Mbonge", "Balundu", "Bafaw");
        if (culturalData.containsKey("ethnie")) {
            ethnieField.setValue((String) culturalData.get("ethnie"));
        }
        
        ComboBox<String> religionField = createComboBox("Christianisme", "Islam", "Hindouisme", "Bouddhisme", "Judaïsme", "Sikhisme", "Jaïnisme", "Bahaïsme", "Shintoïsme", "Taoïsme", "Confucianisme", "Chamanisme", "Zoroastrisme", "Religions traditionnelles africaines", "Religions traditionnelles amérindiennes", "Religions traditionnelles aborigènes", "Religions traditionnelles polynésiennes", "Caodaïsme", "Tenrikyo", "Foi Bahá'íe", "Wicca", "Druidisme", "Asatru", "Santeria", "Vaudou", "Rastafari", "Spiritualisme", "Théosophie", "Scientologie", "Raëlisme", "Jediisme", "Satanisme", "Néo-paganisme", "Animisme", "Omnisme", "Déisme", "Panthéisme", "Agnosticisme", "Athéisme", "Humanism séculier", "Mouvement Hare Krishna", "Mormonisme", "Témoins de Jéhovah", "Adventisme", "Cao Dai", "Falun Gong", "Eckankar", "Cheondoïsme", "Mandéisme", "Druzes", "Yézidisme", "Alévis", "Babisme", "Bön", "Tengrisme", "Odinisme", "Kimbanguisme", "Umbanda", "Candomblé");
        if (culturalData.containsKey("religion")) {
            religionField.setValue((String) culturalData.get("religion"));
        }
        
        formContainer.getChildren().addAll(
            createFormField("Région d'origine", regionOriginField),
            createFormField("Département d'origine", departementOriginField),
            createFormField("Arrondissement d'origine", arrondissementOriginField),
            createFormField("Village", villageField),
            createFormField("Ethnie", ethnieField),
            createFormField("Religion", religionField)
        );
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> data = formData.getOrDefault("identite_culturelle", new HashMap<>());
            
            // Stocker les valeurs
            data.put("matricule", currentMatricule);
            data.put("region_origine", regionOriginField.getValue());
            data.put("departement_origine", departementOriginField.getValue());
            data.put("arrondissement_origine", arrondissementOriginField.getText());
            data.put("village", villageField.getText());
            data.put("ethnie", ethnieField.getValue());
            data.put("religion", religionField.getValue());
            
            // Mettre à jour la map globale
            formData.put("identite_culturelle", data);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations d'identité culturelle ont été enregistrées.");
        });
        
        formContainer.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(formContainer);
    }

    
    // ******************** Grade ********************
    
    /**
     * Affiche le formulaire de gestion des grades.
     */
    @FXML
    private void showGradeInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Gestion des Grades");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création d'un onglet pour le grade actuel et l'historique des grades
        TabPane tabPane = new TabPane();
        
        // Onglet pour le grade actuel
        Tab currentGradeTab = new Tab("Grade Actuel");
        currentGradeTab.setClosable(false);
        currentGradeTab.setContent(createCurrentGradeContent());
        
        // Onglet pour l'historique des grades
        Tab gradesHistoryTab = new Tab("Historique des Grades");
        gradesHistoryTab.setClosable(false);
        gradesHistoryTab.setContent(createGradesHistoryContent());
        
        tabPane.getTabs().addAll(currentGradeTab, gradesHistoryTab);
        
        container.getChildren().add(tabPane);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Crée le contenu pour l'onglet du grade actuel.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createCurrentGradeContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher le grade et statut actuel
        TableView<Grade> currentGradeTable = new TableView<>();
        currentGradeTable.setPrefWidth(Double.MAX_VALUE);
        currentGradeTable.setMaxWidth(Double.MAX_VALUE);
        currentGradeTable.setMinHeight(300);
        currentGradeTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Grade, String> rangCol = new TableColumn<>("Rang");
        rangCol.setCellValueFactory(new PropertyValueFactory<>("rang"));
        
        TableColumn<Grade, String> statutCol = new TableColumn<>("Statut");
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        TableColumn<Grade, String> echelonCol = new TableColumn<>("Échelon");
        echelonCol.setCellValueFactory(new PropertyValueFactory<>("echelon"));
        
        TableColumn<Grade, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Grade, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<Grade, String> echelonGradeCol = new TableColumn<>("Échelon Grade (1-3)");
        echelonGradeCol.setCellValueFactory(new PropertyValueFactory<>("echelonGrade"));
        
        TableColumn<Grade, String> referenceEchelonCol = new TableColumn<>("Référence Échelon");
        referenceEchelonCol.setCellValueFactory(new PropertyValueFactory<>("referenceEchelon"));
        
        TableColumn<Grade, LocalDate> dateEchelonCol = new TableColumn<>("Date Échelon");
        dateEchelonCol.setCellValueFactory(new PropertyValueFactory<>("dateEchelon"));
        
        TableColumn<Grade, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Grade, Button>() {
                private final Button editButton = new Button("Modifier");
                
                {
                    editButton.getStyleClass().addAll("action-button", "edit");
                    editButton.setOnAction(event -> {
                        Grade grade = getTableView().getItems().get(getIndex());
                        if (grade != null) {
                            showGradeEditDialog(grade, currentGradeTable);
                        }
                    });
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().size() <= getIndex()) {
                        setGraphic(null);
                    } else {
                        setGraphic(editButton);
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        currentGradeTable.getColumns().addAll(rangCol, statutCol, echelonCol, dateCol, referenceCol, 
                                    echelonGradeCol, referenceEchelonCol, dateEchelonCol, actionCol);
        
        // Configuration des largeurs
        rangCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.15));
        statutCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.15));
        echelonCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.10));
        dateCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.10));
        referenceCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.15));
        echelonGradeCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.10));
        referenceEchelonCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.15));
        dateEchelonCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.10));
        actionCol.prefWidthProperty().bind(currentGradeTable.widthProperty().multiply(0.10));
        
        // Créer un bouton pour ajouter un grade actuel
        Button addCurrentGradeButton = new Button("Ajouter grade et statut actuel");
        addCurrentGradeButton.setMaxWidth(Double.MAX_VALUE);
        addCurrentGradeButton.setAlignment(Pos.CENTER);
        
        // CORRECTION : Charger le grade actuel s'il existe et l'afficher
        Grade currentGrade = null;
        if (formData.containsKey("grade_actuel")) {
            Map<String, Object> gradeData = formData.get("grade_actuel");
            if (gradeData.containsKey("grade")) {
                currentGrade = (Grade) gradeData.get("grade");
                currentGradeTable.getItems().clear(); // Vider d'abord
                currentGradeTable.getItems().add(currentGrade);
            }
        }
        
        // Le bouton addCurrentGradeButton n'est visible que si le tableau est vide
        addCurrentGradeButton.setVisible(currentGradeTable.getItems().isEmpty());
        addCurrentGradeButton.setManaged(currentGradeTable.getItems().isEmpty());
        
        // Ajouter un listener pour mettre à jour la visibilité du bouton
        currentGradeTable.getItems().addListener((ListChangeListener<Grade>) c -> {
            boolean tableIsEmpty = currentGradeTable.getItems().isEmpty();
            addCurrentGradeButton.setVisible(tableIsEmpty);
            addCurrentGradeButton.setManaged(tableIsEmpty);
        });
        
        // Configurer l'action du bouton d'ajout
        addCurrentGradeButton.setOnAction(e -> {
            showCurrentGradeAddDialog(currentGradeTable);
        });
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> gradeData = formData.getOrDefault("grade_actuel", new HashMap<>());
            
            // Stocker le grade actuel
            gradeData.put("matricule", currentMatricule);
            if (!currentGradeTable.getItems().isEmpty()) {
                Grade gradeActuel = currentGradeTable.getItems().get(0);
                gradeData.put("grade", gradeActuel);
                
                // AJOUT AUTOMATIQUE À L'HISTORIQUE
                addGradeToHistoryWhenSaving(gradeActuel);
            }
            
            // Mettre à jour la map globale
            formData.put("grade_actuel", gradeData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations du grade et statut actuel ont été mises à jour et ajoutées à l'historique.");
        });
        
        // Ajouter les composants à la vue
        content.getChildren().add(addCurrentGradeButton);
        content.getChildren().add(currentGradeTable);
        content.getChildren().add(saveButton);
        
        return content;
    }

    
    /**
     * Ajoute automatiquement un grade à l'historique lors de l'enregistrement.
     */
    private void addGradeToHistoryWhenSaving(Grade grade) {
        // Récupérer ou créer l'historique des grades
        Map<String, Object> historiqueGradesData = formData.getOrDefault("grades_historique", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Grade> grades = (List<Grade>) historiqueGradesData.getOrDefault("grades", new ArrayList<>());
        
        // Créer une copie du grade pour éviter les références partagées
        Grade gradeCopy = new Grade(
            grade.getRang(),
            grade.getEchelon(),
            grade.getDate(),
            grade.getReference(),
            grade.getEchelonGrade(),
            grade.getReferenceEchelon(),
            grade.getDateEchelon(),
            grade.getStatut()
        );
        
        // Vérifier si ce grade exact n'existe pas déjà dans l'historique
        boolean gradeExists = grades.stream().anyMatch(g -> 
            g.getRang().equals(grade.getRang()) && 
            g.getStatut().equals(grade.getStatut()) &&
            g.getDate() != null && grade.getDate() != null &&
            g.getDate().equals(grade.getDate()) &&
            g.getReference().equals(grade.getReference())
        );
        
        if (!gradeExists) {
            // Ajouter le nouveau grade en premier (le plus récent)
            grades.add(0, gradeCopy);
            historiqueGradesData.put("grades", grades);
            historiqueGradesData.put("matricule", currentMatricule);
            formData.put("grades_historique", historiqueGradesData);
            
            System.out.println("Grade ajouté à l'historique: " + grade.getRang() + " - " + grade.getStatut());
        }
    }


    /**
     * Affiche une boîte de dialogue pour ajouter un grade actuel.
     * 
     * @param gradeTable Le tableau des grades actuel
     */
    private void showCurrentGradeAddDialog(TableView<Grade> gradeTable) {
        Dialog<Grade> dialog = new Dialog<>();
        dialog.setTitle("Ajouter le Grade et Statut Actuel");
        dialog.setHeaderText("Renseignez les informations du grade et statut actuel");
        
        // Définir la taille de la fenêtre de dialogue
        dialog.getDialogPane().setPrefSize(600, 500);
        dialog.getDialogPane().setMinSize(600, 500);
        dialog.setResizable(true);
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> rangField = createComboBox();
        rangField.getItems().addAll(getGradeRanks());
        rangField.setPrefWidth(200);
        
        ComboBox<String> statutField = createComboBox("Officier Active", "Sous-Officier de Carrière", "Rengagé", "1ère période");
        statutField.setPrefWidth(200);
        
        ComboBox<String> echelonField = createComboBox("Échelon 1", "Échelon 2", "Échelon 3");
        echelonField.setPrefWidth(200);
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(200);
        
        TextField referenceField = new TextField();
        referenceField.setPrefWidth(200);
        
        Spinner<Integer> echelonGradeSpinner = new Spinner<>(1, 3, 1);
        echelonGradeSpinner.setPrefWidth(200);
        
        TextField referenceEchelonField = new TextField();
        referenceEchelonField.setPrefWidth(200);
        
        DatePicker dateEchelonPicker = new DatePicker(LocalDate.now());
        dateEchelonPicker.setPrefWidth(200);
        
        // Créer le contenu de la boîte de dialogue avec espacement amélioré
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
            createFormFieldForDialog("Rang", rangField),
            createFormFieldForDialog("Statut", statutField),
            createFormFieldForDialog("Échelon", echelonField),
            createFormFieldForDialog("Date", datePicker),
            createFormFieldForDialog("Référence", referenceField)
        );
        
        // Container pour les champs d'échelon de grade
        VBox echelonGradeFields = new VBox(15);
        echelonGradeFields.setPadding(new Insets(10));
        echelonGradeFields.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        Label echelonLabel = new Label("Informations Échelon du Grade");
        echelonLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        echelonGradeFields.getChildren().addAll(
            echelonLabel,
            createFormFieldForDialog("Échelon du Grade (1-3)", echelonGradeSpinner),
            createFormFieldForDialog("Référence de l'Échelon", referenceEchelonField),
            createFormFieldForDialog("Date de l'Échelon", dateEchelonPicker)
        );
        echelonGradeFields.setVisible(false);
        echelonGradeFields.setManaged(false);
        
        content.getChildren().add(echelonGradeFields);
        
        // Afficher/masquer les champs d'échelon de grade en fonction du rang sélectionné
        rangField.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            
            boolean needsEchelon = 
                newVal.equals("Colonel") || 
                newVal.equals("Lieutenant-colonel") || 
                newVal.equals("Commandant") || 
                newVal.equals("Capitaine") || 
                newVal.equals("Lieutenant");
            
            echelonGradeFields.setVisible(needsEchelon);
            echelonGradeFields.setManaged(needsEchelon);
            
            // Ajuster la taille de la dialog si nécessaire
            if (needsEchelon) {
                dialog.getDialogPane().setPrefSize(600, 650);
            } else {
                dialog.getDialogPane().setPrefSize(600, 500);
            }
        });
        
        // Ajouter un ScrollPane pour gérer le contenu si nécessaire
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefSize(580, 480);
        
        dialog.getDialogPane().setContent(scrollPane);
        
        // Convertir le résultat en objet Grade
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String rang = rangField.getValue();
                String statut = statutField.getValue();
                if (rang == null || statut == null) return null;
                
                boolean needsEchelon = 
                    rang.equals("Colonel") || 
                    rang.equals("Lieutenant-colonel") || 
                    rang.equals("Commandant") || 
                    rang.equals("Capitaine") || 
                    rang.equals("Lieutenant");
                
                return new Grade(
                    rang,
                    echelonField.getValue(),
                    datePicker.getValue(),
                    referenceField.getText(),
                    needsEchelon ? String.valueOf(echelonGradeSpinner.getValue()) : "1",
                    needsEchelon ? referenceEchelonField.getText() : "",
                    needsEchelon ? dateEchelonPicker.getValue() : LocalDate.now(),
                    statut
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Grade> result = dialog.showAndWait();
        
        // Ajouter le nouveau grade au tableau
        result.ifPresent(grade -> {
            // Vider le tableau d'abord pour s'assurer qu'il n'y a qu'une seule ligne
            gradeTable.getItems().clear();
            gradeTable.getItems().add(grade);
        });
    }
    
    /**
     * Crée un champ de formulaire spécialement formaté pour les boîtes de dialogue.
     */
    private HBox createFormFieldForDialog(String labelText, Control field) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPrefHeight(35);
        
        Label label = new Label(labelText);
        label.setPrefWidth(180);
        label.setMinWidth(180);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        
        field.setPrefWidth(200);
        if (field instanceof TextField) {
            field.setStyle("-fx-pref-height: 25;");
        }
        
        hbox.getChildren().addAll(label, field);
        
        return hbox;
    }
    
    /**
     * Affiche une boîte de dialogue pour vérifier l'identité de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    private boolean verifyUserIdentity() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification d'identité");
        dialog.setHeaderText("Veuillez vous authentifier pour enregistrer les données");
        
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
        
        Label instructionLabel = new Label("Authentification requise pour enregistrer les données :");
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
                showError("Veuillez remplir tous les champs.");
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
                            showError("Identifiant ou mot de passe incorrect.");
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la vérification des identifiants : " + e.getMessage());
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
     * Ajoute un grade à l'historique automatiquement.
     */
    private void addGradeToHistory(Grade grade) {
        // Récupérer ou créer l'historique des grades
        Map<String, Object> historiqueGradesData = formData.getOrDefault("grades_historique", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Grade> grades = (List<Grade>) historiqueGradesData.getOrDefault("grades", new ArrayList<>());
        
        // Ajouter le grade à l'historique s'il n'y est pas déjà
        boolean gradeExists = grades.stream().anyMatch(g -> 
            g.getRang().equals(grade.getRang()) && 
            g.getDate() != null && grade.getDate() != null &&
            g.getDate().equals(grade.getDate())
        );
        
        if (!gradeExists) {
            grades.add(grade);
            historiqueGradesData.put("grades", grades);
            historiqueGradesData.put("matricule", currentMatricule);
            formData.put("grades_historique", historiqueGradesData);
        }
    }

    /**
     * Crée le contenu pour l'onglet d'historique des grades.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createGradesHistoryContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher l'historique des grades
        TableView<Grade> gradesTable = new TableView<>();
        gradesTable.setPrefWidth(Double.MAX_VALUE);
        gradesTable.setMaxWidth(Double.MAX_VALUE);
        gradesTable.setMinHeight(300);
        gradesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau (mêmes colonnes que pour le grade actuel)
        TableColumn<Grade, String> rangCol = new TableColumn<>("Rang");
        rangCol.setCellValueFactory(new PropertyValueFactory<>("rang"));
        
        TableColumn<Grade, String> statutCol = new TableColumn<>("Statut");
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        TableColumn<Grade, String> echelonCol = new TableColumn<>("Échelon");
        echelonCol.setCellValueFactory(new PropertyValueFactory<>("echelon"));
        
        TableColumn<Grade, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Grade, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<Grade, String> echelonGradeCol = new TableColumn<>("Échelon Grade (1-3)");
        echelonGradeCol.setCellValueFactory(new PropertyValueFactory<>("echelonGrade"));
        
        // Afficher l'échelon seulement pour certains grades
        echelonGradeCol.setCellFactory(col -> {
            return new TableCell<Grade, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().size() <= getIndex()) {
                        setText(null);
                    } else {
                        Grade grade = getTableView().getItems().get(getIndex());
                        String rang = grade.getRang();
                        // Vérifier si le rang est colonel, lieutenant-colonel, commandant, capitaine ou lieutenant
                        if (rang.equals("Colonel") || rang.equals("Lieutenant-colonel") || 
                            rang.equals("Commandant") || rang.equals("Capitaine") || 
                            rang.equals("Lieutenant")) {
                            setText(item);
                        } else {
                            setText("N/A");
                        }
                    }
                }
            };
        });
        
        TableColumn<Grade, String> referenceEchelonCol = new TableColumn<>("Référence Échelon");
        referenceEchelonCol.setCellValueFactory(new PropertyValueFactory<>("referenceEchelon"));
        
        TableColumn<Grade, LocalDate> dateEchelonCol = new TableColumn<>("Date Échelon");
        dateEchelonCol.setCellValueFactory(new PropertyValueFactory<>("dateEchelon"));
        
        TableColumn<Grade, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Grade, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    editButton.getStyleClass().addAll("action-button", "edit");
                    deleteButton.getStyleClass().addAll("action-button", "delete");
                    
                    editButton.setOnAction(event -> {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            Grade grade = getTableView().getItems().get(index);
                            if (grade != null) {
                                showGradeEditDialog(grade, gradesTable);
                            }
                        }
                    });
                    
                    deleteButton.setOnAction(event -> {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            Grade grade = getTableView().getItems().get(index);
                            if (grade != null) {
                                // Confirmer la suppression
                                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                                confirmAlert.setTitle("Confirmer la suppression");
                                confirmAlert.setHeaderText("Supprimer ce grade de l'historique");
                                confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer ce grade : " + 
                                                           grade.getRang() + " (" + grade.getStatut() + ") ?");
                                
                                Optional<ButtonType> result = confirmAlert.showAndWait();
                                if (result.isPresent() && result.get() == ButtonType.OK) {
                                    gradesTable.getItems().remove(grade);
                                    showSuccessMessage("Grade supprimé de l'historique.");
                                }
                            }
                        }
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().size() <= getIndex()) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        gradesTable.getColumns().addAll(rangCol, statutCol, echelonCol, dateCol, referenceCol, 
                                       echelonGradeCol, referenceEchelonCol, dateEchelonCol,
                                       actionCol);
        
        // Configurer les largeurs des colonnes pour qu'elles prennent proportionnellement la largeur
        rangCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.13));
        statutCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.13));
        echelonCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.10));
        dateCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.10));
        referenceCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.13));
        echelonGradeCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.13));
        referenceEchelonCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.13));
        dateEchelonCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.10));
        actionCol.prefWidthProperty().bind(gradesTable.widthProperty().multiply(0.05));
        
        // Ajouter un bouton pour ajouter un nouveau grade
        Button addButton = new Button("Ajouter un Grade");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showGradeAddDialog(gradesTable));
        content.getChildren().add(addButton);
        
        // Ajouter le tableau
        content.getChildren().add(gradesTable);
        
        // CORRECTION : Précharger l'historique des grades si disponible
        if (formData.containsKey("grades_historique")) {
            Map<String, Object> gradesData = formData.get("grades_historique");
            if (gradesData.containsKey("grades")) {
                @SuppressWarnings("unchecked")
                List<Grade> grades = (List<Grade>) gradesData.get("grades");
                gradesTable.getItems().clear(); // Vider d'abord
                gradesTable.getItems().addAll(grades);
            }
        }
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> gradesData = formData.getOrDefault("grades_historique", new HashMap<>());
            
            // Stocker les valeurs
            gradesData.put("matricule", currentMatricule);
            gradesData.put("grades", gradesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("grades_historique", gradesData);
            
            // Afficher une confirmation
            showSuccessMessage("L'historique des grades a été mis à jour.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }

    
    
    /**
     * Crée un tableau pour afficher et modifier les grades.
     * 
     * @return Le tableau des grades
     */
    private TableView<Grade> createGradeTable() {
        TableView<Grade> table = new TableView<>();
        table.getStyleClass().add("grade-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Grade, String> rangCol = new TableColumn<>("Rang");
        rangCol.setCellValueFactory(new PropertyValueFactory<>("rang"));
        rangCol.setPrefWidth(150);
        
        TableColumn<Grade, String> echelonCol = new TableColumn<>("Échelon");
        echelonCol.setCellValueFactory(new PropertyValueFactory<>("echelon"));
        echelonCol.setPrefWidth(100);
        
        TableColumn<Grade, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);
        
        TableColumn<Grade, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        referenceCol.setPrefWidth(150);
        
        // Nouvelle colonne pour l'échelon du grade (spécifique aux grades comme colonel, etc.)
        TableColumn<Grade, String> echelonGradeCol = new TableColumn<>("Échelon Grade (1-3)");
        echelonGradeCol.setCellValueFactory(new PropertyValueFactory<>("echelonGrade"));
        echelonGradeCol.setPrefWidth(120);
        
        // Afficher l'échelon seulement pour certains grades
        echelonGradeCol.setCellFactory(col -> {
            return new TableCell<Grade, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        Grade grade = getTableView().getItems().get(getIndex());
                        String rang = grade.getRang();
                        // Vérifier si le rang est colonel, lieutenant-colonel, commandant, capitaine ou lieutenant
                        if (rang.equals("Colonel") || rang.equals("Lieutenant-colonel") || 
                            rang.equals("Commandant") || rang.equals("Capitaine") || 
                            rang.equals("Lieutenant")) {
                            setText(item);
                        } else {
                            setText("N/A");
                        }
                    }
                }
            };
        });
        
        TableColumn<Grade, String> referenceEchelonCol = new TableColumn<>("Référence Échelon");
        referenceEchelonCol.setCellValueFactory(new PropertyValueFactory<>("referenceEchelon"));
        referenceEchelonCol.setPrefWidth(150);
        
        TableColumn<Grade, LocalDate> dateEchelonCol = new TableColumn<>("Date Échelon");
        dateEchelonCol.setCellValueFactory(new PropertyValueFactory<>("dateEchelon"));
        dateEchelonCol.setPrefWidth(100);
        
        // Colonne pour l'indicateur de grade actuel
        TableColumn<Grade, Boolean> gradeActuelCol = new TableColumn<>("Grade Actuel");
        gradeActuelCol.setCellValueFactory(cellData -> {
            Grade grade = cellData.getValue();
            // Trouver le grade le plus récent
            Grade gradeActuel = findMostRecentGrade(table.getItems());
            return new SimpleBooleanProperty(grade.equals(gradeActuel));
        });
        gradeActuelCol.setCellFactory(col -> {
            return new TableCell<Grade, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (item) {
                            setText("✓");
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else {
                            setText("");
                        }
                    }
                }
            };
        });
        gradeActuelCol.setPrefWidth(100);
        
        TableColumn<Grade, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Grade, Button>() {
                private final Button editButton = new Button("Modifier");
                
                {
                    editButton.getStyleClass().addAll("action-button", "edit");
                    editButton.setOnAction(event -> {
                        Grade grade = getTableView().getItems().get(getIndex());
                        showGradeEditDialog(grade);
                    });
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editButton);
                    }
                }
            };
        });
        actionCol.setPrefWidth(100);
        
        // Ajouter les colonnes au tableau
        table.getColumns().addAll(rangCol, echelonCol, dateCol, referenceCol, 
                                  echelonGradeCol, referenceEchelonCol, dateEchelonCol,
                                  gradeActuelCol, actionCol);
        
        // Remplir le tableau avec les rangs
        for (String rang : getGradeRanks()) {
            table.getItems().add(new Grade(rang, "Port initial", LocalDate.now(), ""));
        }
        
        return table;
    }

    // Méthode pour trouver le grade le plus récent
    private Grade findMostRecentGrade(ObservableList<Grade> grades) {
        Grade mostRecent = null;
        LocalDate mostRecentDate = LocalDate.MIN;
        
        for (Grade grade : grades) {
            if (grade.getDate() != null && grade.getDate().isAfter(mostRecentDate)) {
                mostRecentDate = grade.getDate();
                mostRecent = grade;
            }
        }
        
        return mostRecent;
    }
    
    /**
     * Trouve le grade le plus récent parmi tous les grades enregistrés.
     * 
     * @return Le grade le plus récent, ou null si aucun grade n'est enregistré
     */
    private Grade findMostRecentGrade() {
        Grade mostRecent = null;
        LocalDate mostRecentDate = LocalDate.MIN;
        
        // Vérifier dans les données du formulaire
        if (formData.containsKey("grades_historique")) {
            Map<String, Object> gradesData = formData.get("grades_historique");
            if (gradesData.containsKey("grades")) {
                @SuppressWarnings("unchecked")
                List<Grade> grades = (List<Grade>) gradesData.get("grades");
                
                for (Grade grade : grades) {
                    if (grade.getDate() != null && grade.getDate().isAfter(mostRecentDate)) {
                        mostRecentDate = grade.getDate();
                        mostRecent = grade;
                    }
                }
            }
        }
        
        return mostRecent;
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier un grade avec support du statut.
     * 
     * @param grade Le grade à modifier
     * @param gradeTable Le tableau des grades (optionnel)
     */
    private void showGradeEditDialog(Grade grade, TableView<Grade> gradeTable) {
        if (grade == null) {
            showError("Erreur : grade non valide pour la modification.");
            return;
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Grade");
        dialog.setHeaderText("Modifier toutes les informations du grade");
        
        // Définir la taille de la fenêtre de dialogue
        dialog.getDialogPane().setPrefSize(600, 650);
        dialog.getDialogPane().setMinSize(600, 650);
        dialog.setResizable(true);
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // ========== TOUS LES CHAMPS MODIFIABLES ==========
        
        // 1. RANG (modifiable)
        ComboBox<String> rangField = createComboBox();
        rangField.getItems().addAll(getGradeRanks());
        rangField.setValue(grade.getRang() != null ? grade.getRang() : "");
        rangField.setPrefWidth(200);
        
        // 2. STATUT (modifiable)
        ComboBox<String> statutField = createComboBox("Officier Active", "Sous-Officier de Carrière", "Rengagé", "1ère période");
        statutField.setValue(grade.getStatut() != null ? grade.getStatut() : "Officier Active");
        statutField.setPrefWidth(200);
        
        // 3. ÉCHELON (modifiable)
        ComboBox<String> echelonField = createComboBox("Port initial", "Échelon 1", "Échelon 2");
        echelonField.setValue(grade.getEchelon() != null ? grade.getEchelon() : "Port initial");
        echelonField.setPrefWidth(200);
        
        // 4. DATE (modifiable)
        DatePicker datePicker = new DatePicker(grade.getDate() != null ? grade.getDate() : LocalDate.now());
        datePicker.setPrefWidth(200);
        
        // 5. RÉFÉRENCE (modifiable)
        TextField referenceField = new TextField(grade.getReference() != null ? grade.getReference() : "");
        referenceField.setPrefWidth(200);
        
        // 6. ÉCHELON DU GRADE (modifiable)
        String echelonGradeValue = grade.getEchelonGrade() != null ? grade.getEchelonGrade() : "1";
        int echelonGradeInt = 1;
        try {
            echelonGradeInt = Integer.parseInt(echelonGradeValue);
            if (echelonGradeInt < 1 || echelonGradeInt > 3) echelonGradeInt = 1;
        } catch (NumberFormatException e) {
            echelonGradeInt = 1;
        }
        
        Spinner<Integer> echelonGradeSpinner = new Spinner<>(1, 3, echelonGradeInt);
        echelonGradeSpinner.setPrefWidth(200);
        
        // 7. RÉFÉRENCE ÉCHELON (modifiable)
        TextField referenceEchelonField = new TextField(grade.getReferenceEchelon() != null ? grade.getReferenceEchelon() : "");
        referenceEchelonField.setPrefWidth(200);
        
        // 8. DATE ÉCHELON (modifiable)
        DatePicker dateEchelonPicker = new DatePicker(grade.getDateEchelon() != null ? grade.getDateEchelon() : LocalDate.now());
        dateEchelonPicker.setPrefWidth(200);
        
        // Créer le contenu principal de la boîte de dialogue
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Section des informations de base
        VBox basicInfoSection = new VBox(10);
        basicInfoSection.setStyle("-fx-border-color: #007bff; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f8f9fa; -fx-padding: 10;");
        
        Label basicInfoLabel = new Label("Informations de Base du Grade");
        basicInfoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #007bff;");
        
        basicInfoSection.getChildren().addAll(
            basicInfoLabel,
            createFormFieldForDialog("Rang", rangField),
            createFormFieldForDialog("Statut", statutField),
            createFormFieldForDialog("Échelon", echelonField),
            createFormFieldForDialog("Date", datePicker),
            createFormFieldForDialog("Référence", referenceField)
        );
        
        content.getChildren().add(basicInfoSection);
        
        // Section des informations d'échelon (conditionnelle)
        VBox echelonInfoSection = new VBox(10);
        echelonInfoSection.setPadding(new Insets(10));
        echelonInfoSection.setStyle("-fx-border-color: #28a745; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f1f8e9; -fx-padding: 10;");
        
        Label echelonInfoLabel = new Label("Informations Échelon du Grade (pour Officiers)");
        echelonInfoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #28a745;");
        
        echelonInfoSection.getChildren().addAll(
            echelonInfoLabel,
            createFormFieldForDialog("Échelon du Grade (1-3)", echelonGradeSpinner),
            createFormFieldForDialog("Référence de l'Échelon", referenceEchelonField),
            createFormFieldForDialog("Date de l'Échelon", dateEchelonPicker)
        );
        
        // Fonction pour afficher/masquer la section échelon
        Runnable updateEchelonVisibility = () -> {
            String selectedRang = rangField.getValue();
            boolean needsEchelon = selectedRang != null && (
                selectedRang.equals("Colonel") || 
                selectedRang.equals("Lieutenant-colonel") || 
                selectedRang.equals("Commandant") || 
                selectedRang.equals("Capitaine") || 
                selectedRang.equals("Lieutenant")
            );
            
            echelonInfoSection.setVisible(needsEchelon);
            echelonInfoSection.setManaged(needsEchelon);
            
            // Ajuster la taille de la dialogue
            if (needsEchelon) {
                dialog.getDialogPane().setPrefSize(600, 750);
            } else {
                dialog.getDialogPane().setPrefSize(600, 500);
            }
        };
        
        // Vérifier initialement si on a besoin de la section échelon
        updateEchelonVisibility.run();
        
        // Listener pour le changement de rang
        rangField.valueProperty().addListener((obs, oldVal, newVal) -> updateEchelonVisibility.run());
        
        content.getChildren().add(echelonInfoSection);
        
        // Note d'information
        Label infoLabel = new Label("💡 Tous les champs peuvent être modifiés. Les informations d'échelon ne s'appliquent qu'aux grades d'Officiers.");
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #6c757d; -fx-font-size: 11px;");
        content.getChildren().add(infoLabel);
        
        // Ajouter un ScrollPane pour gérer le contenu
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefSize(580, 630);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        dialog.getDialogPane().setContent(scrollPane);
        
        // Attendre le résultat de la boîte de dialogue
        Optional<ButtonType> result = dialog.showAndWait();
        
        // Mettre à jour le grade si l'utilisateur a cliqué sur Enregistrer
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                // Validation des champs obligatoires
                if (rangField.getValue() == null || rangField.getValue().trim().isEmpty()) {
                    showError("Le rang est obligatoire.");
                    return;
                }
                
                if (statutField.getValue() == null || statutField.getValue().trim().isEmpty()) {
                    showError("Le statut est obligatoire.");
                    return;
                }
                
                if (datePicker.getValue() == null) {
                    showError("La date est obligatoire.");
                    return;
                }
                
                // ========== MISE À JOUR DE TOUS LES ATTRIBUTS ==========
                
                // Sauvegarder l'ancien rang pour l'historique
                String oldRang = grade.getRang();
                
                // Mettre à jour tous les attributs
                grade.setDate(datePicker.getValue());
                grade.setReference(referenceField.getText().trim());
                grade.setEchelon(echelonField.getValue());
                grade.setStatut(statutField.getValue());
                
                // IMPORTANT: Mettre à jour le rang (c'était manquant avant !)
                // Créer un nouveau grade avec le nouveau rang
                String newRang = rangField.getValue();
                Grade updatedGrade = new Grade(
                    newRang,                                    // Nouveau rang
                    echelonField.getValue(),                    // Échelon
                    datePicker.getValue(),                      // Date
                    referenceField.getText().trim(),            // Référence
                    String.valueOf(echelonGradeSpinner.getValue()), // Échelon du grade
                    referenceEchelonField.getText().trim(),     // Référence échelon
                    dateEchelonPicker.getValue(),               // Date échelon
                    statutField.getValue()                      // Statut
                );
                
                // Vérifier si c'est un grade d'officier pour les informations d'échelon
                boolean needsEchelon = newRang.equals("Colonel") || 
                                      newRang.equals("Lieutenant-colonel") || 
                                      newRang.equals("Commandant") || 
                                      newRang.equals("Capitaine") || 
                                      newRang.equals("Lieutenant");
                
                if (needsEchelon) {
                    updatedGrade.setEchelonGrade(String.valueOf(echelonGradeSpinner.getValue()));
                    updatedGrade.setReferenceEchelon(referenceEchelonField.getText().trim());
                    updatedGrade.setDateEchelon(dateEchelonPicker.getValue());
                } else {
                    // Pour les non-officiers, définir des valeurs par défaut
                    updatedGrade.setEchelonGrade("1");
                    updatedGrade.setReferenceEchelon("");
                    updatedGrade.setDateEchelon(LocalDate.now());
                }
                
                // Remplacer l'ancien grade par le nouveau dans le tableau
                if (gradeTable != null) {
                    int index = gradeTable.getItems().indexOf(grade);
                    if (index >= 0) {
                        gradeTable.getItems().set(index, updatedGrade);
                    }
                    gradeTable.refresh();
                }
                
                // Si c'est un changement de rang, ajouter l'ancien grade à l'historique
                if (!oldRang.equals(newRang)) {
                    addGradeToHistoryWhenSaving(grade); // Ajouter l'ancien grade à l'historique
                    showSuccessMessage("Le grade a été modifié avec succès. L'ancien grade (" + oldRang + ") a été ajouté à l'historique.");
                } else {
                    showSuccessMessage("Le grade a été modifié avec succès.");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur lors de la modification du grade : " + e.getMessage());
            }
        }
    }

    
    /**
     * Méthode de compatibilité pour l'ancienne signature (garde la compatibilité avec l'existant).
     * 
     * @param grade Le grade à modifier
     */
    private void showGradeEditDialog(Grade grade) {
        showGradeEditDialog(grade, null);
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter un nouveau grade.
     * 
     * @param gradesTable Le tableau des grades
     */
    private void showGradeAddDialog(TableView<Grade> gradesTable) {
        Dialog<Grade> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Grade");
        dialog.setHeaderText("Ajouter un grade à l'historique");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> rangField = createComboBox();
        rangField.getItems().addAll(getGradeRanks());
        
        ComboBox<String> statutField = createComboBox("Officier Active", "Sous-Officier de Carrière", "Rengagé", "1ère période");
        
        ComboBox<String> echelonField = createComboBox("Port initial", "Échelon 1", "Échelon 2");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField referenceField = new TextField();
        
        Spinner<Integer> echelonGradeSpinner = new Spinner<>(1, 3, 1);
        TextField referenceEchelonField = new TextField();
        DatePicker dateEchelonPicker = new DatePicker(LocalDate.now());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Rang", rangField),
            createFormField("Statut", statutField),
            createFormField("Échelon", echelonField),
            createFormField("Date", datePicker),
            createFormField("Référence", referenceField)
        );
        
        // Container pour les champs d'échelon de grade (à afficher conditionnellement)
        VBox echelonGradeFields = new VBox(10);
        echelonGradeFields.getChildren().addAll(
            createFormField("Échelon du Grade (1-3)", echelonGradeSpinner),
            createFormField("Référence de l'Échelon", referenceEchelonField),
            createFormField("Date de l'Échelon", dateEchelonPicker)
        );
        echelonGradeFields.setVisible(false);
        echelonGradeFields.setManaged(false);
        
        content.getChildren().add(echelonGradeFields);
        
        // Afficher/masquer les champs d'échelon de grade en fonction du rang sélectionné
        rangField.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsEchelon = 
                newVal.equals("Colonel") || 
                newVal.equals("Lieutenant-colonel") || 
                newVal.equals("Commandant") || 
                newVal.equals("Capitaine") || 
                newVal.equals("Lieutenant");
            
            echelonGradeFields.setVisible(needsEchelon);
            echelonGradeFields.setManaged(needsEchelon);
        });
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Grade
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String rang = rangField.getValue();
                String statut = statutField.getValue();
                if (rang == null || statut == null) return null;
                
                boolean needsEchelon = 
                    rang.equals("Colonel") || 
                    rang.equals("Lieutenant-colonel") || 
                    rang.equals("Commandant") || 
                    rang.equals("Capitaine") || 
                    rang.equals("Lieutenant");
                
                return new Grade(
                    rang,
                    echelonField.getValue(),
                    datePicker.getValue(),
                    referenceField.getText(),
                    needsEchelon ? String.valueOf(echelonGradeSpinner.getValue()) : "1",
                    needsEchelon ? referenceEchelonField.getText() : "",
                    needsEchelon ? dateEchelonPicker.getValue() : LocalDate.now(),
                    statut
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Grade> result = dialog.showAndWait();
        
        // Ajouter le nouveau grade au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(grade -> {
            gradesTable.getItems().add(grade);
            
            // Mettre à jour le grade actuel si ce grade est le plus récent
            Grade currentGrade = findMostRecentGrade();
            if (currentGrade == null || (grade.getDate() != null && grade.getDate().isAfter(currentGrade.getDate()))) {
                // Mettre à jour le grade actuel
                Map<String, Object> gradeActuelData = formData.getOrDefault("grade_actuel", new HashMap<>());
                gradeActuelData.put("grade", grade);
                formData.put("grade_actuel", gradeActuelData);
            }
        });
    }
    
    /**
     * Retourne la liste des rangs militaires.
     * 
     * @return Tableau des rangs militaires
     */
    private String[] getGradeRanks() {
        return new String[]{
            "Général d'Armée", "Général de Corps d'Armée", "Général de Division Aérienne",
            "Général de Brigade Aérienne", "Colonel", "Lieutenant-colonel", 
            "Commandant", "Capitaine", "Lieutenant", "Sous-lieutenant", 
            "Adjudant-chef-major", "Adjudant-chef", "Adjudant", 
            "Sergent-chef", "Sergent", "Caporal-chef", 
            "Caporal", "Soldat de 1ère classe", "Soldat de 2e classe"
        };
    }
    
    /**
     * Classe pour représenter un grade militaire.
     */
    public static class Grade {
    	private final SimpleStringProperty rang;
        private final SimpleStringProperty echelon;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty reference;
        private final SimpleStringProperty echelonGrade;
        private final SimpleStringProperty referenceEchelon;
        private final SimpleObjectProperty<LocalDate> dateEchelon;
        private final SimpleStringProperty statut; // Nouveau champ
        
        public Grade(String rang, String echelon, LocalDate date, String reference) {
            this.rang = new SimpleStringProperty(rang);
            this.echelon = new SimpleStringProperty(echelon);
            this.date = new SimpleObjectProperty<>(date);
            this.reference = new SimpleStringProperty(reference);
            this.echelonGrade = new SimpleStringProperty("1");
            this.referenceEchelon = new SimpleStringProperty("");
            this.dateEchelon = new SimpleObjectProperty<>(LocalDate.now());
            this.statut = new SimpleStringProperty("Officier Active"); // Valeur par défaut
        }
        
        // Constructeur complet avec statut
        public Grade(String rang, String echelon, LocalDate date, String reference, 
                    String echelonGrade, String referenceEchelon, LocalDate dateEchelon, String statut) {
            this.rang = new SimpleStringProperty(rang);
            this.echelon = new SimpleStringProperty(echelon);
            this.date = new SimpleObjectProperty<>(date);
            this.reference = new SimpleStringProperty(reference);
            this.echelonGrade = new SimpleStringProperty(echelonGrade);
            this.referenceEchelon = new SimpleStringProperty(referenceEchelon);
            this.dateEchelon = new SimpleObjectProperty<>(dateEchelon);
            this.statut = new SimpleStringProperty(statut);
        }
        
        // Getters existants
        public String getRang() { return rang.get(); }
        public String getEchelon() { return echelon.get(); }
        public LocalDate getDate() { return date.get(); }
        public String getReference() { return reference.get(); }
        public String getEchelonGrade() { return echelonGrade.get(); }
        public String getReferenceEchelon() { return referenceEchelon.get(); }
        public LocalDate getDateEchelon() { return dateEchelon.get(); }
        public String getStatut() { return statut.get(); }
        
        // Setters existants
        public void setEchelon(String echelon) { this.echelon.set(echelon); }
        public void setDate(LocalDate date) { this.date.set(date); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setEchelonGrade(String echelonGrade) { this.echelonGrade.set(echelonGrade); }
        public void setReferenceEchelon(String referenceEchelon) { this.referenceEchelon.set(referenceEchelon); }
        public void setDateEchelon(LocalDate dateEchelon) { this.dateEchelon.set(dateEchelon); }
        public void setStatut(String statut) { this.statut.set(statut); }
        
        // Propriétés pour le binding
        public SimpleStringProperty echelonGradeProperty() { return echelonGrade; }
        public SimpleStringProperty referenceEchelonProperty() { return referenceEchelon; }
        public SimpleObjectProperty<LocalDate> dateEchelonProperty() { return dateEchelon; }
        public SimpleStringProperty statutProperty() { return statut; }
    }
    
// ******************** Formation et Poste ********************
    
    /**
     * Affiche le formulaire de gestion des formations et postes.
     */
    @FXML
    private void showFormationInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Formation et Poste");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création d'un onglet pour la formation actuelle et l'historique des postes
        TabPane tabPane = new TabPane();
        
        // Onglet pour la formation actuelle
        Tab currentFormationTab = new Tab("Formation Actuelle");
        currentFormationTab.setClosable(false);
        currentFormationTab.setContent(createCurrentFormationContent());
        
        // Onglet pour l'historique des postes
        Tab postsHistoryTab = new Tab("Historique des Postes");
        postsHistoryTab.setClosable(false);
        postsHistoryTab.setContent(createPostsHistoryContent());
        
        tabPane.getTabs().addAll(currentFormationTab, postsHistoryTab);
        
        container.getChildren().add(tabPane);
        
     // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Crée le contenu pour l'onglet de formation actuelle.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createCurrentFormationContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher la formation actuelle
        TableView<FormationActuelle> currentFormationTable = new TableView<>();
        currentFormationTable.setPrefWidth(Double.MAX_VALUE);
        currentFormationTable.setMaxWidth(Double.MAX_VALUE);
        currentFormationTable.setMinHeight(300);
        currentFormationTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<FormationActuelle, String> formationCol = new TableColumn<>("Formation");
        formationCol.setCellValueFactory(new PropertyValueFactory<>("formation"));
        
        TableColumn<FormationActuelle, LocalDate> dateAffectationCol = new TableColumn<>("Date d'affectation");
        dateAffectationCol.setCellValueFactory(new PropertyValueFactory<>("dateAffectation"));
        
        TableColumn<FormationActuelle, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<FormationActuelle, String> uniteCol = new TableColumn<>("Unité");
        uniteCol.setCellValueFactory(new PropertyValueFactory<>("unite"));
        
        TableColumn<FormationActuelle, String> posteCol = new TableColumn<>("Poste");
        posteCol.setCellValueFactory(new PropertyValueFactory<>("poste"));
        
        TableColumn<FormationActuelle, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<FormationActuelle, Button>() {
                private final Button editButton = new Button("Modifier");
                
                {
                    editButton.setOnAction(event -> {
                        FormationActuelle formation = getTableView().getItems().get(getIndex());
                        showFormationActuelleEditDialog(formation, currentFormationTable);
                    });
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editButton);
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        currentFormationTable.getColumns().addAll(formationCol, dateAffectationCol, referenceCol, uniteCol, posteCol, actionCol);
        
        // Configuration des largeurs
        formationCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.20));
        dateAffectationCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.15));
        referenceCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.20));
        uniteCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.15));
        posteCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.20));
        actionCol.prefWidthProperty().bind(currentFormationTable.widthProperty().multiply(0.10));
        
        // Créer un bouton pour ajouter une formation actuelle
        Button addCurrentFormationButton = new Button("Ajouter formation actuelle");
        addCurrentFormationButton.setMaxWidth(Double.MAX_VALUE);
        addCurrentFormationButton.setAlignment(Pos.CENTER);
        
        // Obtenir la formation actuelle s'il existe
        if (formData.containsKey("formation_actuelle")) {
            Map<String, Object> formationData = formData.get("formation_actuelle");
            if (formationData.containsKey("formation_obj")) {
                FormationActuelle currentFormation = (FormationActuelle) formationData.get("formation_obj");
                currentFormationTable.getItems().add(currentFormation);
            }
        }
        
        // Le bouton n'est visible que si le tableau est vide
        addCurrentFormationButton.setVisible(currentFormationTable.getItems().isEmpty());
        addCurrentFormationButton.setManaged(currentFormationTable.getItems().isEmpty());
        
        // Ajouter un listener pour mettre à jour la visibilité du bouton
        currentFormationTable.getItems().addListener((ListChangeListener<FormationActuelle>) c -> {
            boolean tableIsEmpty = currentFormationTable.getItems().isEmpty();
            addCurrentFormationButton.setVisible(tableIsEmpty);
            addCurrentFormationButton.setManaged(tableIsEmpty);
        });
        
        // Configurer l'action du bouton d'ajout
        addCurrentFormationButton.setOnAction(e -> {
            showFormationActuelleAddDialog(currentFormationTable);
        });
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> formationData = formData.getOrDefault("formation_actuelle", new HashMap<>());
            
            // Stocker la formation actuelle
            formationData.put("matricule", currentMatricule);
            if (!currentFormationTable.getItems().isEmpty()) {
                FormationActuelle formation = currentFormationTable.getItems().get(0);
                formationData.put("formation_obj", formation);
                // Conserver les anciennes clés pour compatibilité
                formationData.put("formation", formation.getFormation());
                formationData.put("date_affectation", formation.getDateAffectation());
                formationData.put("reference_affectation", formation.getReference());
                formationData.put("unite", formation.getUnite());
                formationData.put("poste", formation.getPoste());
                
                // AJOUT AUTOMATIQUE À L'HISTORIQUE
                addFormationToHistoryWhenSaving(formation);
            }
            
            // Mettre à jour la map globale
            formData.put("formation_actuelle", formationData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations de formation actuelle ont été enregistrées et ajoutées à l'historique.");
        });
        
        // Ajouter les composants à la vue
        content.getChildren().add(addCurrentFormationButton);
        content.getChildren().add(currentFormationTable);
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Ajoute automatiquement une formation à l'historique lors de l'enregistrement.
     */
    private void addFormationToHistoryWhenSaving(FormationActuelle formation) {
        // Récupérer ou créer l'historique des formations
        Map<String, Object> historiqueFormationsData = formData.getOrDefault("historique_postes", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<PosteHistorique> postes = (List<PosteHistorique>) historiqueFormationsData.getOrDefault("postes", new ArrayList<>());
        
        // Convertir FormationActuelle en PosteHistorique
        PosteHistorique historiquePoste = new PosteHistorique(
            formation.getFormation(),
            formation.getUnite(),
            formation.getDateAffectation(),
            LocalDate.now(), // Date de fin = aujourd'hui (peut être modifiée plus tard)
            formation.getPoste()
        );
        
        // Vérifier si cette formation exacte n'existe pas déjà dans l'historique
        boolean formationExists = postes.stream().anyMatch(p -> 
            p.getFormation().equals(formation.getFormation()) && 
            p.getUnite().equals(formation.getUnite()) &&
            p.getDateDebut() != null && formation.getDateAffectation() != null &&
            p.getDateDebut().equals(formation.getDateAffectation()) &&
            p.getPoste().equals(formation.getPoste())
        );
        
        if (!formationExists) {
            // Ajouter la nouvelle formation en premier (la plus récente)
            postes.add(0, historiquePoste);
            historiqueFormationsData.put("postes", postes);
            historiqueFormationsData.put("matricule", currentMatricule);
            formData.put("historique_postes", historiqueFormationsData);
            
            System.out.println("Formation ajoutée à l'historique: " + formation.getFormation() + " - " + formation.getUnite());
        }
    }


    
    /**
     * Affiche une boîte de dialogue pour ajouter une formation actuelle.
     */
    private void showFormationActuelleAddDialog(TableView<FormationActuelle> formationTable) {
        Dialog<FormationActuelle> dialog = new Dialog<>();
        dialog.setTitle("Ajouter Formation Actuelle");
        dialog.setHeaderText("Renseignez les informations de la formation actuelle");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        DatePicker dateAffectationField = new DatePicker(LocalDate.now());
        TextField referenceField = new TextField();
        ComboBox<String> uniteField = createComboBox("GMT", "GMX", "GMO", "BAFUSAIR", "ECMAA", "Compagnie EMAA");
        TextField posteField = new TextField();
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Formation", formationField),
            createFormField("Date d'affectation", dateAffectationField),
            createFormField("Référence", referenceField),
            createFormField("Unité", uniteField),
            createFormField("Poste", posteField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet FormationActuelle
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new FormationActuelle(
                    formationField.getValue(),
                    dateAffectationField.getValue(),
                    referenceField.getText(),
                    uniteField.getValue(),
                    posteField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<FormationActuelle> result = dialog.showAndWait();
        
        // Ajouter la nouvelle formation au tableau
        result.ifPresent(formation -> {
            // Vider le tableau et ajouter la nouvelle formation
            formationTable.getItems().clear();
            formationTable.getItems().add(formation);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une formation actuelle.
     */
    private void showFormationActuelleEditDialog(FormationActuelle formation, TableView<FormationActuelle> formationTable) {
        Dialog<FormationActuelle> dialog = new Dialog<>();
        dialog.setTitle("Modifier Formation Actuelle");
        dialog.setHeaderText("Modifier les informations de la formation actuelle");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire avec les valeurs existantes
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        formationField.setValue(formation.getFormation());
        
        DatePicker dateAffectationField = new DatePicker(formation.getDateAffectation());
        TextField referenceField = new TextField(formation.getReference());
        
        ComboBox<String> uniteField = createComboBox("GMT", "GMX", "GMO", "BAFUSAIR", "ECMAA", "Compagnie EMAA");
        uniteField.setValue(formation.getUnite());
        
        TextField posteField = new TextField(formation.getPoste());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Formation", formationField),
            createFormField("Date d'affectation", dateAffectationField),
            createFormField("Référence", referenceField),
            createFormField("Unité", uniteField),
            createFormField("Poste", posteField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet FormationActuelle
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new FormationActuelle(
                    formationField.getValue(),
                    dateAffectationField.getValue(),
                    referenceField.getText(),
                    uniteField.getValue(),
                    posteField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<FormationActuelle> result = dialog.showAndWait();
        
        // Mettre à jour la formation
        result.ifPresent(updatedFormation -> {
            // Sauvegarder l'ancienne formation dans l'historique avant la modification
            addFormationToHistory(formation);
            
            // Mettre à jour la formation actuelle
            int index = formationTable.getItems().indexOf(formation);
            formationTable.getItems().set(index, updatedFormation);
        });
    }

    /**
     * Ajoute une formation à l'historique automatiquement.
     */
    private void addFormationToHistory(FormationActuelle formation) {
        // Récupérer ou créer l'historique des formations
        Map<String, Object> historiqueFormationsData = formData.getOrDefault("historique_formations", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<FormationHistorique> formations = (List<FormationHistorique>) historiqueFormationsData.getOrDefault("formations", new ArrayList<>());
        
        // Convertir FormationActuelle en FormationHistorique
        FormationHistorique historiqueFormation = new FormationHistorique(
            formation.getFormation(),
            formation.getUnite(),
            formation.getDateAffectation(),
            LocalDate.now(), // Date de fin = aujourd'hui
            formation.getPoste()
        );
        
        // Ajouter la formation à l'historique si elle n'y est pas déjà
        boolean formationExists = formations.stream().anyMatch(f -> 
            f.getFormation().equals(formation.getFormation()) && 
            f.getDateDebut() != null && formation.getDateAffectation() != null &&
            f.getDateDebut().equals(formation.getDateAffectation())
        );
        
        if (!formationExists) {
            formations.add(historiqueFormation);
            historiqueFormationsData.put("formations", formations);
            historiqueFormationsData.put("matricule", currentMatricule);
            formData.put("historique_formations", historiqueFormationsData);
        }
    }
    
    /**
     * Crée le contenu pour l'onglet d'historique des postes.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createPostsHistoryContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setPrefWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher l'historique des postes
        TableView<PosteHistorique> postesTable = new TableView<>();
        postesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        postesTable.setPrefWidth(Double.MAX_VALUE);
        postesTable.setPrefWidth(Double.MAX_VALUE);
        postesTable.setMinHeight(300);
        
        // Colonnes du tableau
        TableColumn<PosteHistorique, String> formationCol = new TableColumn<>("Formation");
        formationCol.setCellValueFactory(new PropertyValueFactory<>("formation"));
        
        TableColumn<PosteHistorique, String> uniteCol = new TableColumn<>("Unité");
        uniteCol.setCellValueFactory(new PropertyValueFactory<>("unite"));
        
        TableColumn<PosteHistorique, LocalDate> dateDebutCol = new TableColumn<>("Date de début");
        dateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        
        TableColumn<PosteHistorique, LocalDate> dateFinCol = new TableColumn<>("Date de fin");
        dateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        
        TableColumn<PosteHistorique, String> posteCol = new TableColumn<>("Poste occupé");
        posteCol.setCellValueFactory(new PropertyValueFactory<>("poste"));
        
        TableColumn<PosteHistorique, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<PosteHistorique, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        PosteHistorique poste = getTableView().getItems().get(getIndex());
                        showPosteHistoriqueEditDialog(poste, postesTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        PosteHistorique poste = getTableView().getItems().get(getIndex());
                        postesTable.getItems().remove(poste);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        
        // Ajouter les colonnes au tableau
        postesTable.getColumns().addAll(formationCol, uniteCol, dateDebutCol, dateFinCol, posteCol, actionCol);

        // Configurer les largeurs des colonnes pour qu'elles prennent proportionnellement la largeur
        formationCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.20));
        uniteCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.15));
        dateDebutCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.15));
        dateFinCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.15));
        posteCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.20));
        actionCol.prefWidthProperty().bind(postesTable.widthProperty().multiply(0.15));
        
        // Ajouter un bouton pour ajouter un nouveau poste
        Button addButton = new Button("Ajouter un Poste");
   
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER); // Centrer le texte dans le bouton
        addButton.setOnAction(e -> showPosteHistoriqueAddDialog(postesTable));
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> historyData = formData.getOrDefault("historique_postes", new HashMap<>());
            
            // Stocker les valeurs
            historyData.put("matricule", currentMatricule);
            historyData.put("postes", postesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("historique_postes", historyData);
            
            // Afficher une confirmation
            showSuccessMessage("L'historique des postes a été enregistré.");
        });
        
        content.getChildren().addAll(addButton, postesTable, saveButton);
        
        VBox.setVgrow(postesTable, Priority.ALWAYS);
        
        return content;
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter un nouveau poste à l'historique.
     * 
     * @param postesTable Le tableau des postes
     */
    private void showPosteHistoriqueAddDialog(TableView<PosteHistorique> postesTable) {
        Dialog<PosteHistorique> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Poste");
        dialog.setHeaderText("Ajouter un poste à l'historique");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        ComboBox<String> uniteField = createComboBox("GMT", "GMX", "GMO", "BAFUSAIR", "ECMAA", "Compagnie EMAA");
        DatePicker dateDebutField = new DatePicker();
        DatePicker dateFinField = new DatePicker();
        TextField posteField = new TextField();
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Formation", formationField),
            createFormField("Unité", uniteField),
            createFormField("Date de début", dateDebutField),
            createFormField("Date de fin", dateFinField),
            createFormField("Poste occupé", posteField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet PosteHistorique
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new PosteHistorique(
                    formationField.getValue(),
                    uniteField.getValue(),
                    dateDebutField.getValue(),
                    dateFinField.getValue(),
                    posteField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<PosteHistorique> result = dialog.showAndWait();
        
        // Ajouter le nouveau poste au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(poste -> {
            postesTable.getItems().add(poste);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier un poste de l'historique.
     * 
     * @param poste Le poste à modifier
     * @param postesTable Le tableau des postes
     */
    private void showPosteHistoriqueEditDialog(PosteHistorique poste, TableView<PosteHistorique> postesTable) {
        Dialog<PosteHistorique> dialog = new Dialog<>();
        dialog.setTitle("Modifier un Poste");
        dialog.setHeaderText("Modifier les informations du poste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        formationField.setValue(poste.getFormation());
        
        ComboBox<String> uniteField = createComboBox("GMT", "GMX", "GMO", "BAFUSAIR", "ECMAA", "Compagnie EMAA");
        uniteField.setValue(poste.getUnite());
        
        DatePicker dateDebutField = new DatePicker(poste.getDateDebut());
        DatePicker dateFinField = new DatePicker(poste.getDateFin());
        
        TextField posteField = new TextField(poste.getPoste());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Formation", formationField),
            createFormField("Unité", uniteField),
            createFormField("Date de début", dateDebutField),
            createFormField("Date de fin", dateFinField),
            createFormField("Poste occupé", posteField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet PosteHistorique
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new PosteHistorique(
                    formationField.getValue(),
                    uniteField.getValue(),
                    dateDebutField.getValue(),
                    dateFinField.getValue(),
                    posteField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<PosteHistorique> result = dialog.showAndWait();
        
        // Mettre à jour le poste si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedPoste -> {
            int index = postesTable.getItems().indexOf(poste);
            postesTable.getItems().set(index, updatedPoste);
        });
    }
    
    /**
     * Classe pour représenter une formation actuelle.
     */
    public static class FormationActuelle {
        private final SimpleStringProperty formation;
        private final SimpleObjectProperty<LocalDate> dateAffectation;
        private final SimpleStringProperty reference;
        private final SimpleStringProperty unite;
        private final SimpleStringProperty poste;
        
        public FormationActuelle(String formation, LocalDate dateAffectation, String reference, String unite, String poste) {
            this.formation = new SimpleStringProperty(formation);
            this.dateAffectation = new SimpleObjectProperty<>(dateAffectation);
            this.reference = new SimpleStringProperty(reference);
            this.unite = new SimpleStringProperty(unite);
            this.poste = new SimpleStringProperty(poste);
        }
        
        public String getFormation() { return formation.get(); }
        public LocalDate getDateAffectation() { return dateAffectation.get(); }
        public String getReference() { return reference.get(); }
        public String getUnite() { return unite.get(); }
        public String getPoste() { return poste.get(); }
        
        public void setFormation(String formation) { this.formation.set(formation); }
        public void setDateAffectation(LocalDate dateAffectation) { this.dateAffectation.set(dateAffectation); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setUnite(String unite) { this.unite.set(unite); }
        public void setPoste(String poste) { this.poste.set(poste); }
    }
    
    /**
     * Classe pour représenter une formation dans l'historique.
     */
    public static class FormationHistorique {
        private final SimpleStringProperty formation;
        private final SimpleStringProperty unite;
        private final SimpleObjectProperty<LocalDate> dateDebut;
        private final SimpleObjectProperty<LocalDate> dateFin;
        private final SimpleStringProperty poste;
        
        public FormationHistorique(String formation, String unite, LocalDate dateDebut, LocalDate dateFin, String poste) {
            this.formation = new SimpleStringProperty(formation);
            this.unite = new SimpleStringProperty(unite);
            this.dateDebut = new SimpleObjectProperty<>(dateDebut);
            this.dateFin = new SimpleObjectProperty<>(dateFin);
            this.poste = new SimpleStringProperty(poste);
        }
        
        public String getFormation() { return formation.get(); }
        public String getUnite() { return unite.get(); }
        public LocalDate getDateDebut() { return dateDebut.get(); }
        public LocalDate getDateFin() { return dateFin.get(); }
        public String getPoste() { return poste.get(); }
        
        public void setFormation(String formation) { this.formation.set(formation); }
        public void setUnite(String unite) { this.unite.set(unite); }
        public void setDateDebut(LocalDate dateDebut) { this.dateDebut.set(dateDebut); }
        public void setDateFin(LocalDate dateFin) { this.dateFin.set(dateFin); }
        public void setPoste(String poste) { this.poste.set(poste); }
    }
    
    /**
     * Classe pour représenter un poste dans l'historique.
     */
    public static class PosteHistorique {
        private final SimpleStringProperty formation;
        private final SimpleStringProperty unite;
        private final SimpleObjectProperty<LocalDate> dateDebut;
        private final SimpleObjectProperty<LocalDate> dateFin;
        private final SimpleStringProperty poste;
        
        public PosteHistorique(String formation, String unite, LocalDate dateDebut, LocalDate dateFin, String poste) {
            this.formation = new SimpleStringProperty(formation);
            this.unite = new SimpleStringProperty(unite);
            this.dateDebut = new SimpleObjectProperty<>(dateDebut);
            this.dateFin = new SimpleObjectProperty<>(dateFin);
            this.poste = new SimpleStringProperty(poste);
        }
        
        public String getFormation() { return formation.get(); }
        public String getUnite() { return unite.get(); }
        public LocalDate getDateDebut() { return dateDebut.get(); }
        public LocalDate getDateFin() { return dateFin.get(); }
        public String getPoste() { return poste.get(); }
        
        public void setFormation(String formation) { this.formation.set(formation); }
        public void setUnite(String unite) { this.unite.set(unite); }
        public void setDateDebut(LocalDate dateDebut) { this.dateDebut.set(dateDebut); }
        public void setDateFin(LocalDate dateFin) { this.dateFin.set(dateFin); }
        public void setPoste(String poste) { this.poste.set(poste); }
    }
    
    // ******************** Écoles et Diplômes ********************
    
    /**
     * Affiche le formulaire de gestion des écoles et diplômes.
     */
    @FXML
    private void showEcolesEtDiplomesInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Écoles et Diplômes");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création des onglets pour les différentes catégories d'écoles
        TabPane tabPane = new TabPane();
        
        // Onglet pour l'école de formation initiale
        Tab formationInitialeTab = new Tab("École de Formation Initiale");
        formationInitialeTab.setClosable(false);
        formationInitialeTab.setContent(createFormationInitialeContent());
        
        // Onglet pour l'école civile
        Tab ecoleCivileTab = new Tab("École Civile");
        ecoleCivileTab.setClosable(false);
        ecoleCivileTab.setContent(createEcoleCivileContent());
        
        // Onglet pour les écoles militaires
        Tab ecolesMilitairesTab = new Tab("Écoles et Stages Militaires");
        ecolesMilitairesTab.setClosable(false);
        ecolesMilitairesTab.setContent(createEcolesMilitairesContent());
        
        tabPane.getTabs().addAll(formationInitialeTab, ecoleCivileTab, ecolesMilitairesTab);
        
        container.getChildren().add(tabPane);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Crée le contenu pour l'onglet d'école de formation initiale.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createFormationInitialeContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        
        // Champs du formulaire d'école de formation initiale
        TextField nomEcoleField = new TextField();
        ComboBox<String> paysField = createComboBox(
        		"Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Angola", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine", "Arménie", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn", "Bangladesh", "Barbade", "Belgique", "Belize", "Bénin", "Bhoutan", "Biélorussie", "Birmanie", "Bolivie", "Bosnie-Herzégovine", "Botswana", "Brésil", "Brunei", "Bulgarie", "Burkina Faso", "Burundi", "Cambodge", "Cameroun", "Canada", "Cap-Vert", "Chili", "Chine", "Chypre", "Colombie", "Comores", "Congo", "Corée du Nord", "Corée du Sud", "Costa Rica", "Côte d'Ivoire", "Croatie", "Cuba", "Danemark", "Djibouti", "Dominique", "Égypte", "Émirats arabes unis", "Équateur", "Érythrée", "Espagne", "Estonie", "Eswatini", "États-Unis", "Éthiopie", "Fidji", "Finlande", "France", "Gabon", "Gambie", "Géorgie", "Ghana", "Grèce", "Grenade", "Guatemala", "Guinée", "Guinée-Bissau", "Guinée équatoriale", "Guyana", "Haïti", "Honduras", "Hongrie", "Îles Marshall", "Îles Salomon", "Inde", "Indonésie", "Irak", "Iran", "Irlande", "Islande", "Israël", "Italie", "Jamaïque", "Japon", "Jordanie", "Kazakhstan", "Kenya", "Kirghizistan", "Kiribati", "Koweït", "Laos", "Lesotho", "Lettonie", "Liban", "Liberia", "Libye", "Liechtenstein", "Lituanie", "Luxembourg", "Macédoine du Nord", "Madagascar", "Malaisie", "Malawi", "Maldives", "Mali", "Malte", "Maroc", "Maurice", "Mauritanie", "Mexique", "Micronésie", "Moldavie", "Monaco", "Mongolie", "Monténégro", "Mozambique", "Namibie", "Nauru", "Népal", "Nicaragua", "Niger", "Nigeria", "Norvège", "Nouvelle-Zélande", "Oman", "Ouganda", "Ouzbékistan", "Pakistan", "Palaos", "Palestine", "Panama", "Papouasie-Nouvelle-Guinée", "Paraguay", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Portugal", "Qatar", "République centrafricaine", "République démocratique du Congo", "République dominicaine", "République tchèque", "Roumanie", "Royaume-Uni", "Russie", "Rwanda", "Saint-Christophe-et-Niévès", "Sainte-Lucie", "Saint-Marin", "Saint-Vincent-et-les-Grenadines", "Salvador", "Samoa", "São Tomé-et-Principe", "Sénégal", "Serbie", "Seychelles", "Sierra Leone", "Singapour", "Slovaquie", "Slovénie", "Somalie", "Soudan", "Soudan du Sud", "Sri Lanka", "Suède", "Suisse", "Suriname", "Syrie", "Tadjikistan", "Tanzanie", "Tchad", "Thaïlande", "Timor oriental", "Togo", "Tonga", "Trinité-et-Tobago", "Tunisie", "Turkménistan", "Turquie", "Tuvalu", "Ukraine", "Uruguay", "Vanuatu", "Vatican", "Venezuela", "Viêt Nam", "Yémen", "Zambie", "Zimbabwe"
        );
        TextField regionField = new TextField();
        DatePicker dateEntreeField = new DatePicker();
        DatePicker dateSortieField = new DatePicker();
        Label tempsMisLabel = new Label();
        TextField diplomeObtenuField = new TextField();
        
        // Calculer le temps passé en formation
        dateEntreeField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateEntreeField, dateSortieField, tempsMisLabel);
        });
        
        dateSortieField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateEntreeField, dateSortieField, tempsMisLabel);
        });
        
        content.getChildren().addAll(
            createFormField("Nom de l'école", nomEcoleField),
            createFormField("Pays", paysField),
            createFormField("Région", regionField),
            createFormField("Date d'entrée", dateEntreeField),
            createFormField("Date de sortie", dateSortieField),
            createFormField("Temps mis en formation", tempsMisLabel),
            createFormField("Diplôme obtenu", diplomeObtenuField)
        );
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> ecoleData = formData.getOrDefault("ecole_formation_initiale", new HashMap<>());
            
            // Stocker les valeurs
            ecoleData.put("matricule", currentMatricule);
            ecoleData.put("nom_ecole", nomEcoleField.getText());
            ecoleData.put("pays", paysField.getValue());
            ecoleData.put("region", regionField.getText());
            ecoleData.put("date_entree", dateEntreeField.getValue());
            ecoleData.put("date_sortie", dateSortieField.getValue());
            ecoleData.put("temps_mis", tempsMisLabel.getText());
            ecoleData.put("diplome_obtenu", diplomeObtenuField.getText());
            
            // Mettre à jour la map globale
            formData.put("ecole_formation_initiale", ecoleData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations de l'école de formation initiale ont été enregistrées.");
        });
        
        content.getChildren().add(saveButton);
        
        return new ScrollPane(content);
    }
    
    /**
     * Crée le contenu pour l'onglet d'école civile.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createEcoleCivileContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher les écoles civiles
        TableView<EcoleCivile> ecolesTable = new TableView<>();
        ecolesTable.setPrefWidth(Double.MAX_VALUE);
        ecolesTable.setMaxWidth(Double.MAX_VALUE);
        ecolesTable.setMinHeight(300);
        ecolesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<EcoleCivile, String> nomEcoleCol = new TableColumn<>("Nom de l'école");
        nomEcoleCol.setCellValueFactory(new PropertyValueFactory<>("nomEcole"));
        
        TableColumn<EcoleCivile, String> paysCol = new TableColumn<>("Pays");
        paysCol.setCellValueFactory(new PropertyValueFactory<>("pays"));
        
        TableColumn<EcoleCivile, String> regionCol = new TableColumn<>("Région");
        regionCol.setCellValueFactory(new PropertyValueFactory<>("region"));
        
        TableColumn<EcoleCivile, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<EcoleCivile, String> diplomeCol = new TableColumn<>("Diplôme obtenu");
        diplomeCol.setCellValueFactory(new PropertyValueFactory<>("diplomeObtenu"));
        
        TableColumn<EcoleCivile, String> appreciationCol = new TableColumn<>("Appréciation");
        appreciationCol.setCellValueFactory(new PropertyValueFactory<>("appreciation"));
        
        TableColumn<EcoleCivile, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<EcoleCivile, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        EcoleCivile ecole = getTableView().getItems().get(getIndex());
                        showEcoleCivileEditDialog(ecole, ecolesTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        EcoleCivile ecole = getTableView().getItems().get(getIndex());
                        ecolesTable.getItems().remove(ecole);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        ecolesTable.getColumns().addAll(nomEcoleCol, paysCol, regionCol, referenceCol, diplomeCol, appreciationCol, actionCol);
        
        // Configurer les largeurs des colonnes
        nomEcoleCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.18));
        paysCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.12));
        regionCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.12));
        referenceCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.15));
        diplomeCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.15));
        appreciationCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.13));
        actionCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.15));
        
        // Ajouter un bouton pour ajouter une nouvelle école
        Button addButton = new Button("Ajouter une École Civile");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showEcoleCivileAddDialog(ecolesTable));
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> ecolesData = formData.getOrDefault("ecoles_civiles", new HashMap<>());
            
            // Stocker les valeurs
            ecolesData.put("matricule", currentMatricule);
            ecolesData.put("ecoles", ecolesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("ecoles_civiles", ecolesData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des écoles civiles ont été enregistrées.");
        });
        
        content.getChildren().addAll(addButton, ecolesTable, saveButton);
        
        return content;
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle école civile.
     * 
     * @param ecolesTable Le tableau des écoles civiles
     */
    private void showEcoleCivileAddDialog(TableView<EcoleCivile> ecolesTable) {
        Dialog<EcoleCivile> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une École Civile");
        dialog.setHeaderText("Ajouter une école civile à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField nomEcoleField = new TextField();
        ComboBox<String> paysField = createComboBox(
            "Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Angola", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine", "Arménie", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn", "Bangladesh", "Barbade", "Belgique", "Belize", "Bénin", "Bhoutan", "Biélorussie", "Birmanie", "Bolivie", "Bosnie-Herzégovine", "Botswana", "Brésil", "Brunei", "Bulgarie", "Burkina Faso", "Burundi", "Cambodge", "Cameroun", "Canada", "Cap-Vert", "Chili", "Chine", "Chypre", "Colombie", "Comores", "Congo", "Corée du Nord", "Corée du Sud", "Costa Rica", "Côte d'Ivoire", "Croatie", "Cuba", "Danemark", "Djibouti", "Dominique", "Égypte", "Émirats arabes unis", "Équateur", "Érythrée", "Espagne", "Estonie", "Eswatini", "États-Unis", "Éthiopie", "Fidji", "Finlande", "France", "Gabon", "Gambie", "Géorgie", "Ghana", "Grèce", "Grenade", "Guatemala", "Guinée", "Guinée-Bissau", "Guinée équatoriale", "Guyana", "Haïti", "Honduras", "Hongrie", "Îles Marshall", "Îles Salomon", "Inde", "Indonésie", "Irak", "Iran", "Irlande", "Islande", "Israël", "Italie", "Jamaïque", "Japon", "Jordanie", "Kazakhstan", "Kenya", "Kirghizistan", "Kiribati", "Koweït", "Laos", "Lesotho", "Lettonie", "Liban", "Liberia", "Libye", "Liechtenstein", "Lituanie", "Luxembourg", "Macédoine du Nord", "Madagascar", "Malaisie", "Malawi", "Maldives", "Mali", "Malte", "Maroc", "Maurice", "Mauritanie", "Mexique", "Micronésie", "Moldavie", "Monaco", "Mongolie", "Monténégro", "Mozambique", "Namibie", "Nauru", "Népal", "Nicaragua", "Niger", "Nigeria", "Norvège", "Nouvelle-Zélande", "Oman", "Ouganda", "Ouzbékistan", "Pakistan", "Palaos", "Palestine", "Panama", "Papouasie-Nouvelle-Guinée", "Paraguay", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Portugal", "Qatar", "République centrafricaine", "République démocratique du Congo", "République dominicaine", "République tchèque", "Roumanie", "Royaume-Uni", "Russie", "Rwanda", "Saint-Christophe-et-Niévès", "Sainte-Lucie", "Saint-Marin", "Saint-Vincent-et-les-Grenadines", "Salvador", "Samoa", "São Tomé-et-Principe", "Sénégal", "Serbie", "Seychelles", "Sierra Leone", "Singapour", "Slovaquie", "Slovénie", "Somalie", "Soudan", "Soudan du Sud", "Sri Lanka", "Suède", "Suisse", "Suriname", "Syrie", "Tadjikistan", "Tanzanie", "Tchad", "Thaïlande", "Timor oriental", "Togo", "Tonga", "Trinité-et-Tobago", "Tunisie", "Turkménistan", "Turquie", "Tuvalu", "Ukraine", "Uruguay", "Vanuatu", "Vatican", "Venezuela", "Viêt Nam", "Yémen", "Zambie", "Zimbabwe"
        );
        TextField regionField = new TextField();
        TextField referenceField = new TextField();
        TextField diplomeObtenuField = new TextField();
        ComboBox<String> appreciationField = createComboBox("Excellent", "Très bien", "Bien", "Assez bien", "Passable", "Insuffisant");
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de l'école", nomEcoleField),
            createFormField("Pays", paysField),
            createFormField("Région", regionField),
            createFormField("Référence", referenceField),
            createFormField("Diplôme obtenu", diplomeObtenuField),
            createFormField("Appréciation", appreciationField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet EcoleCivile
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new EcoleCivile(
                    nomEcoleField.getText(),
                    paysField.getValue(),
                    regionField.getText(),
                    referenceField.getText(),
                    diplomeObtenuField.getText(),
                    appreciationField.getValue()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<EcoleCivile> result = dialog.showAndWait();
        
        // Ajouter la nouvelle école au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(ecole -> {
            ecolesTable.getItems().add(ecole);
        });
    }

    
    /**
     * Affiche une boîte de dialogue pour modifier une école civile.
     * 
     * @param ecole L'école à modifier
     * @param ecolesTable Le tableau des écoles civiles
     */
    private void showEcoleCivileEditDialog(EcoleCivile ecole, TableView<EcoleCivile> ecolesTable) {
        Dialog<EcoleCivile> dialog = new Dialog<>();
        dialog.setTitle("Modifier une École Civile");
        dialog.setHeaderText("Modifier les informations de l'école civile");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire avec les valeurs existantes
        TextField nomEcoleField = new TextField(ecole.getNomEcole());
        ComboBox<String> paysField = createComboBox(
            "Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Angola", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine", "Arménie", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn", "Bangladesh", "Barbade", "Belgique", "Belize", "Bénin", "Bhoutan", "Biélorussie", "Birmanie", "Bolivie", "Bosnie-Herzégovine", "Botswana", "Brésil", "Brunei", "Bulgarie", "Burkina Faso", "Burundi", "Cambodge", "Cameroun", "Canada", "Cap-Vert", "Chili", "Chine", "Chypre", "Colombie", "Comores", "Congo", "Corée du Nord", "Corée du Sud", "Costa Rica", "Côte d'Ivoire", "Croatie", "Cuba", "Danemark", "Djibouti", "Dominique", "Égypte", "Émirats arabes unis", "Équateur", "Érythrée", "Espagne", "Estonie", "Eswatini", "États-Unis", "Éthiopie", "Fidji", "Finlande", "France", "Gabon", "Gambie", "Géorgie", "Ghana", "Grèce", "Grenade", "Guatemala", "Guinée", "Guinée-Bissau", "Guinée équatoriale", "Guyana", "Haïti", "Honduras", "Hongrie", "Îles Marshall", "Îles Salomon", "Inde", "Indonésie", "Irak", "Iran", "Irlande", "Islande", "Israël", "Italie", "Jamaïque", "Japon", "Jordanie", "Kazakhstan", "Kenya", "Kirghizistan", "Kiribati", "Koweït", "Laos", "Lesotho", "Lettonie", "Liban", "Liberia", "Libye", "Liechtenstein", "Lituanie", "Luxembourg", "Macédoine du Nord", "Madagascar", "Malaisie", "Malawi", "Maldives", "Mali", "Malte", "Maroc", "Maurice", "Mauritanie", "Mexique", "Micronésie", "Moldavie", "Monaco", "Mongolie", "Monténégro", "Mozambique", "Namibie", "Nauru", "Népal", "Nicaragua", "Niger", "Nigeria", "Norvège", "Nouvelle-Zélande", "Oman", "Ouganda", "Ouzbékistan", "Pakistan", "Palaos", "Palestine", "Panama", "Papouasie-Nouvelle-Guinée", "Paraguay", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Portugal", "Qatar", "République centrafricaine", "République démocratique du Congo", "République dominicaine", "République tchèque", "Roumanie", "Royaume-Uni", "Russie", "Rwanda", "Saint-Christophe-et-Niévès", "Sainte-Lucie", "Saint-Marin", "Saint-Vincent-et-les-Grenadines", "Salvador", "Samoa", "São Tomé-et-Principe", "Sénégal", "Serbie", "Seychelles", "Sierra Leone", "Singapour", "Slovaquie", "Slovénie", "Somalie", "Soudan", "Soudan du Sud", "Sri Lanka", "Suède", "Suisse", "Suriname", "Syrie", "Tadjikistan", "Tanzanie", "Tchad", "Thaïlande", "Timor oriental", "Togo", "Tonga", "Trinité-et-Tobago", "Tunisie", "Turkménistan", "Turquie", "Tuvalu", "Ukraine", "Uruguay", "Vanuatu", "Vatican", "Venezuela", "Viêt Nam", "Yémen", "Zambie", "Zimbabwe"
        );
        paysField.setValue(ecole.getPays());
        TextField regionField = new TextField(ecole.getRegion());
        TextField referenceField = new TextField(ecole.getReference());
        TextField diplomeObtenuField = new TextField(ecole.getDiplomeObtenu());
        ComboBox<String> appreciationField = createComboBox("Excellent", "Très bien", "Bien", "Assez bien", "Passable", "Insuffisant");
        appreciationField.setValue(ecole.getAppreciation());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de l'école", nomEcoleField),
            createFormField("Pays", paysField),
            createFormField("Région", regionField),
            createFormField("Référence", referenceField),
            createFormField("Diplôme obtenu", diplomeObtenuField),
            createFormField("Appréciation", appreciationField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet EcoleCivile
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new EcoleCivile(
                    nomEcoleField.getText(),
                    paysField.getValue(),
                    regionField.getText(),
                    referenceField.getText(),
                    diplomeObtenuField.getText(),
                    appreciationField.getValue()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<EcoleCivile> result = dialog.showAndWait();
        
        // Mettre à jour l'école si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedEcole -> {
            int index = ecolesTable.getItems().indexOf(ecole);
            ecolesTable.getItems().set(index, updatedEcole);
        });
    }

    
    /**
     * Crée le contenu pour l'onglet d'écoles et stages militaires.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createEcolesMilitairesContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Spinner pour définir le nombre d'écoles militaires
        Label questionLabel = new Label("Nombre d'écoles militaires et de stages effectués:");
        Spinner<Integer> nombreEcolesSpinner = new Spinner<>(0, 20, 0);
        nombreEcolesSpinner.setEditable(true);
        
        HBox spinnerBox = new HBox(10, questionLabel, nombreEcolesSpinner);
        content.getChildren().add(spinnerBox);
        
        // Tableau pour afficher les écoles militaires
        TableView<EcoleMilitaire> ecolesTable = new TableView<>();
        ecolesTable.setPrefWidth(Double.MAX_VALUE);
        ecolesTable.setMaxWidth(Double.MAX_VALUE);
        ecolesTable.setMinHeight(300);
        ecolesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<EcoleMilitaire, String> nomEcoleCol = new TableColumn<>("Nom de l'école");
        nomEcoleCol.setCellValueFactory(new PropertyValueFactory<>("nomEcole"));
        
        TableColumn<EcoleMilitaire, String> paysCol = new TableColumn<>("Pays");
        paysCol.setCellValueFactory(new PropertyValueFactory<>("pays"));
        
        TableColumn<EcoleMilitaire, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<EcoleMilitaire, LocalDate> dateDebutCol = new TableColumn<>("Date de début");
        dateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        
        TableColumn<EcoleMilitaire, LocalDate> dateFinCol = new TableColumn<>("Date de fin");
        dateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        
        TableColumn<EcoleMilitaire, String> diplomeCol = new TableColumn<>("Diplôme obtenu");
        diplomeCol.setCellValueFactory(new PropertyValueFactory<>("diplomeObtenu"));
        
        TableColumn<EcoleMilitaire, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<EcoleMilitaire, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<EcoleMilitaire, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        EcoleMilitaire ecole = getTableView().getItems().get(getIndex());
                        showEcoleMilitaireEditDialog(ecole, ecolesTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        EcoleMilitaire ecole = getTableView().getItems().get(getIndex());
                        ecolesTable.getItems().remove(ecole);
                    });
                    
                    setGraphic(hbox);
                }
               
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        ecolesTable.getColumns().addAll(nomEcoleCol, paysCol, typeCol, dateDebutCol, dateFinCol, diplomeCol, referenceCol, actionCol);
        
        // Configuration des largeurs
        nomEcoleCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.18));
        paysCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.12));
        typeCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.12));
        dateDebutCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.10));
        dateFinCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.10));
        diplomeCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.13));
        referenceCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.13));
        actionCol.prefWidthProperty().bind(ecolesTable.widthProperty().multiply(0.12));
        
        // Ajouter le tableau à la vue
        content.getChildren().add(ecolesTable);
        
        // Ajouter un bouton pour ajouter une nouvelle école
        Button addButton = new Button("Ajouter une École ou un Stage Militaire");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showEcoleMilitaireAddDialog(ecolesTable));
        content.getChildren().add(addButton);
        
        // Mettre à jour le nombre d'écoles dans le spinner lorsque le tableau change
        ecolesTable.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends EcoleMilitaire> c) -> {
            nombreEcolesSpinner.getValueFactory().setValue(ecolesTable.getItems().size());
        });
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> ecolesData = formData.getOrDefault("ecoles_militaires", new HashMap<>());
            
            // Stocker les valeurs
            ecolesData.put("matricule", currentMatricule);
            ecolesData.put("nombre_ecoles", nombreEcolesSpinner.getValue());
            ecolesData.put("ecoles", ecolesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("ecoles_militaires", ecolesData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des écoles et stages militaires ont été enregistrées.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle école militaire.
     * 
     * @param ecolesTable Le tableau des écoles militaires
     */
    private void showEcoleMilitaireAddDialog(TableView<EcoleMilitaire> ecolesTable) {
        Dialog<EcoleMilitaire> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une École Militaire");
        dialog.setHeaderText("Ajouter une école ou un stage militaire à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField nomEcoleField = new TextField();
        ComboBox<String> paysField = createComboBox(
            "Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Angola", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine", "Arménie", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn", "Bangladesh", "Barbade", "Belgique", "Belize", "Bénin", "Bhoutan", "Biélorussie", "Birmanie", "Bolivie", "Bosnie-Herzégovine", "Botswana", "Brésil", "Brunei", "Bulgarie", "Burkina Faso", "Burundi", "Cambodge", "Cameroun", "Canada", "Cap-Vert", "Chili", "Chine", "Chypre", "Colombie", "Comores", "Congo", "Corée du Nord", "Corée du Sud", "Costa Rica", "Côte d'Ivoire", "Croatie", "Cuba", "Danemark", "Djibouti", "Dominique", "Égypte", "Émirats arabes unis", "Équateur", "Érythrée", "Espagne", "Estonie", "Eswatini", "États-Unis", "Éthiopie", "Fidji", "Finlande", "France", "Gabon", "Gambie", "Géorgie", "Ghana", "Grèce", "Grenade", "Guatemala", "Guinée", "Guinée-Bissau", "Guinée équatoriale", "Guyana", "Haïti", "Honduras", "Hongrie", "Îles Marshall", "Îles Salomon", "Inde", "Indonésie", "Irak", "Iran", "Irlande", "Islande", "Israël", "Italie", "Jamaïque", "Japon", "Jordanie", "Kazakhstan", "Kenya", "Kirghizistan", "Kiribati", "Koweït", "Laos", "Lesotho", "Lettonie", "Liban", "Liberia", "Libye", "Liechtenstein", "Lituanie", "Luxembourg", "Macédoine du Nord", "Madagascar", "Malaisie", "Malawi", "Maldives", "Mali", "Malte", "Maroc", "Maurice", "Mauritanie", "Mexique", "Micronésie", "Moldavie", "Monaco", "Mongolie", "Monténégro", "Mozambique", "Namibie", "Nauru", "Népal", "Nicaragua", "Niger", "Nigeria", "Norvège", "Nouvelle-Zélande", "Oman", "Ouganda", "Ouzbékistan", "Pakistan", "Palaos", "Palestine", "Panama", "Papouasie-Nouvelle-Guinée", "Paraguay", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Portugal", "Qatar", "République centrafricaine", "République démocratique du Congo", "République dominicaine", "République tchèque", "Roumanie", "Royaume-Uni", "Russie", "Rwanda", "Saint-Christophe-et-Niévès", "Sainte-Lucie", "Saint-Marin", "Saint-Vincent-et-les-Grenadines", "Salvador", "Samoa", "São Tomé-et-Principe", "Sénégal", "Serbie", "Seychelles", "Sierra Leone", "Singapour", "Slovaquie", "Slovénie", "Somalie", "Soudan", "Soudan du Sud", "Sri Lanka", "Suède", "Suisse", "Suriname", "Syrie", "Tadjikistan", "Tanzanie", "Tchad", "Thaïlande", "Timor oriental", "Togo", "Tonga", "Trinité-et-Tobago", "Tunisie", "Turkménistan", "Turquie", "Tuvalu", "Ukraine", "Uruguay", "Vanuatu", "Vatican", "Venezuela", "Viêt Nam", "Yémen", "Zambie", "Zimbabwe"
        );
        ComboBox<String> typeField = createComboBox("École Militaire", "Stage Militaire");
        DatePicker dateDebutField = new DatePicker();
        DatePicker dateFinField = new DatePicker();
        Label tempsMisLabel = new Label();
        TextField diplomeObtenuField = new TextField();
        TextField referenceField = new TextField();
        
        // Calculer le temps passé en formation
        dateDebutField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateDebutField, dateFinField, tempsMisLabel);
        });
        
        dateFinField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateDebutField, dateFinField, tempsMisLabel);
        });
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de l'école ou du stage", nomEcoleField),
            createFormField("Pays", paysField),
            createFormField("Type", typeField),
            createFormField("Date de début", dateDebutField),
            createFormField("Date de fin", dateFinField),
            createFormField("Temps mis", tempsMisLabel),
            createFormField("Diplôme obtenu", diplomeObtenuField),
            createFormField("Référence", referenceField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet EcoleMilitaire
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new EcoleMilitaire(
                    nomEcoleField.getText(),
                    paysField.getValue(),
                    typeField.getValue(),
                    dateDebutField.getValue(),
                    dateFinField.getValue(),
                    tempsMisLabel.getText(),
                    diplomeObtenuField.getText(),
                    referenceField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<EcoleMilitaire> result = dialog.showAndWait();
        
        // Ajouter la nouvelle école au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(ecole -> {
            ecolesTable.getItems().add(ecole);
        });
    }

    
    /**
     * Affiche une boîte de dialogue pour modifier une école militaire.
     * 
     * @param ecole L'école à modifier
     * @param ecolesTable Le tableau des écoles militaires
     */
    private void showEcoleMilitaireEditDialog(EcoleMilitaire ecole, TableView<EcoleMilitaire> ecolesTable) {
        Dialog<EcoleMilitaire> dialog = new Dialog<>();
        dialog.setTitle("Modifier une École Militaire");
        dialog.setHeaderText("Modifier les informations de l'école ou du stage militaire");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField nomEcoleField = new TextField(ecole.getNomEcole());
        ComboBox<String> paysField = createComboBox(
        		"Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Angola", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine", "Arménie", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn", "Bangladesh", "Barbade", "Belgique", "Belize", "Bénin", "Bhoutan", "Biélorussie", "Birmanie", "Bolivie", "Bosnie-Herzégovine", "Botswana", "Brésil", "Brunei", "Bulgarie", "Burkina Faso", "Burundi", "Cambodge", "Cameroun", "Canada", "Cap-Vert", "Chili", "Chine", "Chypre", "Colombie", "Comores", "Congo", "Corée du Nord", "Corée du Sud", "Costa Rica", "Côte d'Ivoire", "Croatie", "Cuba", "Danemark", "Djibouti", "Dominique", "Égypte", "Émirats arabes unis", "Équateur", "Érythrée", "Espagne", "Estonie", "Eswatini", "États-Unis", "Éthiopie", "Fidji", "Finlande", "France", "Gabon", "Gambie", "Géorgie", "Ghana", "Grèce", "Grenade", "Guatemala", "Guinée", "Guinée-Bissau", "Guinée équatoriale", "Guyana", "Haïti", "Honduras", "Hongrie", "Îles Marshall", "Îles Salomon", "Inde", "Indonésie", "Irak", "Iran", "Irlande", "Islande", "Israël", "Italie", "Jamaïque", "Japon", "Jordanie", "Kazakhstan", "Kenya", "Kirghizistan", "Kiribati", "Koweït", "Laos", "Lesotho", "Lettonie", "Liban", "Liberia", "Libye", "Liechtenstein", "Lituanie", "Luxembourg", "Macédoine du Nord", "Madagascar", "Malaisie", "Malawi", "Maldives", "Mali", "Malte", "Maroc", "Maurice", "Mauritanie", "Mexique", "Micronésie", "Moldavie", "Monaco", "Mongolie", "Monténégro", "Mozambique", "Namibie", "Nauru", "Népal", "Nicaragua", "Niger", "Nigeria", "Norvège", "Nouvelle-Zélande", "Oman", "Ouganda", "Ouzbékistan", "Pakistan", "Palaos", "Palestine", "Panama", "Papouasie-Nouvelle-Guinée", "Paraguay", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Portugal", "Qatar", "République centrafricaine", "République démocratique du Congo", "République dominicaine", "République tchèque", "Roumanie", "Royaume-Uni", "Russie", "Rwanda", "Saint-Christophe-et-Niévès", "Sainte-Lucie", "Saint-Marin", "Saint-Vincent-et-les-Grenadines", "Salvador", "Samoa", "São Tomé-et-Principe", "Sénégal", "Serbie", "Seychelles", "Sierra Leone", "Singapour", "Slovaquie", "Slovénie", "Somalie", "Soudan", "Soudan du Sud", "Sri Lanka", "Suède", "Suisse", "Suriname", "Syrie", "Tadjikistan", "Tanzanie", "Tchad", "Thaïlande", "Timor oriental", "Togo", "Tonga", "Trinité-et-Tobago", "Tunisie", "Turkménistan", "Turquie", "Tuvalu", "Ukraine", "Uruguay", "Vanuatu", "Vatican", "Venezuela", "Viêt Nam", "Yémen", "Zambie", "Zimbabwe"
        );
        paysField.setValue(ecole.getPays());
        
        ComboBox<String> typeField = createComboBox("École Militaire", "Stage Militaire");
        typeField.setValue(ecole.getType());
        
        DatePicker dateDebutField = new DatePicker(ecole.getDateDebut());
        DatePicker dateFinField = new DatePicker(ecole.getDateFin());
        
        Label tempsMisLabel = new Label(ecole.getTempsMis());
        
        // Recalculer le temps mis quand les dates changent
        dateDebutField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateDebutField, dateFinField, tempsMisLabel);
        });
        
        dateFinField.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTempsMis(dateDebutField, dateFinField, tempsMisLabel);
        });
        
        TextField diplomeObtenuField = new TextField(ecole.getDiplomeObtenu());
        TextField referenceField = new TextField(ecole.getReference());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de l'école ou du stage", nomEcoleField),
            createFormField("Pays", paysField),
            createFormField("Type", typeField),
            createFormField("Date de début", dateDebutField),
            createFormField("Date de fin", dateFinField),
            createFormField("Temps mis", tempsMisLabel),
            createFormField("Diplôme obtenu", diplomeObtenuField),
            createFormField("Référence", referenceField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet EcoleMilitaire
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new EcoleMilitaire(
                    nomEcoleField.getText(),
                    paysField.getValue(),
                    typeField.getValue(),
                    dateDebutField.getValue(),
                    dateFinField.getValue(),
                    tempsMisLabel.getText(),
                    diplomeObtenuField.getText(),
                    referenceField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<EcoleMilitaire> result = dialog.showAndWait();
        
        // Mettre à jour l'école si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedEcole -> {
            int index = ecolesTable.getItems().indexOf(ecole);
            ecolesTable.getItems().set(index, updatedEcole);
        });
    }
    
    /**
     * Met à jour le label de temps mis en fonction des dates d'entrée et de sortie.
     * 
     * @param dateEntree Le DatePicker pour la date d'entrée
     * @param dateSortie Le DatePicker pour la date de sortie
     * @param tempsMis Le label pour afficher le temps mis
     */
    private void updateTempsMis(DatePicker dateEntree, DatePicker dateSortie, Label tempsMis) {
        LocalDate entree = dateEntree.getValue();
        LocalDate sortie = dateSortie.getValue();
        
        if (entree != null && sortie != null) {
            if (sortie.isAfter(entree)) {
                long mois = ChronoUnit.MONTHS.between(entree, sortie);
                long jours = ChronoUnit.DAYS.between(entree.plusMonths(mois), sortie);
                
                StringBuilder sb = new StringBuilder();
                
                if (mois > 0) {
                    sb.append(mois).append(" mois");
                }
                
                if (jours > 0) {
                    if (sb.length() > 0) {
                        sb.append(" et ");
                    }
                    sb.append(jours).append(" jours");
                }
                
                tempsMis.setText(sb.toString());
            } else {
                tempsMis.setText("Date de sortie doit être postérieure à la date d'entrée");
            }
        } else {
            tempsMis.setText("");
        }
    }
    
    /**
     * Classe pour représenter une école civile.
     */
    public static class EcoleCivile {
        private final SimpleStringProperty nomEcole;
        private final SimpleStringProperty pays;
        private final SimpleStringProperty region;
        private final SimpleStringProperty reference;
        private final SimpleStringProperty diplomeObtenu;
        private final SimpleStringProperty appreciation; // Nouvelle colonne
        
        public EcoleCivile(String nomEcole, String pays, String region, String reference, String diplomeObtenu, String appreciation) {
            this.nomEcole = new SimpleStringProperty(nomEcole);
            this.pays = new SimpleStringProperty(pays);
            this.region = new SimpleStringProperty(region);
            this.reference = new SimpleStringProperty(reference);
            this.diplomeObtenu = new SimpleStringProperty(diplomeObtenu);
            this.appreciation = new SimpleStringProperty(appreciation);
        }
        
        public String getNomEcole() { return nomEcole.get(); }
        public String getPays() { return pays.get(); }
        public String getRegion() { return region.get(); }
        public String getReference() { return reference.get(); }
        public String getDiplomeObtenu() { return diplomeObtenu.get(); }
        public String getAppreciation() { return appreciation.get(); }
        
        public void setNomEcole(String nomEcole) { this.nomEcole.set(nomEcole); }
        public void setPays(String pays) { this.pays.set(pays); }
        public void setRegion(String region) { this.region.set(region); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setDiplomeObtenu(String diplomeObtenu) { this.diplomeObtenu.set(diplomeObtenu); }
        public void setAppreciation(String appreciation) { this.appreciation.set(appreciation); }
    }

    
    /**
     * Classe pour représenter une école ou un stage militaire.
     */
    public static class EcoleMilitaire {
        private final SimpleStringProperty nomEcole;
        private final SimpleStringProperty pays;
        private final SimpleStringProperty type;
        private final SimpleObjectProperty<LocalDate> dateDebut;
        private final SimpleObjectProperty<LocalDate> dateFin;
        private final SimpleStringProperty tempsMis;
        private final SimpleStringProperty diplomeObtenu;
        private final SimpleStringProperty reference; // Nouvelle colonne
        
        public EcoleMilitaire(String nomEcole, String pays, String type, LocalDate dateDebut, 
                             LocalDate dateFin, String tempsMis, String diplomeObtenu, String reference) {
            this.nomEcole = new SimpleStringProperty(nomEcole);
            this.pays = new SimpleStringProperty(pays);
            this.type = new SimpleStringProperty(type);
            this.dateDebut = new SimpleObjectProperty<>(dateDebut);
            this.dateFin = new SimpleObjectProperty<>(dateFin);
            this.tempsMis = new SimpleStringProperty(tempsMis);
            this.diplomeObtenu = new SimpleStringProperty(diplomeObtenu);
            this.reference = new SimpleStringProperty(reference);
        }
        
        public String getNomEcole() { return nomEcole.get(); }
        public String getPays() { return pays.get(); }
        public String getType() { return type.get(); }
        public LocalDate getDateDebut() { return dateDebut.get(); }
        public LocalDate getDateFin() { return dateFin.get(); }
        public String getTempsMis() { return tempsMis.get(); }
        public String getDiplomeObtenu() { return diplomeObtenu.get(); }
        public String getReference() { return reference.get(); }
        
        public void setNomEcole(String nomEcole) { this.nomEcole.set(nomEcole); }
        public void setPays(String pays) { this.pays.set(pays); }
        public void setType(String type) { this.type.set(type); }
        public void setDateDebut(LocalDate dateDebut) { this.dateDebut.set(dateDebut); }
        public void setDateFin(LocalDate dateFin) { this.dateFin.set(dateFin); }
        public void setTempsMis(String tempsMis) { this.tempsMis.set(tempsMis); }
        public void setDiplomeObtenu(String diplomeObtenu) { this.diplomeObtenu.set(diplomeObtenu); }
        public void setReference(String reference) { this.reference.set(reference); }
    }
    
// ******************** Opérations et Déploiements ********************
    
    /**
     * Méthode appelée lorsque l'utilisateur clique sur le bouton "Opérations et Déploiements".
     * Affiche la première sous-section (Opérations Intérieures).
     */
    @FXML
    private void showOperationsInfo() {
        // Cette méthode est liée au bouton et gérée par handleMainButtonClick
        // Elle ne fait rien ici car la logique est dans navigateToSection
    }
    
    /**
     * Affiche le formulaire de gestion des opérations intérieures.
     */
    @FXML
    private void showInteriorOperations() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Opérations Intérieures");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
     // Ajout du champ récapitulatif
        HBox recapBox = new HBox(10);
        Label nombreOpLabel = new Label("Nombre d'opérations intérieures effectuées:");
        Label countLabel = new Label("0");
        countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        recapBox.getChildren().addAll(nombreOpLabel, countLabel);
        container.getChildren().add(recapBox);
        
        // Tableau pour afficher les opérations intérieures
        TableView<Operation> operationsTable = new TableView<>();
        operationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
     // Mettre à jour le compteur lorsque le tableau change
        operationsTable.getItems().addListener((ListChangeListener<? super Operation>) c -> {
            countLabel.setText(String.valueOf(operationsTable.getItems().size()));
        });
        
        // Colonnes du tableau
        TableColumn<Operation, String> nomMissionCol = new TableColumn<>("Nom de la mission");
        nomMissionCol.setCellValueFactory(new PropertyValueFactory<>("nomMission"));
        
        TableColumn<Operation, Integer> anneeCol = new TableColumn<>("Année");
        anneeCol.setCellValueFactory(new PropertyValueFactory<>("annee"));
        
        TableColumn<Operation, String> lieuCol = new TableColumn<>("Lieu");
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        
        TableColumn<Operation, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Operation, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Operation operation = getTableView().getItems().get(getIndex());
                        showOperationEditDialog(operation, operationsTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Operation operation = getTableView().getItems().get(getIndex());
                        operationsTable.getItems().remove(operation);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        operationsTable.getColumns().addAll(nomMissionCol, anneeCol, lieuCol, actionCol);
        
        // Ajouter le tableau à la vue
        container.getChildren().add(operationsTable);
        
        // Ajouter un bouton pour ajouter une nouvelle opération
        Button addButton = new Button("Ajouter une Opération Intérieure");
        addButton.setOnAction(e -> showOperationAddDialog(operationsTable, "Intérieure"));
        container.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> operationsData = formData.getOrDefault("operations_interieures", new HashMap<>());
            
            // Stocker les valeurs
            operationsData.put("matricule", currentMatricule);
            operationsData.put("operations", operationsTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("operations_interieures", operationsData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des opérations intérieures ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche le formulaire de gestion des opérations extérieures.
     */
    @FXML
 // Méthode pour ajouter un spinner de comptage pour les opérations extérieures
    private void showExteriorOperations() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Opérations Extérieures");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Ajout du champ récapitulatif
        HBox recapBox = new HBox(10);
        Label nombreOpLabel = new Label("Nombre d'opérations extérieures effectuées:");
        Label countLabel = new Label("0");
        countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        recapBox.getChildren().addAll(nombreOpLabel, countLabel);
        container.getChildren().add(recapBox);
        
        // Tableau pour afficher les opérations extérieures
        TableView<Operation> operationsTable = new TableView<>();
        operationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Mettre à jour le compteur lorsque le tableau change
        operationsTable.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends Operation> c) -> {
            countLabel.setText(String.valueOf(operationsTable.getItems().size()));
        });
        
        // Colonnes du tableau
        TableColumn<Operation, String> nomMissionCol = new TableColumn<>("Nom de la mission");
        nomMissionCol.setCellValueFactory(new PropertyValueFactory<>("nomMission"));
        nomMissionCol.setPrefWidth(200);
        
        TableColumn<Operation, Integer> anneeCol = new TableColumn<>("Année");
        anneeCol.setCellValueFactory(new PropertyValueFactory<>("annee"));
        anneeCol.setPrefWidth(100);
        
        TableColumn<Operation, String> lieuCol = new TableColumn<>("Lieu");
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        lieuCol.setPrefWidth(150);
        
        TableColumn<Operation, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Operation, Button>() {
                private final Button editButton = createActionButton("Modifier", "edit");
                private final Button deleteButton = createActionButton("Supprimer", "delete");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Operation operation = getTableView().getItems().get(getIndex());
                        showOperationEditDialog(operation, operationsTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Operation operation = getTableView().getItems().get(getIndex());
                        operationsTable.getItems().remove(operation);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        actionCol.setPrefWidth(150);
        
        // Ajouter les colonnes au tableau
        operationsTable.getColumns().addAll(nomMissionCol, anneeCol, lieuCol, actionCol);
        
        // Ajouter le tableau à la vue
        container.getChildren().add(operationsTable);
        
        // Ajouter un bouton pour ajouter une nouvelle opération
        Button addButton = createActionButton("Ajouter une Opération Extérieure", "add");
        addButton.setOnAction(e -> showOperationAddDialog(operationsTable, "Extérieure"));
        container.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = createActionButton("Enregistrer", "");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> operationsData = formData.getOrDefault("operations_exterieures", new HashMap<>());
            
            // Stocker les valeurs
            operationsData.put("matricule", currentMatricule);
            operationsData.put("operations", operationsTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("operations_exterieures", operationsData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des opérations extérieures ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle opération.
     * 
     * @param operationsTable Le tableau des opérations
     * @param type Le type d'opération (Intérieure ou Extérieure)
     */
    private void showOperationAddDialog(TableView<Operation> operationsTable, String type) {
        Dialog<Operation> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Opération " + type);
        dialog.setHeaderText("Ajouter une opération " + type.toLowerCase() + " à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField nomMissionField = new TextField();
        
        // Spinner pour l'année (limité aux années raisonnables)
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1960, LocalDate.now().getYear(), LocalDate.now().getYear());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        // Choix du lieu
        ComboBox<String> lieuField = createComboBox();
        // Adapter les lieux en fonction du type d'opération
        if (type.equals("Intérieure")) {
            lieuField.getItems().addAll("RMIA1", "RMIA2", "RMIA3", "RMIA4", "RMIA5", "RMIA6");
        } else {
            lieuField.getItems().addAll("Afrique", "Amérique du Nord", "Amérique du Sud", "Antarctique", "Asie", "Europe", "Océanie");
        }
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de la mission", nomMissionField),
            createFormField("Année", anneeSpinner),
            createFormField("Lieu", lieuField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Operation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Operation(
                    nomMissionField.getText(),
                    anneeSpinner.getValue(),
                    lieuField.getValue(),
                    type
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Operation> result = dialog.showAndWait();
        
        // Ajouter la nouvelle opération au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(operation -> {
            operationsTable.getItems().add(operation);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une opération.
     * 
     * @param operation L'opération à modifier
     * @param operationsTable Le tableau des opérations
     */
    private void showOperationEditDialog(Operation operation, TableView<Operation> operationsTable) {
        Dialog<Operation> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Opération");
        dialog.setHeaderText("Modifier les informations de l'opération");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField nomMissionField = new TextField(operation.getNomMission());
        
        // Spinner pour l'année
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1960, LocalDate.now().getYear(), operation.getAnnee());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        // Choix du lieu
        ComboBox<String> lieuField = createComboBox();
        // Adapter les lieux en fonction du type d'opération
        if (operation.getType().equals("Intérieure")) {
            lieuField.getItems().addAll("RMIA1", "RMIA2", "RMIA3", "RMIA4", "RMIA5", "RMIA6");
        } else {
            lieuField.getItems().addAll("Afrique", "Amérique du Nord", "Amérique du Sud", "Antarctique", "Asie", "Europe", "Océanie");
        }
        lieuField.setValue(operation.getLieu());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Nom de la mission", nomMissionField),
            createFormField("Année", anneeSpinner),
            createFormField("Lieu", lieuField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Operation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Operation(
                    nomMissionField.getText(),
                    anneeSpinner.getValue(),
                    lieuField.getValue(),
                    operation.getType()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Operation> result = dialog.showAndWait();
        
        // Mettre à jour l'opération si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedOperation -> {
            int index = operationsTable.getItems().indexOf(operation);
            operationsTable.getItems().set(index, updatedOperation);
        });
    }
    
    /**
     * Classe pour représenter une opération militaire.
     */
    public static class Operation {
        private final SimpleStringProperty nomMission;
        private final SimpleObjectProperty<Integer> annee;
        private final SimpleStringProperty lieu;
        private final SimpleStringProperty type; // "Intérieure" ou "Extérieure"
        
        public Operation(String nomMission, Integer annee, String lieu, String type) {
            this.nomMission = new SimpleStringProperty(nomMission);
            this.annee = new SimpleObjectProperty<>(annee);
            this.lieu = new SimpleStringProperty(lieu);
            this.type = new SimpleStringProperty(type);
        }
        
        public String getNomMission() { return nomMission.get(); }
        public Integer getAnnee() { return annee.get(); }
        public String getLieu() { return lieu.get(); }
        public String getType() { return type.get(); }
        
        public void setNomMission(String nomMission) { this.nomMission.set(nomMission); }
        
        public void setAnnee(Integer annee) { this.annee.set(annee); }
        public void setLieu(String lieu) { this.lieu.set(lieu); }
        public void setType(String type) { this.type.set(type); }
    }
    
    // ******************** Décorations ********************
    
    /**
     * Affiche le formulaire de gestion des décorations.
     */
    @FXML
    private void showDecorationInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Décorations");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création des onglets pour les différentes catégories de décorations
        TabPane tabPane = new TabPane();
        
        // Onglet pour les ordres nationaux
        Tab ordresNationauxTab = new Tab("Ordres Nationaux");
        ordresNationauxTab.setClosable(false);
        ordresNationauxTab.setContent(createDecorationTabContent("Ordres Nationaux"));
        
        // Onglet pour les ordres du mérite camerounais
        Tab ordresMeriteTab = new Tab("Ordres du Mérite Camerounais");
        ordresMeriteTab.setClosable(false);
        ordresMeriteTab.setContent(createDecorationTabContent("Ordres du Mérite Camerounais"));
        
        // Onglet pour l'ordre du mérite sportif
        Tab ordreMeriteSportifTab = new Tab("Ordre du Mérite Sportif");
        ordreMeriteSportifTab.setClosable(false);
        ordreMeriteSportifTab.setContent(createDecorationTabContent("Ordre du Mérite Sportif"));
        
        // Onglet pour les médailles
        Tab medaillesTab = new Tab("Médailles");
        medaillesTab.setClosable(false);
        medaillesTab.setContent(createMedaillesTabContent());
        
        tabPane.getTabs().addAll(ordresNationauxTab, ordresMeriteTab, ordreMeriteSportifTab, medaillesTab);
        
        container.getChildren().add(tabPane);
        
        // Ajouter un bouton pour sauvegarder toutes les données
        Button saveAllButton = new Button("Enregistrer Toutes les Décorations");
        saveAllButton.setOnAction(e -> {
            // Sauvegarder les données des décorations
            saveDecorations();
            
            // Afficher une confirmation
            showSuccessMessage("Toutes les informations de décorations ont été enregistrées.");
        });
        container.getChildren().add(saveAllButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Crée le contenu pour un onglet de décoration.
     * 
     * @param decorationType Le type de décoration
     * @return Le contenu de l'onglet
     */
    private Node createDecorationTabContent(String decorationType) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher les décorations
        TableView<Decoration> decorationsTable = new TableView<>();
        decorationsTable.setPrefWidth(Double.MAX_VALUE);
        decorationsTable.setMaxWidth(Double.MAX_VALUE);
        decorationsTable.setMinHeight(300);
        decorationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Stocker la référence du tableau pour l'accès global
        if (decorationType.equals("Ordres Nationaux")) {
            decorationTable = decorationsTable;
        }
        
        // Colonnes du tableau
        TableColumn<Decoration, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        
        TableColumn<Decoration, LocalDate> dateCol = new TableColumn<>("Date de Décoration");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Decoration, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<Decoration, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Decoration, Button>() {
                private final Button editButton = new Button("Modifier");
                
                {
                    editButton.setOnAction(event -> {
                        Decoration decoration = getTableView().getItems().get(getIndex());
                        showDecorationEditDialog(decoration, decorationsTable);
                    });
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editButton);
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        decorationsTable.getColumns().addAll(gradeCol, dateCol, referenceCol, actionCol);
        
        gradeCol.prefWidthProperty().bind(decorationsTable.widthProperty().multiply(0.40));
        dateCol.prefWidthProperty().bind(decorationsTable.widthProperty().multiply(0.20));
        referenceCol.prefWidthProperty().bind(decorationsTable.widthProperty().multiply(0.20));
        actionCol.prefWidthProperty().bind(decorationsTable.widthProperty().multiply(0.20));
        
        // Remplir les données initiales
        decorationsTable.getItems().addAll(getDecorations(decorationType));
        
        // Ajouter le tableau à la vue
        content.getChildren().add(decorationsTable);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> decorationsData = formData.getOrDefault(
                "decorations_" + decorationType.toLowerCase().replace(" ", "_"), 
                new HashMap<>()
            );
            
            // Stocker les valeurs
            decorationsData.put("matricule", currentMatricule);
            decorationsData.put("decorations", decorationsTable.getItems());
            
            // Mettre à jour la map globale
            formData.put(
                "decorations_" + decorationType.toLowerCase().replace(" ", "_"), 
                decorationsData
            );
            
            // Afficher une confirmation
            showSuccessMessage("Les " + decorationType + " ont été enregistrés.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Crée le contenu pour l'onglet des médailles.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createMedaillesTabContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher les médailles
        TableView<Medal> medaillesTable = new TableView<>();
        medaillesTable.setPrefWidth(Double.MAX_VALUE);
        medaillesTable.setMaxWidth(Double.MAX_VALUE);
        medaillesTable.setMinHeight(300);
        medaillesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Medal, String> medalCol = new TableColumn<>("Médaille");
        medalCol.setCellValueFactory(new PropertyValueFactory<>("medal"));
        
        TableColumn<Medal, LocalDate> dateCol = new TableColumn<>("Date de Décoration");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Medal, String> referenceCol = new TableColumn<>("Référence");
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
        
        TableColumn<Medal, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Medal, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Medal medal = getTableView().getItems().get(getIndex());
                        showMedalEditDialog(medal, medaillesTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Medal medal = getTableView().getItems().get(getIndex());
                        medaillesTable.getItems().remove(medal);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        medaillesTable.getColumns().addAll(medalCol, dateCol, referenceCol, actionCol);
        
     // Configurer les largeurs des colonnes pour qu'elles prennent proportionnellement la largeur
        medalCol.prefWidthProperty().bind(medaillesTable.widthProperty().multiply(0.40));
        dateCol.prefWidthProperty().bind(medaillesTable.widthProperty().multiply(0.20));
        referenceCol.prefWidthProperty().bind(medaillesTable.widthProperty().multiply(0.20));
        
        actionCol.prefWidthProperty().bind(medaillesTable.widthProperty().multiply(0.20));
        
        // Remplir les données initiales
        medaillesTable.getItems().addAll(getMedals());
        
        // Ajouter un bouton pour ajouter une nouvelle médaille
        Button addButton = new Button("Ajouter une Médaille");
        addButton.setOnAction(e -> showMedalAddDialog(medaillesTable));
        
        // Ajouter le tableau et le bouton à la vue
        content.getChildren().addAll(addButton, medaillesTable);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> medaillesData = formData.getOrDefault("medailles", new HashMap<>());
            
            // Stocker les valeurs
            medaillesData.put("matricule", currentMatricule);
            medaillesData.put("medailles", medaillesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("medailles", medaillesData);
            
            // Afficher une confirmation
            showSuccessMessage("Les médailles ont été enregistrées.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Sauvegarde toutes les données des décorations.
     */
    private void saveDecorations() {
        // Parcourir les onglets et sauvegarder les données
        for (String type : new String[]{"ordres_nationaux", "ordres_du_merite_camerounais", "ordre_du_merite_sportif", "medailles"}) {
            Map<String, Object> data = formData.getOrDefault("decorations_" + type, new HashMap<>());
            data.put("matricule", currentMatricule);
            formData.put("decorations_" + type, data);
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une décoration.
     * 
     * @param decoration La décoration à modifier
     * @param decorationsTable Le tableau des décorations
     */
    private void showDecorationEditDialog(Decoration decoration, TableView<Decoration> decorationsTable) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Décoration");
        dialog.setHeaderText("Modifier les informations de la décoration: " + decoration.getGrade());
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        DatePicker datePicker = new DatePicker(decoration.getDate());
        TextField referenceField = new TextField(decoration.getReference());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Date", datePicker),
            createFormField("Référence", referenceField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Attendre le résultat de la boîte de dialogue
        Optional<ButtonType> result = dialog.showAndWait();
        
        // Mettre à jour la décoration si l'utilisateur a cliqué sur Enregistrer
        if (result.isPresent() && result.get() == saveButtonType) {
            decoration.setDate(datePicker.getValue());
            decoration.setReference(referenceField.getText());
            
            // Rafraîchir le tableau
            decorationsTable.refresh();
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle médaille.
     * 
     * @param medaillesTable Le tableau des médailles
     */
    private void showMedalAddDialog(TableView<Medal> medaillesTable) {
        Dialog<Medal> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Médaille");
        dialog.setHeaderText("Ajouter une médaille à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField medalField = new TextField();
        DatePicker datePicker = new DatePicker();
        TextField referenceField = new TextField();
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Médaille", medalField),
            createFormField("Date", datePicker),
            createFormField("Référence", referenceField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Medal
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Medal(
                    medalField.getText(),
                    datePicker.getValue(),
                    referenceField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Medal> result = dialog.showAndWait();
        
        // Ajouter la nouvelle médaille au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(medal -> {
            medaillesTable.getItems().add(medal);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une médaille.
     * 
     * @param medal La médaille à modifier
     * @param medaillesTable Le tableau des médailles
     */
    private void showMedalEditDialog(Medal medal, TableView<Medal> medaillesTable) {
        Dialog<Medal> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Médaille");
        dialog.setHeaderText("Modifier les informations de la médaille");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField medalField = new TextField(medal.getMedal());
        DatePicker datePicker = new DatePicker(medal.getDate());
        TextField referenceField = new TextField(medal.getReference());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Médaille", medalField),
            createFormField("Date", datePicker),
            createFormField("Référence", referenceField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Medal
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Medal(
                    medalField.getText(),
                    datePicker.getValue(),
                    referenceField.getText()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Medal> result = dialog.showAndWait();
        
        // Mettre à jour la médaille si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedMedal -> {
            int index = medaillesTable.getItems().indexOf(medal);
            medaillesTable.getItems().set(index, updatedMedal);
        });
    }
    
    /**
     * Retourne la liste des décorations pour un type donné.
     * 
     * @param type Le type de décoration
     * @return La liste des décorations
     */
    private List<Decoration> getDecorations(String type) {
        List<Decoration> decorations = new ArrayList<>();
        
        switch (type) {
            case "Ordres Nationaux":
                decorations.add(new Decoration("Chevalier", LocalDate.now(), ""));
                decorations.add(new Decoration("Officier", LocalDate.now(), ""));
                decorations.add(new Decoration("Commandeur", LocalDate.now(), ""));
                decorations.add(new Decoration("Grand Officier", LocalDate.now(), ""));
                decorations.add(new Decoration("Grand Croix", LocalDate.now(), ""));
                decorations.add(new Decoration("Grand Collier", LocalDate.now(), ""));
                decorations.add(new Decoration("Grand Cordon", LocalDate.now(), ""));
                break;
            case "Ordres du Mérite Camerounais":
                decorations.add(new Decoration("Chevalier", LocalDate.now(), ""));
                decorations.add(new Decoration("Officier", LocalDate.now(), ""));
                decorations.add(new Decoration("Commandeur", LocalDate.now(), ""));
                decorations.add(new Decoration("Grand Cordon", LocalDate.now(), ""));
                break;
            case "Ordre du Mérite Sportif":
                decorations.add(new Decoration("Chevalier", LocalDate.now(), ""));
                decorations.add(new Decoration("Officier", LocalDate.now(), ""));
                decorations.add(new Decoration("Commandeur", LocalDate.now(), ""));
                break;
        }
        
        return decorations;
    }
    
    /**
     * Retourne la liste des médailles.
     * 
     * @return La liste des médailles
     */
    private List<Medal> getMedals() {
        List<Medal> medals = new ArrayList<>();
        medals.add(new Medal("Croix de la Valeur Militaire", LocalDate.now(), ""));
        medals.add(new Medal("Médaille de la Vaillance", LocalDate.now(), ""));
        medals.add(new Medal("Médaille de la Force Publique", LocalDate.now(), ""));
        return medals;
    }
    
    /**
     * Classe pour représenter une décoration.
     */
    public static class Decoration {
        private final SimpleStringProperty grade;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty reference;
        
        public Decoration(String grade, LocalDate date, String reference) {
            this.grade = new SimpleStringProperty(grade);
            this.date = new SimpleObjectProperty<>(date);
            this.reference = new SimpleStringProperty(reference);
        }
        
        public String getGrade() { return grade.get(); }
        public LocalDate getDate() { return date.get(); }
        public String getReference() { return reference.get(); }
        
        public void setDate(LocalDate date) { this.date.set(date); }
        public void setReference(String reference) { this.reference.set(reference); }
    }
    
    /**
     * Classe pour représenter une médaille.
     */
    public static class Medal {
        private final SimpleStringProperty medal;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty reference;
        
        public Medal(String medal, LocalDate date, String reference) {
            this.medal = new SimpleStringProperty(medal);
            this.date = new SimpleObjectProperty<>(date);
            this.reference = new SimpleStringProperty(reference);
        }
        
        public String getMedal() { return medal.get(); }
        public LocalDate getDate() { return date.get(); }
        public String getReference() { return reference.get(); }
        
        public void setMedal(String medal) { this.medal.set(medal); }
        public void setDate(LocalDate date) { this.date.set(date); }
        public void setReference(String reference) { this.reference.set(reference); }
    }
    
// ******************** Punitions ********************
    
    /**
     * Affiche le formulaire de gestion des punitions.
     */
    @FXML
    private void showPunishmentInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        container.setPrefWidth(Double.MAX_VALUE);
        container.setMaxWidth(Double.MAX_VALUE);
        
        Label titleLabel = new Label("Punitions");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Tableau pour afficher les punitions
        TableView<Punishment> punishmentsTable = new TableView<>();
        punishmentsTable.setPrefWidth(Double.MAX_VALUE);
        punishmentsTable.setMaxWidth(Double.MAX_VALUE);
        punishmentsTable.setMinHeight(300);
        punishmentsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Punishment, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("item"));
        
        TableColumn<Punishment, String> motifCol = new TableColumn<>("Motif");
        motifCol.setCellValueFactory(new PropertyValueFactory<>("motif"));
        
        TableColumn<Punishment, String> circumstancesCol = new TableColumn<>("Circonstances");
        circumstancesCol.setCellValueFactory(new PropertyValueFactory<>("circumstances"));
        
        TableColumn<Punishment, Integer> daysCol = new TableColumn<>("Taux (jours)");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("days"));
        
        TableColumn<Punishment, String> authorityCol = new TableColumn<>("Autorité");
        authorityCol.setCellValueFactory(new PropertyValueFactory<>("authority"));
        
        TableColumn<Punishment, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Punishment, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Punishment, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Punishment punishment = getTableView().getItems().get(getIndex());
                        showPunishmentEditDialog(punishment, punishmentsTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Punishment punishment = getTableView().getItems().get(getIndex());
                        punishmentsTable.getItems().remove(punishment);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        punishmentsTable.getColumns().addAll(itemCol, motifCol, circumstancesCol, daysCol, authorityCol, dateCol, actionCol);
        
        itemCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.15));
        motifCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.25));
        circumstancesCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.20));
        daysCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.05));
        authorityCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.10));
        dateCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.10));
        actionCol.prefWidthProperty().bind(punishmentsTable.widthProperty().multiply(0.15));
        
        // Ajouter le tableau à la vue
        container.getChildren().add(punishmentsTable);
        
        // Ajouter un bouton pour ajouter une nouvelle punition
        Button addButton = new Button("Ajouter une Punition");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showPunishmentAddDialog(punishmentsTable));
        container.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> punishmentsData = formData.getOrDefault("punitions", new HashMap<>());
            
            // Stocker les valeurs
            punishmentsData.put("matricule", currentMatricule);
            punishmentsData.put("punitions", punishmentsTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("punitions", punishmentsData);
            
            // Afficher une confirmation
            showSuccessMessage("Les punitions ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle punition.
     * 
     * @param punishmentsTable Le tableau des punitions
     */
    private void showPunishmentAddDialog(TableView<Punishment> punishmentsTable) {
        Dialog<Punishment> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Punition");
        dialog.setHeaderText("Ajouter une punition à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField itemField = new TextField();
        TextField motifField = new TextField();
        TextField circumstancesField = new TextField();
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 1);
        TextField authorityField = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Item", itemField),
            createFormField("Motif", motifField),
            createFormField("Circonstances", circumstancesField),
            createFormField("Taux (jours)", daysSpinner),
            createFormField("Autorité ayant infligé la punition", authorityField),
            createFormField("Date", datePicker)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Punishment
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Punishment(
                    itemField.getText(),
                    motifField.getText(),
                    circumstancesField.getText(),
                    daysSpinner.getValue(),
                    authorityField.getText(),
                    datePicker.getValue()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Punishment> result = dialog.showAndWait();
        
        // Ajouter la nouvelle punition au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(punishment -> {
            punishmentsTable.getItems().add(punishment);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une punition.
     * 
     * @param punishment La punition à modifier
     * @param punishmentsTable Le tableau des punitions
     */
    private void showPunishmentEditDialog(Punishment punishment, TableView<Punishment> punishmentsTable) {
        Dialog<Punishment> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Punition");
        dialog.setHeaderText("Modifier les informations de la punition");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField itemField = new TextField(punishment.getItem());
        TextField motifField = new TextField(punishment.getMotif());
        TextField circumstancesField = new TextField(punishment.getCircumstances());
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, punishment.getDays());
        TextField authorityField = new TextField(punishment.getAuthority());
        DatePicker datePicker = new DatePicker(punishment.getDate());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Item", itemField),
            createFormField("Motif", motifField),
            createFormField("Circonstances", circumstancesField),
            createFormField("Taux (jours)", daysSpinner),
            createFormField("Autorité ayant infligé la punition", authorityField),
            createFormField("Date", datePicker)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Punishment
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Punishment(
                    itemField.getText(),
                    motifField.getText(),
                    circumstancesField.getText(),
                    daysSpinner.getValue(),
                    authorityField.getText(),
                    datePicker.getValue()
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Punishment> result = dialog.showAndWait();
        
        // Mettre à jour la punition si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedPunishment -> {
            int index = punishmentsTable.getItems().indexOf(punishment);
            punishmentsTable.getItems().set(index, updatedPunishment);
        });
    }
    
    /**
     * Classe pour représenter une punition.
     */
    public static class Punishment {
        private final SimpleStringProperty item;
        private final SimpleStringProperty motif;
        private final SimpleStringProperty circumstances;
        private final SimpleObjectProperty<Integer> days;
        private final SimpleStringProperty authority;
        private final SimpleObjectProperty<LocalDate> date;
        
        public Punishment(String item, String motif, String circumstances, Integer days, String authority, LocalDate date) {
            this.item = new SimpleStringProperty(item);
            this.motif = new SimpleStringProperty(motif);
            this.circumstances = new SimpleStringProperty(circumstances);
            this.days = new SimpleObjectProperty<>(days);
            this.authority = new SimpleStringProperty(authority);
            this.date = new SimpleObjectProperty<>(date);
        }
        
        public String getItem() { return item.get(); }
        public String getMotif() { return motif.get(); }
        public String getCircumstances() { return circumstances.get(); }
        public Integer getDays() { return days.get(); }
        public String getAuthority() { return authority.get(); }
        public LocalDate getDate() { return date.get(); }
        
        public void setItem(String item) { this.item.set(item); }
        public void setMotif(String motif) { this.motif.set(motif); }
        public void setCircumstances(String circumstances) { this.circumstances.set(circumstances); }
        public void setDays(Integer days) { this.days.set(days); }
        public void setAuthority(String authority) { this.authority.set(authority); }
        public void setDate(LocalDate date) { this.date.set(date); }
    }
    
    // ******************** Langues ********************
    
    /**
     * Affiche le formulaire de gestion des langues.
     */
    @FXML
    private void showLanguesInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Langues");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création d'un onglet pour les différentes catégories de langues
        TabPane tabPane = new TabPane();
        
        // Onglet pour les langues parlées
        Tab langueParleesTab = new Tab("Langues Parlées");
        langueParleesTab.setClosable(false);
        langueParleesTab.setContent(createLanguageTableContent("Langues Parlées"));
        
        // Onglet pour les langues écrites
        Tab langueEcritesTab = new Tab("Langues Écrites");
        langueEcritesTab.setClosable(false);
        langueEcritesTab.setContent(createLanguageTableContent("Langues Écrites"));
        
        // Onglet pour les langues lues
        Tab langueLuesTab = new Tab("Langues Lues");
        langueLuesTab.setClosable(false);
        langueLuesTab.setContent(createLanguageTableContent("Langues Lues"));
        
        // Onglet pour les langues apprises
        Tab langueApprisesTab = new Tab("Langues Apprises");
        langueApprisesTab.setClosable(false);
        langueApprisesTab.setContent(createLanguageTableContent("Langues Apprises"));
        
        tabPane.getTabs().addAll(langueParleesTab, langueEcritesTab, langueLuesTab, langueApprisesTab);
        
        container.getChildren().add(tabPane);
        
        // Bouton pour sauvegarder toutes les langues
        Button saveButton = new Button("Enregistrer");
        saveButton.getStyleClass().addAll("action-button");
        saveButton.setOnAction(e -> {
            saveLanguages();
            showSuccessMessage("Les informations des langues ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
 // Classe pour représenter une langue
    public static class Langue {
        private final SimpleStringProperty langue;
        private final SimpleStringProperty niveau;
        
        public Langue(String langue, String niveau) {
            this.langue = new SimpleStringProperty(langue);
            this.niveau = new SimpleStringProperty(niveau);
        }
        
        public String getLangue() { return langue.get(); }
        public String getNiveau() { return niveau.get(); }
        
        public void setLangue(String langue) { this.langue.set(langue); }
        public void setNiveau(String niveau) { this.niveau.set(niveau); }
    }
    
 // Méthode pour créer le contenu d'un tableau de langues
    private Node createLanguageTableContent(String category) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher les langues
        TableView<Langue> languesTable = new TableView<>();
        languesTable.setPrefWidth(Double.MAX_VALUE);
        languesTable.setMaxWidth(Double.MAX_VALUE);
        languesTable.setMinHeight(300);
        languesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Langue, String> langueCol = new TableColumn<>("Langue");
        langueCol.setCellValueFactory(new PropertyValueFactory<>("langue"));
        langueCol.setPrefWidth(200);
        
        TableColumn<Langue, String> niveauCol = new TableColumn<>("Niveau");
        niveauCol.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        niveauCol.setPrefWidth(200);
        
        TableColumn<Langue, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Langue, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    editButton.getStyleClass().addAll("action-button", "edit");
                    deleteButton.getStyleClass().addAll("action-button", "delete");
                    
                    editButton.setOnAction(event -> {
                        Langue langue = getTableView().getItems().get(getIndex());
                        showLangueEditDialog(langue, languesTable, category);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Langue langue = getTableView().getItems().get(getIndex());
                        languesTable.getItems().remove(langue);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        actionCol.setPrefWidth(150);
        
        // Ajouter les colonnes au tableau
        languesTable.getColumns().addAll(langueCol, niveauCol, actionCol);
        
     // Configurer les largeurs des colonnes pour qu'elles prennent proportionnellement la largeur
        langueCol.prefWidthProperty().bind(languesTable.widthProperty().multiply(0.50));
        niveauCol.prefWidthProperty().bind(languesTable.widthProperty().multiply(0.20));
        
        actionCol.prefWidthProperty().bind(languesTable.widthProperty().multiply(0.30));
        
        // Ajouter le tableau à la vue
        content.getChildren().add(languesTable);
        
        // Ajouter un bouton pour ajouter une nouvelle langue
        Button addButton = new Button("Ajouter une Langue");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.getStyleClass().addAll("action-button", "add");
        addButton.setOnAction(e -> showLangueAddDialog(languesTable, category));
        content.getChildren().add(addButton);
        
        return content;
    }
    
 // Méthode pour afficher la boîte de dialogue d'ajout de langue
    private void showLangueAddDialog(TableView<Langue> languesTable, String category) {
        Dialog<Langue> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Langue");
        dialog.setHeaderText("Ajouter une langue à la catégorie: " + category);
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> langueCombo = new ComboBox<>();
        langueCombo.setPromptText("Choisir une langue");
        langueCombo.getItems().addAll("Mandarin", "Espagnol", "Anglais", "Hindi", "Arabe", "Bengali", "Portugais", "Russe", "Japonais", "Pendjabi", "Allemand", "Javanais", "Wu (chinois)", "Telugu", "Vietnamien", "Coréen", "Français", "Marathi", "Tamil", "Ourdou", "Italien");
        langueCombo.setEditable(true);
        
        ComboBox<String> niveauCombo = new ComboBox<>();
        niveauCombo.setPromptText("Niveau de maîtrise");
        niveauCombo.getItems().addAll("Débutant", "Médiocre", "Passable", "Bien", "Fluide");
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Langue", langueCombo),
            createFormField("Niveau", niveauCombo)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Langue
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String langue = langueCombo.getValue();
                if (langueCombo.getEditor().getText() != null && !langueCombo.getEditor().getText().isEmpty()) {
                    langue = langueCombo.getEditor().getText();
                }
                
                return new Langue(langue, niveauCombo.getValue());
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Langue> result = dialog.showAndWait();
        
        // Ajouter la nouvelle langue au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(langue -> {
            languesTable.getItems().add(langue);
            
            // Stocker également la langue dans formData
            Map<String, Object> langueData = formData.getOrDefault("langues_" + category.toLowerCase().replace(" ", "_"), new HashMap<>());
            
            // Stocker les valeurs (on utilise un ID unique pour chaque langue)
            String langueId = "langue_" + UUID.randomUUID().toString();
            Map<String, String> langueInfo = new HashMap<>();
            langueInfo.put("langue", langue.getLangue());
            langueInfo.put("niveau", langue.getNiveau());
            
            langueData.put(langueId, langueInfo);
            langueData.put("matricule", currentMatricule);
            
            formData.put("langues_" + category.toLowerCase().replace(" ", "_"), langueData);
        });
    }

    // Méthode pour afficher la boîte de dialogue de modification de langue
    private void showLangueEditDialog(Langue langue, TableView<Langue> languesTable, String category) {
        Dialog<Langue> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Langue");
        dialog.setHeaderText("Modifier les informations de la langue");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> langueCombo = new ComboBox<>();
        langueCombo.setPromptText("Choisir une langue");
        langueCombo.getItems().addAll("Mandarin", "Espagnol", "Anglais", "Hindi", "Arabe", "Bengali", "Portugais", "Russe", "Japonais", "Pendjabi", "Allemand", "Javanais", "Wu (chinois)", "Telugu", "Vietnamien", "Coréen", "Français", "Marathi", "Tamil", "Ourdou", "Italien");
        langueCombo.setValue(langue.getLangue());
        langueCombo.setEditable(true);
        
        ComboBox<String> niveauCombo = new ComboBox<>();
        niveauCombo.setPromptText("Niveau de maîtrise");
        niveauCombo.getItems().addAll("Débutant", "Médiocre", "Passable", "Bien", "Fluide");
        niveauCombo.setValue(langue.getNiveau());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Langue", langueCombo),
            createFormField("Niveau", niveauCombo)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Langue
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String langueValue = langueCombo.getValue();
                if (langueCombo.getEditor().getText() != null && !langueCombo.getEditor().getText().isEmpty()) {
                    langueValue = langueCombo.getEditor().getText();
                }
                
                return new Langue(langueValue, niveauCombo.getValue());
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Langue> result = dialog.showAndWait();
        
        // Mettre à jour la langue si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedLangue -> {
            int index = languesTable.getItems().indexOf(langue);
            languesTable.getItems().set(index, updatedLangue);
            
            // Mettre à jour également les données dans formData
            // Cela nécessiterait d'avoir l'ID de la langue existante, ce qui est complexe
            // Une alternative serait de recréer complètement formData pour cette catégorie
            Map<String, Object> langueData = formData.getOrDefault("langues_" + category.toLowerCase().replace(" ", "_"), new HashMap<>());
            formData.put("langues_" + category.toLowerCase().replace(" ", "_"), updateLanguesData(langueData, languesTable.getItems()));
        });
    }

    // Méthode pour mettre à jour les données de langue dans formData
    private Map<String, Object> updateLanguesData(Map<String, Object> existingData, List<Langue> langues) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("matricule", currentMatricule);
        
        // Recréer toutes les entrées de langue
        for (Langue langue : langues) {
            String langueId = "langue_" + UUID.randomUUID().toString();
            Map<String, String> langueInfo = new HashMap<>();
            langueInfo.put("langue", langue.getLangue());
            langueInfo.put("niveau", langue.getNiveau());
            
            updatedData.put(langueId, langueInfo);
        }
        
        return updatedData;
    }

    // Méthode uniforme pour ajouter des boutons aux tableaux
    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("action-button", styleClass);
        return button;
    }

    
    
    /**
     * Sauvegarde les données des langues.
     */
    private void saveLanguages() {
        // Pour simplifier, nous supposons que les données des langues sont déjà stockées
        // dans la map formData lors de l'ajout de chaque langue
    }
    
    /**
     * Crée une boîte pour une catégorie de langues dans la grille.
     * 
     * @param grid La grille contenant les boîtes de langues
     * @param title Le titre de la catégorie
     * @param col La colonne dans la grille
     * @param row La ligne dans la grille
     * @param borderColor La couleur de la bordure
     */
    private void createLanguageBox(GridPane grid, String title, int col, int row, String borderColor) {
        VBox box = new VBox(10);
        box.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 2px; -fx-padding: 10px; -fx-border-radius: 5px;");
        
        // Configuration pour que la boîte prenne tout l'espace disponible
        GridPane.setFillWidth(box, true);
        GridPane.setFillHeight(box, true);
        box.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Titre de la catégorie
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        // ComboBox pour sélectionner la langue
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.setPromptText("Choisir une langue");
        languageCombo.getItems().addAll("Mandarin", "Espagnol", "Anglais", "Hindi", "Arabe", "Bengali", "Portugais", "Russe", "Japonais", "Pendjabi", "Allemand", "Javanais", "Wu (chinois)", "Telugu", "Vietnamien", "Coréen", "Français", "Marathi", "Tamil", "Ourdou", "Italien");
        languageCombo.setMaxWidth(Double.MAX_VALUE);
        
        // ComboBox pour sélectionner le niveau de maîtrise
        ComboBox<String> levelCombo = new ComboBox<>();
        levelCombo.setPromptText("Niveau de maîtrise");
        levelCombo.getItems().addAll("Débutant", "Médiocre", "Passable", "Bien", "Fluide");
        levelCombo.setMaxWidth(Double.MAX_VALUE);
        
        // VBox pour afficher les langues ajoutées
        VBox languagesList = new VBox(5);
        languagesList.setPrefHeight(200);
        
        // ScrollPane pour faire défiler la liste des langues
        ScrollPane scrollPane = new ScrollPane(languagesList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Bouton pour ajouter une langue
        Button addButton = new Button("Ajouter");
        addButton.setMaxWidth(Double.MAX_VALUE);
        
        // Gestion de l'événement d'ajout de langue
        addButton.setOnAction(e -> {
            String language = languageCombo.getValue();
            String level = levelCombo.getValue();
            
            if (language != null && level != null) {
                // Créer une HBox pour afficher la langue et le niveau avec un bouton de suppression
                HBox languageItem = new HBox(10);
                
                Label languageLabel = new Label(language + " - " + level);
                Button deleteButton = new Button("X");
                deleteButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                
                languageItem.getChildren().addAll(languageLabel, deleteButton);
                
                // Ajouter à la liste des langues
                languagesList.getChildren().add(languageItem);
                
                // Réinitialiser les ComboBox
                languageCombo.setValue(null);
                levelCombo.setValue(null);
                
                // Ajouter à la map des données de formulaire
                Map<String, Object> languageData = formData.getOrDefault("langues_" + title.toLowerCase().replace(" ", "_"), new HashMap<>());
                
                // Stocker les valeurs (on utilise un ID unique pour chaque langue)
                String languageId = "langue_" + UUID.randomUUID().toString();
                Map<String, String> languageInfo = new HashMap<>();
                languageInfo.put("langue", language);
                languageInfo.put("niveau", level);
                
                languageData.put(languageId, languageInfo);
                languageData.put("matricule", currentMatricule);
                
                formData.put("langues_" + title.toLowerCase().replace(" ", "_"), languageData);
                
                // Configurer le bouton de suppression
                deleteButton.setOnAction(event -> {
                    languagesList.getChildren().remove(languageItem);
                    
                    // Supprimer de la map des données de formulaire
                    languageData.remove(languageId);
                });
            }
        });
        
        // Ajouter tous les éléments à la boîte
        box.getChildren().addAll(titleLabel, languageCombo, levelCombo, addButton, scrollPane);
        
        // Ajouter la boîte à la grille
        grid.add(box, col, row);
    }
    
// ******************** Informations Spécifiques ********************
    
    /**
     * Méthode appelée lorsque l'utilisateur clique sur le bouton "Informations Spécifiques".
     * Affiche la première sous-section (Général).
     */
    @FXML
    private void showSpecialInfo() {
        // Cette méthode est liée au bouton et gérée par handleMainButtonClick
        // Elle ne fait rien ici car la logique est dans navigateToSection
    }
    
    /**
     * Affiche le formulaire des informations spécifiques générales.
     */
    @FXML
    private void showGeneralSpecialInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Informations Spécifiques - Général");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Récupérer les données existantes si disponibles
        Map<String, Object> generalData = formData.getOrDefault("infos_specifiques_general", new HashMap<>());
        
        // Création des champs de formulaire existants
        Spinner<Integer> epmsSpinner = new Spinner<>(1, 10, 1);
        if (generalData.containsKey("niveau_epms")) {
            epmsSpinner.getValueFactory().setValue((Integer) generalData.get("niveau_epms"));
        }
        
        ComboBox<String> permisComboBox = createComboBox("A", "B", "C", "D");
        if (generalData.containsKey("permis_militaire")) {
            permisComboBox.setValue((String) generalData.get("permis_militaire"));
        }
        
        TextField armeField = new TextField((String) generalData.getOrDefault("arme_dotation", ""));
        
        ComboBox<String> corpsComboBox = createComboBox("Personnel Naviguant", "Personnel Non Naviguant Spécialistes", "Personnel Non Naviguant du Service Général");
        if (generalData.containsKey("corps_technique")) {
            corpsComboBox.setValue((String) generalData.get("corps_technique"));
        }
        
        // Nouveaux champs ajoutés
        TextField numeroCarteIdentiteField = new TextField((String) generalData.getOrDefault("numero_carte_identite", ""));
        TextField numeroPermisField = new TextField((String) generalData.getOrDefault("numero_permis_militaire", ""));
        TextField promotionContingentField = new TextField((String) generalData.getOrDefault("promotion_contingent", ""));
        TextField numeroCarteIdentiteMilitaireField = new TextField((String) generalData.getOrDefault("numero_carte_identite_militaire", ""));
        TextField numeroMatriculeSoldeField = new TextField((String) generalData.getOrDefault("numero_matricule_solde", ""));
        
        ComboBox<String> positionAdministrativeCombo = createComboBox("Absent", "ASM", "Absence irrégulière", "Présent", "Stage", "Mission", "Subsistant", "Désertion", "Détention", "Poursuite Judiciaire", "Permission", "Hors cadre", "Détaché", "Retraite", "Décédé", "Evasan");
        if (generalData.containsKey("position_administrative")) {
            positionAdministrativeCombo.setValue((String) generalData.get("position_administrative"));
        }
        
        // NOUVEAU CHAMP RÉFÉRENCE ajouté après position administrative
        TextField referenceField = new TextField((String) generalData.getOrDefault("reference", ""));
        
        DatePicker dateIncorporationPicker = new DatePicker();
        if (generalData.containsKey("date_incorporation")) {
            dateIncorporationPicker.setValue((LocalDate) generalData.get("date_incorporation"));
        }
        
        DatePicker dateFinServicePicker = new DatePicker();
        if (generalData.containsKey("date_fin_service")) {
            dateFinServicePicker.setValue((LocalDate) generalData.get("date_fin_service"));
        }
        
        // Label pour afficher le temps restant jusqu'à la retraite
        Label tempsRestantLabel = new Label();
        tempsRestantLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
        
        // Méthode pour calculer et afficher le temps restant
        Runnable updateTempsRestant = () -> {
            LocalDate dateNaissance = getDateNaissanceFromFormData();
            if (dateNaissance != null) {
                String gradeActuel = getGradeActuelFromFormData();
                if (gradeActuel != null && !gradeActuel.isEmpty()) {
                    String tempsRestant = calculateTimeToRetirement(dateNaissance, gradeActuel);
                    tempsRestantLabel.setText(tempsRestant);
                    
                    // Calculer et définir automatiquement la date de fin de service
                    LocalDate dateFinService = calculateRetirementDate(dateNaissance, gradeActuel);
                    dateFinServicePicker.setValue(dateFinService);
                }
            }
        };
        
        // Ajouter des listeners pour recalculer automatiquement
        dateIncorporationPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateTempsRestant.run());
        
        // Création du formulaire avec le nouveau champ référence
        VBox form = new VBox(15);
        form.getChildren().addAll(
            createFormField("Niveau EPMS:", epmsSpinner),
            createFormField("Permis militaire:", permisComboBox),
            createFormField("Numéro permis militaire:", numeroPermisField),
            createFormField("Arme de dotation:", armeField),
            createFormField("Corps technique:", corpsComboBox),
            createFormField("Numéro de Carte d'Identité Nationale:", numeroCarteIdentiteField),
            createFormField("Promotion-contingent:", promotionContingentField),
            createFormField("Numéro de Carte d'Identité Militaire:", numeroCarteIdentiteMilitaireField),
            createFormField("Numéro matricule solde:", numeroMatriculeSoldeField),
            createFormField("Position administrative:", positionAdministrativeCombo),
            createFormField("Référence:", referenceField), // NOUVEAU CHAMP AJOUTÉ ICI
            createFormField("Date d'incorporation:", dateIncorporationPicker),
            createFormField("Date de fin de service:", dateFinServicePicker),
            createFormField("Temps restant jusqu'à la retraite:", tempsRestantLabel)
        );
        
        container.getChildren().add(form);
        
        // Calculer le temps restant initial
        updateTempsRestant.run();
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> data = formData.getOrDefault("infos_specifiques_general", new HashMap<>());
            
            // Stocker les valeurs existantes
            data.put("matricule", currentMatricule);
            data.put("niveau_epms", epmsSpinner.getValue());
            data.put("permis_militaire", permisComboBox.getValue());
            data.put("arme_dotation", armeField.getText());
            data.put("corps_technique", corpsComboBox.getValue());
            
            // Stocker les nouvelles valeurs
            data.put("numero_carte_identite", numeroCarteIdentiteField.getText());
            data.put("numero_permis_militaire", numeroPermisField.getText());
            data.put("promotion_contingent", promotionContingentField.getText());
            data.put("numero_carte_identite_militaire", numeroCarteIdentiteMilitaireField.getText());
            data.put("numero_matricule_solde", numeroMatriculeSoldeField.getText());
            data.put("position_administrative", positionAdministrativeCombo.getValue());
            data.put("reference", referenceField.getText()); // NOUVEAU CHAMP SAUVEGARDÉ
            data.put("date_incorporation", dateIncorporationPicker.getValue());
            data.put("date_fin_service", dateFinServicePicker.getValue());
            data.put("temps_restant_retraite", tempsRestantLabel.getText());
            
            // Mettre à jour la map globale
            formData.put("infos_specifiques_general", data);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations spécifiques générales ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Récupère la date de naissance depuis les données du formulaire.
     */
    private LocalDate getDateNaissanceFromFormData() {
        if (formData.containsKey("identite_personnelle")) {
            Map<String, Object> identiteData = formData.get("identite_personnelle");
            Object dateNaissance = identiteData.get("date_naissance");
            if (dateNaissance instanceof LocalDate) {
                return (LocalDate) dateNaissance;
            }
        }
        return null;
    }

    /**
     * Récupère le grade actuel depuis les données du formulaire.
     */
    private String getGradeActuelFromFormData() {
        if (formData.containsKey("grade_actuel")) {
            Map<String, Object> gradeData = formData.get("grade_actuel");
            if (gradeData.containsKey("grade")) {
                Grade grade = (Grade) gradeData.get("grade");
                return grade.getRang();
            }
        }
        return null;
    }
    
    /**
     * Calcule le temps restant jusqu'à la retraite en fonction de la date de naissance et du grade.
     */
    private String calculateTimeToRetirement(LocalDate dateNaissance, String grade) {
        if (dateNaissance == null || grade == null) {
            return "Informations insuffisantes";
        }
        
        int ageRetraite = getRetirementAge(grade);
        LocalDate dateRetraite = dateNaissance.plusYears(ageRetraite);
        LocalDate today = LocalDate.now();
        
        if (dateRetraite.isBefore(today) || dateRetraite.equals(today)) {
            return "Âge de retraite atteint";
        }
        
        // Calculer la différence
        long totalDays = ChronoUnit.DAYS.between(today, dateRetraite);
        long years = totalDays / 365;
        long remainingDays = totalDays % 365;
        long months = remainingDays / 30;
        long days = remainingDays % 30;
        
        StringBuilder result = new StringBuilder();
        if (years > 0) {
            result.append(years).append(" année").append(years > 1 ? "s" : "");
        }
        if (months > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(months).append(" mois");
        }
        if (days > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(days).append(" jour").append(days > 1 ? "s" : "");
        }
        
        return result.length() > 0 ? result.toString() : "Moins d'un jour";
    }
    
    /**
     * Calcule la date de retraite en fonction de la date de naissance et du grade.
     */
    private LocalDate calculateRetirementDate(LocalDate dateNaissance, String grade) {
        if (dateNaissance == null || grade == null) {
            return null;
        }
        
        int ageRetraite = getRetirementAge(grade);
        return dateNaissance.plusYears(ageRetraite);
    }

    /**
     * Retourne l'âge de retraite selon le grade.
     */
    private int getRetirementAge(String grade) {
        switch (grade) {
            case "Colonel":
                return 58;
            case "Lieutenant-colonel":
                return 57;
            case "Commandant":
                return 56;
            case "Capitaine":
                return 55;
            case "Lieutenant":
            case "Sous-lieutenant":
                return 54;
            case "Adjudant-chef-major":
                return 55;
            case "Adjudant-chef":
            case "Adjudant":
            case "Sergent-chef":
            case "Sergent":
                return 54;
            case "Caporal-chef":
            case "Caporal":
            case "Soldat de 1ère classe":
            case "Soldat de 2e classe":
                return 49;
            default:
                return 55; // Âge par défaut
        }
    }

    
    /**
     * Affiche le formulaire des informations du personnel naviguant.
     */
    @FXML
    private void showNavigatingCrewInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Informations Spécifiques - Personnel Naviguant");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création des champs de formulaire
        TextField aeronefArmeField = new TextField();
        ComboBox<String> fonctionBordComboBox = createComboBox("Pilote", "Copilote", "Mécanicien");
        ComboBox<String> aeronefAffectationComboBox = createComboBox("A320", "B737", "F16");
        ComboBox<String> qualificationTypeComboBox = createComboBox("Type A", "Type B", "Type C");
        
        Spinner<Double> testTrimestrielSpinner = new Spinner<>(0.0, 20.0, 0.0, 0.1);
        testTrimestrielSpinner.setEditable(true);
        
        DatePicker cempnDatePicker = new DatePicker();
        
        Spinner<Integer> heuresVolSpinner = new Spinner<>(0, 10000, 0);
        heuresVolSpinner.setEditable(true);
        
        Spinner<Integer> anciennetePNSpinner = new Spinner<>(0, 50, 0);
        anciennetePNSpinner.setEditable(true);
        
        TextField numeroTitreAerienField = new TextField();
        
        Spinner<Integer> niveauExecutionSpinner = new Spinner<>(1, 10, 1);
        niveauExecutionSpinner.setEditable(true);
        
        // Création du formulaire
        VBox form = new VBox(15);
        form.getChildren().addAll(
            createFormField("Aéronef d'arme:", aeronefArmeField),
            createFormField("Fonction à bord:", fonctionBordComboBox),
            createFormField("Aéronef d'affectation:", aeronefAffectationComboBox),
            createFormField("Qualification de type:", qualificationTypeComboBox),
            createFormField("Note au test trimestriel:", testTrimestrielSpinner),
            createFormField("Délai de validité CEMPN:", cempnDatePicker),
            createFormField("Nombre d'heures de vol:", heuresVolSpinner),
            createFormField("Ancienneté PN:", anciennetePNSpinner),
            createFormField("Numéro de titre aérien:", numeroTitreAerienField),
            createFormField("Niveau d'exécution:", niveauExecutionSpinner)
        );
        
        container.getChildren().add(form);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> navData = formData.getOrDefault("personnel_naviguant", new HashMap<>());
            
            // Stocker les valeurs
            navData.put("matricule", currentMatricule);
            navData.put("aeronef_arme", aeronefArmeField.getText());
            navData.put("fonction_bord", fonctionBordComboBox.getValue());
            navData.put("aeronef_affectation", aeronefAffectationComboBox.getValue());
            navData.put("qualification_type", qualificationTypeComboBox.getValue());
            navData.put("test_trimestriel", testTrimestrielSpinner.getValue());
            navData.put("cempn_validite", cempnDatePicker.getValue());
            navData.put("heures_vol", heuresVolSpinner.getValue());
            navData.put("anciennete_pn", anciennetePNSpinner.getValue());
            navData.put("numero_titre_aerien", numeroTitreAerienField.getText());
            navData.put("niveau_execution", niveauExecutionSpinner.getValue());
            
            // Mettre à jour la map globale
            formData.put("personnel_naviguant", navData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations du personnel naviguant ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche le formulaire des informations de maintenance.
     */
    @FXML
    private void showMaintenanceInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Informations Spécifiques - Maintenance");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création d'un onglet pour les maintenances programmées et curatives
        TabPane tabPane = new TabPane();
        
        // Onglet pour les maintenances programmées
        Tab maintenancesProgrammeesTab = new Tab("Maintenances Programmées");
        maintenancesProgrammeesTab.setClosable(false);
        maintenancesProgrammeesTab.setContent(createMaintenancesProgrammeesContent());
        
        // Onglet pour les maintenances curatives
        Tab maintenancesCurativesTab = new Tab("Maintenances Curatives");
        maintenancesCurativesTab.setClosable(false);
        maintenancesCurativesTab.setContent(createMaintenancesCurativesContent());
        
        tabPane.getTabs().addAll(maintenancesProgrammeesTab, maintenancesCurativesTab);
        
        container.getChildren().add(tabPane);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Crée le contenu pour l'onglet des maintenances programmées.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createMaintenancesProgrammeesContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Niveau d'exécution
        TextField niveauExecutionField = new TextField();
        content.getChildren().add(createFormField("Niveau d'exécution:", niveauExecutionField));
        
        // Tableau pour afficher les maintenances programmées
        TableView<Maintenance> maintenancesTable = new TableView<>();
        
        // Colonnes du tableau
        TableColumn<Maintenance, String> operationCol = new TableColumn<>("Opération");
        operationCol.setCellValueFactory(new PropertyValueFactory<>("operation"));
        
        TableColumn<Maintenance, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Maintenance, String> formationCol = new TableColumn<>("Formation");
        formationCol.setCellValueFactory(new PropertyValueFactory<>("formation"));
        
        TableColumn<Maintenance, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Maintenance, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Maintenance maintenance = getTableView().getItems().get(getIndex());
                        showMaintenanceEditDialog(maintenance, maintenancesTable, true);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Maintenance maintenance = getTableView().getItems().get(getIndex());
                        maintenancesTable.getItems().remove(maintenance);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        maintenancesTable.getColumns().addAll(operationCol, dateCol, formationCol, actionCol);
        
        operationCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.40));
        dateCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        formationCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        actionCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        
        // Ajouter le tableau à la vue
        content.getChildren().add(maintenancesTable);
        
        // Ajouter un bouton pour ajouter une nouvelle maintenance
        Button addButton = new Button("Ajouter une Maintenance Programmée");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showMaintenanceAddDialog(maintenancesTable, true));
        content.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> maintenancesData = formData.getOrDefault("maintenances_programmees", new HashMap<>());
            
            // Stocker les valeurs
            maintenancesData.put("matricule", currentMatricule);
            maintenancesData.put("niveau_execution", niveauExecutionField.getText());
            maintenancesData.put("maintenances", maintenancesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("maintenances_programmees", maintenancesData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des maintenances programmées ont été enregistrées.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Crée le contenu pour l'onglet des maintenances curatives.
     * 
     * @return Le contenu de l'onglet
     */
    private Node createMaintenancesCurativesContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Tableau pour afficher les maintenances curatives
        TableView<Maintenance> maintenancesTable = new TableView<>();
        maintenancesTable.setPrefWidth(Double.MAX_VALUE);
        maintenancesTable.setMaxWidth(Double.MAX_VALUE);
        maintenancesTable.setMinHeight(300);
        maintenancesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Maintenance, String> operationCol = new TableColumn<>("Opération");
        operationCol.setCellValueFactory(new PropertyValueFactory<>("operation"));
        
        TableColumn<Maintenance, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<Maintenance, String> formationCol = new TableColumn<>("Formation");
        formationCol.setCellValueFactory(new PropertyValueFactory<>("formation"));
        
        TableColumn<Maintenance, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Maintenance, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Maintenance maintenance = getTableView().getItems().get(getIndex());
                        showMaintenanceEditDialog(maintenance, maintenancesTable, false);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Maintenance maintenance = getTableView().getItems().get(getIndex());
                        maintenancesTable.getItems().remove(maintenance);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        maintenancesTable.getColumns().addAll(operationCol, dateCol, formationCol, actionCol);
        
        operationCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.40));
        dateCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        formationCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        actionCol.prefWidthProperty().bind(maintenancesTable.widthProperty().multiply(0.20));
        
        // Ajouter le tableau à la vue
        content.getChildren().add(maintenancesTable);
        
        // Ajouter un bouton pour ajouter une nouvelle maintenance
        Button addButton = new Button("Ajouter une Maintenance Curative");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showMaintenanceAddDialog(maintenancesTable, false));
        content.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> maintenancesData = formData.getOrDefault("maintenances_curatives", new HashMap<>());
            
            // Stocker les valeurs
            maintenancesData.put("matricule", currentMatricule);
            maintenancesData.put("maintenances", maintenancesTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("maintenances_curatives", maintenancesData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des maintenances curatives ont été enregistrées.");
        });
        content.getChildren().add(saveButton);
        
        return content;
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle maintenance.
     * 
     * @param maintenancesTable Le tableau des maintenances
     * @param isProgrammee true si c'est une maintenance programmée, false si c'est une maintenance curative
     */
    private void showMaintenanceAddDialog(TableView<Maintenance> maintenancesTable, boolean isProgrammee) {
        Dialog<Maintenance> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Maintenance " + (isProgrammee ? "Programmée" : "Curative"));
        dialog.setHeaderText("Ajouter une maintenance à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> operationField = createComboBox();
        if (isProgrammee) {
            operationField.getItems().addAll("inspection pré-vol", "inspection post-vol", "vidange moteur", "changement d'huile", "remplacement des filtres", "vérification des freins", "remplacement des plaquettes de frein", "contrôle des pneus", "gonflage des pneus", "vérification des niveaux hydrauliques", "contrôle des systèmes électriques", "test des instruments de vol", "nettoyage des capteurs", "inspection des ailes", "vérification de l’intégrité structurale", "graissage des parties mobiles", "test des volets et ailerons", "mise à jour des logiciels avioniques", "vérification des équipements de secours", "inspection des réservoirs de carburant", "dégivrage", "contrôle de la cabine pressurisée", "maintenance des climatiseurs", "nettoyage extérieur", "calibrage des instruments de navigation");
        } else {
            operationField.setEditable(true);
        }
        
        DatePicker datePicker = new DatePicker();
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Opération", operationField),
            createFormField("Date", datePicker),
            createFormField("Formation", formationField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Maintenance
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Maintenance(
                    operationField.getValue() != null ? operationField.getValue() : operationField.getEditor().getText(),
                    datePicker.getValue(),
                    formationField.getValue(),
                    isProgrammee ? "Programmée" : "Curative"
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Maintenance> result = dialog.showAndWait();
        
        // Ajouter la nouvelle maintenance au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(maintenance -> {
            maintenancesTable.getItems().add(maintenance);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une maintenance.
     * 
     * @param maintenance La maintenance à modifier
     * @param maintenancesTable Le tableau des maintenances
     * @param isProgrammee true si c'est une maintenance programmée, false si c'est une maintenance curative
     */
    private void showMaintenanceEditDialog(Maintenance maintenance, TableView<Maintenance> maintenancesTable, boolean isProgrammee) {
        Dialog<Maintenance> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Maintenance");
        dialog.setHeaderText("Modifier les informations de la maintenance");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        ComboBox<String> operationField = createComboBox();
        if (isProgrammee) {
            operationField.getItems().addAll("inspection pré-vol", "inspection post-vol", "vidange moteur", "changement d'huile", "remplacement des filtres", "vérification des freins", "remplacement des plaquettes de frein", "contrôle des pneus", "gonflage des pneus", "vérification des niveaux hydrauliques", "contrôle des systèmes électriques", "test des instruments de vol", "nettoyage des capteurs", "inspection des ailes", "vérification de l’intégrité structurale", "graissage des parties mobiles", "test des volets et ailerons", "mise à jour des logiciels avioniques", "vérification des équipements de secours", "inspection des réservoirs de carburant", "dégivrage", "contrôle de la cabine pressurisée", "maintenance des climatiseurs", "nettoyage extérieur", "calibrage des instruments de navigation");
            operationField.setValue(maintenance.getOperation());
        } else {
            operationField.setEditable(true);
            operationField.setValue(maintenance.getOperation());
        }
        
        DatePicker datePicker = new DatePicker(maintenance.getDate());
        
        ComboBox<String> formationField = createComboBox("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
        formationField.setValue(maintenance.getFormation());
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Opération", operationField),
            createFormField("Date", datePicker),
            createFormField("Formation", formationField)
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Maintenance
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Maintenance(
                    operationField.getValue() != null ? operationField.getValue() : operationField.getEditor().getText(),
                    datePicker.getValue(),
                    formationField.getValue(),
                    isProgrammee ? "Programmée" : "Curative"
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Maintenance> result = dialog.showAndWait();
        
        // Mettre à jour la maintenance si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedMaintenance -> {
            int index = maintenancesTable.getItems().indexOf(maintenance);
            maintenancesTable.getItems().set(index, updatedMaintenance);
        });
    }
    
    /**
     * Affiche le formulaire des informations de spécialité.
     */
    @FXML
    private void showSpecialtyInfo() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Informations Spécifiques - Spécialité");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // ComboBox pour sélectionner la spécialité
        ComboBox<String> specialiteComboBox = createComboBox(
            "Pilote/ Méca Nav", "Ingénieur sol", "Autre spécialité", "PNNSG"
        );
        container.getChildren().add(createFormField("Spécialité:", specialiteComboBox));
        
        // VBox pour le contenu spécifique à la spécialité
        VBox specialiteContent = new VBox(15);
        container.getChildren().add(specialiteContent);
        
        // Ajouter un gestionnaire d'événements pour afficher le contenu approprié
        specialiteComboBox.setOnAction(e -> {
            String specialite = specialiteComboBox.getValue();
            if (specialite != null) {
                updateSpecialiteContent(specialite, specialiteContent);
            }
        });
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            String specialite = specialiteComboBox.getValue();
            if (specialite != null) {
                // Créer ou récupérer la map pour cette section
                Map<String, Object> specialiteData = formData.getOrDefault("specialite", new HashMap<>());
                
                // Stocker les valeurs
                specialiteData.put("matricule", currentMatricule);
                specialiteData.put("type_specialite", specialite);
                
                // Mettre à jour la map globale
                formData.put("specialite", specialiteData);
                
                // Afficher une confirmation
                showSuccessMessage("Les informations de spécialité ont été enregistrées.");
            } else {
                showError("Veuillez sélectionner une spécialité.");
            }
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Met à jour le contenu spécifique à la spécialité sélectionnée.
     * 
     * @param specialite La spécialité sélectionnée
     * @param contentBox La boîte de contenu à mettre à jour
     */
    private void updateSpecialiteContent(String specialite, VBox contentBox) {
        contentBox.getChildren().clear();
        
        switch (specialite) {
            case "Autre spécialité":
                TextField autreSpecialiteField = new TextField();
                contentBox.getChildren().add(createFormField("Précisez la spécialité:", autreSpecialiteField));
                
                // Ajouter un gestionnaire d'événements pour sauvegarder la valeur
                autreSpecialiteField.textProperty().addListener((obs, oldVal, newVal) -> {
                    Map<String, Object> specialiteData = formData.getOrDefault("specialite", new HashMap<>());
                    specialiteData.put("autre_specialite", newVal);
                    formData.put("specialite", specialiteData);
                });
                break;
                
            case "PNNSG":
                Label pnnsgLabel = new Label("Continuer le remplissage du formulaire");
                contentBox.getChildren().add(pnnsgLabel);
                break;
                
            // Les autres cas (Pilote/Méca Nav et Ingénieur sol) sont déjà traités dans les sections correspondantes
        }
    }
    
    /**
     * Classe pour représenter une maintenance.
     */
    public static class Maintenance {
        private final SimpleStringProperty operation;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty formation;
        private final SimpleStringProperty type; // "Programmée" ou "Curative"
        
        public Maintenance(String operation, LocalDate date, String formation, String type) {
            this.operation = new SimpleStringProperty(operation);
            this.date = new SimpleObjectProperty<>(date);
            this.formation = new SimpleStringProperty(formation);
            this.type = new SimpleStringProperty(type);
        }
        
        public String getOperation() { return operation.get(); }
        public LocalDate getDate() { return date.get(); }
        public String getFormation() { return formation.get(); }
        public String getType() { return type.get(); }
        
        public void setOperation(String operation) { this.operation.set(operation); }
        public void setDate(LocalDate date) { this.date.set(date); }
        public void setFormation(String formation) { this.formation.set(formation); }
        public void setType(String type) { this.type.set(type); }
    }
    
// ******************** Dotations Militaires ********************
    
    /**
     * Méthode appelée lorsque l'utilisateur clique sur le bouton "Dotations Militaires".
     * Affiche la première sous-section (Dotation 20 Mai).
     */
    @FXML
    private void showMilitaryDotation() {
        // Cette méthode est liée au bouton et gérée par handleMainButtonClick
        // Elle ne fait rien ici car la logique est dans navigateToSection
    }
    
    /**
     * Affiche le formulaire de gestion des dotations du 20 Mai.
     */
    @FXML
    private void showDotation20Mai() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        container.setPrefWidth(Double.MAX_VALUE);
        container.setMaxWidth(Double.MAX_VALUE);
        
        Label titleLabel = new Label("Dotation 20 Mai");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Spinner pour le nombre de dotations
        Label nombreDotationsLabel = new Label("Nombre de dotations 20 Mai perçues :");
        Spinner<Integer> nombreDotationsSpinner = new Spinner<>(0, 100, 1);
        nombreDotationsSpinner.setEditable(true);
        
        HBox spinnerBox = new HBox(10, nombreDotationsLabel, nombreDotationsSpinner);
        container.getChildren().add(spinnerBox);
        
        // Tableau pour afficher les dotations
        TableView<Dotation> dotationsTable = new TableView<>();
        dotationsTable.setPrefWidth(Double.MAX_VALUE);
        dotationsTable.setMaxWidth(Double.MAX_VALUE);
        dotationsTable.setMinHeight(300);
        dotationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<Dotation, Integer> anneeCol = new TableColumn<>("Année");
        anneeCol.setCellValueFactory(new PropertyValueFactory<>("annee"));
        
        TableColumn<Dotation, String> contenuCol = new TableColumn<>("Contenu");
        contenuCol.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        
        TableColumn<Dotation, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<Dotation, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        Dotation dotation = getTableView().getItems().get(getIndex());
                        showDotationEditDialog(dotation, dotationsTable, "20 Mai");
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Dotation dotation = getTableView().getItems().get(getIndex());
                        dotationsTable.getItems().remove(dotation);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        dotationsTable.getColumns().addAll(anneeCol, contenuCol, actionCol);
        
        anneeCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.15));
        contenuCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.70));
        
        actionCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.15));
        
        // Ajouter le tableau à la vue
        container.getChildren().add(dotationsTable);
        
        // Mettre à jour le nombre de dotations dans le spinner lorsque le tableau change
        dotationsTable.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends Dotation> c) -> {
            nombreDotationsSpinner.getValueFactory().setValue(dotationsTable.getItems().size());
        });
        
        // Ajouter un bouton pour ajouter une nouvelle dotation
        Button addButton = new Button("Ajouter une Dotation 20 Mai");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnAction(e -> showDotationAddDialog(dotationsTable, "20 Mai"));
        container.getChildren().add(addButton);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setAlignment(Pos.CENTER);
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> dotationsData = formData.getOrDefault("dotation_20_mai", new HashMap<>());
            
            // Stocker les valeurs
            dotationsData.put("matricule", currentMatricule);
            dotationsData.put("nombre_dotations", nombreDotationsSpinner.getValue());
            dotationsData.put("dotations", dotationsTable.getItems());
            
            // Mettre à jour la map globale
            formData.put("dotation_20_mai", dotationsData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des dotations 20 Mai ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche le formulaire de gestion des dotations particulières.
     */
    @FXML
    private void showDotationParticuliere() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        container.setPrefWidth(Double.MAX_VALUE);
        container.setMaxWidth(Double.MAX_VALUE);
        
        Label titleLabel = new Label("Dotation Particulière");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Checkbox pour n'avoir jamais reçu de dotation particulière
        CheckBox noDotationCheckBox = new CheckBox("Je n'ai jamais reçu une dotation particulière");
        container.getChildren().add(noDotationCheckBox);
        
        // VBox pour le contenu des dotations particulières
        VBox dotationContent = new VBox(15);
        container.getChildren().add(dotationContent);
        
        // Tableau pour afficher les dotations particulières
        TableView<DotationParticuliere> dotationsTable = new TableView<>();
        dotationsTable.setPrefWidth(Double.MAX_VALUE);
        dotationsTable.setMaxWidth(Double.MAX_VALUE);
        dotationsTable.setMinHeight(300);
        dotationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonnes du tableau
        TableColumn<DotationParticuliere, String> raisonCol = new TableColumn<>("Raison");
        raisonCol.setCellValueFactory(new PropertyValueFactory<>("raison"));
        
        TableColumn<DotationParticuliere, Integer> anneeCol = new TableColumn<>("Année");
        anneeCol.setCellValueFactory(new PropertyValueFactory<>("annee"));
        
        TableColumn<DotationParticuliere, String> moisCol = new TableColumn<>("Mois");
        moisCol.setCellValueFactory(new PropertyValueFactory<>("mois"));
        
        TableColumn<DotationParticuliere, String> contenuCol = new TableColumn<>("Contenu");
        contenuCol.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        
        TableColumn<DotationParticuliere, Button> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> {
            return new TableCell<DotationParticuliere, Button>() {
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    HBox hbox = new HBox(5, editButton, deleteButton);
                    
                    editButton.setOnAction(event -> {
                        DotationParticuliere dotation = getTableView().getItems().get(getIndex());
                        showDotationParticuliereEditDialog(dotation, dotationsTable);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        DotationParticuliere dotation = getTableView().getItems().get(getIndex());
                        dotationsTable.getItems().remove(dotation);
                    });
                    
                    setGraphic(hbox);
                }
                
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getGraphic());
                    }
                }
            };
        });
        
        // Ajouter les colonnes au tableau
        dotationsTable.getColumns().addAll(raisonCol, anneeCol, moisCol, contenuCol, actionCol);
        
        raisonCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.15));
        anneeCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.15));
        contenuCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.55));
        
        actionCol.prefWidthProperty().bind(dotationsTable.widthProperty().multiply(0.15));
        
        // Ajouter le tableau à la vue
        dotationContent.getChildren().add(dotationsTable);
        
        // Ajouter un bouton pour ajouter une nouvelle dotation particulière
        Button addButton = new Button("Ajouter une Dotation Particulière");
        addButton.setOnAction(e -> showDotationParticuliereAddDialog(dotationsTable));
        dotationContent.getChildren().add(addButton);
        
        // Configurer le comportement de la checkbox
        noDotationCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            dotationContent.setDisable(newVal);
            dotationContent.setVisible(!newVal);
        });
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> dotationsData = formData.getOrDefault("dotation_particuliere", new HashMap<>());
            
            // Stocker les valeurs
            dotationsData.put("matricule", currentMatricule);
            dotationsData.put("jamais_recu", noDotationCheckBox.isSelected());
            
            if (!noDotationCheckBox.isSelected()) {
                dotationsData.put("dotations", dotationsTable.getItems());
            }
            
            // Mettre à jour la map globale
            formData.put("dotation_particuliere", dotationsData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des dotations particulières ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche le formulaire de gestion des paramètres corporels.
     */
    @FXML
    private void showParametresCorporels() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Paramètres Corporels");
        titleLabel.getStyleClass().add("form-title");
        container.getChildren().add(titleLabel);
        
        // Création des champs de formulaire
        Spinner<Integer> contourTeteSpinner = new Spinner<>(30, 100, 50);
        contourTeteSpinner.setEditable(true);
        
        Spinner<Integer> pointureSpinner = new Spinner<>(20, 50, 40);
        pointureSpinner.setEditable(true);
        
        Spinner<Integer> tourHancheSpinner = new Spinner<>(50, 200, 80);
        tourHancheSpinner.setEditable(true);
        
        Spinner<Integer> tourPoignetSpinner = new Spinner<>(10, 50, 15);
        tourPoignetSpinner.setEditable(true);
        
        ComboBox<String> tailleCombo = createComboBox("XS", "S", "M", "L", "XL", "XXL", "XXXL");
        
        // Création du formulaire
        VBox form = new VBox(15);
        form.getChildren().addAll(
            createFormField("Contour de tête (en cm)", contourTeteSpinner),
            createFormField("Pointure de chaussure (système EU)", pointureSpinner),
            createFormField("Tour de hanche (en cm)", tourHancheSpinner),
            createFormField("Tour du poignet (en cm)", tourPoignetSpinner),
            createFormField("Taille", tailleCombo)
        );
        
        container.getChildren().add(form);
        
        // Ajouter un bouton pour sauvegarder les données
        Button saveButton = new Button("Enregistrer");
        saveButton.setOnAction(e -> {
            // Créer ou récupérer la map pour cette section
            Map<String, Object> parametresData = formData.getOrDefault("parametres_corporels", new HashMap<>());
            
            // Stocker les valeurs
            parametresData.put("matricule", currentMatricule);
            parametresData.put("contour_tete", contourTeteSpinner.getValue());
            parametresData.put("pointure", pointureSpinner.getValue());
            parametresData.put("tour_hanche", tourHancheSpinner.getValue());
            parametresData.put("tour_poignet", tourPoignetSpinner.getValue());
            parametresData.put("taille", tailleCombo.getValue());
            
            // Mettre à jour la map globale
            formData.put("parametres_corporels", parametresData);
            
            // Afficher une confirmation
            showSuccessMessage("Les informations des paramètres corporels ont été enregistrées.");
        });
        container.getChildren().add(saveButton);
        
        // Ajouter au contentArea
        contentArea.getChildren().add(container);
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle dotation.
     * 
     * @param dotationsTable Le tableau des dotations
     * @param type Le type de dotation
     */
    private void showDotationAddDialog(TableView<Dotation> dotationsTable, String type) {
        Dialog<Dotation> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Dotation " + type);
        dialog.setHeaderText("Ajouter une dotation à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1900, LocalDate.now().getYear(), LocalDate.now().getYear());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        // Liste des items disponibles pour les dotations
        ObservableList<String> dotationItems = FXCollections.observableArrayList(
            "Rangers", "Chaussettes rangers", "Camouflés", "Ceinture du camouflé",
            "Paire basse", "Tenue claire", "Ceinture de la tenue bleue", "Bérêt", "Insigne de bérêt"
        );
        
        // ListView pour sélectionner les items (sélection multiple)
        ListView<String> itemsList = new ListView<>(dotationItems);
        itemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsList.setPrefHeight(200);
        
        // Champ pour ajouter un nouvel item
        TextField newItemField = new TextField();
        newItemField.setPromptText("Ajouter un nouvel item");
        
        Button addItemBtn = new Button("Ajouter Item");
        addItemBtn.setOnAction(event -> {
            String newItem = newItemField.getText().trim();
            if (!newItem.isEmpty() && !dotationItems.contains(newItem)) {
                dotationItems.add(newItem);
                newItemField.clear();
            }
        });
        
        HBox itemAddBox = new HBox(10, newItemField, addItemBtn);
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Année de dotation", anneeSpinner),
            new Label("Contenu de la dotation :"),
            itemsList,
            itemAddBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Dotation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Récupérer les items sélectionnés
                ObservableList<String> selectedItems = itemsList.getSelectionModel().getSelectedItems();
                String contenu = String.join(", ", selectedItems);
                
                return new Dotation(
                    anneeSpinner.getValue(),
                    contenu
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Dotation> result = dialog.showAndWait();
        
        // Ajouter la nouvelle dotation au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(dotation -> {
            dotationsTable.getItems().add(dotation);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une dotation.
     * 
     * @param dotation La dotation à modifier
     * @param dotationsTable Le tableau des dotations
     * @param type Le type de dotation
     */
    private void showDotationEditDialog(Dotation dotation, TableView<Dotation> dotationsTable, String type) {
        Dialog<Dotation> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Dotation " + type);
        dialog.setHeaderText("Modifier les informations de la dotation");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1900, LocalDate.now().getYear(), dotation.getAnnee());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        // Liste des items disponibles pour les dotations
        ObservableList<String> dotationItems = FXCollections.observableArrayList(
            "Rangers", "Chaussettes rangers", "Camouflés", "Ceinture du camouflé",
            "Paire basse", "Tenue claire", "Ceinture de la tenue bleue", "Bérêt", "Insigne de bérêt"
        );
        
        // Ajouter les items actuels de la dotation s'ils ne sont pas déjà dans la liste
        List<String> currentItems = Arrays.asList(dotation.getContenu().split(", "));
        for (String item : currentItems) {
            if (!dotationItems.contains(item) && !item.isEmpty()) {
                dotationItems.add(item);
            }
        }
        
        // ListView pour sélectionner les items (sélection multiple)
        ListView<String> itemsList = new ListView<>(dotationItems);
        itemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsList.setPrefHeight(200);
        
        // Sélectionner les items actuels
        for (String item : currentItems) {
            if (dotationItems.contains(item)) {
                itemsList.getSelectionModel().select(item);
            }
        }
        
        // Champ pour ajouter un nouvel item
        TextField newItemField = new TextField();
        newItemField.setPromptText("Ajouter un nouvel item");
        
        Button addItemBtn = new Button("Ajouter Item");
        addItemBtn.setOnAction(event -> {
            String newItem = newItemField.getText().trim();
            if (!newItem.isEmpty() && !dotationItems.contains(newItem)) {
                dotationItems.add(newItem);
                newItemField.clear();
            }
        });
        
        HBox itemAddBox = new HBox(10, newItemField, addItemBtn);
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Année de dotation", anneeSpinner),
            new Label("Contenu de la dotation :"),
            itemsList,
            itemAddBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet Dotation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Récupérer les items sélectionnés
                ObservableList<String> selectedItems = itemsList.getSelectionModel().getSelectedItems();
                String contenu = String.join(", ", selectedItems);
                
                return new Dotation(
                    anneeSpinner.getValue(),
                    contenu
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<Dotation> result = dialog.showAndWait();
        
        // Mettre à jour la dotation si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedDotation -> {
            int index = dotationsTable.getItems().indexOf(dotation);
            dotationsTable.getItems().set(index, updatedDotation);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour ajouter une nouvelle dotation particulière.
     * 
     * @param dotationsTable Le tableau des dotations particulières
     */
    private void showDotationParticuliereAddDialog(TableView<DotationParticuliere> dotationsTable) {
        Dialog<DotationParticuliere> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Dotation Particulière");
        dialog.setHeaderText("Ajouter une dotation particulière à la liste");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField raisonField = new TextField();
        
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1900, LocalDate.now().getYear(), LocalDate.now().getYear());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        ComboBox<String> moisCombo = createComboBox(
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", 
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        );
        
        // Liste des items disponibles pour les dotations
        ObservableList<String> dotationItems = FXCollections.observableArrayList(
            "Rangers", "Chaussettes rangers", "Camouflés", "Ceinture du camouflé",
            "Paire basse", "Tenue claire", "Ceinture de la tenue bleue", "Bérêt", "Insigne de bérêt"
        );
        
        // ListView pour sélectionner les items (sélection multiple)
        ListView<String> itemsList = new ListView<>(dotationItems);
        itemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsList.setPrefHeight(200);
        
        // Champ pour ajouter un nouvel item
        TextField newItemField = new TextField();
        newItemField.setPromptText("Ajouter un nouvel item");
        
        Button addItemBtn = new Button("Ajouter Item");
        addItemBtn.setOnAction(event -> {
            String newItem = newItemField.getText().trim();
            if (!newItem.isEmpty() && !dotationItems.contains(newItem)) {
                dotationItems.add(newItem);
                newItemField.clear();
            }
        });
        
        HBox itemAddBox = new HBox(10, newItemField, addItemBtn);
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Raison de la dotation", raisonField),
            createFormField("Année de dotation", anneeSpinner),
            createFormField("Mois de dotation", moisCombo),
            new Label("Contenu de la dotation :"),
            itemsList,
            itemAddBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet DotationParticuliere
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Récupérer les items sélectionnés
                ObservableList<String> selectedItems = itemsList.getSelectionModel().getSelectedItems();
                String contenu = String.join(", ", selectedItems);
                
                return new DotationParticuliere(
                    raisonField.getText(),
                    anneeSpinner.getValue(),
                    moisCombo.getValue(),
                    contenu
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<DotationParticuliere> result = dialog.showAndWait();
        
        // Ajouter la nouvelle dotation au tableau si l'utilisateur a cliqué sur Ajouter
        result.ifPresent(dotation -> {
            dotationsTable.getItems().add(dotation);
        });
    }
    
    /**
     * Affiche une boîte de dialogue pour modifier une dotation particulière.
     * 
     * @param dotation La dotation à modifier
     * @param dotationsTable Le tableau des dotations particulières
     */
    private void showDotationParticuliereEditDialog(DotationParticuliere dotation, TableView<DotationParticuliere> dotationsTable) {
        Dialog<DotationParticuliere> dialog = new Dialog<>();
        dialog.setTitle("Modifier une Dotation Particulière");
        dialog.setHeaderText("Modifier les informations de la dotation particulière");
        
        // Ajouter les boutons OK et Annuler
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Créer les champs de formulaire
        TextField raisonField = new TextField(dotation.getRaison());
        
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1900, LocalDate.now().getYear(), dotation.getAnnee());
        Spinner<Integer> anneeSpinner = new Spinner<>();
        anneeSpinner.setValueFactory(valueFactory);
        anneeSpinner.setEditable(true);
        
        ComboBox<String> moisCombo = createComboBox(
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", 
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        );
        moisCombo.setValue(dotation.getMois());
        
        // Liste des items disponibles pour les dotations
        ObservableList<String> dotationItems = FXCollections.observableArrayList(
            "Rangers", "Chaussettes rangers", "Camouflés", "Ceinture du camouflé",
            "Paire basse", "Tenue claire", "Ceinture de la tenue bleue", "Bérêt", "Insigne de bérêt"
        );
        
        // Ajouter les items actuels de la dotation s'ils ne sont pas déjà dans la liste
        List<String> currentItems = Arrays.asList(dotation.getContenu().split(", "));
        for (String item : currentItems) {
            if (!dotationItems.contains(item) && !item.isEmpty()) {
                dotationItems.add(item);
            }
        }
        
        // ListView pour sélectionner les items (sélection multiple)
        ListView<String> itemsList = new ListView<>(dotationItems);
        itemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsList.setPrefHeight(200);
        
        // Sélectionner les items actuels
        for (String item : currentItems) {
            if (dotationItems.contains(item)) {
                itemsList.getSelectionModel().select(item);
            }
        }
        
        // Champ pour ajouter un nouvel item
        TextField newItemField = new TextField();
        newItemField.setPromptText("Ajouter un nouvel item");
        
        Button addItemBtn = new Button("Ajouter Item");
        addItemBtn.setOnAction(event -> {
            String newItem = newItemField.getText().trim();
            if (!newItem.isEmpty() && !dotationItems.contains(newItem)) {
                dotationItems.add(newItem);
                newItemField.clear();
            }
        });
        
        HBox itemAddBox = new HBox(10, newItemField, addItemBtn);
        
        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createFormField("Raison de la dotation", raisonField),
            createFormField("Année de dotation", anneeSpinner),
            createFormField("Mois de dotation", moisCombo),
            new Label("Contenu de la dotation :"),
            itemsList,
            itemAddBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convertir le résultat en objet DotationParticuliere
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Récupérer les items sélectionnés
                ObservableList<String> selectedItems = itemsList.getSelectionModel().getSelectedItems();
                String contenu = String.join(", ", selectedItems);
                
                return new DotationParticuliere(
                    raisonField.getText(),
                    anneeSpinner.getValue(),
                    moisCombo.getValue(),
                    contenu
                );
            }
            return null;
        });
        
        // Attendre le résultat de la boîte de dialogue
        Optional<DotationParticuliere> result = dialog.showAndWait();
        
        // Mettre à jour la dotation si l'utilisateur a cliqué sur Enregistrer
        result.ifPresent(updatedDotation -> {
            int index = dotationsTable.getItems().indexOf(dotation);
            dotationsTable.getItems().set(index, updatedDotation);
        });
    }
    
    /**
     * Classe pour représenter une dotation.
     */
    public static class Dotation {
        private final SimpleObjectProperty<Integer> annee;
        private final SimpleStringProperty contenu;
        
        public Dotation(Integer annee, String contenu) {
            this.annee = new SimpleObjectProperty<>(annee);
            this.contenu = new SimpleStringProperty(contenu);
        }
        
        public Integer getAnnee() { return annee.get(); }
        public String getContenu() { return contenu.get(); }
        
        public void setAnnee(Integer annee) { this.annee.set(annee); }
        public void setContenu(String contenu) { this.contenu.set(contenu); }
    }
    
    /**
     * Classe pour représenter une dotation particulière.
     */
    public static class DotationParticuliere {
        private final SimpleStringProperty raison;
        private final SimpleObjectProperty<Integer> annee;
        private final SimpleStringProperty mois;
        private final SimpleStringProperty contenu;
        
        public DotationParticuliere(String raison, Integer annee, String mois, String contenu) {
            this.raison = new SimpleStringProperty(raison);
            this.annee = new SimpleObjectProperty<>(annee);
            this.mois = new SimpleStringProperty(mois);
            this.contenu = new SimpleStringProperty(contenu);
        }
        
        public String getRaison() { return raison.get(); }
        public Integer getAnnee() { return annee.get(); }
        public String getMois() { return mois.get(); }
        public String getContenu() { return contenu.get(); }
        
        public void setRaison(String raison) { this.raison.set(raison); }
        public void setAnnee(Integer annee) { this.annee.set(annee); }
        public void setMois(String mois) { this.mois.set(mois); }
        public void setContenu(String contenu) { this.contenu.set(contenu); }
    }
    
// ******************** Méthodes de navigation pour les boutons du bas ********************
    
    /**
     * Gère le clic sur le bouton Précédent.
     * Navigue vers la section précédente.
     */
    @FXML
    private void handlePrevious() {
        if (!navigationHistory.isEmpty()) {
            NavigationHistoryItem previous = navigationHistory.remove(navigationHistory.size() - 1);
            int prevMainSection = previous.getMainSection();
            int prevSubSection = previous.getSubSection();
            
            // Mise à jour des indices de section actuelle sans ajouter à l'historique
            currentMainSection = prevMainSection;
            currentSubSection = prevSubSection;
            
            // Mettre à jour l'état actif des boutons
            updateActiveButtons();
            
            // Afficher le contenu de la section précédente
            String sectionName = mainSections[prevMainSection];
            
            // Vérifier si c'est une section avec sous-menus
            if (subSections.containsKey(sectionName) && prevSubSection >= 0) {
                // Ouvrir le sous-menu si nécessaire
                VBox subMenu = subMenus.get(sectionName);
                if (!subMenu.isVisible()) {
                    closeAllSubMenus();
                    toggleSubMenu(subMenu);
                }
            }
            
            // Naviguer vers la section précédente
            navigateToSection(prevMainSection, prevSubSection);
        }
    }
    
    /**
     * Gère le clic sur le bouton Suivant.
     * Navigue vers la section suivante.
     */
    @FXML
    private void handleNext() {
        // Déterminer la section suivante
        int nextMainSection = currentMainSection;
        int nextSubSection = currentSubSection + 1;
        
        String currentSectionName = mainSections[currentMainSection];
        
        // Vérifier si nous devons passer à la sous-section suivante ou à la section principale suivante
        if (subSections.containsKey(currentSectionName)) {
            String[] currentSubSectionNames = subSections.get(currentSectionName);
            
            if (nextSubSection >= currentSubSectionNames.length) {
                // Passer à la section principale suivante
                nextMainSection++;
                nextSubSection = 0;
            }
        } else {
            // Passer à la section principale suivante
            nextMainSection++;
            nextSubSection = 0;
        }
        
        // Vérifier que nous sommes dans les limites
        if (nextMainSection < mainSections.length) {
            String nextSectionName = mainSections[nextMainSection];
            
            // Vérifier si la section suivante a des sous-sections
            if (subSections.containsKey(nextSectionName)) {
                // Fermer tous les sous-menus et ouvrir celui de la section suivante
                closeAllSubMenus();
                toggleSubMenu(subMenus.get(nextSectionName));
                
                // Naviguer vers la première sous-section
                navigateToSection(nextMainSection, nextSubSection);
            } else {
                // Fermer tous les sous-menus
                closeAllSubMenus();
                
                // Naviguer vers la section principale sans sous-section
                navigateToSection(nextMainSection, -1);
            }
        }
    }
    
    /**
     * Gère le clic sur le bouton Soumettre.
     * Sauvegarde toutes les données dans la base de données.
     */
    @FXML
    private void handleSubmit() {
        // Vérifier que le matricule d'incorporation est défini
        if (currentMatricule == null || currentMatricule.isEmpty()) {
            showError("Le matricule d'incorporation n'est pas défini.");
            return;
        }
        
        // Vérifier que certaines informations essentielles sont présentes
        if (!formData.containsKey("identite_personnelle")) {
            showError("Les informations d'identité personnelle sont requises.");
            return;
        }
        
        // NOUVELLE VÉRIFICATION D'IDENTITÉ
        if (!verifyUserIdentity()) {
            return; // Arrêter si l'authentification échoue
        }
        
        // Confirmer avec l'utilisateur
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation");
        confirmDialog.setHeaderText("Enregistrement des données");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir enregistrer toutes les données dans la base de données ?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Obtenir le nom d'utilisateur actuel depuis les préférences système
                Preferences prefs = Preferences.userNodeForPackage(AjoutPersonnelController.class);
                String currentUser = prefs.get("username", "utilisateur");
                // Enregistrer toutes les données dans la base de données
                saveAllData();
                
             // Récupérer le nom et prénom du personnel pour l'historique
                String nom = "";
                String prenom = "";
                
                if (formData.containsKey("identite_personnelle")) {
                    Map<String, Object> identiteData = formData.get("identite_personnelle");
                    nom = (String) identiteData.getOrDefault("nom", "");
                    prenom = (String) identiteData.getOrDefault("prenom", "");
                }
                
                // Construire le détail de l'action pour l'historique
                String detailAction = String.format("Ajout du personnel %s (%s %s)", 
                                       currentMatricule, nom.toUpperCase(), prenom);
                
                // Enregistrer l'action dans l'historique
                historiqueController.enregistrerAction(
                    historiqueController.ACTION_AJOUT,
                    historiqueController.CIBLE_PERSONNEL,
                    detailAction,
                    currentUser
                );
                
                // Afficher un message de succès
                Alert successDialog = new Alert(AlertType.INFORMATION);
                successDialog.setTitle("Succès");
                successDialog.setHeaderText("Enregistrement réussi");
                successDialog.setContentText("Les données ont été enregistrées avec succès dans la base de données.");
                successDialog.showAndWait();
                
                // Réinitialiser le formulaire pour un nouvel enregistrement
                resetForm();
            } catch (Exception e) {
                e.printStackTrace();
                
                // Afficher un message d'erreur
                showError("Une erreur s'est produite lors de l'enregistrement des données :\n" + e.getMessage());
            }
        }
    }
    
    
    /**
     * Gère le clic sur le bouton Annuler.
     * Retourne à l'écran d'accueil.
     */
    @FXML
    private void handleBack() {
        // Demander confirmation à l'utilisateur
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation");
        confirmDialog.setHeaderText("Annuler l'enregistrement");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir annuler ? Les données non enregistrées seront perdues.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Charger l'écran d'accueil
                FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1024, 768);
                scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
                
                Stage stage = (Stage) backBtn.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Accueil");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Une erreur s'est produite lors du chargement de l'écran d'accueil :\n" + e.getMessage());
            }
        }
    }
    

    
    // ******************** Méthodes utilitaires ********************
    
    /**
     * Valide les données du formulaire avant l'enregistrement.
     * 
     * @return true si les données sont valides, false sinon
     */
    private boolean validateFormData() {
        StringBuilder errors = new StringBuilder();
        
        // Vérifier que le matricule est défini et non vide
        if (currentMatricule == null || currentMatricule.trim().isEmpty()) {
            errors.append("- Le matricule d'incorporation est obligatoire.\n");
        }
        
        // Vérifier les informations d'identité personnelle
        if (!formData.containsKey("identite_personnelle")) {
            errors.append("- Les informations d'identité personnelle sont obligatoires.\n");
        } else {
            Map<String, Object> identiteData = formData.get("identite_personnelle");
            if (identiteData.get("nom") == null || identiteData.get("nom").toString().trim().isEmpty()) {
                errors.append("- Le nom est obligatoire.\n");
            }
            if (identiteData.get("prenom") == null || identiteData.get("prenom").toString().trim().isEmpty()) {
                errors.append("- Le prénom est obligatoire.\n");
            }
        }
        
        // S'il y a des erreurs, les afficher
        if (errors.length() > 0) {
            showError("Validation échouée :\n" + errors.toString());
            return false;
        }
        
        return true;
    }
    
    /**
     * Charge les données d'un personnel existant pour modification.
     * Cette méthode doit être ajoutée dans la classe AjoutPersonnelController.
     * 
     * @param matricule Le matricule du personnel à charger
     */
    public void loadPersonnelForModification(String matricule) {
        if (matricule == null || matricule.trim().isEmpty()) {
            showError("Matricule invalide pour le chargement des données.");
            return;
        }
        
        try {
            Connection connection = getConnection();
            
            // Charger toutes les données
            loadIdentitePersonnelle(connection, matricule);
            loadIdentiteSociale(connection, matricule);
            loadIdentiteCulturelle(connection, matricule);
            loadGradeActuel(connection, matricule);
            loadHistoriqueGrades(connection, matricule);
            loadFormationActuelle(connection, matricule);
            loadHistoriquePostes(connection, matricule);
            loadEcoleFormationInitiale(connection, matricule);
            loadEcolesCiviles(connection, matricule);
            loadEcolesMilitaires(connection, matricule);
            loadOperationsInterieures(connection, matricule);
            loadOperationsExterieures(connection, matricule);
            loadDecorations(connection, matricule);
            loadMedailles(connection, matricule);
            loadPunitions(connection, matricule);
            loadLangues(connection, matricule);
            loadInfosSpecifiquesGeneral(connection, matricule);
            loadPersonnelNaviguant(connection, matricule);
            loadMaintenancesProgrammees(connection, matricule);
            loadMaintenancesCuratives(connection, matricule);
            loadSpecialite(connection, matricule);
            loadDotation20Mai(connection, matricule);
            loadDotationParticuliere(connection, matricule);
            loadParametresCorporels(connection, matricule);
            
            connection.close();
            
            // Définir le matricule courant
            currentMatricule = matricule;
            if (matriculeLabel != null) {
                matriculeLabel.setText(matricule);
            }
            
            // Pré-remplir l'interface utilisateur
            preloadUserInterfaceData();
            
            System.out.println("Données complètes chargées pour le personnel: " + matricule);
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de charger les données du personnel: " + e.getMessage());
        }
    }
    
    /**
     * Pré-remplit l'interface utilisateur avec les données chargées.
     */
    private void preloadUserInterfaceData() {
        // Naviguer vers la première section pour déclencher le pré-remplissage
        navigateToSection(0, 0);
        
        // Attendre que l'interface soit initialisée et pré-remplir les données
        Platform.runLater(() -> {
            try {
                preloadIdentitePersonnelle();
                preloadIdentiteSociale();
                preloadIdentiteCulturelle();
                preloadGradeInfo();
                preloadFormationInfo();
                preloadEcolesInfo();
                preloadOperationsInfo();
                preloadDecorationsInfo();
                preloadPunitionsInfo();
                preloadLanguesInfo();
                preloadInfosSpecifiquesInfo();
                preloadDotationsInfo();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Erreur lors du pré-remplissage de l'interface: " + e.getMessage());
            }
        });
    }

    /**
     * Pré-remplit les données d'identité personnelle.
     */
    private void preloadIdentitePersonnelle() {
        Map<String, Object> data = formData.get("identite_personnelle");
        if (data == null) return;
        
        // Cette méthode sera appelée quand l'utilisateur navigue vers cette section
        // Les données seront automatiquement pré-remplies via les listeners existants
    }

    /**
     * Pré-remplit les données d'identité sociale.
     */
    private void preloadIdentiteSociale() {
        Map<String, Object> data = formData.get("identite_sociale");
        if (data == null) return;
        
        // Les données seront pré-remplies automatiquement
    }

    /**
     * Pré-remplit les données d'identité culturelle.
     */
    private void preloadIdentiteCulturelle() {
        Map<String, Object> data = formData.get("identite_culturelle");
        if (data == null) return;
        
        // Les données seront pré-remplies automatiquement
    }

    /**
     * Pré-remplit les informations de grade.
     */
    private void preloadGradeInfo() {
        // Les données de grade sont déjà chargées dans formData
        // Elles seront affichées automatiquement quand l'utilisateur navigue vers cette section
    }

    /**
     * Pré-remplit les informations de formation.
     */
    private void preloadFormationInfo() {
        // Les données de formation sont déjà chargées dans formData
        // Elles seront affichées automatiquement quand l'utilisateur navigue vers cette section
    }

    /**
     * Pré-remplit les informations des écoles.
     */
    private void preloadEcolesInfo() {
        // Les données des écoles sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations des opérations.
     */
    private void preloadOperationsInfo() {
        // Les données des opérations sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations des décorations.
     */
    private void preloadDecorationsInfo() {
        // Les données des décorations sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations des punitions.
     */
    private void preloadPunitionsInfo() {
        // Les données des punitions sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations des langues.
     */
    private void preloadLanguesInfo() {
        // Les données des langues sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations spécifiques.
     */
    private void preloadInfosSpecifiquesInfo() {
        // Les données spécifiques sont déjà chargées dans formData
    }

    /**
     * Pré-remplit les informations des dotations.
     */
    private void preloadDotationsInfo() {
        // Les données des dotations sont déjà chargées dans formData
    }


    
    /**
     * Charge les données d'identité personnelle.
     */
    private void loadIdentitePersonnelle(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM identite_personnelle WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", rs.getString("matricule"));
                    data.put("nom", rs.getString("nom"));
                    data.put("prenom", rs.getString("prenom"));
                    data.put("lieu_naissance", rs.getString("lieu_naissance"));
                    data.put("date_naissance", rs.getDate("date_naissance") != null ? rs.getDate("date_naissance").toLocalDate() : null);
                    data.put("telephone", rs.getString("telephone"));
                    data.put("sexe", rs.getString("sexe"));
                    data.put("groupe_sanguin", rs.getString("groupe_sanguin"));
                    data.put("photo_path", rs.getString("photo_path"));
                    
                    formData.put("identite_personnelle", data);
                }
            }
        }
    }

    /**
     * Charge les données d'identité sociale.
     */
    private void loadIdentiteSociale(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM identite_sociale WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", rs.getString("matricule"));
                    data.put("nom_pere", rs.getString("nom_pere"));
                    data.put("nom_mere", rs.getString("nom_mere"));
                    data.put("nombre_conjoints", rs.getInt("nombre_conjoints"));
                    data.put("nombre_enfants", rs.getInt("nombre_enfants"));
                    data.put("personne_contact", rs.getString("personne_contact"));
                    data.put("lien_personne_contact", rs.getString("lien_personne_contact"));
                    data.put("telephone_personne_contact", rs.getString("telephone_personne_contact"));
                    data.put("lieu_residence_personne_contact", rs.getString("lieu_residence_personne_contact"));
                    data.put("remarque_particuliere", rs.getString("remarque_particuliere"));
                    data.put("regime_matrimonial", rs.getString("regime_matrimonial"));
                    data.put("nom_conjoint", rs.getString("nom_conjoint"));
                    
                    formData.put("identite_sociale", data);
                }
            }
        }
    }

    /**
     * Charge les données d'identité culturelle.
     */
    private void loadIdentiteCulturelle(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM identite_culturelle WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", rs.getString("matricule"));
                    data.put("region_origine", rs.getString("region_origine"));
                    data.put("departement_origine", rs.getString("departement_origine"));
                    data.put("arrondissement_origine", rs.getString("arrondissement_origine"));
                    data.put("village", rs.getString("village"));
                    data.put("ethnie", rs.getString("ethnie"));
                    data.put("religion", rs.getString("religion"));
                    
                    formData.put("identite_culturelle", data);
                }
            }
        }
    }

    /**
     * Charge le grade actuel.
     */
    private void loadGradeActuel(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM grade_actuel WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Grade grade = new Grade(
                        rs.getString("rang"),
                        rs.getString("echelon"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("reference"),
                        rs.getString("echelon_grade"),
                        rs.getString("reference_echelon"),
                        rs.getDate("date_echelon") != null ? rs.getDate("date_echelon").toLocalDate() : LocalDate.now(),
                        rs.getString("statut")
                    );
                    
                    Map<String, Object> gradeData = new HashMap<>();
                    gradeData.put("matricule", matricule);
                    gradeData.put("grade", grade);
                    
                    formData.put("grade_actuel", gradeData);
                }
            }
        }
    }

    /**
     * Charge l'historique des grades.
     */
    private void loadHistoriqueGrades(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM historique_grades WHERE matricule = ? ORDER BY date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Grade> grades = new ArrayList<>();
                
                while (rs.next()) {
                    Grade grade = new Grade(
                        rs.getString("rang"),
                        rs.getString("echelon"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("reference"),
                        rs.getString("echelon_grade"),
                        rs.getString("reference_echelon"),
                        rs.getDate("date_echelon") != null ? rs.getDate("date_echelon").toLocalDate() : LocalDate.now(),
                        rs.getString("statut")
                    );
                    grades.add(grade);
                }
                
                Map<String, Object> gradesData = new HashMap<>();
                gradesData.put("matricule", matricule);
                gradesData.put("grades", grades);
                
                formData.put("grades_historique", gradesData);
            }
        }
    }

    /**
     * Charge la formation actuelle.
     */
    private void loadFormationActuelle(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM formation_actuelle WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FormationActuelle formation = new FormationActuelle(
                        rs.getString("formation"),
                        rs.getDate("date_affectation") != null ? rs.getDate("date_affectation").toLocalDate() : LocalDate.now(),
                        rs.getString("reference_affectation"),
                        rs.getString("unite"),
                        rs.getString("poste")
                    );
                    
                    Map<String, Object> formationData = new HashMap<>();
                    formationData.put("matricule", matricule);
                    formationData.put("formation_obj", formation);
                    formationData.put("formation", formation.getFormation());
                    formationData.put("date_affectation", formation.getDateAffectation());
                    formationData.put("reference_affectation", formation.getReference());
                    formationData.put("unite", formation.getUnite());
                    formationData.put("poste", formation.getPoste());
                    
                    formData.put("formation_actuelle", formationData);
                }
            }
        }
    }

    /**
     * Charge l'historique des postes.
     */
    private void loadHistoriquePostes(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM historique_postes WHERE matricule = ? ORDER BY date_debut DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<PosteHistorique> postes = new ArrayList<>();
                
                while (rs.next()) {
                    PosteHistorique poste = new PosteHistorique(
                        rs.getString("formation"),
                        rs.getString("unite"),
                        rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : LocalDate.now(),
                        rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : LocalDate.now(),
                        rs.getString("poste")
                    );
                    postes.add(poste);
                }
                
                Map<String, Object> postesData = new HashMap<>();
                postesData.put("matricule", matricule);
                postesData.put("postes", postes);
                
                formData.put("historique_postes", postesData);
            }
        }
    }

    /**
     * Charge l'école de formation initiale.
     */
    private void loadEcoleFormationInitiale(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM ecole_formation_initiale WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", matricule);
                    data.put("nom_ecole", rs.getString("nom_ecole"));
                    data.put("pays", rs.getString("pays"));
                    data.put("region", rs.getString("region"));
                    data.put("date_entree", rs.getDate("date_entree") != null ? rs.getDate("date_entree").toLocalDate() : null);
                    data.put("date_sortie", rs.getDate("date_sortie") != null ? rs.getDate("date_sortie").toLocalDate() : null);
                    data.put("temps_mis", rs.getString("temps_mis"));
                    data.put("diplome_obtenu", rs.getString("diplome_obtenu"));
                    
                    formData.put("ecole_formation_initiale", data);
                }
            }
        }
    }

    /**
     * Charge les écoles civiles.
     */
    private void loadEcolesCiviles(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM ecole_civile WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<EcoleCivile> ecoles = new ArrayList<>();
                
                while (rs.next()) {
                    EcoleCivile ecole = new EcoleCivile(
                        rs.getString("nom_ecole"),
                        rs.getString("pays"),
                        rs.getString("region"),
                        rs.getString("reference"),
                        rs.getString("diplome_obtenu"),
                        rs.getString("appreciation")
                    );
                    ecoles.add(ecole);
                }
                
                Map<String, Object> ecolesData = new HashMap<>();
                ecolesData.put("matricule", matricule);
                ecolesData.put("ecoles", ecoles);
                
                formData.put("ecoles_civiles", ecolesData);
            }
        }
    }

    /**
     * Charge les écoles militaires.
     */
    private void loadEcolesMilitaires(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM ecole_militaire WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<EcoleMilitaire> ecoles = new ArrayList<>();
                
                while (rs.next()) {
                    EcoleMilitaire ecole = new EcoleMilitaire(
                        rs.getString("nom_ecole"),
                        rs.getString("pays"),
                        rs.getString("type"),
                        rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
                        rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
                        rs.getString("temps_mis"),
                        rs.getString("diplome_obtenu"),
                        rs.getString("reference")
                    );
                    ecoles.add(ecole);
                }
                
                Map<String, Object> ecolesData = new HashMap<>();
                ecolesData.put("matricule", matricule);
                ecolesData.put("nombre_ecoles", ecoles.size());
                ecolesData.put("ecoles", ecoles);
                
                formData.put("ecoles_militaires", ecolesData);
            }
        }
    }

    /**
     * Charge les opérations intérieures.
     */
    private void loadOperationsInterieures(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM operation WHERE matricule = ? AND type = 'Intérieure'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Operation> operations = new ArrayList<>();
                
                while (rs.next()) {
                    Operation operation = new Operation(
                        rs.getString("nom_mission"),
                        rs.getInt("annee"),
                        rs.getString("lieu"),
                        "Intérieure"
                    );
                    operations.add(operation);
                }
                
                Map<String, Object> operationsData = new HashMap<>();
                operationsData.put("matricule", matricule);
                operationsData.put("operations", operations);
                
                formData.put("operations_interieures", operationsData);
            }
        }
    }

    /**
     * Charge les opérations extérieures.
     */
    private void loadOperationsExterieures(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM operation WHERE matricule = ? AND type = 'Extérieure'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Operation> operations = new ArrayList<>();
                
                while (rs.next()) {
                    Operation operation = new Operation(
                        rs.getString("nom_mission"),
                        rs.getInt("annee"),
                        rs.getString("lieu"),
                        "Extérieure"
                    );
                    operations.add(operation);
                }
                
                Map<String, Object> operationsData = new HashMap<>();
                operationsData.put("matricule", matricule);
                operationsData.put("operations", operations);
                
                formData.put("operations_exterieures", operationsData);
            }
        }
    }

    /**
     * Charge les décorations.
     */
    private void loadDecorations(Connection connection, String matricule) throws SQLException {
        // Charger les ordres nationaux
        loadDecorationsParType(connection, matricule, "Ordres Nationaux", "decorations_ordres_nationaux");
        
        // Charger les ordres du mérite camerounais
        loadDecorationsParType(connection, matricule, "Ordres du Mérite Camerounais", "decorations_ordres_du_merite_camerounais");
        
        // Charger l'ordre du mérite sportif
        loadDecorationsParType(connection, matricule, "Ordre du Mérite Sportif", "decorations_ordre_du_merite_sportif");
    }

    /**
     * Charge les décorations par type.
     */
    private void loadDecorationsParType(Connection connection, String matricule, String type, String formDataKey) throws SQLException {
        String sql = "SELECT * FROM decoration WHERE matricule = ? AND type = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Decoration> decorations = new ArrayList<>();
                
                while (rs.next()) {
                    Decoration decoration = new Decoration(
                        rs.getString("grade"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("reference")
                    );
                    decorations.add(decoration);
                }
                
                Map<String, Object> decorationsData = new HashMap<>();
                decorationsData.put("matricule", matricule);
                decorationsData.put("decorations", decorations);
                
                formData.put(formDataKey, decorationsData);
            }
        }
    }

    /**
     * Charge les médailles.
     */
    private void loadMedailles(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM medaille WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Medal> medailles = new ArrayList<>();
                
                while (rs.next()) {
                    Medal medaille = new Medal(
                        rs.getString("medaille"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("reference")
                    );
                    medailles.add(medaille);
                }
                
                Map<String, Object> medaillesData = new HashMap<>();
                medaillesData.put("matricule", matricule);
                medaillesData.put("medailles", medailles);
                
                formData.put("medailles", medaillesData);
            }
        }
    }

    /**
     * Charge les punitions.
     */
    private void loadPunitions(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM punition WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Punishment> punitions = new ArrayList<>();
                
                while (rs.next()) {
                    Punishment punition = new Punishment(
                        rs.getString("item"),
                        rs.getString("motif"),
                        rs.getString("circumstances"),
                        rs.getInt("days"),
                        rs.getString("authority"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now()
                    );
                    punitions.add(punition);
                }
                
                Map<String, Object> punitionsData = new HashMap<>();
                punitionsData.put("matricule", matricule);
                punitionsData.put("punitions", punitions);
                
                formData.put("punitions", punitionsData);
            }
        }
    }

    /**
     * Charge les langues.
     */
    private void loadLangues(Connection connection, String matricule) throws SQLException {
        String[] categories = {"langues_parlées", "langues_écrites", "langues_lues", "langues_apprises"};
        
        for (String categorie : categories) {
            String sql = "SELECT * FROM langue WHERE matricule = ? AND categorie = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, matricule);
                stmt.setString(2, categorie);
                try (ResultSet rs = stmt.executeQuery()) {
                    Map<String, Object> langueData = new HashMap<>();
                    langueData.put("matricule", matricule);
                    
                    int count = 0;
                    while (rs.next()) {
                        String langueId = "langue_" + (++count);
                        Map<String, String> langueInfo = new HashMap<>();
                        langueInfo.put("langue", rs.getString("langue"));
                        langueInfo.put("niveau", rs.getString("niveau"));
                        
                        langueData.put(langueId, langueInfo);
                    }
                    
                    formData.put(categorie, langueData);
                }
            }
        }
    }

    /**
     * Charge les informations spécifiques générales.
     */
    private void loadInfosSpecifiquesGeneral(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM infos_specifiques_general WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", matricule);
                    data.put("niveau_epms", rs.getInt("niveau_epms"));
                    data.put("permis_militaire", rs.getString("permis_militaire"));
                    data.put("numero_permis_militaire", rs.getString("numero_permis_militaire"));
                    data.put("arme_dotation", rs.getString("arme_dotation"));
                    data.put("corps_technique", rs.getString("corps_technique"));
                    data.put("numero_carte_identite", rs.getString("numero_carte_identite"));
                    data.put("promotion_contingent", rs.getString("promotion_contingent"));
                    data.put("numero_carte_identite_militaire", rs.getString("numero_carte_identite_militaire"));
                    data.put("numero_matricule_solde", rs.getString("numero_matricule_solde"));
                    data.put("position_administrative", rs.getString("position_administrative"));
                    data.put("reference", rs.getString("reference")); // NOUVEAU CHAMP RÉFÉRENCE
                    data.put("date_incorporation", rs.getDate("date_incorporation") != null ? rs.getDate("date_incorporation").toLocalDate() : null);
                    data.put("date_fin_service", rs.getDate("date_fin_service") != null ? rs.getDate("date_fin_service").toLocalDate() : null);
                    data.put("temps_restant_retraite", rs.getString("temps_restant_retraite"));
                    
                    formData.put("infos_specifiques_general", data);
                }
            }
        }
    }

    /**
     * Charge les informations du personnel naviguant.
     */
    private void loadPersonnelNaviguant(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM personnel_naviguant WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", matricule);
                    data.put("aeronef_arme", rs.getString("aeronef_arme"));
                    data.put("fonction_bord", rs.getString("fonction_bord"));
                    data.put("aeronef_affectation", rs.getString("aeronef_affectation"));
                    data.put("qualification_type", rs.getString("qualification_type"));
                    data.put("test_trimestriel", rs.getDouble("test_trimestriel"));
                    data.put("cempn_validite", rs.getDate("cempn_validite") != null ? rs.getDate("cempn_validite").toLocalDate() : null);
                    data.put("heures_vol", rs.getInt("heures_vol"));
                    data.put("anciennete_pn", rs.getInt("anciennete_pn"));
                    data.put("numero_titre_aerien", rs.getString("numero_titre_aerien"));
                    data.put("niveau_execution", rs.getInt("niveau_execution"));
                    
                    formData.put("personnel_naviguant", data);
                }
            }
        }
    }

    /**
     * Charge les maintenances programmées.
     */
    private void loadMaintenancesProgrammees(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM maintenance WHERE matricule = ? AND type = 'Programmée'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Maintenance> maintenances = new ArrayList<>();
                String niveauExecution = "";
                
                while (rs.next()) {
                    Maintenance maintenance = new Maintenance(
                        rs.getString("operation"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("formation"),
                        "Programmée"
                    );
                    maintenances.add(maintenance);
                    
                    if (niveauExecution.isEmpty()) {
                        niveauExecution = rs.getString("niveau_execution");
                    }
                }
                
                Map<String, Object> maintenancesData = new HashMap<>();
                maintenancesData.put("matricule", matricule);
                maintenancesData.put("niveau_execution", niveauExecution);
                maintenancesData.put("maintenances", maintenances);
                
                formData.put("maintenances_programmees", maintenancesData);
            }
        }
    }

    /**
     * Charge les maintenances curatives.
     */
    private void loadMaintenancesCuratives(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM maintenance WHERE matricule = ? AND type = 'Curative'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Maintenance> maintenances = new ArrayList<>();
                
                while (rs.next()) {
                    Maintenance maintenance = new Maintenance(
                        rs.getString("operation"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : LocalDate.now(),
                        rs.getString("formation"),
                        "Curative"
                    );
                    maintenances.add(maintenance);
                }
                
                Map<String, Object> maintenancesData = new HashMap<>();
                maintenancesData.put("matricule", matricule);
                maintenancesData.put("maintenances", maintenances);
                
                formData.put("maintenances_curatives", maintenancesData);
            }
        }
    }

    /**
     * Charge la spécialité.
     */
    private void loadSpecialite(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM specialite WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", matricule);
                    data.put("type_specialite", rs.getString("type_specialite"));
                    data.put("autre_specialite", rs.getString("autre_specialite"));
                    
                    formData.put("specialite", data);
                }
            }
        }
    }

    /**
     * Charge les dotations 20 Mai.
     */
    private void loadDotation20Mai(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM dotation_20_mai WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Dotation> dotations = new ArrayList<>();
                
                while (rs.next()) {
                    Dotation dotation = new Dotation(
                        rs.getInt("annee"),
                        rs.getString("contenu")
                    );
                    dotations.add(dotation);
                }
                
                Map<String, Object> dotationsData = new HashMap<>();
                dotationsData.put("matricule", matricule);
                dotationsData.put("nombre_dotations", dotations.size());
                dotationsData.put("dotations", dotations);
                
                formData.put("dotation_20_mai", dotationsData);
            }
        }
    }

    /**
     * Charge les dotations particulières.
     */
    private void loadDotationParticuliere(Connection connection, String matricule) throws SQLException {
        // Charger la configuration
        String configSql = "SELECT * FROM dotation_particuliere_config WHERE matricule = ?";
        boolean jamaisRecu = false;
        
        try (PreparedStatement stmt = connection.prepareStatement(configSql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    jamaisRecu = rs.getBoolean("jamais_recu");
                }
            }
        }
        
        Map<String, Object> dotationsData = new HashMap<>();
        dotationsData.put("matricule", matricule);
        dotationsData.put("jamais_recu", jamaisRecu);
        
        // Charger les dotations si applicable
        if (!jamaisRecu) {
            String sql = "SELECT * FROM dotation_particuliere WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, matricule);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<DotationParticuliere> dotations = new ArrayList<>();
                    
                    while (rs.next()) {
                        DotationParticuliere dotation = new DotationParticuliere(
                            rs.getString("raison"),
                            rs.getInt("annee"),
                            rs.getString("mois"),
                            rs.getString("contenu")
                        );
                        dotations.add(dotation);
                    }
                    
                    dotationsData.put("dotations", dotations);
                }
            }
        }
        
        formData.put("dotation_particuliere", dotationsData);
    }

    /**
     * Charge les paramètres corporels.
     */
    private void loadParametresCorporels(Connection connection, String matricule) throws SQLException {
        String sql = "SELECT * FROM parametres_corporels WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("matricule", matricule);
                    data.put("contour_tete", rs.getInt("contour_tete"));
                    data.put("pointure", rs.getInt("pointure"));
                    data.put("tour_hanche", rs.getInt("tour_hanche"));
                    data.put("tour_poignet", rs.getInt("tour_poignet"));
                    data.put("taille", rs.getString("taille"));
                    
                    formData.put("parametres_corporels", data);
                }
            }
        }
    }    
    
    /**
     * Sauvegarde toutes les données dans la base de données.
     * 
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveAllData() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Commencer une transaction
            connection.setAutoCommit(false);
            
            // Enregistrer les données d'identité personnelle
            saveIdentitePersonnelle(connection);
            
            // Enregistrer les données d'identité sociale
            saveIdentiteSociale(connection);
            
            // Enregistrer les données d'identité culturelle
            saveIdentiteCulturelle(connection);
            
            // Enregistrer les grades
            saveGrades(connection);
            
            // Enregistrer les formations et postes
            saveFormationPoste(connection);
            
            // Enregistrer les écoles et diplômes
            saveEcolesDiplomes(connection);
            
            // Enregistrer les opérations
            saveOperations(connection);
            
            // Enregistrer les décorations
            saveDecorations(connection);
            
            // Enregistrer les punitions
            savePunitions(connection);
            
            // Enregistrer les langues
            saveLangues(connection);
            
            // Enregistrer les informations spécifiques
            saveInformationsSpecifiques(connection);
            
            // Enregistrer les dotations
            saveDotations(connection);
            
            // Valider la transaction
            connection.commit();
            
            // Journaliser le succès
            System.out.println("Données enregistrées" + 
                              " avec succès pour le matricule: " + currentMatricule);
            
        } catch (SQLException e) {
            // En cas d'erreur, annuler la transaction
            if (connection != null) {
                try {
                    connection.rollback();
                    System.err.println("Transaction annulée en raison d'une erreur.");
                } catch (SQLException rollbackEx) {
                    System.err.println("Erreur lors de l'annulation de la transaction: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            // Rétablir le mode auto-commit et fermer la connexion
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    System.err.println("Erreur lors de la fermeture de la connexion: " + closeEx.getMessage());
                }
            }
        }
    }
    
    /**
     * Obtient une connexion à la base de données.
     * 
     * @return La connexion à la base de données
     * @throws SQLException Si une erreur survient lors de la connexion
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
     * Enregistre les données d'identité personnelle dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveIdentitePersonnelle(Connection connection) throws SQLException {
        Map<String, Object> data = formData.get("identite_personnelle");
        if (data != null) {
            String sql = "INSERT INTO identite_personnelle "
                    + "(matricule, nom, prenom, lieu_naissance, date_naissance, telephone, sexe, groupe_sanguin, photo_path) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "nom = VALUES(nom), prenom = VALUES(prenom), lieu_naissance = VALUES(lieu_naissance), "
                    + "date_naissance = VALUES(date_naissance), telephone = VALUES(telephone), "
                    + "sexe = VALUES(sexe), groupe_sanguin = VALUES(groupe_sanguin), photo_path = VALUES(photo_path)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) data.get("nom"));
                stmt.setString(3, (String) data.get("prenom"));
                stmt.setString(4, (String) data.get("lieu_naissance"));
                Object dateNaissance = data.get("date_naissance");
                if (dateNaissance == null || dateNaissance.toString().trim().isEmpty()) {
                    stmt.setNull(5, java.sql.Types.DATE);
                } else {
                    stmt.setObject(5, dateNaissance);
                }
                stmt.setString(6, (String) data.get("telephone"));
                stmt.setString(7, (String) data.get("sexe"));
                stmt.setString(8, (String) data.get("groupe_sanguin"));
                stmt.setString(9, (String) data.get("photo_path"));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Enregistre les données d'identité sociale dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveIdentiteSociale(Connection connection) throws SQLException {
        Map<String, Object> data = formData.get("identite_sociale");
        if (data != null) {
            String sql = "INSERT INTO identite_sociale "
                    + "(matricule, nom_pere, nom_mere, nombre_conjoints, nombre_enfants, "
                    + "personne_contact, lien_personne_contact, telephone_personne_contact, "
                    + "lieu_residence_personne_contact, remarque_particuliere, regime_matrimonial, nom_conjoint) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "nom_pere = VALUES(nom_pere), nom_mere = VALUES(nom_mere), "
                    + "nombre_conjoints = VALUES(nombre_conjoints), nombre_enfants = VALUES(nombre_enfants), "
                    + "personne_contact = VALUES(personne_contact), lien_personne_contact = VALUES(lien_personne_contact), "
                    + "telephone_personne_contact = VALUES(telephone_personne_contact), "
                    + "lieu_residence_personne_contact = VALUES(lieu_residence_personne_contact), "
                    + "remarque_particuliere = VALUES(remarque_particuliere), regime_matrimonial = VALUES(regime_matrimonial), "
                    + "nom_conjoint = VALUES(nom_conjoint)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) data.get("nom_pere"));
                stmt.setString(3, (String) data.get("nom_mere"));
                stmt.setObject(4, data.get("nombre_conjoints"));
                stmt.setObject(5, data.get("nombre_enfants"));
                stmt.setString(6, (String) data.get("personne_contact"));
                stmt.setString(7, (String) data.get("lien_personne_contact"));
                stmt.setString(8, (String) data.get("telephone_personne_contact"));
                stmt.setString(9, (String) data.get("lieu_residence_personne_contact"));
                stmt.setString(10, (String) data.get("remarque_particuliere"));
                stmt.setString(11, (String) data.get("regime_matrimonial"));
                stmt.setString(12, (String) data.get("nom_conjoint"));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Enregistre les données d'identité culturelle dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveIdentiteCulturelle(Connection connection) throws SQLException {
        Map<String, Object> data = formData.get("identite_culturelle");
        if (data != null) {
            String sql = "INSERT INTO identite_culturelle "
                    + "(matricule, region_origine, departement_origine, arrondissement_origine, village, ethnie, religion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "region_origine = VALUES(region_origine), departement_origine = VALUES(departement_origine), "
                    + "arrondissement_origine = VALUES(arrondissement_origine), village = VALUES(village), "
                    + "ethnie = VALUES(ethnie), religion = VALUES(religion)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
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
    
    /**
     * Enregistre les grades dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveGrades(Connection connection) throws SQLException {
        // Sauvegarde du grade actuel
        Map<String, Object> gradeActuelData = formData.get("grade_actuel");
        if (gradeActuelData != null && gradeActuelData.containsKey("grade")) {
            // Suppression du grade actuel précédent
            String deleteActuelSql = "DELETE FROM grade_actuel WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteActuelSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insertion du nouveau grade actuel avec le statut
            String insertActuelSql = "INSERT INTO grade_actuel "
                    + "(matricule, rang, echelon, date, reference, echelon_grade, reference_echelon, date_echelon, statut) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(insertActuelSql)) {
                Grade grade = (Grade) gradeActuelData.get("grade");
                
                stmt.setString(1, currentMatricule);
                stmt.setString(2, grade.getRang());
                stmt.setString(3, grade.getEchelon());
                stmt.setObject(4, grade.getDate());
                stmt.setString(5, grade.getReference());
                stmt.setString(6, grade.getEchelonGrade());
                stmt.setString(7, grade.getReferenceEchelon());
                stmt.setObject(8, grade.getDateEchelon());
                stmt.setString(9, grade.getStatut());
                
                stmt.executeUpdate();
            }
        }
        
        // Sauvegarde de l'historique des grades
        Map<String, Object> historiqueGradesData = formData.get("grades_historique");
        if (historiqueGradesData != null && historiqueGradesData.containsKey("grades")) {
            // Suppression de l'historique précédent
            String deleteHistoriqueSql = "DELETE FROM historique_grades WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteHistoriqueSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insertion du nouvel historique avec le statut
            String insertHistoriqueSql = "INSERT INTO historique_grades "
                    + "(matricule, rang, echelon, date, reference, echelon_grade, reference_echelon, date_echelon, statut) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(insertHistoriqueSql)) {
                @SuppressWarnings("unchecked")
                List<Grade> grades = (List<Grade>) historiqueGradesData.get("grades");
                
                for (Grade grade : grades) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, grade.getRang());
                    stmt.setString(3, grade.getEchelon());
                    stmt.setObject(4, grade.getDate());
                    stmt.setString(5, grade.getReference());
                    stmt.setString(6, grade.getEchelonGrade());
                    stmt.setString(7, grade.getReferenceEchelon());
                    stmt.setObject(8, grade.getDateEchelon());
                    stmt.setString(9, grade.getStatut());
                    
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Enregistre les formations et postes dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveFormationPoste(Connection connection) throws SQLException {
        // Enregistrer la formation actuelle
        Map<String, Object> formationData = formData.get("formation_actuelle");
        if (formationData != null) {
            String sql = "INSERT INTO formation_actuelle "
                    + "(matricule, formation, date_affectation, reference_affectation, unite, poste) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "formation = VALUES(formation), date_affectation = VALUES(date_affectation), "
                    + "reference_affectation = VALUES(reference_affectation), unite = VALUES(unite), "
                    + "poste = VALUES(poste)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) formationData.get("formation"));
                stmt.setObject(3, formationData.get("date_affectation"));
                stmt.setString(4, (String) formationData.get("reference_affectation"));
                stmt.setString(5, (String) formationData.get("unite"));
                stmt.setString(6, (String) formationData.get("poste"));
                
                stmt.executeUpdate();
            }
        }
        
        // Enregistrer l'historique des postes
        Map<String, Object> historiqueData = formData.get("historique_postes");
        if (historiqueData != null && historiqueData.containsKey("postes")) {
            // Supprimer les anciens postes
            String deleteSql = "DELETE FROM historique_postes WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouveaux postes
            String sql = "INSERT INTO historique_postes "
                    + "(matricule, formation, unite, date_debut, date_fin, poste) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<PosteHistorique> postes = (List<PosteHistorique>) historiqueData.get("postes");
                
                for (PosteHistorique poste : postes) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, poste.getFormation());
                    stmt.setString(3, poste.getUnite());
                    stmt.setObject(4, poste.getDateDebut());
                    stmt.setObject(5, poste.getDateFin());
                    stmt.setString(6, poste.getPoste());
                    
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Enregistre les écoles et diplômes dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveEcolesDiplomes(Connection connection) throws SQLException {
        // Enregistrer l'école de formation initiale
        Map<String, Object> formationInitialeData = formData.get("ecole_formation_initiale");
        if (formationInitialeData != null) {
            String sql = "INSERT INTO ecole_formation_initiale "
                    + "(matricule, nom_ecole, pays, region, date_entree, date_sortie, temps_mis, diplome_obtenu) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "nom_ecole = VALUES(nom_ecole), pays = VALUES(pays), region = VALUES(region), "
                    + "date_entree = VALUES(date_entree), date_sortie = VALUES(date_sortie), "
+ "temps_mis = VALUES(temps_mis), diplome_obtenu = VALUES(diplome_obtenu)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) formationInitialeData.get("nom_ecole"));
                stmt.setString(3, (String) formationInitialeData.get("pays"));
                stmt.setString(4, (String) formationInitialeData.get("region"));
                stmt.setObject(5, formationInitialeData.get("date_entree"));
                stmt.setObject(6, formationInitialeData.get("date_sortie"));
                stmt.setString(7, (String) formationInitialeData.get("temps_mis"));
                stmt.setString(8, (String) formationInitialeData.get("diplome_obtenu"));
                
                stmt.executeUpdate();
            }
        }
        
        // Enregistrer les écoles civiles
        Map<String, Object> ecolesCivilesData = formData.get("ecoles_civiles");
        if (ecolesCivilesData != null && ecolesCivilesData.containsKey("ecoles")) {
            // Supprimer les anciennes écoles civiles
            String deleteSql = "DELETE FROM ecole_civile WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles écoles civiles
            String sql = "INSERT INTO ecole_civile "
                    + "(matricule, nom_ecole, pays, region, diplome_obtenu) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<EcoleCivile> ecoles = (List<EcoleCivile>) ecolesCivilesData.get("ecoles");
                
                for (EcoleCivile ecole : ecoles) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, ecole.getNomEcole());
                    stmt.setString(3, ecole.getPays());
                    stmt.setString(4, ecole.getRegion());
                    stmt.setString(5, ecole.getDiplomeObtenu());
                    
                    stmt.executeUpdate();
                }
            }
        }
        
        // Enregistrer les écoles et stages militaires
        Map<String, Object> ecolesMilitairesData = formData.get("ecoles_militaires");
        if (ecolesMilitairesData != null && ecolesMilitairesData.containsKey("ecoles")) {
            // Supprimer les anciennes écoles militaires
            String deleteSql = "DELETE FROM ecole_militaire WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles écoles militaires
            String sql = "INSERT INTO ecole_militaire "
                    + "(matricule, nom_ecole, pays, type, date_debut, date_fin, temps_mis, diplome_obtenu) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<EcoleMilitaire> ecoles = (List<EcoleMilitaire>) ecolesMilitairesData.get("ecoles");
                
                for (EcoleMilitaire ecole : ecoles) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, ecole.getNomEcole());
                    stmt.setString(3, ecole.getPays());
                    stmt.setString(4, ecole.getType());
                    stmt.setObject(5, ecole.getDateDebut());
                    stmt.setObject(6, ecole.getDateFin());
                    stmt.setString(7, ecole.getTempsMis());
                    stmt.setString(8, ecole.getDiplomeObtenu());
                    
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Enregistre les opérations dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveOperations(Connection connection) throws SQLException {
        // Enregistrer les opérations intérieures
        Map<String, Object> operationsInterieuresData = formData.get("operations_interieures");
        if (operationsInterieuresData != null && operationsInterieuresData.containsKey("operations")) {
            // Supprimer les anciennes opérations intérieures
            String deleteSql = "DELETE FROM operation WHERE matricule = ? AND type = 'Intérieure'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles opérations intérieures
            String sql = "INSERT INTO operation "
                    + "(matricule, nom_mission, annee, lieu, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Operation> operations = (List<Operation>) operationsInterieuresData.get("operations");
                
                for (Operation operation : operations) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, operation.getNomMission());
                    stmt.setInt(3, operation.getAnnee());
                    stmt.setString(4, operation.getLieu());
                    stmt.setString(5, "Intérieure");
                    
                    stmt.executeUpdate();
                }
            }
        }
        
        // Enregistrer les opérations extérieures
        Map<String, Object> operationsExterieuresData = formData.get("operations_exterieures");
        if (operationsExterieuresData != null && operationsExterieuresData.containsKey("operations")) {
            // Supprimer les anciennes opérations extérieures
            String deleteSql = "DELETE FROM operation WHERE matricule = ? AND type = 'Extérieure'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles opérations extérieures
            String sql = "INSERT INTO operation "
                    + "(matricule, nom_mission, annee, lieu, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Operation> operations = (List<Operation>) operationsExterieuresData.get("operations");
                
                for (Operation operation : operations) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, operation.getNomMission());
                    stmt.setInt(3, operation.getAnnee());
                    stmt.setString(4, operation.getLieu());
                    stmt.setString(5, "Extérieure");
                    
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Enregistre les décorations dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveDecorations(Connection connection) throws SQLException {
        // Enregistrer les ordres nationaux
        Map<String, Object> ordresNationauxData = formData.get("decorations_ordres_nationaux");
        if (ordresNationauxData != null && ordresNationauxData.containsKey("decorations")) {
            // Supprimer les anciennes décorations de ce type
            String deleteSql = "DELETE FROM decoration WHERE matricule = ? AND type = 'Ordres Nationaux'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles décorations
            String sql = "INSERT INTO decoration "
                    + "(matricule, grade, date, reference, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Decoration> decorations = (List<Decoration>) ordresNationauxData.get("decorations");
                
                for (Decoration decoration : decorations) {
                    if (decoration.getDate() != null) {
                        stmt.setString(1, currentMatricule);
                        stmt.setString(2, decoration.getGrade());
                        stmt.setObject(3, decoration.getDate());
                        stmt.setString(4, decoration.getReference());
                        stmt.setString(5, "Ordres Nationaux");
                        
                        stmt.executeUpdate();
                    }
                }
            }
        }
        
        // Enregistrer les ordres du mérite camerounais
        Map<String, Object> ordresMeriteData = formData.get("decorations_ordres_du_merite_camerounais");
        if (ordresMeriteData != null && ordresMeriteData.containsKey("decorations")) {
            // Supprimer les anciennes décorations de ce type
            String deleteSql = "DELETE FROM decoration WHERE matricule = ? AND type = 'Ordres du Mérite Camerounais'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles décorations
            String sql = "INSERT INTO decoration "
                    + "(matricule, grade, date, reference, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Decoration> decorations = (List<Decoration>) ordresMeriteData.get("decorations");
                
                for (Decoration decoration : decorations) {
                    if (decoration.getDate() != null) {
                        stmt.setString(1, currentMatricule);
                        stmt.setString(2, decoration.getGrade());
                        stmt.setObject(3, decoration.getDate());
                        stmt.setString(4, decoration.getReference());
                        stmt.setString(5, "Ordres du Mérite Camerounais");
                        
                        stmt.executeUpdate();
                    }
                }
            }
        }
        
        // Enregistrer les ordres du mérite sportif
        Map<String, Object> ordreMeriteSportifData = formData.get("decorations_ordre_du_merite_sportif");
        if (ordreMeriteSportifData != null && ordreMeriteSportifData.containsKey("decorations")) {
            // Supprimer les anciennes décorations de ce type
            String deleteSql = "DELETE FROM decoration WHERE matricule = ? AND type = 'Ordre du Mérite Sportif'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles décorations
            String sql = "INSERT INTO decoration "
                    + "(matricule, grade, date, reference, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Decoration> decorations = (List<Decoration>) ordreMeriteSportifData.get("decorations");
                
                for (Decoration decoration : decorations) {
                    if (decoration.getDate() != null) {
                        stmt.setString(1, currentMatricule);
                        stmt.setString(2, decoration.getGrade());
                        stmt.setObject(3, decoration.getDate());
                        stmt.setString(4, decoration.getReference());
                        stmt.setString(5, "Ordre du Mérite Sportif");
                        
                        stmt.executeUpdate();
                    }
                }
            }
        }
        
        // Enregistrer les médailles
        Map<String, Object> medaillesData = formData.get("medailles");
        if (medaillesData != null && medaillesData.containsKey("medailles")) {
            // Supprimer les anciennes médailles
            String deleteSql = "DELETE FROM medaille WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles médailles
            String sql = "INSERT INTO medaille "
                    + "(matricule, medaille, date, reference) "
                    + "VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Medal> medailles = (List<Medal>) medaillesData.get("medailles");
                
                for (Medal medaille : medailles) {
                    if (medaille.getDate() != null) {
                        stmt.setString(1, currentMatricule);
                        stmt.setString(2, medaille.getMedal());
                        stmt.setObject(3, medaille.getDate());
                        stmt.setString(4, medaille.getReference());
                        
                        stmt.executeUpdate();
                    }
                }
            }
        }
    }
    
    /**
     * Enregistre les punitions dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void savePunitions(Connection connection) throws SQLException {
        Map<String, Object> punitionsData = formData.get("punitions");
        if (punitionsData != null && punitionsData.containsKey("punitions")) {
            // Supprimer les anciennes punitions
            String deleteSql = "DELETE FROM punition WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles punitions
            String sql = "INSERT INTO punition "
                    + "(matricule, item, motif, circumstances, days, authority, date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Punishment> punitions = (List<Punishment>) punitionsData.get("punitions");
                
                for (Punishment punition : punitions) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, punition.getItem());
                    stmt.setString(3, punition.getMotif());
                    stmt.setString(4, punition.getCircumstances());
                    stmt.setInt(5, punition.getDays());
                    stmt.setString(6, punition.getAuthority());
                    stmt.setObject(7, punition.getDate());
                    
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Enregistre les informations spécifiques générales dans la base de données (version mise à jour).
     */
    private void saveInformationsSpecifiquesGenerales(Connection connection) throws SQLException {
        Map<String, Object> generalData = formData.get("infos_specifiques_general");
        if (generalData != null) {
            String sql = "INSERT INTO infos_specifiques_general "
                    + "(matricule, niveau_epms, permis_militaire, numero_permis_militaire, arme_dotation, corps_technique, "
                    + "numero_carte_identite, promotion_contingent, numero_carte_identite_militaire, numero_matricule_solde, "
                    + "position_administrative, reference, date_incorporation, date_fin_service, temps_restant_retraite) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "niveau_epms = VALUES(niveau_epms), permis_militaire = VALUES(permis_militaire), "
                    + "numero_permis_militaire = VALUES(numero_permis_militaire), arme_dotation = VALUES(arme_dotation), "
                    + "corps_technique = VALUES(corps_technique), numero_carte_identite = VALUES(numero_carte_identite), "
                    + "promotion_contingent = VALUES(promotion_contingent), "
                    + "numero_carte_identite_militaire = VALUES(numero_carte_identite_militaire), "
                    + "numero_matricule_solde = VALUES(numero_matricule_solde), "
                    + "position_administrative = VALUES(position_administrative), "
                    + "reference = VALUES(reference), "
                    + "date_incorporation = VALUES(date_incorporation), date_fin_service = VALUES(date_fin_service), "
                    + "temps_restant_retraite = VALUES(temps_restant_retraite)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setObject(2, generalData.get("niveau_epms"));
                stmt.setString(3, (String) generalData.get("permis_militaire"));
                stmt.setString(4, (String) generalData.get("numero_permis_militaire"));
                stmt.setString(5, (String) generalData.get("arme_dotation"));
                stmt.setString(6, (String) generalData.get("corps_technique"));
                stmt.setString(7, (String) generalData.get("numero_carte_identite"));
                stmt.setString(8, (String) generalData.get("promotion_contingent"));
                stmt.setString(9, (String) generalData.get("numero_carte_identite_militaire"));
                stmt.setString(10, (String) generalData.get("numero_matricule_solde"));
                stmt.setString(11, (String) generalData.get("position_administrative"));
                stmt.setString(12, (String) generalData.get("reference")); // NOUVEAU CHAMP RÉFÉRENCE
                stmt.setObject(13, generalData.get("date_incorporation"));
                stmt.setObject(14, generalData.get("date_fin_service"));
                stmt.setString(15, (String) generalData.get("temps_restant_retraite"));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Enregistre les langues dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveLangues(Connection connection) throws SQLException {
        // Supprimer toutes les langues pour ce matricule
        String deleteSql = "DELETE FROM langue WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setString(1, currentMatricule);
            stmt.executeUpdate();
        }
        
        // Enregistrer les différentes catégories de langues
        for (String categorie : new String[]{"langues_parlées", "langues_écrites", "langues_lues", "langues_apprises"}) {
            Map<String, Object> languesData = formData.get(categorie);
            if (languesData != null) {
                // Insérer les langues
                String sql = "INSERT INTO langue (matricule, langue, niveau, categorie) VALUES (?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (String key : languesData.keySet()) {
                        if (key.startsWith("langue_") && languesData.get(key) instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, String> langueInfo = (Map<String, String>) languesData.get(key);
                            
                            stmt.setString(1, currentMatricule);
                            stmt.setString(2, langueInfo.get("langue"));
                            stmt.setString(3, langueInfo.get("niveau"));
                            stmt.setString(4, categorie);
                            
                            stmt.executeUpdate();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Enregistre les informations spécifiques dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveInformationsSpecifiques(Connection connection) throws SQLException {
    	// Enregistrer les informations générales (mise à jour)
        saveInformationsSpecifiquesGenerales(connection);
        
        // Enregistrer les informations du personnel naviguant (code existant)
        Map<String, Object> navData = formData.get("personnel_naviguant");
        if (navData != null) {
            String sql = "INSERT INTO personnel_naviguant "
                    + "(matricule, aeronef_arme, fonction_bord, aeronef_affectation, qualification_type, "
                    + "test_trimestriel, cempn_validite, heures_vol, anciennete_pn, numero_titre_aerien, niveau_execution) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "aeronef_arme = VALUES(aeronef_arme), fonction_bord = VALUES(fonction_bord), "
                    + "aeronef_affectation = VALUES(aeronef_affectation), qualification_type = VALUES(qualification_type), "
                    + "test_trimestriel = VALUES(test_trimestriel), cempn_validite = VALUES(cempn_validite), "
                    + "heures_vol = VALUES(heures_vol), anciennete_pn = VALUES(anciennete_pn), "
                    + "numero_titre_aerien = VALUES(numero_titre_aerien), niveau_execution = VALUES(niveau_execution)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) navData.get("aeronef_arme"));
                stmt.setString(3, (String) navData.get("fonction_bord"));
                stmt.setString(4, (String) navData.get("aeronef_affectation"));
                stmt.setString(5, (String) navData.get("qualification_type"));
                stmt.setObject(6, navData.get("test_trimestriel"));
                stmt.setObject(7, navData.get("cempn_validite"));
                stmt.setObject(8, navData.get("heures_vol"));
                stmt.setObject(9, navData.get("anciennete_pn"));
                stmt.setString(10, (String) navData.get("numero_titre_aerien"));
                stmt.setObject(11, navData.get("niveau_execution"));
                
                stmt.executeUpdate();
            }
        }
        
        // Enregistrer les maintenances programmées
        Map<String, Object> maintenancesProgrammees = formData.get("maintenances_programmees");
        if (maintenancesProgrammees != null && maintenancesProgrammees.containsKey("maintenances")) {
            // Supprimer les anciennes maintenances
            String deleteSql = "DELETE FROM maintenance WHERE matricule = ? AND type = 'Programmée'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles maintenances
            String sql = "INSERT INTO maintenance "
                    + "(matricule, operation, date, formation, type, niveau_execution) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Maintenance> maintenances = (List<Maintenance>) maintenancesProgrammees.get("maintenances");
                
                for (Maintenance maintenance : maintenances) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, maintenance.getOperation());
                    stmt.setObject(3, maintenance.getDate());
                    stmt.setString(4, maintenance.getFormation());
                    stmt.setString(5, "Programmée");
                    stmt.setString(6, (String) maintenancesProgrammees.get("niveau_execution"));
                    
                    stmt.executeUpdate();
                }
            }
        }
        
        // Enregistrer les maintenances curatives
        Map<String, Object> maintenancesCuratives = formData.get("maintenances_curatives");
        if (maintenancesCuratives != null && maintenancesCuratives.containsKey("maintenances")) {
            // Supprimer les anciennes maintenances
            String deleteSql = "DELETE FROM maintenance WHERE matricule = ? AND type = 'Curative'";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles maintenances
            String sql = "INSERT INTO maintenance "
                    + "(matricule, operation, date, formation, type) "
                    + "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Maintenance> maintenances = (List<Maintenance>) maintenancesCuratives.get("maintenances");
                
                for (Maintenance maintenance : maintenances) {
                    stmt.setString(1, currentMatricule);
                    stmt.setString(2, maintenance.getOperation());
                    stmt.setObject(3, maintenance.getDate());
                    stmt.setString(4, maintenance.getFormation());
                    stmt.setString(5, "Curative");
                    
                    stmt.executeUpdate();
                }
            }
        }
        
        // Enregistrer la spécialité
        Map<String, Object> specialiteData = formData.get("specialite");
        if (specialiteData != null) {
            String sql = "INSERT INTO specialite "
                    + "(matricule, type_specialite, autre_specialite) "
                    + "VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "type_specialite = VALUES(type_specialite), autre_specialite = VALUES(autre_specialite)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setString(2, (String) specialiteData.get("type_specialite"));
                stmt.setString(3, (String) specialiteData.get("autre_specialite"));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Enregistre les dotations dans la base de données.
     * 
     * @param connection La connexion à la base de données
     * @throws SQLException Si une erreur SQL survient
     */
    private void saveDotations(Connection connection) throws SQLException {
        // Enregistrer les dotations 20 Mai
        Map<String, Object> dotations20Mai = formData.get("dotation_20_mai");
        if (dotations20Mai != null && dotations20Mai.containsKey("dotations")) {
            // Supprimer les anciennes dotations
            String deleteSql = "DELETE FROM dotation_20_mai WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles dotations
            String sql = "INSERT INTO dotation_20_mai "
                    + "(matricule, annee, contenu) "
                    + "VALUES (?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                @SuppressWarnings("unchecked")
                List<Dotation> dotations = (List<Dotation>) dotations20Mai.get("dotations");
                
                for (Dotation dotation : dotations) {
                    stmt.setString(1, currentMatricule);
                    stmt.setInt(2, dotation.getAnnee());
                    stmt.setString(3, dotation.getContenu());
                    
                    stmt.executeUpdate();
                }
            }
        }
        
        // Enregistrer les dotations particulières
        Map<String, Object> dotationsParticulieres = formData.get("dotation_particuliere");
        if (dotationsParticulieres != null) {
            // Supprimer les anciennes dotations
            String deleteSql = "DELETE FROM dotation_particuliere WHERE matricule = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, currentMatricule);
                stmt.executeUpdate();
            }
            
            // Insérer les données de configuration
            String configSql = "INSERT INTO dotation_particuliere_config "
                    + "(matricule, jamais_recu) "
                    + "VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "jamais_recu = VALUES(jamais_recu)";
            
            try (PreparedStatement stmt = connection.prepareStatement(configSql)) {
                stmt.setString(1, currentMatricule);
                stmt.setBoolean(2, (Boolean) dotationsParticulieres.get("jamais_recu"));
                
                stmt.executeUpdate();
            }
            
            // Insérer les nouvelles dotations, si applicables
            if (!((Boolean) dotationsParticulieres.get("jamais_recu")) && dotationsParticulieres.containsKey("dotations")) {
                String sql = "INSERT INTO dotation_particuliere "
                        + "(matricule, raison, annee, mois, contenu) "
                        + "VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    @SuppressWarnings("unchecked")
                    List<DotationParticuliere> dotations = (List<DotationParticuliere>) dotationsParticulieres.get("dotations");
                    
                    for (DotationParticuliere dotation : dotations) {
                        stmt.setString(1, currentMatricule);
                        stmt.setString(2, dotation.getRaison());
                        stmt.setInt(3, dotation.getAnnee());
                        stmt.setString(4, dotation.getMois());
                        stmt.setString(5, dotation.getContenu());
                        
                        stmt.executeUpdate();
                    }
                }
            }
        }
        
        // Enregistrer les paramètres corporels
        Map<String, Object> parametresData = formData.get("parametres_corporels");
        if (parametresData != null) {
            String sql = "INSERT INTO parametres_corporels "
                    + "(matricule, contour_tete, pointure, tour_hanche, tour_poignet, taille) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "contour_tete = VALUES(contour_tete), pointure = VALUES(pointure), "
                    + "tour_hanche = VALUES(tour_hanche), tour_poignet = VALUES(tour_poignet), "
                    + "taille = VALUES(taille)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentMatricule);
                stmt.setObject(2, parametresData.get("contour_tete"));
                stmt.setObject(3, parametresData.get("pointure"));
                stmt.setObject(4, parametresData.get("tour_hanche"));
                stmt.setObject(5, parametresData.get("tour_poignet"));
                stmt.setString(6, (String) parametresData.get("taille"));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Réinitialise le formulaire pour un nouvel enregistrement.
     */
    private void resetForm() {
       
        // Réinitialiser les données du formulaire
        formData.clear();
        
        // Réinitialiser l'historique de navigation
        navigationHistory.clear();
        
        // Revenir à la première section
        navigateToSection(0, 0);
    }
    
    /**
     * Affiche un message d'erreur.
     * 
     * @param message Le message d'erreur à afficher
     */
    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Affiche un message de succès.
     * 
     * @param message Le message de succès à afficher
     */
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    
}
         
    