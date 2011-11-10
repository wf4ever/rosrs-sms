/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl
	implements SemanticMetadataService
{

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://example.wf4ever-project.org/2011/ro.owl#";

	private static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

	private static final String AO_NAMESPACE = "http://purl.org/ao/core/";

	private static final String PAV_NAMESPACE = "http://purl.org/pav/";

	private static final String DEFAULT_NAMED_GRAPH_URI = "http://example.wf4ever-project.org/2011/defaultGraph";

	private static final PrefixMapping standardNamespaces = PrefixMapping.Factory.create()
			.setNsPrefix("ore", ORE_NAMESPACE).setNsPrefix("ro", RO_NAMESPACE).setNsPrefix("dcterms", DCTerms.NS)
			.setNsPrefix("foaf", FOAF_NAMESPACE).lock();

	private final NamedGraphSet graphset;

	private final OntModel model;

	private final OntClass researchObjectClass;

	private final OntClass manifestClass;

	private final OntClass foafAgentClass;

	private final OntClass resourceClass;

	private final OntClass annotationClass;

	private final Property describes;

	private final Property aggregates;

	private final Property foafName;

	private final Property name;

	private final Property filesize;

	private final Property checksum;

	private final Property annotatesResource;

	private final Property hasTopic;

	private final Property pavCreatedBy;

	private final Property pavCreatedOn;

	private final String getManifestQueryTmpl = "PREFIX ore: <http://www.openarchives.org/ore/terms/> "
			+ "DESCRIBE <%s> ?ro ?proxy ?resource "
			+ "WHERE { <%<s> ore:describes ?ro. OPTIONAL { ?proxy ore:proxyIn ?ro. ?ro ore:aggregates ?resource. } }";

	private final String getResourceQueryTmpl = "DESCRIBE <%s> WHERE { }";


	public SemanticMetadataServiceImpl()
	{
		graphset = new NamedGraphSetImpl();
		NamedGraph defaultGraph = graphset.createGraph(DEFAULT_NAMED_GRAPH_URI);
		Model defaultModel = ModelFactory.createModelForGraph(defaultGraph);

		InputStream modelIS = getClass().getClassLoader().getResourceAsStream("ro.owl");
		defaultModel.read(modelIS, null);

		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, defaultModel);

		OntModel foafModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		foafModel.read(FOAF_NAMESPACE, null);
		model.addSubModel(foafModel);

		researchObjectClass = model.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = model.getOntClass(RO_NAMESPACE + "Manifest");
		resourceClass = model.getOntClass(RO_NAMESPACE + "Resource");
		annotationClass = model.getOntClass(RO_NAMESPACE + "GraphAnnotation");

		name = model.getProperty(RO_NAMESPACE + "name");
		filesize = model.getProperty(RO_NAMESPACE + "filesize");
		checksum = model.getProperty(RO_NAMESPACE + "checksum");
		describes = model.getProperty(ORE_NAMESPACE + "describes");
		aggregates = model.getProperty(ORE_NAMESPACE + "aggregates");
		foafAgentClass = model.getOntClass(FOAF_NAMESPACE + "Agent");
		foafName = model.getProperty(FOAF_NAMESPACE + "name");
		annotatesResource = model.getProperty(AO_NAMESPACE + "annotatesResource");
		hasTopic = model.getProperty(AO_NAMESPACE + "hasTopic");
		pavCreatedBy = model.getProperty(PAV_NAMESPACE + "createdBy");
		pavCreatedOn = model.getProperty(PAV_NAMESPACE + "createdOn");

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
	public void createManifest(URI manifestURI, UserProfile userProfile)
	{
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest != null) {
			throw new IllegalArgumentException("URI already exists");
		}
		manifest = model.createIndividual(manifestURI.toString(), manifestClass);
		Individual ro = model.createIndividual(manifestURI.toString() + "#ro", researchObjectClass);
		model.add(manifest, describes, ro);
		model.add(manifest, DCTerms.created, model.createTypedLiteral(Calendar.getInstance()));

		Individual agent = model.createIndividual(foafAgentClass);
		model.add(agent, foafName, userProfile.getName());
		model.add(manifest, DCTerms.creator, agent);

		Resource annotations = model.createResource(manifestURI.resolve("annotations").toString());
		manifest.addSeeAlso(annotations);
	}


	@Override
	public void createManifest(URI manifestURI, InputStream is, Notation notation, UserProfile userProfile)
	{
		model.read(is, manifestURI.resolve(".").toString(), getJenaLang(notation));

		// leave only one dcterms:created - the earliest
		Individual manifest = model.getIndividual(manifestURI.toString());
		NodeIterator it = model.listObjectsOfProperty(manifest, DCTerms.created);
		Calendar earliest = null;
		while (it.hasNext()) {
			Calendar created = ((XSDDateTime) it.next().asLiteral().getValue()).asCalendar();
			if (earliest == null || created.before(earliest))
				earliest = created;
		}
		if (earliest != null) {
			model.removeAll(manifest, DCTerms.created, null);
			model.add(manifest, DCTerms.created, model.createTypedLiteral(earliest));
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#createResearchObjectAsCopy
	 * (java.net.URI, java.net.URI)
	 */
	@Override
	public void createResearchObjectAsCopy(URI manifestURI, URI baseManifestURI)
	{
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
	public void removeManifest(URI manifestURI)
	{
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
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getManifest(java.net.URI,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getManifest(URI manifestURI, Notation notation)
	{
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getManifestQueryTmpl, manifestURI.toString());
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
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addResource(java.net.URI,
	 * java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)
	 */
	@Override
	public void addResource(URI manifestURI, URI resourceURI, ResourceInfo resourceInfo)
	{
		Individual manifest = model.getIndividual(manifestURI.toString());
		if (manifest == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual ro = model.getIndividual(manifestURI.toString() + "#ro");
		if (ro == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual resource = model.createIndividual(resourceURI.toString(), resourceClass);
		model.add(ro, aggregates, resource);
		if (resourceInfo != null) {
			if (resourceInfo.getName() != null) {
				model.add(resource, name, model.createTypedLiteral(resourceInfo.getName()));
			}
			model.add(resource, filesize, model.createTypedLiteral(resourceInfo.getSizeInBytes()));
			if (resourceInfo.getChecksum() != null && resourceInfo.getDigestMethod() != null) {
				model.add(
					resource,
					checksum,
					model.createResource(String.format("urn:%s:%s", resourceInfo.getDigestMethod(),
						resourceInfo.getChecksum())));
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResource(java.net
	 * .URI, java.net.URI)
	 */
	@Override
	public void removeResource(URI manifestURI, URI resourceURI)
	{
		Individual ro = model.getIndividual(manifestURI.toString() + "#ro");
		if (ro == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual resource = model.getIndividual(resourceURI.toString());
		if (resource == null) {
			throw new IllegalArgumentException("URI not found");
		}
		model.remove(ro, aggregates, resource);

		StmtIterator it = model.listStatements(null, aggregates, resource);
		if (!it.hasNext()) {
			model.removeAll(resource, null, null);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getResource(java.net.URI,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getResource(URI resourceURI, Notation notation)
	{
		Individual resource = model.getIndividual(resourceURI.toString());
		if (resource == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getResourceQueryTmpl, resourceURI.toString());
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
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addAnnotation(java.net
	 * .URI, java.net.URI, java.net.URI, java.util.Map)
	 */
	@Override
	public void addAnnotation(URI annotationURI, URI annotationBodyURI, Map<URI, Map<URI, String>> triples,
			UserProfile userProfile)
	{
		URI annotationsURI = annotationURI.resolve("annotations");
		OntModel annotationsModel = createOntModelForNamedGraph(annotationsURI);

		Individual annotation = annotationsModel.createIndividual(annotationURI.toString(), annotationClass);
		annotationsModel.add(annotation, pavCreatedOn, annotationsModel.createTypedLiteral(Calendar.getInstance()));

		Individual agent = annotationsModel.createIndividual(foafAgentClass);
		annotationsModel.add(agent, foafName, userProfile.getName());
		annotationsModel.add(annotation, pavCreatedBy, agent);

		Resource annotationBodyRef = annotationsModel.createResource(annotationBodyURI.toString());
		annotationsModel.add(annotation, hasTopic, annotationBodyRef);

		OntModel annotationBodyModel = createOntModelForNamedGraph(annotationBodyURI);

		for (Map.Entry<URI, Map<URI, String>> entry : triples.entrySet()) {
			Individual resource = model.getIndividual(entry.getKey().toString());
			if (resource == null) {
				continue;
			}
			annotationsModel.add(annotation, annotatesResource, resource);

			for (Map.Entry<URI, String> attributes : entry.getValue().entrySet()) {
				annotationBodyModel.add(resource, annotationBodyModel.createProperty(attributes.getKey().toString()),
					attributes.getValue());
			}
		}

	}


	@Override
	public void addAnnotation(URI annotationURI, URI annotationBodyURI, InputStream is, Notation notation,
			UserProfile userProfile)
	{
		URI annotationsURI = annotationURI.resolve("annotations");
		OntModel annotationsModel = createOntModelForNamedGraph(annotationsURI);

		Individual annotation = annotationsModel.createIndividual(annotationURI.toString(), annotationClass);
		annotationsModel.add(annotation, pavCreatedOn, annotationsModel.createTypedLiteral(Calendar.getInstance()));

		Individual agent = annotationsModel.createIndividual(foafAgentClass);
		annotationsModel.add(agent, foafName, userProfile.getName());
		annotationsModel.add(annotation, pavCreatedBy, agent);

		Resource annotationBodyRef = annotationsModel.createResource(annotationBodyURI.toString());
		model.add(annotation, hasTopic, annotationBodyRef);

		OntModel annotationModel = createOntModelForNamedGraph(annotationBodyURI);
		annotationModel.read(is, annotationURI.resolve(".").toString(), getJenaLang(notation));
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#deleteAnnotationsWithBodies
	 * (java.net.URI)
	 */
	@Override
	public void deleteAnnotationWithBody(URI annotationBodyURI)
	{
		if (!graphset.containsGraph(annotationBodyURI.toString())) {
			throw new IllegalArgumentException("URI not found");
		}
		graphset.removeGraph(annotationBodyURI.toString());

		OntModel commonModel = createOntModelForAllNamedGraphs();
		Resource annotationBodyRef = commonModel.createResource(annotationBodyURI.toString());
		ResIterator it = commonModel.listResourcesWithProperty(hasTopic, annotationBodyRef);
		while (it.hasNext()) {
			Resource annotation = it.next();
			commonModel.removeAll(annotation, null, null);
		}
	}


	@Override
	public void deleteAllAnnotationsWithBodies(URI annotationsURI)
	{
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
	public InputStream getAnnotation(URI annotationURI, Notation notation)
	{
		URI annotationsURI = annotationURI.resolve("annotations");
		OntModel annotationModel = createOntModelForNamedGraph(annotationsURI);

		Individual annotation = annotationModel.getIndividual(annotationURI.toString());
		if (annotation == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getResourceQueryTmpl, annotationURI.toString());
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, annotationModel);
		Model resultModel = qexec.execDescribe();
		qexec.close();

		resultModel.write(out, getJenaLang(notation));
		return new ByteArrayInputStream(out.toByteArray());
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getAnnotationBody(java
	 * .net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getAnnotationBody(URI annotationBodyURI, Notation notation)
	{
		if (!graphset.containsGraph(annotationBodyURI.toString())) {
			return null;
		}
		OntModel annotationModel = createOntModelForNamedGraph(annotationBodyURI);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		annotationModel.write(out, getJenaLang(notation));
		return new ByteArrayInputStream(out.toByteArray());
	}


	@Override
	public InputStream getAllAnnotations(URI annotationsURI, Notation notation)
	{
		if (!graphset.containsGraph(annotationsURI.toString())) {
			return null;
		}
		OntModel annotationModel = createOntModelForNamedGraph(annotationsURI);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		annotationModel.write(out, getJenaLang(notation));
		return new ByteArrayInputStream(out.toByteArray());
	}


	@Override
	public InputStream getAllAnnotationsWithBodies(URI annotationsURI, Notation notation)
	{
		if (!graphset.containsGraph(annotationsURI.toString())) {
			return null;
		}
		NamedGraphSet annotationGraphSet = new NamedGraphSetImpl();
		annotationGraphSet.addGraph(graphset.getGraph(annotationsURI.toString()));

		OntModel annotationModel = createOntModelForNamedGraph(annotationsURI);
		NodeIterator it = annotationModel.listObjectsOfProperty(hasTopic);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			if (graphset.containsGraph(annotationBodyRef.asResource().getURI())) {
				annotationGraphSet.addGraph(graphset.getGraph(annotationBodyRef.asResource().getURI()));
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		graphset.write(out, getJenaLang(notation), null);
		return new ByteArrayInputStream(out.toByteArray());
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#findResearchObjects(java
	 * .lang.String, java.util.Map)
	 */
	@Override
	public List<URI> findResearchObjects(String workspaceId, Map<String, List<String>> queryParameters)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * @param namedGraphURI
	 * @return
	 */
	private OntModel createOntModelForNamedGraph(URI namedGraphURI)
	{
		NamedGraph namedGraph = (graphset.containsGraph(namedGraphURI.toString()) ? graphset.getGraph(namedGraphURI
				.toString()) : graphset.createGraph(namedGraphURI.toString()));
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
			ModelFactory.createModelForGraph(namedGraph));
		ontModel.addSubModel(model);
		return ontModel;
	}


	/**
	 * @param namedGraphURI
	 * @return
	 */
	private OntModel createOntModelForAllNamedGraphs()
	{
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
			graphset.asJenaModel(DEFAULT_NAMED_GRAPH_URI));
		ontModel.addSubModel(model);
		return ontModel;
	}


	private String getJenaLang(Notation notation)
	{
		switch (notation) {
			case RDF_XML:
				return "RDF/XML";
			case TURTLE:
				return "TURTLE";
			case N3:
				return "N3";
			case TRIG:
				return "TRIG";
			case TRIX:
				return "TRIX";
			default:
				return "RDF/XML";
		}
	}

}
