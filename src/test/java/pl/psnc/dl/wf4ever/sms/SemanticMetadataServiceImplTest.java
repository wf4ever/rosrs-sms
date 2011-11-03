/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;
import pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
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

	/**
	 * Date format used for dates. This is NOT xsd:dateTime because of missing :
	 * in time zone.
	 */
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	private final URI manifestURI = URI.create("http://example.org/ROs/ro1/manifest");

	private final URI researchObjectURI = URI.create("http://example.org/ROs/ro1/manifest#ro");

	private final URI resource1URI = URI.create("http://example.org/ROs/ro1/foo/bar.txt");

	private final ResourceInfo resource1Info = new ResourceInfo("bar.txt", "ABC123455666344E", 646365L, "SHA1");

	private final URI resource2URI = URI.create("http://workflows.org/a/workflow.scufl");

	private final ResourceInfo resource2Info = new ResourceInfo("a workflow", "A0987654321EDCB", 6L, "MD5");

	private final UserProfile userProfile = new UserProfile("jank", "pass", "Jan Kowalski", false);

	private static final String RO_NAMESPACE = "http://example.wf4ever-project.org/2011/ro.owl#";

	private final Property foafName = ModelFactory.createDefaultModel()
			.createProperty("http://xmlns.com/foaf/0.1/name");

	private final Property name = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "name");

	private final Property filesize = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "filesize");

	private final Property checksum = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "checksum");


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#SemanticMetadataServiceImpl()}
	 * .
	 */
	@Test
	public final void testSemanticMetadataServiceImpl()
	{
		new SemanticMetadataServiceImpl();
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObject(java.net.URI)}
	 * .
	 */
	@Test
	public final void testCreateResearchObject()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI, userProfile);
		try {
			sms.createResearchObject(manifestURI, userProfile);
			fail("Should have thrown an exception");
		}
		catch (IllegalArgumentException e) {
			// good
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObjectAsCopy(java.net.URI, java.net.URI)}
	 * .
	 */
	@Test
	public final void testCreateResearchObjectAsCopy()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResearchObject(java.net.URI)}
	 * .
	 */
	@Test
	public final void testRemoveResearchObject()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI, userProfile);
		sms.removeResearchObject(manifestURI);
		try {
			sms.removeResearchObject(manifestURI);
			fail("Should have thrown an exception");
		}
		catch (IllegalArgumentException e) {
			// good
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getManifest(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public final void testGetManifest()
		throws IOException, ParseException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		Calendar before = Calendar.getInstance();
		sms.createResearchObject(manifestURI, userProfile);
		Calendar after = Calendar.getInstance();
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(sms.getManifest(manifestURI, Notation.RDF_XML), null);

		Individual manifest = model.getIndividual(manifestURI.toString());
		Individual ro = model.getIndividual(researchObjectURI.toString());
		Assert.assertNotNull("Manifest must contain ro:Manifest", manifest);
		Assert.assertNotNull("Manifest must contain ro:ResearchObject", ro);
		Assert.assertTrue("Manifest must be a ro:Manifest", manifest.hasRDFType(RO_NAMESPACE + "Manifest"));
		Assert.assertTrue("RO must be a ro:ResearchObject", ro.hasRDFType(RO_NAMESPACE + "ResearchObject"));

		Literal createdLiteral = manifest.getPropertyValue(DCTerms.created).asLiteral();
		Assert.assertNotNull("Manifest must contain dcterms:created", createdLiteral);
		Assert.assertEquals("Date type is xsd:dateTime", XSDDatatype.XSDdateTime, createdLiteral.getDatatype());
		Calendar created = ((XSDDateTime) createdLiteral.asLiteral().getValue()).asCalendar();
		Assert.assertTrue("Created is a valid date", !before.after(created) && !after.before(created));

		Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
		Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);
		Individual creator = creatorResource.as(Individual.class);
		Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
		Assert.assertEquals("Creator name must be correct", userProfile.getName(), creator.getPropertyValue(foafName)
				.asLiteral().getString());

		log.debug(IOUtils.toString(sms.getManifest(manifestURI, Notation.TRIG), "UTF-8"));
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#updateManifest(java.net.URI, java.io.InputStream, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testUpdateManifest()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addResource(java.net.URI, java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)}
	 * .
	 */
	@Test
	public final void testAddResource()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);
		sms.addResource(manifestURI, resource1URI, null);
		sms.addResource(manifestURI, resource1URI, new ResourceInfo(null, null, 0, null));
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResource(java.net.URI, java.net.URI)}
	 * .
	 */
	@Test
	public final void testRemoveResource()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResource(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 * @throws URISyntaxException 
	 */
	@Test
	public final void testGetResource()
		throws URISyntaxException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(sms.getResource(resource1URI, Notation.RDF_XML), null);
		verifyResource(model, resource1URI, resource1Info);

		model.read(sms.getResource(resource2URI, Notation.RDF_XML), null);
		verifyResource(model, resource2URI, resource2Info);
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
	 */
	@Test
	public final void testAddAnnotation()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#deleteAnnotationsWithBodies(java.net.URI)}
	 * .
	 */
	@Test
	public final void testDeleteAnnotationsWithBodies()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotations(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotations()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotationBody(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotationBody()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findResearchObjects(java.lang.String, java.util.Map)}
	 * .
	 */
	@Test
	public final void testFindResearchObjects()
	{
		fail("Not yet implemented");
	}

}
