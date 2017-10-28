package org.liveontologies.pinpointing.experiments;

import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Factory;
import org.liveontologies.puli.pinpointing.ResolutionJustificationComputation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class OwlResolutionJustificationExperiment extends
		OwlJustificationExperiment<OwlResolutionJustificationExperiment.Options> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(OwlResolutionJustificationExperiment.class);

	public static final String SELECTION_OPT = "selection";

	public static class Options extends OwlJustificationExperiment.Options {
		@Arg(dest = SELECTION_OPT)
		public ResolutionJustificationComputation.SelectionType selectionType;
	}

	private ResolutionJustificationComputation.SelectionType selectionType_;

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		super.addArguments(parser);
		parser.description(
				"Experiment using Resolutionun Justification Computation and OWL API proofs from ELK.");
		parser.addArgument(SELECTION_OPT)
				.type(ResolutionJustificationComputation.SelectionType.class)
				.help("selection type");
	}

	@Override
	protected void init(final Options options) throws ExperimentException {
		super.init(options);
		LOGGER_.info("selectionType: {}", options.selectionType);
		this.selectionType_ = options.selectionType;
	}

	@Override
	protected Factory<OWLAxiom, OWLAxiom> newComputation(
			final Proof<? extends Inference<OWLAxiom>> proof,
			final InferenceJustifier<? super Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> justifier,
			final InterruptMonitor monitor) throws ExperimentException {
		return ResolutionJustificationComputation
				.<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> getFactory()
				.create(proof, justifier, monitor, selectionType_);
	}

}
