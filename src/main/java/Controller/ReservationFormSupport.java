package Controller;

import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Service.AIExtractionException;
import Service.AIReservationExtractionService;
import Service.ExtractedReservationData;
import View.DatePickerDialog;
import View.TimePickerDialog;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Comportamiento compartido por los dos formularios de reserva del
 * funcionario: "Nueva reserva" ({@code View.ReservationsView}) y
 * "Editar reserva" ({@code View.EditReservationForm}). Ambos se llenan
 * exactamente igual (llenado por IA a partir de la "Frase", selectores de
 * fecha/hora, combo + minitabla de categorías) y se validan con las mismas
 * reglas, así que esa lógica vive aquí una sola vez en vez de repetirse
 * en cada listener.
 */
final class ReservationFormSupport {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ReservationFormSupport() {
    }

    // ------------------------------------------------------------------
    // Selectores de fecha/hora
    // ------------------------------------------------------------------

    /**
     * Abre el selector de fecha sobre {@code dateField}. Una reserva no se
     * puede hacer en una fecha que ya pasó (regla que también aplica
     * Service.ReservationService): se previene directamente en el
     * selector, en vez de dejar elegirla y recién avisar al guardar.
     */
    static void pickDate(Component parent, JTextField dateField) {
        LocalDate initial = parseDateOrNull(dateField.getText());
        LocalDate picked = DatePickerDialog.show(windowOf(parent), initial, LocalDate.now(), null);
        if (picked != null) {
            dateField.setText(picked.format(DATE_FORMAT));
        }
    }

    /** La hora de inicio no puede quedar después de la hora de fin ya elegida (si existe). */
    static void pickStartTime(Component parent, JTextField startField, JTextField endField) {
        LocalTime initial = parseTimeOrNull(startField.getText());
        LocalTime max = parseTimeOrNull(endField.getText());
        LocalTime picked = TimePickerDialog.show(windowOf(parent), initial, null, max);
        if (picked != null) {
            startField.setText(picked.format(TIME_FORMAT));
        }
    }

    /** La hora de fin no puede quedar antes de la hora de inicio ya elegida (si existe). */
    static void pickEndTime(Component parent, JTextField startField, JTextField endField) {
        LocalTime initial = parseTimeOrNull(endField.getText());
        LocalTime min = parseTimeOrNull(startField.getText());
        LocalTime picked = TimePickerDialog.show(windowOf(parent), initial, min, null);
        if (picked != null) {
            endField.setText(picked.format(TIME_FORMAT));
        }
    }

    // ------------------------------------------------------------------
    // Combo + minitabla de categorías requeridas
    // ------------------------------------------------------------------

    static void loadCategoryOptions(JComboBox<String> combo, List<ResourceCategory> categories) {
        combo.removeAllItems();
        for (ResourceCategory category : categories) {
            combo.addItem(category.getDescription());
        }
    }

    /** Agrega a la minitabla la categoría elegida en el combo (sin duplicarla). */
    static void addSelectedCategory(JComboBox<String> combo, JTable categoriesTable) {
        Object selected = combo.getSelectedItem();
        if (selected == null) {
            return;
        }
        String category = selected.toString();
        DefaultTableModel model = (DefaultTableModel) categoriesTable.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (category.equals(model.getValueAt(row, 0))) {
                return; // ya está en la lista, no se duplica
            }
        }
        model.addRow(new Object[]{category});
    }

    /** Quita de la minitabla la categoría seleccionada en ella. */
    static void removeSelectedCategory(Component parent, JTable categoriesTable) {
        int row = categoriesTable.getSelectedRow();
        if (row < 0) {
            DialogHelper.warn(parent, "Debe seleccionar una categoría de la lista para quitarla.");
            return;
        }
        ((DefaultTableModel) categoriesTable.getModel()).removeRow(row);
    }

    /**
     * Categorías de una reserva ya registrada, en orden y sin repetir.
     * <p>
     * Por ahora {@link Reservation} no guarda las categorías requeridas
     * por separado, así que se deducen de la categoría de cada recurso
     * asignado. Cuando el modelo de estados registre las categorías
     * pedidas por el funcionario (los recursos pasan a asignarse al
     * aprobar la solicitud), basta con cambiar este método.
     */
    static List<String> categoriesOf(Reservation reservation) {
        List<String> categories = new ArrayList<>();
        for (Resource resource : reservation.getAssignedResources()) {
            String description = resource.getResourceCategoryReference().getDescription();
            if (!categories.contains(description)) {
                categories.add(description);
            }
        }
        return categories;
    }

    // ------------------------------------------------------------------
    // Llenado automático por IA (a partir de la "Frase")
    // ------------------------------------------------------------------

    /** Campos de un formulario de reserva que el llenado por IA puede completar. */
    record FormFields(JTextField activity, JTextField date, JTextField startTime, JTextField endTime,
                      JTable categoriesTable) {
    }

    /**
     * Envía la "Frase" a {@link AIReservationExtractionService} en un hilo
     * aparte (la llamada de red toma 1-3s) y, con lo que devuelva, precarga
     * los campos del formulario. Nunca bloquea el flujo normal: ante
     * cualquier falla (sin red, clave ausente, bloqueo de seguridad, XML
     * mal formado) solo avisa y deja el formulario para llenado manual.
     * <p>
     * Solo se reemplaza lo que la IA logró identificar: un campo que la
     * frase no menciona conserva su valor actual (importante en "Editar
     * reserva", donde el formulario ya viene con los datos de la reserva).
     */
    static void fillFromPhrase(Component parent, AIReservationExtractionService aiService, JTextField phraseField,
                               JButton aiButton, FormFields fields, List<ResourceCategory> systemCategories) {
        String phrase = phraseField.getText().trim();
        if (phrase.isEmpty()) {
            DialogHelper.warn(parent, "Escriba una frase en el campo \"Frase\" antes de generar la reserva.");
            return;
        }

        aiButton.setEnabled(false);
        Cursor previousCursor = parent.getCursor();
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ExtractedReservationData, Void>() {
            @Override
            protected ExtractedReservationData doInBackground() throws AIExtractionException {
                return aiService.extractFromPhrase(phrase);
            }

            @Override
            protected void done() {
                parent.setCursor(previousCursor);
                aiButton.setEnabled(true);
                try {
                    applyExtractedData(parent, get(), fields, systemCategories);
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    DialogHelper.warn(parent, "No se pudo interpretar la frase (" + cause.getMessage()
                            + "). Complete los campos manualmente.");
                }
            }
        }.execute();
    }

    /** Precarga el formulario con lo que la IA logró extraer; el usuario puede revisar/editar antes de guardar. */
    private static void applyExtractedData(Component parent, ExtractedReservationData data, FormFields fields,
                                           List<ResourceCategory> systemCategories) {
        if (data.getActivity() != null) {
            fields.activity().setText(data.getActivity());
        }
        if (data.getDate() != null) {
            fields.date().setText(data.getDate().format(DATE_FORMAT));
        }
        if (data.getStartTime() != null) {
            fields.startTime().setText(data.getStartTime().format(TIME_FORMAT));
        }
        if (data.getEndTime() != null) {
            fields.endTime().setText(data.getEndTime().format(TIME_FORMAT));
        }

        if (!data.getCategoryDescriptions().isEmpty()) {
            DefaultTableModel model = (DefaultTableModel) fields.categoriesTable().getModel();
            model.setRowCount(0);
            for (String category : data.getCategoryDescriptions()) {
                model.addRow(new Object[]{normalizeCategoryDescription(category, systemCategories)});
            }
        }

        if (data.isEmpty()) {
            DialogHelper.info(parent, "Generación automática",
                    "No se pudo identificar ningún dato en esa frase. Complete el formulario manualmente.");
        }
    }

    /**
     * Si lo que extrajo la IA coincide (sin importar mayúsculas/minúsculas)
     * con una categoría real del sistema, se usa el texto exacto de esa
     * categoría, para que quede reconocida al guardar sin que el usuario
     * tenga que corregir un simple cambio de mayúsculas. Si no coincide con
     * ninguna, se deja tal cual la extrajo la IA: el usuario la revisa y,
     * si hace falta, la reemplaza usando el combo de categorías — aquí no
     * se valida existencia ni disponibilidad, eso sigue en ReservationService.
     */
    private static String normalizeCategoryDescription(String extracted, List<ResourceCategory> systemCategories) {
        for (ResourceCategory category : systemCategories) {
            if (category.getDescription().equalsIgnoreCase(extracted)) {
                return category.getDescription();
            }
        }
        return extracted;
    }

    // ------------------------------------------------------------------
    // Validación del formulario
    // ------------------------------------------------------------------

    /** Datos ya validados y convertidos de un formulario de reserva. */
    record ReservationInput(String activity, LocalDate date, LocalTime startTime, LocalTime endTime,
                            List<String> categories) {
    }

    /**
     * Valida los campos de un formulario de reserva con las reglas
     * pedidas (todos los campos con * son obligatorios, formatos de fecha
     * y hora, hora de fin posterior a la de inicio). Si algo no cumple,
     * avisa con el mensaje correspondiente y devuelve {@code null}.
     */
    static ReservationInput readAndValidate(Component parent, String activityText, String dateText,
                                            String startText, String endText, List<String> categories) {
        String activity = activityText.trim();
        String date = dateText.trim();
        String start = startText.trim();
        String end = endText.trim();

        if (activity.isEmpty() || date.isEmpty() || start.isEmpty() || end.isEmpty() || categories.isEmpty()) {
            DialogHelper.warn(parent, "Debe completar todos los campos marcados con * antes de guardar la reserva.");
            return null;
        }

        LocalDate parsedDate;
        LocalTime startTime;
        LocalTime endTime;
        try {
            parsedDate = LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(parent, "La fecha debe tener el formato dd/mm/aaaa.");
            return null;
        }
        try {
            startTime = LocalTime.parse(start, TIME_FORMAT);
            endTime = LocalTime.parse(end, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(parent, "Las horas deben tener el formato hh:mm (24 horas).");
            return null;
        }
        if (!endTime.isAfter(startTime)) {
            DialogHelper.warn(parent, "La hora de fin debe ser posterior a la hora de inicio.");
            return null;
        }
        return new ReservationInput(activity, parsedDate, startTime, endTime, List.copyOf(categories));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    static LocalDate parseDateOrNull(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    static LocalTime parseTimeOrNull(String text) {
        try {
            return LocalTime.parse(text.trim(), TIME_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    private static Window windowOf(Component component) {
        return (component instanceof Window window) ? window : SwingUtilities.getWindowAncestor(component);
    }
}
