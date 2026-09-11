import View.Events.LoginFrameListener;
import View.Events.SessionContext;
import View.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SessionContext session = new SessionContext();
            LoginFrame loginFrame = new LoginFrame();
            new LoginFrameListener(loginFrame, session);
            loginFrame.setVisible(true);
        });
    }
}
