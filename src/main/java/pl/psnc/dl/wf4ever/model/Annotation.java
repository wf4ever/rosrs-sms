package pl.psnc.dl.wf4ever.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class Annotation extends Resource {

    private List<AggreagetedResource> annotated;
    private AnnotationBody body;
    
    public Annotation(URI uri){
        super(uri);
        annotated = new ArrayList<AggreagetedResource>();
    }
    
}
