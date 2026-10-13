package Controller;

import Model.Reservation;
import View.EditReservationForm;

/**
 * Maneja los eventos de {@link EditReservationForm}: carga en el
 * formulario los datos de la reserva a editar y conecta los selectores
 * de fecha/hora, el combo + minitabla de categorías y los botones
 * Guardar/Cancelar.
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

        // PENDIENTE (Modelo, paso 1): aplicar la edición con el servicio de
        // reservas, p. ej. session.getReservationService().editReservation(...)
        // con los datos de "input", traduciendo su ServiceException a
        // DialogHelper.error. Hasta entonces el formulario solo valida.
        DialogHelper.info(form, "Editar reserva",
                "Los datos son válidos. El guardado de los cambios se habilitará al implementar la edición en el modelo.");

        form.dispose();
        afterSave.run();
    }
}
