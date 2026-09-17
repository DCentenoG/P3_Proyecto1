package Service;

/**
 * Señala que no fue posible obtener o interpretar la respuesta de la IA
 * usada para el llenado automático de reservas (sin conexión, clave de
 * API ausente o inválida, respuesta bloqueada por filtros de seguridad,
 * XML mal formado, etc.).
 * <p>
 * El Controller la captura y deja el formulario disponible para llenado
 * manual, tal como pide el enunciado ("el usuario podrá... modificar
 * los datos generados antes de aplicar la reserva") — nunca debe
 * bloquear el flujo normal de creación de una reserva.
 */
public class AIExtractionException extends Exception {

    public AIExtractionException(String message) {
        super(message);
    }

    public AIExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
