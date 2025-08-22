package application;

import javafx.fxml.FXML;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class aboutController {

    @FXML
    private TextFlow aboutText;

    @FXML
    private void initialize() {
        Text text = new Text(
            "Notre application de gestion des ressources militaires est conçue pour optimiser "
            + "l'efficacité opérationnelle des forces armées modernes.\n\n"
            + "Objectifs principaux :\n"
            + "• Centraliser la gestion du personnel, des équipements et des véhicules\n"
            + "• Améliorer la planification et l'allocation des ressources\n"
            + "• Faciliter la prise de décision stratégique\n"
            + "• Assurer un suivi en temps réel des actifs militaires\n\n"
            + "Notre application s'adresse aux :\n"
            + "• Commandants et officiers supérieurs\n"
            + "• Gestionnaires de ressources militaires\n"
            + "• Personnels logistiques\n"
            + "• Analystes de données militaires\n\n"
            + "Avec notre solution, nous visons à renforcer la capacité opérationnelle "
            + "et la réactivité des forces armées face aux défis du 21e siècle."
        );

        text.setTextAlignment(TextAlignment.JUSTIFY);
        aboutText.getChildren().add(text);
    }
}