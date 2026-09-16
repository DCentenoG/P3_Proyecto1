package Model;

import Model.exceptions.DuplicateResourceException;
import Model.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ResourceCategory {
    //Attributes
    String id;
    String description;
    private ArrayList<Resource> resources; //Contiene un tipo de recurso especifico
    //End of Attributes

    //Default Builder
    public ResourceCategory() {
        resources = new ArrayList<>();
        id = null;
        description = null;
    }

    //Parameterized Builder
    public ResourceCategory(String id, String description) {
        resources = new ArrayList<>();
        this.id = id;
        this.description = description;
    }

    //basic getters
    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    //setters
    public void setId(String id) {this.id = id;}
    public void setDescription(String description) {this.description = description;}
    public void setResources(ArrayList<Resource> resources) {this.resources = resources;}

    //getter for the full container
    public List<Resource> getResources() {return Collections.unmodifiableList(resources);}
    public int getSize() { return resources.size();}

    //Logic and calculus methods
    public void addResource(int id, String description) throws DuplicateResourceException {
        for (Resource resource : resources) {
            if (resource.getId() == id) {
                throw new DuplicateResourceException(
                        "Ya existe un recurso con el id '" + id + "' en esta categoria.");
            }
        }
        resources.add(new Resource(id, this, description));
    }

    public Resource getResourceById(int id) throws ResourceNotFoundException {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == id) {
                return resources.get(i);
            }
        }
        throw new ResourceNotFoundException(
                "No existe ningun recurso con el id '" + id + "' en esta categoria.");
    }

    public Resource getResourceByDescription(String description) throws ResourceNotFoundException {
        for (int i = 0; i < resources.size(); i++) {
            if (Objects.equals(resources.get(i).getDescription(), description)) {
                return resources.get(i);
            }
        }
        throw new ResourceNotFoundException(
                "No existe ningun recurso con la descripcion '" + description + "' en esta categoria.");
    }

    public void deleteResourceByIdAndDescription(int id, String description) throws ResourceNotFoundException {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == id && resources.get(i).getDescription().equals(description)) {
                resources.remove(i);
                return;
            }
        }
        throw new ResourceNotFoundException(
                "No existe el recurso indicado en esta categoria.");
    }

    public void deleteAllResources() {
        resources.clear();
    }

    public Resource getResourceByIndex(int index){return resources.get(index);}

}
