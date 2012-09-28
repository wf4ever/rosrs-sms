package pl.psnc.dl.wf4ever.vocabulary;

import java.net.URI;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DCTerms;

public class W4E {

    public static final String DEFAULT_MANIFEST_PATH = ".ro/manifest.rdf";
    
    public static final Syntax sparqlSyntax = Syntax.syntaxARQ;
    public static final URI DEFAULT_NAMED_GRAPH_URI = URI.create("sms");
    public static final PrefixMapping standardNamespaces = PrefixMapping.Factory.create()
            .setNsPrefix("ore", ORE.NAMESPACE).setNsPrefix("ro", RO.NAMESPACE).setNsPrefix("roevo", ROEVO.NAMESPACE)
            .setNsPrefix("dcterms", DCTerms.NS).setNsPrefix("foaf", FOAF.NAMESPACE).lock();
    public static final OntModel defaultModel = ModelFactory.createOntologyModel(
        new OntModelSpec(OntModelSpec.OWL_MEM),
        ModelFactory.createDefaultModel().read(RO.NAMESPACE).read(ROEVO.NAMESPACE));
}
