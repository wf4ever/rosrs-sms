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
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

import com.hp.hpl.jena.graph.Node;
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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.db.NamedGraphSetDB;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl
	implements SemanticMetadataService
{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(SemanticMetadataServiceImpl.class);

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://purl.org/wf4ever/ro#";

	private static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

	private static final String AO_NAMESPACE = "http://purl.org/ao/";

	private static final URI DEFAULT_NAMED_GRAPH_URI = URI.create("sms");

	private static final PrefixMapping standardNamespaces = PrefixMapping.Factory.create()
			.setNsPrefix("ore", ORE_NAMESPACE).setNsPrefix("ro", RO_NAMESPACE).setNsPrefix("dcterms", DCTerms.NS)
			.setNsPrefix("foaf", FOAF_NAMESPACE).lock();

	private static final String SERVICE_NAME = "RODL";

	private final NamedGraphSet graphset;

	/**
	 * For storing triples that don't belong to any RO, the default graph. 
	 */
	private final OntModel defaultModel;

	private final OntClass researchObjectClass;

	private final OntClass manifestClass;

	private final OntClass foafAgentClass;

	private final OntClass resourceClass;

	private final OntClass roFolderClass;

	private final Property describes;

	private final Property isDescribedBy;

	private final Property aggregates;

	private final Property foafName;

	private final Property name;

	private final Property filesize;

	private final Property checksum;

	private final Property body;

	private final String getResourceQueryTmpl = "DESCRIBE <%s> WHERE { }";

	private final String findResearchObjectsQueryTmpl = "PREFIX ro: <" + RO_NAMESPACE + "> SELECT ?ro "
			+ "WHERE { ?ro a ro:ResearchObject. FILTER regex(str(?ro), \"^%s\") . }";

	private final Connection connection;

	private final UserProfile user;


	public SemanticMetadataServiceImpl(UserProfile user)
		throws IOException, NamingException, SQLException, ClassNotFoundException
	{
		connection = getConnection("connection.properties");
		if (connection == null) {
			throw new RuntimeException("Connection could not be created");
		}
		this.user = user;

		graphset = new NamedGraphSetDB(connection, "sms");
		NamedGraph defaultGraph = getOrCreateGraph(graphset, DEFAULT_NAMED_GRAPH_URI);
		Model tmpModel = ModelFactory.createModelForGraph(defaultGraph);

		//		InputStream modelIS = getClass().getClassLoader().getResourceAsStream("ro.rdf");
		//		defaultModel.read(modelIS, null);
		tmpModel.read(RO_NAMESPACE, "TTL");

		defaultModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, tmpModel);

		OntModel foafModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		foafModel.read(FOAF_NAMESPACE, null);
		defaultModel.addSubModel(foafModel);

		researchObjectClass = defaultModel.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = defaultModel.getOntClass(RO_NAMESPACE + "Manifest");
		resourceClass = defaultModel.getOntClass(RO_NAMESPACE + "Resource");
		roFolderClass = defaultModel.getOntClass(RO_NAMESPACE + "Folder");

		name = defaultModel.getProperty(RO_NAMESPACE + "name");
		filesize = defaultModel.getProperty(RO_NAMESPACE + "filesize");
		checksum = defaultModel.getProperty(RO_NAMESPACE + "checksum");
		describes = defaultModel.getProperty(ORE_NAMESPACE + "describes");
		isDescribedBy = defaultModel.getProperty(ORE_NAMESPACE + "isDescribedBy");
		aggregates = defaultModel.getProperty(ORE_NAMESPACE + "aggregates");
		foafAgentClass = defaultModel.getOntClass(FOAF_NAMESPACE + "Agent");
		foafName = defaultModel.getProperty(FOAF_NAMESPACE + "name");
		body = defaultModel.getProperty(AO_NAMESPACE + "body");

		defaultModel.setNsPrefixes(standardNamespaces);
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
	public void createResearchObject(URI researchObjectURI)
	{
		URI manifestURI = getManifestURI(researchObjectURI.normalize());
		OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
		Individual manifest = manifestModel.getIndividual(manifestURI.toString());
		if (manifest != null) {
			throw new IllegalArgumentException("URI already exists: " + manifestURI);
		}
		manifest = manifestModel.createIndividual(manifestURI.toString(), manifestClass);
		Individual ro = manifestModel.createIndividual(researchObjectURI.toString(), researchObjectClass);

		manifestModel.add(ro, isDescribedBy, manifest);
		manifestModel.add(ro, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
		Individual agent = manifestModel.createIndividual(foafAgentClass);
		manifestModel.add(agent, foafName, user.getName());
		manifestModel.add(ro, DCTerms.creator, agent);

		manifestModel.add(manifest, describes, ro);
		manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
		agent = manifestModel.createIndividual(foafAgentClass);
		manifestModel.add(agent, foafName, SERVICE_NAME);
		manifestModel.add(manifest, DCTerms.creator, agent);
	}


	@Override
	public void updateManifest(URI manifestURI, InputStream is, RDFFormat rdfFormat)
	{
		//TODO validate the manifest?
		addNamedGraph(manifestURI, is, rdfFormat);

		//
		//		// leave only one dcterms:created - the earliest
		//		Individual manifest = manifestModel.getIndividual(manifestURI.toString());
		//		NodeIterator it = manifestModel.listObjectsOfProperty(manifest, DCTerms.created);
		//		Calendar earliest = null;
		//		while (it.hasNext()) {
		//			Calendar created = ((XSDDateTime) it.next().asLiteral().getValue()).asCalendar();
		//			if (earliest == null || created.before(earliest))
		//				earliest = created;
		//		}
		//		if (earliest != null) {
		//			manifestModel.removeAll(manifest, DCTerms.created, null);
		//			manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(earliest));
		//		}
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
		return getNamedGraph(manifestURI, rdfFormat);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addResource(java.net.URI,
	 * java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)
	 */
	@Override
	public void addResource(URI roURI, URI resourceURI, ResourceInfo resourceInfo)
	{
		resourceURI = resourceURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(roURI.normalize()));
		Individual ro = manifestModel.getIndividual(roURI.toString());
		if (ro == null) {
			throw new IllegalArgumentException("URI not found: " + roURI);
		}
		Individual resource = manifestModel.createIndividual(resourceURI.toString(), resourceClass);
		manifestModel.add(ro, aggregates, resource);
		if (resourceInfo != null) {
			if (resourceInfo.getName() != null) {
				manifestModel.add(resource, name, manifestModel.createTypedLiteral(resourceInfo.getName()));
			}
			manifestModel.add(resource, filesize, manifestModel.createTypedLiteral(resourceInfo.getSizeInBytes()));
			if (resourceInfo.getChecksum() != null && resourceInfo.getDigestMethod() != null) {
				manifestModel.add(resource, checksum, manifestModel.createResource(String.format("urn:%s:%s",
					resourceInfo.getDigestMethod(), resourceInfo.getChecksum())));
			}
		}
		manifestModel.add(resource, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
		Individual agent = manifestModel.createIndividual(foafAgentClass);
		manifestModel.add(agent, foafName, user.getName());
		manifestModel.add(resource, DCTerms.creator, agent);

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResource(java.net
	 * .URI, java.net.URI)
	 */
	@Override
	public void removeResource(URI roURI, URI resourceURI)
	{
		resourceURI = resourceURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(roURI.normalize()));
		Individual ro = manifestModel.getIndividual(roURI.toString());
		if (ro == null) {
			throw new IllegalArgumentException("URI not found: " + roURI);
		}
		Individual resource = manifestModel.getIndividual(resourceURI.toString());
		if (resource == null) {
			throw new IllegalArgumentException("URI not found: " + resourceURI);
		}
		manifestModel.remove(ro, aggregates, resource);

		StmtIterator it = manifestModel.listStatements(null, aggregates, resource);
		if (!it.hasNext()) {
			manifestModel.removeAll(resource, null, null);
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
	public InputStream getResource(URI roURI, URI resourceURI, RDFFormat rdfFormat)
	{
		resourceURI = resourceURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(roURI.normalize()));
		Individual resource = manifestModel.getIndividual(resourceURI.toString());
		if (resource == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getResourceQueryTmpl, resourceURI.toString());
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, manifestModel);
		Model resultModel = qexec.execDescribe();
		qexec.close();

		resultModel.removeNsPrefix("xml");

		resultModel.write(out, rdfFormat.getName().toUpperCase(), roURI.toString());
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
	public InputStream getNamedGraph(URI namedGraphURI, RDFFormat rdfFormat)
	{
		namedGraphURI = namedGraphURI.normalize();
		if (!graphset.containsGraph(namedGraphURI.toString())) {
			return null;
		}
		NamedGraphSet tmpGraphSet = new NamedGraphSetImpl();
		if (rdfFormat.supportsContexts()) {
			addGraphsRecursively(tmpGraphSet, namedGraphURI);
		}
		else {
			tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tmpGraphSet.write(out, rdfFormat.getName().toUpperCase(), null);
		return new ByteArrayInputStream(out.toByteArray());
	}


	private void addGraphsRecursively(NamedGraphSet tmpGraphSet, URI namedGraphURI)
	{
		tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));

		OntModel annotationModel = createOntModelForNamedGraph(namedGraphURI);
		NodeIterator it = annotationModel.listObjectsOfProperty(body);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			URI childURI = URI.create(annotationBodyRef.asResource().getURI());
			if (graphset.containsGraph(childURI.toString()) && !tmpGraphSet.containsGraph(childURI.toString())) {
				addGraphsRecursively(tmpGraphSet, childURI);
			}
		}
	}


	@Override
	public Set<URI> findResearchObjects(URI partialURI)
	{
		String queryString = String.format(findResearchObjectsQueryTmpl, partialURI != null ? partialURI.normalize()
				.toString() : "");
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, createOntModelForAllNamedGraphs());
		ResultSet results = qe.execSelect();
		Set<URI> uris = new HashSet<URI>();
		while (results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			Resource manifest = solution.getResource("ro");
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
		ontModel.addSubModel(defaultModel);
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


	@Override
	public boolean isRoFolder(URI researchObjectURI, URI resourceURI)
	{
		resourceURI = resourceURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObjectURI.normalize()));
		Individual resource = manifestModel.getIndividual(resourceURI.toString());
		if (resource == null) {
			return false;
		}
		return resource.hasRDFType(roFolderClass);
	}


	@Override
	public void addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat)
	{
		OntModel namedGraphModel = createOntModelForNamedGraph(graphURI);
		namedGraphModel.removeAll();
		namedGraphModel.read(inputStream, graphURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());
	}


	@Override
	public boolean isROMetadataNamedGraph(URI researchObjectURI, URI graphURI)
	{
		Node manifest = Node.createURI(getManifestURI(researchObjectURI).toString());
		Node body = Node.createURI(this.body.getURI());
		Node annBody = Node.createURI(graphURI.toString());
		boolean isManifest = getManifestURI(researchObjectURI).equals(graphURI);
		boolean isAnnotationBody = graphset.containsQuad(new Quad(manifest, Node.ANY, body, annBody));
		return isManifest || isAnnotationBody;
	}


	@Override
	public void removeNamedGraph(URI researchObjectURI, URI graphURI)
	{
		graphURI = graphURI.normalize();
		if (!graphset.containsGraph(graphURI.toString())) {
			throw new IllegalArgumentException("URI not found: " + graphURI);
		}
		OntModel manifestModel = createOntModelForNamedGraph(graphURI);

		NodeIterator it = manifestModel.listObjectsOfProperty(body);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			//TODO make sure that this named graph is internal
			if (graphset.containsGraph(annotationBodyRef.asResource().getURI())) {
				removeNamedGraph(researchObjectURI, URI.create(annotationBodyRef.asResource().getURI()));
			}
		}
		graphset.removeGraph(graphURI.toString());
	}


	private OntModel createOntModelForAllNamedGraphs()
	{
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
			graphset.asJenaModel(DEFAULT_NAMED_GRAPH_URI.toString()));
		ontModel.addSubModel(defaultModel);
		return ontModel;
	}


	/**
	 * 
	 * @param roURI must end with /
	 * @return
	 */
	private URI getManifestURI(URI roURI)
	{
		return roURI.resolve(".ro/manifest");
	}


	@Override
	public void removeResearchObject(URI researchObjectURI)
	{
		removeNamedGraph(researchObjectURI, getManifestURI(researchObjectURI));
	}


	@Override
	public boolean containsNamedGraph(URI graphURI)
	{
		return graphset.containsGraph(graphURI.toString());
	}

}
