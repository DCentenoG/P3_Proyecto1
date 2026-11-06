package Service;

/*
Excepcion de negocio para todo lo que puede fallar al operar sobre el
sistema desde la capa Service: datos invalidos, entidades no encontradas,
duplicados, recursos sin disponibilidad, o errores de lectura/escritura del
archivo XML (estos ultimos se envuelven aqui para que quien la use solo
tenga que atrapar un unico tipo de excepcion revisada).
*/
public class ServiceException extends Exception {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
