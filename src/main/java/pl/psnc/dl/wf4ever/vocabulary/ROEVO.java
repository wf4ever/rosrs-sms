package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class ROEVO {

    public static final String NAMESPACE = "http://purl.org/wf4ever/roevo#";
    public static OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,ModelFactory.createDefaultModel().read(NAMESPACE));
    public static final OntClass LiveROClass = ontModel.getOntClass(NAMESPACE + "LiveRO");
    public static final OntClass SnapshotROClass = ontModel.getOntClass(NAMESPACE + "SnapshotRO");
    public static final OntClass ArchivedROClass = ontModel.getOntClass(NAMESPACE + "ArchivedRO");
    public static final OntClass ChangeSpecificationClass = ontModel.getOntClass(NAMESPACE + "ChangeSpecification");
    public static final OntClass ChangeClass = ontModel.getOntClass(NAMESPACE + "Change");
    public static final OntClass AdditionClass = ontModel.getOntClass(NAMESPACE + "Addition");
    public static final OntClass ModificationClass = ontModel.getOntClass(NAMESPACE + "Modification");
    public static final OntClass RemovalClass = ontModel.getOntClass(NAMESPACE + "Removal");

    public static final Property isSnapshotOf = ontModel.getProperty(NAMESPACE + "isSnapshotOf");
    public static final Property hasSnapshot = ontModel.getProperty(NAMESPACE + "hasSnapshot");
    public static final Property hasChange = ontModel.getProperty(NAMESPACE + "hasChange");
    public static final Property snapshottedAtTime = ontModel.getProperty(NAMESPACE + "snapshottedAtTime");
    public static final Property archivedBy = ontModel.getProperty(NAMESPACE + "archivedBy");
    public static final Property isArchiveOf = ontModel.getProperty(NAMESPACE + "isArchiveOf");
    public static final Property hasArchive = ontModel.getProperty(NAMESPACE + "hasArchive");
    public static final Property archivedAtTime = ontModel.getProperty(NAMESPACE + "archivedAtTime");
    public static final Property snapshottedBy = ontModel.getProperty(NAMESPACE + "snapshottedBy");
    public static final Property wasChangedBy = ontModel.getProperty(NAMESPACE + "wasChangedBy");
    public static final Property relatedResource = ontModel.getProperty(NAMESPACE + "relatedResource");

}
