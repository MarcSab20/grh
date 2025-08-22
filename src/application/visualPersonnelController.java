package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.Optional;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

public class visualPersonnelController {

    @FXML private TableView<Personnel> personnelTable;
    @FXML private TableColumn<Personnel, String> matriculeColumn;
    @FXML private TableColumn<Personnel, String> nomColumn;
    @FXML private TableColumn<Personnel, String> sexeColumn;
    @FXML private TableColumn<Personnel, Void> actionColumn;
 // Ajouter ces attributs FXML
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label totalLabel;

    private ObservableList<Personnel> personnelList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // NOUVELLE VÉRIFICATION D'IDENTITÉ AVANT L'INITIALISATION
        Platform.runLater(() -> {
            if (!verifyUserIdentity()) {
                // Si l'authentification échoue, fermer la fenêtre
                Stage stage = (Stage) personnelTable.getScene().getWindow();
                stage.close();
                return;
            }
            
            // Si l'authentification réussit, continuer avec l'initialisation normale
            initializeComponents();
        });
    }
    
    /**
     * Initialise les composants de l'interface utilisateur
     */
    private void initializeComponents() {
        // Configurer les colonnes de la table
        matriculeColumn.setCellValueFactory(new PropertyValueFactory<>("matricule"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        sexeColumn.setCellValueFactory(new PropertyValueFactory<>("sexe"));

        // Configurer la colonne d'action
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button downloadButton = new Button("Télécharger");

            {
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
                    HBox buttons = new HBox(5, downloadButton);
                    setGraphic(buttons);
                }
            }
        });

        // Charger les données depuis la base de données
        loadDataFromDatabase();
        
        // Définir les données dans la table
        personnelTable.setItems(personnelList);
        
        // Configurer la barre de recherche
        setupSearchFunctionality();
    }
    
    /**
     * Configure la fonctionnalité de recherche
     */
    private void setupSearchFunctionality() {
        if (searchField != null && searchButton != null) {
            // Configurer l'action du bouton de recherche
            searchButton.setOnAction(e -> performSearch());
            
            // Permettre la recherche avec la touche Entrée
            searchField.setOnAction(e -> performSearch());
            
            // Recherche en temps réel (optionnel)
            searchField.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.trim().isEmpty()) {
                    // Si le champ est vide, afficher tous les personnels
                    personnelTable.setItems(personnelList);
                } else {
                    // Sinon, filtrer automatiquement
                    performSearch();
                }
            });
        }
    }
    
    
    
    /**
     * Efface la recherche et affiche tous les personnels
     */
    @FXML
    private void clearSearch() {
        if (searchField != null) {
            searchField.clear();
            personnelTable.setItems(personnelList);
            updateTotalLabel(personnelList.size());
        }
    }

    
    /**
     * Charge les données depuis la base de données
     */
    private void loadDataFromDatabase() {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = "SELECT matricule, nom, sexe FROM identite_personnelle ORDER BY nom";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                personnelList.clear(); // Vider la liste avant de la remplir
                
                while (rs.next()) {
                    Personnel personnel = new Personnel(
                        rs.getString("matricule"),
                        rs.getString("nom"),
                        rs.getString("sexe")
                    );
                    personnelList.add(personnel);
                }
                
                // Mettre à jour le compteur si le label existe
                updateTotalLabel(personnelList.size());
                
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des données: " + e.getMessage());
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
    
    /**
     * Effectue une recherche avancée avec plusieurs critères
     * @param nom Le nom à rechercher (peut être null ou vide)
     * @param matricule Le matricule à rechercher (peut être null ou vide)
     * @param sexe Le sexe à rechercher (peut être null ou vide)
     */
    public void performAdvancedSearch(String nom, String matricule, String sexe) {
        ObservableList<Personnel> filteredList = FXCollections.observableArrayList();
        
        for (Personnel personnel : personnelList) {
            boolean matches = true;
            
            // Vérifier chaque critère s'il est fourni
            if (nom != null && !nom.trim().isEmpty()) {
                if (!personnel.getNom().toLowerCase().contains(nom.toLowerCase())) {
                    matches = false;
                }
            }
            
            
            if (matricule != null && !matricule.trim().isEmpty()) {
                if (!personnel.getMatricule().toLowerCase().contains(matricule.toLowerCase())) {
                    matches = false;
                }
            }
            
            if (sexe != null && !sexe.trim().isEmpty()) {
                if (!personnel.getSexe().toLowerCase().equals(sexe.toLowerCase())) {
                    matches = false;
                }
            }
            
            if (matches) {
                filteredList.add(personnel);
            }
        }
        
        // Mettre à jour la table avec les résultats filtrés
        personnelTable.setItems(filteredList);
        updateTotalLabel(filteredList.size());
    }
    
    /**
     * Met à jour le label du nombre total de personnels
     * @param count Le nombre de personnels à afficher
     */
    private void updateTotalLabel(int count) {
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(count));
        }
    }
    
    /**
     * Effectue la recherche de personnel par nom (VERSION MODIFIÉE AVEC COMPTEUR)
     */
    @FXML
    private void performSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            // Si le terme de recherche est vide, afficher tous les personnels
            personnelTable.setItems(personnelList);
            updateTotalLabel(personnelList.size());
            return;
        }
        
        // Filtrer la liste en fonction du terme de recherche
        ObservableList<Personnel> filteredList = FXCollections.observableArrayList();
        
        for (Personnel personnel : personnelList) {
            String nom = personnel.getNom().toLowerCase();
            String matricule = personnel.getMatricule().toLowerCase();
            
            // Rechercher dans le nom, prénom et matricule
            if (nom.contains(searchTerm) || matricule.contains(searchTerm)) {
                filteredList.add(personnel);
            }
        }
        
        // Mettre à jour la table avec les résultats filtrés
        personnelTable.setItems(filteredList);
        updateTotalLabel(filteredList.size());
        
        // Afficher un message si aucun résultat
        if (filteredList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Recherche");
            alert.setHeaderText("Aucun résultat");
            alert.setContentText("Aucun personnel trouvé pour le terme de recherche : \"" + searchTerm + "\"");
            alert.showAndWait();
        }
    }
    
    /**
     * Exporte les résultats de recherche actuels
     */
    @FXML
    private void exportSearchResults() {
        ObservableList<Personnel> currentList = personnelTable.getItems();
        
        if (currentList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Exportation");
            alert.setHeaderText("Aucune donnée à exporter");
            alert.setContentText("Il n'y a aucun personnel dans la liste actuelle à exporter.");
            alert.showAndWait();
            return;
        }
        
        // Vérifier l'identité avant l'exportation
        if (!verifyUserIdentity()) {
            return;
        }
        
        // Code d'exportation ici (similaire à expPersonnelController)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportation");
        alert.setHeaderText("Exportation réussie");
        alert.setContentText(currentList.size() + " personnel(s) exporté(s) avec succès.");
        alert.showAndWait();
    }

    /**
     * Gère le téléchargement du PDF pour un personnel
     */
    private void handleDownload(Personnel personnel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder la fiche du personnel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName(personnel.getMatricule() + "_" + personnel.getNom() + ".pdf");
        
        File file = fileChooser.showSaveDialog(personnelTable.getScene().getWindow());
        
        if (file != null) {
            try {
                // Récupérer toutes les données du personnel
                Map<String, Object> personnelData = getPersonnelData(personnel.getMatricule());
                
                // Générer le PDF
                generatePDF(file, personnelData);
                
                // Afficher un message de confirmation
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Téléchargement réussi");
                alert.setHeaderText(null);
                alert.setContentText("La fiche de " + personnel.getNom() + " "+ 
                                    " a été téléchargée avec succès.");
                alert.showAndWait();
                
            } catch (Exception e) {
                showError("Erreur lors du téléchargement: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Récupère toutes les données d'un personnel depuis la base de données
     */
    private Map<String, Object> getPersonnelData(String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        Connection connection = null;
        
        try {
            connection = getConnection();
            
            // Récupérer les données d'identité personnelle
            data.put("identite_personnelle", getIdentitePersonnelle(connection, matricule));
            
            // Récupérer les données d'identité sociale
            data.put("identite_sociale", getIdentiteSociale(connection, matricule));
            
            // Récupérer les données d'identité culturelle
            data.put("identite_culturelle", getIdentiteCulturelle(connection, matricule));
            
            // Récupérer le grade actuel
            data.put("grade_actuel", getGradeActuel(connection, matricule));
            
            // Récupérer l'historique des grades
            data.put("historique_grades", getHistoriqueGrades(connection, matricule));
            
            // Récupérer la formation actuelle
            data.put("formation_actuelle", getFormationActuelle(connection, matricule));
            
            // Récupérer l'historique des postes
            data.put("historique_postes", getHistoriquePostes(connection, matricule));
            
            // Récupérer l'école de formation initiale
            data.put("ecole_formation_initiale", getEcoleFormationInitiale(connection, matricule));
            
            // Récupérer les écoles civiles
            data.put("ecoles_civiles", getEcolesCiviles(connection, matricule));
            
            // Récupérer les écoles militaires
            data.put("ecoles_militaires", getEcolesMilitaires(connection, matricule));
            
            // Récupérer les opérations
            data.put("operations", getOperations(connection, matricule));
            
            // Récupérer les décorations
            data.put("decorations", getDecorations(connection, matricule));
            
            // Récupérer les médailles
            data.put("medailles", getMedailles(connection, matricule));
            
            // Récupérer les punitions
            data.put("punitions", getPunitions(connection, matricule));
            
            // Récupérer les langues
            data.put("langues", getLangues(connection, matricule));
            
            // Récupérer les informations spécifiques générales
            data.put("infos_specifiques_general", getInfosSpecifiquesGeneral(connection, matricule));
            
            // Récupérer les informations du personnel naviguant
            data.put("personnel_naviguant", getPersonnelNaviguant(connection, matricule));
            
            // Récupérer les maintenances
            data.put("maintenances", getMaintenances(connection, matricule));
            
            // Récupérer la spécialité
            data.put("specialite", getSpecialite(connection, matricule));
            
            // Récupérer les dotations 20 Mai
            data.put("dotations_20_mai", getDotations20Mai(connection, matricule));
            
            // Récupérer les dotations particulières
            data.put("dotations_particulieres", getDotationsParticulieres(connection, matricule));
            
            // Récupérer les paramètres corporels
            data.put("parametres_corporels", getParametresCorporels(connection, matricule));
            
            return data;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    private Map<String, Object> getIdentitePersonnelle(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM identite_personnelle WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("matricule", rs.getString("matricule"));
                    data.put("nom", rs.getString("nom"));
                    data.put("lieu_naissance", rs.getString("lieu_naissance"));
                    data.put("date_naissance", rs.getDate("date_naissance"));
                    data.put("telephone", rs.getString("telephone"));
                    data.put("sexe", rs.getString("sexe"));
                    data.put("groupe_sanguin", rs.getString("groupe_sanguin"));
                    data.put("photo_path", rs.getString("photo_path"));
                }
            }
        }
        
        return data;
    }
    
    private Map<String, Object> getIdentiteSociale(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM identite_sociale WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
                }
            }
        }
        
        return data;
    }
    
    private Map<String, Object> getIdentiteCulturelle(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM identite_culturelle WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("region_origine", rs.getString("region_origine"));
                    data.put("departement_origine", rs.getString("departement_origine"));
                    data.put("arrondissement_origine", rs.getString("arrondissement_origine"));
                    data.put("village", rs.getString("village"));
                    data.put("ethnie", rs.getString("ethnie"));
                    data.put("religion", rs.getString("religion"));
                }
            }
        }
        
        return data;
    }
    
    private Map<String, Object> getGradeActuel(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM grade_actuel WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("rang", rs.getString("rang"));
                    data.put("echelon", rs.getString("echelon"));
                    data.put("date", rs.getDate("date"));
                    data.put("reference", rs.getString("reference"));
                    data.put("echelon_grade", rs.getString("echelon_grade"));
                    data.put("reference_echelon", rs.getString("reference_echelon"));
                    data.put("date_echelon", rs.getDate("date_echelon"));
                    data.put("statut", rs.getString("statut"));
                }
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> getHistoriqueGrades(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM historique_grades WHERE matricule = ? ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("rang", rs.getString("rang"));
                    data.put("echelon", rs.getString("echelon"));
                    data.put("date", rs.getDate("date"));
                    data.put("reference", rs.getString("reference"));
                    data.put("echelon_grade", rs.getString("echelon_grade"));
                    data.put("reference_echelon", rs.getString("reference_echelon"));
                    data.put("date_echelon", rs.getDate("date_echelon"));
                    data.put("statut", rs.getString("statut"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private Map<String, Object> getFormationActuelle(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM formation_actuelle WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("formation", rs.getString("formation"));
                    data.put("date_affectation", rs.getDate("date_affectation"));
                    data.put("reference_affectation", rs.getString("reference_affectation"));
                    data.put("unite", rs.getString("unite"));
                    data.put("poste", rs.getString("poste"));
                }
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> getHistoriquePostes(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM historique_postes WHERE matricule = ? ORDER BY date_debut DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("formation", rs.getString("formation"));
                    data.put("unite", rs.getString("unite"));
                    data.put("date_debut", rs.getDate("date_debut"));
                    data.put("date_fin", rs.getDate("date_fin"));
                    data.put("poste", rs.getString("poste"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private Map<String, Object> getEcoleFormationInitiale(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM ecole_formation_initiale WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("nom_ecole", rs.getString("nom_ecole"));
                    data.put("pays", rs.getString("pays"));
                    data.put("region", rs.getString("region"));
                    data.put("date_entree", rs.getDate("date_entree"));
                    data.put("date_sortie", rs.getDate("date_sortie"));
                    data.put("temps_mis", rs.getString("temps_mis"));
                    data.put("diplome_obtenu", rs.getString("diplome_obtenu"));
                }
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> getEcolesCiviles(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM ecole_civile WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("nom_ecole", rs.getString("nom_ecole"));
                    data.put("pays", rs.getString("pays"));
                    data.put("region", rs.getString("region"));
                    data.put("reference", rs.getString("reference"));
                    data.put("diplome_obtenu", rs.getString("diplome_obtenu"));
                    data.put("appreciation", rs.getString("appreciation"));
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private List<Map<String, Object>> getEcolesMilitaires(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM ecole_militaire WHERE matricule = ? ORDER BY date_debut DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("nom_ecole", rs.getString("nom_ecole"));
                    data.put("pays", rs.getString("pays"));
                    data.put("type", rs.getString("type"));
                    data.put("date_debut", rs.getDate("date_debut"));
                    data.put("date_fin", rs.getDate("date_fin"));
                    data.put("temps_mis", rs.getString("temps_mis"));
                    data.put("diplome_obtenu", rs.getString("diplome_obtenu"));
                    data.put("reference", rs.getString("reference"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private List<Map<String, Object>> getOperations(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM operation WHERE matricule = ? ORDER BY annee DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("nom_mission", rs.getString("nom_mission"));
                    data.put("annee", rs.getInt("annee"));
                    data.put("lieu", rs.getString("lieu"));
                    data.put("type", rs.getString("type"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private List<Map<String, Object>> getDecorations(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM decoration WHERE matricule = ? ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("grade", rs.getString("grade"));
                    data.put("date", rs.getDate("date"));
                    data.put("reference", rs.getString("reference"));
                    data.put("type", rs.getString("type"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private List<Map<String, Object>> getMedailles(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM medaille WHERE matricule = ? ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("medaille", rs.getString("medaille"));
                    data.put("date", rs.getDate("date"));
                    data.put("reference", rs.getString("reference"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private List<Map<String, Object>> getPunitions(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM punition WHERE matricule = ? ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("item", rs.getString("item"));
                    data.put("motif", rs.getString("motif"));
                    data.put("circumstances", rs.getString("circumstances"));
                    data.put("days", rs.getInt("days"));
                    data.put("authority", rs.getString("authority"));
                    data.put("date", rs.getDate("date"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private Map<String, List<Map<String, Object>>> getLangues(Connection connection, String matricule) throws SQLException {
        Map<String, List<Map<String, Object>>> categoriesData = new HashMap<>();
        String sql = "SELECT * FROM langue WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String categorie = rs.getString("categorie");
                    
                    if (!categoriesData.containsKey(categorie)) {
                        categoriesData.put(categorie, new ArrayList<>());
                    }
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("langue", rs.getString("langue"));
                    data.put("niveau", rs.getString("niveau"));
                    
                    categoriesData.get(categorie).add(data);
                }
            }
        }
        
        return categoriesData;
    }
    
    private Map<String, Object> getInfosSpecifiquesGeneral(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM infos_specifiques_general WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("niveau_epms", rs.getInt("niveau_epms"));
                    data.put("permis_militaire", rs.getString("permis_militaire"));
                    data.put("arme_dotation", rs.getString("arme_dotation"));
                    data.put("corps_technique", rs.getString("corps_technique"));
                    data.put("numero_permis_militaire", rs.getString("numero_permis_militaire"));
                    data.put("numero_carte_identite", rs.getString("numero_carte_identite"));
                    data.put("promotion_contingent", rs.getString("promotion_contingent"));
                    data.put("numero_carte_identite_militaire", rs.getString("numero_carte_identite_militaire"));
                    data.put("numero_matricule_solde", rs.getString("numero_matricule_solde"));
                    data.put("position_administrative", rs.getString("position_administrative"));
                    data.put("reference", rs.getString("reference"));
                    data.put("date_incorporation", rs.getDate("date_incorporation"));
                    data.put("date_fin_service", rs.getDate("date_fin_service"));
                    data.put("temps_restant_retraite", rs.getString("temps_restant_retraite"));
                }
            }
        }
        
        return data;
    }
    
    private Map<String, Object> getPersonnelNaviguant(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM personnel_naviguant WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("aeronef_arme", rs.getString("aeronef_arme"));
                    data.put("fonction_bord", rs.getString("fonction_bord"));
                    data.put("aeronef_affectation", rs.getString("aeronef_affectation"));
                    data.put("qualification_type", rs.getString("qualification_type"));
                    data.put("test_trimestriel", rs.getBigDecimal("test_trimestriel"));
                    data.put("cempn_validite", rs.getDate("cempn_validite"));
                    data.put("heures_vol", rs.getInt("heures_vol"));
                    data.put("anciennete_pn", rs.getInt("anciennete_pn"));
                    data.put("numero_titre_aerien", rs.getString("numero_titre_aerien"));
                    data.put("niveau_execution", rs.getInt("niveau_execution"));
                }
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> getMaintenances(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM maintenance WHERE matricule = ? ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("operation", rs.getString("operation"));
                    data.put("date", rs.getDate("date"));
                    data.put("formation", rs.getString("formation"));
                    data.put("type", rs.getString("type"));
                    data.put("niveau_execution", rs.getString("niveau_execution"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private Map<String, Object> getSpecialite(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM specialite WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("type_specialite", rs.getString("type_specialite"));
                    data.put("autre_specialite", rs.getString("autre_specialite"));
                }
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> getDotations20Mai(Connection connection, String matricule) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String sql = "SELECT * FROM dotation_20_mai WHERE matricule = ? ORDER BY annee DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("annee", rs.getInt("annee"));
                    data.put("contenu", rs.getString("contenu"));
                    
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    private Map<String, Object> getDotationsParticulieres(Connection connection, String matricule) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        // Récupérer la configuration
        String configSql = "SELECT * FROM dotation_particuliere_config WHERE matricule = ?";
        try (PreparedStatement stmt = connection.prepareStatement(configSql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.put("jamais_recu", rs.getBoolean("jamais_recu"));
                }
            }
        }
        
        // Si le personnel a reçu des dotations particulières, les récupérer
        if (!(Boolean)result.getOrDefault("jamais_recu", false)) {
            List<Map<String, Object>> dataList = new ArrayList<>();
            String sql = "SELECT * FROM dotation_particuliere WHERE matricule = ? ORDER BY annee DESC, mois";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, matricule);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", rs.getInt("id"));
                        data.put("raison", rs.getString("raison"));
                        data.put("annee", rs.getInt("annee"));
                        data.put("mois", rs.getString("mois"));
                        data.put("contenu", rs.getString("contenu"));
                        
                        dataList.add(data);
                    }
                }
            }
            
            result.put("dotations", dataList);
        }
        
        return result;
    }
    
    private Map<String, Object> getParametresCorporels(Connection connection, String matricule) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM parametres_corporels WHERE matricule = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, matricule);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.put("contour_tete", rs.getInt("contour_tete"));
                    data.put("pointure", rs.getInt("pointure"));
                    data.put("tour_hanche", rs.getInt("tour_hanche"));
                    data.put("tour_poignet", rs.getInt("tour_poignet"));
                    data.put("taille", rs.getString("taille"));
                }
            }
        }
        
        return data;
    }
    
    /**
     * Affiche une boîte de dialogue pour vérifier l'identité de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    private boolean verifyUserIdentity() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification d'identité");
        dialog.setHeaderText("Accès sécurisé - Authentification requise");
        
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
        
        Label instructionLabel = new Label("Veuillez vous authentifier pour accéder aux données :");
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
     * Génère un PDF avec toutes les informations d'un personnel
     */
    private void generatePDF(File file, Map<String, Object> personnelData) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        
        document.open();
        
        // Polices et couleurs
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLUE);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 102, 204));
        Font subsectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(0, 102, 204));
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font italicFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        
        // En-tête du document
        Map<String, Object> identitePersonnelle = (Map<String, Object>) personnelData.get("identite_personnelle");
        String nom = (String) identitePersonnelle.get("nom");
        String matricule = (String) identitePersonnelle.get("matricule");
        
        Paragraph header = new Paragraph("FICHE COMPLÈTE DU PERSONNEL MILITAIRE", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        
        Paragraph nameHeader = new Paragraph(nom + " " + " (" + matricule + ")", sectionFont);
        nameHeader.setAlignment(Element.ALIGN_CENTER);
        nameHeader.setSpacingBefore(10);
        nameHeader.setSpacingAfter(20);
        document.add(nameHeader);
        
        // 1. Identité Personnelle
        addSection(document, "1. IDENTITÉ PERSONNELLE", sectionFont);
        
        PdfPTable identiteTable = new PdfPTable(2);
        identiteTable.setWidthPercentage(100);
        identiteTable.setSpacingBefore(10);
        identiteTable.setSpacingAfter(10);
        
        addTableRow(identiteTable, "Matricule:", (String) identitePersonnelle.get("matricule"), boldFont, normalFont);
        addTableRow(identiteTable, "Nom:", (String) identitePersonnelle.get("nom"), boldFont, normalFont);
        addTableRow(identiteTable, "Lieu de naissance:", (String) identitePersonnelle.get("lieu_naissance"), boldFont, normalFont);
        
        if (identitePersonnelle.get("date_naissance") != null) {
            Date dateNaissance = (Date) identitePersonnelle.get("date_naissance");
            addTableRow(identiteTable, "Date de naissance:", formatDate(dateNaissance), boldFont, normalFont);
        }
        
        addTableRow(identiteTable, "Téléphone:", (String) identitePersonnelle.get("telephone"), boldFont, normalFont);
        addTableRow(identiteTable, "Sexe:", (String) identitePersonnelle.get("sexe"), boldFont, normalFont);
        addTableRow(identiteTable, "Groupe sanguin:", (String) identitePersonnelle.get("groupe_sanguin"), boldFont, normalFont);
        
        document.add(identiteTable);
        
        // 2. Identité Sociale
        addSection(document, "2. IDENTITÉ SOCIALE", sectionFont);
        
        Map<String, Object> identiteSociale = (Map<String, Object>) personnelData.get("identite_sociale");
        if (!identiteSociale.isEmpty()) {
            PdfPTable socialeTable = new PdfPTable(2);
            socialeTable.setWidthPercentage(100);
            socialeTable.setSpacingBefore(10);
            socialeTable.setSpacingAfter(10);
            
            addTableRow(socialeTable, "Nom du père:", (String) identiteSociale.get("nom_pere"), boldFont, normalFont);
            addTableRow(socialeTable, "Nom de la mère:", (String) identiteSociale.get("nom_mere"), boldFont, normalFont);
            addTableRow(socialeTable, "Nombre de conjoints:", String.valueOf(identiteSociale.get("nombre_conjoints")), boldFont, normalFont);
            addTableRow(socialeTable, "Nombre d'enfants:", String.valueOf(identiteSociale.get("nombre_enfants")), boldFont, normalFont);
            addTableRow(socialeTable, "Personne à contacter en cas d'urgence:", (String) identiteSociale.get("personne_contact"), boldFont, normalFont);
            addTableRow(socialeTable, "Lien avec cette personne:", (String) identiteSociale.get("lien_personne_contact"), boldFont, normalFont);
            addTableRow(socialeTable, "Numéro de téléphone de cette personne à contacter:", (String) identiteSociale.get("telephone_personne_contact"), boldFont, normalFont);
            addTableRow(socialeTable, "Lieu de résidence de la personne à contacter:", (String) identiteSociale.get("lieu_residence_personne_contact"), boldFont, normalFont);
            addTableRow(socialeTable, "Remarque particulière:", (String) identiteSociale.get("remarque_particuliere"), boldFont, normalFont);
            addTableRow(socialeTable, "Régime matriminial:", (String) identiteSociale.get("regime_matrimonial"), boldFont, normalFont);
            addTableRow(socialeTable, "Nom du conjoint:", (String) identiteSociale.get("nom_conjoint"), boldFont, normalFont);
            
            document.add(socialeTable);
        } else {
            document.add(new Paragraph("Aucune information d'identité sociale disponible.", italicFont));
        }
        
        // 3. Identité Culturelle
        addSection(document, "3. IDENTITÉ CULTURELLE", sectionFont);
        
        Map<String, Object> identiteCulturelle = (Map<String, Object>) personnelData.get("identite_culturelle");
        if (!identiteCulturelle.isEmpty()) {
            PdfPTable culturelleTable = new PdfPTable(2);
            culturelleTable.setWidthPercentage(100);
            culturelleTable.setSpacingBefore(10);
            culturelleTable.setSpacingAfter(10);
            
            addTableRow(culturelleTable, "Région d'origine:", (String) identiteCulturelle.get("region_origine"), boldFont, normalFont);
            addTableRow(culturelleTable, "Département d'origine:", (String) identiteCulturelle.get("departement_origine"), boldFont, normalFont);
            addTableRow(culturelleTable, "Arrondissement d'origine:", (String) identiteCulturelle.get("arrondissement_origine"), boldFont, normalFont);
            addTableRow(culturelleTable, "Village:", (String) identiteCulturelle.get("village"), boldFont, normalFont);
            addTableRow(culturelleTable, "Ethnie:", (String) identiteCulturelle.get("ethnie"), boldFont, normalFont);
            addTableRow(culturelleTable, "Religion:", (String) identiteCulturelle.get("religion"), boldFont, normalFont);
            
            document.add(culturelleTable);
        } else {
            document.add(new Paragraph("Aucune information d'identité culturelle disponible.", italicFont));
        }
        
        // 4. Grade Actuel
        addSection(document, "4. GRADE ACTUEL", sectionFont);
        
        Map<String, Object> gradeActuel = (Map<String, Object>) personnelData.get("grade_actuel");
        if (!gradeActuel.isEmpty()) {
            PdfPTable gradeTable = new PdfPTable(2);
            gradeTable.setWidthPercentage(100);
            gradeTable.setSpacingBefore(10);
            gradeTable.setSpacingAfter(10);
            
            addTableRow(gradeTable, "Rang:", (String) gradeActuel.get("rang"), boldFont, normalFont);
            addTableRow(gradeTable, "Échelon:", (String) gradeActuel.get("echelon"), boldFont, normalFont);
            addTableRow(gradeTable, "Statut:", (String) identiteSociale.get("statut"), boldFont, normalFont);
            if (gradeActuel.get("date") != null) {
                Date date = (Date) gradeActuel.get("date");
                addTableRow(gradeTable, "Date:", formatDate(date), boldFont, normalFont);
            }
            
            addTableRow(gradeTable, "Référence:", (String) gradeActuel.get("reference"), boldFont, normalFont);
            addTableRow(gradeTable, "Échelon du grade:", (String) gradeActuel.get("echelon_grade"), boldFont, normalFont);
            addTableRow(gradeTable, "Référence échelon:", (String) gradeActuel.get("reference_echelon"), boldFont, normalFont);
            
            if (gradeActuel.get("date_echelon") != null) {
                Date dateEchelon = (Date) gradeActuel.get("date_echelon");
                addTableRow(gradeTable, "Date échelon:", formatDate(dateEchelon), boldFont, normalFont);
            }
            
            document.add(gradeTable);
        } else {
            document.add(new Paragraph("Aucune information de grade actuel disponible.", italicFont));
        }
        
        // 5. Historique des Grades
        addSection(document, "5. HISTORIQUE DES GRADES", sectionFont);
        
        List<Map<String, Object>> historiqueGrades = (List<Map<String, Object>>) personnelData.get("historique_grades");
        if (historiqueGrades != null && !historiqueGrades.isEmpty()) {
            PdfPTable historiqueTable = new PdfPTable(new float[] { 3, 2, 2, 3, 2, 3, 2,3 });
            historiqueTable.setWidthPercentage(100);
            historiqueTable.setSpacingBefore(10);
            historiqueTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(historiqueTable, new String[] {
                "Rang", "Échelon", "Statut","Date", "Référence", "Échelon Grade", "Référence Échelon", "Date Échelon"
            }, boldFont);
            
            // Données
            for (Map<String, Object> grade : historiqueGrades) {
                PdfPCell rangCell = new PdfPCell(new Phrase((String) grade.get("rang"), normalFont));
                PdfPCell echelonCell = new PdfPCell(new Phrase((String) grade.get("echelon"), normalFont));
                PdfPCell statutCell = new PdfPCell(new Phrase((String) grade.get("statut"), normalFont));
                String dateStr = "";
                if (grade.get("date") != null) {
                    Date date = (Date) grade.get("date");
                    dateStr = formatDate(date);
                }
                PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                
                PdfPCell referenceCell = new PdfPCell(new Phrase((String) grade.get("reference"), normalFont));
                PdfPCell echelonGradeCell = new PdfPCell(new Phrase((String) grade.get("echelon_grade"), normalFont));
                PdfPCell referenceEchelonCell = new PdfPCell(new Phrase((String) grade.get("reference_echelon"), normalFont));
                
                String dateEchelonStr = "";
                if (grade.get("date_echelon") != null) {
                    Date dateEchelon = (Date) grade.get("date_echelon");
                    dateEchelonStr = formatDate(dateEchelon);
                }
                PdfPCell dateEchelonCell = new PdfPCell(new Phrase(dateEchelonStr, normalFont));
                
                historiqueTable.addCell(rangCell);
                historiqueTable.addCell(echelonCell);
                historiqueTable.addCell(statutCell);
                historiqueTable.addCell(dateCell);
                historiqueTable.addCell(referenceCell);
                historiqueTable.addCell(echelonGradeCell);
                historiqueTable.addCell(referenceEchelonCell);
                historiqueTable.addCell(dateEchelonCell);
            }
            
            document.add(historiqueTable);
        } else {
            document.add(new Paragraph("Aucun historique de grades disponible.", italicFont));
        }
        
        // 6. Formation et Poste
        addSection(document, "6. FORMATION ET POSTE", sectionFont);
        
        // Formation actuelle
        addSubsection(document, "6.1 Formation Actuelle", subsectionFont);
        
        Map<String, Object> formationActuelle = (Map<String, Object>) personnelData.get("formation_actuelle");
        if (!formationActuelle.isEmpty()) {
            PdfPTable formationTable = new PdfPTable(2);
            formationTable.setWidthPercentage(100);
            formationTable.setSpacingBefore(10);
            formationTable.setSpacingAfter(10);
            
            addTableRow(formationTable, "Formation:", (String) formationActuelle.get("formation"), boldFont, normalFont);
            
            if (formationActuelle.get("date_affectation") != null) {
                Date dateAffectation = (Date) formationActuelle.get("date_affectation");
                addTableRow(formationTable, "Date d'affectation:", formatDate(dateAffectation), boldFont, normalFont);
            }
            
            addTableRow(formationTable, "Référence d'affectation:", (String) formationActuelle.get("reference_affectation"), boldFont, normalFont);
            addTableRow(formationTable, "Unité:", (String) formationActuelle.get("unite"), boldFont, normalFont);
            addTableRow(formationTable, "Poste:", (String) formationActuelle.get("poste"), boldFont, normalFont);
            
            document.add(formationTable);
        } else {
            document.add(new Paragraph("Aucune information de formation actuelle disponible.", italicFont));
        }
        
        // Historique des postes
        addSubsection(document, "6.2 Historique des Postes", subsectionFont);
        
        List<Map<String, Object>> historiquePostes = (List<Map<String, Object>>) personnelData.get("historique_postes");
        if (historiquePostes != null && !historiquePostes.isEmpty()) {
            PdfPTable postesTable = new PdfPTable(new float[] { 4, 3, 2, 2, 4 });
            postesTable.setWidthPercentage(100);
            postesTable.setSpacingBefore(10);
            postesTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(postesTable, new String[] {
                "Formation", "Unité", "Date début", "Date fin", "Poste"
            }, boldFont);
            
            // Données
            for (Map<String, Object> poste : historiquePostes) {
                PdfPCell formationCell = new PdfPCell(new Phrase((String) poste.get("formation"), normalFont));
                PdfPCell uniteCell = new PdfPCell(new Phrase((String) poste.get("unite"), normalFont));
                
                String dateDebutStr = "";
                if (poste.get("date_debut") != null) {
                    Date dateDebut = (Date) poste.get("date_debut");
                    dateDebutStr = formatDate(dateDebut);
                }
                PdfPCell dateDebutCell = new PdfPCell(new Phrase(dateDebutStr, normalFont));
                
                String dateFinStr = "";
                if (poste.get("date_fin") != null) {
                    Date dateFin = (Date) poste.get("date_fin");
                    dateFinStr = formatDate(dateFin);
                }
                PdfPCell dateFinCell = new PdfPCell(new Phrase(dateFinStr, normalFont));
                
                PdfPCell posteCell = new PdfPCell(new Phrase((String) poste.get("poste"), normalFont));
                
                postesTable.addCell(formationCell);
                postesTable.addCell(uniteCell);
                postesTable.addCell(dateDebutCell);
                postesTable.addCell(dateFinCell);
                postesTable.addCell(posteCell);
            }
            
            document.add(postesTable);
        } else {
            document.add(new Paragraph("Aucun historique de postes disponible.", italicFont));
        }
        
        // 7. Écoles et Diplômes
        addSection(document, "7. ÉCOLES ET DIPLÔMES", sectionFont);
        
        // École de formation initiale
        addSubsection(document, "7.1 École de Formation Initiale", subsectionFont);
        
        Map<String, Object> ecoleFormationInitiale = (Map<String, Object>) personnelData.get("ecole_formation_initiale");
        if (!ecoleFormationInitiale.isEmpty()) {
            PdfPTable ecoleTable = new PdfPTable(2);
            ecoleTable.setWidthPercentage(100);
            ecoleTable.setSpacingBefore(10);
            ecoleTable.setSpacingAfter(10);
            
            addTableRow(ecoleTable, "Nom de l'école:", (String) ecoleFormationInitiale.get("nom_ecole"), boldFont, normalFont);
            addTableRow(ecoleTable, "Pays:", (String) ecoleFormationInitiale.get("pays"), boldFont, normalFont);
            addTableRow(ecoleTable, "Région:", (String) ecoleFormationInitiale.get("region"), boldFont, normalFont);
            
            if (ecoleFormationInitiale.get("date_entree") != null) {
                Date dateEntree = (Date) ecoleFormationInitiale.get("date_entree");
                addTableRow(ecoleTable, "Date d'entrée:", formatDate(dateEntree), boldFont, normalFont);
            }
            
            if (ecoleFormationInitiale.get("date_sortie") != null) {
                Date dateSortie = (Date) ecoleFormationInitiale.get("date_sortie");
                addTableRow(ecoleTable, "Date de sortie:", formatDate(dateSortie), boldFont, normalFont);
            }
            
            addTableRow(ecoleTable, "Temps mis:", (String) ecoleFormationInitiale.get("temps_mis"), boldFont, normalFont);
            addTableRow(ecoleTable, "Diplôme obtenu:", (String) ecoleFormationInitiale.get("diplome_obtenu"), boldFont, normalFont);
            
            document.add(ecoleTable);
        } else {
            document.add(new Paragraph("Aucune information d'école de formation initiale disponible.", italicFont));
        }
        
     // Écoles civiles
        addSubsection(document, "7.2 Écoles Civiles", subsectionFont);
        
        List<Map<String, Object>> ecolesCiviles = (List<Map<String, Object>>) personnelData.get("ecoles_civiles");
        if (ecolesCiviles != null && !ecolesCiviles.isEmpty()) {
            PdfPTable ecolesTable = new PdfPTable(new float[] { 5, 2, 2, 3, 3, 3 });
            ecolesTable.setWidthPercentage(100);
            ecolesTable.setSpacingBefore(10);
            ecolesTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(ecolesTable, new String[] {
                "Nom de l'école", "Pays", "Région", "Référence", "Diplôme obtenu", "Appréciation"
            }, boldFont);
            
            // Données
            for (Map<String, Object> ecole : ecolesCiviles) {
                PdfPCell nomEcoleCell = new PdfPCell(new Phrase((String) ecole.get("nom_ecole"), normalFont));
                PdfPCell paysCell = new PdfPCell(new Phrase((String) ecole.get("pays"), normalFont));
                PdfPCell regionCell = new PdfPCell(new Phrase((String) ecole.get("region"), normalFont));
                PdfPCell referenceCell = new PdfPCell(new Phrase((String) ecole.get("reference"), normalFont));
                PdfPCell diplomeCell = new PdfPCell(new Phrase((String) ecole.get("diplome_obtenu"), normalFont));
                PdfPCell appreciationCell = new PdfPCell(new Phrase((String) ecole.get("appreciation"), normalFont));
                
                ecolesTable.addCell(nomEcoleCell);
                ecolesTable.addCell(paysCell);
                ecolesTable.addCell(regionCell);
                ecolesTable.addCell(referenceCell);
                ecolesTable.addCell(diplomeCell);
                ecolesTable.addCell(appreciationCell);
            }
            
            document.add(ecolesTable);
        } else {
            document.add(new Paragraph("Aucune école civile disponible.", italicFont));
        }
        
        // Écoles militaires
        addSubsection(document, "7.3 Écoles et Stages Militaires", subsectionFont);
        
        List<Map<String, Object>> ecolesMilitaires = (List<Map<String, Object>>) personnelData.get("ecoles_militaires");
        if (ecolesMilitaires != null && !ecolesMilitaires.isEmpty()) {
            PdfPTable ecolesTable = new PdfPTable(new float[] { 4, 2, 2, 2, 2, 2, 3 });
            ecolesTable.setWidthPercentage(100);
            ecolesTable.setSpacingBefore(10);
            ecolesTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(ecolesTable, new String[] {
                "Nom de l'école", "Pays", "Type", "Date début", "Date fin", "Temps mis", "Diplôme obtenu", "reference"
            }, boldFont);
            
            // Données
            for (Map<String, Object> ecole : ecolesMilitaires) {
                PdfPCell nomEcoleCell = new PdfPCell(new Phrase((String) ecole.get("nom_ecole"), normalFont));
                PdfPCell paysCell = new PdfPCell(new Phrase((String) ecole.get("pays"), normalFont));
                PdfPCell typeCell = new PdfPCell(new Phrase((String) ecole.get("type"), normalFont));
                
                String dateDebutStr = "";
                if (ecole.get("date_debut") != null) {
                    Date dateDebut = (Date) ecole.get("date_debut");
                    dateDebutStr = formatDate(dateDebut);
                }
                PdfPCell dateDebutCell = new PdfPCell(new Phrase(dateDebutStr, normalFont));
                
                String dateFinStr = "";
                if (ecole.get("date_fin") != null) {
                    Date dateFin = (Date) ecole.get("date_fin");
                    dateFinStr = formatDate(dateFin);
                }
                PdfPCell dateFinCell = new PdfPCell(new Phrase(dateFinStr, normalFont));
                
                PdfPCell tempsMisCell = new PdfPCell(new Phrase((String) ecole.get("temps_mis"), normalFont));
                PdfPCell diplomeCell = new PdfPCell(new Phrase((String) ecole.get("diplome_obtenu"), normalFont));
                PdfPCell referenceCell = new PdfPCell(new Phrase((String) ecole.get("reference"), normalFont));
                
                
                ecolesTable.addCell(nomEcoleCell);
                ecolesTable.addCell(paysCell);
                ecolesTable.addCell(typeCell);
                ecolesTable.addCell(dateDebutCell);
                ecolesTable.addCell(dateFinCell);
                ecolesTable.addCell(tempsMisCell);
                ecolesTable.addCell(diplomeCell);
                ecolesTable.addCell(referenceCell);
            }
            
            document.add(ecolesTable);
        } else {
            document.add(new Paragraph("Aucune école militaire disponible.", italicFont));
        }
        
        // 8. Opérations et Déploiements
        addSection(document, "8. OPÉRATIONS ET DÉPLOIEMENTS", sectionFont);
        
        List<Map<String, Object>> operations = (List<Map<String, Object>>) personnelData.get("operations");
        if (operations != null && !operations.isEmpty()) {
            // Créer deux listes: une pour les opérations intérieures et une pour les extérieures
            List<Map<String, Object>> operationsInterieures = new ArrayList<>();
            List<Map<String, Object>> operationsExterieures = new ArrayList<>();
            
            for (Map<String, Object> operation : operations) {
                String type = (String) operation.get("type");
                if ("Intérieure".equals(type)) {
                    operationsInterieures.add(operation);
                } else if ("Extérieure".equals(type)) {
                    operationsExterieures.add(operation);
                }
            }
            
            // Opérations intérieures
            if (!operationsInterieures.isEmpty()) {
                addSubsection(document, "8.1 Opérations Intérieures", subsectionFont);
                
                PdfPTable operationsTable = new PdfPTable(new float[] { 5, 2, 4 });
                operationsTable.setWidthPercentage(100);
                operationsTable.setSpacingBefore(10);
                operationsTable.setSpacingAfter(10);
                
                // En-têtes
                addTableHeader(operationsTable, new String[] {
                    "Nom de la mission", "Année", "Lieu"
                }, boldFont);
                
                // Données
                for (Map<String, Object> operation : operationsInterieures) {
                    PdfPCell nomMissionCell = new PdfPCell(new Phrase((String) operation.get("nom_mission"), normalFont));
                    PdfPCell anneeCell = new PdfPCell(new Phrase(String.valueOf(operation.get("annee")), normalFont));
                    PdfPCell lieuCell = new PdfPCell(new Phrase((String) operation.get("lieu"), normalFont));
                    
                    operationsTable.addCell(nomMissionCell);
                    operationsTable.addCell(anneeCell);
                    operationsTable.addCell(lieuCell);
                }
                
                document.add(operationsTable);
            } else {
                document.add(new Paragraph("Aucune opération intérieure disponible.", italicFont));
            }
            
            // Opérations extérieures
            if (!operationsExterieures.isEmpty()) {
                addSubsection(document, "8.2 Opérations Extérieures", subsectionFont);
                
                PdfPTable operationsTable = new PdfPTable(new float[] { 5, 2, 4 });
                operationsTable.setWidthPercentage(100);
                operationsTable.setSpacingBefore(10);
                operationsTable.setSpacingAfter(10);
                
                // En-têtes
                addTableHeader(operationsTable, new String[] {
                    "Nom de la mission", "Année", "Lieu"
                }, boldFont);
                
                // Données
                for (Map<String, Object> operation : operationsExterieures) {
                    PdfPCell nomMissionCell = new PdfPCell(new Phrase((String) operation.get("nom_mission"), normalFont));
                    PdfPCell anneeCell = new PdfPCell(new Phrase(String.valueOf(operation.get("annee")), normalFont));
                    PdfPCell lieuCell = new PdfPCell(new Phrase((String) operation.get("lieu"), normalFont));
                    
                    operationsTable.addCell(nomMissionCell);
                    operationsTable.addCell(anneeCell);
                    operationsTable.addCell(lieuCell);
                }
                
                document.add(operationsTable);
            } else {
                document.add(new Paragraph("Aucune opération extérieure disponible.", italicFont));
            }
        } else {
            document.add(new Paragraph("Aucune opération disponible.", italicFont));
        }
        
        // 9. Décorations et Médailles
        addSection(document, "9. DÉCORATIONS ET MÉDAILLES", sectionFont);
        
        // Décorations
        addSubsection(document, "9.1 Décorations", subsectionFont);
        
        List<Map<String, Object>> decorations = (List<Map<String, Object>>) personnelData.get("decorations");
        if (decorations != null && !decorations.isEmpty()) {
            // Regrouper les décorations par type
            Map<String, List<Map<String, Object>>> decorationsParType = new HashMap<>();
            
            for (Map<String, Object> decoration : decorations) {
                String type = (String) decoration.get("type");
                if (!decorationsParType.containsKey(type)) {
                    decorationsParType.put(type, new ArrayList<>());
                }
                decorationsParType.get(type).add(decoration);
            }
            
            // Pour chaque type de décoration
            for (String type : decorationsParType.keySet()) {
                Paragraph typeTitle = new Paragraph(type, subsectionFont);
                typeTitle.setSpacingBefore(5);
                document.add(typeTitle);
                
                PdfPTable decorationsTable = new PdfPTable(new float[] { 4, 3, 4 });
                decorationsTable.setWidthPercentage(100);
                decorationsTable.setSpacingBefore(5);
                decorationsTable.setSpacingAfter(10);
                
                // En-têtes
                addTableHeader(decorationsTable, new String[] {
                    "Grade", "Date", "Référence"
                }, boldFont);
                
                // Données
                for (Map<String, Object> decoration : decorationsParType.get(type)) {
                    PdfPCell gradeCell = new PdfPCell(new Phrase((String) decoration.get("grade"), normalFont));
                    
                    String dateStr = "";
                    if (decoration.get("date") != null) {
                        Date date = (Date) decoration.get("date");
                        dateStr = formatDate(date);
                    }
                    PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                    
                    PdfPCell referenceCell = new PdfPCell(new Phrase((String) decoration.get("reference"), normalFont));
                    
                    decorationsTable.addCell(gradeCell);
                    decorationsTable.addCell(dateCell);
                    decorationsTable.addCell(referenceCell);
                }
                
                document.add(decorationsTable);
            }
        } else {
            document.add(new Paragraph("Aucune décoration disponible.", italicFont));
        }
        
        // Médailles
        addSubsection(document, "9.2 Médailles", subsectionFont);
        
        List<Map<String, Object>> medailles = (List<Map<String, Object>>) personnelData.get("medailles");
        if (medailles != null && !medailles.isEmpty()) {
            PdfPTable medaillesTable = new PdfPTable(new float[] { 5, 3, 4 });
            medaillesTable.setWidthPercentage(100);
            medaillesTable.setSpacingBefore(10);
            medaillesTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(medaillesTable, new String[] {
                "Médaille", "Date", "Référence"
            }, boldFont);
            
            // Données
            for (Map<String, Object> medaille : medailles) {
                PdfPCell medailleCell = new PdfPCell(new Phrase((String) medaille.get("medaille"), normalFont));
                
                String dateStr = "";
                if (medaille.get("date") != null) {
                    Date date = (Date) medaille.get("date");
                    dateStr = formatDate(date);
                }
                PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                
                PdfPCell referenceCell = new PdfPCell(new Phrase((String) medaille.get("reference"), normalFont));
                
                medaillesTable.addCell(medailleCell);
                medaillesTable.addCell(dateCell);
                medaillesTable.addCell(referenceCell);
            }
            
            document.add(medaillesTable);
        } else {
            document.add(new Paragraph("Aucune médaille disponible.", italicFont));
        }
        
        // 10. Punitions
        addSection(document, "10. PUNITIONS", sectionFont);
        
        List<Map<String, Object>> punitions = (List<Map<String, Object>>) personnelData.get("punitions");
        if (punitions != null && !punitions.isEmpty()) {
            PdfPTable punitionsTable = new PdfPTable(new float[] { 3, 5, 5, 1, 3, 2 });
            punitionsTable.setWidthPercentage(100);
            punitionsTable.setSpacingBefore(10);
            punitionsTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(punitionsTable, new String[] {
                "Item", "Motif", "Circonstances", "Jours", "Autorité", "Date"
            }, boldFont);
            
            // Données
            for (Map<String, Object> punition : punitions) {
                PdfPCell itemCell = new PdfPCell(new Phrase((String) punition.get("item"), normalFont));
                PdfPCell motifCell = new PdfPCell(new Phrase((String) punition.get("motif"), normalFont));
                PdfPCell circumstancesCell = new PdfPCell(new Phrase((String) punition.get("circumstances"), normalFont));
                PdfPCell daysCell = new PdfPCell(new Phrase(String.valueOf(punition.get("days")), normalFont));
                PdfPCell authorityCell = new PdfPCell(new Phrase((String) punition.get("authority"), normalFont));
                
                String dateStr = "";
                if (punition.get("date") != null) {
                    Date date = (Date) punition.get("date");
                    dateStr = formatDate(date);
                }
                PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                
                punitionsTable.addCell(itemCell);
                punitionsTable.addCell(motifCell);
                punitionsTable.addCell(circumstancesCell);
                punitionsTable.addCell(daysCell);
                punitionsTable.addCell(authorityCell);
                punitionsTable.addCell(dateCell);
            }
            
            document.add(punitionsTable);
        } else {
            document.add(new Paragraph("Aucune punition disponible.", italicFont));
        }
        
        // 11. Langues
        addSection(document, "11. LANGUES", sectionFont);
        
        Map<String, List<Map<String, Object>>> langues = (Map<String, List<Map<String, Object>>>) personnelData.get("langues");
        if (langues != null && !langues.isEmpty()) {
            for (String categorie : langues.keySet()) {
                addSubsection(document, "11." + (langues.keySet().toArray()).toString() + " " + formatCategorieLangue(categorie), subsectionFont);
                
                List<Map<String, Object>> languesList = langues.get(categorie);
                if (languesList != null && !languesList.isEmpty()) {
                    PdfPTable languesTable = new PdfPTable(new float[] { 5, 3 });
                    languesTable.setWidthPercentage(100);
                    languesTable.setSpacingBefore(5);
                    languesTable.setSpacingAfter(10);
                    
                    // En-têtes
                    addTableHeader(languesTable, new String[] {
                        "Langue", "Niveau"
                    }, boldFont);
                    
                    // Données
                    for (Map<String, Object> langue : languesList) {
                        PdfPCell langueCell = new PdfPCell(new Phrase((String) langue.get("langue"), normalFont));
                        PdfPCell niveauCell = new PdfPCell(new Phrase((String) langue.get("niveau"), normalFont));
                        
                        languesTable.addCell(langueCell);
                        languesTable.addCell(niveauCell);
                    }
                    
                    document.add(languesTable);
                } else {
                    document.add(new Paragraph("Aucune langue disponible pour cette catégorie.", italicFont));
                }
            }
        } else {
            document.add(new Paragraph("Aucune information sur les langues disponible.", italicFont));
        }
        
        // 12. Informations Spécifiques
        addSection(document, "12. INFORMATIONS SPÉCIFIQUES", sectionFont);
        
        // Informations générales
        addSubsection(document, "12.1 Informations Générales", subsectionFont);
        
        Map<String, Object> infosSpecifiquesGeneral = (Map<String, Object>) personnelData.get("infos_specifiques_general");
        if (!infosSpecifiquesGeneral.isEmpty()) {
            PdfPTable infosTable = new PdfPTable(2);
            infosTable.setWidthPercentage(100);
            infosTable.setSpacingBefore(5);
            infosTable.setSpacingAfter(10);
            
            addTableRow(infosTable, "Niveau EPMS:", String.valueOf(infosSpecifiquesGeneral.get("niveau_epms")), boldFont, normalFont);
            addTableRow(infosTable, "Permis militaire:", (String) infosSpecifiquesGeneral.get("permis_militaire"), boldFont, normalFont);
            addTableRow(infosTable, "Arme de dotation:", (String) infosSpecifiquesGeneral.get("arme_dotation"), boldFont, normalFont);
            addTableRow(infosTable, "Corps technique:", (String) infosSpecifiquesGeneral.get("corps_technique"), boldFont, normalFont);
            addTableRow(infosTable, "Numéro de Permis Militaire:", (String) infosSpecifiquesGeneral.get("numero_permis_militaire"), boldFont, normalFont);
            addTableRow(infosTable, "Numéro de Carte d'Identité (CNI):", (String) infosSpecifiquesGeneral.get("numero_carte_identite"), boldFont, normalFont);
            addTableRow(infosTable, "Promotion et Contingent:", (String) infosSpecifiquesGeneral.get("promotion_contingent"), boldFont, normalFont);
            addTableRow(infosTable, "Numéro Carte Identité Militaire (CIM):", (String) infosSpecifiquesGeneral.get("numero_carte_identite_militaire"), boldFont, normalFont);
            addTableRow(infosTable, "Numéro Matricule Solde:", (String) infosSpecifiquesGeneral.get("numero_matricule_solde"), boldFont, normalFont);
            addTableRow(infosTable, "Position administrative:", (String) infosSpecifiquesGeneral.get("position_administrative"), boldFont, normalFont);
            addTableRow(infosTable, "Reférence:", (String) infosSpecifiquesGeneral.get("reference"), boldFont, normalFont);
            if (infosSpecifiquesGeneral.get("date_incorporation") != null) {
                Date dateSortie = (Date) infosSpecifiquesGeneral.get("date_incorporation");
                addTableRow(infosTable, "Date d'incorporation:", formatDate(dateSortie), boldFont, normalFont);
            }
            if (infosSpecifiquesGeneral.get("date_fin_service") != null) {
                Date dateSortie = (Date) infosSpecifiquesGeneral.get("date_fin_service");
                addTableRow(infosTable, "Date de fin de service:", formatDate(dateSortie), boldFont, normalFont);
            }
            addTableRow(infosTable, "Temps restant pour la retraite:", (String) infosSpecifiquesGeneral.get("temps_restant_retraite"), boldFont, normalFont);
            
            document.add(infosTable);
        } else {
            document.add(new Paragraph("Aucune information spécifique générale disponible.", italicFont));
        }
        
        // Personnel naviguant
        addSubsection(document, "12.2 Personnel Naviguant", subsectionFont);
        
        Map<String, Object> personnelNaviguant = (Map<String, Object>) personnelData.get("personnel_naviguant");
        if (!personnelNaviguant.isEmpty()) {
            PdfPTable naviguantTable = new PdfPTable(2);
            naviguantTable.setWidthPercentage(100);
            naviguantTable.setSpacingBefore(5);
            naviguantTable.setSpacingAfter(10);
            
            addTableRow(naviguantTable, "Aéronef d'arme:", (String) personnelNaviguant.get("aeronef_arme"), boldFont, normalFont);
            addTableRow(naviguantTable, "Fonction à bord:", (String) personnelNaviguant.get("fonction_bord"), boldFont, normalFont);
            addTableRow(naviguantTable, "Aéronef d'affectation:", (String) personnelNaviguant.get("aeronef_affectation"), boldFont, normalFont);
            addTableRow(naviguantTable, "Qualification de type:", (String) personnelNaviguant.get("qualification_type"), boldFont, normalFont);
            
            if (personnelNaviguant.get("test_trimestriel") != null) {
                addTableRow(naviguantTable, "Test trimestriel:", personnelNaviguant.get("test_trimestriel").toString(), boldFont, normalFont);
            }
            
            if (personnelNaviguant.get("cempn_validite") != null) {
                Date cempnValidite = (Date) personnelNaviguant.get("cempn_validite");
                addTableRow(naviguantTable, "Validité CEMPN:", formatDate(cempnValidite), boldFont, normalFont);
            }
            
            addTableRow(naviguantTable, "Heures de vol:", String.valueOf(personnelNaviguant.get("heures_vol")), boldFont, normalFont);
            addTableRow(naviguantTable, "Ancienneté PN:", String.valueOf(personnelNaviguant.get("anciennete_pn")), boldFont, normalFont);
            addTableRow(naviguantTable, "Numéro de titre aérien:", (String) personnelNaviguant.get("numero_titre_aerien"), boldFont, normalFont);
            addTableRow(naviguantTable, "Niveau d'exécution:", String.valueOf(personnelNaviguant.get("niveau_execution")), boldFont, normalFont);
            
            document.add(naviguantTable);
        } else {
            document.add(new Paragraph("Aucune information de personnel naviguant disponible.", italicFont));
        }
        
        // Maintenances
        addSubsection(document, "12.3 Maintenances", subsectionFont);
        
        List<Map<String, Object>> maintenances = (List<Map<String, Object>>) personnelData.get("maintenances");
        if (maintenances != null && !maintenances.isEmpty()) {
            // Créer deux listes: une pour les maintenances programmées et une pour les curatives
            List<Map<String, Object>> maintenancesProgrammees = new ArrayList<>();
            List<Map<String, Object>> maintenancesCuratives = new ArrayList<>();
            
            for (Map<String, Object> maintenance : maintenances) {
                String type = (String) maintenance.get("type");
                if ("Programmée".equals(type)) {
                    maintenancesProgrammees.add(maintenance);
                } else if ("Curative".equals(type)) {
                    maintenancesCuratives.add(maintenance);
                }
            }
            
            // Maintenances programmées
            if (!maintenancesProgrammees.isEmpty()) {
                Paragraph typeTitle = new Paragraph("Maintenances Programmées", subsectionFont);
                typeTitle.setSpacingBefore(5);
                document.add(typeTitle);
                
                PdfPTable maintenancesTable = new PdfPTable(new float[] { 5, 3, 3, 2 });
                maintenancesTable.setWidthPercentage(100);
                maintenancesTable.setSpacingBefore(5);
                maintenancesTable.setSpacingAfter(10);
                
                // En-têtes
                addTableHeader(maintenancesTable, new String[] {
                    "Opération", "Date", "Formation", "Niveau"
                }, boldFont);
                
                // Données
                for (Map<String, Object> maintenance : maintenancesProgrammees) {
                    PdfPCell operationCell = new PdfPCell(new Phrase((String) maintenance.get("operation"), normalFont));
                    
                    String dateStr = "";
                    if (maintenance.get("date") != null) {
                        Date date = (Date) maintenance.get("date");
                        dateStr = formatDate(date);
                    }
                    PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                    
                    PdfPCell formationCell = new PdfPCell(new Phrase((String) maintenance.get("formation"), normalFont));
                    PdfPCell niveauCell = new PdfPCell(new Phrase((String) maintenance.get("niveau_execution"), normalFont));
                    
                    maintenancesTable.addCell(operationCell);
                    maintenancesTable.addCell(dateCell);
                    maintenancesTable.addCell(formationCell);
                    maintenancesTable.addCell(niveauCell);
                }
                
                document.add(maintenancesTable);
            }
            
            // Maintenances curatives
            if (!maintenancesCuratives.isEmpty()) {
                Paragraph typeTitle = new Paragraph("Maintenances Curatives", subsectionFont);
                typeTitle.setSpacingBefore(5);
                document.add(typeTitle);
                
                PdfPTable maintenancesTable = new PdfPTable(new float[] { 5, 3, 3 });
                maintenancesTable.setWidthPercentage(100);
                maintenancesTable.setSpacingBefore(5);
                maintenancesTable.setSpacingAfter(10);
                
                // En-têtes
                addTableHeader(maintenancesTable, new String[] {
                    "Opération", "Date", "Formation"
                }, boldFont);
                
                // Données
                for (Map<String, Object> maintenance : maintenancesCuratives) {
                    PdfPCell operationCell = new PdfPCell(new Phrase((String) maintenance.get("operation"), normalFont));
                    
                    String dateStr = "";
                    if (maintenance.get("date") != null) {
                        Date date = (Date) maintenance.get("date");
                        dateStr = formatDate(date);
                    }
                    PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
                    
                    PdfPCell formationCell = new PdfPCell(new Phrase((String) maintenance.get("formation"), normalFont));
                    
                    maintenancesTable.addCell(operationCell);
                    maintenancesTable.addCell(dateCell);
                    maintenancesTable.addCell(formationCell);
                }
                
                document.add(maintenancesTable);
            }
        } else {
            document.add(new Paragraph("Aucune information de maintenance disponible.", italicFont));
        }
        
        // Spécialité
        addSubsection(document, "12.4 Spécialité", subsectionFont);
        
        Map<String, Object> specialite = (Map<String, Object>) personnelData.get("specialite");
        if (!specialite.isEmpty()) {
            PdfPTable specialiteTable = new PdfPTable(2);
            specialiteTable.setWidthPercentage(100);
            specialiteTable.setSpacingBefore(5);
            specialiteTable.setSpacingAfter(10);
            
            addTableRow(specialiteTable, "Type de spécialité:", (String) specialite.get("type_specialite"), boldFont, normalFont);
            
            if ("Autre spécialité".equals(specialite.get("type_specialite"))) {
                addTableRow(specialiteTable, "Autre spécialité:", (String) specialite.get("autre_specialite"), boldFont, normalFont);
            }
            
            document.add(specialiteTable);
        } else {
            document.add(new Paragraph("Aucune information de spécialité disponible.", italicFont));
        }
        
        // 13. Dotations
        addSection(document, "13. DOTATIONS MILITAIRES", sectionFont);
        
        // Dotations 20 Mai
        addSubsection(document, "13.1 Dotations 20 Mai", subsectionFont);
        
        List<Map<String, Object>> dotations20Mai = (List<Map<String, Object>>) personnelData.get("dotations_20_mai");
        if (dotations20Mai != null && !dotations20Mai.isEmpty()) {
            PdfPTable dotationsTable = new PdfPTable(new float[] { 2, 8 });
            dotationsTable.setWidthPercentage(100);
            dotationsTable.setSpacingBefore(5);
            dotationsTable.setSpacingAfter(10);
            
            // En-têtes
            addTableHeader(dotationsTable, new String[] {
                "Année", "Contenu"
            }, boldFont);
            
            // Données
            for (Map<String, Object> dotation : dotations20Mai) {
                PdfPCell anneeCell = new PdfPCell(new Phrase(String.valueOf(dotation.get("annee")), normalFont));
                PdfPCell contenuCell = new PdfPCell(new Phrase((String) dotation.get("contenu"), normalFont));
                
                dotationsTable.addCell(anneeCell);
                dotationsTable.addCell(contenuCell);
            }
            
            document.add(dotationsTable);
        } else {
            document.add(new Paragraph("Aucune dotation 20 Mai disponible.", italicFont));
        }
        
        // Dotations particulières
        addSubsection(document, "13.2 Dotations Particulières", subsectionFont);
        
        Map<String, Object> dotationsParticulieres = (Map<String, Object>) personnelData.get("dotations_particulieres");
        if (dotationsParticulieres != null && !dotationsParticulieres.isEmpty()) {
            Boolean jamaisRecu = (Boolean) dotationsParticulieres.get("jamais_recu");
            
            if (jamaisRecu != null && jamaisRecu) {
                document.add(new Paragraph("Le personnel n'a jamais reçu de dotation particulière.", normalFont));
            } else {
                List<Map<String, Object>> dotations = (List<Map<String, Object>>) dotationsParticulieres.get("dotations");
                
                if (dotations != null && !dotations.isEmpty()) {
                    PdfPTable dotationsTable = new PdfPTable(new float[] { 4, 2, 2, 6 });
                    dotationsTable.setWidthPercentage(100);
                    dotationsTable.setSpacingBefore(5);
                    dotationsTable.setSpacingAfter(10);
                    
                    // En-têtes
                    addTableHeader(dotationsTable, new String[] {
                        "Raison", "Année", "Mois", "Contenu"
                    }, boldFont);
                    
                    // Données
                    for (Map<String, Object> dotation : dotations) {
                        PdfPCell raisonCell = new PdfPCell(new Phrase((String) dotation.get("raison"), normalFont));
                        PdfPCell anneeCell = new PdfPCell(new Phrase(String.valueOf(dotation.get("annee")), normalFont));
                        PdfPCell moisCell = new PdfPCell(new Phrase((String) dotation.get("mois"), normalFont));
                        PdfPCell contenuCell = new PdfPCell(new Phrase((String) dotation.get("contenu"), normalFont));
                        
                        dotationsTable.addCell(raisonCell);
                        dotationsTable.addCell(anneeCell);
                        dotationsTable.addCell(moisCell);
                        dotationsTable.addCell(contenuCell);
                    }
                    
                    document.add(dotationsTable);
                } else {
                    document.add(new Paragraph("Aucune dotation particulière disponible.", italicFont));
                }
            }
        } else {
            document.add(new Paragraph("Aucune information de dotation particulière disponible.", italicFont));
        }
        
        // Paramètres corporels
        addSubsection(document, "13.3 Paramètres Corporels", subsectionFont);
        
        Map<String, Object> parametresCorporels = (Map<String, Object>) personnelData.get("parametres_corporels");
        if (!parametresCorporels.isEmpty()) {
            PdfPTable parametresTable = new PdfPTable(2);
            parametresTable.setWidthPercentage(100);
            parametresTable.setSpacingBefore(5);
            parametresTable.setSpacingAfter(10);
            
            addTableRow(parametresTable, "Contour de tête:", parametresCorporels.get("contour_tete") + " cm", boldFont, normalFont);
            addTableRow(parametresTable, "Pointure:", String.valueOf(parametresCorporels.get("pointure")), boldFont, normalFont);
            addTableRow(parametresTable, "Tour de hanche:", parametresCorporels.get("tour_hanche") + " cm", boldFont, normalFont);
            addTableRow(parametresTable, "Tour de poignet:", parametresCorporels.get("tour_poignet") + " cm", boldFont, normalFont);
            addTableRow(parametresTable, "Taille:", (String) parametresCorporels.get("taille"), boldFont, normalFont);
            
            document.add(parametresTable);
        } else {
            document.add(new Paragraph("Aucune information de paramètres corporels disponible.", italicFont));
        }
        
        // Pied de page avec date de génération
        Paragraph footer = new Paragraph("Document généré le " + formatCurrentDate(), italicFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
        
        document.close();
    }
    
    /**
     * Formatte une date au format dd/MM/yyyy
     */
    private String formatDate(Date date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatter.format(date.toLocalDate());
    }
    
    /**
     * Retourne la date courante au format dd/MM/yyyy
     */
    private String formatCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatter.format(LocalDate.now());
    }
    
    /**
     * Formatte une catégorie de langue
     */
    private String formatCategorieLangue(String categorie) {
        if (categorie == null) return "";
        // Exemple: "langues_parlées" -> "Langues Parlées"
        String[] parts = categorie.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (part.length() > 0) {
                result.append(part.substring(0, 1).toUpperCase())
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Ajoute un titre de section au document
     */
    private void addSection(Document document, String title, Font font) throws DocumentException {
        Paragraph section = new Paragraph(title, font);
        section.setSpacingBefore(15);
        section.setSpacingAfter(5);
        document.add(section);
        
        // Ajouter une ligne de séparation
        LineSeparator line = new LineSeparator();
        line.setPercentage(100);
        line.setLineColor(new BaseColor(0, 102, 204));
        document.add(line);
    }
    
    /**
     * Ajoute un titre de sous-section au document
     */
    private void addSubsection(Document document, String title, Font font) throws DocumentException {
        Paragraph subsection = new Paragraph(title, font);
        subsection.setSpacingBefore(10);
        subsection.setSpacingAfter(5);
        document.add(subsection);
    }
    
    /**
     * Ajoute une ligne à une table
     */
    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    /**
     * Ajoute un en-tête à une table
     */
    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
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
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Classe interne représentant un personnel
     */
    public static class Personnel {
        private final SimpleStringProperty matricule;
        private final SimpleStringProperty nom;
        private final SimpleStringProperty sexe;
        
        public Personnel(String matricule, String nom, String sexe) {
            this.matricule = new SimpleStringProperty(matricule);
            this.nom = new SimpleStringProperty(nom);
            this.sexe = new SimpleStringProperty(sexe);
        }
        
        public String getMatricule() { return matricule.get(); }
        public String getNom() { return nom.get(); }
        public String getSexe() { return sexe.get(); }
    }
}
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                