package org.liveontologies.pinpointing.experiments;

import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.ResolutionJustificationComputation;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class ElkResolutionJustificationExperiment extends
		ElkJustificationExperiment<ElkResolutionJustificationExperiment.Options> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ElkResolutionJustificationExperiment.class);

	public static final String SELECTION_OPT = "selection";

	public static class Options extends ElkJustificationExperiment.Options {
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
				"Experiment using Resolutionun Justification Computation and internal proofs from ELK.");
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
	protected MinimalSubsetEnumerator.Factory<Object, ElkAxiom> newComputation(
			final Proof<? extends Inference<Object>> proof,
			final InferenceJustifier<? super Inference<Object>, ? extends Set<? extends ElkAxiom>> justifier,
			final InterruptMonitor monitor) throws ExperimentException {
		return ResolutionJustificationComputation
				.<Object, Inference<Object>, ElkAxiom> getFactory()
				.create(proof, justifier, monitor, selectionType_);
	}

}
