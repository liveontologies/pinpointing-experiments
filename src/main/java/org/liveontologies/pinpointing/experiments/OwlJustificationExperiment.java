package org.liveontologies.pinpointing.experiments;

import java.io.File;
import java.util.Set;

import org.liveontologies.owlapi.proof.OWLProver;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.InferenceJustifiers;
import org.liveontologies.puli.Proof;
import org.semanticweb.elk.owlapi.ElkProverFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlJustificationExperiment extends
		ReasonerJustificationExperiment<OWLAxiom, Inference<OWLAxiom>, OWLAxiom, OWLProver> {

	private static final Logger LOG = LoggerFactory
			.getLogger(OwlJustificationExperiment.class);

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

	@Override
	protected OWLProver loadAndClassifyOntology(final String ontologyFileName)
			throws ExperimentException {

		try {

			LOG.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont = getManager()
					.loadOntologyFromOntologyDocument(
							new File(ontologyFileName));
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
	protected Proof<? extends Inference<OWLAxiom>> newProof(
			final OWLAxiom query) throws ExperimentException {
		return getReasoner().getProof(query);
	}

	@Override
	protected InferenceJustifier<Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> newJustifier()
			throws ExperimentException {
		return InferenceJustifiers.justifyAssertedInferences();
	}

}
