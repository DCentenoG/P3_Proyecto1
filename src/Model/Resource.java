package Model;

public class Resource {
    //Attributes
    private int id;
    private ResourceCategory ResourceCategoryReference;
    private String description;
    //End of attributes

    //Default Builder
    public Resource(){
        id = 0;
        ResourceCategoryReference = null;
        description = null;
    }

    //Parameterized builder
    public Resource(int id, ResourceCategory ResourceCategoryReference, String description) {
        this.id = id;
        this.ResourceCategoryReference = ResourceCategoryReference;
        this.description = description;
    }

    //getters
    public int getId() {return id;}
    public String getDescription() {return description;}
    public ResourceCategory getResourceContainerReference() {return ResourceCategoryReference;}

    //setters
    public void setId(int id) {this.id = id;}
    public void setDescription(String description) {this.description = description;}
    public void setResourceCategoryReference(ResourceCategory ResourceCategoryReference){
        this.ResourceCategoryReference = ResourceCategoryReference;
    }


}
