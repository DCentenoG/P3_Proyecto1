package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulario "Revisar solicitud" (mockup): se abre desde el botón
 * "Revisar" de {@link RequestsView} sobre la solicitud seleccionada.
 * <p>
 * Muestra, solo para lectura, los datos de la solicitud (funcionario,
 * actividad, fecha y horario). En "Recursos" hay una fila por cada
 * categoría que el funcionario pidió, con una lista desplegable para
 * que el administrador elija el recurso específico de esa categoría que
 * se asignará a la reserva (solo se ofrecen los recursos libres en esa
 * fecha y horario). Abajo están las acciones Aprobar y Rechazar.
 * <p>
 * Solo construye la interfaz gráfica: la carga de la solicitud, los
 * recursos disponibles y las acciones se conectan desde el manejo de
 * eventos en una etapa posterior.
 */
public class ReviewRequestForm extends JDialog {

    /** Ancho de la columna de etiquetas, para que todos los campos queden alineados. */
    private static final int LABEL_WIDTH = 104;
    /** Ancho de los campos de hora (más angostos que un campo normal, como en el mockup). */
    private static final int TIME_FIELD_WIDTH = 72;
    /** Alto máximo visible de la lista de recursos; si hay más categorías, aparece scroll. */
    private static final int RESOURCES_MAX_HEIGHT = 150;
    /** Ancho mínimo de cada lista de recursos (luego se estira al ancho disponible). */
    private static final int COMBO_MIN_WIDTH = 120;
    /** Ancho de los campos de solo lectura. */
    private static final int FIELD_WIDTH = 260;

    private JTextField employeeIdField;
    private JTextField employeeNameField;
    private JTextField activityField;
    private JTextField dateField;
    private JTextField startTimeField;
    private JTextField endTimeField;

    private JPanel resourcesPanel;
    private JScrollPane resourcesScroll;
    private final Map<String, JComboBox<String>> resourceCombos = new LinkedHashMap<>();

    private JButton approveButton;
    private JButton rejectButton;

    public ReviewRequestForm(Window owner) {
        super(owner, "Revisar solicitud", ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    private void initComponents() {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));
        setContentPane(content);

        content.add(buildFieldsPanel(), BorderLayout.CENTER);
        content.add(buildButtonsPanel(), BorderLayout.SOUTH);

        setRequiredCategories(Collections.emptyList());
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        employeeIdField = readOnlyField();
        employeeNameField = readOnlyField();
        activityField = readOnlyField();
        dateField = readOnlyField();

        int row = 0;
        gbc.gridy = row++;
        panel.add(inlineRow("ID funcionario", employeeIdField), gbc);
        gbc.gridy = row++;
        panel.add(inlineRow("Nombre", employeeNameField), gbc);
        gbc.gridy = row++;
        panel.add(inlineRow("Actividad", activityField), gbc);
        gbc.gridy = row++;
        panel.add(inlineRow("Fecha", dateField), gbc);
        gbc.gridy = row++;
        panel.add(buildTimeRow(), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 6, 0);
        panel.add(buildResourcesHeader(), gbc);

        resourcesPanel = new JPanel(new GridBagLayout());
        resourcesPanel.setBackground(UITheme.WHITE);
        resourcesPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        resourcesScroll = new JScrollPane(resourcesPanel);
        UITheme.styleScrollPane(resourcesScroll);
        resourcesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        resourcesScroll.getViewport().setBackground(UITheme.WHITE);

        gbc.gridy = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(resourcesScroll, gbc);

        return panel;
    }

    private JPanel buildTimeRow() {
        startTimeField = readOnlyField();
        startTimeField.setPreferredSize(new Dimension(TIME_FIELD_WIDTH, UITheme.FIELD_HEIGHT));
        endTimeField = readOnlyField();
        endTimeField.setPreferredSize(new Dimension(TIME_FIELD_WIDTH, UITheme.FIELD_HEIGHT));

        JPanel endGroup = new JPanel(new BorderLayout(8, 0));
        endGroup.setOpaque(false);
        endGroup.add(UITheme.createLabel("Hora fin"), BorderLayout.WEST);
        endGroup.add(endTimeField, BorderLayout.CENTER);

        JPanel startGroup = new JPanel(new BorderLayout(8, 0));
        startGroup.setOpaque(false);
        startGroup.add(fixedWidthLabel("Hora inicio"), BorderLayout.WEST);
        startGroup.add(startTimeField, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(startGroup);
        row.add(javax.swing.Box.createHorizontalStrut(28));
        row.add(endGroup);
        return row;
    }

    private JPanel buildResourcesHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UITheme.createLabel("Recursos"), BorderLayout.WEST);
        header.add(UITheme.createNote("Uno disponible por categoría"), BorderLayout.EAST);
        return header;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        approveButton = UITheme.createStackedIconButton("Aprobar", IconLibrary.CHECKED_GREEN, 34);
        approveButton.setForeground(UITheme.ADD_GREEN);
        rejectButton = UITheme.createStackedIconButton("Rechazar", IconLibrary.REMOVE_RED, 34);
        rejectButton.setForeground(UITheme.DANGER_RED);

        panel.add(approveButton);
        panel.add(rejectButton);
        return panel;
    }

    // ---------------------------------------------------------------
    // Recursos por categoría
    // ---------------------------------------------------------------

    /**
     * Arma una fila por cada categoría requerida por la solicitud: el
     * nombre de la categoría y una lista desplegable (vacía) para elegir
     * el recurso. El controlador la llena con los recursos disponibles
     * mediante {@link #getResourceCombo(String)}. Reemplaza las filas que
     * hubiera antes y reajusta el tamaño del formulario.
     */
    public void setRequiredCategories(List<String> categories) {
        resourcesPanel.removeAll();
        resourceCombos.clear();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;

        if (categories.isEmpty()) {
            gbc.gridx = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            resourcesPanel.add(UITheme.createNote("La solicitud no tiene categorías requeridas."), gbc);
        }

        for (String category : categories) {
            JLabel categoryLabel = new JLabel(category);
            categoryLabel.setFont(UITheme.FIELD_FONT);
            categoryLabel.setForeground(UITheme.TEXT_BLUE);

            JComboBox<String> combo = new JComboBox<>();
            UITheme.styleCombo(combo);
            // Ancho preferido chico: la columna se estira (weightx) hasta el ancho
            // disponible, en vez de desbordar la lista de recursos por la derecha.
            combo.setPreferredSize(new Dimension(COMBO_MIN_WIDTH, combo.getPreferredSize().height));
            resourceCombos.put(category, combo);

            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(gbc.gridy == 0 ? 0 : 8, 0, 0, 12);
            resourcesPanel.add(categoryLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(gbc.gridy == 0 ? 0 : 8, 0, 0, 0);
            resourcesPanel.add(combo, gbc);

            gbc.gridy++;
        }

        int contentHeight = resourcesPanel.getPreferredSize().height + 4;
        resourcesScroll.setPreferredSize(new Dimension(10, Math.min(contentHeight, RESOURCES_MAX_HEIGHT)));

        resourcesPanel.revalidate();
        resourcesPanel.repaint();
        pack();
        setMinimumSize(null);
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    // ---------------------------------------------------------------
    // Utilidades de construcción propias de este formulario
    // ---------------------------------------------------------------

    /** Campo de solo lectura: muestra el dato de la solicitud pero no permite modificarlo. */
    private static JTextField readOnlyField() {
        JTextField field = new JTextField();
        UITheme.styleField(field);
        field.setPreferredSize(new Dimension(FIELD_WIDTH, UITheme.FIELD_HEIGHT));
        field.setEditable(false);
        field.setBackground(UITheme.BACKGROUND);
        return field;
    }

    private static JPanel inlineRow(String labelText, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(fixedWidthLabel(labelText), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JLabel fixedWidthLabel(String text) {
        JLabel label = UITheme.createLabel(text);
        int width = Math.max(LABEL_WIDTH, label.getPreferredSize().width);
        label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
        return label;
    }

    // ---- Carga de la solicitud (la usa el controlador al abrir el formulario) ----

    public void setEmployeeId(String employeeId) { employeeIdField.setText(employeeId); }
    public void setEmployeeName(String employeeName) { employeeNameField.setText(employeeName); }
    public void setActivity(String activity) { activityField.setText(activity); }
    public void setDate(String date) { dateField.setText(date); }
    public void setStartTime(String startTime) { startTimeField.setText(startTime); }
    public void setEndTime(String endTime) { endTimeField.setText(endTime); }

    // ---- Métodos de acceso para el futuro controlador ----

    /** Lista desplegable de recursos de {@code category}, o {@code null} si la solicitud no la requiere. */
    public JComboBox<String> getResourceCombo(String category) {
        return resourceCombos.get(category);
    }

    /** Listas desplegables de recursos, una por categoría requerida, en el orden de la solicitud. */
    public Map<String, JComboBox<String>> getResourceCombos() {
        return Collections.unmodifiableMap(resourceCombos);
    }

    public JButton getApproveButton() { return approveButton; }
    public JButton getRejectButton() { return rejectButton; }
}
