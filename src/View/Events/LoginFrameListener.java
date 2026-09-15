package View.Events;

import Model.User;
import View.LoginFrame;
import View.MainFrame;

/**
 * Maneja los eventos de {@link LoginFrame}: valida que se hayan
 * completado el ID y la clave, autentica contra la {@link SessionContext}
 * y, de ser correctos, abre {@link MainFrame} (con su propio
 * {@link MainFrameListener}) para el usuario autenticado.
 */
public final class LoginFrameListener {

    private final LoginFrame view;
    private final SessionContext session;

    public LoginFrameListener(LoginFrame view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
    }

    private void wire() {
        view.getLoginButton().addActionListener(e -> onLogin());
        view.getCancelButton().addActionListener(e -> onCancel());
    }

    private void onLogin() {
        String idText = view.getEnteredId().trim();
        String password = new String(view.getEnteredPassword());

        if (idText.isEmpty() || password.isEmpty()) {
            DialogHelper.warn(view, "Debe ingresar el ID y la clave para continuar.");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            DialogHelper.warn(view, "El ID debe ser un valor numérico.");
            return;
        }

        User user = session.authenticate(id, password);
        if (user == null) {
            DialogHelper.error(view, "El ID o la clave ingresados son incorrectos.");
            return;
        }

        session.setCurrentUser(user);
        MainFrame mainFrame = new MainFrame(user);
        new MainFrameListener(mainFrame, session);
        view.dispose();
        mainFrame.setVisible(true);
    }

    private void onCancel() {
        if (DialogHelper.confirm(view, "¿Desea salir del sistema?")) {
            view.dispose();
            System.exit(0);
        }
    }
}
