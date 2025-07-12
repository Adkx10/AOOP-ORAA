package main.newmotorph;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import model.LogInPage;

public class NewMotorPH {

    public static void main(String[] args) {
        
        try{
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch(UnsupportedLookAndFeelException e){
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(LogInPage::new);
    }
}