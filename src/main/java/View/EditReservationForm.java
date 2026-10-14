package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario "Editar reserva" (mockup): se abre desde el botón
 * "Editar reserva" de {@link ReservationsView} sobre la reserva
 * seleccionada en "Mis reservas", con sus datos ya cargados.
 * <p>
 * Los campos se modifican exactamente igual que al crear una reserva
 * nueva: se pueden completar con IA a partir de la "Frase", la fecha y
 * las horas no se escriben sino que se eligen con sus botones selectores
 * ({@link DatePickerDialog}/{@link TimePickerDialog}), y las categorías
 * requeridas se agregan desde el combo con "+" y se quitan de la
 * minitabla con el botón de basurero.
 * <p>
 * Solo construye la interfaz gráfica: cargar los datos de la reserva,
 * los selectores, las validaciones y el guardado real se conectan desde
 * el manejo de eventos en una etapa posterior.
 */
public class EditReservationForm extends JDialog {

    /** Ancho de la columna de etiquetas, para que todos los campos queden alineados. */
    private static final int LABEL_WIDTH = 92;
    /** Ancho de los campos de hora (más angostos que un campo normal, como en el mockup). */
    private static final int TIME_FIELD_WIDTH = 72;
    /** Alto del campo "Frase" (más alto que un campo normal, como en el mockup). */
    private static final int PHRASE_FIELD_HEIGHT = 40;

    private JTextField phraseField;
    private JButton aiButton;

    private JTextField activityField;
    private JTextField dateField;
    private JButton dateDropdownButton;
    private JTextField startTimeField;
    private JButton startTimeButton;
    private JTextField endTimeField;
    private JButton endTimeButton;

    private JComboBox<String> categoryCombo;
    private JButton addCategoryButton;
    private JButton removeCategoryButton;
    private JTable categoriesTable;

    private JButton saveButton;
    private JButton cancelButton;

    public EditReservationForm(Window owner) {
        super(owner, "Editar reserva", ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    private void initComponents() {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));
        setContentPane(content);

        content.add(buildPhraseSection(), BorderLayout.NORTH);
        content.add(buildFieldsPanel(), BorderLayout.CENTER);
        content.add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    /**
     * Sección "Frase" (mockup): etiqueta con el botón de IA a la derecha y,
     * debajo, el campo donde el funcionario describe en lenguaje natural
     * los cambios. Igual que en "Nueva reserva", la IA interpreta la frase
     * y completa el resto del formulario; el funcionario revisa antes de
     * guardar. Cierra con la nota de campos obligatorios, que aplica a los
     * campos de abajo.
     */
    private JPanel buildPhraseSection() {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setOpaque(false);

        // Mismo botón que el de "Nueva reserva" (ReservationsView).
        aiButton = UITheme.createIconOnlyButton(IconLibrary.GENERATIVE, 26);
        aiButton.setToolTipText("Completar el formulario a partir de la frase (IA)");
        aiButton.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
        aiButton.setOpaque(true);
        aiButton.setBackground(UITheme.WHITE);
        aiButton.setContentAreaFilled(true);
        aiButton.setPreferredSize(new Dimension(42, 36));

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        JLabel phraseLabel = UITheme.createLabel("Frase");
        labelRow.add(phraseLabel, BorderLayout.WEST);
        labelRow.add(aiButton, BorderLayout.EAST);

        phraseField = new JTextField();
        UITheme.styleField(phraseField);
        phraseField.setPreferredSize(new Dimension(phraseField.getPreferredSize().width, PHRASE_FIELD_HEIGHT));

        JPanel note = new JPanel(new BorderLayout());
        note.setOpaque(false);
        note.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        note.add(UITheme.createNote("*: Campo obligatorio"), BorderLayout.EAST);

        section.add(labelRow, BorderLayout.NORTH);
        section.add(phraseField, BorderLayout.CENTER);
        section.add(note, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Actividad
        activityField = new JTextField();
        UITheme.styleField(activityField);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(inlineRow("Actividad *", activityField, null), gbc);

        // Fecha: solo se selecciona con el botón, no se escribe.
        dateField = new JTextField();
        UITheme.styleField(dateField);
        UITheme.lockAsPickerOnly(dateField);
        dateDropdownButton = UITheme.createPickerButton("▾");
        gbc.gridy = 1;
        panel.add(inlineRow("Fecha *", dateField, dateDropdownButton), gbc);

        // Hora inicio / Hora fin, en la misma fila como en el mockup.
        gbc.gridy = 2;
        panel.add(buildTimeRow(), gbc);

        // Categorías requeridas: combo + agregar/quitar, y la minitabla debajo.
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(UITheme.createLabel("Categorías *"), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(buildCategoryPickerRow(), gbc);

        categoriesTable = buildCategoriesTable();
        JScrollPane categoriesScroll = new JScrollPane(categoriesTable);
        UITheme.styleScrollPane(categoriesScroll);
        categoriesScroll.setPreferredSize(new Dimension(10, 96));

        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(categoriesScroll, gbc);

        return panel;
    }

    private JPanel buildTimeRow() {
        startTimeField = new JTextField();
        UITheme.styleField(startTimeField);
        UITheme.lockAsPickerOnly(startTimeField); // Hora: solo se selecciona con el botón, no se escribe.
        narrow(startTimeField);
        startTimeButton = UITheme.createPickerButton("…");

        endTimeField = new JTextField();
        UITheme.styleField(endTimeField);
        UITheme.lockAsPickerOnly(endTimeField);
        narrow(endTimeField);
        endTimeButton = UITheme.createPickerButton("…");

        JPanel endGroup = new JPanel(new BorderLayout(8, 0));
        endGroup.setOpaque(false);
        endGroup.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        endGroup.add(UITheme.createLabel("Hora fin *"), BorderLayout.WEST);
        endGroup.add(fieldWithButton(endTimeField, endTimeButton), BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setOpaque(false);
        row.add(inlineRow("Hora inicio *", fieldWithButton(startTimeField, startTimeButton), null), BorderLayout.WEST);
        row.add(endGroup, BorderLayout.EAST);
        return row;
    }

    /**
     * Fila para elegir una categoría requerida: igual que en "Nueva
     * reserva" de {@link ReservationsView}, un combo con las categorías
     * del sistema y dos botones compactos para agregarla a la minitabla
     * o quitar la que esté seleccionada en ella.
     */
    private JPanel buildCategoryPickerRow() {
        categoryCombo = new JComboBox<>();
        UITheme.styleCombo(categoryCombo);

        addCategoryButton = UITheme.createSmallRoundedIconButton(IconLibrary.PLUS_WHITE, UITheme.ADD_GREEN, 30);
        addCategoryButton.setToolTipText("Agregar la categoría elegida a la lista");
        removeCategoryButton = UITheme.createSmallRoundedIconButton(IconLibrary.TRASH_WHITE, UITheme.DANGER_RED, 30);
        removeCategoryButton.setToolTipText("Quitar de la lista la categoría seleccionada");

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(categoryCombo, BorderLayout.CENTER);
        row.add(UITheme.row(6, addCategoryButton, removeCategoryButton), BorderLayout.EAST);
        return row;
    }

    /** Minitabla de una sola columna con las categorías requeridas por la reserva. */
    private JTable buildCategoriesTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[0][1], new String[]{"Categoría"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        table.setTableHeader(null);
        return table;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        saveButton = UITheme.createStackedIconButton("Guardar", IconLibrary.DISKETTE, 30);
        cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 30);

        panel.add(saveButton);
        panel.add(cancelButton);
        return panel;
    }

    // ---------------------------------------------------------------
    // Utilidades de construcción propias de este formulario
    // ---------------------------------------------------------------

    /** Etiqueta a la izquierda (ancho fijo, para alinear) + campo y, opcionalmente, su botón selector. */
    private static JPanel inlineRow(String labelText, java.awt.Component field, JButton trailingButton) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(fixedWidthLabel(labelText), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        if (trailingButton != null) {
            row.add(trailingButton, BorderLayout.EAST);
        }
        return row;
    }

    private static JPanel fieldWithButton(JTextField field, JButton button) {
        JPanel group = new JPanel(new BorderLayout(6, 0));
        group.setOpaque(false);
        group.add(field, BorderLayout.CENTER);
        group.add(button, BorderLayout.EAST);
        return group;
    }

    private static JLabel fixedWidthLabel(String text) {
        JLabel label = UITheme.createLabel(text);
        int width = Math.max(LABEL_WIDTH, label.getPreferredSize().width);
        label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
        return label;
    }

    private static void narrow(JTextField field) {
        field.setPreferredSize(new Dimension(TIME_FIELD_WIDTH, UITheme.FIELD_HEIGHT));
    }

    // ---- Carga de la reserva a editar (la usa el controlador al abrir el formulario) ----

    public void setActivity(String activity) { activityField.setText(activity); }
    public void setDate(String date) { dateField.setText(date); }
    public void setStartTime(String startTime) { startTimeField.setText(startTime); }
    public void setEndTime(String endTime) { endTimeField.setText(endTime); }

    /** Reemplaza el contenido de la minitabla por {@code categories}, en orden. */
    public void setSelectedCategories(List<String> categories) {
        DefaultTableModel model = (DefaultTableModel) categoriesTable.getModel();
        model.setRowCount(0);
        for (String category : categories) {
            model.addRow(new Object[]{category});
        }
    }

    // ---- Métodos de acceso para el futuro controlador ----

    /** Frase en lenguaje natural que la IA interpreta para completar el formulario. */
    public JTextField getPhraseField() { return phraseField; }
    /** Llena el formulario a partir de {@link #getPhraseField()} (IA). */
    public JButton getAiButton() { return aiButton; }
    public JTextField getActivityField() { return activityField; }
    public JTextField getDateField() { return dateField; }
    public JButton getDateDropdownButton() { return dateDropdownButton; }
    public JTextField getStartTimeField() { return startTimeField; }
    public JButton getStartTimeButton() { return startTimeButton; }
    public JTextField getEndTimeField() { return endTimeField; }
    public JButton getEndTimeButton() { return endTimeButton; }

    /** Combo con las categorías del sistema, para elegir la próxima a agregar a la lista. */
    public JComboBox<String> getCategoryCombo() { return categoryCombo; }
    /** Agrega a la minitabla la categoría elegida en {@link #getCategoryCombo()}. */
    public JButton getAddCategoryButton() { return addCategoryButton; }
    /** Quita de la minitabla la categoría allí seleccionada. */
    public JButton getRemoveCategoryButton() { return removeCategoryButton; }
    /** Minitabla de categorías requeridas por la reserva. */
    public JTable getCategoriesTable() { return categoriesTable; }

    /** Categorías actualmente en la minitabla, en orden. */
    public List<String> getSelectedCategories() {
        List<String> result = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) categoriesTable.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            Object value = model.getValueAt(row, 0);
            if (value != null) {
                result.add(value.toString());
            }
        }
        return result;
    }

    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
}
