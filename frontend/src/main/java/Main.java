import Controller.DialogHelper;
import Controller.LoginFrameListener;
import Controller.SessionContext;
import Network.ConnectionSettings;
import Network.NotificationListener;
import Network.ServiceProxy;
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

            ConnectionSettings settings;
            ServiceProxy proxy;
            try {
                settings = ConnectionSettings.resolve(args);
                proxy = new ServiceProxy(settings.host(), settings.port());
            } catch (IOException | IllegalArgumentException e) {
                DialogHelper.error(null, "No fue posible conectar con el servidor:\n" + e.getMessage()
                        + "\n\nVerifique que el Backend este en ejecucion y que el host y el puerto sean correctos.");
                System.exit(1);
                return;
            }

            SessionContext session = new SessionContext(proxy);

            // El hilo lector NO es el EDT: cada alerta pasa a la interfaz con invokeLater.
            try {
                new NotificationListener(settings.host(), settings.port(), proxy.getSessionId(),
                        notification -> SwingUtilities.invokeLater(
                                () -> session.getAlertCenter().add(notification)));
            } catch (IOException e) {
                DialogHelper.warn(null, "No se pudo abrir el canal de notificaciones: " + e.getMessage()
                        + "\nLa aplicacion funcionara, pero no recibira alertas de otros usuarios.");
            }

            LoginFrame loginFrame = new LoginFrame();
            new LoginFrameListener(loginFrame, session);
            loginFrame.setVisible(true);
        });
    }
}
