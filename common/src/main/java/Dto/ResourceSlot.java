package Dto;

/**
 * Una franja ocupada de un recurso concreto en un dia: el recurso, y la reserva
 * Aceptada que lo tiene tomado. Es lo que alimenta la grilla de la pantalla de
 * Calendarizacion, donde cada columna es un recurso.
 * <p>
 * Una reserva con varios recursos asignados produce un ResourceSlot por cada
 * uno, todos apuntando al mismo {@link ReservationSummary}.
 * <p>
 * El recurso se identifica por la pareja (categoryId, resourceId), que es su
 * identidad real: el id de un recurso solo es unico dentro de su categoria.
 *
 * @param categoryId          id de la categoria del recurso ("CAT-000001")
 * @param resourceId          id del recurso dentro de esa categoria
 * @param resourceDescription descripcion del recurso, para rotular la columna
 * @param reservation         la reserva que ocupa el recurso
 */
public record ResourceSlot(String categoryId,
                           int resourceId,
                           String resourceDescription,
                           ReservationSummary reservation) {
}
