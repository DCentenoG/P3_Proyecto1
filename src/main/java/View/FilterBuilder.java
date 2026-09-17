package View;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye el panel de filtros de búsqueda para {@link CRUDView} según
 * la entidad consultada: Funcionario (ID, Nombre), Categoría
 * (Descripción) o Recurso (Categoría, Descripción). Solo arma los
 * campos; la búsqueda en sí se dispara desde el manejo de eventos al
 * presionar el ícono de buscar.
 * <p>
 * El panel usa un {@link BoxLayout} horizontal (en vez de
 * {@link java.awt.FlowLayout}) para que el primer campo (p. ej. "ID")
 * quede exactamente en el borde izquierdo del panel, sin el margen
 * adicional que {@code FlowLayout} reserva antes del primer componente;
 * así la etiqueta "Búsqueda" puesta arriba en {@link CRUDView} queda
 * perfectamente alineada con él.
 */
public class FilterBuilder {

    /** Valor sentinela que representa "sin filtro aplicado" en los combos de filtro. */
    public static final String NO_FILTER = "(Todas)";

    private static final int FIELD_GAP = 24;

    private final JPanel panel = new JPanel();
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    public FilterBuilder(EntityType entityType, List<String> categoryOptions) {
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        switch (entityType) {
            case FUNCIONARIO -> {
                addTextFilter("ID");
                addTextFilter("Nombre");
            }
            case CATEGORIA -> addTextFilter("Descripción");
            case RECURSO -> {
                addComboFilter("Categoría", categoryOptions);
                addTextFilter("Descripción");
            }
        }
    }

    private void addTextFilter(String label) {
        JTextField field = new JTextField(14);
        UITheme.styleField(field);
        fields.put(label, field);
        addToPanel(UITheme.labeledField(label, field));
    }

    private void addComboFilter(String label, List<String> options) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem(NO_FILTER);
        for (String option : options) {
            combo.addItem(option);
        }
        UITheme.styleCombo(combo);
        fields.put(label, combo);
        addToPanel(UITheme.labeledField(label, combo));
    }

    private void addToPanel(JComponent group) {
        if (panel.getComponentCount() > 0) {
            panel.add(Box.createHorizontalStrut(FIELD_GAP));
        }
        panel.add(group);
    }

    public JPanel getPanel() {
        return panel;
    }

    /** Devuelve el campo de filtro asociado a una etiqueta (p. ej. "ID", "Descripción"). */
    public JComponent getField(String label) {
        return fields.get(label);
    }

    /** Texto ingresado en un filtro de texto; {@code null} si no existe o no es de texto. */
    public String getTextValue(String label) {
        JComponent component = fields.get(label);
        return (component instanceof JTextField field) ? field.getText() : null;
    }

    /**
     * Valor seleccionado en un filtro de combo; {@code null} si no existe o no es un combo.
     * Puede devolver {@link #NO_FILTER} cuando el usuario no aplicó ese filtro.
     */
    public Object getSelectedValue(String label) {
        JComponent component = fields.get(label);
        return (component instanceof JComboBox<?> combo) ? combo.getSelectedItem() : null;
    }
}
