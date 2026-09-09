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
        content.setBackground(FormStyle.BACKGROUND);
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
        FormStyle.styleField(idField);

        passwordField = new JPasswordField();
        FormStyle.styleField(passwordField);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(FormStyle.createFieldLabel("ID:"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 24, 0);
        panel.add(idField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(FormStyle.createFieldLabel("Clave:"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(passwordField, gbc);

        return panel;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        loginButton = FormStyle.createIconButton("Ingresar", CircleIcon.Symbol.ARROW_RIGHT, 46);
        cancelButton = FormStyle.createIconButton("Cancelar", CircleIcon.Symbol.CLOSE, 46);

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
