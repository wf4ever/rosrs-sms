package pl.psnc.dl.wf4ever.sms;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

import com.google.common.collect.Multimap;

/**
 * @author piotrhol
 * 
 */
public interface SemanticMetadataService {

    public static final RDFFormat SPARQL_XML = new RDFFormat("XML", "application/sparql-results+xml",
            Charset.forName("UTF-8"), "xml", false, false);

    public static final RDFFormat SPARQL_JSON = new RDFFormat("JSON", "application/sparql-results+json",
            Charset.forName("UTF-8"), "json", false, false);


    /**
     * Return the user profile with which the service has been created.
     * 
     * @return the user of the service
     */
    UserProfile getUserProfile();


    /**
     * Create a new ro:ResearchObject and ro:Manifest. The new research object will have a LiveRO evolution class.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     */
    void createResearchObject(URI researchObjectURI);


    /**
     * Create a new ro:ResearchObject and ro:Manifest. The new research object will have a LiveRO evolution class, and
     * will point to its source.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param source
     *            URI of a source of the research object, may be null
     */
    void createLiveResearchObject(URI researchObjectURI, URI source);


    /**
     * Create a new ro:ResearchObject and ro:Manifest. The new research object will have a SnapshotRO evolution class.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param liveRO
     *            URI of a live research object, may be null
     */
    void createSnapshotResearchObject(URI researchObjectURI, URI liveRO);


    /**
     * Create a new ro:ResearchObject and ro:Manifest. The new research object will have a ArchivedRO evolution class.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param liveRO
     *            URI of a live research object, may be null
     */
    void createArchivedResearchObject(URI researchObjectURI, URI liveRO);


    /**
     * Update the manifest of a research object.
     * 
     * @param manifestURI
     *            manifest URI, absolute
     * @param inputStream
     *            the manifest
     * @param rdfFormat
     *            manifest RDF format
     */
    void updateManifest(URI manifestURI, InputStream inputStream, RDFFormat rdfFormat);


    /**
     * Removes a research object, its manifest, proxies, internal aggregated resources and internal named graphs. A
     * resource/named graph is considered internal if it contains the research object URI.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     */
    void removeResearchObject(URI researchObjectURI);


    /**
     * Returns the manifest of an RO.
     * 
     * @param manifestURI
     *            manifest URI, absolute
     * @param rdfFormat
     *            returned manifest format
     * @return manifest with the research object URI as base URI
     */
    InputStream getManifest(URI manifestURI, RDFFormat rdfFormat);


    /**
     * Adds a resource to ro:ResearchObject.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param resourceURI
     *            resource URI, absolute or relative to RO URI
     * @param resourceInfo
     *            resource metadata
     * @return true if a new resource is added, false if it existed
     */
    boolean addResource(URI researchObjectURI, URI resourceURI, ResourceInfo resourceInfo);


    /**
     * Removes a resource from ro:ResearchObject.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param resourceURI
     *            resource URI, absolute or relative to RO URI
     */
    void removeResource(URI researchObjectURI, URI resourceURI);


    /**
     * Returns resource metadata.
     * 
     * @param researchObjectURI
     *            RO URI, absolute
     * @param resourceURI
     *            resource URI, absolute or relative to RO URI
     * @param rdfFormat
     *            resource metadata format
     * @return resource description or null if no data found
     */
    InputStream getResource(URI researchObjectURI, URI resourceURI, RDFFormat rdfFormat);


    /**
     * Return true if the resource exists and belongs to class ro:Folder
     * 
     * @param resourceURI
     *            resource URI
     * @return true if the resource exists and belongs to class ro:Folder, false otherwise
     */
    boolean isRoFolder(URI researchObjectURI, URI resourceURI);


    /**
     * Add a named graph to the quadstore
     * 
     * @param graphURI
     *            named graph URI
     * @param inputStream
     *            named graph content
     * @param rdfFormat
     *            graph content format
     * @return true if a new named graph is added, false if it existed
     */
    boolean addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat);


    /**
     * Check if a named graph exists
     * 
     * @param graphURI
     *            named graph URI
     * @return true if a named graph with this URI exists, false otherwie
     */
    boolean containsNamedGraph(URI graphURI);


    /**
     * Checks if the resource is the manifest or there exists an annotation that has ao:body pointing to the resource.
     * Note that in case of the annotation body, it does not necessarily exist.
     * 
     * @param researchObject
     *            research object to search in
     * @param resource
     *            resource which should be pointed
     * @return true if it's a manifest or annotation body, false otherwise
     */
    boolean isROMetadataNamedGraph(URI researchObjectURI, URI graphURI);


    /**
     * Get a named graph. If the named graph references other named graphs and the RDF format is TriG or TriX,
     * referenced named graphs are returned as well.
     * 
     * @param graphURI
     *            graph URI
     * @param rdfFormat
     *            response format
     * @return named graph or null
     */
    InputStream getNamedGraph(URI graphURI, RDFFormat rdfFormat);


    /**
     * Get a portable named graph. The URIs will be relativized against the RO URI. All references to other named graphs
     * within the RO will have a file extension appended.
     * 
     * @param graphURI
     * @param rdfFormat
     * @param researchObjectURI
     * @param fileExtension
     * @return
     */
    InputStream getNamedGraphWithRelativeURIs(URI graphURI, URI researchObjectURI, RDFFormat rdfFormat);


    /**
     * Delete a named graph from the quadstore.
     * 
     * @param roURI
     *            the RO URI, used for distinguishing internal resources from external
     * @param graphURI
     *            graph URI
     */
    void removeNamedGraph(URI researchObjectURI, URI graphURI);


    /**
     * List ro:ResearchObject resources that start with the given URI.
     * 
     * @param partialURI
     *            URI with which the RO URI must start. If null, all ROs are returned.
     * 
     * @return set of RO URIs
     */
    Set<URI> findResearchObjectsByPrefix(URI partialURI);


    /**
     * List ro:ResearchObject resources that have the given author as dcterms:creator in their manifest.
     * 
     * @param user
     *            User URI.
     * 
     * @return set of RO URIs
     */
    Set<URI> findResearchObjectsByCreator(URI user);


    /**
     * List ro:ResearchObject resources.
     * 
     * @return set of RO URIs
     */
    Set<URI> findResearchObjects();


    /**
     * Responses are a available in a range of different formats. The specific formats available depend on the type of
     * SPARQL query being executed. SPARQL defines four different types of query: CONSTRUCT, DESCRIBE, SELECT and ASK.
     * 
     * CONSTRUCT and DESCRIBE queries both return RDF graphs and so the usual range of RDF serializations are available,
     * including RDF/XML, RDF/JSON, Turtle, etc.
     * 
     * SELECT queries return a tabular result set, while ASK queries return a boolean value. Results from both of these
     * query types can be returned in either SPARQL XML Results Format or SPARQL JSON Results Format.
     * 
     * See also http://www.w3.org/TR/rdf-sparql-XMLres/
     * 
     * @param query
     * @param rdfFormat
     * @return
     */
    QueryResult executeSparql(String query, RDFFormat rdfFormat);


    /**
     * Returns an RDF graph describing the given user.
     * 
     * @param userURI
     *            User URI
     * @param rdfFormat
     *            Requested RDF format, RDF/XML is the default one
     * @return A FOAF RDF graph in selected format
     */
    QueryResult getUser(URI userURI, RDFFormat rdfFormat);


    /**
     * Removes all data about a given user.
     * 
     * @param userURI
     *            User URI
     */
    void removeUser(URI userURI);


    /**
     * Returns a flat list of all attributes (facts and annotations) having a given resource as a subject. This searches
     * all named graphs, in all ROs.
     * 
     * If the property is dcterms:creator and the object is a foaf:Person, instead of the Person resource, its foaf:name
     * is put.
     * 
     * @param subjectURI
     *            URI of the resource
     * @return map of property URI with either a resource URI or a literal value (i.e. String or Calendar)
     */
    Multimap<URI, Object> getAllAttributes(URI subjectURI);


    /**
     * Closes the SemanticMetadataService and frees up resources held. Any subsequent calls to methods of the object
     * have undefined results.
     */
    void close();


    /**
     * Check if the research object aggregates a resource.
     * 
     * @param researchObject
     *            the research object
     * @param resource
     *            the resource URI
     * @return true if the research object aggregates the resource, false otherwise
     */
    boolean isAggregatedResource(URI researchObject, URI resource);


    /**
     * Create a new ro:Proxy for a resource. The resource does not have to be already aggregated.
     * 
     * @param researchObject
     *            research object to which to add the proxy
     * @param resource
     *            resource for which the proxy will be
     * @return proxy URI
     */
    URI addProxy(URI researchObject, URI resource);


    /**
     * Check if the research object defines a proxy with a given URI
     * 
     * @param researchObject
     *            research object to search
     * @param resource
     *            resource that may be a proxy
     * @return true if the resource is a proxy, false otherwise
     */
    boolean isProxy(URI researchObject, URI resource);


    /**
     * Checks if there exists a proxy that has ore:proxyFor pointing to the resource.
     * 
     * @param researchObject
     *            research object to search in
     * @param resource
     *            resource which should be pointed
     * @return true if a proxy was found, false otherwise
     */
    boolean existsProxyForResource(URI researchObject, URI resource);


    /**
     * Find a proxy that has ore:proxyFor pointing to the resource.
     * 
     * @param researchObject
     *            research object to search
     * @param resource
     *            resource that the proxy must be for
     * @return proxy URI or null
     */
    URI getProxyForResource(URI researchObject, URI resource);


    /**
     * Return the ore:proxyFor object of a proxy.
     * 
     * @param researchObject
     *            research object in which the proxy is
     * @param proxy
     *            the proxy URI
     * @return the proxyFor object URI or null if not defined
     */
    URI getProxyFor(URI researchObject, URI proxy);


    /**
     * Delete a proxy.
     * 
     * @param researchObject
     *            research object in which the proxy is
     * @param proxy
     *            the proxy URI
     */
    void deleteProxy(URI researchObject, URI proxy);


    /**
     * Add an annotation to the research object.
     * 
     * @param researchObject
     *            research object
     * @param annotationTargets
     *            a list of annotated resources
     * @param annotationBody
     *            the annotation body
     * @return URI of the annotation
     */
    URI addAnnotation(URI researchObject, List<URI> annotationTargets, URI annotationBody);


    /**
     * Update an existing annotation.
     * 
     * @param researchObject
     *            research object
     * @param annotation
     *            URI of the annotation
     * @param annotationTargets
     *            a list of annotated resources
     * @param annotationBody
     *            the annotation body
     */
    void updateAnnotation(URI researchObject, URI annotation, List<URI> annotationTargets, URI annotationBody);


    /**
     * Check if a resource is an annotation defined in a research object.
     * 
     * @param researchObject
     *            research object to search
     * @param resource
     *            resource that may be an annotation
     * @return true if this resource is an annotation in the research object, false otherwise
     */
    boolean isAnnotation(URI researchObject, URI resource);


    /**
     * Return the ao:body object of an annotation.
     * 
     * @param researchObject
     *            research object in which the annotation is
     * @param annotation
     *            the annotation URI
     * @return the ao:body object URI or null if not defined
     */
    URI getAnnotationBody(URI researchObject, URI annotation);


    /**
     * Delete an annotation.
     * 
     * @param researchObject
     *            research object in which the annotation is
     * @param annotation
     *            the annotation URI
     */
    void deleteAnnotation(URI researchObject, URI annotation);

}
