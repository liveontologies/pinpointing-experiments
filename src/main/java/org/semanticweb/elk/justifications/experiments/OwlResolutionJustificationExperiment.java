package org.semanticweb.elk.justifications.experiments;

import java.io.File;
import java.util.Set;

import org.liveontologies.owlapi.proof.OWLProver;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;
import org.semanticweb.elk.owlapi.ElkProverFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class OwlResolutionJustificationExperiment
		extends ResolutionJustificationExperiment<OWLAxiom, OWLAxiom> {

	private static final Logger LOG = LoggerFactory
			.getLogger(OwlResolutionJustificationExperiment.class);

	public static final String ONTOLOGY_OPT = "ontology";

	private OWLOntologyManager manager_ = null;
	private OWLDataFactory factory_ = null;

	private OWLOntologyManager getManager() {
		if (manager_ == null) {
			manager_ = OWLManager.createOWLOntologyManager();
		}
		return manager_;
	}

	private OWLDataFactory getFactory() {
		if (factory_ == null) {
			factory_ = getManager().getOWLDataFactory();
		}
		return factory_;
	}

	private OWLProver reasoner_;

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(ONTOLOGY_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("ontology file");
	}

	@Override
	protected void init(final Namespace options) throws ExperimentException {
		manager_ = null;
		reasoner_ = loadAndClassifyOntology(options.<File> get(ONTOLOGY_OPT));
	}

	protected OWLProver loadAndClassifyOntology(final File ontologyFileName)
			throws ExperimentException {

		try {

			LOG.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont = getManager()
					.loadOntologyFromOntologyDocument(ontologyFileName);
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);
			LOG.info("Loaded ontology: {}", ont.getOntologyID());

			final OWLProver prover = new ElkProverFactory().createReasoner(ont);

			LOG.info("Classifying ...");
			start = System.currentTimeMillis();
			prover.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

			return prover;
		} catch (final OWLOntologyCreationException e) {
			throw new ExperimentException(e);
		}

	}

	public OWLProver getReasoner() {
		return reasoner_;
	}

	@Override
	protected OWLAxiom decodeQuery(final String query)
			throws ExperimentException {
		return CsvQueryDecoder.decode(query,
				new CsvQueryDecoder.Factory<OWLAxiom>() {

					@Override
					public OWLAxiom createQuery(final String subIri,
							final String supIri) {
						return getFactory().getOWLSubClassOfAxiom(
								getFactory().getOWLClass(IRI.create(subIri)),
								getFactory().getOWLClass(IRI.create(supIri)));
					}

				});
	}

	@Override
	protected Proof<OWLAxiom> newProof(final OWLAxiom query)
			throws ExperimentException {
		return Proofs.addAssertedInferences(getReasoner().getProof(query),
				getReasoner().getRootOntology().getAxioms(Imports.EXCLUDED));
	}

	@Override
	protected InferenceJustifier<OWLAxiom, ? extends Set<? extends OWLAxiom>> newJustifier()
			throws ExperimentException {
		return Proofs.justifyAssertedInferences();
	}

}
