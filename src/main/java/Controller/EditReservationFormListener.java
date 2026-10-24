package Controller;

import Model.Reservation;
import Service.AIReservationExtractionService;
import Service.ServiceException;
import View.EditReservationForm;

/**
 * Maneja los eventos de {@link EditReservationForm}: carga en el
 * formulario los datos de la reserva a editar y conecta el llenado por
 * IA a partir de la "Frase", los selectores de fecha/hora, el combo +
 * minitabla de categorías y los botones Guardar/Cancelar.
 * <p>
 * El formulario se llena y se valida exactamente igual que "Nueva
 * reserva" (ver {@link ReservationFormSupport}): no se puede guardar con
 * campos obligatorios vacíos, la hora de fin debe ser posterior a la de
 * inicio y Guardar pide confirmación antes de ejecutarse.
 */
public final class EditReservationFormListener {

    private final EditReservationForm form;
    private final SessionContext session;
    private final Reservation reservation;

    // Misma IA que usa "Nueva reserva": solo interpreta la frase y llena
    // texto; validar y guardar sigue en ReservationService.
    private final AIReservationExtractionService aiExtractionService = new AIReservationExtractionService();

    /** Se invoca después de guardar los cambios, para que la vista que abrió el formulario se refresque. */
    private final Runnable afterSave;

    public EditReservationFormListener(EditReservationForm form, SessionContext session, Reservation reservation,
                                       Runnable afterSave) {
        this.form = form;
        this.session = session;
        this.reservation = reservation;
        this.afterSave = (afterSave != null) ? afterSave : () -> { };
        ReservationFormSupport.loadCategoryOptions(form.getCategoryCombo(), session.getCategories().getCategories());
        loadReservation();
        wire();
    }

    private void wire() {
        form.getAiButton().addActionListener(e -> onAiFill());
        form.getDateDropdownButton().addActionListener(e ->
                ReservationFormSupport.pickDate(form, form.getDateField()));
        form.getStartTimeButton().addActionListener(e ->
                ReservationFormSupport.pickStartTime(form, form.getStartTimeField(), form.getEndTimeField()));
        form.getEndTimeButton().addActionListener(e ->
                ReservationFormSupport.pickEndTime(form, form.getStartTimeField(), form.getEndTimeField()));
        form.getAddCategoryButton().addActionListener(e ->
                ReservationFormSupport.addSelectedCategory(form.getCategoryCombo(), form.getCategoriesTable()));
        form.getRemoveCategoryButton().addActionListener(e ->
                ReservationFormSupport.removeSelectedCategory(form, form.getCategoriesTable()));
        form.getSaveButton().addActionListener(e -> onSave());
        form.getCancelButton().addActionListener(e -> form.dispose());
    }

    /**
     * Completa el formulario a partir de la "Frase" con IA, igual que en
     * "Nueva reserva" (ver {@link ReservationFormSupport#fillFromPhrase}):
     * solo se reemplaza lo que la frase menciona, el resto de los datos de
     * la reserva se conserva.
     */
    private void onAiFill() {
        ReservationFormSupport.fillFromPhrase(form, aiExtractionService, form.getPhraseField(), form.getAiButton(),
                new ReservationFormSupport.FormFields(form.getActivityField(), form.getDateField(),
                        form.getStartTimeField(), form.getEndTimeField(), form.getCategoriesTable()),
                session.getCategories().getCategories());
    }

    /** Precarga el formulario con los datos actuales de la reserva. */
    private void loadReservation() {
        form.setActivity(reservation.getActivity());
        form.setDate(reservation.getDate().format(ReservationFormSupport.DATE_FORMAT));
        form.setStartTime(reservation.getStartTime().format(ReservationFormSupport.TIME_FORMAT));
        form.setEndTime(reservation.getEndTime().format(ReservationFormSupport.TIME_FORMAT));
        form.setSelectedCategories(ReservationFormSupport.categoriesOf(reservation));
    }

    private void onSave() {
        ReservationFormSupport.ReservationInput input = ReservationFormSupport.readAndValidate(form,
                form.getActivityField().getText(), form.getDateField().getText(),
                form.getStartTimeField().getText(), form.getEndTimeField().getText(),
                form.getSelectedCategories());
        if (input == null) {
            return;
        }
        if (!DialogHelper.confirm(form, "¿Desea guardar los cambios de esta reserva?")) {
            return;
        }

        // Revalidar, reasignar recursos (todo o nada) y guardar el XML vive en
        // Service.ReservationService (ver Model.Employee#tryEdit): aquí solo se
        // juntan los datos del formulario y se traduce el resultado a diálogos.
        // Si falla, el formulario queda abierto para que el funcionario corrija.
        try {
            session.getReservationService().editReservation(session.getCurrentUser().getId(), reservation,
                    input.activity(), input.date(), input.startTime(), input.endTime(), input.categories());
        } catch (ServiceException ex) {
            DialogHelper.error(form, ex.getMessage());
            return;
        }

        DialogHelper.info(form, "Editar reserva", "Los cambios de la reserva se guardaron correctamente.");
        form.dispose();
        afterSave.run();
    }
}
