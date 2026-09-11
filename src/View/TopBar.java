package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Barra superior de {@link MainFrame}, visible sin importar el tab que
 * se esté consultando. Muestra a la izquierda el ID y el rol del
 * usuario con el que se ingresó, y a la derecha las opciones para
 * cambiar la clave (abre {@link PasswordChangeForm}) y cerrar sesión.
 * <p>
 * Solo construye la interfaz gráfica y expone los botones; la acción de
 * cambiar clave / cerrar sesión se conecta desde el manejo de eventos.
 */
public class TopBar extends JPanel {

    private static final Font IDENTITY_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Color SEPARATOR_COLOR = new Color(148, 163, 184);

    private final JLabel identityLabel;
    private final JButton changePasswordButton;
    private final JButton logoutButton;

    public TopBar(String userId, String roleLabel) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));

        identityLabel = new JLabel();
        identityLabel.setFont(IDENTITY_FONT);
        identityLabel.setForeground(UITheme.TEXT_BLUE);
        add(identityLabel, BorderLayout.WEST);

        changePasswordButton = UITheme.createInlineIconButton("Cambiar clave", IconLibrary.PADLOCK, UITheme.ICON_SMALL);
        logoutButton = UITheme.createInlineIconButton("Salir", IconLibrary.LOGOUT, UITheme.ICON_SMALL);

        JLabel separator = new JLabel("•");
        separator.setFont(IDENTITY_FONT);
        separator.setForeground(SEPARATOR_COLOR);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(changePasswordButton);
        rightPanel.add(separator);
        rightPanel.add(logoutButton);
        add(rightPanel, BorderLayout.EAST);

        setIdentity(userId, roleLabel);
    }

    /** Actualiza el texto "ID - ROL" mostrado a la izquierda de la barra. */
    public final void setIdentity(String userId, String roleLabel) {
        identityLabel.setText(userId + " - " + roleLabel.toUpperCase());
    }

    public JButton getChangePasswordButton() {
        return changePasswordButton;
    }

    public JButton getLogoutButton() {
        return logoutButton;
    }
}
