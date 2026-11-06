package Protocol;

import java.io.Serializable;

/**
 * La respuesta del Backend a una {@link Request}: o salio bien y trae el
 * resultado, o fallo y trae el mensaje de error.
 *
 * <h2>Por que el error viaja como texto y no como excepcion</h2>
 * Seria tentador serializar la {@code ServiceException} completa y relanzarla
 * en el cliente, pero una excepcion arrastra su causa, y la causa suele ser una
 * clase que solo existe en el Backend (un error de JDBC, de parseo XML o de la
 * API de IA). Al llegar al Frontend, que no tiene esas clases en su classpath,
 * la deserializacion falla con {@code ClassNotFoundException} y el usuario
 * termina viendo un error de infraestructura en vez del mensaje real.
 * <p>
 * Mandando solo el texto, el Proxy reconstruye una {@code ServiceException}
 * limpia con ese mensaje. El Frontend ve exactamente el mismo mensaje que veria
 * si todo corriera en un solo proceso, y no necesita conocer ni una clase del
 * Backend.
 */
public final class Response implements Serializable {

    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    private final boolean successful;
    private final Object payload;
    private final String errorMessage;

    private Response(boolean successful, Object payload, String errorMessage) {
        this.successful = successful;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    /** Respuesta correcta. El payload es null para las operaciones que no devuelven nada. */
    public static Response success(Object payload) {
        return new Response(true, payload, null);
    }

    /** Respuesta fallida, con el mensaje que se le va a mostrar al usuario. */
    public static Response failure(String errorMessage) {
        return new Response(false, null, errorMessage);
    }

    public boolean isSuccessful() {
        return successful;
    }

    /** Resultado de la operacion; null si fallo o si la operacion no devuelve nada. */
    public Object getPayload() {
        return payload;
    }

    /** Mensaje de error listo para mostrarle al usuario; null si la operacion salio bien. */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return successful ? "Response{ok, " + payload + "}" : "Response{error, " + errorMessage + "}";
    }
}
