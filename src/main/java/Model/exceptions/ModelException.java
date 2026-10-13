package Model.exceptions;

/**
 * Excepción base de todo lo que puede fallar dentro de la capa Model:
 * búsquedas sin resultado, duplicados, o reservas sin disponibilidad.
 * La capa Service es quien debe atrapar estas excepciones y traducirlas
 * a ServiceException con un mensaje de cara al usuario; el Model nunca
 * debe conocer ServiceException (evita una dependencia circular entre
 * capas).
 */
public abstract class ModelException extends Exception {
    protected ModelException(String message) {
        super(message);
    }
}