package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;


public class FOAF {
    
    public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static final OntModel defaultModel = ModelFactory.createOntologyModel(
        new OntModelSpec(OntModelSpec.OWL_MEM),
        ModelFactory.createDefaultModel().read(RO.NAMESPACE).read(ROEVO.NAMESPACE));
    
    public static final OntClass Agent = defaultModel.getOntClass(NAMESPACE + "Agent");
    public static final OntClass Person = defaultModel.getOntClass(NAMESPACE + "Person");
    public static final Property name = defaultModel.getProperty(NAMESPACE + "name");
    
}
