/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImplTestOld
{

	private static final Logger log = Logger.getLogger(SemanticMetadataServiceImplTestOld.class);

	private final URI baseURI = URI.create("http://example.org/ROs/ro1/");

	private final static URI manifestURI = URI.create("http://example.org/ROs/ro1/manifest");

	private final static URI manifest2URI = URI.create("http://example.org/ROs/ro2/manifest");

	private final URI researchObjectURI = URI.create("http://example.org/ROs/ro1/manifest#ro");

	private final static URI annotationsURI = URI.create("http://example.org/ROs/ro1/annotations");

	private final static URI resource1URI = URI.create("http://example.org/ROs/ro1/foo/bar.txt");

	private final ResourceInfo resource1Info = new ResourceInfo("bar.txt", "ABC123455666344E", 646365L, "SHA1");

	private final static URI resource2URI = URI.create("http://workflows.org/a/workflow.scufl");

	private final URI annotation1URI = URI.create("http://example.org/ROs/ro1/annotations#myTitle");

	private final URI annotationBody1URI = URI.create("http://example.org/ROs/ro1/annotations/myTitleContent");

	private static Map<URI, Map<URI, String>> annotation1Body;

	private final URI annotation2URI = URI.create("http://example.org/ROs/ro1/annotations#someComments");

	private final URI annotationBody2URI = URI.create("http://example.org/ROs/ro1/annotations/someComments");

	private static Map<URI, Map<URI, String>> annotation2Body;

	private final ResourceInfo resource2Info = new ResourceInfo("a workflow", "A0987654321EDCB", 6L, "MD5");

	private final UserProfile userProfile = new UserProfile("jank", "pass", "Jan Kowalski", false);

	private static final String RO_NAMESPACE = "http://www.wf4ever-project.org/vocab/ro#";

	private final Property foafName = ModelFactory.createDefaultModel()
			.createProperty("http://xmlns.com/foaf/0.1/name");

	private final Property name = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "name");

	private final Property filesize = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "filesize");

	private final Property checksum = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "checksum");

	private final Property annotatesResource = ModelFactory.createDefaultModel().createProperty(
		"http://purl.org/ao/core/annotatesResource");

	private final Property hasTopic = ModelFactory.createDefaultModel().createProperty(
		"http://purl.org/ao/core/hasTopic");

	private final Property aggregates = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/aggregates");

	private final Property proxyFor = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/proxyFor");

	private final Property proxyIn = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/proxyIn");

	private final Property pavCreatedOn = ModelFactory.createDefaultModel().createProperty(
		"http://purl.org/pav/createdOn");

	private final Property pavCreatedBy = ModelFactory.createDefaultModel().createProperty(
		"http://purl.org/pav/createdBy");


	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void setup()
	{
		annotation1Body = new HashMap<URI, Map<URI, String>>();
		annotation1Body.put(resource1URI,
			ArrayUtils.toMap(new Object[][] { { URI.create("http://purl.org/dc/terms/title"), "Foobar"}}));
		annotation2Body = new HashMap<URI, Map<URI, String>>();
		annotation2Body.put(
			resource1URI,
			ArrayUtils.toMap(new Object[][] { { URI.create("http://purl.org/dc/terms/title"), "A test"},
					{ URI.create("http://purl.org/dc/terms/licence"), "GPL"}}));
		annotation2Body.put(
			resource2URI,
			ArrayUtils.toMap(new Object[][] { { URI.create("http://purl.org/dc/terms/description"),
					"Something interesting"}}));
	}


	@Before
	public void setupTest()
	{
		cleanData();
	}


	@AfterClass
	public static void cleanup()
	{
		cleanData();
	}


	private static void cleanData()
	{
		SemanticMetadataService sms = null;
		try {
			sms = new SemanticMetadataServiceImpl();
			try {
				sms.removeManifest(manifestURI);
			}
			catch (IllegalArgumentException e) {
				// nothing
			}
			try {
				sms.removeManifest(manifest2URI);
			}
			catch (IllegalArgumentException e) {
				// nothing
			}
			try {
				sms.deleteAllAnnotationsWithBodies(annotationsURI);
			}
			catch (IllegalArgumentException e) {
				// nothing
			}
		}
		catch (ClassNotFoundException | IOException | NamingException | SQLException e) {
			e.printStackTrace();
		}
		finally {
			if (sms != null)
				sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#SemanticMetadataServiceImpl()}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testSemanticMetadataServiceImpl()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);
		}
		finally {
			sms.close();
		}

		SemanticMetadataService sms2 = new SemanticMetadataServiceImpl();
		try {
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);

			model.read(sms2.getManifest(manifestURI, RDFFormat.RDFXML), null);
			Individual manifest = model.getIndividual(manifestURI.toString());
			Individual ro = model.getIndividual(researchObjectURI.toString());
			Assert.assertNotNull("Persistent manifest must contain ro:Manifest", manifest);
			Assert.assertNotNull("Persistent manifest must contain ro:ResearchObject", ro);
			Assert.assertTrue("Manifest must be a ro:Manifest", manifest.hasRDFType(RO_NAMESPACE + "Manifest"));
			Assert.assertTrue("RO must be a ro:ResearchObject", ro.hasRDFType(RO_NAMESPACE + "ResearchObject"));

			Literal createdLiteral = manifest.getPropertyValue(DCTerms.created).asLiteral();
			Assert.assertNotNull("Manifest must contain dcterms:created", createdLiteral);

			Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
			Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);

			Resource annotations = manifest.getSeeAlso();
			Assert.assertNotNull("Manifest must contain reference to annotations", annotations);
			Assert.assertEquals("Annotations URI must be correct", annotationsURI.toString(), annotations.getURI());

			model.read(sms2.getAllAnnotations(annotationsURI, RDFFormat.RDFXML), null);
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
		}
		finally {
			sms2.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testCreateManifest()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			try {
				sms.createManifest(manifestURI, userProfile);
				fail("Should have thrown an exception");
			}
			catch (IllegalArgumentException e) {
				// good
			}
			Assert.assertNotNull("Annotations after creating a manifest must not be null",
				sms.getAllAnnotations(annotationsURI, RDFFormat.RDFXML));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObjectAsCopy(java.net.URI, java.net.URI)}
	 * .
	 */
	@Test
	@Ignore
	public final void testCreateResearchObjectAsCopy()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeManifest(java.net.URI)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testRemoveManifest()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

			sms.removeManifest(manifestURI);
			try {
				sms.removeManifest(manifestURI);
				fail("Should have thrown an exception");
			}
			catch (IllegalArgumentException e) {
				// good
			}

			Assert.assertNull("Get deleted annotation must return null",
				sms.getAnnotation(annotation1URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation body must return null",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation must return null",
				sms.getAnnotation(annotation2URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation body must return null",
				sms.getNamedGraph(annotationBody2URI, RDFFormat.RDFXML));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getManifest(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * 
	 * @throws IOException
	 * @throws ParseException
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetManifest()
		throws IOException, ParseException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when manifest does not exist",
				sms.getManifest(manifestURI, RDFFormat.RDFXML));

			Calendar before = Calendar.getInstance();
			sms.createManifest(manifestURI, userProfile);
			Calendar after = Calendar.getInstance();
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);

			Individual manifest = model.getIndividual(manifestURI.toString());
			Individual ro = model.getIndividual(researchObjectURI.toString());
			Assert.assertNotNull("Manifest must contain ro:Manifest", manifest);
			Assert.assertNotNull("Manifest must contain ro:ResearchObject", ro);
			Assert.assertTrue("Manifest must be a ro:Manifest", manifest.hasRDFType(RO_NAMESPACE + "Manifest"));
			Assert.assertTrue("RO must be a ro:ResearchObject", ro.hasRDFType(RO_NAMESPACE + "ResearchObject"));

			Literal createdLiteral = manifest.getPropertyValue(DCTerms.created).asLiteral();
			Assert.assertNotNull("Manifest must contain dcterms:created", createdLiteral);
			Assert.assertEquals("Date type is xsd:dateTime", XSDDatatype.XSDdateTime, createdLiteral.getDatatype());
			Calendar created = ((XSDDateTime) createdLiteral.getValue()).asCalendar();
			Assert.assertTrue("Created is a valid date", !before.after(created) && !after.before(created));

			Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
			Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);
			Individual creator = creatorResource.as(Individual.class);
			Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
			Assert.assertEquals("Creator name must be correct", userProfile.getName(),
				creator.getPropertyValue(foafName).asLiteral().getString());

			Resource annotations = manifest.getSeeAlso();
			Assert.assertNotNull("Manifest must contain reference to annotations", annotations);
			Assert.assertEquals("Annotations URI must be correct", annotationsURI.toString(), annotations.getURI());

			log.debug(IOUtils.toString(sms.getManifest(manifestURI, RDFFormat.TURTLE), "UTF-8"));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI, java.io.InputStream, pl.psnc.dl.wf4everdlibra.UserProfile)}
	 * .
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testUpdateManifest()
		throws IOException, ParseException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);

			InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
			sms.createManifest(manifestURI, is, RDFFormat.TURTLE, userProfile);

			log.debug(IOUtils.toString(sms.getManifest(manifestURI, RDFFormat.TURTLE), "UTF-8"));

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);

			Individual manifest = model.getIndividual(manifestURI.toString());
			Individual ro = model.getIndividual(researchObjectURI.toString());

			Assert.assertEquals("Created has been updated", "2011-07-14T15:01:14Z",
				manifest.getPropertyValue(DCTerms.created).asLiteral().getString());

			Set<String> creators = new HashSet<String>();
			String creatorsQuery = String.format("PREFIX dcterms: <%s> PREFIX foaf: <%s> SELECT ?name "
					+ "WHERE { <%s> dcterms:creator ?x . ?x foaf:name ?name . }", DCTerms.NS,
				"http://xmlns.com/foaf/0.1/", manifestURI.toString());
			Query query = QueryFactory.create(creatorsQuery);
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					creators.add(results.nextSolution().getLiteral("name").getString());
				}
			}
			finally {
				qexec.close();
			}

			Assert.assertTrue("New creator has been added", creators.contains("Stian Soiland-Reyes"));
			Assert.assertTrue("Old creator has been preserved", creators.contains(userProfile.getName()));

			Assert.assertTrue("RO must aggregate resources",
				model.contains(ro, aggregates, model.createResource("http://example.com/workflow.scufl2")));
			Assert.assertTrue("RO must aggregate resources",
				model.contains(ro, aggregates, model.createResource("http://example.org/ROs/ro1/input.txt")));
			Assert.assertTrue("RO must aggregate resources",
				model.contains(ro, aggregates, model.createResource("http://example.org/ROs/ro1/output.txt")));
			validateProxy(model, manifest, manifestURI.toString() + "#workflowProxy",
				"http://example.com/workflow.scufl2");
			validateProxy(model, manifest, manifestURI.toString() + "#inputProxy",
				"http://example.org/ROs/ro1/input.txt");
			validateProxy(model, manifest, manifestURI.toString() + "#outputProxy",
				"http://example.org/ROs/ro1/output.txt");
		}
		finally {
			sms.close();
		}
	}


	private void validateProxy(OntModel model, Individual manifest, String proxyURI, String proxyForURI)
	{
		Individual proxy = model.getIndividual(proxyURI);
		Assert.assertNotNull("Manifest must contain " + proxyURI, proxy);
		Assert.assertTrue(String.format("Proxy %s must be a ore:Proxy", proxyURI),
			proxy.hasRDFType("http://www.openarchives.org/ore/terms/Proxy"));
		Assert.assertEquals("Proxy for must be valid", proxyForURI, proxy.getPropertyResourceValue(proxyFor).getURI());
		Assert.assertEquals("Proxy in must be valid", researchObjectURI.toString(),
			proxy.getPropertyResourceValue(proxyIn).getURI());
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addResource(java.net.URI, java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testAddResource()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addResource(manifestURI, resource1URI, null);
			sms.addResource(manifestURI, resource1URI, new ResourceInfo(null, null, 0, null));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResource(java.net.URI, java.net.URI)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testRemoveResource()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.removeResource(manifestURI, resource1URI);
			try {
				sms.removeResource(manifestURI, resource1URI);
				fail("Should have thrown an exception");
			}
			catch (IllegalArgumentException e) {
				// good
			}
			sms.removeResource(manifestURI, resource2URI);
			try {
				sms.removeResource(manifestURI, resource2URI);
				fail("Should have thrown an exception");
			}
			catch (IllegalArgumentException e) {
				// good
			}
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResource(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * @throws URISyntaxException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetResource()
		throws URISyntaxException, ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when resource does not exist",
				sms.getResource(resource1URI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getResource(resource1URI, RDFFormat.RDFXML), null);
			verifyResource(model, resource1URI, resource1Info);

			model.read(sms.getResource(resource2URI, RDFFormat.RDFXML), null);
			verifyResource(model, resource2URI, resource2Info);
		}
		finally {
			sms.close();
		}
	}


	/**
	 * @param model
	 * @param resourceInfo 
	 * @param resourceURI 
	 * @throws URISyntaxException
	 */
	private void verifyResource(OntModel model, URI resourceURI, ResourceInfo resourceInfo)
		throws URISyntaxException
	{
		Individual resource = model.getIndividual(resourceURI.toString());
		Assert.assertNotNull("Resource cannot be null", resource);
		Assert.assertTrue(String.format("Resource %s must be a ore:AggregatedResource", resourceURI),
			resource.hasRDFType("http://www.openarchives.org/ore/terms/AggregatedResource"));

		Literal nameLiteral = resource.getPropertyValue(name).asLiteral();
		Assert.assertNotNull("Resource must contain ro:name", nameLiteral);
		Assert.assertEquals("Name type is xsd:string", XSDDatatype.XSDstring, nameLiteral.getDatatype());
		Assert.assertEquals("Name is valid", resourceInfo.getName(), nameLiteral.asLiteral().getString());

		Literal filesizeLiteral = resource.getPropertyValue(filesize).asLiteral();
		Assert.assertNotNull("Resource must contain ro:filesize", filesizeLiteral);
		Assert.assertEquals("Filesize type is xsd:long", XSDDatatype.XSDlong, filesizeLiteral.getDatatype());
		Assert.assertEquals("Filesize is valid", resourceInfo.getSizeInBytes(), filesizeLiteral.asLiteral().getLong());

		Resource checksumResource = resource.getPropertyValue(checksum).asResource();
		Assert.assertNotNull("Resource must contain ro:checksum", checksumResource);
		URI checksumURN = new URI(checksumResource.getURI());
		Pattern p = Pattern.compile("urn:(\\w+):([0-9a-fA-F]+)");
		Matcher m = p.matcher(checksumURN.toString());
		Assert.assertTrue("Checksum can be parsed", m.matches());
		Assert.assertEquals("Digest method is correct", resourceInfo.getDigestMethod(), m.group(1));
		Assert.assertEquals("Checksum is correct", resourceInfo.getChecksum(), m.group(2));
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addAnnotation(java.net.URI, java.net.URI, java.net.URI, java.util.Map)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testAddAnnotation()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#deleteAnnotationWithBody(java.net.URI)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testDeleteAnnotationWithBody()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);

			sms.deleteAnnotationWithBody(annotationBody1URI);
			Assert.assertNull("Get deleted annotation must return null",
				sms.getAnnotation(annotation1URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation body must return null",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#deleteAllAnnotationsWithBodies(java.net.URI)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testDeleteAllAnnotationsWithBodies()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

			sms.deleteAllAnnotationsWithBodies(annotationsURI);
			Assert.assertNull("Get deleted annotation must return null",
				sms.getAnnotation(annotation1URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation body must return null",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation must return null",
				sms.getAnnotation(annotation2URI, RDFFormat.RDFXML));
			Assert.assertNull("Get deleted annotation body must return null",
				sms.getNamedGraph(annotationBody2URI, RDFFormat.RDFXML));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotation(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetAnnotation()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when annotation does not exist",
				sms.getAnnotation(annotation1URI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getAnnotation(annotation1URI, RDFFormat.RDFXML), null);
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			model.read(sms.getAnnotation(annotation2URI, RDFFormat.RDFXML), null);
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
		}
		finally {
			sms.close();
		}
	}


	private void verifyAnnotation(OntModel model, URI annotationURI, URI annotationBodyURI,
			Set<URI> annotatedResourcesURIs)
	{
		Individual annotation = model.getIndividual(annotationURI.toString());
		Assert.assertNotNull("Annotation cannot be null", annotation);
		Assert.assertTrue(String.format("Annotation %s must be a ro:GraphAnnotation", annotationURI),
			annotation.hasRDFType(RO_NAMESPACE + "GraphAnnotation"));

		for (URI annotatedResourceURI : annotatedResourcesURIs) {
			Resource resource = model.createResource(annotatedResourceURI.toString());
			Assert.assertTrue(
				String.format("Annotation %s must annotate resource %s", annotationURI.toString(),
					annotatedResourceURI.toString()), model.contains(annotation, annotatesResource, resource));
		}

		Resource annotationBody = annotation.getPropertyValue(hasTopic).asResource();
		Assert.assertNotNull("Annotation must contain annotation body", annotationBody);
		Assert.assertEquals("Annotation body must be valid", annotationBodyURI.toString(), annotationBody.getURI());

		Literal createdLiteral = annotation.getPropertyValue(pavCreatedOn).asLiteral();
		Assert.assertNotNull("Manifest must contain pav:createdOn", createdLiteral);
		Assert.assertEquals("Date type is xsd:dateTime", XSDDatatype.XSDdateTime, createdLiteral.getDatatype());
		//		Calendar created = ((XSDDateTime) createdLiteral.getValue()).asCalendar();
		//		Assert.assertTrue("Created is a valid date", !before.after(created) && !after.before(created));

		Resource creatorResource = annotation.getPropertyResourceValue(pavCreatedBy);
		Assert.assertNotNull("Annotation must contain pav:createdBy", creatorResource);
		Individual creator = creatorResource.as(Individual.class);
		Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
		Assert.assertEquals("Creator name must be correct", userProfile.getName(), creator.getPropertyValue(foafName)
				.asLiteral().getString());
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getNamedGraph(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetAnnotationBody()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when annotation body does not exist",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);
		}
		finally {
			sms.close();
		}

		SemanticMetadataService sms2 = new SemanticMetadataServiceImpl();
		try {
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms2.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML), null);
			verifyAnnotationBody(model, annotation1Body);
			model.read(sms2.getNamedGraph(annotationBody2URI, RDFFormat.RDFXML), null);
			verifyAnnotationBody(model, annotation2Body);
		}
		finally {
			sms2.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addAnnotation(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testAddAnnotationFromGraph()
		throws IOException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when annotation body does not exist",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));

			InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation2URI, annotationBody2URI, is, RDFFormat.TURTLE, userProfile);

			log.debug(IOUtils.toString(sms.getNamedGraph(annotationBody2URI, RDFFormat.TURTLE), "UTF-8"));

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getNamedGraph(annotationBody2URI, RDFFormat.RDFXML), null);
			verifyAnnotationBody(model, annotation2Body);
		}
		finally {
			sms.close();
		}
	}


	private void verifyAnnotationBody(OntModel model, Map<URI, Map<URI, String>> annotationBody)
	{
		for (Map.Entry<URI, Map<URI, String>> entry : annotationBody.entrySet()) {
			Resource subject = model.createResource(entry.getKey().toString());
			for (Map.Entry<URI, String> entry2 : entry.getValue().entrySet()) {
				Property property = model.createProperty(entry2.getKey().toString());
				Assert.assertTrue(
					String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
						property.getURI(), entry2.getValue()), model.contains(subject, property, entry2.getValue()));
			}
		}
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAllAnnotations(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}.
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetAllAnnotations()
		throws IOException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when annotations do not exist",
				sms.getAllAnnotations(annotationsURI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getAllAnnotations(annotationsURI, RDFFormat.RDFXML), null);
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());

			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getAllAnnotations(annotationsURI, RDFFormat.TURTLE), null, "TURTLE");
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAllAnnotationsWithBodies(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}.
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetAllAnnotationsWithBodies()
		throws IOException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			Assert.assertNull("Returns null when annotations do not exist",
				sms.getAllAnnotations(annotationsURI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
			sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getAllAnnotationsWithBodies(annotationsURI, RDFFormat.RDFXML), null);
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
			verifyAnnotationBody(model, annotation1Body);
			verifyAnnotationBody(model, annotation2Body);

			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
			model.read(sms.getAllAnnotationsWithBodies(annotationsURI, RDFFormat.TURTLE), null, "TURTLE");
			verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
			verifyAnnotationBody(model, annotation1Body);
			verifyAnnotationBody(model, annotation2Body);

			NamedGraphSet ngset = new NamedGraphSetImpl();
			ngset.read(sms.getAllAnnotationsWithBodies(annotationsURI, RDFFormat.TRIG), "TRIG", baseURI.toString());

			Assert.assertTrue("Graphset contains annotations as default graph",
				ngset.containsGraph(annotationsURI.toString()));
			OntModel graphModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
				ModelFactory.createModelForGraph(ngset.getGraph(annotationsURI.toString())));
			verifyAnnotation(graphModel, annotation1URI, annotationBody1URI, annotation1Body.keySet());
			verifyAnnotation(graphModel, annotation2URI, annotationBody2URI, annotation2Body.keySet());

			Assert.assertTrue("Graphset contains annotation body", ngset.containsGraph(annotationBody1URI.toString()));
			OntModel graphModel1 = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
				ModelFactory.createModelForGraph(ngset.getGraph(annotationBody1URI.toString())));
			verifyAnnotationBody(graphModel1, annotation1Body);

			Assert.assertTrue("Graphset contains annotation body", ngset.containsGraph(annotationBody2URI.toString()));
			OntModel graphModel2 = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
				ModelFactory.createModelForGraph(ngset.getGraph(annotationBody2URI.toString())));
			verifyAnnotationBody(graphModel2, annotation2Body);
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findResearchObjects(java.lang.String, java.util.Map)}
	 * .
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testFindManifests()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		try {
			sms.createManifest(manifestURI, userProfile);
			sms.createManifest(manifest2URI, userProfile);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);

			Set<URI> expected = new HashSet<URI>();
			expected.add(manifestURI);
			Assert.assertEquals("Find with manifest URI", expected, sms.findManifests(manifestURI));

			expected.clear();
			expected.add(manifestURI);
			Assert.assertEquals("Find with base URI", expected, sms.findManifests(manifestURI.resolve(".")));

			expected.clear();
			expected.add(manifestURI);
			expected.add(manifest2URI);
			Assert.assertEquals("Find with base of base URI", expected, sms.findManifests(manifestURI.resolve("..")));
		}
		finally {
			sms.close();
		}
	}
}
