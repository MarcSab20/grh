package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

/**
 * Classe utilitaire pour générer des statistiques à partir de la base de données
 */
public class StatisticsUtils {
    // Constantes de connexion à la base de données
    private static final String DB_URL = "jdbc:mysql://localhost:3306/exploit";
    private static final String DB_USER = "marco";
    private static final String DB_PASSWORD = "29Papa278.";
    
    /**
     * Établit une connexion à la base de données
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC non trouvé", e);
        }
    }
    
    /**
     * Récupère les données pour un graphique camembert de répartition du personnel par formation
     */
    public static ObservableList<PieChart.Data> getPersonnelByFormation() throws SQLException {
        Connection conn = null;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        try {
            conn = getConnection();
            
            // Requête SQL pour compter le personnel par formation
            String query = "SELECT fa.formation, COUNT(*) as count " +
                           "FROM formation_actuelle fa " +
                           "GROUP BY fa.formation " +
                           "ORDER BY count DESC";
            
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
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return pieChartData;
    }
    
    /**
     * Récupère les données pour un graphique camembert de répartition du personnel par grade
     */
    public static ObservableList<PieChart.Data> getPersonnelByGrade() throws SQLException {
        Connection conn = null;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        try {
            conn = getConnection();
            
            // Requête SQL pour compter le personnel par grade
            String query = "SELECT ga.rang, COUNT(*) as count " +
                           "FROM grade_actuel ga " +
                           "GROUP BY ga.rang " +
                           "ORDER BY count DESC";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    String grade = rs.getString("rang");
                    int count = rs.getInt("count");
                    
                    // Si le grade est null, l'afficher comme "Non défini"
                    if (grade == null || grade.isEmpty()) {
                        grade = "Non défini";
                    }
                    
                    pieChartData.add(new PieChart.Data(grade, count));
                }
            }
            
            // Si aucune donnée n'a été trouvée, ajouter une entrée "Aucune donnée"
            if (pieChartData.isEmpty()) {
                pieChartData.add(new PieChart.Data("Aucune donnée", 1));
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return pieChartData;
    }
    
    /**
     * Récupère les données d'évolution des effectifs sur les derniers mois
     */
    public static XYChart.Series<String, Number> getEffectifsEvolution(int months) throws SQLException {
        Connection conn = null;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Effectifs");
        
        try {
            conn = getConnection();
            
            // On utilise une approche qui combine les données réelles et des estimations
            Map<String, Integer> monthlyData = new TreeMap<>(); // TreeMap pour garder l'ordre chronologique
            
            // Initialiser avec des 0 pour tous les mois demandés
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
            
            for (int i = months - 1; i >= 0; i--) {
                LocalDate date = now.minusMonths(i);
                String monthKey = date.format(formatter);
                monthlyData.put(monthKey, 0);
            }
            
            // Récupérer les données réelles si disponibles avec une requête compatible SQL_MODE=only_full_group_by
            String dateQuery = 
                "SELECT DATE_FORMAT(date_naissance, '%m-%Y') as month, COUNT(*) as count " +
                "FROM identite_personnelle " +
                "WHERE date_naissance IS NOT NULL " +
                "GROUP BY month " +
                "ORDER BY STR_TO_DATE(month, '%m-%Y')";
            
            try (Statement dateStmt = conn.createStatement();
                 ResultSet dateRs = dateStmt.executeQuery(dateQuery)) {
                
                while (dateRs.next()) {
                    String month = dateRs.getString("month");
                    int count = dateRs.getInt("count");
                    
                    // Ajouter seulement si le mois est dans notre liste (pour éviter les données trop anciennes)
                    if (monthlyData.containsKey(month)) {
                        monthlyData.put(month, count);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de la récupération des données par mois: " + e.getMessage());
                
                // Si trop peu de données, utiliser une approche alternative basée sur le total
                String totalQuery = "SELECT COUNT(*) as total FROM identite_personnelle";
                
                try (Statement totalStmt = conn.createStatement();
                     ResultSet totalRs = totalStmt.executeQuery(totalQuery)) {
                    
                    if (totalRs.next()) {
                        int total = totalRs.getInt("total");
                        
                        // Estimer une croissance progressive
                        int baseValue = Math.max(total - (months * 50), 0);
                        int step = (total - baseValue) / (months > 0 ? months : 1);
                        
                        int i = 0;
                        for (String month : monthlyData.keySet()) {
                            int estimatedValue = baseValue + (step * i);
                            monthlyData.put(month, estimatedValue);
                            i++;
                        }
                    }
                }
            }
            
            // Ajouter les données à la série
            for (Map.Entry<String, Integer> entry : monthlyData.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return series;
    }
    
    /**
     * Récupère la répartition du personnel par âge
     */
    public static Map<String, Integer> getPersonnelByAgeGroup() throws SQLException {
        Connection conn = null;
        Map<String, Integer> ageGroups = new HashMap<>();
        
        // Initialiser les tranches d'âge
        ageGroups.put("18-25", 0);
        ageGroups.put("26-35", 0);
        ageGroups.put("36-45", 0);
        ageGroups.put("46-55", 0);
        ageGroups.put("56+", 0);
        
        try {
            conn = getConnection();
            
            // Requête pour compter le personnel par tranche d'âge
            String query = 
                "SELECT " +
                "SUM(CASE WHEN TIMESTAMPDIFF(YEAR, date_naissance, CURDATE()) BETWEEN 18 AND 25 THEN 1 ELSE 0 END) as age_18_25, " +
                "SUM(CASE WHEN TIMESTAMPDIFF(YEAR, date_naissance, CURDATE()) BETWEEN 26 AND 35 THEN 1 ELSE 0 END) as age_26_35, " +
                "SUM(CASE WHEN TIMESTAMPDIFF(YEAR, date_naissance, CURDATE()) BETWEEN 36 AND 45 THEN 1 ELSE 0 END) as age_36_45, " +
                "SUM(CASE WHEN TIMESTAMPDIFF(YEAR, date_naissance, CURDATE()) BETWEEN 46 AND 55 THEN 1 ELSE 0 END) as age_46_55, " +
                "SUM(CASE WHEN TIMESTAMPDIFF(YEAR, date_naissance, CURDATE()) >= 56 THEN 1 ELSE 0 END) as age_56_plus " +
                "FROM identite_personnelle " +
                "WHERE date_naissance IS NOT NULL";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                if (rs.next()) {
                    ageGroups.put("18-25", rs.getInt("age_18_25"));
                    ageGroups.put("26-35", rs.getInt("age_26_35"));
                    ageGroups.put("36-45", rs.getInt("age_36_45"));
                    ageGroups.put("46-55", rs.getInt("age_46_55"));
                    ageGroups.put("56+", rs.getInt("age_56_plus"));
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return ageGroups;
    }
    
    /**
     * Récupère des statistiques sur les opérations
     */
    public static Map<String, Integer> getOperationsStats() throws SQLException {
        Connection conn = null;
        Map<String, Integer> operationsStats = new HashMap<>();
        
        try {
            conn = getConnection();
            
            // Nombre total d'opérations
            String totalQuery = "SELECT COUNT(*) as total FROM operation";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                
                if (rs.next()) {
                    operationsStats.put("total", rs.getInt("total"));
                }
            }
            
            // Nombre d'opérations intérieures
            String intQuery = "SELECT COUNT(*) as count FROM operation WHERE type = 'Intérieure'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(intQuery)) {
                
                if (rs.next()) {
                    operationsStats.put("interieures", rs.getInt("count"));
                }
            }
            
            // Nombre d'opérations extérieures
            String extQuery = "SELECT COUNT(*) as count FROM operation WHERE type = 'Extérieure'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(extQuery)) {
                
                if (rs.next()) {
                    operationsStats.put("exterieures", rs.getInt("count"));
                }
            }
            
            // Nombre d'opérations par année (pour les 5 dernières années)
            String yearQuery = "SELECT YEAR(date_debut) as year, COUNT(*) as count " +
                              "FROM operation " +
                              "WHERE date_debut IS NOT NULL " +
                              "GROUP BY YEAR(date_debut) " +
                              "ORDER BY year DESC " +
                              "LIMIT 5";
            
            // Note: Adaptez cette requête si le champ date est différent dans votre table
            
            int currentYear = LocalDate.now().getYear();
            for (int i = 0; i < 5; i++) {
                int year = currentYear - i;
                operationsStats.put("year_" + year, 0);
            }
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(yearQuery)) {
                
                while (rs.next()) {
                    int year = rs.getInt("year");
                    int count = rs.getInt("count");
                    operationsStats.put("year_" + year, count);
                }
            } catch (SQLException e) {
                // Si la requête échoue (peut-être que le champ date_debut n'existe pas), on ignore
                System.err.println("Erreur lors de la récupération des opérations par année: " + e.getMessage());
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return operationsStats;
    }
    
    /**
     * Récupère des statistiques sur les formations et écoles
     */
    public static Map<String, Integer> getEducationStats() throws SQLException {
        Connection conn = null;
        Map<String, Integer> educationStats = new HashMap<>();
        
        try {
            conn = getConnection();
            
            // Nombre d'écoles civiles
            String civilQuery = "SELECT COUNT(*) as count FROM ecole_civile";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(civilQuery)) {
                
                if (rs.next()) {
                    educationStats.put("ecoles_civiles", rs.getInt("count"));
                }
            }
            
            // Nombre d'écoles militaires
            String militaryQuery = "SELECT COUNT(*) as count FROM ecole_militaire";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(militaryQuery)) {
                
                if (rs.next()) {
                    educationStats.put("ecoles_militaires", rs.getInt("count"));
                }
            }
            
            // Nombre de personnes avec une formation initiale
            String initialQuery = "SELECT COUNT(*) as count FROM ecole_formation_initiale";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(initialQuery)) {
                
                if (rs.next()) {
                    educationStats.put("formation_initiale", rs.getInt("count"));
                }
            }
            
            // Types de formations les plus fréquentes
            String formationQuery = "SELECT formation, COUNT(*) as count " +
                                   "FROM formation_actuelle " +
                                   "GROUP BY formation " +
                                   "ORDER BY count DESC " +
                                   "LIMIT 5";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(formationQuery)) {
                
                int index = 1;
                while (rs.next()) {
                    String formation = rs.getString("formation");
                    int count = rs.getInt("count");
                    
                    if (formation != null && !formation.isEmpty()) {
                        educationStats.put("top_formation_" + index, count);
                        educationStats.put("top_formation_name_" + index, formation.hashCode()); // On utilise hashCode pour éviter les problèmes de clés
                        index++;
                    }
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return educationStats;
    }
    
    /**
     * Récupère des statistiques sur le personnel (décorations, langues, etc.)
     */
    public static Map<String, Integer> getPersonnelDetailStats() throws SQLException {
        Connection conn = null;
        Map<String, Integer> detailStats = new HashMap<>();
        
        try {
            conn = getConnection();
            
            // Nombre total de personnel
            String totalQuery = "SELECT COUNT(*) as count FROM identite_personnelle";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                
                if (rs.next()) {
                    detailStats.put("total_personnel", rs.getInt("count"));
                }
            }
            
            // Répartition par sexe
            String sexeQuery = "SELECT sexe, COUNT(*) as count " +
                              "FROM identite_personnelle " +
                              "GROUP BY sexe";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sexeQuery)) {
                
                while (rs.next()) {
                    String sexe = rs.getString("sexe");
                    int count = rs.getInt("count");
                    
                    if (sexe != null && !sexe.isEmpty()) {
                        detailStats.put("sexe_" + sexe.toLowerCase(), count);
                    } else {
                        detailStats.put("sexe_non_specifie", count);
                    }
                }
            }
            
            // Nombre de décorations
            String decorationQuery = "SELECT COUNT(*) as count FROM decoration";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(decorationQuery)) {
                
                if (rs.next()) {
                    detailStats.put("decorations", rs.getInt("count"));
                }
            }
            
            // Nombre de médailles
            String medailleQuery = "SELECT COUNT(*) as count FROM medaille";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(medailleQuery)) {
                
                if (rs.next()) {
                    detailStats.put("medailles", rs.getInt("count"));
                }
            }
            
            // Nombre de punitions
            String punitionQuery = "SELECT COUNT(*) as count FROM punition";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(punitionQuery)) {
                
                if (rs.next()) {
                    detailStats.put("punitions", rs.getInt("count"));
                }
            }
            
            // Nombre de langues parlées
            String langueQuery = "SELECT COUNT(DISTINCT matricule) as personnel_count, " +
                                "COUNT(*) as total_langues " +
                                "FROM langue " +
                                "WHERE categorie = 'langues_parlées'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(langueQuery)) {
                
                if (rs.next()) {
                    detailStats.put("personnel_avec_langues", rs.getInt("personnel_count"));
                    detailStats.put("total_langues_parlees", rs.getInt("total_langues"));
                }
            } catch (SQLException e) {
                // Si la structure de la table n'a pas de colonne 'categorie'
                String altQuery = "SELECT COUNT(DISTINCT matricule) as personnel_count, " +
                                 "COUNT(*) as total_langues " +
                                 "FROM langue";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(altQuery)) {
                    
                    if (rs.next()) {
                        detailStats.put("personnel_avec_langues", rs.getInt("personnel_count"));
                        detailStats.put("total_langues_parlees", rs.getInt("total_langues"));
                    }
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return detailStats;
    }
    
    /**
     * Récupère un tableau des derniers personnels ajoutés
     */
    public static List<Map<String, Object>> getRecentPersonnel(int limit) throws SQLException {
        Connection conn = null;
        List<Map<String, Object>> recentPersonnel = new ArrayList<>();
        
        try {
            conn = getConnection();
            
            // Requête pour récupérer les derniers personnels ajoutés
            // Note: Ceci suppose qu'il y a un ID auto-incrémenté ou une date d'ajout
            // Adaptez la requête selon votre structure de base de données
            
            String query = "SELECT ip.matricule, ip.nom, ip.prenom, fa.formation, ga.rang " +
                          "FROM identite_personnelle ip " +
                          "LEFT JOIN formation_actuelle fa ON ip.matricule = fa.matricule " +
                          "LEFT JOIN grade_actuel ga ON ip.matricule = ga.matricule " +
                          "ORDER BY ip.matricule DESC " + // Adapté pour utiliser le matricule comme critère de tri
                          "LIMIT ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, limit);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> personnelInfo = new HashMap<>();
                        personnelInfo.put("matricule", rs.getString("matricule"));
                        personnelInfo.put("nom", rs.getString("nom"));
                        personnelInfo.put("prenom", rs.getString("prenom"));
                        personnelInfo.put("formation", rs.getString("formation"));
                        personnelInfo.put("grade", rs.getString("rang"));
                        
                        recentPersonnel.add(personnelInfo);
                    }
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return recentPersonnel;
    }
    
    /**
     * Récupère des statistiques sur les dotations
     */
    public static Map<String, Integer> getDotationStats() throws SQLException {
        Connection conn = null;
        Map<String, Integer> dotationStats = new HashMap<>();
        
        try {
            conn = getConnection();
            
            // Nombre de dotations 20 Mai
            String dotation20MaiQuery = "SELECT COUNT(*) as count FROM dotation_20_mai";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(dotation20MaiQuery)) {
                
                if (rs.next()) {
                    dotationStats.put("dotations_20_mai", rs.getInt("count"));
                }
            }
            
            // Nombre de dotations particulières
            String dotationParticuliereQuery = "SELECT COUNT(*) as count FROM dotation_particuliere";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(dotationParticuliereQuery)) {
                
                if (rs.next()) {
                    dotationStats.put("dotations_particulieres", rs.getInt("count"));
                }
            }
            
            // Nombre de personnes avec des paramètres corporels enregistrés
            String parametresCorporelsQuery = "SELECT COUNT(*) as count FROM parametres_corporels";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(parametresCorporelsQuery)) {
                
                if (rs.next()) {
                    dotationStats.put("parametres_corporels", rs.getInt("count"));
                }
            }
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        
        return dotationStats;
    }
}