import Controller.LoginFrameListener;
import Controller.SessionContext;
import View.LoginFrame;
import View.UITheme;

import javax.swing.SwingUtilities;

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
