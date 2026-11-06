package Dto;

/**
 * Una fila de estadistica: una etiqueta y cuantas veces se conto.
 * <p>
 * Sirve para los dos paneles de la pantalla de Estadisticas:
 * <ul>
 *   <li>Recursos usados: la etiqueta es la descripcion de la categoria.</li>
 *   <li>Actividades realizadas: la etiqueta es la semana ("Semana 32-2026").</li>
 * </ul>
 * El conteo lo hace el Backend. Antes lo hacia el Controlador recorriendo todas
 * las reservas de todos los funcionarios en memoria; con el Backend remoto eso
 * obligaria a traerse el sistema entero por el socket para sumar numeros que el
 * servidor puede calcular de una vez (y que, con base de datos, seran un
 * GROUP BY).
 *
 * @param label etiqueta de la fila, tal como se muestra
 * @param count cantidad contada
 */
public record StatRow(String label, int count) {
}
