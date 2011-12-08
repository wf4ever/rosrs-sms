/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

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
	 * Create a manifest (ore:ResourceMap) with a given URI. Automatically creates a 
	 * research object (ro:ResearchObject, ore:Aggregation) described by it as well. 
	 * If URI is used already, IllegalArgumentException is thrown.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @return Research Object URI
	 */
	void createManifest(URI manifestURI);


	/**
	 * Create or update a manifest (ore:ResourceMap) with a given URI. Automatically creates a 
	 * research object (ro:ResearchObject, ore:Aggregation) described by it as well. 
	 * If URI is used already, the manifest is updated. If dcterms:created date has changed, 
	 * the earlier of the two is preserved.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @return Research Object URI
	 */
	void createManifest(URI manifestURI, InputStream is, RDFFormat rdfFormat);


	/**
	 * Remove all research objects whose manifests match the given URI (includes
	 * its descendants).
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param baseURI the base URI of RO, used for distinguishing internal resources from external
	 */
	void removeManifest(URI manifestURI, URI baseURI);


	/**
	 * Get the manifest (ore:ResourceMap), which includes the RO metadata and
	 * proxies.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param notation
	 *            RDF/XML or Trig
	 * @return manifest or null
	 */
	InputStream getManifest(URI manifestURI, RDFFormat rdfFormat);


	/**
	 * Add an aggregated resource (ro:Resource).
	 * 
	 * @param resourceURI
	 *            resource URI
	 * @param resourceInfo
	 *            resource name, file size and checksum
	 */
	void addResource(URI manifestURI, URI resourceURI, ResourceInfo resourceInfo);


	/**
	 * Remove an aggregated resource.
	 * 
	 * @param resourceURI
	 *            resource URI
	 */
	void removeResource(URI manifestURI, URI resourceURI);


	/**
	 * Get an RDF resource. If the RDF format supports 
	 * named graphs, referenced named graphs will be included as well.
	 * 
	 * @param resourceURI
	 *            resource URI
	 * @param notation
	 *            Notation of the result. In text/plain, pairs
	 *            attribute/attribute_values are returned.
	 * @return resource or null
	 */
	InputStream getResource(URI resourceURI, RDFFormat rdfFormat);


	/**
	 * Return true if the resource exists and belongs to class ro:Folder
	 * @param resourceURI resource URI
	 * @return
	 */
	boolean isRoFolder(URI resourceURI);


	/**
	 * Add a named graph to the quadstore
	 * @param graphURI named graph URI
	 * @param inputStream named graph content
	 * @param rdfFormat graph content format
	 */
	void addNamedGraph(URI graphURI, InputStream inputStream, RDFFormat rdfFormat);


	/**
	 * Return true if a named graph with given URI exists.
	 * @param graphURI graph URI
	 * @return
	 */
	boolean isNamedGraph(URI graphURI);


	/**
	 * Get a named graph. If the named graph references other named graphs and the RDF format
	 * is TriG or TriX, referenced named graphs are returned as well.
	 * 
	 * @param graphURI
	 *            graph URI
	 * @param rdfFormat response format
	 * @return
	 */
	InputStream getNamedGraph(URI graphURI, RDFFormat rdfFormat);


	/**
	 * Delete a named graph from the quadstore.
	 * @param graphURI graph URI
	 * @param baseURI the base URI of RO, used for distinguishing internal resources from external
	 */
	void removeNamedGraph(URI graphURI, URI baseURI);


	/**
	 * List manifest resources that start with the given URI.
	 * 
	 * @param partialURI URI with which the manifest URI must start
	 * 
	 * @return list of manifest URIs
	 */
	Set<URI> findManifests(URI partialURI);


	/**
	 * Closes the SemanticMetadataService and frees up resources held. Any subsequent calls to methods of 
	 * the object have undefined results. 
	 */
	void close();

}
