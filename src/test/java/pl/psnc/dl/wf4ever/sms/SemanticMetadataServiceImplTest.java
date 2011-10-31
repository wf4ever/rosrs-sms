/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Test;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImplTest {

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
	 */
	@Test
	public final void testGetManifest() {
		fail("Not yet implemented");
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
