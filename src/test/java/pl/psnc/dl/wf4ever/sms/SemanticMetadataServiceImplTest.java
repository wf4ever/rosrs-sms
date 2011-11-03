/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImplTest {

	private static final Logger log = Logger
			.getLogger(SemanticMetadataServiceImplTest.class);

	/**
	 * Date format used for dates. This is NOT xsd:dateTime because of missing :
	 * in time zone.
	 */
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssZ");

	private final URI manifestURI = URI
			.create("http://example.org/ROs/ro1/manifest");
	private final URI researchObjectURI = URI
			.create("http://example.org/ROs/ro1/manifest#ro");

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#SemanticMetadataServiceImpl()}
	 * .
	 */
	@Test
	public final void testSemanticMetadataServiceImpl() {
		new SemanticMetadataServiceImpl();
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObject(java.net.URI)}
	 * .
	 */
	@Test
	public final void testCreateResearchObject() {
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI);
		try {
			sms.createResearchObject(manifestURI);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			// good
		}
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObjectAsCopy(java.net.URI, java.net.URI)}
	 * .
	 */
	@Test
	public final void testCreateResearchObjectAsCopy() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResearchObject(java.net.URI)}
	 * .
	 */
	@Test
	public final void testRemoveResearchObject() {
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createResearchObject(manifestURI);
		sms.removeResearchObject(manifestURI);
		try {
			sms.removeResearchObject(manifestURI);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			// good
		}
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResearchObject(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetResearchObject() {
		fail("Not yet implemented");
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
	public final void testGetManifest() throws IOException, ParseException {
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		Calendar before = Calendar.getInstance();
		sms.createResearchObject(manifestURI);
		Calendar after = Calendar.getInstance();
		InputStream is = sms.getManifest(manifestURI, Notation.RDF_XML);
		OntModel model = ModelFactory
				.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(is, null);
		Individual manifest = model.getIndividual(manifestURI.toString());
		Individual ro = model.getIndividual(researchObjectURI.toString());
		Assert.assertNotNull("Manifest must contain ro:Manifest", manifest);
		Assert.assertNotNull("Manifest must contain ro:ResearchObject", ro);
		Literal createdLiteral = manifest.getPropertyValue(DCTerms.created)
				.asLiteral();
		Assert.assertEquals("Date type is xsd:dateTime",
				XSDDatatype.XSDdateTime, createdLiteral.getDatatype());
		Calendar created = ((XSDDateTime) createdLiteral.asLiteral().getValue())
				.asCalendar();
		Assert.assertTrue("Created is a valid date", !before.after(created)
				&& !after.before(created));

		InputStream is2 = sms.getManifest(manifestURI, Notation.TRIG);
		log.debug(IOUtils.toString(is2, "UTF-8"));
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#updateManifest(java.net.URI, java.io.InputStream, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testUpdateManifest() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addResource(java.net.URI, java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)}
	 * .
	 */
	@Test
	public final void testAddResource() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResource(java.net.URI, java.net.URI)}
	 * .
	 */
	@Test
	public final void testRemoveResource() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResource(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetResource() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addAnnotation(java.net.URI, java.net.URI, java.net.URI, java.util.Map)}
	 * .
	 */
	@Test
	public final void testAddAnnotation() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#deleteAnnotationsWithBodies(java.net.URI)}
	 * .
	 */
	@Test
	public final void testDeleteAnnotationsWithBodies() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotations(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotations() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotationBody(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotationBody() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findResearchObjects(java.lang.String, java.util.Map)}
	 * .
	 */
	@Test
	public final void testFindResearchObjects() {
		fail("Not yet implemented");
	}

}
