package Service;

import Model.Resource;
import Model.ResourceCategory;

import java.util.List;

/*
Servicio de dominio de categorias de recursos y recursos (funcionalidades 4 y
5 del enunciado). Ambas entidades viven juntas porque ResourceCategory ya
contiene y administra sus propios recursos en el Model; este servicio solo
agrega las validaciones minimas antes de delegar y guarda el XML despues de
cada cambio.
*/
public class ResourceService {

    private final Service service;

    public ResourceService(Service service) {
        this.service = service;
    }

    //--- Categorias ---

    public List<ResourceCategory> listCategories() {
        return service.getCategories().getCategories();
    }

    public ResourceCategory findCategoryByDescription(String description) {
        return service.getCategories().getCategorybyDescription(description);
    }

    public ResourceCategory addCategory(String description) throws ServiceException {
        if (description == null || description.isBlank()) {
            throw new ServiceException("Debe indicar la descripcion de la categoria.");
        }
        if (findCategoryByDescription(description.trim()) != null) {
            throw new ServiceException("Ya existe una categoria con esa descripcion.");
        }

        //addCategory genera el id consecutivo "CAT-00000N"
        service.getCategories().addCategory(description.trim());
        service.save();
        return findCategoryByDescription(description.trim());
    }

    public void updateCategory(String id, String currentDescription, String newDescription) throws ServiceException {
        ResourceCategory category = requireCategory(id, currentDescription);
        if (newDescription == null || newDescription.isBlank()) {
            throw new ServiceException("Debe indicar la nueva descripcion de la categoria.");
        }
        if (findCategoryByDescription(newDescription.trim()) != null
                && !newDescription.trim().equalsIgnoreCase(currentDescription)) {
            throw new ServiceException("Ya existe otra categoria con esa descripcion.");
        }

        category.setDescription(newDescription.trim());
        service.save();
    }

    public void removeCategory(String id, String description) throws ServiceException {
        ResourceCategory category = requireCategory(id, description);
        if (!category.getResources().isEmpty()) {
            throw new ServiceException(
                    "No se puede eliminar la categoria '" + description + "' porque todavia tiene recursos asociados.");
        }

        service.getCategories().deleteCategoryByIdAndDescription(id, description);
        service.save();
    }

    private ResourceCategory requireCategory(String id, String description) throws ServiceException {
        ResourceCategory category = findCategoryByDescription(description);
        if (category == null || !category.getId().equals(id)) {
            throw new ServiceException("No existe ninguna categoria registrada con esos datos.");
        }
        return category;
    }

    //--- Recursos ---

    public List<Resource> listResourcesByCategory(String categoryDescription) throws ServiceException {
        return requireCategory(categoryDescription).getResources();
    }

    public Resource addResource(String categoryDescription, int id, String description) throws ServiceException {
        ResourceCategory category = requireCategory(categoryDescription);
        if (description == null || description.isBlank()) {
            throw new ServiceException("Debe indicar la descripcion del recurso.");
        }
        if (category.getResourceById(id) != null) {
            throw new ServiceException("Ya existe un recurso con el id '" + id + "' en esa categoria.");
        }

        category.addResource(id, description.trim());
        service.save();
        return category.getResourceById(id);
    }

    public void updateResource(String categoryDescription, int id, String newDescription) throws ServiceException {
        Resource resource = requireResource(categoryDescription, id);
        if (newDescription == null || newDescription.isBlank()) {
            throw new ServiceException("Debe indicar la nueva descripcion del recurso.");
        }

        resource.setDescription(newDescription.trim());
        service.save();
    }

    public void removeResource(String categoryDescription, int id, String description) throws ServiceException {
        ResourceCategory category = requireCategory(categoryDescription);
        if (category.getResourceById(id) == null) {
            throw new ServiceException("No existe ningun recurso con ese id en la categoria seleccionada.");
        }

        category.deleteResourceByIdAndDescription(id, description);
        service.save();
    }

    private ResourceCategory requireCategory(String categoryDescription) throws ServiceException {
        ResourceCategory category = findCategoryByDescription(categoryDescription);
        if (category == null) {
            throw new ServiceException("No existe ninguna categoria con la descripcion '" + categoryDescription + "'.");
        }
        return category;
    }

    private Resource requireResource(String categoryDescription, int id) throws ServiceException {
        ResourceCategory category = requireCategory(categoryDescription);
        Resource resource = category.getResourceById(id);
        if (resource == null) {
            throw new ServiceException("No existe ningun recurso con el id '" + id + "' en esa categoria.");
        }
        return resource;
    }
}
