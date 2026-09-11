package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulario de edición dinámico: según el {@link EntityType} recibido
 * arma los campos correspondientes (Funcionario: ID/Nombre/Teléfono;
 * Categoría: ID/Descripción; Recurso: ID/Categoría/Descripción, con la
 * Categoría como lista desplegable) y los botones Guardar, Limpiar y
 * Cancelar del mockup. Solo construye la interfaz gráfica: la
 * confirmación antes de ejecutar la opción elegida y el guardado real
 * se conectan desde el manejo de eventos en una etapa posterior.
 */
public class FormDialog extends JDialog {

    private final EntityType entityType;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    private JButton saveButton;
    private JButton clearButton;
    private JButton cancelButton;

    public FormDialog(Window owner, EntityType entityType, List<String> categoryOptions) {
        super(owner, "Editar " + entityType.getSingularTitle(), ModalityType.APPLICATION_MODAL);
        this.entityType = entityType;
        initComponents(categoryOptions);
    }

    private void initComponents(List<String> categoryOptions) {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(28, 40, 24, 40));
        setContentPane(content);

        content.add(buildFieldsPanel(categoryOptions), BorderLayout.CENTER);
        content.add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private JPanel buildFieldsPanel(List<String> categoryOptions) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = entityType.getColumns();
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];

            gbc.gridy = i * 2;
            gbc.insets = new Insets(i == 0 ? 0 : 14, 0, 6, 0);
            panel.add(UITheme.createLabel(label + ":"), gbc);

            gbc.gridy = i * 2 + 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            JComponent field = createField(label, categoryOptions);
            fields.put(label, field);
            panel.add(field, gbc);
        }

        return panel;
    }

    private JComponent createField(String label, List<String> categoryOptions) {
        if (entityType == EntityType.RECURSO && "Categoría".equals(label)) {
            JComboBox<String> combo = new JComboBox<>(categoryOptions.toArray(new String[0]));
            combo.setFont(UITheme.FIELD_FONT);
            return combo;
        }
        JTextField field = new JTextField();
        UITheme.styleField(field);
        return field;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        saveButton = UITheme.createStackedIconButton("Guardar", IconLibrary.DISKETTE, 30);
        clearButton = UITheme.createStackedIconButton("Limpiar", IconLibrary.ERASER, 30);
        cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 30);

        panel.add(saveButton);
        panel.add(clearButton);
        panel.add(cancelButton);
        return panel;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JComponent getField(String label) {
        return fields.get(label);
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
