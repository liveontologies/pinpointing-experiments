package org.liveontologies.pinpointing;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.liveontologies.proofs.TracingInferenceJustifier;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkDeclarationAxiom;
import org.semanticweb.elk.owl.visitors.DummyElkAxiomVisitor;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.elk.reasoner.TestReasonerUtils;
import org.semanticweb.elk.reasoner.tracing.Conclusion;
import org.semanticweb.elk.reasoner.tracing.TracingInference;

public abstract class TracingJustificationComputationTest extends
		BaseJustificationComputationTest<Conclusion, TracingInference, ElkAxiom> {

	private final Reasoner reasoner_;

	public TracingJustificationComputationTest(
			final MinimalSubsetsFromProofs.Factory<Conclusion, TracingInference, ElkAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws Exception {
		super(factory, ontoFile, entailFilesPerJustFile);

		this.reasoner_ = TestReasonerUtils
				.createTestReasoner(new FileInputStream(ontoFile));

	}

	@Override
	public Set<? extends Set<? extends ElkAxiom>> getActualJustifications(
			final File entailFile) throws Exception {

		final ElkAxiom entailment = filterLogical(
				TestReasonerUtils.loadAxioms(entailFile)).iterator().next();

		final Conclusion conclusion = Utils
				.getFirstDerivedConclusionForSubsumption(reasoner_, entailment);
		final MinimalSubsetCollector<Conclusion, TracingInference, ElkAxiom> collector = new MinimalSubsetCollector<>(
				getFactory(), reasoner_.getProof(),
				TracingInferenceJustifier.INSTANCE);

		return new HashSet<>(collector.collect(conclusion));
	}

	@Override
	public Set<? extends Set<? extends ElkAxiom>> getExpectedJustifications(
			final File[] justFiles) throws Exception {
		final Set<Set<? extends ElkAxiom>> expectedJusts = new HashSet<>();
		for (final File justFile : justFiles) {
			final Set<? extends ElkAxiom> just = filterLogical(
					TestReasonerUtils.loadAxioms(justFile));
			expectedJusts.add(just);
		}
		return expectedJusts;
	}

	private static Set<? extends ElkAxiom> filterLogical(
			final Set<? extends ElkAxiom> axioms) {
		final Set<? extends ElkAxiom> result = new HashSet<>(axioms);
		final Iterator<? extends ElkAxiom> iter = result.iterator();
		while (iter.hasNext()) {
			iter.next().accept(new DummyElkAxiomVisitor<Void>() {

				@Override
				protected Void defaultNonLogicalVisit(final ElkAxiom axiom) {
					iter.remove();
					return null;
				}

				@Override
				public Void visit(final ElkDeclarationAxiom axiom) {
					iter.remove();
					return null;
				}

			});
		}
		return result;
	}

}
