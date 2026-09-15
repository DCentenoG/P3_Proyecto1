package View.Events;

import Model.Employee;
import Model.Resource;
import Model.ResourceCategory;
import View.CRUDView;
import View.EntityType;
import View.FilterBuilder;
import View.FormDialog;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maneja los eventos de {@link CRUDView} (compartido por Funcionarios,
 * Categorías y Recursos): la búsqueda con filtros, y las acciones
 * independientes "Agregar", "Editar" y "Borrar". También abre y conecta
 * el {@link FormDialog} usado para crear o editar un registro.
 * <p>
 * Reglas pedidas: no se puede buscar sin haber aplicado al menos un
 * criterio en los filtros; no se puede editar ni borrar sin haber
 * seleccionado un elemento de la tabla (Agregar sí funciona sin
 * selección); y tanto "Guardar" como "Borrar" piden confirmación antes
 * de ejecutarse. Por ahora no se validan los datos ingresados en el
 * formulario de agregar/editar (eso se conectará en una etapa
 * posterior, junto con las validaciones/excepciones del Model).
 */
public final class CrudViewListener {

    private final CRUDView view;
    private final SessionContext session;
    private final EntityType entityType;

    /** Entidades actualmente mostradas en la tabla, en el mismo orden que sus filas. */
    private final List<Object> currentResults = new ArrayList<>();

    /** Se invoca tras crear/editar/borrar una categoría, para refrescar el combo de Recursos. */
    private Runnable afterCategoryChange = () -> { };

    public CrudViewListener(CRUDView view, SessionContext session) {
        this.view = view;
        this.session = session;
        this.entityType = view.getEntityType();
        wire();
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getAddButton().addActionListener(e -> onAdd());
        view.getEditButton().addActionListener(e -> onEdit());
        view.getDeleteButton().addActionListener(e -> onDelete());
    }

    public void setAfterCategoryChange(Runnable callback) {
        this.afterCategoryChange = (callback != null) ? callback : () -> { };
    }

    /** Reconstruye el combo de categorías del filtro de Recursos con las categorías actuales. */
    public void refreshCategoryOptions() {
        if (entityType != EntityType.RECURSO) {
            return;
        }
        JComponent filterField = view.getFilterBuilder().getField("Categoría");
        if (!(filterField instanceof JComboBox<?> rawCombo)) {
            return;
        }
        @SuppressWarnings("unchecked")
        JComboBox<String> combo = (JComboBox<String>) rawCombo;
        Object previousSelection = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(FilterBuilder.NO_FILTER);
        for (String description : categoryDescriptions()) {
            combo.addItem(description);
        }
        if (previousSelection != null) {
            combo.setSelectedItem(previousSelection);
        }
    }

    // ------------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------------

    private void onSearch() {
        FilterBuilder filters = view.getFilterBuilder();
        if (!hasAnyCriteria(filters)) {
            DialogHelper.warn(view, "Debe indicar al menos un criterio de búsqueda antes de continuar.");
            return;
        }
        runSearch(filters);
    }

    private boolean hasAnyCriteria(FilterBuilder filters) {
        return switch (entityType) {
            case FUNCIONARIO -> nonBlank(filters.getTextValue("ID")) || nonBlank(filters.getTextValue("Nombre"));
            case CATEGORIA -> nonBlank(filters.getTextValue("Descripción"));
            case RECURSO -> {
                Object selectedCategory = filters.getSelectedValue("Categoría");
                boolean categoryApplied = selectedCategory != null && !FilterBuilder.NO_FILTER.equals(selectedCategory);
                yield categoryApplied || nonBlank(filters.getTextValue("Descripción"));
            }
        };
    }

    private void runSearch(FilterBuilder filters) {
        currentResults.clear();
        switch (entityType) {
            case FUNCIONARIO -> searchEmployees(filters);
            case CATEGORIA -> searchCategories(filters);
            case RECURSO -> searchResources(filters);
        }
        renderResults();
    }

    private void searchEmployees(FilterBuilder filters) {
        String idFilter = filters.getTextValue("ID");
        String nameFilter = filters.getTextValue("Nombre");
        for (Employee employee : session.getUsers().getListOfEmployees()) {
            boolean matchesId = !nonBlank(idFilter) || String.valueOf(employee.getId()).equals(idFilter.trim());
            boolean matchesName = !nonBlank(nameFilter)
                    || employee.getName().toLowerCase(Locale.ROOT).contains(nameFilter.trim().toLowerCase(Locale.ROOT));
            if (matchesId && matchesName) {
                currentResults.add(employee);
            }
        }
    }

    private void searchCategories(FilterBuilder filters) {
        String descriptionFilter = filters.getTextValue("Descripción");
        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (!nonBlank(descriptionFilter) || containsIgnoreCase(category.getDescription(), descriptionFilter)) {
                currentResults.add(category);
            }
        }
    }

    private void searchResources(FilterBuilder filters) {
        Object selectedCategory = filters.getSelectedValue("Categoría");
        String categoryFilter = (selectedCategory != null && !FilterBuilder.NO_FILTER.equals(selectedCategory))
                ? selectedCategory.toString() : null;
        String descriptionFilter = filters.getTextValue("Descripción");

        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (categoryFilter != null && !category.getDescription().equals(categoryFilter)) {
                continue;
            }
            for (Resource resource : category.getResources()) {
                if (!nonBlank(descriptionFilter) || containsIgnoreCase(resource.getDescription(), descriptionFilter)) {
                    currentResults.add(resource);
                }
            }
        }
    }

    private void renderResults() {
        JTable table = view.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(Math.max(7, currentResults.size()));

        for (int row = 0; row < model.getRowCount(); row++) {
            if (row < currentResults.size()) {
                Object[] values = toRow(currentResults.get(row));
                for (int col = 0; col < values.length; col++) {
                    model.setValueAt(values[col], row, col);
                }
            } else {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    model.setValueAt(null, row, col);
                }
            }
        }
        table.clearSelection();
    }

    private Object[] toRow(Object entity) {
        return switch (entityType) {
            case FUNCIONARIO -> {
                Employee employee = (Employee) entity;
                yield new Object[]{employee.getId(), employee.getName(), employee.getPhoneNumber()};
            }
            case CATEGORIA -> {
                ResourceCategory category = (ResourceCategory) entity;
                yield new Object[]{category.getId(), category.getDescription()};
            }
            case RECURSO -> {
                Resource resource = (Resource) entity;
                yield new Object[]{resource.getId(), resource.getResourceCategoryReference().getDescription(),
                        resource.getDescription()};
            }
        };
    }

    // ------------------------------------------------------------------
    // Agregar / Editar
    // ------------------------------------------------------------------

    private void onAdd() {
        openFormDialog(null);
    }

    private void onEdit() {
        int row = view.getTable().getSelectedRow();
        if (row < 0) {
            DialogHelper.warn(view, "Debe seleccionar un elemento de la tabla para continuar.");
            return;
        }
        boolean isNew = row >= currentResults.size();
        openFormDialog(isNew ? null : currentResults.get(row));
    }

    private void openFormDialog(Object entityOrNull) {
        FormDialog dialog = new FormDialog(SwingUtilities.getWindowAncestor(view), entityType, categoryDescriptions());
        String verb = (entityOrNull == null) ? "Agregar" : "Editar";
        dialog.setTitle(verb + " " + entityType.getSingularTitle());
        prefill(dialog, entityOrNull);
        wireFormDialog(dialog, entityOrNull);
        dialog.setVisible(true);
    }

    private List<String> categoryDescriptions() {
        List<String> options = new ArrayList<>();
        for (ResourceCategory category : session.getCategories().getCategories()) {
            options.add(category.getDescription());
        }
        return options;
    }

    private void prefill(FormDialog dialog, Object entityOrNull) {
        JComponent idField = dialog.getField("ID");
        switch (entityType) {
            case FUNCIONARIO -> {
                idField.setEnabled(false);
                if (entityOrNull != null) {
                    Employee employee = (Employee) entityOrNull;
                    ((JTextField) idField).setText(String.valueOf(employee.getId()));
                    ((JTextField) dialog.getField("Nombre")).setText(employee.getName());
                    ((JTextField) dialog.getField("Teléfono")).setText(String.valueOf(employee.getPhoneNumber()));
                } else {
                    ((JTextField) idField).setText("(automático)");
                }
            }
            case CATEGORIA -> {
                idField.setEnabled(false);
                if (entityOrNull != null) {
                    ResourceCategory category = (ResourceCategory) entityOrNull;
                    ((JTextField) idField).setText(category.getId());
                    ((JTextField) dialog.getField("Descripción")).setText(category.getDescription());
                } else {
                    ((JTextField) idField).setText("(automático)");
                }
            }
            case RECURSO -> {
                if (entityOrNull != null) {
                    Resource resource = (Resource) entityOrNull;
                    ((JTextField) idField).setText(String.valueOf(resource.getId()));
                    ((JTextField) dialog.getField("Descripción")).setText(resource.getDescription());
                    ((JComboBox<?>) dialog.getField("Categoría"))
                            .setSelectedItem(resource.getResourceCategoryReference().getDescription());
                }
            }
        }
    }

    private void wireFormDialog(FormDialog dialog, Object entityOrNull) {
        dialog.getCancelButton().addActionListener(e -> dialog.dispose());
        dialog.getClearButton().addActionListener(e -> clearFormDialog(dialog));
        dialog.getSaveButton().addActionListener(e -> onSaveFormDialog(dialog, entityOrNull));
    }

    private void clearFormDialog(FormDialog dialog) {
        for (String label : entityType.getColumns()) {
            JComponent field = dialog.getField(label);
            if (!field.isEnabled()) {
                continue; // el ID automático no se limpia
            }
            if (field instanceof JTextField textField) {
                textField.setText("");
            } else if (field instanceof JComboBox<?> combo && combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
        }
    }

    // ------------------------------------------------------------------
    // Guardar (sin validación de datos por ahora; solo confirmación)
    // ------------------------------------------------------------------

    private void onSaveFormDialog(FormDialog dialog, Object entityOrNull) {
        switch (entityType) {
            case FUNCIONARIO -> saveEmployee(dialog, (Employee) entityOrNull);
            case CATEGORIA -> saveCategory(dialog, (ResourceCategory) entityOrNull);
            case RECURSO -> saveResource(dialog, (Resource) entityOrNull);
        }
    }

    private void saveEmployee(FormDialog dialog, Employee existing) {
        String name = ((JTextField) dialog.getField("Nombre")).getText().trim();
        int phone = parseIntSafe(((JTextField) dialog.getField("Teléfono")).getText().trim());

        if (!DialogHelper.confirm(dialog, "¿Desea guardar los cambios de este funcionario?")) {
            return;
        }

        if (existing != null) {
            existing.setName(name);
            existing.setPhoneNumber(phone);
        } else {
            session.getUsers().addEmployee(name, phone);
        }

        dialog.dispose();
        DialogHelper.info(view, "Funcionarios", "Funcionario guardado correctamente.");
        refreshAfterChange();
    }

    private void saveCategory(FormDialog dialog, ResourceCategory existing) {
        String description = ((JTextField) dialog.getField("Descripción")).getText().trim();

        if (!DialogHelper.confirm(dialog, "¿Desea guardar los cambios de esta categoría?")) {
            return;
        }

        if (existing != null) {
            existing.setDescription(description);
        } else {
            session.getCategories().addCategory(description);
        }

        dialog.dispose();
        DialogHelper.info(view, "Categorías", "Categoría guardada correctamente.");
        afterCategoryChange.run();
        refreshAfterChange();
    }

    private void saveResource(FormDialog dialog, Resource existing) {
        JComboBox<?> categoryCombo = (JComboBox<?>) dialog.getField("Categoría");
        int id = parseIntSafe(((JTextField) dialog.getField("ID")).getText().trim());
        String description = ((JTextField) dialog.getField("Descripción")).getText().trim();
        Object selectedCategory = categoryCombo.getSelectedItem();

        if (!DialogHelper.confirm(dialog, "¿Desea guardar los cambios de este recurso?")) {
            return;
        }

        ResourceCategory targetCategory = (selectedCategory != null)
                ? session.getCategories().getCategorybyDescription(selectedCategory.toString())
                : null;
        if (targetCategory == null) {
            // Aún no hay categorías creadas; sin ellas no hay dónde guardar el recurso.
            dialog.dispose();
            return;
        }

        if (existing != null) {
            ResourceCategory originalCategory = existing.getResourceCategoryReference();
            if (originalCategory != targetCategory) {
                originalCategory.deleteResourceByIdAndDescription(existing.getId(), existing.getDescription());
                targetCategory.addResource(id, description);
            } else {
                existing.setId(id);
                existing.setDescription(description);
            }
        } else {
            targetCategory.addResource(id, description);
        }

        dialog.dispose();
        DialogHelper.info(view, "Recursos", "Recurso guardado correctamente.");
        refreshAfterChange();
    }

    // ------------------------------------------------------------------
    // Borrar
    // ------------------------------------------------------------------

    private void onDelete() {
        int row = view.getTable().getSelectedRow();
        if (row < 0 || row >= currentResults.size()) {
            DialogHelper.warn(view, "Debe seleccionar un elemento de la tabla para continuar.");
            return;
        }
        if (!DialogHelper.confirm(view, "¿Desea eliminar el elemento seleccionado?")) {
            return;
        }

        Object entity = currentResults.get(row);
        switch (entityType) {
            case FUNCIONARIO -> {
                Employee employee = (Employee) entity;
                session.getUsers().removeEmployeeByNameAndPhoneNumber(employee.getName(), employee.getPhoneNumber());
            }
            case CATEGORIA -> {
                ResourceCategory category = (ResourceCategory) entity;
                session.getCategories().deleteCategoryByIdAndDescription(category.getId(), category.getDescription());
                afterCategoryChange.run();
            }
            case RECURSO -> {
                Resource resource = (Resource) entity;
                resource.getResourceCategoryReference()
                        .deleteResourceByIdAndDescription(resource.getId(), resource.getDescription());
            }
        }

        DialogHelper.info(view, entityType.getPluralTitle(), "Elemento eliminado correctamente.");
        refreshAfterChange();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void refreshAfterChange() {
        runSearch(view.getFilterBuilder());
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean containsIgnoreCase(String text, String needle) {
        return text.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    /** Convierte a entero de forma tolerante; sin validación por ahora, un valor no numérico se guarda como 0. */
    private static int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
