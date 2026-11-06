package Model.exceptions;

/** No existe ningún funcionario con el id o el nombre buscado. */
public class EmployeeNotFoundException extends ModelException {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}