package Model;

import java.io.Serializable;
import java.util.Objects;

/*
La identidad de un recurso es la pareja (categoria, id): el id solo es unico
DENTRO de su categoria, asi que el recurso 1 de "Salas" y el recurso 1 de
"Proyectores" son recursos distintos. La descripcion NO forma parte de la
identidad: es un dato que se puede editar sin que el recurso deje de ser el
mismo.
*/
public class Resource implements Serializable {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    //Attributes
    private int id;
    private ResourceCategory resourceCategoryReference;
    private String description;
    //End of attributes

    //Default Builder
    public Resource(){
        id = 0;
        resourceCategoryReference = null;
        description = null;
    }

    //Parameterized builder
    public Resource(int id, ResourceCategory ResourceCategoryReference, String description) {
        this.id = id;
        this.resourceCategoryReference = ResourceCategoryReference;
        this.description = description;
    }

    //getters
    public int getId() {return id;}
    public String getDescription() {return description;}
    public ResourceCategory getResourceCategoryReference() {return resourceCategoryReference;}

    //setters
    public void setId(int id) {this.id = id;}
    public void setDescription(String description) {this.description = description;}
    public void setResourceCategoryReference(ResourceCategory ResourceCategoryReference){
        this.resourceCategoryReference = ResourceCategoryReference;
    }

    /* Id de la categoria a la que pertenece, o null si todavia no tiene categoria. */
    public String getCategoryId() {
        return resourceCategoryReference == null ? null : resourceCategoryReference.getId();
    }

    //logic and calculus methods

    /*
    Dos recursos son el mismo si coinciden en categoria e id. Antes se comparaba
    (id, descripcion) sin la categoria, lo que hacia que el recurso 1 de una
    categoria y el 1 de otra se consideraran el mismo: al verificar disponibilidad,
    reservar uno bloqueaba al otro.
    */
    public boolean isSameResource(Resource other) {
        return equals(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource otherResource)) {
            return false;
        }
        return id == otherResource.id
                && Objects.equals(getCategoryId(), otherResource.getCategoryId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getCategoryId());
    }

    @Override
    public String toString() {
        return "Resource{" + getCategoryId() + "/" + id + " " + description + "}";
    }
}
