package pl.psnc.dl.wf4ever.model.AO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import pl.psnc.dl.wf4ever.model.RO.Resource;
import pl.psnc.dl.wf4ever.vocabulary.AO;
import pl.psnc.dl.wf4ever.vocabulary.RO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Statement;

public class Annotation extends Resource {

    private List<Resource> annotated;
    private AnnotationBody body;


    public Annotation(URI uri) {
        super(uri);
        annotated = new ArrayList<Resource>();
        body = null;
    }


    public Annotation(URI uri, OntModel model) {
        this(uri);
        annotated = new ArrayList<Resource>();
        fillUp(model);
    }


    public void fillUp(OntModel model) {
        Individual source = model.getIndividual(uri.toString());
        for (Statement statement : source.listProperties(RO.annotatesAggregatedResource).toList()) {
            try {
                annotated.add(new Resource(new URI(statement.getObject().asResource().getURI())));
            } catch (URISyntaxException e) {
            }
        }
        try {
            body = new AnnotationBody(new URI(source.getPropertyValue(AO.body).asResource().getURI()));
        } catch (URISyntaxException e) {
            body = null;
        }
    }
    
    public List<Resource> getAnnotated() {
        return annotated;
    }
    
    public List<URI> getAnnotatedToURIList() {
        List<URI> result = new ArrayList<URI>();
        for (Resource r : annotated) {
            result.add(r.getUri());
        }
        return result;
    }
    
    public AnnotationBody getBody() {
        return body;
    }

}
