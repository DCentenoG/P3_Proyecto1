package Model;

import java.util.ArrayList;
import java.util.Objects;

public class ResourceContainer {
    //Attributes
    private ArrayList<Resource> resources; //Contiene un tipo de recurso especifico
    //End of Attributes

    //Default Builder
    public ResourceContainer() {
        resources = new ArrayList<>();
    }

    //Parameterized Builder
    public ResourceContainer(ArrayList<Resource> resources) {
        this.resources = resources;
    }

    //getter for the full container
    public ArrayList<Resource> getResources() {return resources;}

    //Logic and calculus methods
    public void addResource(int id, String description) {
        resources.add(new Resource(id, description));
    } //PENDIENTE IMPLEMENTAR EXCEPCIONES DE PARAMETROS VALIDOS

    public Resource getResourceById(int id) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == id) {
                return resources.get(i);
            }
        }
        return null; //PENDIENTE RETORNAR UNA EXCEPCION DE NO EXISTIR EL RECURSO BUSCADO Y DE ID VALIDO
    }

    public Resource getResourceByDescription(String description) {
        for (int i = 0; i < resources.size(); i++) {
            if (Objects.equals(resources.get(i).getDescription(), description)) {
                return resources.get(i);
            }
        }
        return null; //PENDIENTE RETORNAR UNA EXCEPCION DE NO EXISTIR EL RECURSO BUSCADO
    }

    public void deleteResourceByIdAndDescription(int id, String description) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == id && resources.get(i).getDescription().equals(description)) {
                resources.remove(i);
            }
        }
    } //PENDIENTE IMPLEMENTAR EXCEPCION DE RECURSO NO ENCONTRADO Y DE PARAMETROS INVALIDOS

    public Resource getResourceByIndex(int index){return resources.get(index);}

}
