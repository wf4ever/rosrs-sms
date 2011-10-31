/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl implements SemanticMetadataService {

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://example.wf4ever-project.org/2011/ro.owl#";

	private final OntModel model;

	private final OntClass researchObjectClass;

	private final OntClass manifestClass;

	private final Property describes;

	public SemanticMetadataServiceImpl() {
		InputStream modelIS = getClass().getClassLoader().getResourceAsStream(
				"ro.owl");
		OntModel base = ModelFactory
				.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
		base.read(modelIS, "");

		researchObjectClass = base.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = base.getOntClass(RO_NAMESPACE + "Manifest");
		describes = base.getProperty(ORE_NAMESPACE + "describes");

		model = ModelFactory.createOntologyModel(
				OntModelSpec.OWL_LITE_MEM_RULES_INF, base);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#createResearchObject(java
	 * .net.URI)
	 */
	@Override
	public void createResearchObject(URI manifestURI) {
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest != null) {
			throw new IllegalArgumentException("URI already exists");
		}
		manifest = model
				.createIndividual(manifestURI.toString(), manifestClass);
		Individual ro = model.createIndividual(manifestURI.toString() + "#ro",
				researchObjectClass);
		manifest.addProperty(describes, ro);
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
		Model manifestModel = ModelFactory.createDefaultModel();

		manifestModel.write(out, getJenaLang(notation));
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

}
