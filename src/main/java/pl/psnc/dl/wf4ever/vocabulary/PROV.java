package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class PROV {

    public static final String NAMESPACE = "http://www.w3.org/ns/prov#";
    public static OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,ModelFactory.createDefaultModel().read(NAMESPACE));
    public static final Property hadOriginalSource = ontModel.getProperty(NAMESPACE + "hadOriginalSource");

}
