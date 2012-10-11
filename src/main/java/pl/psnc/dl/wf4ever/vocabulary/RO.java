package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class RO {

    public static final String NAMESPACE = "http://purl.org/wf4ever/ro#";
    public static OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,ModelFactory.createDefaultModel().read(NAMESPACE));
    public static final OntClass ResearchObject = ontModel.getOntClass(NAMESPACE + "ResearchObject");
    public static final OntClass Manifest = ontModel.getOntClass(NAMESPACE + "Manifest");
    public static final OntClass Resource = ontModel.getOntClass(NAMESPACE + "Resource");
    public static final OntClass Folder = ontModel.getOntClass(NAMESPACE + "Folder");
    public static final OntClass AggregatedAnnotation = ontModel.getOntClass(NAMESPACE + "AggregatedAnnotation");

    public static final Property name = ontModel.getProperty(NAMESPACE + "name");
    public static final Property filesize = ontModel.getProperty(NAMESPACE + "filesize");
    public static final Property checksum = ontModel.getProperty(NAMESPACE + "checksum");
    public static final Property annotatesAggregatedResource = ontModel.getProperty(NAMESPACE
        + "annotatesAggregatedResource");
}
