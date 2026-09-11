package View;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye el panel de filtros de búsqueda para {@link CRUDView} según
 * la entidad consultada: Funcionario (ID, Nombre), Categoría
 * (Descripción) o Recurso (Categoría, Descripción). Solo arma los
 * campos; la búsqueda en sí se dispara desde el manejo de eventos al
 * presionar el ícono de buscar.
 */
public class FilterBuilder {

    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    public FilterBuilder(EntityType entityType, List<String> categoryOptions) {
        panel.setOpaque(false);
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
        panel.add(UITheme.labeledField(label, field));
    }

    private void addComboFilter(String label, List<String> options) {
        JComboBox<String> combo = new JComboBox<>(options.toArray(new String[0]));
        combo.setFont(UITheme.FIELD_FONT);
        fields.put(label, combo);
        panel.add(UITheme.labeledField(label, combo));
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

    /** Valor seleccionado en un filtro de combo; {@code null} si no existe o no es un combo. */
    public Object getSelectedValue(String label) {
        JComponent component = fields.get(label);
        return (component instanceof JComboBox<?> combo) ? combo.getSelectedItem() : null;
    }
}
