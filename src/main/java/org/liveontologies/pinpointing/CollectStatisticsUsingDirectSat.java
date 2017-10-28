package org.liveontologies.pinpointing;

import java.io.File;

import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.proofs.ProofProvider;
import org.liveontologies.proofs.SatProofProvider;
import org.liveontologies.puli.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class CollectStatisticsUsingDirectSat extends
		StatisticsCollector<CollectStatisticsUsingDirectSat.Options, Integer, Inference<Integer>, Integer> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(CollectStatisticsUsingDirectSat.class);

	public static final String INPUT_DIR_OPT = "input";

	public static class Options extends StatisticsCollector.Options {
		@Arg(dest = INPUT_DIR_OPT)
		public File inputDir;
	}

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(INPUT_DIR_OPT)
				.type(Arguments.fileType().verifyExists().verifyIsDirectory())
				.help("directory with the input");
	}

	@Override
	protected ProofProvider<String, Integer, Inference<Integer>, Integer> init(
			final Options options) throws ExperimentException {
		LOGGER_.info("inputDir: {}", options.inputDir);
		return new SatProofProvider(options.inputDir);
	}

}
