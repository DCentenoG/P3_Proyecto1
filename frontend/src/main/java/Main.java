import Controller.LoginFrameListener;
import Controller.SessionContext;
import View.LoginFrame;
import View.UITheme;

import javax.swing.SwingUtilities;

/* claves para users de prueba:
* admin
*   User: 1
*   Password: 123admin (puede modificarse)
* funcionario
*   User: 2
*   Password: 2*/

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UITheme.installSwingTheme();
            SessionContext session = new SessionContext();
            LoginFrame loginFrame = new LoginFrame();
            new LoginFrameListener(loginFrame, session);
            loginFrame.setVisible(true);
        });
    }
}
