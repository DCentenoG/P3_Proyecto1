package Service;

import Model.CategoryContainer;
import Model.UserContainer;
import Persistence.SystemData;
import Persistence.SystemXmlRepository;

/*
Fachada de persistencia compartida por toda la aplicacion: es el "linker"
entre los Controladores y la persistencia de datos que describe el diseño del
proyecto. Mantiene en memoria una unica copia de los datos cargados del XML
(SystemData) y es el unico punto por el que se lee y se escribe el archivo.

SessionContext posee una unica instancia de esta clase, y UserService,
ResourceService y ReservationService reciben esa misma instancia por
constructor para garantizar que todos trabajan siempre sobre los mismos
UserContainer/CategoryContainer: si se agrega una categoria, el resto la ve
de inmediato, sin tener que recargar el archivo.
*/

public class Service {

    private final SystemXmlRepository repository;
    private SystemData data;

    public Service(String xmlPath) {
        this.repository = new SystemXmlRepository(xmlPath);
        this.data = new SystemData();
    }

    //Debe llamarse una unica vez al iniciar la aplicacion (lo hace SessionContext)
    public void load() throws ServiceException {
        try {
            data = repository.load();
        } catch (Exception e) {
            throw new ServiceException("No fue posible cargar los datos del archivo XML:\n" + e.getMessage(), e);
        }
    }

    public void save() throws ServiceException {
        try {
            repository.save(data);
        } catch (Exception e) {
            throw new ServiceException("No fue posible guardar los datos en el archivo XML:\n" + e.getMessage(), e);
        }
    }

    public UserContainer getUsers() {
        return data.getUsers();
    }

    public CategoryContainer getCategories() {
        return data.getCategories();
    }

    public SystemData getData() {
        return data;
    }
}
