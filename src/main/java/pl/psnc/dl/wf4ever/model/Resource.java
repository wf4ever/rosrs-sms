package pl.psnc.dl.wf4ever.model;

import java.net.URI;


public class Resource {

    protected URI uri;
    
    public Resource(URI uri){
        this.uri=uri;
    }
    
    public URI getUri(){
        return uri;
    }
    
}
