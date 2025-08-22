package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class historiqueController implements Initializable {
    
    @FXML private TableView<HistoryEntry> historyTable;
    @FXML private TableColumn<HistoryEntry, String> actionColumn;
    @FXML private TableColumn<HistoryEntry, String> cibleColumn;
    @FXML private TableColumn<HistoryEntry, String> detailsColumn;
    @FXML private TableColumn<HistoryEntry, String> dateTimeColumn;
    @FXML private TableColumn<HistoryEntry, String> utilisateurColumn;
    
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private ComboBox<String> actionFilterComboBox;
    @FXML private Button rechercheButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    
    // Constantes pour les noms des actions
    public static final String ACTION_AJOUT = "AJOUT";
    public static final String ACTION_MODIFICATION = "MODIFICATION";
    public static final String ACTION_EXPORTATION = "EXPORTATION";
    public static final String ACTION_IMPORTATION = "IMPORTATION";
    public static final String ACTION_SUPPRESSION = "SUPPRESSION";
    
    // Constantes pour les types de cibles
    public static final String CIBLE_PERSONNEL = "PERSONNEL";
    public static final String CIBLE_ENSEMBLE = "ENSEMBLE";
    
    // Constantes pour la connexion à la base de données
    private static final String DB_URL = "jdbc:mysql://localhost:3306/exploit";
    private static final String DB_USER = "marco";
    private static final String DB_PASSWORD = "29Papa278.";
    
    private ObservableList<HistoryEntry> historyData = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les colonnes de la table
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        cibleColumn.setCellValueFactory(new PropertyValueFactory<>("cible"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        utilisateurColumn.setCellValueFactory(new PropertyValueFactory<>("utilisateur"));
        
        // Initialiser les filtres
        initializeFilters();
        
        // Charger les données initiales
        loadHistoryData();
        
        // Configurer les actions des boutons
        rechercheButton.setOnAction(e -> handleSearch());
        resetButton.setOnAction(e -> handleReset());
        backButton.setOnAction(e -> handleBack());
    }
    
    /**
     * Initialise les filtres de recherche
     */
    private void initializeFilters() {
        // Initialiser les dates par défaut
        dateDebutPicker.setValue(LocalDate.now().minusMonths(1));
        dateFinPicker.setValue(LocalDate.now());
        
        // Remplir le ComboBox des actions
        actionFilterComboBox.getItems().addAll(
            "Toutes les actions",
            ACTION_AJOUT,
            ACTION_MODIFICATION,
            ACTION_EXPORTATION,
            ACTION_IMPORTATION,
            ACTION_SUPPRESSION
        );
        actionFilterComboBox.setValue("Toutes les actions");
    }
    
    /**
     * Charge les données de l'historique depuis la base de données
     */
    private void loadHistoryData() {
        historyData.clear();
        Connection connection = null;
        
        try {
            connection = getConnection();
            String sql = "SELECT * FROM historique ORDER BY date_action DESC LIMIT 500";
            
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    historyData.add(new HistoryEntry(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("cible"),
                        rs.getString("details"),
                        rs.getTimestamp("date_action").toLocalDateTime(),
                        rs.getString("utilisateur")
                    ));
                }
            }
            
            historyTable.setItems(historyData);
            
        } catch (SQLException e) {
            showError("Erreur de base de données", "Impossible de charger l'historique: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Gère l'action du bouton Recherche
     */
    @FXML
    private void handleSearch() {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();
        String actionFilter = actionFilterComboBox.getValue();
        
        // Vérifier la validité des dates
        if (dateDebut == null || dateFin == null) {
            showAlert(AlertType.WARNING, "Dates manquantes", "Veuillez sélectionner des dates de début et de fin.");
            return;
        }
        
        if (dateDebut.isAfter(dateFin)) {
            showAlert(AlertType.WARNING, "Dates invalides", "La date de début doit être antérieure à la date de fin.");
            return;
        }
        
        // Construire la requête SQL avec les filtres
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM historique WHERE 1=1");
        
        // Ajouter le filtre de date
        sqlBuilder.append(" AND date_action BETWEEN ? AND ?");
        
        // Ajouter le filtre d'action si nécessaire
        if (actionFilter != null && !actionFilter.equals("Toutes les actions")) {
            sqlBuilder.append(" AND action = ?");
        }
        
        // Ajouter l'ordre de tri
        sqlBuilder.append(" ORDER BY date_action DESC");
        
        Connection connection = null;
        try {
            connection = getConnection();
            
            try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
                // Définir les paramètres de la requête
                pstmt.setTimestamp(1, Timestamp.valueOf(dateDebut.atStartOfDay()));
                pstmt.setTimestamp(2, Timestamp.valueOf(dateFin.plusDays(1).atStartOfDay()));
                
                if (actionFilter != null && !actionFilter.equals("Toutes les actions")) {
                    pstmt.setString(3, actionFilter);
                }
                
                // Exécuter la requête
                ResultSet rs = pstmt.executeQuery();
                
                // Effacer les données actuelles
                historyData.clear();
                
                // Charger les nouvelles données
                while (rs.next()) {
                    historyData.add(new HistoryEntry(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("cible"),
                        rs.getString("details"),
                        rs.getTimestamp("date_action").toLocalDateTime(),
                        rs.getString("utilisateur")
                    ));
                }
                
                historyTable.setItems(historyData);
            }
        } catch (SQLException e) {
            showError("Erreur de recherche", "Impossible d'exécuter la recherche: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Gère l'action du bouton Reset
     */
    @FXML
    private void handleReset() {
        // Réinitialiser les filtres
        dateDebutPicker.setValue(LocalDate.now().minusMonths(1));
        dateFinPicker.setValue(LocalDate.now());
        actionFilterComboBox.setValue("Toutes les actions");
        
        // Recharger les données
        loadHistoryData();
    }
    
    /**
     * Gère l'action du bouton Retour
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("landing.css").toExternalForm());
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accueil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de navigation", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }
    
    /**
     * Méthode utilitaire pour enregistrer un événement dans l'historique
     * Cette méthode peut être appelée statiquement depuis d'autres contrôleurs
     * 
     * @param action Type d'action (AJOUT, MODIFICATION, etc.)
     * @param cible Type de cible (PERSONNEL, ENSEMBLE)
     * @param details Détails spécifiques à l'action (matricule, nom, etc.)
     * @param utilisateur Nom de l'utilisateur qui a effectué l'action
     * @return true si l'enregistrement a réussi, false sinon
     */
    public static boolean enregistrerAction(String action, String cible, String details, String utilisateur) {
        Connection connection = null;
        
        try {
            connection = getConnection();
            
            String sql = "INSERT INTO historique (action, cible, details, date_action, utilisateur) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, action);
                pstmt.setString(2, cible);
                pstmt.setString(3, details);
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(5, utilisateur);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
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
     * Utilitaire pour obtenir une connexion à la base de données
     */
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC non trouvé", e);
        }
    }
    
    /**
     * Ferme une connexion à la base de données
     */
    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void showError(String title, String message) {
        showAlert(AlertType.ERROR, title, message);
    }
    
    /**
     * Affiche une boîte de dialogue avec le type spécifié
     */
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Classe interne pour les entrées de l'historique
     */
    public static class HistoryEntry {
        private final int id;
        private final String action;
        private final String cible;
        private final String details;
        private final String dateTime;
        private final String utilisateur;
        
        public HistoryEntry(int id, String action, String cible, String details, LocalDateTime dateTime, String utilisateur) {
            this.id = id;
            this.action = action;
            this.cible = cible;
            this.details = details;
            this.dateTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.utilisateur = utilisateur;
        }
        
        public int getId() { return id; }
        public String getAction() { return action; }
        public String getCible() { return cible; }
        public String getDetails() { return details; }
        public String getDateTime() { return dateTime; }
        public String getUtilisateur() { return utilisateur; }
    }
}