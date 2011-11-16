/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.openrdf.rio.RDFFormat;

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
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
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
import de.fuberlin.wiwiss.ng4j.db.NamedGraphSetDB;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl
	implements SemanticMetadataService
{

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://www.wf4ever-project.org/vocab/ro#";

	private static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

	private static final String AO_NAMESPACE = "http://purl.org/ao/core/";

	private static final String PAV_NAMESPACE = "http://purl.org/pav/";

	private static final URI DEFAULT_NAMED_GRAPH_URI = URI
			.create("http://example.wf4ever-project.org/2011/defaultGraph");

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

	private final String findManifestsQueryTmpl = "PREFIX ro: <" + RO_NAMESPACE + "> SELECT ?manifest "
			+ "WHERE { ?manifest a ro:Manifest. FILTER regex(str(?manifest), \"^%s\") . }";

	private final Connection connection;


	public SemanticMetadataServiceImpl()
		throws IOException, NamingException, SQLException, ClassNotFoundException
	{
		connection = getConnection("connection.properties");
		if (connection == null) {
			throw new RuntimeException("Connection could not be created");
		}

		graphset = new NamedGraphSetDB(connection, "sms");
		NamedGraph defaultGraph = getOrCreateGraph(graphset, DEFAULT_NAMED_GRAPH_URI);
		Model defaultModel = ModelFactory.createModelForGraph(defaultGraph);

		InputStream modelIS = getClass().getClassLoader().getResourceAsStream("ro.rdf");
		defaultModel.read(modelIS, null);
		//		defaultModel.read("http://www.wf4ever-project.org/wiki/download/attachments/2064773/ro.rdf");

		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, defaultModel);

		OntModel foafModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		foafModel.read(FOAF_NAMESPACE, null);
		model.addSubModel(foafModel);

		researchObjectClass = model.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = model.getOntClass(RO_NAMESPACE + "Manifest");
		resourceClass = model.getOntClass(ORE_NAMESPACE + "AggregatedResource");
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


	private Connection getConnection(String filename)
		throws IOException, NamingException, SQLException, ClassNotFoundException
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
		Properties props = new Properties();
		props.load(is);

		if (props.containsKey("datasource")) {
			String datasource = props.getProperty("datasource");
			if (datasource != null) {
				InitialContext ctx = new InitialContext();
				DataSource ds = (DataSource) ctx.lookup(datasource);
				return ds.getConnection();
			}
		}
		else {
			String driver_class = props.getProperty("driver_class");
			String url = props.getProperty("url");
			String username = props.getProperty("username");
			String password = props.getProperty("password");
			if (driver_class != null && url != null && username != null && password != null) {
				Class.forName(driver_class);
				return DriverManager.getConnection(url, username, password);
			}
		}

		return null;
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
		manifestURI = manifestURI.normalize();
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
		graphset.createGraph(annotations.getURI());
	}


	@Override
	public void createManifest(URI manifestURI, InputStream is, RDFFormat rdfFormat, UserProfile userProfile)
	{
		manifestURI = manifestURI.normalize();
		model.read(is, manifestURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());

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

		if (!graphset.containsGraph(manifestURI.resolve("annotations").toString())) {
			graphset.createGraph(manifestURI.resolve("annotations").toString());
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
		manifestURI = manifestURI.normalize();
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
		deleteAllAnnotationsWithBodies(manifestURI.resolve("annotations"));
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getManifest(java.net.URI,
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getManifest(URI manifestURI, RDFFormat rdfFormat)
	{
		manifestURI = manifestURI.normalize();
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

		resultModel.write(out, rdfFormat.getName().toUpperCase());
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
		manifestURI = manifestURI.normalize();
		resourceURI = resourceURI.normalize();
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
		manifestURI = manifestURI.normalize();
		resourceURI = resourceURI.normalize();
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
	public InputStream getResource(URI resourceURI, RDFFormat rdfFormat)
	{
		resourceURI = resourceURI.normalize();
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

		resultModel.write(out, rdfFormat.getName().toUpperCase());
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
		annotationURI = annotationURI.normalize();
		annotationBodyURI = annotationBodyURI.normalize();
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
	public void addAnnotation(URI annotationURI, URI annotationBodyURI, InputStream is, RDFFormat rdfFormat,
			UserProfile userProfile)
	{
		annotationURI = annotationURI.normalize();
		annotationBodyURI = annotationBodyURI.normalize();
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
		annotationModel.read(is, annotationURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());
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
		annotationBodyURI = annotationBodyURI.normalize();
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
		annotationsURI = annotationsURI.normalize();
		if (!graphset.containsGraph(annotationsURI.toString())) {
			return;
		}
		OntModel annotationModel = createOntModelForNamedGraph(annotationsURI);
		NodeIterator it = annotationModel.listObjectsOfProperty(hasTopic);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			if (graphset.containsGraph(annotationBodyRef.asResource().getURI())) {
				graphset.removeGraph(annotationBodyRef.asResource().getURI());
			}
		}
		graphset.removeGraph(annotationsURI.toString());
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getAnnotations(java.net
	 * .URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
	 */
	@Override
	public InputStream getAnnotation(URI annotationURI, RDFFormat rdfFormat)
	{
		annotationURI = annotationURI.normalize();
		URI annotationsURI = annotationURI.resolve("annotations");
		if (!graphset.containsGraph(annotationsURI.toString())) {
			return null;
		}
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

		resultModel.write(out, rdfFormat.getName().toUpperCase());
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
	public InputStream getAnnotationBody(URI annotationBodyURI, RDFFormat rdfFormat)
	{
		annotationBodyURI = annotationBodyURI.normalize();
		if (!graphset.containsGraph(annotationBodyURI.toString())) {
			return null;
		}
		OntModel annotationModel = createOntModelForNamedGraph(annotationBodyURI);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		annotationModel.write(out, rdfFormat.getName().toUpperCase());
		return new ByteArrayInputStream(out.toByteArray());
	}


	@Override
	public InputStream getAllAnnotations(URI annotationsURI, RDFFormat rdfFormat)
	{
		annotationsURI = annotationsURI.normalize();
		if (!graphset.containsGraph(annotationsURI.toString())) {
			return null;
		}
		OntModel annotationModel = createOntModelForNamedGraph(annotationsURI);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		annotationModel.write(out, rdfFormat.getName().toUpperCase());
		return new ByteArrayInputStream(out.toByteArray());
	}


	@Override
	public InputStream getAllAnnotationsWithBodies(URI annotationsURI, RDFFormat rdfFormat)
	{
		annotationsURI = annotationsURI.normalize();
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
		graphset.write(out, rdfFormat.getName().toUpperCase(), null);
		return new ByteArrayInputStream(out.toByteArray());
	}


	@Override
	public Set<URI> findManifests(URI partialURI)
	{
		partialURI = partialURI.normalize();
		String queryString = String.format(findManifestsQueryTmpl, partialURI.toString());
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		Set<URI> uris = new HashSet<URI>();
		while (results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			Resource manifest = solution.getResource("manifest");
			uris.add(URI.create(manifest.getURI()));
		}

		// Important - free up resources used running the query
		qe.close();
		return uris;
	}


	/**
	 * @param namedGraphURI
	 * @return
	 */
	private OntModel createOntModelForNamedGraph(URI namedGraphURI)
	{
		NamedGraph namedGraph = getOrCreateGraph(graphset, namedGraphURI);
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
			graphset.asJenaModel(DEFAULT_NAMED_GRAPH_URI.toString()));
		ontModel.addSubModel(model);
		return ontModel;
	}


	private NamedGraph getOrCreateGraph(NamedGraphSet graphset, URI namedGraphURI)
	{
		return graphset.containsGraph(namedGraphURI.toString()) ? graphset.getGraph(namedGraphURI.toString())
				: graphset.createGraph(namedGraphURI.toString());
	}


	@Override
	public void close()
	{
		graphset.close();
	}

}
