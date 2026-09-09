import View.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Lanza la pantalla de login para poder revisar visualmente la GUI.
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
