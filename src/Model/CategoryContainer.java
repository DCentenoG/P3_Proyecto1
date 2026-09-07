package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public List<ResourceCategory> getCategories() {
        return Collections.unmodifiableList(categories);
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
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(id) && categories.get(i).getDescription().equals(description)) {
                categories.get(i).deleteAllResources();
                categories.remove(categories.get(i));
                break;
            }
        }
    }
}
