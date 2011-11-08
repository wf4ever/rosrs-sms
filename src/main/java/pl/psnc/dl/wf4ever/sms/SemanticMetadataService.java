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
public interface SemanticMetadataService
{

	public enum Notation {
		RDF_XML, TURTLE, N3, TRIG, TRIX
	}


	/**
	 * Create a manifest (ore:ResourceMap) with a given URI. Automatically creates a 
	 * research object (ro:ResearchObject, ore:Aggregation) described by it as well. 
	 * If URI is used already, IllegalArgumentException is thrown.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param userProfile
	 *            profile of the user creating the research object, will be
	 *            saved as FOAF
	 * @return Research Object URI
	 */
	void createManifest(URI manifestURI, UserProfile userProfile);


	/**
	 * Create or update a manifest (ore:ResourceMap) with a given URI. Automatically creates a 
	 * research object (ro:ResearchObject, ore:Aggregation) described by it as well. 
	 * If URI is used already, the manifest is updated. If dcterms:created date has changed, 
	 * the earlier of the two is preserved.
	 * 
	 * @param manifestURI
	 *            manifest URI
	 * @param userProfile
	 *            profile of the user creating the research object, will be
	 *            saved as FOAF
	 * @return Research Object URI
	 */
	void createManifest(URI manifestURI, InputStream is, Notation notation, UserProfile userProfile);


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
	void removeManifest(URI manifestURI);


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
	InputStream getManifest(URI manifestURI, Notation notation);


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
	 * @return resource or null
	 */
	InputStream getResource(URI resourceURI, Notation notation);


	/**
	 * Add an annotation (ro:GraphAnnotation, ao:Annotation) together with an annotation body.
	 * 
	 * @param annotationURI
	 *            annotation URI
	 * @param annotationBodyURI
	 *            annotation body URI
	 * @param triples
	 *            map of annotated resources, attributes and attribute values, e.g. {"input.txt" => {"http://purl.org/dc/terms/title" => "My title"}}
	 */
	void addAnnotation(URI annotationURI, URI annotationBodyURI, Map<URI, Map<URI, String>> triples,
			UserProfile userProfile);


	/**
	 * Add an annotation (ro:GraphAnnotation, ao:Annotation) together with an annotation body.
	 * 
	 * @param annotationURI
	 *            annotation URI
	 * @param annotationBodyURI
	 *            annotation body URI
	 * @param is
	 *            named graph with annotated resources, attributes and attribute values
	 * @param notation
	 * 			  named graph notation
	 */
	void addAnnotation(URI annotationURI, URI annotationBodyURI, InputStream is, Notation notation,
			UserProfile userProfile);


	/**
	 * Delete an annotation and its body.
	 * 
	 * @param annotationBodyURI
	 *            annotation body URI
	 */
	void deleteAnnotationWithBody(URI annotationBodyURI);


	/**
	 * Delete all annotations bodies that match a given URI (including their
	 * descendants). All annotations with those bodies will also be deleted.
	 * 
	 * @param annotationsURI
	 *            annotations URI
	 */
	void deleteAllAnnotationsWithBodies(URI annotationsURI);


	/**
	 * Get an annotation with a given URI.
	 * 
	 * @param annotationURI
	 *            annotation URI
	 * @param notation
	 *            Notation of the result
	 * @return annotation or null
	 */
	InputStream getAnnotation(URI annotationURI, Notation notation);


	/**
	 * Returns all annotation without annotation bodies.
	 * 
	 * @param annotationsURI
	 * 				URI of all annotations
	 * @param notation
	 * 				Notation of the returned graph
	 * @return annotations with bodies or null if URI is incorrect
	 */
	InputStream getAllAnnotations(URI annotationsURI, Notation notation);


	/**
	 * Returns all annotation and annotation bodies. If notation is TRIG or TRIX, bodies
	 * are returned as named graphs, otherwise the graph is flattened and the information
	 * about who annotated what is lost.
	 * 
	 * @param annotationsURI
	 * 				URI of all annotations
	 * @param notation
	 * 				Notation of the returned graph
	 * @return annotations with bodies or null if URI is incorrect
	 */
	InputStream getAllAnnotationsWithBodies(URI annotationsURI, Notation notation);


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
	List<URI> findResearchObjects(String workspaceId, Map<String, List<String>> queryParameters);

}
