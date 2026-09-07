package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryContainer {
    //Attributes
    private ArrayList<ResourceCategory> categories;
    int nextId;
    //End of attributes


    //Default Builder
    public CategoryContainer() {
        categories = new ArrayList<>();
        nextId = 1;
    }

    //Parameterized Builder
    public CategoryContainer(ArrayList<ResourceCategory> categories) {
        this.categories = categories;
        nextId = categories.size();
    }

    //getter and setter for nextId
    public int getNextId() {return nextId;}
    public void setNextId(int nextId) {this.nextId = nextId;}

    //getter for the full container
    public List<ResourceCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    //Logic and calculus methods
    public void setCategories(ArrayList<ResourceCategory> categories) {
        this.categories = categories;
    }

    public void addCategory(String description) { // ya no recibe id como parámetro
        String generatedId = "CAT-" + String.format("%06d", nextId);
        ResourceCategory category = new ResourceCategory(generatedId, description);
        categories.add(category);
        nextId++;
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
