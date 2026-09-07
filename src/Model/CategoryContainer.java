package Model;

import java.util.ArrayList;

public class CategoryContainer {
    //Attributes
    private ArrayList<ResourceCategory> categories;
    //End of attributes


    //Default Builder
    public CategoryContainer() {
        categories = new ArrayList<>();
    }

    //Parameterized Builder
    public CategoryContainer(ArrayList<ResourceCategory> categories) {
        this.categories = categories;
    }

    //getter for the full container
    public ArrayList<ResourceCategory> getCategories() {
        return categories;
    }

    //Logic and calculus methods
    public void setCategories(ArrayList<ResourceCategory> categories) {
        this.categories = categories;
    }

    public void addCategory(String id, String description) {
        ResourceCategory category = new ResourceCategory(id, description);
        categories.add(category);
    }

    public ResourceCategory getCategorybyDescription(String description) {
        for (ResourceCategory category : categories) {
            if (category.getDescription().equals(description)) {
                return category;
            }
        }
        return null; //PENDIENTE IMPLEMENTAR EXCEPCION DE CATEGORIA NO ENCONTRADA
    }

    public void deleteCategoryByIdAndDescription(String id, String description) {
        for (ResourceCategory category : categories) {
            if (category.getId().equals(id) && category.getDescription().equals(description)) {
                category.deleteAllResources();
                categories.remove(category);
            }
        }
    }
}
