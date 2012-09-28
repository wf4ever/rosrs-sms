package pl.psnc.dl.wf4ever.vocabulary;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class ROEVO {

    public static final String NAMESPACE = "http://purl.org/wf4ever/roevo#";
    public static OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,ModelFactory.createDefaultModel().read(NAMESPACE));
    public static final OntClass liveROClass = ontModel.getOntClass(NAMESPACE + "LiveRO");
    public static final OntClass snapshotROClass = ontModel.getOntClass(NAMESPACE + "SnapshotRO");
    public static final OntClass archivedROClass = ontModel.getOntClass(NAMESPACE + "ArchivedRO");
    public static final OntClass ChangeSpecificationClass = ontModel.getOntClass(NAMESPACE + "ChangeSpecification");
    public static final OntClass ChangeClass = ontModel.getOntClass(NAMESPACE + "Change");
    public static final OntClass AdditionClass = ontModel.getOntClass(NAMESPACE + "Addition");
    public static final OntClass ModificationClass = ontModel.getOntClass(NAMESPACE + "Modification");
    public static final OntClass RemovalClass = ontModel.getOntClass(NAMESPACE + "Removal");

    public static final Property roevoIsSnapshotOf = ontModel.getProperty(NAMESPACE + "isSnapshotOf");
    public static final Property roevoHasSnapshot = ontModel.getProperty(NAMESPACE + "hasSnapshot");
    public static final Property roevoHasChange = ontModel.getProperty(NAMESPACE + "hasChange");
    public static final Property roevoSnapshottedAtTime = ontModel.getProperty(NAMESPACE + "snapshottedAtTime");
    public static final Property roevoArchivedBy = ontModel.getProperty(NAMESPACE + "archivedBy");
    public static final Property roevoIsArchiveOf = ontModel.getProperty(NAMESPACE + "isArchiveOf");
    public static final Property roevoHasArchive = ontModel.getProperty(NAMESPACE + "hasArchive");
    public static final Property roevoArchivedAtTime = ontModel.getProperty(NAMESPACE + "archivedAtTime");
    public static final Property roevoSnapshottedBy = ontModel.getProperty(NAMESPACE + "snapshottedBy");
    public static final Property roevoWasChangedBy = ontModel.getProperty(NAMESPACE + "wasChangedBy");
    public static final Property roevoRelatedResource = ontModel.getProperty(NAMESPACE + "relatedResource");

}
