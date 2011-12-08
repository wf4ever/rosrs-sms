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
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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

/**
 * @author piotrhol
 *
 */
public class SemanticMetadataServiceImplTest
{

	private static final Logger log = Logger.getLogger(SemanticMetadataServiceImplTest.class);

	private final static URI baseURI = URI.create("http://example.org/ROs/ro1/");

	private final static URI manifestURI = URI.create("http://example.org/ROs/ro1/manifest");

	private final static URI manifest2URI = URI.create("http://example.org/ROs/ro2/manifest");

	private final URI researchObjectURI = URI.create("http://example.org/ROs/ro1/manifest#ro");

	private final static UserProfile userProfile = new UserProfile("jank", "pass", "Jan Kowalski", false);

	private final static URI resource1URI = URI.create("http://example.org/ROs/ro1/foo/bar.txt");

	private final ResourceInfo resource1Info = new ResourceInfo("bar.txt", "ABC123455666344E", 646365L, "SHA1");

	private final static URI resource2URI = URI.create("http://workflows.org/a/workflow.scufl");

	private final ResourceInfo resource2Info = new ResourceInfo("a workflow", "A0987654321EDCB", 6L, "MD5");

	private final URI annotationBody1URI = URI.create("http://example.org/ROs/ro1/annotations/myTitleContent");

	private static final String RO_NAMESPACE = "http://purl.org/wf4ever/ro#";

	private final Property aggregates = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/aggregates");

	private final Property proxyIn = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/proxyIn");

	private final Property proxyFor = ModelFactory.createDefaultModel().createProperty(
		"http://www.openarchives.org/ore/terms/proxyFor");

	private final Property foafName = ModelFactory.createDefaultModel()
			.createProperty("http://xmlns.com/foaf/0.1/name");

	private final Property name = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "name");

	private final Property filesize = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "filesize");

	private final Property checksum = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "checksum");


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass()
		throws Exception
	{
	}


	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass()
		throws Exception
	{
		cleanData();
	}


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		cleanData();
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
	}


	private static void cleanData()
	{
		SemanticMetadataService sms = null;
		try {
			sms = new SemanticMetadataServiceImpl(userProfile);
			try {
				sms.removeManifest(manifestURI, baseURI);
			}
			catch (IllegalArgumentException e) {
				// nothing
			}
			try {
				sms.removeManifest(manifest2URI, baseURI);
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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#SemanticMetadataServiceImpl(pl.psnc.dl.wf4ever.dlibra.UserProfile)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testSemanticMetadataServiceImpl()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
		}
		finally {
			sms.close();
		}

		SemanticMetadataService sms2 = new SemanticMetadataServiceImpl(userProfile);
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
		}
		finally {
			sms2.close();
		}
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testCreateManifestURI()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
			try {
				sms.createManifest(manifestURI);
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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI, java.io.InputStream, org.openrdf.rio.RDFFormat)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testCreateManifestURIInputStreamRDFFormat()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);

			InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
			sms.createManifest(manifestURI, is, RDFFormat.TURTLE);

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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeManifest(java.net.URI, java.net.URI)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testRemoveManifest()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
			sms.addResource(manifestURI, resource1URI, resource1Info);
			sms.addResource(manifestURI, resource2URI, resource2Info);
			InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
			sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

			sms.removeManifest(manifestURI, baseURI);
			try {
				sms.removeManifest(manifestURI, baseURI);
				fail("Should have thrown an exception");
			}
			catch (IllegalArgumentException e) {
				// good
			}

			Assert.assertNotNull("Get other named graph must not return null",
				sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getManifest(java.net.URI, org.openrdf.rio.RDFFormat)}.
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testGetManifest()
		throws IOException, ClassNotFoundException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			Assert.assertNull("Returns null when manifest does not exist",
				sms.getManifest(manifestURI, RDFFormat.RDFXML));

			Calendar before = Calendar.getInstance();
			sms.createManifest(manifestURI);
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

			log.debug(IOUtils.toString(sms.getManifest(manifestURI, RDFFormat.TURTLE), "UTF-8"));
		}
		finally {
			sms.close();
		}
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addResource(java.net.URI, java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testAddResource()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResource(java.net.URI, java.net.URI)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testRemoveResource()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResource(java.net.URI, org.openrdf.rio.RDFFormat)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws URISyntaxException 
	 */
	@Test
	public final void testGetResource()
		throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			Assert.assertNull("Returns null when resource does not exist",
				sms.getResource(resource1URI, RDFFormat.RDFXML));

			sms.createManifest(manifestURI);
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
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getNamedGraph(java.net.URI, org.openrdf.rio.RDFFormat)}.
	 */
	@Test
	public final void testGetNamedGraph()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findManifests(java.net.URI)}.
	 * @throws SQLException 
	 * @throws NamingException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public final void testFindManifests()
		throws ClassNotFoundException, IOException, NamingException, SQLException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
		try {
			sms.createManifest(manifestURI);
			sms.createManifest(manifest2URI);
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


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#isRoFolder(java.net.URI)}.
	 */
	@Test
	public final void testIsRoFolder()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addNamedGraph(java.net.URI, java.io.InputStream, org.openrdf.rio.RDFFormat)}.
	 */
	@Test
	public final void testAddNamedGraph()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#isNamedGraph(java.net.URI)}.
	 */
	@Test
	public final void testIsNamedGraph()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeNamedGraph(java.net.URI, java.net.URI)}.
	 */
	@Test
	public final void testRemoveNamedGraph()
	{
		fail("Not yet implemented");
	}

}
