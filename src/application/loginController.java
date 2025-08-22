package application;

import javafx.fxml.Initializable;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class loginController implements Initializable {

	@FXML
    private Button connect_btn;

    @FXML
    private ComboBox<String> formation_combo;

    @FXML
    private TextField identifiant;

    @FXML
    private PasswordField password;

    @FXML
    private ComboBox<String> unite_combo;
    
    private Connection connect;
    private PreparedStatement prepare;
    private ResultSet result;
    
    public void loginAccount() {
    	
    	String sql = "Select identifiant, password, formation, unite FROM users WHERE identifiant = ? and password = ? and formation = ? and unite = ?";
    	
    	connect = database.connect();
    	
    	try {
    		Alert alert;
    		if (identifiant.getText().isEmpty() || password.getText().isEmpty() || formation_combo.getValue().toString() == null || unite_combo.getValue().toString() == null) {
    			alert = new Alert(AlertType.ERROR);
    			alert.setTitle("Error Message");
    			alert.setHeaderText(null);
    			alert.setContentText("S'il vous plaît remplissez tous les champs");
    			alert.showAndWait();
    		}else {
    			
    			prepare = connect.prepareStatement(sql);
        		prepare.setString(1, identifiant.getText());
        		prepare.setString(2, password.getText());
        		prepare.setString(3, formation_combo.getValue().toString());
        		prepare.setString(4, unite_combo.getValue().toString());
        		
        		result = prepare.executeQuery();
        		
        		if (result.next()) {
        			// si les entrées matchent
//        			alert = new Alert(AlertType.INFORMATION);
//        			alert.setTitle("Information Message");
//        			alert.setHeaderText(null);
//        			alert.setContentText("Connexion réussie");
//        			alert.showAndWait();
        			
        			FXMLLoader loader = new FXMLLoader(getClass().getResource("exper2.fxml"));
        		    Parent root = loader.load();
        		    Scene scene = new Scene(root, 1024, 768);
        		    scene.getStylesheets().add(getClass().getResource("loginPage.css").toExternalForm());
        		    
        		    Stage stage = (Stage) connect_btn.getScene().getWindow(); // Supposons que loginButton est un élément de votre page de login
        		    stage.setScene(scene);
        		    stage.setTitle("Application de Gestion Militaire");
        		    stage.show();
        		}else {
        			// sinon
        			alert = new Alert(AlertType.ERROR);
        			alert.setTitle("Error Message");
        			alert.setHeaderText(null);
        			alert.setContentText("Au moins une entrée est incorrecte, veuillez corriger");
        			alert.showAndWait();
        		}
    		}
   
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    }
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		formation_combo.getItems().addAll("BA 101", "BA 102", "BA 201", "BA 301", "BA 302", "BA 401", "BA 501", "ECMAA", "Compagnie EMAA");
		unite_combo.getItems().addAll("GMT", "GMX", "GMO", "BAFUSAIR", "ECMAA", "Compagnie EMAA");
	}
}
