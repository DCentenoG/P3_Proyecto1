package Controller;

import Model.User;
import Service.IService;

/**
 * Estado de la sesión en el Frontend: el usuario que inició sesión y el
 * {@link IService} contra el que trabajan todos los controladores. Se
 * construye una única vez desde {@code Main} y se comparte entre todos los
 * listeners de eventos.
 * <p>
 * Ya no contiene datos ni lógica. Antes guardaba los contenedores de usuarios
 * y categorías en memoria, cargaba el XML y sembraba los registros de prueba;
 * todo eso vive ahora en el Backend, detrás de {@link IService}. Lo único que
 * esta clase conserva es lo que de verdad es estado de la sesión: quién está
 * autenticado.
 * <p>
 * <b>El punto importante:</b> esta clase recibe el {@link IService} ya
 * construido y no sabe cuál implementación le tocó. Hoy es
 * {@code LocalService}, que ejecuta todo en el mismo proceso; cuando exista la
 * capa de sockets será el Proxy, que manda cada llamada por la red. Ningún
 * controlador se entera de la diferencia, y ese es justamente el patrón Proxy
 * que pide el enunciado.
 */
public final class SessionContext {

    private final IService service;
    private User currentUser;

    public SessionContext(IService service) {
        this.service = service;
    }

    private final AlertCenter alertCenter = new AlertCenter();

    /**
     * Única puerta de entrada a las operaciones del sistema. Todos los
     * controladores pasan por aquí: no hay ningún otro camino desde la capa de
     * Presentación hacia los datos.
     */
    public IService getService() {
        return service;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public AlertCenter getAlertCenter() {
        return alertCenter;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
