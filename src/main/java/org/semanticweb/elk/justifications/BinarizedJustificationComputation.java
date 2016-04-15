package org.semanticweb.elk.justifications;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.elk.proofs.InferenceSet;
import org.semanticweb.elk.proofs.adapters.InferenceSets;

/**
 * The {@link BottomUpJustificationComputation} applied to the binarization of
 * the input inference set.
 * 
 * @see BinarizedInferenceSetAdapter
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of conclusion and premises used by the inferences
 * @param <A>
 *            the type of axioms used by the inferences
 */
public class BinarizedJustificationComputation<C, A>
		extends AbstractJustificationComputation<C, A> {

	private final JustificationComputation<List<C>, A> computaiton_;

	BinarizedJustificationComputation(
			JustificationComputation.Factory<List<C>, A> mainFactory,
			InferenceSet<C, A> inferences, final Monitor monitor) {
		super(inferences);
		computaiton_ =
				mainFactory.create(InferenceSets.binarize(inferences), monitor);
	}

	@Override
	public Collection<Set<A>> computeJustifications(C conclusion) {
		return computaiton_
				.computeJustifications(Collections.singletonList(conclusion));
	}

	@Override
	public void logStatistics() {
		computaiton_.logStatistics();
	}

	public static <C, A> JustificationComputation.Factory<C, A> getFactory(
			JustificationComputation.Factory<List<C>, A> mainFactory) {
		return new Factory<C, A>(mainFactory);
	}

	private static class Factory<C, A>
			implements JustificationComputation.Factory<C, A> {

		JustificationComputation.Factory<List<C>, A> mainFactory_;

		Factory(JustificationComputation.Factory<List<C>, A> mainFactory) {
			this.mainFactory_ = mainFactory;
		}

		@Override
		public JustificationComputation<C, A> create(
				final InferenceSet<C, A> inferenceSet, final Monitor monitor) {
			return new BinarizedJustificationComputation<C, A>(mainFactory_,
					inferenceSet, monitor);
		}

	}

}