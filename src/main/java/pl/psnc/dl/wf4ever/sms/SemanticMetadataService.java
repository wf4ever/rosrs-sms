/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;

/**
 * @author piotrhol
 * 
 */
public interface SemanticMetadataService {

	public enum Notation {
		RDF_XML, TRIG, TEXT_PLAIN
	}

	/**
	 * Create a research object (ro:ResearchObject, ore:Aggregation) with a
	 * given URI. Automatically creates a manifest as well (ore:ResourceMap).
	 * 
	 * @param researchObjectURI
	 *            Research Object URI
	 */
	void createResearchObject(URI researchObjectURI);

	/**
	 * Create a copy of an existing RO under a new URI.
	 * 
	 * @param researchObjectURI
	 *            New Research Object URI
	 * @param baseResearchObjectURI
	 *            Original Research Object URI
	 */
	void createResearchObjectAsCopy(URI researchObjectURI,
			URI baseResearchObjectURI);

	/**
	 * Remove all research objects that match the given URI (includes its
	 * descendants).
	 * 
	 * @param researchObjectURI
	 *            Research Object URI
	 */
	void removeResearchObject(URI researchObjectURI);

	/**
	 * Get the RO metadata (ro:ResearchObject, ore:Aggregation).
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param notation
	 *            RDF/XML or Trig
	 * @return
	 */
	InputStream getResearchObject(URI manifestURI, Notation notation);

	/**
	 * Get the manifest (ore:ResourceMap), which includes the RO metadata and
	 * proxies.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param notation
	 *            RDF/XML or Trig
	 * @return
	 */
	InputStream getManifest(URI manifestURI, Notation notation);

	/**
	 * Add an aggregated resource (ro:Resource).
	 * 
	 * @param resourceURI
	 *            resource URI
	 * @param resourceInfo
	 *            resource name, file size and checksum
	 */
	void addResource(URI resourceURI, ResourceInfo resourceInfo);

	/**
	 * Remove an aggregated resource.
	 * 
	 * @param resourceURI
	 *            resource URI
	 */
	void removeResource(URI resourceURI);

	/**
	 * Get resource description (name, file size and checksum).
	 * 
	 * @param resourceURI
	 *            resource URI
	 * @param notation
	 *            Notation of the result. In text/plain, pairs
	 *            attribute/attribute_values are returned.
	 * @return
	 */
	InputStream getResource(URI resourceURI, Notation notation);

	/**
	 * Add an annotation (ro:GraphAnnotation, ao:Annotation).
	 * 
	 * @param annotationURI
	 *            annotation URI
	 * @param annotationBodyURI
	 *            annotation body URI
	 * @param annotatedResourceURI
	 *            annotated resource URI
	 * @param attributes
	 *            map of attributes and attribute values
	 */
	void addAnnotation(URI annotationURI, URI annotationBodyURI,
			URI annotatedResourceURI, Map<String, String> attributes);

	/**
	 * Delete all annotations bodies that match a given URI (including their
	 * descendants). All annotations with those bodies will also be deleted.
	 * 
	 * @param annotationBodyURI
	 *            annotation body URI
	 */
	void deleteAnnotationsWithBodies(URI annotationBodyURI);

	/**
	 * Get a list of annotations that match a given URI.
	 * 
	 * @param annotationsURI
	 *            annotations URI
	 * @param notation
	 *            Notation of the result
	 * @return
	 */
	InputStream getAnnotations(URI annotationsURI, Notation notation);

	/**
	 * Get an annotation body.
	 * 
	 * @param annotationBodyURI
	 *            annotation body URI
	 * @param notation
	 *            Notation of the result. In text/plain, pairs
	 *            attribute/attribute_values are returned.
	 * @return
	 */
	InputStream getAnnotationBody(URI annotationBodyURI, Notation notation);

	/**
	 * Add a proxy to the manifest.
	 * 
	 * @param proxyURI
	 *            must be a fragment of the manifest.
	 * @param proxyForURI
	 *            aggregated resource URI
	 * @param proxyInURI
	 *            research object URI
	 */
	void addProxy(URI proxyURI, URI proxyForURI, URI proxyInURI);

	/**
	 * Delete a proxy
	 * 
	 * @param proxyURI
	 *            proxy URI
	 */
	void deleteProxy(URI proxyURI);

	/**
	 * Get a proxy.
	 * 
	 * @param proxyURI
	 *            proxy URI
	 * @param notation
	 *            Notation of the result. In text/plain, the proxyFor is
	 *            returned.
	 * @return
	 */
	InputStream getProxy(URI proxyURI, Notation notation);
}
