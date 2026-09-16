package Controller;

import Model.Employee;
import Model.Resource;
import Model.ResourceCategory;
import View.CRUDView;
import View.EntityType;
import View.FilterBuilder;
import View.FormDialog;
import Service.ServiceException;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maneja los eventos de {@link CRUDView} (compartido por Funcionarios,
 * Categorías y Recursos): la búsqueda con filtros, y las acciones
 * independientes "Agregar", "Editar" y "Borrar". También abre y conecta
 * el {@link FormDialog} usado para crear o editar un registro.
 * <p>
 * Reglas pedidas: buscar sin haber aplicado ningún criterio en los
 * filtros (todos los campos vacíos y, en Recursos, la categoría en
 * "(Todas)") no es un error: simplemente reestablece la tabla al
 * listado completo, igual que al entrar a la pantalla. No se puede
 * editar ni borrar sin haber seleccionado un elemento de la tabla
 * (Agregar sí funciona sin selección); y tanto "Guardar" como "Borrar"
 * piden confirmación antes de ejecutarse. Por ahora no se validan los
 * datos ingresados en el formulario de agregar/editar (eso se
 * conectará en una etapa posterior, junto con las
 * validaciones/excepciones del Model).
 */
public final class CrudViewListener {

    private final CRUDView view;
    private final SessionContext session;
    private final EntityType entityType;

    /** Entidades actualmente mostradas en la tabla, en el mismo orden que sus filas. */
    private final List<Object> currentResults = new ArrayList<>();

    /** Se invoca tras crear/editar/borrar una categoría, para refrescar el combo de Recursos. */
    private Runnable afterCategoryChange = () -> { };

    /**
     * Se invoca tras crear/editar/borrar en ESTE CRUD, para que los otros
     * CRUD del sistema (armados una sola vez al abrir la aplicación y
     * mantenidos vivos en las demás pestañas) refresquen su propio
     * listado con los datos actuales, en vez de mostrar la foto desactualizada
     * de la última búsqueda hasta que el usuario vuelva a buscar manualmente.
     */
    private Runnable afterAnyChange = () -> { };

    /**
     * Última búsqueda realmente aplicada (con "Buscar", o el listado
     * completo con el que arranca la pantalla): es contra ESTO, y no
     * contra lo que haya en ese momento tecleado/seleccionado en los
     * campos de filtro (que puede ser un criterio a medio escribir que el
     * usuario todavía no mandó a buscar), que se recarga la tabla cuando
     * hay un refresco automático por cambios de datos (ver
     * {@link #refresh()} y {@link #refreshAfterChange()}). Así, cambiar
     * los filtros nunca actualiza la tabla por sí solo -- solo lo hace
     * "Buscar" -- y eso queda totalmente separado del refresco en tiempo
     * real de altas/ediciones/bajas.
     */
    private FilterSnapshot activeFilters = FilterSnapshot.empty();

    public CrudViewListener(CRUDView view, SessionContext session) {
        this.view = view;
        this.session = session;
        this.entityType = view.getEntityType();
        wire();
        // Se muestra el listado completo desde el inicio (sin exigir
        // criterios de búsqueda), en vez de dejar la tabla vacía hasta la
        // primera búsqueda manual del usuario.
        runSearch(activeFilters);
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getAddButton().addActionListener(e -> onAdd());
        view.getEditButton().addActionListener(e -> onEdit());
        view.getDeleteButton().addActionListener(e -> onDelete());
        TableInteractionUtil.deselectOnClickOutside(view.getTable(),
                view.getAddButton(), view.getEditButton(), view.getDeleteButton());
    }

    public void setAfterCategoryChange(Runnable callback) {
        this.afterCategoryChange = (callback != null) ? callback : () -> { };
    }

    public void setAfterAnyChange(Runnable callback) {
        this.afterAnyChange = (callback != null) ? callback : () -> { };
    }

    /**
     * Vuelve a ejecutar la última búsqueda aplicada ({@link #activeFilters})
     * contra los datos actuales, para reflejar un alta/edición/baja hecha
     * en OTRO CRUD. A propósito no relee los campos de filtro en pantalla:
     * si el usuario tiene un criterio nuevo a medio escribir ahí, ese
     * criterio no debe colarse en la tabla hasta que presione "Buscar".
     */
    public void refresh() {
        runSearch(activeFilters);
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
        // No exigir ningún criterio: dejar todos los campos vacíos (o la
        // categoría de Recursos en "(Todas)") es una búsqueda válida que
        // simplemente reestablece la tabla al listado completo. Este es el
        // ÚNICO lugar donde lo que hay tecleado/seleccionado en los campos
        // de filtro pasa a ser la búsqueda activa: cambiar un filtro sin
        // presionar "Buscar" no debe tocar la tabla.
        activeFilters = captureFilters();
        runSearch(activeFilters);
    }

    /** Congela en un snapshot lo que hay en ese momento en los campos de filtro de este CRUD. */
    private FilterSnapshot captureFilters() {
        FilterBuilder filters = view.getFilterBuilder();
        Map<String, Object> values = new LinkedHashMap<>();
        switch (entityType) {
            case FUNCIONARIO -> {
                values.put("ID", filters.getTextValue("ID"));
                values.put("Nombre", filters.getTextValue("Nombre"));
            }
            case CATEGORIA -> values.put("Descripción", filters.getTextValue("Descripción"));
            case RECURSO -> {
                values.put("Categoría", filters.getSelectedValue("Categoría"));
                values.put("Descripción", filters.getTextValue("Descripción"));
            }
        }
        return new FilterSnapshot(values);
    }

    private void runSearch(FilterSnapshot filters) {
        currentResults.clear();
        switch (entityType) {
            case FUNCIONARIO -> searchEmployees(filters);
            case CATEGORIA -> searchCategories(filters);
            case RECURSO -> searchResources(filters);
        }
        renderResults();
    }

    private void searchEmployees(FilterSnapshot filters) {
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

    private void searchCategories(FilterSnapshot filters) {
        String descriptionFilter = filters.getTextValue("Descripción");
        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (!nonBlank(descriptionFilter) || containsIgnoreCase(category.getDescription(), descriptionFilter)) {
                currentResults.add(category);
            }
        }
    }

    private void searchResources(FilterSnapshot filters) {
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

        if (currentResults.isEmpty()) {
            model.setRowCount(0);
            view.showEmptyState(true);
            return;
        }
        view.showEmptyState(false);
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
                // El orden debe calzar con EntityType.RECURSO.getTableColumns()
                // ("Categoría", "ID", "Descripción"), que a propósito no es el
                // mismo orden que el formulario de Agregar/Editar.
                Resource resource = (Resource) entity;
                yield new Object[]{resource.getResourceCategoryReference().getDescription(), resource.getId(),
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
        String phone = ((JTextField) dialog.getField("Teléfono")).getText().trim();

        if (!DialogHelper.confirm(dialog, "¿Desea guardar los cambios de este funcionario?")) {
            return;
        }

        try {
            if (existing != null) {
                session.getUserService().updateEmployee(existing.getId(), name, phone);
            } else {
                session.getUserService().addEmployee(name, phone);
            }
        } catch (ServiceException ex) {
            DialogHelper.error(dialog, ex.getMessage());
            return;
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

        try {
            if (existing != null) {
                session.getResourceService().updateCategory(existing.getId(), existing.getDescription(), description);
            } else {
                session.getResourceService().addCategory(description);
            }
        } catch (ServiceException ex) {
            DialogHelper.error(dialog, ex.getMessage());
            return;
        }

        dialog.dispose();
        DialogHelper.info(view, "Categorías", "Categoría guardada correctamente.");
        afterCategoryChange.run();
        refreshAfterChange();
    }

    private void saveResource(FormDialog dialog, Resource existing) {
        JComboBox<?> categoryCombo = (JComboBox<?>) dialog.getField("Categoría");
        String id = ((JTextField) dialog.getField("ID")).getText().trim();
        String description = ((JTextField) dialog.getField("Descripción")).getText().trim();
        Object selectedCategory = categoryCombo.getSelectedItem();

        if (!DialogHelper.confirm(dialog, "¿Desea guardar los cambios de este recurso?")) {
            return;
        }

        try {
            if (selectedCategory == null) {
                throw new ServiceException("Debe seleccionar una categoria para el recurso.");
            }
            ResourceCategory targetCategory =
                    session.getResourceService().findCategoryByDescription(selectedCategory.toString());
            if (existing != null) {
                session.getResourceService().updateResource(existing, targetCategory, id, description);
            } else {
                session.getResourceService().addResource(targetCategory.getDescription(), id, description);
            }
        } catch (ServiceException ex) {
            DialogHelper.error(dialog, ex.getMessage());
            return;
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
        try {
            switch (entityType) {
                case FUNCIONARIO -> {
                    Employee employee = (Employee) entity;
                    session.getUserService().removeEmployee(employee.getName(), employee.getPhoneNumber());
                }
                case CATEGORIA -> {
                    ResourceCategory category = (ResourceCategory) entity;
                    session.getResourceService().removeCategory(category.getId(), category.getDescription());
                    afterCategoryChange.run();
                }
                case RECURSO -> {
                    Resource resource = (Resource) entity;
                    ResourceCategory category = resource.getResourceCategoryReference();
                    session.getResourceService().removeResource(
                            category.getDescription(), resource.getId(), resource.getDescription());
                }
            }
        } catch (ServiceException ex) {
            DialogHelper.error(view, ex.getMessage());
            return;
        }

        DialogHelper.info(view, entityType.getPluralTitle(), "Elemento eliminado correctamente.");
        refreshAfterChange();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void refreshAfterChange() {
        // Igual que refresh(): se replica la última búsqueda aplicada, no
        // lo que haya en ese momento en los campos de filtro.
        runSearch(activeFilters);
        afterAnyChange.run();
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean containsIgnoreCase(String text, String needle) {
        return text.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Copia inmutable, congelada en el momento de una búsqueda real, de
     * los valores de los campos de filtro de este CRUD. Expone la misma
     * lectura ({@code getTextValue}/{@code getSelectedValue}) que
     * {@link FilterBuilder}, pero sin quedar atada a los
     * {@link JComponent} en pantalla: así un refresco automático (por un
     * cambio de datos en este u otro CRUD) puede repetir la última
     * búsqueda aplicada sin arrastrar de paso un criterio que el usuario
     * todavía esté escribiendo/seleccionando y no haya buscado.
     */
    private static final class FilterSnapshot {
        private final Map<String, Object> values;

        private FilterSnapshot(Map<String, Object> values) {
            this.values = values;
        }

        static FilterSnapshot empty() {
            return new FilterSnapshot(Map.of());
        }

        String getTextValue(String label) {
            Object value = values.get(label);
            return (value instanceof String text) ? text : null;
        }

        Object getSelectedValue(String label) {
            return values.get(label);
        }
    }
}
