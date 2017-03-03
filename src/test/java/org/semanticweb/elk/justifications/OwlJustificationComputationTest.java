package org.semanticweb.elk.justifications;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.liveontologies.owlapi.proof.OWLProver;
import org.liveontologies.puli.GenericInferenceSet;
import org.liveontologies.puli.JustifiedInference;
import org.semanticweb.elk.owlapi.ElkProverFactory;
import org.semanticweb.elk.proofs.adapters.InferenceSets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

public abstract class OwlJustificationComputationTest
		extends BaseJustificationComputationTest<OWLAxiom, OWLAxiom> {

	private static final OWLOntologyManager OWL_MANAGER_ = OWLManager
			.createOWLOntologyManager();

	private final OWLProver prover_;

	public OwlJustificationComputationTest(
			final JustificationComputation.Factory<OWLAxiom, OWLAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws OWLOntologyCreationException {
		super(factory, ontoFile, entailFilesPerJustFile);

		this.prover_ = new ElkProverFactory().createReasoner(
				OWL_MANAGER_.loadOntologyFromOntologyDocument(ontoFile));

	}

	@Override
	public Set<? extends Set<? extends OWLAxiom>> getActualJustifications(
			final File entailFile) throws OWLOntologyCreationException {

		final OWLAxiom entailment = OWL_MANAGER_
				.loadOntologyFromOntologyDocument(entailFile).getLogicalAxioms()
				.iterator().next();

		final GenericInferenceSet<OWLAxiom, ? extends JustifiedInference<OWLAxiom, OWLAxiom>> inferenceSet = InferenceSets
				.justifyAsserted(prover_.getProof(entailment),
						prover_.getRootOntology().getAxioms(Imports.EXCLUDED));

		final JustificationComputation<OWLAxiom, OWLAxiom> computation = getFactory()
				.create(inferenceSet, DummyMonitor.INSTANCE);

		return new HashSet<>(computation.computeJustifications(entailment));
	}

	@Override
	public Set<? extends Set<? extends OWLAxiom>> getExpectedJustifications(
			final File[] justFiles) throws OWLOntologyCreationException {
		final Set<Set<? extends OWLAxiom>> expectedJusts = new HashSet<>();
		for (final File justFile : justFiles) {
			final OWLOntology just = OWL_MANAGER_
					.loadOntologyFromOntologyDocument(justFile);
			expectedJusts.add(just.getLogicalAxioms());
		}
		return expectedJusts;
	}

	@Override
	public void dispose() {
		super.dispose();
		prover_.dispose();
		final Collection<OWLOntology> ontologies = new ArrayList<>(
				OWL_MANAGER_.getOntologies());
		for (final OWLOntology ontology : ontologies) {
			OWL_MANAGER_.removeOntology(ontology);
		}
	}

}