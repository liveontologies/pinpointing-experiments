package org.liveontologies.pinpointing;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.liveontologies.proofs.adapters.Proofs;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.statistics.NestedStats;

/**
 * Provided justification computation applied to the binarization of the input
 * proof.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of conclusion and premises used by the inferences
 * @param <I>
 *            the type of inferences used in proofs
 * @param <A>
 *            the type of axioms used by the inferences
 */
public class BinarizedJustificationComputation<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private final MinimalSubsetEnumerator.Factory<List<C>, A> enumeratorFactory_;

	BinarizedJustificationComputation(
			final MinimalSubsetsFromProofs.Factory<List<C>, Inference<List<C>>, A> mainFactory,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
		enumeratorFactory_ = mainFactory.create(Proofs.<C, I> binarize(proof),
				Proofs.<C, I, A> binarize(justifier), monitor);
	}

	@Override
	public MinimalSubsetEnumerator<A> newEnumerator(final C query) {
		return enumeratorFactory_
				.newEnumerator(Collections.singletonList(query));
	}

	@NestedStats
	public MinimalSubsetEnumerator.Factory<List<C>, A> getDelegate() {
		return enumeratorFactory_;
	}

	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory(
			final MinimalSubsetsFromProofs.Factory<List<C>, Inference<List<C>>, A> mainFactory) {
		return new Factory<C, I, A>(mainFactory);
	}

	private static class Factory<C, I extends Inference<? extends C>, A>
			implements MinimalSubsetsFromProofs.Factory<C, I, A> {

		private final MinimalSubsetsFromProofs.Factory<List<C>, Inference<List<C>>, A> mainFactory_;

		Factory(MinimalSubsetsFromProofs.Factory<List<C>, Inference<List<C>>, A> mainFactory) {
			this.mainFactory_ = mainFactory;
		}

		@Override
		public MinimalSubsetEnumerator.Factory<C, A> create(
				final Proof<? extends I> proof,
				final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
				final InterruptMonitor monitor) {
			return new BinarizedJustificationComputation<C, I, A>(mainFactory_,
					proof, justifier, monitor);
		}

	}

}