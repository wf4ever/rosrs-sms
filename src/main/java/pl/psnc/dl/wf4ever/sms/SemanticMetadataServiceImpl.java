package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;
import pl.psnc.dl.wf4ever.exceptions.ManifestTraversingException;
import pl.psnc.dl.wf4ever.vocabulary.AO;
import pl.psnc.dl.wf4ever.vocabulary.FOAF;
import pl.psnc.dl.wf4ever.vocabulary.ORE;
import pl.psnc.dl.wf4ever.vocabulary.PROV;
import pl.psnc.dl.wf4ever.vocabulary.RO;
import pl.psnc.dl.wf4ever.vocabulary.ROEVO;
import pl.psnc.dl.wf4ever.vocabulary.W4E;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.db.NamedGraphSetDB;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;
import de.fuberlin.wiwiss.ng4j.sparql.NamedGraphDataset;

/**
 * @author piotrhol
 * 
 */
public class SemanticMetadataServiceImpl implements SemanticMetadataService {

    private static final Logger log = Logger.getLogger(SemanticMetadataServiceImpl.class);

    private final NamedGraphSet graphset;

    private final String getResourceQueryTmpl = "DESCRIBE <%s> WHERE { }";

    private final String getUserQueryTmpl = "DESCRIBE <%s> WHERE { }";

    private final String findResearchObjectsQueryTmpl = "PREFIX ro: <" + RO.NAMESPACE + "> SELECT ?ro "
            + "WHERE { ?ro a ro:ResearchObject. FILTER regex(str(?ro), \"^%s\") . }";

    private final String findResearchObjectsByCreatorQueryTmpl = "PREFIX ro: <" + RO.NAMESPACE + "> PREFIX dcterms: <"
            + DCTerms.NS + "> SELECT ?ro WHERE { ?ro a ro:ResearchObject ; dcterms:creator <%s> . }";

    private final Connection connection;

    private final UserProfile user;


    public SemanticMetadataServiceImpl(UserProfile user)
            throws IOException, NamingException, SQLException, ClassNotFoundException {
        this.user = user;
        connection = getConnection("connection.properties");
        if (connection == null) {
            throw new RuntimeException("Connection could not be created");
        }

        graphset = new NamedGraphSetDB(connection, "sms");
        W4E.defaultModel.setNsPrefixes(W4E.standardNamespaces);
        createUserProfile(user);
    }


    public SemanticMetadataServiceImpl(UserProfile user, URI researchObject, InputStream manifest, RDFFormat rdfFormat) {
        this.user = user;
        this.connection = null;

        graphset = new NamedGraphSetImpl();
        W4E.defaultModel.setNsPrefixes(W4E.standardNamespaces);
        createUserProfile(user);

        createResearchObject(researchObject);
        updateManifest(researchObject, manifest, rdfFormat);
    }


    private void createUserProfile(UserProfile user) {
        if (!containsNamedGraph(user.getUri())) {
            OntModel userModel = createOntModelForNamedGraph(user.getUri());
            userModel.removeAll();
            Individual agent = userModel.createIndividual(user.getUri().toString(), FOAF.Agent);
            userModel.add(agent, FOAF.name, user.getName());
        }
    }


    private Connection getConnection(String filename)
            throws IOException, NamingException, SQLException, ClassNotFoundException {
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
        } else {
            String driver_class = props.getProperty("driver_class");
            String url = props.getProperty("url");
            String username = props.getProperty("username");
            String password = props.getProperty("password");
            if (driver_class != null && url != null && username != null && password != null) {
                Class.forName(driver_class);
                log.debug("" + this + " opens a connection");
                return DriverManager.getConnection(url, username, password);
            }
        }

        return null;
    }


    @Override
    public UserProfile getUserProfile() {
        return user;
    }


    @Override
    public void createResearchObject(URI researchObjectURI) {
        createLiveResearchObject(researchObjectURI, null);
    }


    @Override
    public void createLiveResearchObject(URI researchObjectURI, URI source) {
        URI manifestURI = getManifestURI(researchObjectURI.normalize());
        OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
        Individual manifest = manifestModel.getIndividual(manifestURI.toString());
        if (manifest != null) {
            throw new IllegalArgumentException("URI already exists: " + manifestURI);
        }
        manifest = manifestModel.createIndividual(manifestURI.toString(), RO.Manifest);
        Individual ro = manifestModel.createIndividual(researchObjectURI.toString(), RO.ResearchObject);
        ro.addRDFType(ROEVO.LiveROClass);

        manifestModel.add(ro, ORE.isDescribedBy, manifest);
        manifestModel.add(ro, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
        manifestModel.add(ro, DCTerms.creator, manifestModel.createResource(user.getUri().toString()));

        manifestModel.add(manifest, ORE.describes, ro);
        manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));

        if (source != null) {
            manifestModel.add(ro, PROV.hadOriginalSource, manifestModel.createResource(source.toString()));
        }
    }


    @Override
    public void createSnapshotResearchObject(URI researchObject, URI liveROURI) {
        URI manifestURI = getManifestURI(researchObject.normalize());
        OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
        Individual manifest = manifestModel.getIndividual(manifestURI.toString());
        if (manifest != null) {
            throw new IllegalArgumentException("URI already exists: " + manifestURI);
        }

        URI liveManifestURI = getManifestURI(liveROURI.normalize());
        OntModel liveManifestModel = createOntModelForNamedGraph(liveManifestURI);
        Individual liveManifest = liveManifestModel.getIndividual(liveManifestURI.toString());

        manifest = manifestModel.createIndividual(manifestURI.toString(), RO.Manifest);
        Individual ro = manifestModel.createIndividual(researchObject.toString(), RO.ResearchObject);
        ro.addRDFType(ROEVO.SnapshotROClass);

        RDFNode creator, created;
        Resource liveRO;
        if (liveManifest == null) {
            log.warn("Live RO is not an RO: " + liveROURI);
            liveRO = manifestModel.createResource(liveROURI.toString());
            creator = manifestModel.createResource(user.getUri().toString());
            created = manifestModel.createTypedLiteral(Calendar.getInstance());
        } else {
            liveRO = liveManifestModel.getIndividual(liveROURI.toString());
            if (liveRO == null) {
                throw new IllegalArgumentException("Live RO does not describe the research object");
            }
            creator = liveRO.getPropertyResourceValue(DCTerms.creator);
            created = liveRO.as(Individual.class).getPropertyValue(DCTerms.created);
            liveManifestModel.add(liveRO, ROEVO.hasSnapshot, ro);
        }

        manifestModel.add(ro, ORE.isDescribedBy, manifest);
        manifestModel.add(ro, DCTerms.created, created);
        manifestModel.add(ro, DCTerms.creator, creator);

        manifestModel.add(manifest, ORE.describes, ro);
        manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));

        manifestModel.add(ro, ROEVO.isSnapshotOf, liveRO);
        manifestModel.add(ro, ROEVO.snapshottedAtTime, manifestModel.createTypedLiteral(Calendar.getInstance()));
        manifestModel.add(ro, ROEVO.snapshottedBy, manifestModel.createResource(user.getUri().toString()));

        //TODO add wasRevisionOf
    }


    @Override
    public void createArchivedResearchObject(URI researchObject, URI liveROURI) {
        URI manifestURI = getManifestURI(researchObject.normalize());
        OntModel manifestModel = createOntModelForNamedGraph(manifestURI);
        Individual manifest = manifestModel.getIndividual(manifestURI.toString());
        if (manifest != null) {
            throw new IllegalArgumentException("URI already exists: " + manifestURI);
        }

        URI liveManifestURI = getManifestURI(liveROURI.normalize());
        OntModel liveManifestModel = createOntModelForNamedGraph(liveManifestURI);
        Individual liveManifest = liveManifestModel.getIndividual(liveManifestURI.toString());

        manifest = manifestModel.createIndividual(manifestURI.toString(), RO.Manifest);
        Individual ro = manifestModel.createIndividual(researchObject.toString(), RO.ResearchObject);
        ro.addRDFType(ROEVO.ArchivedROClass);

        RDFNode creator, created;
        Resource liveRO;
        if (liveManifest == null) {
            log.warn("Live RO is not an RO: " + liveROURI);
            liveRO = manifestModel.createResource(liveROURI.toString());
            creator = manifestModel.createResource(user.getUri().toString());
            created = manifestModel.createTypedLiteral(Calendar.getInstance());
        } else {
            liveRO = liveManifestModel.getIndividual(liveROURI.toString());
            if (liveRO == null) {
                throw new IllegalArgumentException("Live RO does not describe the research object");
            }
            creator = liveRO.getPropertyResourceValue(DCTerms.creator);
            created = liveRO.as(Individual.class).getPropertyValue(DCTerms.created);
            liveManifestModel.add(liveRO, ROEVO.hasArchive, ro);
        }

        manifestModel.add(ro, ORE.isDescribedBy, manifest);
        manifestModel.add(ro, DCTerms.created, created);
        manifestModel.add(ro, DCTerms.creator, creator);

        manifestModel.add(manifest, ORE.describes, ro);
        manifestModel.add(manifest, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));

        manifestModel.add(ro, ROEVO.isArchiveOf, liveRO);
        manifestModel.add(ro, ROEVO.archivedAtTime, manifestModel.createTypedLiteral(Calendar.getInstance()));
        manifestModel.add(ro, ROEVO.archivedBy, manifestModel.createResource(user.getUri().toString()));

        //TODO add wasRevisionOf
    }


    @Override
    public void updateManifest(URI manifestURI, InputStream is, RDFFormat rdfFormat) {
        // TODO validate the manifest?
        addNamedGraph(manifestURI, is, rdfFormat);
    }


    /*
     * (non-Javadoc)
     * 
     * @see pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getManifest(java.net.URI,
     * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
     */
    @Override
    public InputStream getManifest(URI manifestURI, RDFFormat rdfFormat) {
        return getNamedGraph(manifestURI, rdfFormat);
    }


    /*
     * (non-Javadoc)
     * 
     * @see pl.psnc.dl.wf4ever.sms.SemanticMetadataService#addResource(java.net.URI,
     * java.net.URI, pl.psnc.dl.wf4ever.dlibra.ResourceInfo)
     */
    @Override
    public boolean addResource(URI roURI, URI resourceURI, ResourceInfo resourceInfo) {
        resourceURI = resourceURI.normalize();
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(roURI.normalize()));
        Individual ro = manifestModel.getIndividual(roURI.toString());
        if (ro == null) {
            throw new IllegalArgumentException("URI not found: " + roURI);
        }
        boolean created = false;
        Individual resource = manifestModel.getIndividual(resourceURI.toString());
        if (resource == null) {
            created = true;
            resource = manifestModel.createIndividual(resourceURI.toString(), RO.Resource);
        }
        manifestModel.add(ro, ORE.aggregates, resource);
        if (resourceInfo != null) {
            if (resourceInfo.getName() != null) {
                manifestModel.add(resource, RO.name, manifestModel.createTypedLiteral(resourceInfo.getName()));
            }
            manifestModel.add(resource, RO.filesize, manifestModel.createTypedLiteral(resourceInfo.getSizeInBytes()));
            if (resourceInfo.getChecksum() != null && resourceInfo.getDigestMethod() != null) {
                manifestModel.add(resource, RO.checksum, manifestModel.createResource(String.format("urn:%s:%s",
                    resourceInfo.getDigestMethod(), resourceInfo.getChecksum())));
            }
        }
        manifestModel.add(resource, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
        manifestModel.add(resource, DCTerms.creator, manifestModel.createResource(user.getUri().toString()));
        return created;
    }


    /*
     * (non-Javadoc)
     * 
     * @see pl.psnc.dl.wf4ever.sms.SemanticMetadataService#removeResource(java.net .URI,
     * java.net.URI)
     */
    @Override
    public void removeResource(URI roURI, URI resourceURI) {
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
        manifestModel.remove(ro, ORE.aggregates, resource);

        StmtIterator it = manifestModel.listStatements(null, ORE.aggregates, resource);
        if (!it.hasNext()) {
            manifestModel.removeAll(resource, null, null);
        }

        ResIterator it2 = manifestModel.listSubjectsWithProperty(RO.annotatesAggregatedResource, resource);
        while (it2.hasNext()) {
            Resource ann = it2.next();
            manifestModel.remove(ann, RO.annotatesAggregatedResource, resource);
            if (!ann.hasProperty(RO.annotatesAggregatedResource)) {
                Resource annBody = ann.getPropertyResourceValue(AO.body);
                if (annBody != null && annBody.isURIResource()) {
                    URI annBodyURI = URI.create(annBody.getURI());
                    if (containsNamedGraph(annBodyURI)) {
                        removeNamedGraph(roURI, annBodyURI);
                    }
                }
                manifestModel.removeAll(ann, null, null);
                manifestModel.removeAll(null, null, ann);
            }
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getResource(java.net.URI,
     * pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
     */
    @Override
    public InputStream getResource(URI roURI, URI resourceURI, RDFFormat rdfFormat) {
        resourceURI = resourceURI.normalize();
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(roURI.normalize()));
        Individual resource = manifestModel.getIndividual(resourceURI.toString());
        if (resource == null) {
            return null;
        }

        String queryString = String.format(getResourceQueryTmpl, resourceURI.toString());
        Query query = QueryFactory.create(queryString);

        QueryResult result = processDescribeQuery(query, rdfFormat);

        if (!result.getFormat().equals(rdfFormat)) {
            log.warn(String.format("Possible RDF format mismatch: %s requested, %s returned", rdfFormat.getName(),
                result.getFormat().getName()));
        }

        return result.getInputStream();
    }


    /*
     * (non-Javadoc)
     * 
     * @see pl.psnc.dl.wf4ever.sms.SemanticMetadataService#getAnnotationBody(java
     * .net.URI, pl.psnc.dl.wf4ever.sms.SemanticMetadataService.Notation)
     */
    @Override
    public InputStream getNamedGraph(URI namedGraphURI, RDFFormat rdfFormat) {
        namedGraphURI = namedGraphURI.normalize();
        if (!graphset.containsGraph(namedGraphURI.toString())) {
            return null;
        }
        NamedGraphSet tmpGraphSet = new NamedGraphSetImpl();
        if (rdfFormat.supportsContexts()) {
            addGraphsRecursively(tmpGraphSet, namedGraphURI);
        } else {
            tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tmpGraphSet.write(out, rdfFormat.getName().toUpperCase(), null);
        return new ByteArrayInputStream(out.toByteArray());
    }


    @Override
    public InputStream getNamedGraphWithRelativeURIs(URI namedGraphURI, URI researchObjectURI, RDFFormat rdfFormat) {
        ResearchObjectRelativeWriter writer;
        if (rdfFormat != RDFFormat.RDFXML && rdfFormat != RDFFormat.TURTLE) {
            throw new RuntimeException("Format " + rdfFormat + " is not supported");
        } else if (rdfFormat == RDFFormat.RDFXML) {
            writer = new RO_RDFXMLWriter();
        } else {
            writer = new RO_TurtleWriter();
        }
        namedGraphURI = namedGraphURI.normalize();
        if (!graphset.containsGraph(namedGraphURI.toString())) {
            return null;
        }
        NamedGraphSet tmpGraphSet = new NamedGraphSetImpl();
        if (rdfFormat.supportsContexts()) {
            addGraphsRecursively(tmpGraphSet, namedGraphURI);
        } else {
            tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));
        }

        writer.setResearchObjectURI(researchObjectURI);
        writer.setBaseURI(namedGraphURI);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(tmpGraphSet.asJenaModel(namedGraphURI.toString()), out, null);
        return new ByteArrayInputStream(out.toByteArray());
    }


    private void addGraphsRecursively(NamedGraphSet tmpGraphSet, URI namedGraphURI) {
        tmpGraphSet.addGraph(graphset.getGraph(namedGraphURI.toString()));

        OntModel annotationModel = createOntModelForNamedGraph(namedGraphURI);
        NodeIterator it = annotationModel.listObjectsOfProperty(AO.body);
        while (it.hasNext()) {
            RDFNode annotationBodyRef = it.next();
            URI childURI = URI.create(annotationBodyRef.asResource().getURI());
            if (graphset.containsGraph(childURI.toString()) && !tmpGraphSet.containsGraph(childURI.toString())) {
                addGraphsRecursively(tmpGraphSet, childURI);
            }
        }
    }


    @Override
    public Set<URI> findResearchObjectsByPrefix(URI partialURI) {
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


    @Override
    public Set<URI> findResearchObjectsByCreator(URI user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        String queryString = String.format(findResearchObjectsByCreatorQueryTmpl, user.toString());
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


    @Override
    public Set<URI> findResearchObjects() {
        return findResearchObjectsByPrefix(null);
    }


    /**
     * @param namedGraphURI
     * @return
     */
    private OntModel createOntModelForNamedGraph(URI namedGraphURI) {
        NamedGraph namedGraph = getOrCreateGraph(graphset, namedGraphURI);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
            ModelFactory.createModelForGraph(namedGraph));
        ontModel.addSubModel(W4E.defaultModel);
        return ontModel;
    }


    private NamedGraph getOrCreateGraph(NamedGraphSet graphset, URI namedGraphURI) {
        return graphset.containsGraph(namedGraphURI.toString()) ? graphset.getGraph(namedGraphURI.toString())
                : graphset.createGraph(namedGraphURI.toString());
    }


    @Override
    public void close() {
        log.debug("" + this + " closes a connection");
        graphset.close();
    }


    @Override
    public boolean isRoFolder(URI researchObjectURI, URI resourceURI) {
        resourceURI = resourceURI.normalize();
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObjectURI.normalize()));
        Individual resource = manifestModel.getIndividual(resourceURI.toString());
        if (resource == null) {
            return false;
        }
        return resource.hasRDFType(RO.Folder);
    }


    @Override
    public boolean addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat) {
        boolean created = !containsNamedGraph(graphURI);
        OntModel namedGraphModel = createOntModelForNamedGraph(graphURI);
        namedGraphModel.removeAll();
        namedGraphModel.read(inputStream, graphURI.resolve(".").toString(), rdfFormat.getName().toUpperCase());
        return created;
    }


    @Override
    public boolean isROMetadataNamedGraph(URI researchObjectURI, URI graphURI) {
        Node manifest = Node.createURI(getManifestURI(researchObjectURI).toString());
        Node deprecatedManifest = Node.createURI(getDeprecatedManifestURI(researchObjectURI).toString());
        Node bodyNode = Node.createURI(AO.body.getURI());
        Node annBody = Node.createURI(graphURI.toString());
        boolean isManifest = getManifestURI(researchObjectURI).equals(graphURI);
        boolean isDeprecatedManifest = getDeprecatedManifestURI(researchObjectURI).equals(graphURI);
        boolean isAnnotationBody = graphset.containsQuad(new Quad(manifest, Node.ANY, bodyNode, annBody));
        boolean isDeprecatedAnnotationBody = graphset.containsQuad(new Quad(deprecatedManifest, Node.ANY, bodyNode,
                annBody));
        return isManifest || isAnnotationBody || isDeprecatedManifest || isDeprecatedAnnotationBody;
    }


    @Override
    public void removeNamedGraph(URI researchObjectURI, URI graphURI) {
        //@TODO Remove evo_inf file
        graphURI = graphURI.normalize();
        if (!graphset.containsGraph(graphURI.toString())) {
            throw new IllegalArgumentException("URI not found: " + graphURI);
        }

        List<URI> graphsToDelete = new ArrayList<>();
        graphsToDelete.add(graphURI);

        int i = 0;
        while (i < graphsToDelete.size()) {
            OntModel manifestModel = createOntModelForNamedGraph(graphsToDelete.get(i));
            NodeIterator it = manifestModel.listObjectsOfProperty(AO.body);
            while (it.hasNext()) {
                RDFNode annotationBodyRef = it.next();
                URI graphURI2 = URI.create(annotationBodyRef.asResource().getURI());
                // TODO make sure that this named graph is internal
                if (graphset.containsGraph(graphURI2.toString()) && !graphsToDelete.contains(graphURI2)) {
                    graphsToDelete.add(graphURI2);
                }
            }
            List<RDFNode> evos = manifestModel.listObjectsOfProperty(ROEVO.wasChangedBy).toList();
            for (RDFNode evo : evos) {
                URI graphURI2 = URI.create(evo.asResource().getURI());
                if (graphset.containsGraph(graphURI2.toString()) && !graphsToDelete.contains(graphURI2)) {
                    graphsToDelete.add(graphURI2);
                }
            }
            i++;
        }
        for (URI graphURI2 : graphsToDelete) {
            graphset.removeGraph(graphURI2.toString());
        }
    }


    private OntModel createOntModelForAllNamedGraphs() {
        return createOntModelForAllNamedGraphs(OntModelSpec.OWL_MEM);
    }


    private OntModel createOntModelForAllNamedGraphs(OntModelSpec spec) {
        OntModel ontModel = ModelFactory.createOntologyModel(spec,
            graphset.asJenaModel(W4E.DEFAULT_NAMED_GRAPH_URI.toString()));
        ontModel.addSubModel(W4E.defaultModel);
        return ontModel;
    }


    /**
     * 
     * @param roURI
     *            must end with /
     * @return
     */
    private URI getManifestURI(URI roURI) {
        return roURI.resolve(".ro/manifest.rdf");
    }


    /**
     * 
     * @param roURI
     *            must end with /
     * @return
     */
    private URI getDeprecatedManifestURI(URI roURI) {
        return roURI.resolve(".ro/manifest");
    }


    @Override
    public void removeResearchObject(URI researchObjectURI) {
        try {
            removeNamedGraph(researchObjectURI, getDeprecatedManifestURI(researchObjectURI));
        } catch (IllegalArgumentException e) {
            // it is a hack so ignore exceptions
        }
        removeNamedGraph(researchObjectURI, getManifestURI(researchObjectURI));
    }


    @Override
    public boolean containsNamedGraph(URI graphURI) {
        return graphset.containsGraph(graphURI.toString());
    }


    @Override
    public QueryResult executeSparql(String queryS, RDFFormat rdfFormat) {
        if (queryS == null)
            throw new NullPointerException("Query cannot be null");
        Query query = null;
        try {
            query = QueryFactory.create(queryS, W4E.sparqlSyntax);
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong query syntax: " + e.getMessage());
        }

        switch (query.getQueryType()) {
            case Query.QueryTypeSelect:
                return processSelectQuery(query, rdfFormat);
            case Query.QueryTypeConstruct:
                return processConstructQuery(query, rdfFormat);
            case Query.QueryTypeDescribe:
                return processDescribeQuery(query, rdfFormat);
            case Query.QueryTypeAsk:
                return processAskQuery(query, rdfFormat);
            default:
                return null;
        }
    }


    private QueryResult processSelectQuery(Query query, RDFFormat rdfFormat) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFFormat outputFormat;
        QueryExecution qexec = QueryExecutionFactory.create(query, new NamedGraphDataset(graphset));
        if (SemanticMetadataService.SPARQL_JSON.equals(rdfFormat)) {
            outputFormat = SemanticMetadataService.SPARQL_JSON;
            ResultSetFormatter.outputAsJSON(out, qexec.execSelect());
        } else {
            outputFormat = SemanticMetadataService.SPARQL_XML;
            ResultSetFormatter.outputAsXML(out, qexec.execSelect());
        }
        qexec.close();

        return new QueryResult(new ByteArrayInputStream(out.toByteArray()), outputFormat);
    }


    private QueryResult processAskQuery(Query query, RDFFormat rdfFormat) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFFormat outputFormat;
        QueryExecution qexec = QueryExecutionFactory.create(query, new NamedGraphDataset(graphset));
        if ("application/sparql-results+json".equals(rdfFormat.getDefaultMIMEType())) {
            outputFormat = SemanticMetadataService.SPARQL_JSON;
            ResultSetFormatter.outputAsJSON(out, qexec.execAsk());
        } else {
            outputFormat = SemanticMetadataService.SPARQL_XML;
            ResultSetFormatter.outputAsXML(out, qexec.execAsk());
        }
        qexec.close();

        return new QueryResult(new ByteArrayInputStream(out.toByteArray()), outputFormat);
    }


    private QueryResult processConstructQuery(Query query, RDFFormat rdfFormat) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueryExecution qexec = QueryExecutionFactory.create(query, new NamedGraphDataset(graphset));
        Model resultModel = qexec.execConstruct();
        qexec.close();

        RDFFormat outputFormat;
        if (RDFFormat.values().contains(rdfFormat)) {
            outputFormat = rdfFormat;
        } else {
            outputFormat = RDFFormat.RDFXML;
        }

        resultModel.write(out, outputFormat.getName().toUpperCase());
        return new QueryResult(new ByteArrayInputStream(out.toByteArray()), outputFormat);
    }


    private QueryResult processDescribeQuery(Query query, RDFFormat rdfFormat) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueryExecution qexec = QueryExecutionFactory.create(query, new NamedGraphDataset(graphset));
        Model resultModel = qexec.execDescribe();
        qexec.close();

        RDFFormat outputFormat;
        if (RDFFormat.values().contains(rdfFormat)) {
            outputFormat = rdfFormat;
        } else {
            outputFormat = RDFFormat.RDFXML;
        }

        resultModel.removeNsPrefix("xml");

        resultModel.write(out, outputFormat.getName().toUpperCase());
        return new QueryResult(new ByteArrayInputStream(out.toByteArray()), outputFormat);
    }


    @Override
    public Multimap<URI, Object> getAllAttributes(URI subjectURI) {
        Multimap<URI, Object> attributes = HashMultimap.<URI, Object> create();
        // This could be an inference model but it slows down the lookup process and
        // generates super-attributes
        OntModel model = createOntModelForAllNamedGraphs(OntModelSpec.OWL_MEM);
        Resource subject = model.getResource(subjectURI.toString());
        if (subject == null)
            return attributes;
        StmtIterator it = model.listStatements(subject, null, (RDFNode) null);
        while (it.hasNext()) {
            Statement s = it.next();
            try {
                URI propURI = new URI(s.getPredicate().getURI());
                Object object;
                if (s.getObject().isResource()) {
                    // Need to check both because the model has no inference
                    if (s.getObject().as(Individual.class).hasRDFType(FOAF.Agent)
                            || s.getObject().as(Individual.class).hasRDFType(FOAF.Person)) {
                        object = s.getObject().asResource().getProperty(FOAF.name).getLiteral().getValue();
                    } else {
                        if (s.getObject().isURIResource()) {
                            object = new URI(s.getObject().asResource().getURI());
                        } else {
                            continue;
                        }
                    }
                } else {
                    object = s.getObject().asLiteral().getValue();
                }
                if (object instanceof XSDDateTime) {
                    object = ((XSDDateTime) object).asCalendar();
                }
                attributes.put(propURI, object);
            } catch (URISyntaxException e) {
                log.error(e);
            }
        }
        return attributes;
    }


    @Override
    public QueryResult getUser(URI userURI, RDFFormat rdfFormat) {
        userURI = userURI.normalize();

        String queryString = String.format(getUserQueryTmpl, userURI.toString());
        Query query = QueryFactory.create(queryString);

        return processDescribeQuery(query, rdfFormat);
    }


    @Override
    public void removeUser(URI userURI) {
        if (graphset.containsGraph(userURI.toString())) {
            graphset.removeGraph(userURI.toString());
        }
    }


    @Override
    public boolean isAggregatedResource(URI researchObject, URI resource) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource researchObjectR = manifestModel.createResource(researchObject.toString());
        Resource resourceR = manifestModel.createResource(resource.normalize().toString());
        return manifestModel.contains(researchObjectR, ORE.aggregates, resourceR);
    }


    @Override
    public boolean isAnnotation(URI researchObject, URI resource) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Individual resourceR = manifestModel.getIndividual(resource.normalize().toString());
        return resourceR != null && resourceR.hasRDFType(RO.AggregatedAnnotation);
    }


    @Override
    public URI addProxy(URI researchObject, URI resource) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource researchObjectR = manifestModel.createResource(researchObject.toString());
        Resource resourceR = manifestModel.createResource(resource.normalize().toString());
        URI proxyURI = generateProxyURI(researchObject);
        Individual proxy = manifestModel.createIndividual(proxyURI.toString(), ORE.Proxy);
        manifestModel.add(proxy, ORE.proxyIn, researchObjectR);
        manifestModel.add(proxy, ORE.proxyFor, resourceR);
        return proxyURI;
    }


    private static URI generateProxyURI(URI researchObject) {
        return researchObject.resolve(".ro/proxies/" + UUID.randomUUID());
    }


    @Override
    public boolean isProxy(URI researchObject, URI resource) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Individual resourceR = manifestModel.getIndividual(resource.normalize().toString());
        return resourceR != null && resourceR.hasRDFType(ORE.Proxy);
    }


    @Override
    public boolean existsProxyForResource(URI researchObject, URI resource) {
        return getProxyForResource(researchObject, resource) != null;
    }


    @Override
    public URI getProxyForResource(URI researchObject, URI resource) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource resourceR = manifestModel.createResource(resource.normalize().toString());
        ResIterator it = manifestModel.listSubjectsWithProperty(ORE.proxyFor, resourceR);
        while (it.hasNext()) {
            Individual r = it.next().as(Individual.class);
            if (r != null && r.hasRDFType(ORE.Proxy)) {
                return URI.create(r.getURI());
            }
        }
        return null;
    }


    @Override
    public URI getProxyFor(URI researchObject, URI proxy) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Individual proxyR = manifestModel.getIndividual(proxy.normalize().toString());
        if (proxyR.hasProperty(ORE.proxyFor)) {
            return URI.create(proxyR.getPropertyResourceValue(ORE.proxyFor).getURI());
        } else {
            return null;
        }
    }


    @Override
    public void deleteProxy(URI researchObject, URI proxy) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource proxyR = manifestModel.getResource(proxy.normalize().toString());
        manifestModel.removeAll(proxyR, null, null);
    }


    @Override
    public URI addAnnotation(URI researchObject, List<URI> annotationTargets, URI annotationBody) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource researchObjectR = manifestModel.createResource(researchObject.toString());
        Resource body = manifestModel.createResource(annotationBody.normalize().toString());
        URI annotationURI = generateAnnotationURI(researchObject);
        Individual annotation = manifestModel.createIndividual(annotationURI.toString(), RO.AggregatedAnnotation);
        manifestModel.add(researchObjectR, ORE.aggregates, annotation);
        manifestModel.add(annotation, AO.body, body);
        for (URI targetURI : annotationTargets) {
            Resource target = manifestModel.createResource(targetURI.normalize().toString());
            manifestModel.add(annotation, RO.annotatesAggregatedResource, target);
        }
        manifestModel.add(annotation, DCTerms.created, manifestModel.createTypedLiteral(Calendar.getInstance()));
        Resource agent = manifestModel.createResource(user.getUri().toString());
        manifestModel.add(annotation, DCTerms.creator, agent);
        return annotationURI;
    }


    private static URI generateAnnotationURI(URI researchObject) {
        return researchObject.resolve(".ro/annotations/" + UUID.randomUUID());
    }


    @Override
    public void updateAnnotation(URI researchObject, URI annotationURI, List<URI> annotationTargets, URI annotationBody) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource body = manifestModel.createResource(annotationBody.normalize().toString());
        Individual annotation = manifestModel.getIndividual(annotationURI.toString());
        manifestModel.removeAll(annotation, AO.body, null);
        manifestModel.removeAll(annotation, RO.annotatesAggregatedResource, null);
        manifestModel.add(annotation, AO.body, body);
        for (URI targetURI : annotationTargets) {
            Resource target = manifestModel.createResource(targetURI.normalize().toString());
            manifestModel.add(annotation, RO.annotatesAggregatedResource, target);
        }
    }


    @Override
    public URI getAnnotationBody(URI researchObject, URI annotation) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Individual annotationR = manifestModel.getIndividual(annotation.normalize().toString());
        if (annotationR.hasProperty(AO.body)) {
            return URI.create(annotationR.getPropertyResourceValue(AO.body).getURI());
        } else {
            return null;
        }
    }


    @Override
    public void deleteAnnotation(URI researchObject, URI annotation) {
        OntModel manifestModel = createOntModelForNamedGraph(getManifestURI(researchObject.normalize()));
        Resource annotationR = manifestModel.getResource(annotation.normalize().toString());
        manifestModel.removeAll(annotationR, null, null);
        manifestModel.removeAll(null, null, annotationR);
    }


    @Override
    public int migrateRosr5To6(String datasource)
            throws NamingException, SQLException {
        if (datasource == null) {
            throw new IllegalArgumentException("Datasource cannot be null");
        }
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(datasource);
        Connection connection = ds.getConnection();
        if (connection == null) {
            throw new IllegalArgumentException("Connection could not be created");
        }

        NamedGraphSetDB oldGraphset = new NamedGraphSetDB(connection, "sms");

        int cnt = 0;
        Iterator<Quad> it = oldGraphset.findQuads(Node.ANY, Node.ANY, Node.ANY, Node.ANY);
        while (it.hasNext()) {
            Quad quad = it.next();
            Node g = quad.getGraphName();
            if (g.isURI()) {
                g = Node.createURI(g.getURI().replaceFirst("rosrs5", "rodl"));
            }
            Node o = quad.getObject();
            if (o.isURI()) {
                o = Node.createURI(o.getURI().replaceFirst("rosrs5", "rodl"));
            }
            Node s = quad.getSubject();
            if (s.isURI()) {
                s = Node.createURI(s.getURI().replaceFirst("rosrs5", "rodl"));
            }
            Quad newQuad = new Quad(g, s, quad.getPredicate(), o);
            if (!graphset.containsQuad(newQuad)) {
                cnt++;
            }
            graphset.addQuad(newQuad);
        }

        return cnt;
    }


    @Override
    public int changeURI(URI oldUri, URI uri) {
        int cnt = 0;
        Node old = Node.createURI(oldUri.toString());
        Node newu = Node.createURI(uri.toString());
        Iterator<Quad> it = graphset.findQuads(Node.ANY, old, Node.ANY, Node.ANY);
        while (it.hasNext()) {
            Quad quad = it.next();
            graphset.removeQuad(quad);
            graphset.addQuad(new Quad(quad.getGraphName(), newu, quad.getPredicate(), quad.getObject()));
            cnt++;
        }
        it = graphset.findQuads(Node.ANY, Node.ANY, Node.ANY, old);
        while (it.hasNext()) {
            Quad quad = it.next();
            graphset.removeQuad(quad);
            graphset.addQuad(new Quad(quad.getGraphName(), quad.getSubject(), quad.getPredicate(), newu));
            cnt++;
        }
        return cnt;
    }


    @Override
    public boolean isSnapshotURI(URI resource) {
        return isSnapshotURI(resource, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public boolean isSnapshotURI(URI resource, String modelPath, String format) {
        return getIndividual(resource, modelPath, format).hasRDFType(ROEVO.SnapshotROClass);
    }


    @Override
    public boolean isArchiveURI(URI resource) {
        return isArchiveURI(resource, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public boolean isArchiveURI(URI resource, String modelPath, String format) {
        return getIndividual(resource, modelPath, format).hasRDFType(ROEVO.ArchivedROClass);
    }


    @Override
    public URI getLiveURIFromSnapshotOrArchive(URI resource)
            throws URISyntaxException {
        return getLiveURIFromSnapshotOrArchive(resource, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public URI getLiveURIFromSnapshotOrArchive(URI resource, String modelPath, String format)
            throws URISyntaxException {
        Individual source = getIndividual(resource, modelPath, format);
        if (isSnapshotURI(resource, modelPath, format)) {
            RDFNode roNode = source.getProperty(ROEVO.isSnapshotOf).getObject();
            return new URI(roNode.toString());
        } else if (isArchiveURI(resource, modelPath, format)) {
            RDFNode roNode = source.getProperty(ROEVO.isArchiveOf).getObject();
            return new URI(roNode.toString());
        }
        return null;
    }


    @Override
    public URI getPreviousSnaphotOrArchive(URI liveRO, URI freshSnapshotOrARchive)
            throws URISyntaxException {
        return getPreviousSnaphotOrArchive(liveRO, freshSnapshotOrARchive, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public URI getPreviousSnaphotOrArchive(URI liveRO, URI freshSnapshotOrArchive, String modelPath, String format)
            throws URISyntaxException {

        Individual liveSource = getIndividual(liveRO, modelPath, format);

        StmtIterator snaphotsIterator;
        snaphotsIterator = liveSource.listProperties(ROEVO.hasSnapshot);
        StmtIterator archiveItertator;
        archiveItertator = liveSource.listProperties(ROEVO.hasArchive);

        Individual freshSource = getIndividual(freshSnapshotOrArchive, modelPath, format);
        RDFNode dateNode;
        if (isSnapshotURI(freshSnapshotOrArchive, modelPath, format)) {
            dateNode = freshSource.getProperty(ROEVO.snapshottedAtTime).getObject();
        } else if (isArchiveURI(freshSnapshotOrArchive, modelPath, format)) {
            dateNode = freshSource.getProperty(ROEVO.archivedAtTime).getObject();
        } else {
            return null;
        }

        DateTime freshTime = new DateTime(dateNode.asLiteral().getValue().toString());
        DateTime predecessorTime = null;
        URI result = null;

        while (snaphotsIterator.hasNext()) {
            URI tmpURI = new URI(snaphotsIterator.next().getObject().toString());
            RDFNode node = getIndividual(tmpURI, modelPath, format).getProperty(ROEVO.snapshottedAtTime).getObject();
            DateTime tmpTime = new DateTime(node.asLiteral().getValue().toString());
            if ((tmpTime.compareTo(freshTime) == -1)
                    && ((predecessorTime == null) || (tmpTime.compareTo(predecessorTime) == 1))) {
                predecessorTime = tmpTime;
                result = tmpURI;
            }
        }
        while (archiveItertator.hasNext()) {
            URI tmpURI = new URI(archiveItertator.next().getObject().toString());
            RDFNode node = getIndividual(tmpURI, modelPath, format).getProperty(ROEVO.archivedAtTime).getObject();
            DateTime tmpTime = new DateTime(node.asLiteral().getValue().toString());
            if ((tmpTime.compareTo(freshTime) == -1)
                    && ((predecessorTime == null) || (tmpTime.compareTo(predecessorTime) == 1))) {
                predecessorTime = tmpTime;
                result = tmpURI;
            }
        }
        return result;
    }


    private enum Direction {
        NEW,
        DELETED
    }


    @Override
    public String storeAggregatedDifferences(URI freshObjectURI, URI antecessorObjectURI)
            throws URISyntaxException {
        return storeAggregatedDifferences(freshObjectURI, antecessorObjectURI, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public String storeAggregatedDifferences(URI freshObjectURI, URI antecessorObjectURI, String modelPath,
            String format)
            throws URISyntaxException {
        if (freshObjectURI == null) {
            throw new NullPointerException("Frsh object URI can not be null");
        }
        if (antecessorObjectURI == null) {
            return "";
        }

        Individual freshObjectSource = getIndividual(freshObjectURI, modelPath, format);
        Individual antecessorObjectSource = getIndividual(antecessorObjectURI, modelPath, format);
        List<RDFNode> freshAggregatesList = freshObjectSource.listPropertyValues(ORE.aggregates).toList();
        List<RDFNode> antecessorAggregatesList = antecessorObjectSource.listPropertyValues(ORE.aggregates).toList();
        OntModel freshROModel = createOntModelForNamedGraph(resolveURI(freshObjectURI, ".ro/manifest.rdf"));
        //@TODO check if evo_inf exists remove it ??
        if (graphset.containsGraph(resolveURI(freshObjectURI, ".ro/evo_inf.rdf").toString())) {
            graphset.removeGraph(resolveURI(freshObjectURI, ".ro/evo_inf.rdf").toString());
        }

        OntModel freshROEvoInfoModel = createOntModelForNamedGraph(resolveURI(freshObjectURI, ".ro/evo_inf.rdf"));
        Individual freshROInvidual = freshROModel.getIndividual(freshObjectURI.toString());
        Individual changeSpecificationIndividual = freshROEvoInfoModel.createIndividual(
            resolveURI(freshObjectURI, ".ro/evo_inf.rdf").toString(), ROEVO.ChangeSpecificationClass);
        freshROInvidual.addProperty(ROEVO.wasChangedBy,
            freshROModel.createResource(resolveURI(freshObjectURI, ".ro/evo_inf.rdf").toString()));
        String result = lookForAggregatedDifferents(freshObjectURI, antecessorObjectURI, freshAggregatesList,
            antecessorAggregatesList, freshROEvoInfoModel, changeSpecificationIndividual, Direction.NEW);
        result += lookForAggregatedDifferents(freshObjectURI, antecessorObjectURI, antecessorAggregatesList,
            freshAggregatesList, freshROEvoInfoModel, changeSpecificationIndividual, Direction.DELETED);
        return result;
    }


    private String lookForAggregatedDifferents(URI freshObjectURI, URI antecessorObjectURI, List<RDFNode> pattern,
            List<RDFNode> compared, OntModel freshROModel, Individual changeSpecificationIndividual, Direction direction) {
        String result = "";
        Boolean tmp = null;
        for (RDFNode patternNode : pattern) {
            Boolean loopResult = null;
            for (RDFNode comparedNode : compared) {
                if (direction == Direction.NEW) {
                    tmp = compareProprties(patternNode, comparedNode, freshObjectURI, antecessorObjectURI);
                } else {
                    tmp = compareProprties(patternNode, comparedNode, antecessorObjectURI, freshObjectURI);
                }
                if (tmp != null) {
                    loopResult = tmp;
                }
            }
            result += serviceDetectedEVOmodification(loopResult, freshObjectURI, antecessorObjectURI, patternNode,
                freshROModel, changeSpecificationIndividual, direction);
        }
        return result;
    }


    private String serviceDetectedEVOmodification(Boolean loopResult, URI freshObjectURI, URI antecessorObjectURI,
            RDFNode node, OntModel freshRoModel, Individual changeSpecificatinIndividual, Direction direction) {
        String result = "";
        //Null means they are not comparable. Resource is new or deleted depends on the direction. 
        if (loopResult == null) {
            if (direction == Direction.NEW) {
                Individual changeIndividual = freshRoModel.createIndividual(ROEVO.ChangeClass);
                changeIndividual.addRDFType(ROEVO.AdditionClass);
                changeIndividual.addProperty(ROEVO.relatedResource, node);
                changeSpecificatinIndividual.addProperty(ROEVO.hasChange, changeIndividual);
                result += freshObjectURI + " " + node.toString() + " " + direction + "\n";
            } else {
                Individual changeIndividual = freshRoModel.createIndividual(ROEVO.ChangeClass);
                changeIndividual.addRDFType(ROEVO.RemovalClass);
                changeIndividual.addProperty(ROEVO.relatedResource, node);
                changeSpecificatinIndividual.addProperty(ROEVO.hasChange, changeIndividual);
                result += freshObjectURI + " " + node.toString() + " " + direction + "\n";
            }
        }
        //False means there are some changes (Changes exists in two directions so they will be stored onlu once)
        else if (loopResult == false && direction == Direction.NEW) {
            Individual changeIndividual = freshRoModel.createIndividual(ROEVO.ChangeClass);
            changeIndividual.addRDFType(ROEVO.ModificationClass);
            changeIndividual.addProperty(ROEVO.relatedResource, node);
            changeSpecificatinIndividual.addProperty(ROEVO.hasChange, changeIndividual);
            result += freshObjectURI + " " + node.toString() + " MODIFICATION" + "\n";
        }
        //True means there are some unchanges objects
        else if (loopResult == false && direction == Direction.NEW) {
            result += freshObjectURI + " " + node.toString() + " UNCHANGED" + "\n";
        }
        return result;
    }


    private Boolean compareProprties(RDFNode pattern, RDFNode compared, URI patternROURI, URI comparedROURI) {
        if (pattern.isResource() && compared.isResource()) {
            return compareTwoResources(pattern.asResource(), compared.asResource(), patternROURI, comparedROURI);
        } else if (pattern.isLiteral() && compared.isLiteral()) {
            return compareTwoLiterals(pattern.asLiteral(), compared.asLiteral());
        }
        return null;
    }


    private Boolean compareTwoLiterals(Literal pattern, Literal compared) {
        //@TODO compare checksums
        Boolean result = null;
        if (pattern.equals(compared)) {
            //@TODO compare checksums
            return true;
        }
        return result;
    }


    private Boolean compareRelativesURI(URI patternURI, URI comparedURI, URI baseForPatternURI, URI baseForComparedURI) {
        return baseForPatternURI.relativize(patternURI).toString()
                .equals(baseForComparedURI.relativize(comparedURI).toString());
    }


    private Boolean compareTwoResources(Resource pattern, Resource compared, URI patternROURI, URI comparedROURI) {
        Individual patternSource = pattern.as(Individual.class);
        Individual comparedSource = compared.as(Individual.class);
        if (patternSource.hasRDFType(RO.AggregatedAnnotation) && comparedSource.hasRDFType(RO.AggregatedAnnotation)) {
            try {
                if (compareRelativesURI(new URI(pattern.getURI()), new URI(compared.getURI()), patternROURI,
                    comparedROURI)) {
                    return compareTwoAggreagatedResources(patternSource, comparedSource, patternROURI, comparedROURI)
                            && compareTwoAggregatedAnnotationBody(patternSource, comparedSource, patternROURI,
                                comparedROURI);
                } else {
                    return null;
                }
            } catch (URISyntaxException e) {
                log.debug(e);
                return null;
            }
        } else {
            try {
                if (compareRelativesURI(new URI(pattern.getURI()), new URI(compared.getURI()), patternROURI,
                    comparedROURI)) {
                    //@TODO compare checksums
                    return true;
                } else {
                    return null;
                }
            } catch (URISyntaxException e) {
                log.info(e);
                return null;
            }
        }
    }


    private Boolean compareTwoAggreagatedResources(Individual pattern, Individual compared, URI patternROURI,
            URI comparedROURI) {
        List<RDFNode> patternList = pattern.listPropertyValues(RO.annotatesAggregatedResource).toList();
        List<RDFNode> comparedList = compared.listPropertyValues(RO.annotatesAggregatedResource).toList();
        if (patternList.size() != comparedList.size())
            return false;
        for (RDFNode patternNode : patternList) {
            Boolean result = null;
            for (RDFNode comparedNode : comparedList) {
                if (comparedNode.isResource() && patternNode.isResource()) {
                    try {
                        if (compareRelativesURI(new URI(patternNode.asResource().getURI()), new URI(comparedNode
                                .asResource().getURI()), patternROURI, comparedROURI)) {
                            result = true;
                        }
                    } catch (URISyntaxException e) {
                        log.debug(e);
                    }
                } else if (comparedNode.isLiteral() && patternNode.isLiteral()) {
                    if (compareTwoLiterals(comparedNode.asLiteral(), patternNode.asLiteral())) {
                        result = true;
                    }
                }
            }
            if (result == null) {
                return false;
            }
        }
        for (RDFNode comparedNode : comparedList) {
            Boolean result = null;
            for (RDFNode patternNode : patternList) {
                if (comparedNode.isResource() && patternNode.isResource()) {
                    try {
                        if (compareRelativesURI(new URI(patternNode.asResource().getURI()), new URI(comparedNode
                                .asResource().getURI()), patternROURI, comparedROURI)) {
                            result = true;
                        }
                    } catch (URISyntaxException e) {
                        log.debug(e);
                        return null;
                    }
                } else if (comparedNode.isLiteral() && patternNode.isLiteral()) {
                    if (compareTwoLiterals(comparedNode.asLiteral(), patternNode.asLiteral())) {
                        result = true;
                    }
                }
            }
            if (result == false || result == null) {
                return false;
            }
        }
        return true;
    }


    private Boolean compareTwoAggregatedAnnotationBody(Individual patternSource, Individual comparedSource,
            URI patternROURI, URI comparedROURI) {
        Resource patternBody = patternSource.getProperty(AO.body).getResource();
        Resource comparedBody = comparedSource.getProperty(AO.body).getResource();
        OntModel patternModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        patternModel.read(patternBody.getURI(), "TTL");
        OntModel comparedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        comparedModel.read(comparedBody.getURI(), "TTL");

        List<Statement> patternList = patternModel.listStatements().toList();
        List<Statement> comparedList = comparedModel.listStatements().toList();

        for (Statement s : patternList) {
            try {
                if (!isStatementInList(s, comparedList, patternROURI, comparedROURI)) {
                    return false;
                }
            } catch (URISyntaxException e) {
                log.debug(e);
                return true;
            }
        }
        for (Statement s : comparedList) {
            try {
                if (!isStatementInList(s, patternList, comparedROURI, patternROURI)) {
                    return false;
                }
            } catch (URISyntaxException e) {
                log.debug(e);
                return true;
            }
        }
        return true;
    }


    private Boolean isStatementInList(Statement statement, List<Statement> list, URI patternURI, URI comparedURI)
            throws URISyntaxException {
        for (Statement listStatement : list) {
            if (compareRelativesURI(new URI(statement.getSubject().asResource().getURI()), new URI(listStatement
                    .getSubject().asResource().getURI()), patternURI, comparedURI)) {
                if (compareRelativesURI(new URI(statement.getPredicate().asResource().getURI()), new URI(listStatement
                        .getPredicate().asResource().getURI()), patternURI, comparedURI)) {
                    if (statement.getObject().isResource() && listStatement.getObject().isResource()) {
                        if (compareRelativesURI(new URI(statement.getObject().asResource().getURI()), new URI(
                                listStatement.getObject().asResource().getURI()), patternURI, comparedURI)) {
                            return true;
                        }
                    }
                    if (listStatement.getObject().isLiteral() && statement.getObject().isLiteral()) {
                        if (compareTwoLiterals(listStatement.getObject().asLiteral(), statement.getObject().asLiteral())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    @Override
    public Individual getIndividual(URI resource) {
        return getIndividual(resource, W4E.DEFAULT_MANIFEST_PATH, "RDF/XML");
    }


    @Override
    public Individual getIndividual(URI resource, String modelPath, String format) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        model.read(resource.resolve(modelPath).toString(), format);
        Individual source = model.getIndividual(resource.toString());
        return source;
    }


    @Override
    public String getDefaultManifestPath() {
        return W4E.DEFAULT_MANIFEST_PATH;
    }


    @Override
    public URI resolveURI(URI base, String second) {
        if ("file".equals(base.getScheme())) {
            URI incorrectURI = base.resolve(second);
            return URI.create(incorrectURI.toString().replaceFirst("file:/", "file:///"));
        } else {
            return base.resolve(second);
        }
    }


    @Override
    public int changeURIInManifestAndAnnotationBodies(URI researchObject, URI oldURI, URI newURI) {
        int cnt = changeURIInNamedGraph(getManifestURI(researchObject), oldURI, newURI);
        OntModel model = createOntModelForNamedGraph(getManifestURI(researchObject));
        List<RDFNode> bodies = model.listObjectsOfProperty(AO.body).toList();
        for (RDFNode body : bodies) {
            URI bodyURI = URI.create(body.asResource().getURI());
            cnt += changeURIInNamedGraph(bodyURI, oldURI, newURI);
        }
        return cnt;
    }


    private int changeURIInNamedGraph(URI graph, URI oldURI, URI newURI) {
        OntModel model = createOntModelForNamedGraph(graph);
        Resource oldResource = model.createResource(oldURI.toString());
        Resource newResource = model.createResource(newURI.toString());
        List<Statement> s1 = model.listStatements(oldResource, null, (RDFNode) null).toList();
        for (Statement s : s1) {
            model.remove(s.getSubject(), s.getPredicate(), s.getObject());
            model.add(newResource, s.getPredicate(), s.getObject());
        }
        List<Statement> s2 = model.listStatements(null, null, oldResource).toList();
        for (Statement s : s2) {
            model.remove(s.getSubject(), s.getPredicate(), s.getObject());
            model.add(s.getSubject(), s.getPredicate(), newResource);
        }
        return s1.size() + s2.size();
    }


    @Override
    public InputStream getEvoInfo(URI researchObjectURI) {
        return getNamedGraph(resolveURI(researchObjectURI, ".ro/evo_inf.rdf"), RDFFormat.TURTLE);
    }


    @Override
    public List<URI> getAggregatedResources(URI researchObject)
            throws ManifestTraversingException {
        OntModel model = createOntModelForNamedGraph(researchObject);
        StmtIterator list = model.listStatements();
        Individual source = model.getIndividual(researchObject.toString());
        if (source == null) {
            throw new ManifestTraversingException();
        }
        List<RDFNode> aggregatesList = source.listPropertyValues(ORE.aggregates).toList();
        List<URI> aggregated = new ArrayList<URI>();
        for (RDFNode node : aggregatesList) {
            try {
                if (!isAnnotation(researchObject, new URI(node.asResource().getURI()))) {
                    aggregated.add(new URI(node.asResource().getURI()));
                }
            } catch (URISyntaxException e) {
                continue;
            }
        }
        return aggregated;
    }


    @Override
    public List<URI> getAnnotationTargets(URI researchObject, URI annotationURI) {
        return null;
    }
}
