package Model;

public class Resource {
    //Attributes
    private int id;
    //private ResourceContainer ResourceContainerReference
    private String description;
    //End of attributes

    //Default Builder
    public Resource(){}

    //Parameterized builder
    public Resource(int id, /*ResourceContainer ResourceContainerReference*/ String description) {
        this.id = id;
        //this.ResourceContainerReference = ResourceContainerReference;
        this.description = description;
    }

    //getters
    public int getId() {return id;}
    public String getDescription() {return description;}
    //public ResourceContainer getResourceContainerReference {return ResourceContainerReference;}

    //setters
    public void setId(int id) {this.id = id;}
    public void setDescription(String description) {this.description = description;}
    /*public void setResourceContainerReference(ResourceContainer ResourceContainerReference){
        this.ResourceContainerReference = ResourceContainerReference;
    }*/


}
