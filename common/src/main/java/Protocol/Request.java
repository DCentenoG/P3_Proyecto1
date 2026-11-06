package Protocol;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Una peticion del Frontend al Backend: el nombre de la operacion de
 * {@code Service.IService} que se quiere ejecutar, y sus argumentos.
 * <p>
 * El Proxy del Frontend arma uno de estos por cada llamada que recibe, lo
 * escribe en el socket y espera la {@link Response}. Del otro lado, el
 * {@code ClientHandler} del servidor lee el nombre, busca la operacion en su
 * {@code LocalService} y la invoca con los argumentos.
 *
 * <h2>Por que el nombre del metodo basta como clave</h2>
 * Porque {@code IService} no declara sobrecargas: dos operaciones nunca
 * comparten nombre, asi que el nombre identifica una operacion sin
 * ambiguedad. Esa regla se fijo al definir la interfaz justamente por esto.
 *
 * <h2>Sobre la serializacion</h2>
 * Todo lo que viaja dentro de {@code args} tiene que implementar
 * {@link Serializable}: las entidades del dominio, los DTOs, y los tipos del
 * JDK que ya lo son ({@code String}, {@code Integer}, {@code LocalDate},
 * {@code LocalTime}, {@code ArrayList}...).
 * <p>
 * Cada clase que viaja declara un {@code serialVersionUID} fijo en 1. Sin el,
 * Java calcula uno a partir de la forma de la clase, y basta con agregarle un
 * campo o un metodo para que el valor cambie y el otro extremo rechace el
 * objeto con {@code InvalidClassException}. Como el Frontend y el Backend se
 * compilan y despliegan juntos, fijarlo a mano evita ese problema.
 * <p>
 * <b>Cuidado al escribir en el socket:</b> {@code ObjectOutputStream} cachea
 * por identidad los objetos que ya mando y, la segunda vez, envia solo una
 * referencia. Si no se llama a {@code reset()} despues de cada escritura, el
 * cliente sigue viendo el estado viejo de un objeto que el servidor ya
 * modifico. Hay que llamar a {@code flush()} y a {@code reset()} siempre.
 */
public final class Request implements Serializable {

    /* Version del formato serializado: ver la nota de arriba. */
    private static final long serialVersionUID = 1L;

    private final String method;
    private final Object[] args;

    public Request(String method, Object... args) {
        this.method = method;
        this.args = (args != null) ? args : new Object[0];
    }

    /** Nombre del metodo de {@code IService} a ejecutar. */
    public String getMethod() {
        return method;
    }

    /** Argumentos, en el mismo orden en que los declara el metodo. */
    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "Request{" + method + "/" + args.length + " args " + Arrays.toString(args) + "}";
    }
}
