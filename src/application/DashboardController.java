package application;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DashboardController {
    
    @FXML private BorderPane borderPane;
    @FXML private PieChart personnelByFormationChart;
    @FXML private PieChart personnelByGradeChart;
    @FXML private BarChart<String, Number> evolutionChart;
    @FXML private GridPane statsGrid;
    @FXML private TableView<Personnel> recentPersonnelTable;
    @FXML private TableColumn<Personnel, String> matriculeColumn;
    @FXML private TableColumn<Personnel, String> nomColumn;
    @FXML private TableColumn<Personnel, String> prenomColumn;
    @FXML private TableColumn<Personnel, String> formationColumn;
    @FXML private TableColumn<Personnel, String> gradeColumn;
    @FXML private Button refreshButton;
    
    /**
     * Classe pour représenter un personnel dans le tableau
     */
    public static class Personnel {
        private String matricule;
        private String nom;
        private String prenom;
        private String formation;
        private String grade;
        
        public Personnel(String matricule, String nom, String prenom, String formation, String grade) {
            this.matricule = matricule;
            this.nom = nom;
            this.prenom = prenom;
            this.formation = formation;
            this.grade = grade;
        }
        
        // Getters
        public String getMatricule() { return matricule; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getFormation() { return formation; }
        public String getGrade() { return grade; }
    }
    
    @FXML
    private void initialize() {
        // Configurer les colonnes du tableau
        matriculeColumn.setCellValueFactory(new PropertyValueFactory<>("matricule"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        formationColumn.setCellValueFactory(new PropertyValueFactory<>("formation"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        
        // Initialiser les graphiques et statistiques
        refreshData();
        
        // Configurer le bouton de rafraîchissement
        refreshButton.setOnAction(e -> refreshData());
    }
    
    /**
     * Rafraîchit toutes les données du tableau de bord
     */
    private void refreshData() {
        try {
            // Charger les graphiques
            loadPersonnelByFormationChart();
            loadPersonnelByGradeChart();
            loadEvolutionChart();
            
            // Charger les statistiques
            loadStatisticsGrid();
            
            // Charger le tableau des derniers personnels
            loadRecentPersonnelTable();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des données", e.getMessage());
        }
    }
    
    /**
     * Charge le graphique de répartition du personnel par formation
     */
    private void loadPersonnelByFormationChart() throws SQLException {
        ObservableList<PieChart.Data> pieChartData = StatisticsUtils.getPersonnelByFormation();
        personnelByFormationChart.setData(pieChartData);
        personnelByFormationChart.setTitle("Répartition par formation");
    }
    
    /**
     * Charge le graphique de répartition du personnel par grade
     */
    private void loadPersonnelByGradeChart() throws SQLException {
        ObservableList<PieChart.Data> pieChartData = StatisticsUtils.getPersonnelByGrade();
        personnelByGradeChart.setData(pieChartData);
        personnelByGradeChart.setTitle("Répartition par grade");
    }
    
    /**
     * Charge le graphique d'évolution des effectifs
     */
    private void loadEvolutionChart() throws SQLException {
        XYChart.Series<String, Number> series = StatisticsUtils.getEffectifsEvolution(6);
        evolutionChart.getData().clear();
        evolutionChart.getData().add(series);
        evolutionChart.setTitle("Évolution des effectifs");
    }
    
    /**
     * Charge la grille de statistiques
     */
    private void loadStatisticsGrid() throws SQLException {
        // Vider la grille
        statsGrid.getChildren().clear();
        
        // Récupérer les différentes statistiques
        Map<String, Integer> personnelStats = StatisticsUtils.getPersonnelDetailStats();
        Map<String, Integer> operationsStats = StatisticsUtils.getOperationsStats();
        Map<String, Integer> educationStats = StatisticsUtils.getEducationStats();
        Map<String, Integer> dotationStats = StatisticsUtils.getDotationStats();
        
        // Ajouter les statistiques du personnel
        addStatCard(statsGrid, 0, 0, "Personnel Total", 
                    String.valueOf(personnelStats.getOrDefault("total_personnel", 0)),
                    "Effectif total enregistré");
        
        addStatCard(statsGrid, 1, 0, "Personnel avec Langues", 
                    String.valueOf(personnelStats.getOrDefault("personnel_avec_langues", 0)),
                    "Maîtrisant au moins une langue");
        
        // Ajouter les statistiques des opérations
        addStatCard(statsGrid, 0, 1, "Opérations", 
                    String.valueOf(operationsStats.getOrDefault("total", 0)),
                    "Nombre total d'opérations");
        
        addStatCard(statsGrid, 1, 1, "Opérations Intérieures", 
                    String.valueOf(operationsStats.getOrDefault("interieures", 0)),
                    "Opérations nationales");
        
        // Ajouter les statistiques de formation
        addStatCard(statsGrid, 0, 2, "Écoles", 
                    String.valueOf(educationStats.getOrDefault("ecoles_civiles", 0) + 
                                  educationStats.getOrDefault("ecoles_militaires", 0)),
                    "Écoles civiles et militaires");
        
        addStatCard(statsGrid, 1, 2, "Formation Initiale", 
                    String.valueOf(educationStats.getOrDefault("formation_initiale", 0)),
                    "Personnel avec formation initiale");
        
        // Ajouter les statistiques de dotation
        addStatCard(statsGrid, 0, 3, "Dotations 20 Mai", 
                    String.valueOf(dotationStats.getOrDefault("dotations_20_mai", 0)),
                    "Nombre total de dotations");
        
        addStatCard(statsGrid, 1, 3, "Dotations Particulières", 
                    String.valueOf(dotationStats.getOrDefault("dotations_particulieres", 0)),
                    "Dotations spéciales");
    }
    
    /**
     * Ajoute une carte de statistique à la grille
     */
    private void addStatCard(GridPane grid, int col, int row, String title, String value, String description) {
        VBox card = new VBox(5);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("stat-description");
        
        card.getChildren().addAll(titleLabel, valueLabel, descLabel);
        
        grid.add(card, col, row);
    }
    
    /**
     * Charge le tableau des derniers personnels ajoutés
     */
    private void loadRecentPersonnelTable() throws SQLException {
        List<Map<String, Object>> recentPersonnelData = StatisticsUtils.getRecentPersonnel(10);
        
        ObservableList<Personnel> tableData = FXCollections.observableArrayList();
        
        for (Map<String, Object> personnelInfo : recentPersonnelData) {
            String matricule = (String) personnelInfo.get("matricule");
            String nom = (String) personnelInfo.get("nom");
            String prenom = (String) personnelInfo.get("prenom");
            String formation = (String) personnelInfo.get("formation");
            String grade = (String) personnelInfo.get("grade");
            
            tableData.add(new Personnel(matricule, nom, prenom, formation, grade));
        }
        
        recentPersonnelTable.setItems(tableData);
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gère le clic sur le bouton de retour à l'accueil
     */
    @FXML
    private void handleBackHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
            
            Stage stage = (Stage) borderPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accueil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de navigation", "Impossible de charger l'écran d'accueil");
        }
    }
}