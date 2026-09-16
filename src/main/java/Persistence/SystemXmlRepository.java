package Persistence;

import Model.Admin;
import Model.CategoryContainer;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import Model.UserContainer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/*
Repositorio que lee y escribe TODO el estado del sistema (usuarios y
categorias de recursos) en un unico archivo XML, usando DOM directamente
(DocumentBuilder para leer, Document+Transformer para escribir).

Estructura del XML generado:

<system>
    <categories>
        <category id="CAT-000001" description="Sala para 10 personas">
            <resource id="1" description="Sala 1 primer piso"/>
        </category>
    </categories>
    <users>
        <user type="ADMIN" id="1" password="admin"/>
        <user type="EMPLOYEE" id="2" password="2" name="Juan Perez" phoneNumber="88881111">
            <reservations>
                <reservation activity="Reunion" date="2026-09-20" startTime="09:00" endTime="10:00">
                    <assignedResource categoryId="CAT-000001" resourceId="1"/>
                </reservation>
            </reservations>
        </user>
    </users>
</system>

Las categorias siempre se cargan antes que los usuarios porque las reservas de
los funcionarios solo guardan una referencia (categoryId + resourceId) al
recurso real: ese recurso debe existir ya en el CategoryContainer para poder
enlazarlo de vuelta a la misma instancia usada por las categorias (necesario
para que la logica de disponibilidad de Reservation.addResource siga
funcionando igual que con datos recien creados en memoria).
*/
public class SystemXmlRepository {

    private final File file;

    public SystemXmlRepository(String xmlPath) {
        this.file = new File(xmlPath);
    }

    public SystemData load() throws Exception {
        if (!file.exists()) {
            return new SystemData();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        CategoryContainer categories = loadCategories(doc);
        UserContainer users = loadUsers(doc, categories);

        return new SystemData(users, categories);
    }

    public void save(SystemData data) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("system");
        doc.appendChild(root);

        saveCategories(doc, root, data.getCategories());
        saveUsers(doc, root, data.getUsers());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    //--- Carga de categorias y recursos ---

    private CategoryContainer loadCategories(Document doc) {
        ArrayList<ResourceCategory> list = new ArrayList<>();
        int highestNumber = 0;

        NodeList categoriesXml = doc.getElementsByTagName("category");
        for (int i = 0; i < categoriesXml.getLength(); i++) {
            Element categoryEl = (Element) categoriesXml.item(i);

            String id = categoryEl.getAttribute("id");
            String description = categoryEl.getAttribute("description");
            ResourceCategory category = new ResourceCategory(id, description);

            NodeList resourcesXml = categoryEl.getElementsByTagName("resource");
            for (int j = 0; j < resourcesXml.getLength(); j++) {
                Element resourceEl = (Element) resourcesXml.item(j);
                int resourceId = Integer.parseInt(resourceEl.getAttribute("id"));
                String resourceDescription = resourceEl.getAttribute("description");
                category.addResource(resourceId, resourceDescription);
            }

            list.add(category);
            highestNumber = Math.max(highestNumber, extractNumber(id));
        }

        CategoryContainer categories = new CategoryContainer(list);
        //El constructor de CategoryContainer inicializa nextId con el tamano de la lista;
        //lo recalculamos aqui a partir del mayor consecutivo usado en los ids "CAT-00000N"
        //para no repetir ids si se borraron categorias intermedias.
        categories.setNextId(highestNumber + 1);
        return categories;
    }

    private int extractNumber(String categoryId) {
        if (categoryId == null) {
            return 0;
        }
        String digitsOnly = categoryId.replaceAll("\\D+", "");
        return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
    }

    //--- Carga de usuarios y reservas ---

    private UserContainer loadUsers(Document doc, CategoryContainer categories) {
        ArrayList<User> list = new ArrayList<>();
        int highestId = 0;

        NodeList usersXml = doc.getElementsByTagName("user");
        for (int i = 0; i < usersXml.getLength(); i++) {
            Element userEl = (Element) usersXml.item(i);

            String type = userEl.getAttribute("type");
            int id = Integer.parseInt(userEl.getAttribute("id"));
            String password = userEl.getAttribute("password");

            User user;
            if ("ADMIN".equals(type)) {
                user = new Admin(id, password);
            } else {
                String name = userEl.getAttribute("name");
                int phoneNumber = Integer.parseInt(userEl.getAttribute("phoneNumber"));
                Employee employee = new Employee(name, phoneNumber, id, password);
                employee.setReservations(loadReservations(userEl, categories));
                user = employee;
            }

            list.add(user);
            highestId = Math.max(highestId, id);
        }

        UserContainer users = new UserContainer(list);
        //Igual que con las categorias: el nextId real depende del mayor id usado, no de
        //cuantos usuarios hay (para que no se dupliquen ids si se elimino algun usuario).
        users.setNextId(highestId + 1);
        return users;
    }

    private ArrayList<Reservation> loadReservations(Element userEl, CategoryContainer categories) {
        ArrayList<Reservation> reservations = new ArrayList<>();

        //getElementsByTagName sobre un Element (no sobre el Document) solo busca
        //entre los descendientes de ESE elemento, por lo que aqui solo trae las
        //reservas del usuario actual.
        NodeList reservationsXml = userEl.getElementsByTagName("reservation");
        for (int i = 0; i < reservationsXml.getLength(); i++) {
            Element reservationEl = (Element) reservationsXml.item(i);

            String activity = reservationEl.getAttribute("activity");
            LocalDate date = LocalDate.parse(reservationEl.getAttribute("date"));
            LocalTime startTime = LocalTime.parse(reservationEl.getAttribute("startTime"));
            LocalTime endTime = LocalTime.parse(reservationEl.getAttribute("endTime"));

            Reservation reservation = new Reservation(activity, date, startTime, endTime);
            reservation.setAssignedResources(loadAssignedResources(reservationEl, categories));

            reservations.add(reservation);
        }
        return reservations;
    }

    private ArrayList<Resource> loadAssignedResources(Element reservationEl, CategoryContainer categories) {
        ArrayList<Resource> assigned = new ArrayList<>();

        NodeList assignedResourcesXml = reservationEl.getElementsByTagName("assignedResource");
        for (int i = 0; i < assignedResourcesXml.getLength(); i++) {
            Element resourceEl = (Element) assignedResourcesXml.item(i);
            String categoryId = resourceEl.getAttribute("categoryId");
            int resourceId = Integer.parseInt(resourceEl.getAttribute("resourceId"));

            ResourceCategory category = findCategoryById(categories, categoryId);
            if (category != null) {
                Resource resource = category.getResourceById(resourceId);
                if (resource != null) {
                    //Se agrega la MISMA instancia que vive dentro de la categoria, no una copia,
                    //para que la reserva y la categoria sigan compartiendo el mismo objeto Resource.
                    assigned.add(resource);
                }
            }
        }
        return assigned;
    }

    private ResourceCategory findCategoryById(CategoryContainer categories, String id) {
        for (ResourceCategory category : categories.getCategories()) {
            if (category.getId().equals(id)) {
                return category;
            }
        }
        return null;
    }

    //--- Escritura de categorias y recursos ---

    private void saveCategories(Document doc, Element root, CategoryContainer categories) {
        Element categoriesEl = doc.createElement("categories");
        root.appendChild(categoriesEl);

        for (ResourceCategory category : categories.getCategories()) {
            Element categoryEl = doc.createElement("category");
            categoryEl.setAttribute("id", category.getId());
            categoryEl.setAttribute("description", category.getDescription());

            for (Resource resource : category.getResources()) {
                Element resourceEl = doc.createElement("resource");
                resourceEl.setAttribute("id", String.valueOf(resource.getId()));
                resourceEl.setAttribute("description", resource.getDescription());
                categoryEl.appendChild(resourceEl);
            }

            categoriesEl.appendChild(categoryEl);
        }
    }

    //--- Escritura de usuarios y reservas ---

    private void saveUsers(Document doc, Element root, UserContainer users) {
        Element usersEl = doc.createElement("users");
        root.appendChild(usersEl);

        for (User user : users.getUsers()) {
            Element userEl = doc.createElement("user");
            userEl.setAttribute("id", String.valueOf(user.getId()));
            userEl.setAttribute("password", user.getPassword());

            if (user instanceof Employee employee) {
                userEl.setAttribute("type", "EMPLOYEE");
                userEl.setAttribute("name", employee.getName());
                userEl.setAttribute("phoneNumber", String.valueOf(employee.getPhoneNumber()));
                saveReservations(doc, userEl, employee.getReservations());
            } else {
                userEl.setAttribute("type", "ADMIN");
            }

            usersEl.appendChild(userEl);
        }
    }

    private void saveReservations(Document doc, Element userEl, List<Reservation> reservations) {
        Element reservationsEl = doc.createElement("reservations");
        userEl.appendChild(reservationsEl);

        for (Reservation reservation : reservations) {
            Element reservationEl = doc.createElement("reservation");
            reservationEl.setAttribute("activity", reservation.getActivity());
            reservationEl.setAttribute("date", reservation.getDate().toString());
            reservationEl.setAttribute("startTime", reservation.getStartTime().toString());
            reservationEl.setAttribute("endTime", reservation.getEndTime().toString());

            for (Resource resource : reservation.getAssignedResources()) {
                Element resourceEl = doc.createElement("assignedResource");
                resourceEl.setAttribute("categoryId", resource.getResourceCategoryReference().getId());
                resourceEl.setAttribute("resourceId", String.valueOf(resource.getId()));
                reservationEl.appendChild(resourceEl);
            }

            reservationsEl.appendChild(reservationEl);
        }
    }
}
