package Persistence;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Apoyo comun de las pruebas de integracion contra Oracle.
 *
 * <h2>Por que las pruebas se OMITEN en vez de fallar</h2>
 * Estas pruebas necesitan una base de datos viva, y {@code db/database.properties}
 * no esta en el repositorio porque contiene la clave. En una maquina recien
 * clonada no existe, y sin este cuidado un {@code mvn verify} fallaria y daria la
 * impresion de que el proyecto esta roto, cuando lo unico que falta es configurar
 * la conexion.
 * <p>
 * Por eso cada prueba empieza con {@link #requireDatabase()}: si la base no
 * responde, JUnit la reporta como <b>omitida</b> y dice por que. Donde si hay
 * Oracle configurado, corren de verdad.
 *
 * <h2>Por que nunca se hace commit</h2>
 * {@link #inRollback(DbWork)} abre una conexion, deja que la prueba escriba y
 * lea lo que necesite, y al terminar hace {@code rollback()} <b>siempre</b>, haya
 * salido bien o mal. Dentro de la misma transaccion lo escrito ya es visible, asi
 * que la prueba puede verificar la lectura; al cerrarse, la base queda
 * exactamente como estaba.
 * <p>
 * La alternativa habitual (escribir con una marca y borrar al final) deja basura
 * cuando una prueba truena a mitad de camino: este proyecto ya vivio ese problema
 * durante la etapa de base de datos, con recursos de prueba que quedaron sin
 * borrar. Con rollback eso no puede pasar.
 * <p>
 * El costo de esta decision es que lo que se prueba son las DAOs y no
 * {@code Service.Service}, porque Service maneja su propia transaccion y hace
 * commit adentro. Es un canje consciente: se prefiere no arriesgar los datos.
 */
final class OracleFixture {

    /**
     * Ids altos a proposito, para que nunca choquen con datos reales. Igual se
     * revierten, pero si alguien mira la base mientras corre una prueba, se ve
     * de inmediato que son de prueba.
     */
    static final int USER_ID = 900001;
    static final int RESERVATION_ID = 900001;
    static final String CATEGORY_ID = "CAT-900001";

    private static ConnectionManager connections;
    private static String unavailableReason;
    private static boolean checked;

    private OracleFixture() {
    }

    /** Trabajo de una prueba sobre una conexion ya abierta. */
    interface DbWork {
        void run(Connection connection) throws Exception;
    }

    /**
     * Omite la prueba, con el motivo a la vista, si no hay una base de datos
     * configurada y alcanzable.
     */
    static void requireDatabase() {
        if (!checked) {
            checked = true;
            try {
                ConnectionManager candidate = ConnectionManager.fromProperties();
                try (Connection probe = candidate.open()) {
                    probe.rollback();
                }
                connections = candidate;
            } catch (IllegalStateException e) {
                unavailableReason = "sin configuracion de base de datos: " + firstLine(e.getMessage());
            } catch (SQLException e) {
                unavailableReason = "la base de datos no responde: " + firstLine(e.getMessage());
            }
        }
        assumeTrue(connections != null, unavailableReason);
    }

    /** Conexiones a la base configurada. Solo valido despues de {@link #requireDatabase()}. */
    static ConnectionManager connections() {
        return connections;
    }

    /**
     * Ejecuta el trabajo en una transaccion que SIEMPRE se revierte, de modo que
     * la prueba no deja ni un registro en la base.
     */
    static void inRollback(DbWork work) throws Exception {
        try (Connection connection = connections.open()) {
            try {
                work.run(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "motivo desconocido";
        }
        int breakAt = message.indexOf('\n');
        return breakAt < 0 ? message : message.substring(0, breakAt);
    }
}
