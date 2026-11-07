package Protocol;

import java.io.Serializable;

/* Primer mensaje de toda conexion: dice si es el canal de peticiones o el de notificaciones. */
public final class Hello implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Channel { REQUEST, PUSH }

    private final Channel channel;
    private final String sessionId;

    private Hello(Channel channel, String sessionId) {
        this.channel = channel;
        this.sessionId = sessionId;
    }

    /** Canal de peticiones: el servidor responde con el id de sesion. */
    public static Hello request() {
        return new Hello(Channel.REQUEST, null);
    }

    /** Canal de notificaciones de una sesion ya abierta. */
    public static Hello push(String sessionId) {
        return new Hello(Channel.PUSH, sessionId);
    }

    public Channel getChannel() {
        return channel;
    }

    public String getSessionId() {
        return sessionId;
    }
}