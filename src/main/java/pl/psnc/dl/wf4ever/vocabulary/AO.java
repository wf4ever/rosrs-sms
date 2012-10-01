package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;


public class AO {

    public static  String NAMESPACE = "http://purl.org/ao/";
    public static OntModel ontModel = ModelFactory.createOntologyModel();
    public static Property body = ontModel.getProperty(NAMESPACE + "body");
    public static Property annotatesResource = ontModel.getProperty(NAMESPACE + "annotatesResource");
}
