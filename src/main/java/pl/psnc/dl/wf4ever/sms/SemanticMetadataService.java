/**
 * 
 */
package pl.psnc.dl.wf4ever.sms;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
import pl.psnc.dl.wf4ever.dlibra.UserProfile;

/**
 * @author piotrhol
 * 
 */
public interface SemanticMetadataService {

	public enum Notation {
		RDF_XML, TRIG, TEXT_PLAIN
	}

	/**
	 * Create a research object (ro:ResearchObject, ore:Aggregation) described
	 * by a manifest with a given URI. Automatically creates a manifest as well
	 * (ore:ResourceMap). If URI is used already, an IllegalArgumentException is
	 * thrown.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param userProfile
	 *            profile of the user creating the research object, will be
	 *            saved as FOAF
	 * @return Research Object URI
	 */
	void createResearchObject(URI manifestURI, UserProfile userProfile);

	/**
	 * Create a copy of an existing RO under a new URI. If the existing URI
	 * cannot be found, an IllegalArgumentException is thrown.
	 * 
	 * Details to be added in the future
	 * 
	 * @param manifestURI
	 *            New manifest URI
	 * @param baseManifestURI
	 *            Original Research Object URI
	 * @return New research object URI
	 */
	void createResearchObjectAsCopy(URI manifestURI, URI baseManifestURI);

	/**
	 * Remove all research objects whose manifests match the given URI (includes
	 * its descendants).
	 * 
	 * @param manifestURI
	 *            manifest URI
	 */
	void removeResearchObject(URI manifestURI);

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
	 * Updates the manifest (ore:ResourceMap), which includes the RO metadata
	 * and proxies.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param manifest
	 *            manifest input stream
	 * @param notation
	 *            RDF/XML or Trig
	 */
	void updateManifest(URI manifestURI, InputStream manifest, Notation notation);

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
	 * To be defined in the future.
	 * 
	 * @param workspaceId
	 * @param queryParameters
	 * @return
	 */
	List<URI> findResearchObjects(String workspaceId,
			Map<String, List<String>> queryParameters);

}
