package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;


public class ORE {
    
    public static final String NAMESPACE = "http://www.openarchives.org/ore/terms/";
    public static OntModel ontModel = ModelFactory.createOntologyModel(NAMESPACE);
    
    public static final OntClass oreProxyClass = ontModel.getOntClass(NAMESPACE + "Proxy");
    public static final Property describes = ontModel.getProperty(NAMESPACE + "describes");
    public static final Property isDescribedBy = ontModel.getProperty(NAMESPACE + "isDescribedBy");
    public static final Property aggregates = ontModel.getProperty(NAMESPACE + "aggregates");
    public static final Property oreProxyIn = ontModel.getProperty(NAMESPACE + "proxyIn");
    public static final Property oreProxyFor = ontModel.getProperty(NAMESPACE + "proxyFor");

}
