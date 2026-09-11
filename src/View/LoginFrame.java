package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Ventana de inicio de sesión del Sistema de Reservas.
 * <p>
 * Esta clase construye únicamente la interfaz gráfica, siguiendo la
 * distribución de campos y botones definida en el mockup del proyecto:
 * el campo "ID", el campo "Clave" y los botones "Ingresar" y
 * "Cancelar", usando los íconos reales de {@code /Resources/Icons}.
 * Todavía no incorpora la lógica de autenticación; esa conexión con el
 * controlador se agregará en una etapa posterior.
 */
public class LoginFrame extends JFrame {

    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;

    public LoginFrame() {
        super("Sistema de Reservas");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(28, 40, 24, 40));
        setContentPane(content);

        content.add(buildFieldsPanel(), BorderLayout.CENTER);
        content.add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField();
        idField.setPreferredSize(new java.awt.Dimension(280, 28));
        UITheme.styleField(idField);

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new java.awt.Dimension(280, 28));
        UITheme.styleField(passwordField);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(UITheme.createLabel("ID:"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 24, 0);
        panel.add(idField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(UITheme.createLabel("Clave:"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(passwordField, gbc);

        return panel;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        loginButton = UITheme.createStackedIconButton("Ingresar", IconLibrary.NEXT, 40);
        cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 40);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 24, 0, 24);

        gbc.gridx = 0;
        panel.add(loginButton, gbc);
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        return panel;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public String getEnteredId() {
        return idField.getText();
    }

    public char[] getEnteredPassword() {
        return passwordField.getPassword();
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
