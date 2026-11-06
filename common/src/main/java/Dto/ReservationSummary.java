package Dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Resumen de solo lectura de una reserva Aceptada, con todo lo que las
 * pantallas de Calendarizacion y Actividades necesitan mostrar: quien la hizo,
 * que actividad, cuando y con que recursos.
 * <p>
 * Existe para que la capa de Presentacion deje de recorrer los objetos del
 * dominio. Antes, las pantallas de calendario y estadisticas iteraban
 * {@code getListOfEmployees()} y las reservas de cada funcionario en memoria:
 * eso es logica de negocio viviendo en el Controlador, y ademas seria inviable
 * con el Backend remoto, porque obligaria a traer por el socket todos los
 * funcionarios con todas sus reservas solo para pintar una grilla.
 * <p>
 * Lleva {@code reservationId} para poder identificar la reserva sin mandar el
 * objeto del dominio.
 *
 * @param reservationId        id de la reserva
 * @param activity             nombre de la actividad
 * @param employeeName         funcionario que hizo la reserva
 * @param date                 dia de la reserva
 * @param startTime            hora de inicio
 * @param endTime              hora de fin
 * @param resourceDescriptions descripcion de cada recurso asignado, en orden
 */
public record ReservationSummary(int reservationId,
                                 String activity,
                                 String employeeName,
                                 LocalDate date,
                                 LocalTime startTime,
                                 LocalTime endTime,
                                 List<String> resourceDescriptions) {

    /** Texto con el que se rotula la franja ocupada en las grillas: "Actividad - Funcionario". */
    public String label() {
        return activity + " - " + employeeName;
    }
}
