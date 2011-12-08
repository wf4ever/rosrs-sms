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

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(SemanticMetadataServiceImpl.class);

	private static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";

	private static final String RO_NAMESPACE = "http://purl.org/wf4ever/ro#";

	private static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

	private static final String AO_NAMESPACE = "http://purl.org/ao/core/";

	private static final URI DEFAULT_NAMED_GRAPH_URI = URI.create("sms");

	private static final PrefixMapping standardNamespaces = PrefixMapping.Factory.create()
			.setNsPrefix("ore", ORE_NAMESPACE).setNsPrefix("ro", RO_NAMESPACE).setNsPrefix("dcterms", DCTerms.NS)
			.setNsPrefix("foaf", FOAF_NAMESPACE).lock();

	private final NamedGraphSet graphset;

	/**
	 * For storing triples that don't belong to any RO, the default graph. 
	 */
	private final OntModel defaultModel;

	private final OntClass researchObjectClass;

	private final OntClass manifestClass;

	private final OntClass foafAgentClass;

	private final OntClass resourceClass;

	private final Property describes;

	private final Property aggregates;

	private final Property foafName;

	private final Property name;

	private final Property filesize;

	private final Property checksum;

	private final Property hasTopic;

	private final String getResourceQueryTmpl = "DESCRIBE <%s> WHERE { }";

	private final String findManifestsQueryTmpl = "PREFIX ro: <" + RO_NAMESPACE + "> SELECT ?manifest "
			+ "WHERE { ?manifest a ro:Manifest. FILTER regex(str(?manifest), \"^%s\") . }";

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
		tmpModel.read(RO_NAMESPACE);

		defaultModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, tmpModel);

		OntModel foafModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		foafModel.read(FOAF_NAMESPACE, null);
		defaultModel.addSubModel(foafModel);

		researchObjectClass = defaultModel.getOntClass(RO_NAMESPACE + "ResearchObject");
		manifestClass = defaultModel.getOntClass(RO_NAMESPACE + "Manifest");
		resourceClass = defaultModel.getOntClass(ORE_NAMESPACE + "AggregatedResource");

		name = defaultModel.getProperty(RO_NAMESPACE + "name");
		filesize = defaultModel.getProperty(RO_NAMESPACE + "filesize");
		checksum = defaultModel.getProperty(RO_NAMESPACE + "checksum");
		describes = defaultModel.getProperty(ORE_NAMESPACE + "describes");
		aggregates = defaultModel.getProperty(ORE_NAMESPACE + "aggregates");
		foafAgentClass = defaultModel.getOntClass(FOAF_NAMESPACE + "Agent");
		foafName = defaultModel.getProperty(FOAF_NAMESPACE + "name");
		hasTopic = defaultModel.getProperty(AO_NAMESPACE + "hasTopic");

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
	public void createManifest(URI manifestURI)
	{
		manifestURI = manifestURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
		Individual manifest = manifestModel.getIndividual(manifestURI.toString());
		if (manifest != null) {
			throw new IllegalArgumentException("URI already exists");
		}
		manifest = manifestModel.createIndividual(manifestURI.toString(), manifestClass);
		Individual ro = manifestModel.createIndividual(manifestURI.toString() + "#ro", researchObjectClass);
		manifestModel.add(manifest, describes, ro);
		manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));

		Individual agent = manifestModel.createIndividual(foafAgentClass);
		manifestModel.add(agent, foafName, user.getName());
		manifestModel.add(manifest, DCTerms.creator, agent);
	}


	@Override
	public void createManifest(URI manifestURI, InputStream is, RDFFormat rdfFormat)
	{
		manifestURI = manifestURI.normalize();
		OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
		manifestModel.read(is, manifestURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());

		// leave only one dcterms:created - the earliest
		Individual manifest = manifestModel.getIndividual(manifestURI.toString());
		NodeIterator it = manifestModel.listObjectsOfProperty(manifest, DCTerms.created);
		Calendar earliest = null;
		while (it.hasNext()) {
			Calendar created = ((XSDDateTime) it.next().asLiteral().getValue()).asCalendar();
			if (earliest == null || created.before(earliest))
				earliest = created;
		}
		if (earliest != null) {
			manifestModel.removeAll(manifest, DCTerms.created, null);
			manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(earliest));
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResearchObject(java
	 * .net.URI)
	 */
	@Override
	public void removeManifest(URI manifestURI, URI baseURI)
	{
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
	public void addResource(URI manifestURI, URI resourceURI, ResourceInfo resourceInfo)
	{
		manifestURI = manifestURI.normalize();
		resourceURI = resourceURI.normalize();
		Individual manifest = defaultModel.getIndividual(manifestURI.toString());
		if (manifest == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual ro = defaultModel.getIndividual(manifestURI.toString() + "#ro");
		if (ro == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual resource = defaultModel.createIndividual(resourceURI.toString(), resourceClass);
		defaultModel.add(ro, aggregates, resource);
		if (resourceInfo != null) {
			if (resourceInfo.getName() != null) {
				defaultModel.add(resource, name, defaultModel.createTypedLiteral(resourceInfo.getName()));
			}
			defaultModel.add(resource, filesize, defaultModel.createTypedLiteral(resourceInfo.getSizeInBytes()));
			if (resourceInfo.getChecksum() != null && resourceInfo.getDigestMethod() != null) {
				defaultModel.add(
					resource,
					checksum,
					defaultModel.createResource(String.format("urn:%s:%s", resourceInfo.getDigestMethod(),
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
		Individual ro = defaultModel.getIndividual(manifestURI.toString() + "#ro");
		if (ro == null) {
			throw new IllegalArgumentException("URI not found");
		}
		Individual resource = defaultModel.getIndividual(resourceURI.toString());
		if (resource == null) {
			throw new IllegalArgumentException("URI not found");
		}
		defaultModel.remove(ro, aggregates, resource);

		StmtIterator it = defaultModel.listStatements(null, aggregates, resource);
		if (!it.hasNext()) {
			defaultModel.removeAll(resource, null, null);
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
		Individual resource = defaultModel.getIndividual(resourceURI.toString());
		if (resource == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String queryString = String.format(getResourceQueryTmpl, resourceURI.toString());
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, defaultModel);
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
	public InputStream getNamedGraph(URI namedGraphURI, RDFFormat rdfFormat)
	{
		namedGraphURI = namedGraphURI.normalize();
		if (!graphset.containsGraph(namedGraphURI.toString())) {
			return null;
		}
		NamedGraphSet tmpGraphSet = new NamedGraphSetImpl();
		addGraphsRecursively(tmpGraphSet, namedGraphURI);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		graphset.write(out, rdfFormat.getName().toUpperCase(), null);
		return new ByteArrayInputStream(out.toByteArray());
	}


	private void addGraphsRecursively(NamedGraphSet tmpGraphSet, URI namedGraphURI)
	{
		tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));

		OntModel annotationModel = createOntModelForNamedGraph(namedGraphURI);
		NodeIterator it = annotationModel.listObjectsOfProperty(hasTopic);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			URI childURI = URI.create(annotationBodyRef.asResource().getURI());
			if (graphset.containsGraph(childURI.toString()) && !tmpGraphSet.containsGraph(childURI.toString())) {
				addGraphsRecursively(tmpGraphSet, childURI);
			}
		}
	}


	@Override
	public Set<URI> findManifests(URI partialURI)
	{
		partialURI = partialURI.normalize();
		String queryString = String.format(findManifestsQueryTmpl, partialURI.toString());
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, defaultModel);
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
	public boolean isRoFolder(URI resourceURI)
	{
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat)
	{
		OntModel namedGraphModel = createOntModelForNamedGraph(graphURI);
		namedGraphModel.read(inputStream, graphURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());
	}


	@Override
	public boolean isNamedGraph(URI graphURI)
	{
		return graphset.containsGraph(graphURI.toString());
	}


	@Override
	public void removeNamedGraph(URI graphURI, URI baseURI)
	{
		graphURI = graphURI.normalize();
		if (!graphset.containsGraph(graphURI.toString())) {
			throw new IllegalArgumentException("URI not found");
		}
		OntModel manifestModel = createOntModelForNamedGraph(graphURI);

		NodeIterator it = manifestModel.listObjectsOfProperty(hasTopic);
		while (it.hasNext()) {
			RDFNode annotationBodyRef = it.next();
			//TODO make sure that this named graph is internal
			if (graphset.containsGraph(annotationBodyRef.asResource().getURI())) {
				removeNamedGraph(URI.create(annotationBodyRef.asResource().getURI()), baseURI);
			}
		}
		graphset.removeGraph(graphURI.toString());
	}

}
