package Server;

import Protocol.MessageCodec;
import Protocol.Notification;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* Sesiones abiertas y su canal de notificaciones. Es lo que permite hacer el broadcast. */
final class ClientRegistry {

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, PushChannel> pushChannels = new ConcurrentHashMap<>();

    String openSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.add(sessionId);
        return sessionId;
    }

    boolean isOpen(String sessionId) {
        return sessionId != null && sessions.contains(sessionId);
    }

    boolean attachPush(String sessionId, PushChannel channel) {
        if (!isOpen(sessionId)) {
            return false;
        }
        PushChannel previous = pushChannels.put(sessionId, channel);
        if (previous != null) {
            previous.close();
        }
        return true;
    }

    void detachPush(String sessionId, PushChannel channel) {
        pushChannels.remove(sessionId, channel);
    }

    /* Se llama cuando el canal de peticiones de la sesion se cierra: la sesion termina completa. */
    void closeSession(String sessionId) {
        sessions.remove(sessionId);
        PushChannel channel = pushChannels.remove(sessionId);
        if (channel != null) {
            channel.close();
        }
    }

    /* Envia la notificacion a todas las sesiones con canal activo, menos a la que la origino. */
    void broadcastExcept(String senderSessionId, Notification notification) {
        byte[] frame;
        try {
            frame = MessageCodec.encode(notification);
        } catch (IOException e) {
            System.err.println("No se pudo serializar la notificacion: " + e.getMessage());
            return;
        }
        for (Map.Entry<String, PushChannel> entry : pushChannels.entrySet()) {
            if (entry.getKey().equals(senderSessionId)) {
                continue;
            }
            try {
                entry.getValue().send(frame);
            } catch (IOException e) {
                // Socket muerto: se da de baja para no volver a escribirle.
                pushChannels.remove(entry.getKey(), entry.getValue());
                entry.getValue().close();
            }
        }
    }
}