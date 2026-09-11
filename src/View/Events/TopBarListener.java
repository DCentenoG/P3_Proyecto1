package View.Events;

import View.LoginFrame;
import View.MainFrame;
import View.PasswordChangeForm;
import View.TopBar;

/**
 * Maneja los eventos de {@link TopBar} dentro de {@link MainFrame}:
 * abrir {@link PasswordChangeForm} y cerrar sesión (con confirmación),
 * regresando a {@link LoginFrame}.
 */
final class TopBarListener {

    private final MainFrame mainFrame;
    private final SessionContext session;

    TopBarListener(MainFrame mainFrame, SessionContext session) {
        this.mainFrame = mainFrame;
        this.session = session;
        wire();
    }

    private void wire() {
        TopBar topBar = mainFrame.getTopBar();
        topBar.getChangePasswordButton().addActionListener(e -> onChangePassword());
        topBar.getLogoutButton().addActionListener(e -> onLogout());
    }

    private void onChangePassword() {
        PasswordChangeForm form = new PasswordChangeForm();
        new PasswordChangeFormListener(form, session.getCurrentUser());
        form.setLocationRelativeTo(mainFrame);
        form.setVisible(true);
    }

    private void onLogout() {
        if (!DialogHelper.confirm(mainFrame, "¿Desea cerrar sesión?")) {
            return;
        }
        session.setCurrentUser(null);
        mainFrame.dispose();

        LoginFrame loginFrame = new LoginFrame();
        new LoginFrameListener(loginFrame, session);
        loginFrame.setVisible(true);
    }
}
