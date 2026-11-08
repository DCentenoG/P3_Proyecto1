package Server;

import Protocol.MessageCodec;
import Protocol.Notification;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
Sesiones abiertas, el usuario que inicio sesion en cada una y su canal de
notificaciones. Es lo que permite el broadcast (a todos) y el envio dirigido (a un
usuario concreto).
*/
final class ClientRegistry {

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, PushChannel> pushChannels = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionUsers = new ConcurrentHashMap<>();

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

    /* La sesion pasa a ser de este usuario (se llama tras un login correcto). */
    void bindUser(String sessionId, int userId) {
        if (isOpen(sessionId)) {
            sessionUsers.put(sessionId, userId);
        }
    }

    /* La sesion deja de tener usuario (logout): ya no recibe avisos dirigidos. */
    void unbindUser(String sessionId) {
        sessionUsers.remove(sessionId);
    }

    /* Se llama cuando el canal de peticiones de la sesion se cierra: la sesion termina completa. */
    void closeSession(String sessionId) {
        sessions.remove(sessionId);
        sessionUsers.remove(sessionId);
        PushChannel channel = pushChannels.remove(sessionId);
        if (channel != null) {
            channel.close();
        }
    }

    /* Envia la notificacion a todas las sesiones con canal activo, menos a la que la origino. */
    void broadcastExcept(String senderSessionId, Notification notification) {
        byte[] frame = encode(notification);
        if (frame == null) {
            return;
        }
        for (Map.Entry<String, PushChannel> entry : pushChannels.entrySet()) {
            if (!entry.getKey().equals(senderSessionId)) {
                deliver(entry.getKey(), entry.getValue(), frame);
            }
        }
    }

    /* Envia la notificacion a todas las sesiones en las que ese usuario tiene la sesion iniciada. */
    void sendToUser(int userId, Notification notification) {
        byte[] frame = encode(notification);
        if (frame == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : sessionUsers.entrySet()) {
            if (entry.getValue() == userId) {
                PushChannel channel = pushChannels.get(entry.getKey());
                if (channel != null) {
                    deliver(entry.getKey(), channel, frame);
                }
            }
        }
    }

    private static byte[] encode(Notification notification) {
        try {
            return MessageCodec.encode(notification);
        } catch (IOException e) {
            System.err.println("No se pudo serializar la notificacion: " + e.getMessage());
            return null;
        }
    }

    private void deliver(String sessionId, PushChannel channel, byte[] frame) {
        try {
            channel.send(frame);
        } catch (IOException e) {
            // Socket muerto: se da de baja para no volver a escribirle.
            pushChannels.remove(sessionId, channel);
            channel.close();
        }
    }
}