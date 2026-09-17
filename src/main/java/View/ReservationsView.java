package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista de reservas del funcionario (mockup pág. 1): formulario de
 * "Nueva reserva" (incluyendo el llenado asistido por IA a partir de la
 * "Frase") y la tabla "Mis reservas". Solo construye la interfaz
 * gráfica; el guardado, la validación de campos/horarios y el llamado a
 * la IA se conectan desde el manejo de eventos en una etapa posterior.
 */
public class ReservationsView extends JPanel {

    private JTextField phraseField;
    private JButton aiButton;
    private JButton helpButton;

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
    private JButton clearButton;

    private JTable reservationsTable;
    private JPanel reservationsTableContainer;
    private JButton printButton;

    private static final String NO_RESERVATIONS_MESSAGE = "No tiene reservas registradas.";

    public ReservationsView() {
        super(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(buildFormSection(), BorderLayout.NORTH);
        add(buildTableSection(), BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Sección "Nueva reserva"
    // ---------------------------------------------------------------

    private JPanel buildFormSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        gbc.gridy = 0;
        section.add(UITheme.sectionTitleWithNote("Nueva reserva", "*: Campo obligatorio"), gbc);

        gbc.gridy = 1;
        section.add(buildPhraseRow(), gbc);

        gbc.gridy = 2;
        section.add(buildActivityRow(), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        section.add(buildDateCategoriesAndTimeRow(), gbc);

        return section;
    }

    private JPanel buildPhraseRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        row.add(fixedWidthLabel("Frase", 70), BorderLayout.WEST);

        phraseField = new JTextField();
        UITheme.styleField(phraseField);
        row.add(phraseField, BorderLayout.CENTER);

        // Mismo tamaño de ícono (26) que los botones "Guardar/Cancelar/Limpiar",
        // para que no se vea desproporcionadamente pequeño junto a ellos.
        aiButton = UITheme.createIconOnlyButton(IconLibrary.GENERATIVE, 26);
        aiButton.setToolTipText("Generar reserva a partir de la frase (IA)");
        aiButton.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
        aiButton.setOpaque(true);
        aiButton.setBackground(UITheme.WHITE);
        aiButton.setContentAreaFilled(true);
        aiButton.setPreferredSize(new Dimension(42, 36));

        helpButton = new HelpButton();

        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        trailing.setOpaque(false);
        trailing.add(aiButton);
        trailing.add(helpButton);
        row.add(trailing, BorderLayout.EAST);

        return row;
    }

    private JPanel buildActivityRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(fixedWidthLabel("Actividad *", 70), BorderLayout.WEST);

        activityField = new JTextField();
        UITheme.styleField(activityField);
        row.add(activityField, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildDateCategoriesAndTimeRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0.55;
        gbc.insets = new Insets(0, 0, 0, 18);
        row.add(buildLeftColumn(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.45;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(buildRightColumn(), gbc);

        return row;
    }

    private JPanel buildLeftColumn() {
        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        dateField = new JTextField();
        UITheme.styleField(dateField);
        UITheme.lockAsPickerOnly(dateField); // Fecha: solo se selecciona con el botón, no se escribe.
        dateDropdownButton = UITheme.createPickerButton("▾");
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 14, 0);
        column.add(buildInlineField("Fecha *", 60, dateField, dateDropdownButton), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        column.add(UITheme.createLabel("Categorías requeridas *"), gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 8, 0);
        column.add(buildCategoryPickerRow(), gbc);

        categoriesTable = buildCategoriesTable();
        JScrollPane categoriesScroll = new JScrollPane(categoriesTable);
        UITheme.styleScrollPane(categoriesScroll);
        categoriesScroll.setPreferredSize(new Dimension(10, 78));

        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        column.add(categoriesScroll, gbc);

        return column;
    }

    /**
     * Fila para elegir una categoría requerida: un combo con las
     * categorías del sistema y dos botones compactos para agregarla a la
     * minitabla de abajo o quitar la que esté seleccionada en ella.
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

    /** Minitabla de una sola columna con las categorías ya agregadas a la reserva. */
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

    private JPanel buildRightColumn() {
        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        startTimeField = new JTextField();
        UITheme.styleField(startTimeField);
        UITheme.lockAsPickerOnly(startTimeField); // Hora: solo se selecciona con el botón, no se escribe.
        startTimeButton = UITheme.createPickerButton("…");
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 14, 0);
        column.add(buildInlineField("Hora inicio *", 82, startTimeField, startTimeButton), gbc);

        endTimeField = new JTextField();
        UITheme.styleField(endTimeField);
        UITheme.lockAsPickerOnly(endTimeField);
        endTimeButton = UITheme.createPickerButton("…");
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        column.add(buildInlineField("Hora fin *", 82, endTimeField, endTimeButton), gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        column.add(javax.swing.Box.createGlue(), gbc);

        saveButton = UITheme.createStackedIconButton("Guardar reserva", IconLibrary.DISKETTE, 26);
        cancelButton = UITheme.createStackedIconButton("Cancelar reserva", IconLibrary.REMOVE, 26);
        clearButton = UITheme.createStackedIconButton("Limpiar", IconLibrary.ERASER, 26);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        actions.setOpaque(false);
        actions.add(saveButton);
        actions.add(cancelButton);
        actions.add(clearButton);

        gbc.gridy = 3;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        column.add(actions, gbc);

        return column;
    }

    // ---------------------------------------------------------------
    // Sección "Mis reservas"
    // ---------------------------------------------------------------

    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        section.add(UITheme.createSectionTitle("Mis reservas"), BorderLayout.NORTH);

        String[] columns = {"ID", "Actividad", "Fecha", "Horario", "Recursos", "Estado"};
        DefaultTableModel model = new DefaultTableModel(new Object[5][columns.length], columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reservationsTable = new JTable(model);
        UITheme.styleTable(reservationsTable);

        JScrollPane tableScroll = new JScrollPane(reservationsTable);
        UITheme.styleScrollPane(tableScroll);

        JLabel emptyLabel = UITheme.createLabel(NO_RESERVATIONS_MESSAGE);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setOpaque(false);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        reservationsTableContainer = new JPanel(new CardLayout());
        reservationsTableContainer.setOpaque(false);
        reservationsTableContainer.add(tableScroll, "table");
        reservationsTableContainer.add(emptyPanel, "empty");
        section.add(reservationsTableContainer, BorderLayout.CENTER);

        printButton = UITheme.createStackedIconButton("Imprimir", IconLibrary.PRINTER, 26);
        JPanel printWrapper = new JPanel(new GridBagLayout());
        printWrapper.setOpaque(false);
        printWrapper.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        printWrapper.add(printButton);
        section.add(printWrapper, BorderLayout.EAST);

        return section;
    }

    // ---------------------------------------------------------------
    // Utilidades de construcción propias de esta vista
    // ---------------------------------------------------------------

    private static javax.swing.JLabel fixedWidthLabel(String text, int width) {
        javax.swing.JLabel label = UITheme.createLabel(text);
        // Se usa el mayor entre el ancho pedido y el que el texto realmente
        // necesita, para que una etiqueta con "*" (campo obligatorio) nunca
        // quede truncada y el asterisco deje de verse.
        int preferredWidth = Math.max(width, label.getPreferredSize().width);
        label.setPreferredSize(new Dimension(preferredWidth, label.getPreferredSize().height));
        return label;
    }

    private static JPanel buildInlineField(String labelText, int labelWidth, JTextField field, JButton trailingButton) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(fixedWidthLabel(labelText, labelWidth), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(trailingButton, BorderLayout.EAST);
        return row;
    }

    /** Botón circular con un "?" que explica el llenado automático por IA (sin ícono propio en /Resources). */
    private static final class HelpButton extends JButton {
        HelpButton() {
            super("?");
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setForeground(UITheme.ACCENT_BLUE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setPreferredSize(new Dimension(20, 20));
            setToolTipText("¿Qué hace el llenado automático de reserva?");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UITheme.ACCENT_BLUE);
            g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTextField getPhraseField() { return phraseField; }
    public JButton getAiButton() { return aiButton; }
    public JButton getHelpButton() { return helpButton; }
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
    /** Minitabla de categorías ya agregadas a la reserva en construcción. */
    public JTable getCategoriesTable() { return categoriesTable; }

    /** Categorías actualmente agregadas a la reserva en construcción, en orden. */
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
    public JButton getClearButton() { return clearButton; }
    public JTable getReservationsTable() { return reservationsTable; }
    public JButton getPrintButton() { return printButton; }

    /** Alterna entre la tabla de "Mis reservas" y el mensaje de "no hay reservas registradas". */
    public void showReservationsEmptyState(boolean empty) {
        CardLayout layout = (CardLayout) reservationsTableContainer.getLayout();
        layout.show(reservationsTableContainer, empty ? "empty" : "table");
    }
}
