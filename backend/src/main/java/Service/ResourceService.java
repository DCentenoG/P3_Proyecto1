package Service;

import Model.Resource;
import Model.ResourceCategory;
import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.DuplicateResourceException;
import Model.exceptions.ResourceNotFoundException;

import java.util.List;

public class ResourceService {

    private final Service service;

    public ResourceService(Service service) {
        this.service = service;
    }

    public List<ResourceCategory> listCategories() {
        return service.getCategories().getCategories();
    }

    public ResourceCategory findCategoryByDescription(String description) throws ServiceException {
        try {
            return service.getCategories().getCategorybyDescription(description);
        } catch (CategoryNotFoundException e) {
            throw new ServiceException(
                    "No existe ninguna categoria con la descripcion '" + description + "'.", e);
        }
    }

    public ResourceCategory addCategory(String description) throws ServiceException {
        if (description == null || description.isBlank()) {
            throw new ServiceException("Debe indicar la descripcion de la categoria.");
        }
        String normalized = description.trim();
        try {
            service.getCategories().getCategorybyDescription(normalized);
            throw new ServiceException("Ya existe una categoria con esa descripcion.");
        } catch (CategoryNotFoundException ignored) {
            service.getCategories().addCategory(normalized);
        }
        service.insertCategory(findCategoryByDescription(normalized));
        return findCategoryByDescription(normalized);
    }

    public void updateCategory(String id, String currentDescription, String newDescription) throws ServiceException {
        ResourceCategory category = requireCategory(id, currentDescription);
        if (newDescription == null || newDescription.isBlank()) {
            throw new ServiceException("Debe indicar la nueva descripcion de la categoria.");
        }
        String normalized = newDescription.trim();
        try {
            ResourceCategory duplicate = service.getCategories().getCategorybyDescription(normalized);
            if (!normalized.equalsIgnoreCase(currentDescription) && !category.equals(duplicate)) {
                throw new ServiceException("Ya existe otra categoria con esa descripcion.");
            }
        } catch (CategoryNotFoundException ignored) {
            // La nueva descripcion no esta repetida.
        }

        category.setDescription(normalized);
        service.updateCategoryDescription(category.getId(), normalized);
    }

    public void removeCategory(String id, String description) throws ServiceException {
        ResourceCategory category = requireCategory(id, description);
        if (!category.getResources().isEmpty()) {
            throw new ServiceException(
                    "No se puede eliminar la categoria '" + description + "' porque todavia tiene recursos asociados.");
        }
        // La base no permite borrar una categoria que alguna reserva haya solicitado,
        // aunque esa reserva ya este cancelada o rechazada: las filas de
        // RESERVA_CATEGORIA la referencian. Se consulta antes para dar un mensaje
        // entendible en lugar de dejar que falle la clave foranea.
        int reservations = service.countReservationsRequiring(id);
        if (reservations > 0) {
            throw new ServiceException("No se puede eliminar la categoria '" + description
                    + "' porque " + reservations + (reservations == 1 ? " reserva la solicito."
                    : " reservas la solicitaron."));
        }
        try {
            service.getCategories().deleteCategoryByIdAndDescription(id, description);
        } catch (CategoryNotFoundException e) {
            throw new ServiceException("No fue posible eliminar la categoria indicada.", e);
        }
        service.deleteCategory(id);
    }

    private ResourceCategory requireCategory(String id, String description) throws ServiceException {
        ResourceCategory category = findCategoryByDescription(description);
        if (!category.getId().equals(id)) {
            throw new ServiceException("No existe ninguna categoria registrada con esos datos.");
        }
        return category;
    }

    public List<Resource> listResourcesByCategory(String categoryDescription) throws ServiceException {
        return requireCategory(categoryDescription).getResources();
    }

    public Resource addResource(String categoryDescription, String idText, String description)
            throws ServiceException {
        int id = parseResourceId(idText);
        ResourceCategory category = requireCategory(categoryDescription);
        if (description == null || description.isBlank()) {
            throw new ServiceException("Debe indicar la descripcion del recurso.");
        }
        try {
            category.getResourceById(id);
            throw new ServiceException("Ya existe un recurso con el id '" + id + "' en esa categoria.");
        } catch (ResourceNotFoundException ignored) {
            // El id esta disponible.
        }

        try {
            category.addResource(id, description.trim());
        } catch (DuplicateResourceException e) {
            throw new ServiceException("Ya existe un recurso con el id '" + id + "' en esa categoria.", e);
        }
        service.insertResource(category.getId(), id, description.trim());
        return getResource(category, id);
    }

    public Resource addResource(String categoryDescription, int id, String description) throws ServiceException {
        return addResource(categoryDescription, String.valueOf(id), description);
    }

    public void updateResource(String categoryDescription, String idText, String newDescription)
            throws ServiceException {
        updateResource(categoryDescription, parseResourceId(idText), newDescription);
    }

    public void updateResource(String categoryDescription, int id, String newDescription) throws ServiceException {
        Resource resource = requireResource(categoryDescription, id);
        if (newDescription == null || newDescription.isBlank()) {
            throw new ServiceException("Debe indicar la nueva descripcion del recurso.");
        }
        resource.setDescription(newDescription.trim());
        service.saveMovedResource(resource.getCategoryId(), resource.getId(),
                resource.getCategoryId(), resource.getId(), resource.getDescription());
    }

    public void removeResource(String categoryDescription, int id, String description) throws ServiceException {
        ResourceCategory category = requireCategory(categoryDescription);
        // Este metodo no verificaba si el recurso estaba asignado a alguna reserva.
        // La clave foranea de RESERVA_RECURSO lo impide, asi que se consulta antes
        // para dar un mensaje entendible en vez de un error de Oracle.
        int reservations = service.countReservationsUsing(category.getId(), id);
        if (reservations > 0) {
            throw new ServiceException("No se puede eliminar el recurso '" + description
                    + "' porque esta asignado a " + reservations
                    + (reservations == 1 ? " reserva." : " reservas."));
        }
        try {
            category.deleteResourceByIdAndDescription(id, description);
        } catch (ResourceNotFoundException e) {
            throw new ServiceException(
                    "No existe el recurso indicado en la categoria seleccionada.", e);
        }
        service.deleteResource(category.getId(), id);
    }

    /*
    Mueve un recurso de una categoria a otra. Se identifica por (categoria origen,
    id del recurso) y la categoria destino por su id, en vez de recibir los objetos:
    al pasar a la arquitectura distribuida, el Frontend solo puede mandar
    identificadores, porque los objetos que tiene son copias.
    */
    public void transferResource(String sourceCategoryId, int resourceId, String destinationCategoryId)
            throws ServiceException {
        ResourceCategory source = requireCategoryById(sourceCategoryId);
        ResourceCategory destination = requireCategoryById(destinationCategoryId);
        Resource resource = getResource(source, resourceId);
        if (source.equals(destination)) {
            return;
        }

        try {
            destination.getResourceById(resource.getId());
            throw new ServiceException(
                    "Ya existe un recurso con el id '" + resource.getId() + "' en la categoria de destino.");
        } catch (ResourceNotFoundException ignored) {
            // El id esta disponible en el destino.
        }

        try {
            destination.addResource(resource.getId(), resource.getDescription());
        } catch (DuplicateResourceException e) {
            throw new ServiceException("No fue posible agregar el recurso a la categoria de destino.", e);
        }

        Resource movedResource;
        try {
            movedResource = destination.getResourceById(resource.getId());
        } catch (ResourceNotFoundException e) {
            throw new ServiceException("No fue posible recuperar el recurso transferido.", e);
        }

        replaceReservationReferences(resource, movedResource);
        try {
            source.deleteResourceByIdAndDescription(resource.getId(), resource.getDescription());
        } catch (ResourceNotFoundException e) {
            replaceReservationReferences(movedResource, resource);
            removeAddedResource(destination, movedResource);
            throw new ServiceException("No fue posible eliminar el recurso de su categoria original.", e);
        }
        // La identidad de un recurso es la pareja (categoria, id): al cambiar de
        // categoria hay que reapuntar las filas de RESERVA_RECURSO que lo tenian
        // asignado. Por eso se le pasa la identidad anterior y la nueva.
        service.saveMovedResource(sourceCategoryId, resourceId,
                destinationCategoryId, movedResource.getId(), movedResource.getDescription());
    }

    /*
    Edita un recurso y, si cambio de categoria, lo mueve. Igual que
    transferResource, identifica por ids y no por objetos.
    */
    public void updateResource(String sourceCategoryId, int resourceId, String destinationCategoryId,
                               String idText, String newDescription) throws ServiceException {
        ResourceCategory source = requireCategoryById(sourceCategoryId);
        ResourceCategory destination = requireCategoryById(destinationCategoryId);
        Resource resource = getResource(source, resourceId);

        int newId = parseResourceId(idText);
        if (newDescription == null || newDescription.isBlank()) {
            throw new ServiceException("Debe indicar la descripcion del recurso.");
        }

        if (!source.equals(destination)) {
            if (newId == resource.getId() && newDescription.trim().equals(resource.getDescription())) {
                transferResource(sourceCategoryId, resourceId, destinationCategoryId);
                return;
            }
            try {
                destination.getResourceById(newId);
                throw new ServiceException(
                        "Ya existe un recurso con el id '" + newId + "' en la categoria de destino.");
            } catch (ResourceNotFoundException ignored) {
                // El id esta disponible.
            }
            try {
                destination.addResource(newId, newDescription.trim());
            } catch (DuplicateResourceException e) {
                throw new ServiceException("No fue posible agregar el recurso a la categoria de destino.", e);
            }
            Resource moved = getResource(destination, newId);
            replaceReservationReferences(resource, moved);
            try {
                source.deleteResourceByIdAndDescription(resource.getId(), resource.getDescription());
            } catch (ResourceNotFoundException e) {
                replaceReservationReferences(moved, resource);
                removeAddedResource(destination, moved);
                throw new ServiceException("No fue posible eliminar el recurso original.", e);
            }
        } else {
            if (newId != resource.getId()) {
                try {
                    destination.getResourceById(newId);
                    throw new ServiceException(
                            "Ya existe un recurso con el id '" + newId + "' en esa categoria.");
                } catch (ResourceNotFoundException ignored) {
                    // El nuevo id esta disponible.
                }
                resource.setId(newId);
            }
            resource.setDescription(newDescription.trim());
        }
        // Identidad anterior: (sourceCategoryId, resourceId), que son los parametros
        // con los que se localizo el recurso, antes de cualquier mutacion. Identidad
        // nueva: (destinationCategoryId, newId) -- y cuando no cambio de categoria,
        // destinationCategoryId es igual a sourceCategoryId.
        service.saveMovedResource(sourceCategoryId, resourceId,
                destinationCategoryId, newId, newDescription.trim());
    }

    private ResourceCategory requireCategory(String categoryDescription) throws ServiceException {
        return findCategoryByDescription(categoryDescription);
    }

    /*
    Categoria con ese id. Es la forma de resolver una categoria cuando el Frontend
    solo manda identificadores; findCategoryByDescription sigue sirviendo para los
    formularios, que trabajan con lo que el usuario ve.
    */
    private ResourceCategory requireCategoryById(String categoryId) throws ServiceException {
        if (categoryId == null || categoryId.isBlank()) {
            throw new ServiceException("Debe indicar la categoria.");
        }
        for (ResourceCategory category : service.getCategories().getCategories()) {
            if (categoryId.equals(category.getId())) {
                return category;
            }
        }
        throw new ServiceException("No existe ninguna categoria con el id '" + categoryId + "'.");
    }

    private Resource requireResource(String categoryDescription, int id) throws ServiceException {
        ResourceCategory category = requireCategory(categoryDescription);
        return getResource(category, id);
    }

    private Resource getResource(ResourceCategory category, int id) throws ServiceException {
        try {
            return category.getResourceById(id);
        } catch (ResourceNotFoundException e) {
            throw new ServiceException(
                    "No existe ningun recurso con el id '" + id + "' en esa categoria.", e);
        }
    }

    private int parseResourceId(String idText) throws ServiceException {
        if (idText == null || idText.isBlank()) {
            throw new ServiceException("Debe indicar el id numerico del recurso.");
        }
        try {
            int id = Integer.parseInt(idText.trim());
            if (id < 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ServiceException("El id del recurso debe ser un numero entero valido.", e);
        }
    }

    private void replaceReservationReferences(Resource original, Resource replacement) {
        service.getUsers().getListOfEmployees().forEach(employee ->
                employee.getReservations().forEach(reservation -> {
                    for (int i = 0; i < reservation.getAssignedResources().size(); i++) {
                        if (reservation.getAssignedResources().get(i) == original) {
                            reservation.getAssignedResources().set(i, replacement);
                        }
                    }
                }));
    }

    private void removeAddedResource(ResourceCategory category, Resource resource) throws ServiceException {
        try {
            category.deleteResourceByIdAndDescription(resource.getId(), resource.getDescription());
        } catch (ResourceNotFoundException rollbackFailure) {
            throw new ServiceException("No fue posible revertir la transferencia del recurso.", rollbackFailure);
        }
    }
}
