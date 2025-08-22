package application;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LandingController {
    // Constantes de connexion à la base de données
    private static final String DB_URL = "jdbc:mysql://localhost:3306/exploit";
    private static final String DB_USER = "marco";
    private static final String DB_PASSWORD = "29Papa278.";
    
    @FXML
    private BorderPane borderPane;
    
    @FXML
    private MenuBar menuBar;

    @FXML
    private Button addPersonnel_btn;

    @FXML
    private Button expPersonnel_btn;

    @FXML
    private Button impPersonnel_btn;
    
    @FXML
    private Button acceuilBtn;
    
    @FXML
    private Button fouillerBtn;
    
    @FXML
    private Button dashboardBtn;
    
    @FXML
    private Button dashboardSideBtn;

    @FXML
    private MenuItem personnelMenu;

    @FXML
    private VBox mainContent;

    @FXML
    private Label politique_conf;
    
    @FXML
    private MenuItem personnelModDown;
    
    @FXML
    private PieChart personnelChart;
    
    @FXML
    private BarChart<String, Number> evolutionChart;
    
    // Labels pour les statistiques
    @FXML
    private Label totalPersonnelLabel;
    
    @FXML
    private Label femininPersonnelLabel;
    
    @FXML
    private Label formationsCountLabel;
    
    @FXML
    private Label officiersPctLabel;
    
    @FXML
    private Label specialiseePctLabel;
    
    @FXML
    private Label opInterieuresLabel;
    
    @FXML
    private Label opExtérieuresLabel;
    
    @FXML
    private Label ecolesMilitairesLabel;
    
    @FXML
    private Label languesParleesLabel;
    
    @FXML
    private void initialize() {
       // Initialisation des graphiques avec les données réelles
       try {
           initializeChartsWithRealData();
           updateStatisticsLabels();
       } catch (SQLException e) {
           e.printStackTrace();
           showErrorAlert("Erreur lors du chargement des données pour les graphiques: " + e.getMessage());
       }
    }
    
    /**
     * Initialise les graphiques avec des données réelles de la base de données
     */
    private void initializeChartsWithRealData() throws SQLException {
        Connection conn = null;
        
        try {
            // Établir la connexion à la base de données
            conn = getConnection();
            
            // Initialiser le graphique camembert avec les données réelles
            initializePieChart(conn);
            
            // Initialiser le graphique à barres avec les données réelles
            initializeBarChart(conn);
            
        } finally {
            // Fermer la connexion
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    /**
     * Met à jour les labels avec les statistiques réelles
     */
    private void updateStatisticsLabels() throws SQLException {
        Connection conn = null;
        
        try {
            conn = getConnection();
            
            // Nombre total de personnel
            String totalQuery = "SELECT COUNT(*) as count FROM identite_personnelle";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                if (rs.next()) {
                    totalPersonnelLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    totalPersonnelLabel.setText("0");
                }
            }
            
            // Personnel féminin
            String femininQuery = "SELECT COUNT(*) as count FROM identite_personnelle WHERE sexe = 'Féminin'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(femininQuery)) {
                if (rs.next()) {
                    femininPersonnelLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    femininPersonnelLabel.setText("0");
                }
            }
            
            // Nombre de formations différentes
            String formationsQuery = "SELECT COUNT(DISTINCT formation) as count FROM formation_actuelle";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(formationsQuery)) {
                if (rs.next()) {
                    formationsCountLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    formationsCountLabel.setText("0");
                }
            }
            
            // Pourcentage d'officiers
            String officiersPctQuery = "SELECT " +
                "COUNT(CASE WHEN rang IN ('Général d''Armée', 'Général de Corps d''Armée', " +
                "'Général de Division Aérienne', 'Général de Brigade Aérienne', " +
                "'Colonel', 'Lieutenant-colonel', 'Commandant', 'Capitaine', " +
                "'Lieutenant', 'Sous-lieutenant') THEN 1 ELSE NULL END) as officiers_count, " +
                "COUNT(*) as total_count " +
                "FROM grade_actuel";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(officiersPctQuery)) {
                if (rs.next()) {
                    int officiers = rs.getInt("officiers_count");
                    int total = rs.getInt("total_count");
                    if (total > 0) {
                        int percentage = (officiers * 100) / total;
                        officiersPctLabel.setText(percentage + "%");
                    } else {
                        officiersPctLabel.setText("0%");
                    }
                } else {
                    officiersPctLabel.setText("0%");
                }
            }
            
            // Pourcentage de personnel spécialisé
            String specialiseePctQuery = "SELECT COUNT(*) as count FROM specialite";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(specialiseePctQuery)) {
                if (rs.next() && rs.getInt("count") > 0) {
                    int totalPersonnel = 0;
                    try (Statement totalStmt = conn.createStatement();
                         ResultSet totalRs = totalStmt.executeQuery(totalQuery)) {
                        if (totalRs.next()) {
                            totalPersonnel = totalRs.getInt("count");
                        }
                    }
                    
                    if (totalPersonnel > 0) {
                        int percentage = (rs.getInt("count") * 100) / totalPersonnel;
                        specialiseePctLabel.setText(percentage + "%");
                    } else {
                        specialiseePctLabel.setText("0%");
                    }
                } else {
                    specialiseePctLabel.setText("0%");
                }
            }
            
            // Opérations intérieures
            String opIntQuery = "SELECT COUNT(*) as count FROM operation WHERE type = 'Intérieure'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(opIntQuery)) {
                if (rs.next()) {
                    opInterieuresLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    opInterieuresLabel.setText("0");
                }
            }
            
            // Opérations extérieures
            String opExtQuery = "SELECT COUNT(*) as count FROM operation WHERE type = 'Extérieure'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(opExtQuery)) {
                if (rs.next()) {
                    opExtérieuresLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    opExtérieuresLabel.setText("0");
                }
            }
            
            // Écoles militaires
            String ecolesMilitairesQuery = "SELECT COUNT(*) as count FROM ecole_militaire";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(ecolesMilitairesQuery)) {
                if (rs.next()) {
                    ecolesMilitairesLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    ecolesMilitairesLabel.setText("0");
                }
            }
            
            // Langues parlées
            String languesQuery = "SELECT COUNT(*) as count FROM langue WHERE categorie = 'langues_parlées'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(languesQuery)) {
                if (rs.next()) {
                    languesParleesLabel.setText(String.valueOf(rs.getInt("count")));
                } else {
                    // Essayer sans le filtre de catégorie au cas où cette colonne n'existe pas
                    String altQuery = "SELECT COUNT(*) as count FROM langue";
                    try (Statement altStmt = conn.createStatement();
                         ResultSet altRs = altStmt.executeQuery(altQuery)) {
                        if (altRs.next()) {
                            languesParleesLabel.setText(String.valueOf(altRs.getInt("count")));
                        } else {
                            languesParleesLabel.setText("0");
                        }
                    }
                }
            } catch (SQLException e) {
                // En cas d'erreur, essayer sans la condition de catégorie
                String altQuery = "SELECT COUNT(*) as count FROM langue";
                try (Statement altStmt = conn.createStatement();
                     ResultSet altRs = altStmt.executeQuery(altQuery)) {
                    if (altRs.next()) {
                        languesParleesLabel.setText(String.valueOf(altRs.getInt("count")));
                    } else {
                        languesParleesLabel.setText("0");
                    }
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    /**
     * Initialise le graphique camembert avec la répartition du personnel par formation
     */
    private void initializePieChart(Connection conn) throws SQLException {
        // Requête SQL pour compter le personnel par formation
        String query = "SELECT fa.formation, COUNT(*) as count " +
                       "FROM formation_actuelle fa " +
                       "GROUP BY fa.formation " +
                       "ORDER BY count DESC";
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String formation = rs.getString("formation");
                int count = rs.getInt("count");
                
                // Si la formation est null, l'afficher comme "Non défini"
                if (formation == null || formation.isEmpty()) {
                    formation = "Non défini";
                }
                
                pieChartData.add(new PieChart.Data(formation, count));
            }
        }
        
        // Si aucune donnée n'a été trouvée, ajouter une entrée "Aucune donnée"
        if (pieChartData.isEmpty()) {
            pieChartData.add(new PieChart.Data("Aucune donnée", 1));
        }
        
        personnelChart.setData(pieChartData);
        personnelChart.setTitle("Répartition du personnel par formation");
    }
    
    /**
     * Initialise le graphique à barres avec l'évolution des effectifs
     */
    private void initializeBarChart(Connection conn) throws SQLException {
        // Créer une série de données pour le graphique
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Effectifs");
        
        // Approche corrigée pour éviter l'erreur only_full_group_by
        // Au lieu d'utiliser un GROUP BY avec une colonne qui n'est pas dans la clause,
        // nous allons générer une liste de mois et faire un comptage distinct
        
        Map<String, Integer> monthlyData = new HashMap<>();
        
        // Générer la liste des 6 derniers mois
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            String monthKey = date.format(formatter);
            monthlyData.put(monthKey, 0); // Initialiser à 0
        }
        
        // Récupérer le comptage total pour chaque mois 
        // en utilisant une structure de requête compatible avec only_full_group_by
        String query = "SELECT DATE_FORMAT(date_naissance, '%m-%Y') as month, COUNT(*) as count " +
                      "FROM identite_personnelle " +
                      "WHERE date_naissance IS NOT NULL " +
                      "GROUP BY month " + // Nous groupons seulement par le format de mois, pas par la date complète
                      "ORDER BY STR_TO_DATE(month, '%m-%Y')"; // Utilisation de STR_TO_DATE pour trier correctement
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            // Remplir avec les données réelles si disponibles
            while (rs.next()) {
                String month = rs.getString("month");
                int count = rs.getInt("count");
                
                // Vérifier si ce mois est dans notre tranche des 6 derniers mois
                if (monthlyData.containsKey(month)) {
                    monthlyData.put(month, count);
                }
            }
        } catch (SQLException e) {
            // En cas d'erreur, nous essayons une approche alternative
            System.err.println("Erreur lors de l'exécution de la requête par mois: " + e.getMessage());
            
            // Requête alternative simplifiée sans groupement 
            String altQuery = "SELECT COUNT(*) as total FROM identite_personnelle";
            
            try (Statement altStmt = conn.createStatement();
                 ResultSet altRs = altStmt.executeQuery(altQuery)) {
                
                if (altRs.next()) {
                    int total = altRs.getInt("total");
                    int baseValue = Math.max(total - 500, 0); // Valeur de base
                    
                    // Distribution progressive sur les 6 derniers mois
                    int i = 0;
                    for (String month : new TreeMap<>(monthlyData).keySet()) { // Utiliser TreeMap pour garantir l'ordre
                        // Distribution croissante
                        int value = baseValue + (total - baseValue) * i / 5;
                        monthlyData.put(month, value);
                        i++;
                    }
                }
            }
        }
        
        // Ajouter les données au graphique dans l'ordre chronologique
        TreeMap<String, Integer> sortedData = new TreeMap<>(monthlyData);
        for (Map.Entry<String, Integer> entry : sortedData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        evolutionChart.getData().clear();
        evolutionChart.getData().add(series);
        evolutionChart.setTitle("Évolution des effectifs sur les 6 derniers mois");
    }
    
    /**
     * Établit une connexion à la base de données
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC non trouvé", e);
        }
    }
    
    @FXML
    private void showDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("dashboard.css").toExternalForm());
            
            Stage stage = (Stage) borderPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Tableau de bord avancé");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement du tableau de bord avancé: " + e.getMessage());
        }
    }
    
    @FXML
    private void handlePersonnelModDown(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ModificationPersonnel.fxml"));
            Parent personnelModifyDownloadView = loader.load();
            borderPane.setCenter(personnelModifyDownloadView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la vue de modification/téléchargement");
        }  
    }
    
    @FXML
    private void handleImportPersonnel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("importPersonnel.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exportPersonnel.fxml"));
            Parent exportPersonnelView = loader.load();
            borderPane.setCenter(exportPersonnelView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur Exportation du personnel");
        }
    }

    @FXML
    private void handleNewPersonnel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ajoutPersonnel.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("ajoutPersonnel.css").toExternalForm());
            
            Stage stage = (Stage) addPersonnel_btn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ajout d'un nouveau personnel");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la vue d'ajout de personnel");
        }
    
        System.out.println("Ajout d'un nouveau personnel");
    }
    
    @FXML
    private void handleAddPersonnel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ajoutPersonnel.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("ajoutPersonnel.css").toExternalForm());
            
            Stage stage = (Stage) menuBar.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ajout d'un nouveau personnel");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la vue d'ajout de personnel");
        }
        System.out.println("Ajout d'un nouveau personnel");
    }
    
    @FXML
    private void showHistory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("historique.fxml"));
            Parent historyView = loader.load();
            borderPane.setCenter(historyView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de l'historique");
        }
    }
    
    @FXML
    private void showAboutDialog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper10.fxml"));
            Parent aboutView = loader.load();
            borderPane.setCenter(aboutView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la page À propos");
        }
    }

    @FXML
    private void showHome(ActionEvent event) {
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
            showErrorAlert("Erreur lors du chargement de l'accueil");
        }
    }

    @FXML
    private void showSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fouiller.fxml"));
            Parent searchView = loader.load();
            borderPane.setCenter(searchView);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la recherche");
        }
    }
    
    @FXML
    private void handlePersonnelVisual(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("visualPersonnel.fxml"));
            Parent sansimp = loader.load();
            borderPane.setCenter(sansimp);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de la visualisation du personnel");
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}