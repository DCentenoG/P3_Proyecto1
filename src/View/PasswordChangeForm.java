package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Ventana para el cambio de clave de un usuario.
 * <p>
 * Esta clase construye únicamente la interfaz gráfica, siguiendo la
 * distribución definida en el mockup del proyecto: los campos "Clave
 * actual", "Clave nueva" y "Confirmar clave nueva", y los botones de
 * confirmar y cancelar con los íconos reales de {@code /Resources/Icons}
 * (sin texto debajo, tal como en el mockup). La validación y el cambio
 * real de la clave se conectarán con el controlador en una etapa
 * posterior.
 */
public class PasswordChangeForm extends JFrame {

    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton confirmButton;
    private JButton cancelButton;

    public PasswordChangeForm() {
        super("Cambiar clave");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

        currentPasswordField = new JPasswordField();
        newPasswordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        for (JPasswordField field : new JPasswordField[]{currentPasswordField, newPasswordField, confirmPasswordField}) {
            field.setPreferredSize(new Dimension(280, 28));
            UITheme.styleField(field);
        }

        addLabeledField(panel, gbc, 0, "Clave actual:", currentPasswordField, true);
        addLabeledField(panel, gbc, 2, "Clave nueva:", newPasswordField, false);
        addLabeledField(panel, gbc, 4, "Confirmar clave nueva:", confirmPasswordField, false);

        return panel;
    }

    private void addLabeledField(JPanel panel, GridBagConstraints gbc, int row,
                                  String labelText, JComponent field, boolean firstRow) {
        gbc.gridy = row;
        gbc.insets = new Insets(firstRow ? 0 : 16, 0, 6, 0);
        panel.add(UITheme.createLabel(labelText), gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(field, gbc);
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 0, 0, 0));

        confirmButton = UITheme.createIconOnlyButton(IconLibrary.CHECKED, 46);
        cancelButton = UITheme.createIconOnlyButton(IconLibrary.REMOVE, 46);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 24, 0, 24);

        gbc.gridx = 0;
        panel.add(confirmButton, gbc);
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        return panel;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public char[] getCurrentPassword() {
        return currentPasswordField.getPassword();
    }

    public char[] getNewPassword() {
        return newPasswordField.getPassword();
    }

    public char[] getConfirmedPassword() {
        return confirmPasswordField.getPassword();
    }

    public JButton getConfirmButton() {
        return confirmButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
