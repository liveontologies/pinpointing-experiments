package org.semanticweb.elk.justifications;

import java.io.File;
import java.net.URISyntaxException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.collect.Iterators;

public class RealWorldOwlJustificationComputationTest
		extends OwlJustificationComputationTest {

	@Parameters
	public static Collection<Object[]> parameters() throws URISyntaxException {

		final List<JustificationComputation.Factory<?, ?>> computations = getComputationFactories();

		final Collection<Object[]> galenParams = BaseJustificationComputationTest
				.getParameters(computations, "test_input/full-galen_cel");
		final Collection<Object[]> goParams = BaseJustificationComputationTest
				.getParameters(computations, "test_input/go_cel");

		return new AbstractCollection<Object[]>() {

			@Override
			public Iterator<Object[]> iterator() {
				return Iterators.concat(galenParams.iterator(),
						goParams.iterator());
			}

			@Override
			public int size() {
				return galenParams.size() + goParams.size();
			}

		};
	}

	public RealWorldOwlJustificationComputationTest(
			final JustificationComputation.Factory<OWLAxiom, OWLAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws OWLOntologyCreationException {
		super(factory, ontoFile, entailFilesPerJustFile);
	}

}
