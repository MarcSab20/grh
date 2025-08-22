package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.beans.property.SimpleStringProperty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class fouillerController {
    @FXML private VBox searchContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> gradeFilter;
    @FXML private ComboBox<String> formationFilter;
    @FXML private TextField nameFilter;
    @FXML private TableView<Personnel> resultTable;
    @FXML private BorderPane borderPane;
    
    private ObservableList<Personnel> personnelData = FXCollections.observableArrayList();
    
    @FXML
    private void initialize() {
        // Configuration des filtres pour le personnel
        setupFilters();
        
        // Configuration de la table des résultats
        setupResultTable();
        
        // Table vide par défaut
        resultTable.setItems(personnelData);
        
        // Chargement des grades depuis la base de données pour le filtre
        chargerGrades();
        
        // Chargement des formations depuis la base de données pour le filtre
        chargerFormations();
    }
    
    private void setupFilters() {
        // Configuration initiale des filtres
        gradeFilter.setValue("Tous");
        
        // Activer le filtre de formation
        formationFilter.setDisable(false);
        formationFilter.setValue("Toutes");
        
        // Ajouter un écouteur d'événement pour le changement de filtre
        gradeFilter.setOnAction(event -> rechercherPersonnel());
        formationFilter.setOnAction(event -> rechercherPersonnel());
        nameFilter.textProperty().addListener((observable, oldValue, newValue) -> rechercherPersonnel());
    }
    
    private void setupResultTable() {
        resultTable.getColumns().clear();
        
        // Création des colonnes pour le Personnel
        TableColumn<Personnel, String> matriculeCol = new TableColumn<>("Matricule");
        matriculeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMatricule()));
        matriculeCol.setPrefWidth(100);
        
        TableColumn<Personnel, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNom()));
        nomCol.setPrefWidth(150);
        
        TableColumn<Personnel, String> prenomCol = new TableColumn<>("Prénom");
        prenomCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPrenom()));
        prenomCol.setPrefWidth(150);
        
        TableColumn<Personnel, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGrade()));
        gradeCol.setPrefWidth(120);
        
        // Permettre le tri sur toutes les colonnes
        matriculeCol.setSortable(true);
        nomCol.setSortable(true);
        prenomCol.setSortable(true);
        gradeCol.setSortable(true);
        
        TableColumn<Personnel, String> formationCol = new TableColumn<>("Formation");
        formationCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormation()));
        formationCol.setPrefWidth(150);
        formationCol.setSortable(true);
        
        // Ajout des colonnes à la table
        resultTable.getColumns().addAll(matriculeCol, nomCol, prenomCol, gradeCol, formationCol);
        
        // Configurer la sélection de ligne
        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                afficherDetailsPersonnel(newSelection);
            }
        });
    }
    
    private void chargerGrades() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            // Récupérer les grades distincts depuis la table grade_actuel
            String sql = "SELECT DISTINCT rang FROM grade_actuel ORDER BY rang";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            List<String> grades = new ArrayList<>();
            grades.add("Tous"); // Option par défaut
            
            while (rs.next()) {
                String grade = rs.getString("rang");
                if (grade != null && !grade.trim().isEmpty()) {
                    grades.add(grade);
                }
            }
            
            gradeFilter.setItems(FXCollections.observableArrayList(grades));
            
        } catch (SQLException e) {
            showError("Erreur lors du chargement des grades", e.getMessage());
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    private void chargerFormations() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            // Récupérer les formations distinctes depuis la table formation_actuelle
            String sql = "SELECT DISTINCT formation FROM formation_actuelle ORDER BY formation";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            List<String> formations = new ArrayList<>();
            formations.add("Toutes"); // Option par défaut
            
            while (rs.next()) {
                String formation = rs.getString("formation");
                if (formation != null && !formation.trim().isEmpty()) {
                    formations.add(formation);
                }
            }
            
            formationFilter.setItems(FXCollections.observableArrayList(formations));
            
        } catch (SQLException e) {
            showError("Erreur lors du chargement des formations", e.getMessage());
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    @FXML
    private void rechercherPersonnel() {
        String critereRecherche = searchField.getText().trim().toLowerCase();
        String gradeSelectionne = gradeFilter.getValue();
        String formationSelectionnee = formationFilter.getValue();
        String nomFiltre = nameFilter.getText().trim().toLowerCase();
        
        // Vider les résultats actuels
        personnelData.clear();
        
        // Effectuer la recherche dans la base de données
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            // Construire la requête SQL avec les filtres
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT ip.matricule, ip.nom, ip.prenom, ga.rang as grade, fa.formation ");
            sqlBuilder.append("FROM identite_personnelle ip ");
            sqlBuilder.append("LEFT JOIN grade_actuel ga ON ip.matricule = ga.matricule ");
            sqlBuilder.append("LEFT JOIN formation_actuelle fa ON ip.matricule = fa.matricule ");
            sqlBuilder.append("WHERE 1=1 ");
            
            List<Object> params = new ArrayList<>();
            
            // Ajouter le filtre de recherche générale
            if (!critereRecherche.isEmpty()) {
                sqlBuilder.append("AND (LOWER(ip.nom) LIKE ? OR LOWER(ip.prenom) LIKE ? OR LOWER(ip.matricule) LIKE ?) ");
                String searchPattern = "%" + critereRecherche + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }
            
            // Ajouter le filtre de grade
            if (gradeSelectionne != null && !gradeSelectionne.equals("Tous")) {
                sqlBuilder.append("AND ga.rang = ? ");
                params.add(gradeSelectionne);
            }
            
            // Ajouter le filtre de formation
            if (formationSelectionnee != null && !formationSelectionnee.equals("Toutes")) {
                sqlBuilder.append("AND fa.formation = ? ");
                params.add(formationSelectionnee);
            }
            
            // Ajouter le filtre de nom
            if (!nomFiltre.isEmpty()) {
                sqlBuilder.append("AND LOWER(ip.nom) LIKE ? ");
                params.add("%" + nomFiltre + "%");
            }
            
            // Exécuter la requête
            stmt = conn.prepareStatement(sqlBuilder.toString());
            
            // Configurer les paramètres
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            rs = stmt.executeQuery();
            
            // Traiter les résultats
            while (rs.next()) {
                String matricule = rs.getString("matricule");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String grade = rs.getString("grade");
                String formation = rs.getString("formation");
                
                // Gérer les valeurs nulles
                if (grade == null) grade = "Non spécifié";
                if (formation == null) formation = "Non spécifié";
                
                personnelData.add(new Personnel(matricule, nom, prenom, grade, formation));
            }
            
            // Mettre à jour la table des résultats
            resultTable.setItems(personnelData);
            
        } catch (SQLException e) {
            showError("Erreur lors de la recherche", e.getMessage());
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    private void afficherDetailsPersonnel(Personnel personnel) {
        // Cette méthode sera implémentée pour afficher les détails d'un personnel sélectionné
        // Pour l'instant, on se contente d'afficher une alerte
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du personnel");
        alert.setHeaderText("Information sur " + personnel.getNom() + " " + personnel.getPrenom());
        alert.setContentText("Matricule: " + personnel.getMatricule() + 
                            "\nGrade: " + personnel.getGrade() + 
                            "\nFormation: " + personnel.getFormation());
        alert.showAndWait();
    }
    
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
    
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Méthode pour lier à l'action de la touche Entrée sur le champ de recherche
    @FXML
    private void onEnterPressed(ActionEvent event) {
        rechercherPersonnel();
    }
    
    // Classe interne pour représenter les données du personnel
    public static class Personnel {
        private final String matricule;
        private final String nom;
        private final String prenom;
        private final String grade;
        private final String formation;
        
        public Personnel(String matricule, String nom, String prenom, String grade, String formation) {
            this.matricule = matricule;
            this.nom = nom;
            this.prenom = prenom;
            this.grade = grade;
            this.formation = formation;
        }
        
        public String getMatricule() { return matricule; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getGrade() { return grade; }
        public String getFormation() { return formation; }
    }
}