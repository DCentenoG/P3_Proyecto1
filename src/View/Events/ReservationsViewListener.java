package View.Events;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import View.ReservationsView;

import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja los eventos de {@link ReservationsView}: el botón de ayuda "?"
 * (explica el llenado por IA), el llenado automático por IA (aún no
 * implementado en el Model), y las acciones de Guardar, Cancelar y
 * Limpiar de la sección "Nueva reserva".
 * <p>
 * Reglas pedidas: no se puede guardar una reserva con campos vacíos
 * (excepto "Frase"); no se puede cancelar una reserva sin haberla
 * seleccionado en la tabla; y tanto Guardar como Cancelar piden
 * confirmación antes de ejecutarse.
 */
public final class ReservationsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String HELP_MESSAGE =
            "El botón de generación automática utiliza la frase que usted escriba en el campo "
                    + "\"Frase\" para interpretar los detalles de la reserva (actividad, fecha, horario y "
                    + "categorías de recursos requeridas) y completar el resto del formulario por usted. "
                    + "Revise siempre los campos completados antes de guardar la reserva.";

    private final ReservationsView view;
    private final SessionContext session;
    private final Employee employee;

    public ReservationsViewListener(ReservationsView view, SessionContext session) {
        this.view = view;
        this.session = session;
        this.employee = (Employee) session.getCurrentUser();
        wire();
        loadCategoryOptions();
        renderReservations();
    }

    private void wire() {
        view.getHelpButton().addActionListener(e ->
                DialogHelper.info(view, "Generación automática de reservas", HELP_MESSAGE));
        view.getAiButton().addActionListener(e -> onAiFill());
        view.getSaveButton().addActionListener(e -> onSave());
        view.getCancelButton().addActionListener(e -> onCancelReservation());
        view.getClearButton().addActionListener(e -> clearForm());
        view.getPrintButton().addActionListener(e -> DialogHelper.info(view, "Imprimir",
                "La generación de reportes en PDF se implementará en una etapa posterior."));
    }

    private void loadCategoryOptions() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (ResourceCategory category : session.getCategories().getCategories()) {
            model.addElement(category.getDescription());
        }
        view.getCategoriesList().setModel(model);
    }

    private void onAiFill() {
        DialogHelper.info(view, "Generación automática",
                "La interpretación de la frase mediante Inteligencia Artificial se implementará en una etapa posterior.");
    }

    // ------------------------------------------------------------------
    // Guardar reserva
    // ------------------------------------------------------------------

    private void onSave() {
        String activity = view.getActivityField().getText().trim();
        String dateText = view.getDateField().getText().trim();
        String startText = view.getStartTimeField().getText().trim();
        String endText = view.getEndTimeField().getText().trim();
        List<String> selectedCategories = view.getCategoriesList().getSelectedValuesList();

        if (activity.isEmpty() || dateText.isEmpty() || startText.isEmpty() || endText.isEmpty()
                || selectedCategories.isEmpty()) {
            DialogHelper.warn(view, "Debe completar todos los campos marcados con * antes de guardar la reserva.");
            return;
        }

        LocalDate date;
        LocalTime startTime;
        LocalTime endTime;
        try {
            date = LocalDate.parse(dateText, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "La fecha debe tener el formato dd/mm/aaaa.");
            return;
        }
        try {
            startTime = LocalTime.parse(startText, TIME_FORMAT);
            endTime = LocalTime.parse(endText, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "Las horas deben tener el formato hh:mm (24 horas).");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            DialogHelper.warn(view, "La hora de fin debe ser posterior a la hora de inicio.");
            return;
        }

        if (!DialogHelper.confirm(view, "¿Desea guardar esta reserva?")) {
            return;
        }

        Reservation reservation = new Reservation(activity, date, startTime, endTime);
        List<String> categoriesWithoutAvailability = new ArrayList<>();
        for (String categoryName : selectedCategories) {
            ResourceCategory category = session.getCategories().getCategorybyDescription(categoryName);
            if (category == null || !assignFirstAvailableResource(reservation, category)) {
                categoriesWithoutAvailability.add(categoryName);
            }
        }
        if (!categoriesWithoutAvailability.isEmpty()) {
            DialogHelper.error(view, "No hay recursos disponibles para el horario seleccionado en: "
                    + String.join(", ", categoriesWithoutAvailability) + ".");
            return;
        }

        employee.getReservations().add(reservation);
        DialogHelper.info(view, "Reservas", "La reserva se guardó correctamente.");
        clearForm();
        renderReservations();
    }

    private boolean assignFirstAvailableResource(Reservation reservation, ResourceCategory category) {
        for (Resource resource : category.getResources()) {
            if (reservation.addResource(resource, session.getUsers())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Cancelar reserva
    // ------------------------------------------------------------------

    private void onCancelReservation() {
        int row = view.getReservationsTable().getSelectedRow();
        if (row < 0 || row >= employee.getReservations().size()) {
            DialogHelper.warn(view, "Debe seleccionar una reserva de la tabla para poder cancelarla.");
            return;
        }
        if (!DialogHelper.confirm(view, "¿Desea cancelar la reserva seleccionada?")) {
            return;
        }
        employee.getReservations().remove(row);
        renderReservations();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void clearForm() {
        view.getPhraseField().setText("");
        view.getActivityField().setText("");
        view.getDateField().setText("");
        view.getStartTimeField().setText("");
        view.getEndTimeField().setText("");
        view.getCategoriesList().clearSelection();
    }

    private void renderReservations() {
        DefaultTableModel model = (DefaultTableModel) view.getReservationsTable().getModel();
        List<Reservation> reservations = employee.getReservations();
        model.setRowCount(Math.max(5, reservations.size()));

        for (int row = 0; row < model.getRowCount(); row++) {
            if (row < reservations.size()) {
                Reservation reservation = reservations.get(row);
                model.setValueAt(row + 1, row, 0);
                model.setValueAt(reservation.getActivity(), row, 1);
                model.setValueAt(reservation.getDate().format(DATE_FORMAT), row, 2);
                model.setValueAt(reservation.getStartTime().format(TIME_FORMAT) + " - "
                        + reservation.getEndTime().format(TIME_FORMAT), row, 3);
                model.setValueAt(describeResources(reservation), row, 4);
                model.setValueAt("Confirmada", row, 5);
            } else {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    model.setValueAt(null, row, col);
                }
            }
        }
    }

    private String describeResources(Reservation reservation) {
        List<String> descriptions = new ArrayList<>();
        for (Resource resource : reservation.getAssignedResources()) {
            descriptions.add(resource.getDescription());
        }
        return String.join(", ", descriptions);
    }
}
