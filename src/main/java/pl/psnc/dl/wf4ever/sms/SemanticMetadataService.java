/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;

import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;

/**
 * @author piotrhol
 * 
 */
public interface SemanticMetadataService
{

	/**
	 * Create a new ro:ResearchObject and ro:Manifest.
	 * 
	 * @param researchObjectURI
	 *            RO URI, absolute
	 */
	void createResearchObject(URI researchObjectURI);


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
	 * Removes a research object, its manifest, proxies, internal aggregated resources and
	 * internal named graphs. A resource/named graph is considered internal if it contains
	 * the research object URI.
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
	 */
	void addResource(URI researchObjectURI, URI resourceURI, ResourceInfo resourceInfo);


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
	 * @return resource description with URIs relative to RO URI
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
	 */
	void addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat);


	/**
	 * Check if a named graph exists
	 * 
	 * @param graphURI
	 *            named graph URI
	 * @return true if a named graph with this URI exists, false otherwie
	 */
	boolean containsNamedGraph(URI graphURI);


	/**
	 * Return true if a named graph with given URI can be part of RO metadata. Such named
	 * graphs are manifests and annotation bodies. Note that the graph itself does not
	 * necessarily exist.
	 * 
	 * @param graphURI
	 *            graph URI
	 * @return
	 */
	boolean isROMetadataNamedGraph(URI researchObjectURI, URI graphURI);


	/**
	 * Get a named graph. If the named graph references other named graphs and the RDF
	 * format is TriG or TriX, referenced named graphs are returned as well.
	 * 
	 * @param graphURI
	 *            graph URI
	 * @param rdfFormat
	 *            response format
	 * @return
	 */
	InputStream getNamedGraph(URI graphURI, RDFFormat rdfFormat);


	/**
	 * Delete a named graph from the quadstore.
	 * 
	 * @param roURI
	 *            the RO URI, used for distinguishing internal resources from external
	 * @param graphURI
	 *            graph URI
	 */
	void removeNamedGraph(URI researchObjectURI, URI graphURI);


	//TODO limit results depending on the user
	/**
	 * List ro:ResearchObject resources that start with the given URI.
	 * 
	 * @param partialURI
	 *            URI with which the RO URI must start. If null, all ROs are returned.
	 * 
	 * @return set of RO URIs
	 */
	Set<URI> findResearchObjects(URI partialURI);


	ByteArrayOutputStream executeSparql(String query, RDFFormat rdfFormat);


	/**
	 * Closes the SemanticMetadataService and frees up resources held. Any subsequent
	 * calls to methods of the object have undefined results.
	 */
	void close();

}
