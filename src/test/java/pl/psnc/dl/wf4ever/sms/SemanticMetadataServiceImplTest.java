/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
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

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;

/**
 * @author piotrhol
 * @author filipwis
 * 
 */
public class SemanticMetadataServiceImplTest {

    private static final Logger log = Logger.getLogger(SemanticMetadataServiceImplTest.class);

    private final static URI manifestURI = URI.create("http://example.org/ROs/ro1/.ro/manifest.rdf");

    private final static URI researchObjectURI = URI.create("http://example.org/ROs/ro1/");

    private final static URI researchObject2URI = URI.create("http://example.org/ROs/ro2/");

    private final static URI snapshotResearchObjectURI = URI.create("http://example.org/ROs/sp1/");

    private final static URI archiveResearchObjectURI = URI.create("http://example.org/ROs/arch1/");

    private final static URI wrongResearchObjectURI = URI.create("http://wrong.example.org/ROs/wrongRo/");

    private final static UserProfile userProfile = new UserProfile("jank", "pass", "Jan Kowalski",
            UserProfile.Role.AUTHENTICATED);

    private final static URI workflowURI = URI.create("http://example.org/ROs/ro1/a%20workflow.t2flow");

    private final static URI workflowPartURI = URI
            .create("http://example.org/ROs/ro1/a%20workflow.t2flow#somePartOfIt");

    private final static URI workflow2URI = URI.create("http://example.org/ROs/ro2/runme.t2flow");

    private final ResourceInfo workflowInfo = new ResourceInfo("a%20workflow.t2flow", "ABC123455666344E", 646365L,
            "SHA1");

    private final static URI ann1URI = URI.create("http://example.org/ROs/ro1/ann1");

    private final ResourceInfo ann1Info = new ResourceInfo("ann1", "A0987654321EDCB", 6L, "MD5");

    private final static URI resourceFakeURI = URI.create("http://example.org/ROs/ro1/xyz");

    private final ResourceInfo resourceFakeInfo = new ResourceInfo("xyz", "A0987654321EDCB", 6L, "MD5");

    private final static URI folderURI = URI.create("http://example.org/ROs/ro1/afolder");

    private final URI annotationBody1URI = URI.create("http://example.org/ROs/ro1/.ro/ann1");

    private static final String RO_NAMESPACE = "http://purl.org/wf4ever/ro#";

    private final Property aggregates = ModelFactory.createDefaultModel().createProperty(
        "http://www.openarchives.org/ore/terms/aggregates");

    private final Property proxyIn = ModelFactory.createDefaultModel().createProperty(
        "http://www.openarchives.org/ore/terms/proxyIn");

    private final Property proxyFor = ModelFactory.createDefaultModel().createProperty(
        "http://www.openarchives.org/ore/terms/proxyFor");

    private final Property foafName = ModelFactory.createDefaultModel()
            .createProperty("http://xmlns.com/foaf/0.1/name");

    private final Property annotatesAggregatedResource = ModelFactory.createDefaultModel().createProperty(
        RO_NAMESPACE + "annotatesAggregatedResource");

    private final Property body = ModelFactory.createDefaultModel().createProperty("http://purl.org/ao/body");

    private final Property name = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "name");

    private final Property filesize = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "filesize");

    private final Property checksum = ModelFactory.createDefaultModel().createProperty(RO_NAMESPACE + "checksum");

    private static final String PROJECT_PATH = System.getProperty("user.dir");

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");


    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass()
            throws Exception {
    }


    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass()
            throws Exception {
        cleanData();
    }


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
            throws Exception {
        cleanData();
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
            throws Exception {
    }


    private static void cleanData() {
        SemanticMetadataService sms = null;
        try {
            sms = new SemanticMetadataServiceImpl(userProfile);
            try {
                sms.removeResearchObject(researchObjectURI);
            } catch (IllegalArgumentException e) {
                // nothing
            }
            try {
                sms.removeResearchObject(researchObject2URI);
            } catch (IllegalArgumentException e) {
                // nothing
            }
            try {
                sms.removeResearchObject(snapshotResearchObjectURI);
            } catch (IllegalArgumentException e) {
                // nothing
            }
            try {
                sms.removeResearchObject(archiveResearchObjectURI);
            } catch (IllegalArgumentException e) {
                // nothing
            }
        } catch (ClassNotFoundException | IOException | NamingException | SQLException e) {
            e.printStackTrace();
        } finally {
            if (sms != null)
                sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#SemanticMetadataServiceImpl(pl.psnc.dl.wf4ever.dlibra.UserProfile)}
     * .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testSemanticMetadataServiceImpl()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
        } finally {
            sms.close();
        }

        SemanticMetadataService sms2 = new SemanticMetadataServiceImpl(userProfile);
        try {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);

            model.read(sms2.getManifest(manifestURI, RDFFormat.RDFXML), "");
            Individual manifest = model.getIndividual(manifestURI.toString());
            Individual ro = model.getIndividual(researchObjectURI.toString());
            Assert.assertNotNull("Manifest must contain ro:Manifest", manifest);
            Assert.assertNotNull("Manifest must contain ro:ResearchObject", ro);
            Assert.assertTrue("Manifest must be a ro:Manifest", manifest.hasRDFType(RO_NAMESPACE + "Manifest"));
            Assert.assertTrue("RO must be a ro:ResearchObject", ro.hasRDFType(RO_NAMESPACE + "ResearchObject"));

            Literal createdLiteral = manifest.getPropertyValue(DCTerms.created).asLiteral();
            Assert.assertNotNull("Manifest must contain dcterms:created", createdLiteral);

            //			Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
            //			Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);
        } finally {
            sms2.close();
        }
    }


    /**
     * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#createResearchObject(java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testCreateResearchObject()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            try {
                sms.createResearchObject(researchObjectURI);
                fail("Should throw an exception");
            } catch (IllegalArgumentException e) {
                // good
            }
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#updateManifest(java.net.URI, java.io.InputStream, org.openrdf.rio.RDFFormat)}
     * .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testUpdateManifest()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, resourceFakeURI, resourceFakeInfo);

            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);

            Individual manifest = model.getIndividual(manifestURI.toString());
            Individual ro = model.getIndividual(researchObjectURI.toString());

            Assert.assertEquals("Manifest created has been updated", "2011-12-02T16:01:10Z",
                manifest.getPropertyValue(DCTerms.created).asLiteral().getString());
            Assert.assertEquals("RO created has been updated", "2011-12-02T15:01:10Z",
                ro.getPropertyValue(DCTerms.created).asLiteral().getString());

            Set<String> creators = new HashSet<String>();
            String creatorsQuery = String.format("PREFIX dcterms: <%s> PREFIX foaf: <%s> SELECT ?name "
                    + "WHERE { <%s> dcterms:creator ?x . ?x foaf:name ?name . }", DCTerms.NS,
                "http://xmlns.com/foaf/0.1/", researchObjectURI.toString());
            Query query = QueryFactory.create(creatorsQuery);
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            try {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    creators.add(results.nextSolution().getLiteral("name").getString());
                }
            } finally {
                qexec.close();
            }

            Assert.assertTrue("New creator has been added", creators.contains("Stian Soiland-Reyes"));
            Assert.assertTrue("Old creator has been deleted", !creators.contains(userProfile.getName()));

            Assert.assertTrue("RO must aggregate resources",
                model.contains(ro, aggregates, model.createResource(workflowURI.toString())));
            Assert.assertTrue("RO must aggregate resources",
                model.contains(ro, aggregates, model.createResource(ann1URI.toString())));
            Assert.assertTrue("RO must not aggregate previous resources",
                !model.contains(ro, aggregates, model.createResource(resourceFakeURI.toString())));
            validateProxy(model, manifest, researchObjectURI.toString() + "proxy1", workflowURI.toString());
        } finally {
            sms.close();
        }
    }


    private void validateProxy(OntModel model, Individual manifest, String proxyURI, String proxyForURI) {
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
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResearchObject(java.net.URI, java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testRemoveManifest()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

            sms.removeResearchObject(researchObjectURI);
            try {
                sms.removeResearchObject(researchObjectURI);
                fail("Should throw an exception");
            } catch (IllegalArgumentException e) {
                // good
            }

            Assert.assertNotNull("Get other named graph must not return null",
                sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getManifest(java.net.URI, org.openrdf.rio.RDFFormat)} .
     * 
     * @throws IOException
     * @throws SQLException
     * @throws NamingException
     * @throws ClassNotFoundException
     * @throws URISyntaxException
     */
    @Test
    public final void testGetManifest()
            throws IOException, ClassNotFoundException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            Assert.assertNull("Returns null when manifest does not exist",
                sms.getManifest(manifestURI, RDFFormat.RDFXML));

            Calendar before = Calendar.getInstance();
            sms.createResearchObject(researchObjectURI);
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

            //			Resource creatorResource = manifest.getPropertyResourceValue(DCTerms.creator);
            //			Assert.assertNotNull("Manifest must contain dcterms:creator", creatorResource);
            //			Individual creator = creatorResource.as(Individual.class);
            //			Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
            //			Assert.assertEquals("Creator name must be correct", "RODL", creator.getPropertyValue(foafName).asLiteral()
            //					.getString());

            Resource creatorResource = ro.getPropertyResourceValue(DCTerms.creator);
            Assert.assertNotNull("RO must contain dcterms:creator", creatorResource);

            OntModel userModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            userModel.read(sms.getNamedGraph(new URI(creatorResource.getURI()), RDFFormat.RDFXML), "");

            Individual creator = userModel.getIndividual(creatorResource.getURI());
            Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
            Assert.assertEquals("Creator name must be correct", userProfile.getName(),
                creator.getPropertyValue(foafName).asLiteral().getString());

            log.debug(IOUtils.toString(sms.getManifest(manifestURI, RDFFormat.RDFXML), "UTF-8"));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getManifest(java.net.URI, org.openrdf.rio.RDFFormat)} .
     * 
     * @throws IOException
     * @throws SQLException
     * @throws NamingException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testGetManifestWithAnnotationBodies()
            throws IOException, ClassNotFoundException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            Assert.assertNull("Returns null when manifest does not exist", sms.getManifest(manifestURI, RDFFormat.TRIG));

            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);
            is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

            NamedGraphSet graphset = new NamedGraphSetImpl();
            graphset.read(sms.getManifest(manifestURI, RDFFormat.TRIG), "TRIG", null);

            Quad sampleAgg = new Quad(Node.createURI(manifestURI.toString()), Node.createURI(researchObjectURI
                    .toString()), Node.createURI(aggregates.getURI()), Node.createURI(workflowURI.toString()));
            Assert.assertTrue("Contains a sample aggregation", graphset.containsQuad(sampleAgg));

            Quad sampleAnn = new Quad(Node.createURI(annotationBody1URI.toString()), Node.createURI(workflowURI
                    .toString()), Node.createURI("http://purl.org/dc/terms/license"), Node.createLiteral("GPL"));
            Assert.assertTrue("Contains a sample annotation", graphset.containsQuad(sampleAnn));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addResource(java.net.URI, java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)}
     * .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testAddResource()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            Assert.assertTrue(sms.addResource(researchObjectURI, workflowURI, workflowInfo));
            Assert.assertTrue(sms.addResource(researchObjectURI, ann1URI, ann1Info));
            Assert.assertFalse(sms.addResource(researchObjectURI, workflowURI, null));
            Assert.assertFalse(sms.addResource(researchObjectURI, workflowURI, new ResourceInfo(null, null, 0, null)));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeResource(java.net.URI, java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testRemoveResource()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            sms.removeResource(researchObjectURI, workflowURI);
            try {
                sms.removeResource(researchObjectURI, workflowURI);
                fail("Should throw an exception");
            } catch (IllegalArgumentException e) {
                // good
            }
            sms.removeResource(researchObjectURI, ann1URI);
            try {
                sms.removeResource(researchObjectURI, ann1URI);
                fail("Should throw an exception");
            } catch (IllegalArgumentException e) {
                // good
            }

            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);
            sms.removeResource(researchObjectURI, workflowURI);
            Assert.assertNull("There should be no annotation body after a resource is deleted",
                sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);
            Assert.assertFalse(model.listStatements(null, null, model.createResource(ann1URI.toString())).hasNext());
            Assert.assertFalse(model.listStatements(model.createResource(ann1URI.toString()), null, (RDFNode) null)
                    .hasNext());

        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getResource(java.net.URI, org.openrdf.rio.RDFFormat)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws URISyntaxException
     */
    @Test
    public final void testGetResource()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            Assert.assertNull("Returns null when resource does not exist",
                sms.getResource(researchObjectURI, workflowURI, RDFFormat.RDFXML));

            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

            log.debug(IOUtils.toString(sms.getResource(researchObjectURI, workflowURI, RDFFormat.RDFXML), "UTF-8"));

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getResource(researchObjectURI, workflowURI, RDFFormat.RDFXML), researchObjectURI.toString());
            verifyResource(sms, model, workflowURI, workflowInfo);
            verifyTriple(model, workflowURI, URI.create("http://purl.org/dc/terms/title"), "A test");
            verifyTriple(model, workflowURI, URI.create("http://purl.org/dc/terms/title"), "An alternative title");
            verifyTriple(model, workflowURI, URI.create("http://purl.org/dc/terms/license"), "GPL");

            model.read(sms.getResource(researchObjectURI, ann1URI, RDFFormat.TURTLE), null, "TTL");
            verifyResource(sms, model, ann1URI, ann1Info);
        } finally {
            sms.close();
        }
    }


    /**
     * @param model
     * @param resourceInfo
     * @param resourceURI
     * @throws URISyntaxException
     */
    private void verifyResource(SemanticMetadataService sms, OntModel model, URI resourceURI, ResourceInfo resourceInfo)
            throws URISyntaxException {
        Individual resource = model.getIndividual(resourceURI.toString());
        Assert.assertNotNull("Resource cannot be null", resource);
        Assert.assertTrue(String.format("Resource %s must be a ro:Resource", resourceURI),
            resource.hasRDFType(RO_NAMESPACE + "Resource"));

        RDFNode createdLiteral = resource.getPropertyValue(DCTerms.created);
        Assert.assertNotNull("Resource must contain dcterms:created", createdLiteral);
        Assert.assertEquals("Date type is xsd:dateTime", XSDDatatype.XSDdateTime, createdLiteral.asLiteral()
                .getDatatype());

        Resource creatorResource = resource.getPropertyResourceValue(DCTerms.creator);
        Assert.assertNotNull("Resource must contain dcterms:creator", creatorResource);

        OntModel userModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        userModel.read(sms.getNamedGraph(new URI(creatorResource.getURI()), RDFFormat.RDFXML), "");
        Individual creator = userModel.getIndividual(creatorResource.getURI());
        Assert.assertNotNull("User named graph must contain dcterms:creator", creator);
        Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
        Assert.assertEquals("Creator name must be correct", userProfile.getName(), creator.getPropertyValue(foafName)
                .asLiteral().getString());

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
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#getNamedGraph(java.net.URI, org.openrdf.rio.RDFFormat)}
     * .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testGetNamedGraph()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);
        } finally {
            sms.close();
        }
        SemanticMetadataService sms2 = new SemanticMetadataServiceImpl(userProfile);
        try {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms2.getNamedGraph(annotationBody1URI, RDFFormat.TURTLE), null, "TTL");

            verifyTriple(model, workflowURI, URI.create("http://purl.org/dc/terms/title"), "A test");
            verifyTriple(model, workflowURI, URI.create("http://purl.org/dc/terms/license"), "GPL");
            verifyTriple(model, URI.create("http://workflows.org/a%20workflow.scufl"),
                URI.create("http://purl.org/dc/terms/description"), "Something interesting");
            verifyTriple(model, workflowPartURI, URI.create("http://purl.org/dc/terms/description"), "The key part");
        } finally {
            sms.close();
        }
    }


    private void verifyTriple(Model model, URI subjectURI, URI propertyURI, String object) {
        Resource subject = model.createResource(subjectURI.toString());
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    private void verifyTriple(Model model, String subjectURI, URI propertyURI, String object) {
        Resource subject = model.createResource(subjectURI);
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    private void verifyTriple(Model model, String subjectURI, URI propertyURI, Resource object) {
        Resource subject = model.createResource(subjectURI);
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#findResearchObjectsByPrefix(java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testFindManifests()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.createResearchObject(researchObject2URI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);

            Set<URI> result = sms.findResearchObjectsByPrefix(researchObjectURI.resolve(".."));
            Assert.assertTrue("Find with base of RO", result.contains(researchObjectURI));
            Assert.assertTrue("Find with base of RO", result.contains(researchObject2URI));

            result = sms.findResearchObjectsByPrefix(wrongResearchObjectURI.resolve(".."));
            Assert.assertFalse("Not find with the wrong base", result.contains(researchObjectURI));
            Assert.assertFalse("Not find with the wrong base", result.contains(researchObject2URI));

            result = sms.findResearchObjectsByCreator(userProfile.getUri());
            Assert.assertTrue("Find by creator of RO", result.contains(researchObjectURI));
            Assert.assertTrue("Find by creator of RO", result.contains(researchObject2URI));

            result = sms.findResearchObjectsByCreator(wrongResearchObjectURI);
            Assert.assertFalse("Not find by the wrong creator", result.contains(researchObjectURI));
            Assert.assertFalse("Not find by the wrong creator", result.contains(researchObject2URI));

        } finally {
            sms.close();
        }
    }


    /**
     * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#isRoFolder(java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testIsRoFolder()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);

            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);

            Assert.assertTrue("<afolder> is an ro:Folder", sms.isRoFolder(researchObjectURI, folderURI));
            Assert.assertTrue("<ann1> is not an ro:Folder", !sms.isRoFolder(researchObjectURI, ann1URI));
            Assert.assertTrue("Fake resource is not an ro:Folder", !sms.isRoFolder(researchObjectURI, resourceFakeURI));
            Assert.assertTrue("<afolder> is not an ro:Folder according to other RO",
                !sms.isRoFolder(researchObject2URI, folderURI));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#addNamedGraph(java.net.URI, java.io.InputStream, org.openrdf.rio.RDFFormat)}
     * .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testAddNamedGraph()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            Assert.assertTrue(sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#isROMetadataNamedGraph(java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testIsROMetadataNamedGraph()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);

            Assert.assertTrue("Only mentioned annotation body is an RO metadata named graph",
                sms.isROMetadataNamedGraph(researchObjectURI, annotationBody1URI));

            is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);
            is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI.resolve("fake"), is, RDFFormat.TURTLE);

            Assert.assertTrue("Annotation body is an RO metadata named graph",
                sms.isROMetadataNamedGraph(researchObjectURI, annotationBody1URI));
            Assert.assertTrue("An random named graph is not an RO metadata named graph",
                !sms.isROMetadataNamedGraph(researchObjectURI, annotationBody1URI.resolve("fake")));
            Assert.assertTrue("Manifest is an RO metadata named graph",
                sms.isROMetadataNamedGraph(researchObjectURI, manifestURI));
            Assert.assertTrue("A resource is not an RO metadata named graph",
                !sms.isROMetadataNamedGraph(researchObjectURI, workflowURI));
            Assert.assertTrue("A fake resource is not an RO metadata named graph",
                !sms.isROMetadataNamedGraph(researchObjectURI, resourceFakeURI));
        } finally {
            sms.close();
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.sms.SemanticMetadataServiceImpl#removeNamedGraph(java.net.URI, java.net.URI)} .
     * 
     * @throws SQLException
     * @throws NamingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public final void testRemoveNamedGraph()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);
            Assert.assertNotNull("A named graph exists", sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
            sms.removeNamedGraph(researchObjectURI, annotationBody1URI);
            Assert.assertNull("A deleted named graph no longer exists",
                sms.getNamedGraph(annotationBody1URI, RDFFormat.RDFXML));
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testExecuteSparql()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);

            is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

            String describeQuery = String.format("DESCRIBE <%s>", workflowURI.toString());
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            QueryResult res = sms.executeSparql(describeQuery, RDFFormat.RDFXML);
            model.read(res.getInputStream(), null, "RDF/XML");
            Individual resource = model.getIndividual(workflowURI.toString());
            Assert.assertNotNull("Resource cannot be null", resource);
            Assert.assertTrue(String.format("Resource %s must be a ro:Resource", workflowURI),
                resource.hasRDFType(RO_NAMESPACE + "Resource"));

            is = getClass().getClassLoader().getResourceAsStream("direct-annotations-construct.sparql");
            String constructQuery = IOUtils.toString(is, "UTF-8");
            model.removeAll();
            model.read(sms.executeSparql(constructQuery, RDFFormat.RDFXML).getInputStream(), null, "RDF/XML");
            Assert.assertTrue("Construct contains triple 1",
                model.contains(model.createResource(workflowURI.toString()), DCTerms.title, "A test"));
            Assert.assertTrue("Construct contains triple 2",
                model.contains(model.createResource(workflowURI.toString()), DCTerms.license, "GPL"));

            is = getClass().getClassLoader().getResourceAsStream("direct-annotations-select.sparql");
            String selectQuery = IOUtils.toString(is, "UTF-8");
            String xml = IOUtils.toString(sms.executeSparql(selectQuery, SemanticMetadataService.SPARQL_XML)
                    .getInputStream(), "UTF-8");
            // FIXME make more in-depth XML validation
            Assert.assertTrue("XML looks correct", xml.contains("Marco Roos"));

            String json = IOUtils.toString(sms.executeSparql(selectQuery, SemanticMetadataService.SPARQL_JSON)
                    .getInputStream(), "UTF-8");
            // FIXME make more in-depth JSON validation
            Assert.assertTrue("JSON looks correct", json.contains("Marco Roos"));

            is = getClass().getClassLoader().getResourceAsStream("direct-annotations-ask-true.sparql");
            String askTrueQuery = IOUtils.toString(is, "UTF-8");
            xml = IOUtils.toString(
                sms.executeSparql(askTrueQuery, SemanticMetadataService.SPARQL_XML).getInputStream(), "UTF-8");
            Assert.assertTrue("XML looks correct", xml.contains("true"));

            is = getClass().getClassLoader().getResourceAsStream("direct-annotations-ask-false.sparql");
            String askFalseQuery = IOUtils.toString(is, "UTF-8");
            xml = IOUtils.toString(sms.executeSparql(askFalseQuery, SemanticMetadataService.SPARQL_XML)
                    .getInputStream(), "UTF-8");
            Assert.assertTrue("XML looks correct", xml.contains("false"));

            RDFFormat jpeg = new RDFFormat("JPEG", "image/jpeg", Charset.forName("UTF-8"), "jpeg", false, false);
            res = sms.executeSparql(describeQuery, jpeg);
            Assert.assertEquals("RDF/XML is the default format", RDFFormat.RDFXML, res.getFormat());
            res = sms.executeSparql(constructQuery, jpeg);
            Assert.assertEquals("RDF/XML is the default format", RDFFormat.RDFXML, res.getFormat());
            res = sms.executeSparql(selectQuery, jpeg);
            Assert.assertEquals("SPARQL XML is the default format", SemanticMetadataService.SPARQL_XML, res.getFormat());
            res = sms.executeSparql(askTrueQuery, jpeg);
            Assert.assertEquals("SPARQL XML is the default format", SemanticMetadataService.SPARQL_XML, res.getFormat());
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testGetAllAttributes()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("manifest.ttl");
            sms.updateManifest(manifestURI, is, RDFFormat.TURTLE);

            is = getClass().getClassLoader().getResourceAsStream("annotationBody.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);

            Multimap<URI, Object> atts = sms.getAllAttributes(workflowURI);
            Assert.assertEquals(6, atts.size());
            Assert.assertTrue("Attributes contain type",
                atts.containsValue(URI.create("http://purl.org/wf4ever/ro#Resource")));
            Assert.assertTrue("Attributes contain created", atts.get(URI.create(DCTerms.created.toString())).iterator()
                    .next() instanceof Calendar);
            Assert.assertTrue("Attributes contain title",
                atts.get(URI.create(DCTerms.title.toString())).contains("A test"));
            Assert.assertTrue("Attributes contain title2",
                atts.get(URI.create(DCTerms.title.toString())).contains("An alternative title"));
            Assert.assertTrue("Attributes contain licence",
                atts.get(URI.create(DCTerms.license.toString())).contains("GPL"));
            Assert.assertTrue("Attributes contain creator",
                atts.get(URI.create(DCTerms.creator.toString())).contains("Stian Soiland-Reyes"));
        } finally {
            sms.close();
        }
    }


    @Test
    public void testGetNamedGraphWithRelativeURIs()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            sms.addResource(researchObjectURI, ann1URI, ann1Info);
            InputStream is = getClass().getClassLoader().getResourceAsStream("annotationBody2.ttl");
            sms.addNamedGraph(annotationBody1URI, is, RDFFormat.TURTLE);
        } finally {
            sms.close();
        }
        SemanticMetadataService sms2 = new SemanticMetadataServiceImpl(userProfile);
        try {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms2.getNamedGraphWithRelativeURIs(annotationBody1URI, researchObjectURI, RDFFormat.RDFXML), "",
                "RDF/XML");

            log.debug(IOUtils.toString(
                sms2.getNamedGraphWithRelativeURIs(annotationBody1URI, researchObjectURI, RDFFormat.RDFXML), "UTF-8"));

            ResIterator x = model.listSubjects();
            while (x.hasNext()) {
                System.out.println(x.next().getURI());
            }

            //FIXME this does not work correctly, for some reason ".." is stripped when reading the model
            verifyTriple(model, /* "../a_workflow.t2flow" */"a%20workflow.t2flow",
                URI.create("http://purl.org/dc/terms/title"), "A test");
            verifyTriple(model, /* "../a_workflow.t2flow" */"a%20workflow.t2flow",
                URI.create("http://purl.org/dc/terms/source"), model.createResource(workflow2URI.toString()));
            verifyTriple(model, new URI("manifest.rdf"), URI.create("http://purl.org/dc/terms/license"), "GPL");
            verifyTriple(model, URI.create("http://workflows.org/a%20workflow.scufl"),
                URI.create("http://purl.org/dc/terms/description"), "Something interesting");
            verifyTriple(model, /* "../a_workflow.t2flow#somePartOfIt" */"a%20workflow.t2flow#somePartOfIt",
                URI.create("http://purl.org/dc/terms/description"), "The key part");
        } finally {
            sms.close();
        }

    }


    @Test
    public final void testGetRemoveUser()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            OntModel userModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            userModel.read(sms.getUser(userProfile.getUri(), RDFFormat.RDFXML).getInputStream(), "", "RDF/XML");
            Individual creator = userModel.getIndividual(userProfile.getUri().toString());
            Assert.assertNotNull("User named graph must contain dcterms:creator", creator);
            Assert.assertTrue("Creator must be a foaf:Agent", creator.hasRDFType("http://xmlns.com/foaf/0.1/Agent"));
            Assert.assertEquals("Creator name must be correct", userProfile.getName(),
                creator.getPropertyValue(foafName).asLiteral().getString());

            sms.removeUser(userProfile.getUri());
            userModel.removeAll();
            userModel.read(sms.getUser(userProfile.getUri(), RDFFormat.RDFXML).getInputStream(), "", "RDF/XML");
            creator = userModel.getIndividual(userProfile.getUri().toString());
            Assert.assertNull("User named graph must not contain dcterms:creator", creator);
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testIsAggregatedResource()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            Assert.assertTrue("Is aggregated", sms.isAggregatedResource(researchObjectURI, workflowURI));
            Assert.assertFalse("Is not aggregated", sms.isAggregatedResource(researchObjectURI, workflow2URI));
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testIsAnnotation()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            URI ann = sms.addAnnotation(researchObjectURI, Arrays.asList(workflowURI), annotationBody1URI);
            Assert.assertTrue("Annotation is an annotation", sms.isAnnotation(researchObjectURI, ann));
            Assert.assertFalse("Workflow is not an annotation", sms.isAnnotation(researchObjectURI, workflowURI));
            Assert.assertFalse("2nd workflow is not an annotation", sms.isAnnotation(researchObjectURI, workflow2URI));
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testAddAnnotation()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            URI ann = sms
                    .addAnnotation(researchObjectURI, Arrays.asList(workflowURI, workflow2URI), annotationBody1URI);
            Assert.assertNotNull("Ann URI is not null", ann);

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);
            Resource researchObject = model.getResource(researchObjectURI.toString());
            Resource annotation = model.getResource(ann.toString());
            Resource workflow = model.getResource(workflowURI.toString());
            Resource workflow2 = model.getResource(workflow2URI.toString());
            Resource abody = model.getResource(annotationBody1URI.toString());

            Assert.assertTrue(model.contains(researchObject, aggregates, annotation));
            Assert.assertTrue(model.contains(annotation, annotatesAggregatedResource, workflow));
            Assert.assertTrue(model.contains(annotation, annotatesAggregatedResource, workflow2));
            Assert.assertTrue(model.contains(annotation, body, abody));
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testUpdateAnnotation()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            URI ann = sms
                    .addAnnotation(researchObjectURI, Arrays.asList(workflowURI, workflow2URI), annotationBody1URI);
            sms.updateAnnotation(researchObjectURI, ann, Arrays.asList(workflowURI, researchObjectURI), workflow2URI);

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);
            Resource researchObject = model.getResource(researchObjectURI.toString());
            Resource annotation = model.getResource(ann.toString());
            Resource workflow = model.getResource(workflowURI.toString());
            Resource workflow2 = model.getResource(workflow2URI.toString());
            Resource abody = model.getResource(annotationBody1URI.toString());

            Assert.assertTrue(model.contains(researchObject, aggregates, annotation));
            Assert.assertTrue(model.contains(annotation, annotatesAggregatedResource, workflow));
            Assert.assertFalse(model.contains(annotation, annotatesAggregatedResource, workflow2));
            Assert.assertTrue(model.contains(annotation, annotatesAggregatedResource, researchObject));
            Assert.assertFalse(model.contains(annotation, body, abody));
            Assert.assertTrue(model.contains(annotation, body, workflow2));
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testGetAnnotationBody()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            URI ann = sms
                    .addAnnotation(researchObjectURI, Arrays.asList(workflowURI, workflow2URI), annotationBody1URI);
            URI annBody = sms.getAnnotationBody(researchObjectURI, ann);
            Assert.assertEquals("Annotation body retrieved correctly", annotationBody1URI, annBody);
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testDeleteAnnotation()
            throws ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        try {
            sms.createResearchObject(researchObjectURI);
            sms.addResource(researchObjectURI, workflowURI, workflowInfo);
            URI ann = sms
                    .addAnnotation(researchObjectURI, Arrays.asList(workflowURI, workflow2URI), annotationBody1URI);
            sms.deleteAnnotation(researchObjectURI, ann);

            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
            model.read(sms.getManifest(manifestURI, RDFFormat.RDFXML), null);
            Resource annotation = model.createResource(ann.toString());
            Assert.assertFalse("No annotation statements", model.listStatements(annotation, null, (RDFNode) null)
                    .hasNext());
            Assert.assertFalse("No annotation statements", model.listStatements(null, null, annotation).hasNext());
        } finally {
            sms.close();
        }
    }


    @Test
    public final void testIsSnapshot()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        Assert.assertTrue("snapshot does not recognized",
            sms.isSnapshotURI(getResourceURI("ro1-sp1/"), ".ro/manifest.ttl", "TTL"));
        Assert.assertFalse("snapshot wrongly recognized",
            sms.isSnapshotURI(getResourceURI("ro1/"), ".ro/manifest.ttl", "TTL"));
        sms.close();
    }


    @Test
    public final void testIsArchive()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        Assert.assertTrue("snapshot does not recognized",
            sms.isArchiveURI(getResourceURI("ro1-arch1/"), ".ro/manifest.ttl", "TTL"));
        Assert.assertFalse("snapshot wrongly recognized",
            sms.isArchiveURI(getResourceURI("ro1/"), ".ro/manifest.ttl", "TTL"));
        sms.close();
    }


    @Test
    public final void testGetPreviousSnaphotOrArchive()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        URI sp1Antecessor = sms.getPreviousSnaphotOrArchive(getResourceURI("ro1/"), getResourceURI("ro1-sp1/"),
            ".ro/manifest.ttl", "TTL");
        URI sp2Antecessor = sms.getPreviousSnaphotOrArchive(getResourceURI("ro1/"), getResourceURI("ro1-sp2/"),
            ".ro/manifest.ttl", "TTL");
        sms.close();
        Assert.assertNull("wrong antecessor URI", sp1Antecessor);
        Assert.assertEquals("wrong antecessor URI", sp2Antecessor, getResourceURI("ro1-sp1/"));
    }


    @Test
    public final void testGetIndividual()
            throws URISyntaxException, ClassNotFoundException, IOException, NamingException, SQLException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        model.read(getResourceURI("ro1/").resolve(".ro/manifest.ttl").toString(), "TTL");
        Individual source = model.getIndividual(getResourceURI("ro1/").toString());
        Individual source2 = sms.getIndividual(getResourceURI("ro1/"), ".ro/manifest.ttl", "TTL");
        Assert.assertEquals("wrong individual returned", source, source2);
        sms.close();
    }


    @Test
    public final void testGetLiveROfromSnapshotOrArchive()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        URI liveFromRO = sms.getLiveURIFromSnapshotOrArchive(getResourceURI("ro1/"), ".ro/manifest.ttl", "TTL");
        URI liveFromSP = sms.getLiveURIFromSnapshotOrArchive(getResourceURI("ro1-sp1/"), ".ro/manifest.ttl", "TTL");
        URI liveFromARCH = sms.getLiveURIFromSnapshotOrArchive(getResourceURI("ro1-arch1/"), ".ro/manifest.ttl", "TTL");
        sms.close();
        Assert.assertNull("live RO does not have a parent RO", liveFromRO);
        Assert.assertEquals("wrong parent URI", liveFromSP, getResourceURI("ro1/"));
        Assert.assertEquals("wrong parent URI", liveFromARCH, getResourceURI("ro1/"));

    }


    @Test
    public final void testStoreROhistory()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        InputStream is = getClass().getClassLoader().getResourceAsStream("rdfStructure/ro1-sp2/.ro/manifest.ttl");
        sms.addNamedGraph(getResourceURI("ro1-sp2/.ro/manifest.rdf"), is, RDFFormat.TURTLE);
        sms.storeAggregatedDifferences(getResourceURI("ro1-sp2/"), getResourceURI("ro1-sp1/"), ".ro/manifest.ttl",
            "TTL");
        
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        model.read(sms.getManifest(getResourceURI("ro1-sp2/.ro/evoo_info.rdf"), RDFFormat.RDFXML), null);
        //Individual snaphotIndividual = model.getIndividual(getResourceURI("ro1-sp2/").toString());
        //List<RDFNode> changesList = snaphotIndividual
        //        .getProperty(model.createProperty("http://purl.org/wf4ever/roevo#wasChangedBy")).getObject()
        //        .as(Individual.class)
        //        .listPropertyValues(model.createProperty("http://purl.org/wf4ever/roevo#hasChange")).toList();
     
        String w = (IOUtils.toString(sms.getManifest(getResourceURI("ro1-sp2/.ro/evok_inf.rdf"), RDFFormat.RDFXML), "UTF-8"));
        String a = (IOUtils.toString(sms.getManifest(getResourceURI("ro1-sp2/.ro/manifest.rdf"), RDFFormat.RDFXML), "UTF-8"));
        String b = "";
        String c= b;

        /*
        Assert.assertTrue(isChangeInTheChangesList(
            "file:///home/pejot/code/rosrs-sms/src/test/resources/rdfStructure/ro1-sp2/ann3",
            "http://purl.org/wf4ever/roevo#Modification", model, changesList));
        Assert.assertTrue(isChangeInTheChangesList(
            "file:///home/pejot/code/rosrs-sms/src/test/resources/rdfStructure/ro1-sp2/res1",
            "http://purl.org/wf4ever/roevo#Addition", model, changesList));
        Assert.assertTrue(isChangeInTheChangesList(
            "file:///home/pejot/code/rosrs-sms/src/test/resources/rdfStructure/ro1-sp2/afinalfolder",
            "http://purl.org/wf4ever/roevo#Addition", model, changesList));
        Assert.assertTrue(isChangeInTheChangesList(
            "file:///home/pejot/code/rosrs-sms/src/test/resources/rdfStructure/ro1-sp2/ann2",
            "http://purl.org/wf4ever/roevo#Modification", model, changesList));
        Assert.assertTrue(isChangeInTheChangesList(
            "file:///home/pejot/code/rosrs-sms/src/test/resources/rdfStructure/ro1-sp1/afolder",
            "http://purl.org/wf4ever/roevo#Removal", model, changesList));
         */

    }

    @Test(expected = NullPointerException.class)
    public final void testStoreROhistoryWithWrongParametrs()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        InputStream is = getClass().getClassLoader().getResourceAsStream("rdfStructure/ro1-sp2/.ro/manifest.ttl");
        sms.addNamedGraph(getResourceURI("ro1-sp2/.ro/manifest.rdf"), is, RDFFormat.TURTLE);
        sms.storeAggregatedDifferences(null, getResourceURI("ro1-sp1/"), ".ro/manifest.ttl", "TTL");
    }


    @Test
    public final void testStoreROhistoryWithNoAccenestor()
            throws ClassNotFoundException, IOException, NamingException, SQLException, URISyntaxException {
        SemanticMetadataService sms = new SemanticMetadataServiceImpl(userProfile);
        InputStream is = getClass().getClassLoader().getResourceAsStream("rdfStructure/ro1-sp1/.ro/manifest.ttl");
        sms.addNamedGraph(getResourceURI("ro1-sp1/.ro/manifest.rdf"), is, RDFFormat.TURTLE);
        String result = sms.storeAggregatedDifferences(getResourceURI("ro1-sp1/"), null);
        Assert.assertEquals("", result);
        //@TODO read created model and look for the possible written changes
    }


    /***** HELPERS *****/

    private Boolean isChangeInTheChangesList(String relatedObjectURI, String rdfClass, OntModel model,
            List<Resource> changesList) {
        for (RDFNode change : changesList) {
            Boolean partialResult1 = change.asResource()
                    .getProperty(model.createProperty("http://purl.org/wf4ever/roevo#relatedResource")).getObject()
                    .toString().equals(relatedObjectURI);
            Boolean partialREsult2 = change.as(Individual.class).hasRDFType(rdfClass);
            if (partialResult1 && partialREsult2) {
                return true;
            }
        }
        return false;
    }


    private URI getResourceURI(String resourceName)
            throws URISyntaxException {
        String result = PROJECT_PATH;
        result += FILE_SEPARATOR + "src" + FILE_SEPARATOR + "test" + FILE_SEPARATOR + "resources" + FILE_SEPARATOR
                + "rdfStructure" + FILE_SEPARATOR + resourceName;
        return new URI("file://" + result);
    }
}
