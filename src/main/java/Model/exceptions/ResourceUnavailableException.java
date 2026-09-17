package Model.exceptions;

import java.util.List;

/**
 * Employee.tryBook() no pudo asignar recursos a una o más categorías
 * requeridas en el horario solicitado. Carga la lista de descripciones
 * de esas categorías, para que quien la atrape pueda reportarlas al
 * usuario sin tener que recalcularlas.
 */
public class ResourceUnavailableException extends ModelException {

    private final List<String> unavailableCategories;

    public ResourceUnavailableException(List<String> unavailableCategories) {
        super("No hay disponibilidad para las siguientes categorías: " + String.join(", ", unavailableCategories));
        this.unavailableCategories = List.copyOf(unavailableCategories);
    }

    public List<String> getUnavailableCategories() {
        return unavailableCategories;
    }
}