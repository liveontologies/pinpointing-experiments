package org.semanticweb.elk.justifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.GenericInferenceSet;
import org.liveontologies.puli.JustifiedInference;
import org.liveontologies.puli.Util;
import org.semanticweb.elk.statistics.NestedStats;
import org.semanticweb.elk.statistics.ResetStats;
import org.semanticweb.elk.statistics.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

// TODO: enumerate justifications (e.g., using a visitor), make priority used in the queue explicit
public class BottomUpJustificationComputation<C, A>
		extends CancellableJustificationComputation<C, A> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(BottomUpJustificationComputation.class);

	private static final int INITIAL_QUEUE_CAPACITY_ = 11;

	private static final BottomUpJustificationComputation.Factory<?, ?> FACTORY_ = new Factory<Object, Object>();

	/**
	 * conclusions for which computation of justifications has been initialized
	 */
	private final Set<C> initialized_ = new HashSet<C>();

	/**
	 * a map from conclusions to their justifications
	 */
	private final ListMultimap<C, Justification<C, A>> justifications_ = ArrayListMultimap
			.create();

	/**
	 * justifications blocked from propagation because they are not needed for
	 * computing justifications for the goal conclusion
	 */
	private final ListMultimap<C, Justification<C, A>> blockedJustifications_ = ArrayListMultimap
			.create();

	/**
	 * the maximal sizes of justifications for conclusions computed so far
	 */
	private final Map<C, Integer> sizeLimits_ = new HashMap<C, Integer>();

	/**
	 * a map from premises to inferences for relevant conclusions
	 */
	private final Multimap<C, JustifiedInference<C, A>> inferencesByPremises_ = ArrayListMultimap
			.create();

	/**
	 * newly computed justifications to be propagated
	 */
	private PriorityQueue<Justification<C, A>> toDoJustifications_;

	private JustificationVisitor<A> visitor_ = null;

	// Statistics

	private int countInferences_ = 0, countConclusions_ = 0,
			countJustificationCandidates_ = 0;

	private BottomUpJustificationComputation(
			final GenericInferenceSet<C, ? extends JustifiedInference<C, A>> inferences,
			final Monitor monitor) {
		super(inferences, monitor);
		initQueue(null);
	}

	private void reset() {
		initialized_.clear();
		justifications_.clear();
		blockedJustifications_.clear();
		sizeLimits_.clear();
		toDoJustifications_ = null;
	}

	private void initQueue(final Comparator<? super Set<A>> order) {
		this.toDoJustifications_ = new PriorityQueue<Justification<C, A>>(
				INITIAL_QUEUE_CAPACITY_, new Order(order));
	}

	@Override
	public void enumerateJustifications(final C conclusion,
			final Comparator<? super Set<A>> order,
			final JustificationVisitor<A> visitor) {
		Util.checkNotNull(visitor);
		this.visitor_ = visitor;

		boolean doNotReset = true;
		if (toDoJustifications_ != null) {
			final Comparator<? super Justification<C, A>> comparator = toDoJustifications_
					.comparator();
			if (comparator != null
					&& (comparator instanceof BottomUpJustificationComputation.Order)) {
				@SuppressWarnings("unchecked")
				final Order oldOrder = (Order) comparator;
				doNotReset = order == null ? oldOrder.originalOrder == null
						: order.equals(oldOrder.originalOrder);
			}
		}

		if (doNotReset) {
			// Visit already computed justifications. They should be in the
			// correct order.
			for (final Justification<C, A> just : justifications_
					.get(conclusion)) {
				visitor.visit(just);
			}
		} else {
			// Reset everything.
			reset();
		}

		initQueue(order);

		new JustificationEnumerator(conclusion, Integer.MAX_VALUE).process();

	}

	@Stat
	public int nProcessedInferences() {
		return countInferences_;
	}

	@Stat
	public int nProcessedConclusions() {
		return countConclusions_;
	}

	@Stat
	public int nProcessedJustificationCandidates() {
		return countJustificationCandidates_;
	}

	@Stat
	public int nJustificationsOfAllConclusions() {
		return justifications_.size();
	}

	@Stat
	public int nBlockedJustifications() {
		return blockedJustifications_.size();
	}

	@Stat
	public int maxNJustificationsOfAConclusion() {
		int max = 0;
		for (final C conclusion : justifications_.keySet()) {
			final List<Justification<C, A>> justs = justifications_
					.get(conclusion);
			if (justs.size() > max) {
				max = justs.size();
			}
		}
		return max;
	}

	@ResetStats
	public void resetStats() {
		countInferences_ = 0;
		countConclusions_ = 0;
		countJustificationCandidates_ = 0;
	}

	@NestedStats
	public static Class<?> getNestedStats() {
		return BloomSet.class;
	}

	@SuppressWarnings("unchecked")
	public static <C, A> JustificationComputation.Factory<C, A> getFactory() {
		return (Factory<C, A>) FACTORY_;
	}

	int getSizeLimit(C conclusion) {
		Integer result = sizeLimits_.get(conclusion);
		if (result == null) {
			return 0;
		}
		// else
		return result;
	}

	@SafeVarargs
	private static <C, A> Justification<C, A> createJustification(C conclusion,
			Collection<? extends A>... collections) {
		return new BloomSet<C, A>(conclusion, collections);
	}

	/**
	 * Performs computation of justifications for the given conclusion. Can
	 * compute and reuse justifications for other conclusions.
	 * 
	 * @author Yevgeny Kazakov
	 */
	private class JustificationEnumerator {

		private final C conclusion_;

		private final int sizeLimit_;

		/**
		 * the conclusions that are relevant for the computation of the
		 * justifications, i.e., those from which the conclusion for which the
		 * justifications are computed can be derived
		 */
		private final Set<C> relevant_ = new HashSet<C>();

		/**
		 * temporary queue to compute {@link #relevant_}
		 */
		private final Queue<C> toDo_ = new LinkedList<C>();

		/**
		 * the justifications will be returned here, they come in increasing
		 * size order
		 */
		private final List<? extends Set<A>> result_;

		JustificationEnumerator(C conclusion, int sizeLimit) {
			this.conclusion_ = conclusion;
			this.sizeLimit_ = sizeLimit;
			this.result_ = justifications_.get(conclusion);
			toDo(conclusion);
			initialize();
		}

		private Collection<? extends Set<A>> getResult() {
			process();
			if (sizeLimit_ > getSizeLimit(conclusion_)) {
				sizeLimits_.put(conclusion_, sizeLimit_);
			}
			if (result_.isEmpty()) {
				return result_;
			}
			// else filter out oversized justifications
			int index = result_.size() - 1;
			while (result_.get(index).size() > sizeLimit_) {
				index--;
			}
			return result_.subList(0, index + 1);
		}

		/**
		 * traverse inferences to find relevant conclusions and create the queue
		 * of justifications to be propagated reusing previously computed
		 * justifications
		 */
		private void initialize() {

			C conclusion;
			while ((conclusion = toDo_.poll()) != null) {
				if (getSizeLimit(conclusion) >= sizeLimit_) {
					// relevant justifications already computed
					continue;
				}
				boolean initialized = initialized_.add(conclusion);
				if (initialized) {
					countConclusions_++;
					LOGGER_.trace(
							"{}: computation of justifiations initialized",
							conclusion);
				} else {
					List<Justification<C, A>> blocked = blockedJustifications_
							.get(conclusion);
					for (Justification<C, A> just : blocked) {
						LOGGER_.trace("unblocked {}", just);
						toDoJustifications_.add(just);
					}
					blocked.clear();
				}
				boolean derived = false;
				for (JustifiedInference<C, A> inf : getInferences(conclusion)) {
					LOGGER_.trace("{}: new inference", inf);
					derived = true;
					countInferences_++;
					for (C premise : inf.getPremises()) {
						inferencesByPremises_.put(premise, inf);
						toDo(premise);
					}
					if (initialized) {
						// propagate existing justifications for premises
						List<Justification<C, A>> conclusionJusts = new ArrayList<Justification<C, A>>();
						conclusionJusts.add(createJustification(
								inf.getConclusion(), inf.getJustification()));
						for (C premise : inf.getPremises()) {
							conclusionJusts = Utils.join(conclusionJusts,
									justifications_.get(premise));
						}
						for (Justification<C, A> just : conclusionJusts) {
							toDoJustifications_.add(just);
							countJustificationCandidates_++;
						}
					}
				}
				if (!derived) {
					LOGGER_.warn("{}: lemma not derived!", conclusion);
				}
			}

		}

		private void toDo(C conclusion) {
			if (!relevant_.contains(conclusion)) {
				countConclusions_++;
				relevant_.add(conclusion);
				toDo_.add(conclusion);
			}
		}

		/**
		 * process new justifications until the fixpoint
		 */
		private void process() {
			Justification<C, A> just;
			int currentSize_ = 0; //
			while ((just = toDoJustifications_.poll()) != null) {
				if (monitor_.isCancelled()) {
					return;
				}

				int size = just.size();
				if (size != currentSize_) {
					currentSize_ = size;
					if (currentSize_ > sizeLimit_) {
						// stop
						LOGGER_.trace(
								"there are justifications of size larger than {}",
								sizeLimit_);
						toDoJustifications_.add(just);
						return;
					}
					LOGGER_.debug("enumerating justifications of size {}...",
							currentSize_);
				}

				C conclusion = just.getConclusion();
				if (!relevant_.contains(conclusion)) {
					blockedJustifications_.put(conclusion, just);
					LOGGER_.trace("blocked {}", just);
					continue;
				}
				List<Justification<C, A>> justs = justifications_
						.get(conclusion);
				if (!Utils.isMinimal(just, justs)) {
					continue;
				}
				if (!Utils.isMinimal(just, result_)) {
					blockedJustifications_.put(conclusion, just);
					LOGGER_.trace("blocked {}", just);
					continue;
				}
				// else
				justs.add(just);
				LOGGER_.trace("new {}", just);
				if (conclusion_.equals(conclusion) && visitor_ != null) {
					visitor_.visit(just);
				}

				if (just.isEmpty()) {
					// all justifications are computed,
					// the inferences are not needed anymore
					for (JustifiedInference<C, A> inf : getInferences(
							conclusion)) {
						for (C premise : inf.getPremises()) {
							inferencesByPremises_.remove(premise, inf);
						}
					}
				}

				/*
				 * propagating justification over inferences
				 */
				for (JustifiedInference<C, A> inf : inferencesByPremises_
						.get(conclusion)) {

					Collection<Justification<C, A>> conclusionJusts = new ArrayList<Justification<C, A>>();
					Justification<C, A> conclusionJust = just
							.copyTo(inf.getConclusion())
							.addElements(inf.getJustification());
					conclusionJusts.add(conclusionJust);
					for (final C premise : inf.getPremises()) {
						if (!premise.equals(conclusion)) {
							conclusionJusts = Utils.join(conclusionJusts,
									justifications_.get(premise));
						}
					}

					for (Justification<C, A> conclJust : conclusionJusts) {
						toDoJustifications_.add(conclJust);
						countJustificationCandidates_++;
					}

				}

			}

		}

	}

	private class Order implements Comparator<Justification<C, A>> {

		public final Comparator<? super Set<A>> originalOrder;

		private final Comparator<? super Set<A>> setOrder_;

		public Order(final Comparator<? super Set<A>> innerOrder) {
			this.originalOrder = innerOrder;
			if (innerOrder == null) {
				setOrder_ = DEFAULT_ORDER;
			} else {
				setOrder_ = innerOrder;
			}
		}

		@Override
		public int compare(final Justification<C, A> just1,
				final Justification<C, A> just2) {
			final int result = setOrder_.compare(just1, just2);
			if (result != 0) {
				return result;
			}
			return Integer.compare(just1.getConclusion().hashCode(),
					just2.getConclusion().hashCode());
		}

	}

	/**
	 * The factory for creating a {@link BottomUpJustificationComputation}
	 * 
	 * @author Yevgeny Kazakov
	 *
	 * @param <C>
	 *            the type of conclusion and premises used by the inferences
	 * @param <A>
	 *            the type of axioms used by the inferences
	 */
	private static class Factory<C, A>
			implements JustificationComputation.Factory<C, A> {

		@Override
		public JustificationComputation<C, A> create(
				final GenericInferenceSet<C, ? extends JustifiedInference<C, A>> inferenceSet,
				final Monitor monitor) {
			return new BottomUpJustificationComputation<>(inferenceSet,
					monitor);
		}

	}

}
