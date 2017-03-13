package org.semanticweb.elk.justifications.experiments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.liveontologies.puli.GenericInferenceSet;
import org.liveontologies.puli.JustifiedInference;
import org.semanticweb.elk.exceptions.ElkException;
import org.semanticweb.elk.justifications.Utils;
import org.semanticweb.elk.loading.AxiomLoader;
import org.semanticweb.elk.loading.Owl2StreamLoader;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.elk.owl.parsing.javacc.Owl2FunctionalStyleParserFactory;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.elk.reasoner.ReasonerFactory;
import org.semanticweb.elk.reasoner.tracing.Conclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElkResolutionJustificationExperiment
		extends ResolutionJustificationExperiment<Conclusion, ElkAxiom> {

	private static final Logger LOG = LoggerFactory
			.getLogger(ElkResolutionJustificationExperiment.class);

	private final Reasoner reasoner_;

	public ElkResolutionJustificationExperiment(final String[] args)
			throws ExperimentException {
		super(args);

		final int requiredArgCount = 2;

		if (args.length < requiredArgCount) {
			throw new ExperimentException("Insufficient arguments!");
		}

		final String ontologyFileName = args[1];

		reasoner_ = loadAndClassifyOntology(ontologyFileName);

	}

	protected Reasoner loadAndClassifyOntology(final String ontologyFileName)
			throws ExperimentException {

		InputStream ontologyIS = null;

		try {

			ontologyIS = new FileInputStream(ontologyFileName);

			final AxiomLoader.Factory loader = new Owl2StreamLoader.Factory(
					new Owl2FunctionalStyleParserFactory(), ontologyIS);
			final Reasoner reasoner = new ReasonerFactory()
					.createReasoner(loader);

			LOG.info("Classifying ...");
			long start = System.currentTimeMillis();
			reasoner.getTaxonomy();
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

			return reasoner;
		} catch (final FileNotFoundException e) {
			throw new ExperimentException(e);
		} catch (final ElkException e) {
			throw new ExperimentException(e);
		} finally {
			Utils.closeQuietly(ontologyIS);
		}

	}

	public Reasoner getReasoner() {
		return reasoner_;
	}

	@Override
	protected Conclusion decodeQuery(final String query)
			throws ExperimentException {
		return CsvQueryDecoder.decode(query,
				new CsvQueryDecoder.Factory<Conclusion>() {

					@Override
					public Conclusion createQuery(final String subIri,
							final String supIri) {

						final ElkObject.Factory factory = getReasoner()
								.getElkFactory();
						final ElkAxiom query = factory.getSubClassOfAxiom(
								factory.getClass(new ElkFullIri(subIri)),
								factory.getClass(new ElkFullIri(supIri)));

						try {
							return Utils
									.getFirstDerivedConclusionForSubsumption(
											getReasoner(), query);
						} catch (final ElkException e) {
							throw new RuntimeException(e);
						}
					}

				});
	}

	@Override
	protected GenericInferenceSet<Conclusion, ? extends JustifiedInference<Conclusion, ElkAxiom>> newInferenceSet(
			final Conclusion query) throws ExperimentException {
		try {

			return getReasoner().explainConclusion(query);

		} catch (final ElkException e) {
			throw new ExperimentException(e);
		}
	}

}