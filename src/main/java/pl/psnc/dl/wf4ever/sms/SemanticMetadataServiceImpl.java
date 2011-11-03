/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl implements SemanticMetadataService {

	/**
	 * Date format used for dates. This is NOT xsd:dateTime because of missing :
	 * in time zone.
	 */
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssZ");

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://example.wf4ever-project.org/2011/ro.owl#";

	private static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

	private static final PrefixMapping standardNamespaces = PrefixMapping.Factory
			.create().setNsPrefix("ore", ORE_NAMESPACE)
			.setNsPrefix("ro", RO_NAMESPACE).setNsPrefix("dcterms", DCTerms.NS)
			.setNsPrefix("foaf", FOAF_NAMESPACE).lock();

	private final OntModel model;

	private final OntClass researchObjectClass;

	private final OntClass manifestClass;

	private final OntClass foafAgentClass;

	private final Property describes;

	private final Property name;

	private final String getManifestQueryTmpl = "PREFIX ore: <http://www.openarchives.org/ore/terms/> DESCRIBE <%s> ?ro WHERE { <%<s> ore:describes ?ro. }";

	public SemanticMetadataServiceImpl() {
		InputStream modelIS = getClass().getClassLoader().getResourceAsStream(
				"ro.owl");
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(modelIS, null);

		OntModel foafModel = ModelFactory
				.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		foafModel.read(FOAF_NAMESPACE, null);
		model.addSubModel(foafModel);

		researchObjectClass = model
				.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = model.getOntClass(RO_NAMESPACE + "Manifest");
		describes = model.getProperty(ORE_NAMESPACE + "describes");
		foafAgentClass = model.getOntClass(FOAF_NAMESPACE + "Agent");
		name = model.getProperty(FOAF_NAMESPACE + "name");

		model.setNsPrefixes(standardNamespaces);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#createResearchObject(java
	 * .net.URI)
	 */
	@Override
	public void createResearchObject(URI manifestURI, UserProfile userProfile) {
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest != null) {
			throw new IllegalArgumentException("URI already exists");
		}
		manifest = model
				.createIndividual(manifestURI.toString(), manifestClass);
		Individual ro = model.createIndividual(manifestURI.toString() + "#ro",
				researchObjectClass);
		model.add(manifest, describes, ro);
		model.add(manifest, DCTerms.created,
				createDateLiteral(Calendar.getInstance()));

		Individual agent = model.createIndividual(foafAgentClass);
		model.add(agent, name, userProfile.getName());
		model.add(manifest, DCTerms.creator, agent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#createResearchObjectAsCopy
	 * (java.net.URI, java.net.URI)
	 */
	@Override
	public void createResearchObjectAsCopy(URI manifestURI, URI baseManifestURI) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResearchObject(java
	 * .net.URI)
	 */
	@Override
	public void removeResearchObject(URI manifestURI) {
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest == null) {
			throw new IllegalArgumentException("URI not found");
		}
		model.removeAll(manifest, null, null);
		Individual ro = model.getIndividual(manifestURI.toString() + "#ro");
		if (ro == null) {
			throw new IllegalArgumentException("URI not found");
		}
		model.removeAll(ro, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getResearchObject(java
	 * .net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getResearchObject(URI manifestURI, Notation notation) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getManifest(java.net.URI,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getManifest(URI manifestURI, Notation notation) {
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest == null) {
			throw new IllegalArgumentException("URI not found");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getManifestQueryTmpl,
				manifestURI.toString());
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		Model resultModel = qexec.execDescribe();
		qexec.close();

		resultModel.write(out, getJenaLang(notation));
		return new ByteArrayInputStream(out.toByteArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#updateManifest(java.net
	 * .URI, java.io.InputStream,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public void updateManifest(URI manifestURI, InputStream manifest,
			Notation notation) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addResource(java.net.URI,
	 * java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)
	 */
	@Override
	public void addResource(URI manifestURI, URI resourceURI,
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResource(java.net
	 * .URI, java.net.URI)
	 */
	@Override
	public void removeResource(URI manifestURI, URI resourceURI) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getResource(java.net.URI,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getResource(URI resourceURI, Notation notation) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addAnnotation(java.net
	 * .URI, java.net.URI, java.net.URI, java.util.Map)
	 */
	@Override
	public void addAnnotation(URI annotationURI, URI annotationBodyURI,
			URI annotatedResourceURI, Map<String, String> attributes) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#deleteAnnotationsWithBodies
	 * (java.net.URI)
	 */
	@Override
	public void deleteAnnotationsWithBodies(URI annotationBodyURI) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getAnnotations(java.net
	 * .URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getAnnotations(URI annotationsURI, Notation notation) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getAnnotationBody(java
	 * .net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getAnnotationBody(URI annotationBodyURI,
			Notation notation) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#findResearchObjects(java
	 * .lang.String, java.util.Map)
	 */
	@Override
	public List<URI> findResearchObjects(String workspaceId,
			Map<String, List<String>> queryParameters) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getJenaLang(Notation notation) {
		switch (notation) {
		case RDF_XML:
			return "RDF/XML";
		case TRIG:
			return "N3";
		default:
			return "RDF/XML";
		}
	}

	public Literal createDateLiteral(Calendar cal) {

		return model.createTypedLiteral(new XSDDateTime(cal),
				XSDDatatype.XSDdateTime);
	}

}
