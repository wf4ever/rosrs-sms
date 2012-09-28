package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;


public class FOAF {
    
    public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static OntModel ontModel = ModelFactory.createOntologyModel(NAMESPACE);
    
    public static final OntClass foafAgentClass = ontModel.getOntClass(NAMESPACE + "Agent");
    public static final OntClass foafPersonClass = ontModel.getOntClass(NAMESPACE + "Person");
    public static final Property foafName = ontModel.getProperty(NAMESPACE + "name");
    
}
