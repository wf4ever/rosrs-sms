/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;
import pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation;

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

	private final URI manifestURI = URI.create("http://example.org/ROs/ro1/manifest");

	private final URI researchObjectURI = URI.create("http://example.org/ROs/ro1/manifest#ro");

	private final URI annotationsURI = URI.create("http://example.org/ROs/ro1/annotations");

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

	private static final String RO_NAMESPACE = "http://example.wf4ever-project.org/2011/ro.owl#";

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
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI)}
	 * .
	 */
	@Test
	public final void testCreateManifest()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createManifest(manifestURI, userProfile);
		try {
			sms.createManifest(manifestURI, userProfile);
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
	@Ignore
	public final void testCreateResearchObjectAsCopy()
	{
		fail("Not yet implemented");
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeManifest(java.net.URI)}
	 * .
	 */
	@Test
	public final void testRemoveManifest()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createManifest(manifestURI, userProfile);
		sms.removeManifest(manifestURI);
		try {
			sms.removeManifest(manifestURI);
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
		Assert.assertNull("Returns null when manifest does not exist", sms.getManifest(manifestURI, Notation.RDF_XML));

		Calendar before = Calendar.getInstance();
		sms.createManifest(manifestURI, userProfile);
		Calendar after = Calendar.getInstance();
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
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
		Calendar created = ((XSDDateTime) createdLiteral.getValue()).asCalendar();
		Assert.assertTrue("Created is a valid date", !before.after(created) && !after.before(created));

		Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
		Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);
		Individual creator = creatorResource.as(Individual.class);
		Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
		Assert.assertEquals("Creator name must be correct", userProfile.getName(), creator.getPropertyValue(foafName)
				.asLiteral().getString());

		Resource annotations = manifest.getSeeAlso();
		Assert.assertNotNull("Manifest must contain reference to annotations", annotations);
		Assert.assertEquals("Annotations URI must be correct", annotationsURI.toString(), annotations.getURI());

		log.debug(IOUtils.toString(sms.getManifest(manifestURI, Notation.TURTLE), "UTF-8"));
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createManifest(java.net.URI, java.io.InputStream, pl.psnc.dl.wf4everdlibra.UserProfile)}
	 * .
	 * @throws IOException 
	 * @throws ParseException 
	 */
	@Test
	public final void testUpdateManifest()
		throws IOException, ParseException
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createManifest(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);

		InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
		sms.createManifest(manifestURI, is, Notation.TURTLE, userProfile);

		log.debug(IOUtils.toString(sms.getManifest(manifestURI, Notation.TURTLE), "UTF-8"));

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(sms.getManifest(manifestURI, Notation.RDF_XML), null);

		Individual manifest = model.getIndividual(manifestURI.toString());
		Individual ro = model.getIndividual(researchObjectURI.toString());

		Assert.assertEquals("Created has been updated", "2011-07-14T15:01:14Z",
			manifest.getPropertyValue(DCTerms.created).asLiteral().getString());

		Set<String> creators = new HashSet<String>();
		String creatorsQuery = String.format("PREFIX dcterms: <%s> PREFIX foaf: <%s> SELECT ?name "
				+ "WHERE { <%s> dcterms:creator ?x . ?x foaf:name ?name . }", DCTerms.NS, "http://xmlns.com/foaf/0.1/",
			manifestURI.toString());
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
		validateProxy(model, manifest, manifestURI.toString() + "#workflowProxy", "http://example.com/workflow.scufl2");
		validateProxy(model, manifest, manifestURI.toString() + "#inputProxy", "http://example.org/ROs/ro1/input.txt");
		validateProxy(model, manifest, manifestURI.toString() + "#outputProxy", "http://example.org/ROs/ro1/output.txt");
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
	 */
	@Test
	public final void testAddResource()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createManifest(manifestURI, userProfile);
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
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
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
		Assert.assertNull("Returns null when resource does not exist", sms.getResource(resource1URI, Notation.RDF_XML));

		sms.createManifest(manifestURI, userProfile);
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
		Assert.assertTrue(String.format("Resource %s must be a ro:Resource", resourceURI),
			resource.hasRDFType(RO_NAMESPACE + "Resource"));

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
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		sms.createManifest(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);
		sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
		sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);
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
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotation(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotation()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		Assert.assertNull("Returns null when annotation does not exist",
			sms.getAnnotation(annotation1URI, Notation.RDF_XML));

		sms.createManifest(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);
		sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
		sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(sms.getAnnotation(annotation1URI, Notation.RDF_XML), null);
		verifyAnnotation(model, annotation1URI, annotationBody1URI, annotation1Body.keySet());
		model.read(sms.getAnnotation(annotation2URI, Notation.RDF_XML), null);
		verifyAnnotation(model, annotation2URI, annotationBody2URI, annotation2Body.keySet());
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
			Assert.assertTrue(String.format("Annotation annotates resource %s", annotatedResourceURI.toString()),
				model.contains(annotation, annotatesResource, resource));
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
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getAnnotationBody(java.net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)}
	 * .
	 */
	@Test
	public final void testGetAnnotationBody()
	{
		SemanticMetadataService sms = new SemanticMetadataServiceImpl();
		Assert.assertNull("Returns null when annotation body does not exist",
			sms.getAnnotationBody(annotationBody1URI, Notation.RDF_XML));

		sms.createManifest(manifestURI, userProfile);
		sms.addResource(manifestURI, resource1URI, resource1Info);
		sms.addResource(manifestURI, resource2URI, resource2Info);
		sms.addAnnotation(annotation1URI, annotationBody1URI, annotation1Body, userProfile);
		sms.addAnnotation(annotation2URI, annotationBody2URI, annotation2Body, userProfile);

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		model.read(sms.getAnnotationBody(annotationBody1URI, Notation.RDF_XML), null);
		verifyAnnotationBody(model, annotation1Body);
		model.read(sms.getAnnotationBody(annotationBody2URI, Notation.RDF_XML), null);
		verifyAnnotationBody(model, annotation2Body);
	}


	private void verifyAnnotationBody(OntModel model, Map<URI, Map<URI, String>> annotationBody)
	{
		for (Map.Entry<URI, Map<URI, String>> entry : annotationBody.entrySet()) {
			Resource subject = model.createResource(entry.getKey().toString());
			for (Map.Entry<URI, String> entry2 : entry.getValue().entrySet()) {
				Property property = model.createProperty(entry2.getKey().toString());
				Assert.assertTrue(
					String.format("Annotation body contains a triple <%s> <%s> <%s>", subject.getURI(),
						property.getURI(), entry2.getValue()), model.contains(subject, property, entry2.getValue()));
			}
		}
	}


	/**
	 * Test method for
	 * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findResearchObjects(java.lang.String, java.util.Map)}
	 * .
	 */
	@Test
	@Ignore
	public final void testFindResearchObjects()
	{
		fail("Not yet implemented");
	}

}
