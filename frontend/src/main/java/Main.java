import Controller.DialogHelper;
import Controller.LoginFrameListener;
import Controller.SessionContext;
import Network.ConnectionSettings;
import Network.ServiceProxy;
import Service.IService;
import View.LoginFrame;
import View.UITheme;

import javax.swing.*;
import java.io.IOException;

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

            IService service;
            try {
                ConnectionSettings settings = ConnectionSettings.resolve(args);
                service = new ServiceProxy(settings.host(), settings.port());
            } catch (IOException | IllegalArgumentException e) {
                DialogHelper.error(null, "No fue posible conectar con el servidor:\n" + e.getMessage()
                        + "\n\nVerifique que el Backend este en ejecucion y que el host y el puerto sean correctos.");
                System.exit(1);
                return;
            }

            SessionContext session = new SessionContext(service);
            LoginFrame loginFrame = new LoginFrame();
            new LoginFrameListener(loginFrame, session);
            loginFrame.setVisible(true);
        });
    }
}
