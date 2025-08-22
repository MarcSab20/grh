package application;

import java.util.prefs.Preferences;

/**
 * Utilitaire pour gérer les informations de session utilisateur
 * à travers l'application.
 */
public class UserSessionUtil {
    
    private static final String PREF_USERNAME = "username";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_ROLE = "user_role";
    
    // Instance privée pour le singleton
    private static UserSessionUtil instance;
    
    // Préférences pour stocker les informations de session
    private Preferences prefs;
    
    /**
     * Constructeur privé
     */
    private UserSessionUtil() {
        prefs = Preferences.userNodeForPackage(UserSessionUtil.class);
    }
    
    /**
     * Obtient l'instance unique de la classe
     */
    public static synchronized UserSessionUtil getInstance() {
        if (instance == null) {
            instance = new UserSessionUtil();
        }
        return instance;
    }
    
    /**
     * Définit les informations de l'utilisateur connecté
     * 
     * @param username Nom d'utilisateur
     * @param userId Identifiant de l'utilisateur
     * @param role Rôle de l'utilisateur
     */
    public void setCurrentUser(String username, int userId, String role) {
        prefs.put(PREF_USERNAME, username);
        prefs.putInt(PREF_USER_ID, userId);
        prefs.put(PREF_USER_ROLE, role);
    }
    
    /**
     * Récupère le nom d'utilisateur actuel
     * 
     * @return Le nom d'utilisateur ou "utilisateur" si non défini
     */
    public String getCurrentUsername() {
        return prefs.get(PREF_USERNAME, "utilisateur");
    }
    
    /**
     * Récupère l'identifiant de l'utilisateur actuel
     * 
     * @return L'identifiant de l'utilisateur ou -1 si non défini
     */
    public int getCurrentUserId() {
        return prefs.getInt(PREF_USER_ID, -1);
    }
    
    /**
     * Récupère le rôle de l'utilisateur actuel
     * 
     * @return Le rôle de l'utilisateur ou "guest" si non défini
     */
    public String getCurrentUserRole() {
        return prefs.get(PREF_USER_ROLE, "guest");
    }
    
    /**
     * Vérifie si un utilisateur est connecté
     * 
     * @return true si un utilisateur est connecté, false sinon
     */
    public boolean isUserLoggedIn() {
        return prefs.getInt(PREF_USER_ID, -1) != -1;
    }
    
    /**
     * Déconnecte l'utilisateur actuel
     */
    public void logout() {
        prefs.remove(PREF_USERNAME);
        prefs.remove(PREF_USER_ID);
        prefs.remove(PREF_USER_ROLE);
    }
    
    /**
     * Vérifie si l'utilisateur actuel a un rôle spécifique
     * 
     * @param role Le rôle à vérifier
     * @return true si l'utilisateur a le rôle spécifié, false sinon
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole.equals(role);
    }
    
    /**
     * Vérifie si l'utilisateur actuel est un administrateur
     * 
     * @return true si l'utilisateur est un administrateur, false sinon
     */
    public boolean isAdmin() {
        return hasRole("admin");
    }
}