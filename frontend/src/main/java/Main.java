import Controller.LoginFrameListener;
import Controller.SessionContext;
import Service.IService;
import Service.LocalService;
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

            // Mientras no exista la capa de sockets, el Frontend usa la
            // implementacion local y la aplicacion corre en un solo proceso,
            // igual que en el Proyecto 1.
            //
            // Cuando entre el Proxy, esta linea pasa a ser:
            //     IService service = new ServiceProxy(host, puerto);
            // y con eso el modulo frontend deja de depender del backend.

            IService service = LocalService.bootstrapFromXml();

            SessionContext session = new SessionContext(service);
            LoginFrame loginFrame = new LoginFrame();
            new LoginFrameListener(loginFrame, session);
            loginFrame.setVisible(true);
        });
    }
}
