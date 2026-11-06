package Model;

/*
Estado de revision de una reserva (flujo de solicitudes del administrador):

- IN_REVIEW ("En revision"): estado inicial de TODA reserva. Tambien vuelve a
  este estado cualquier reserva que el funcionario edite. Es una solicitud que
  el administrador todavia debe revisar.
- ACCEPTED ("Aceptada"): el administrador aprobo la solicitud y le asigno un
  recurso por cada categoria requerida. Solo estas reservas se muestran de
  forma global en el sistema y ocupan recursos/horarios.
- REJECTED ("Rechazada"): el administrador la rechazo, indicando el motivo.
- CANCELLED ("Cancelada"): el funcionario la cancelo. Ya no se borra del
  sistema; puede editarse para volver a solicitarla.

El nombre de la constante (p. ej. "IN_REVIEW") es lo que se guarda en el XML;
getLabel() es el texto que se muestra al usuario.
*/
public enum State {

    IN_REVIEW("En revisión"),
    ACCEPTED("Aceptada"),
    REJECTED("Rechazada"),
    CANCELLED("Cancelada");

    private final String label;

    State(String label) {
        this.label = label;
    }

    /** Texto del estado tal como se muestra en la interfaz (p. ej. "En revisión"). */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
