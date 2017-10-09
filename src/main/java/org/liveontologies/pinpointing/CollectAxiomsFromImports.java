package org.liveontologies.pinpointing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectAxiomsFromImports {
	
	private static final Logger LOG = LoggerFactory.getLogger(
			CollectAxiomsFromImports.class);

	public static void main(final String[] args) {
		
		if (args.length < 2) {
			LOG.error("Insufficient arguments!");
			System.exit(1);
		}

		final File inputFile = new File(args[0]);
		final File outputFile = new File(args[1]);
		if (outputFile.exists()) {
			Utils.recursiveDelete(outputFile);
		}
		
		final OWLOntologyManager manager =
				OWLManager.createOWLOntologyManager();
		
		try {
			
			LOG.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont =
					manager.loadOntologyFromOntologyDocument(inputFile);
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start)/1000.0);
			LOG.info("Loaded ontology: {}", ont);
			
			final Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			for (final OWLAxiom axiom : ont.getAxioms(Imports.INCLUDED)) {
				axioms.add(axiom);
			}
			
			manager.removeOntology(ont);
			final OWLOntology outOnt =
					manager.createOntology(ont.getOntologyID());
			manager.addAxioms(outOnt, axioms);
			
			manager.saveOntology(outOnt,
					new FunctionalSyntaxDocumentFormat(),
					new FileOutputStream(outputFile));
			
			
			
		} catch (final OWLOntologyCreationException e) {
			LOG.error("Could not load the ontology!", e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOG.error("File not found!", e);
			System.exit(2);
		} catch (final OWLOntologyStorageException e) {
			LOG.error("Cannot save the output ontology!", e);
			System.exit(3);
		}
		
	}

}