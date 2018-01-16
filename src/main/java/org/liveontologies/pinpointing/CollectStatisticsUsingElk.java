package org.liveontologies.pinpointing;

import java.io.File;

import org.liveontologies.pinpointing.experiments.CsvQueryDecoder;
import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.proofs.CsvQueryProofProvider;
import org.liveontologies.proofs.ElkProofProvider;
import org.liveontologies.proofs.ProofProvider;
import org.liveontologies.puli.Inference;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class CollectStatisticsUsingElk extends
		StatisticsCollector<CollectStatisticsUsingElk.Options, Object, Inference<Object>, ElkAxiom> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(CollectStatisticsUsingElk.class);

	public static final String ONTOLOGY_OPT = "ontology";

	private OWLOntologyManager manager_ = null;

	private OWLOntologyManager getManager() {
		if (manager_ == null) {
			manager_ = OWLManager.createOWLOntologyManager();
		}
		return manager_;
	}

	public static class Options extends StatisticsCollector.Options {
		@Arg(dest = ONTOLOGY_OPT)
		public File ontologyFile;
	}

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(ONTOLOGY_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("ontology file");
	}

	@Override
	protected ProofProvider<String, Object, Inference<Object>, ElkAxiom> init(
			final Options options) throws ExperimentException {
		LOGGER_.info("ontologyFile: {}", options.ontologyFile);

		final ElkProofProvider elkProofProvider = new ElkProofProvider(
				options.ontologyFile, getManager());
		final ElkObject.Factory factory = elkProofProvider.getReasoner()
				.getElkFactory();

		final CsvQueryDecoder.Factory<ElkAxiom> decoder = new CsvQueryDecoder.Factory<ElkAxiom>() {

			@Override
			public ElkAxiom createQuery(final String subIri,
					final String supIri) {
				return factory.getSubClassOfAxiom(
						factory.getClass(new ElkFullIri(subIri)),
						factory.getClass(new ElkFullIri(supIri)));
			}

		};
		final ProofProvider<String, Object, Inference<Object>, ElkAxiom> proofProvider = new CsvQueryProofProvider<>(
				decoder, elkProofProvider);

		return proofProvider;
	}

}
