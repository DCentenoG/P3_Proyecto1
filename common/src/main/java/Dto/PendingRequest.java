package Dto;

/**
 * Una solicitud de reserva pendiente de revision, tal como la ve el
 * administrador en el sidebar: quien la pidio, que pidio y si ya fue marcada
 * como vista.
 * <p>
 * Antes era un record anidado en {@code ReservationService} que llevaba las
 * entidades {@code Employee} y {@code Reservation}. Se convirtio en DTO al
 * subir el contrato a {@code common}: la capa de Presentacion solo necesita
 * mostrar estos datos, y mandar las entidades completas por el socket
 * arrastraria todas las reservas del funcionario y todos los recursos de cada
 * una.
 * <p>
 * El orden de la lista que devuelve el servicio ya viene resuelto (no vistas
 * primero, mas reciente arriba), asi que la Vista la pinta tal cual.
 *
 * @param employeeId  id del funcionario que hizo la solicitud
 * @param reservation resumen de la reserva solicitada
 * @param seen        si el administrador ya la marco como vista
 */
public record PendingRequest(int employeeId, ReservationSummary reservation, boolean seen) {

    /** Nombre del funcionario que la pidio. */
    public String employeeName() {
        return reservation.employeeName();
    }

    /** Id de la reserva solicitada. */
    public int reservationId() {
        return reservation.reservationId();
    }
}
