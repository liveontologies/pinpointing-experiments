package org.semanticweb.elk.justifications;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * A set enhanced with a Bloom filter to quickly check set inclusion. The Bloom
 * filter uses just one hash function; containment of elements is not optimized.
 * As it is common with Bloom filters, removal of elements is not supported.
 * 
 * @see Set#contains(Object)
 * @see Set#containsAll(Collection)
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of the conclusion for which the justification is computed
 * @param <A>
 *            the type of axioms in the justification
 */
class BloomHashSet<C, A> extends ForwardingSet<A>
		implements Justification<C, A>, Comparable<BloomHashSet<C, A>> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(BloomHashSet.class);

	private static final boolean COLLECT_STATS_ = true;

	public static final String STAT_NAME_CONTAINS_ALL_COUNT = "BloomHashSet.CONTAINS_ALL_COUNT";
	public static final String STAT_NAME_CONTAINS_ALL_POSITIVE = "BloomHashSet.CONTAINS_ALL_POSITIVE";
	public static final String STAT_NAME_CONTAINS_ALL_FILTERED = "BloomHashSet.CONTAINS_ALL_FILTERED";

	private static long STATS_CONTAINS_ALL_COUNT_ = 0,
			STATS_CONTAINS_ALL_POSITIVE_ = 0, STATS_CONTAINS_ALL_FILTERED_ = 0;

	private static final short SHIFT_ = 6; // 2^6 = 64 bits is good enough

	// = 11..1 SHIFT_ times
	private static final int MASK_ = (1 << SHIFT_) - 1;

	private final C conclusion_;

	private final Set<A> elements_;

	/**
	 * the age of this justification
	 */
	private final int age_;

	/**
	 * cache the size to avoid the unnecessary pointer access
	 */
	private final int size_;

	/**
	 * use this value for the second priority in the comparator (the first
	 * priority is size)
	 */
	private final int priority2_;

	/**
	 * filter for subset tests of SHIFT_ bits, each elements in the set sets one
	 * bit to 1
	 */
	private long filter_ = 0;

	@SafeVarargs
	public BloomHashSet(C conclusion, int age,
			Collection<? extends A>... collections) {
		Builder<A> elementsBuilder = new ImmutableSet.Builder<A>();
		this.conclusion_ = conclusion;
		this.age_ = age;
		for (int i = 0; i < collections.length; i++) {
			elementsBuilder.addAll(collections[i]);
		}
		this.elements_ = elementsBuilder.build();
		this.size_ = elements_.size();
		// try to group justifications for the same conclusions together
		this.priority2_ = conclusion.hashCode();
		buildFilter();
	}

	@Override
	public C getConclusion() {
		return conclusion_;
	}

	@Override
	public int getAge() {
		return age_;
	}

	@Override
	public int size() {
		return size_;
	}

	private void buildFilter() {
		for (A e : elements_) {
			filter_ |= 1 << (e.hashCode() & MASK_);
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (COLLECT_STATS_) {
			STATS_CONTAINS_ALL_COUNT_++;
		}
		if (c instanceof BloomHashSet<?, ?>) {
			BloomHashSet<?, ?> other = (BloomHashSet<?, ?>) c;
			if ((filter_ & other.filter_) != other.filter_) {
				if (COLLECT_STATS_) {
					STATS_CONTAINS_ALL_FILTERED_++;
				}
				return false;
			}
		}
		if (super.containsAll(c)) {
			if (COLLECT_STATS_) {
				STATS_CONTAINS_ALL_POSITIVE_++;
			}
			return true;
		}
		// else
		return false;
	}

	@Override
	public String toString() {
		Object[] elements = toArray();
		Arrays.sort(elements, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return String.valueOf(o1).compareTo(String.valueOf(o2));
			}
		});
		return getAge() + "-gen-" + getConclusion() + ": "
				+ Arrays.toString(elements);
	}

	@Override
	public int compareTo(BloomHashSet<C, A> o) {
		// first prioritize smaller justifications
		int sizeDiff = size_ - o.size_;
		if (sizeDiff != 0) {
			return sizeDiff;
		}
		// this makes sure that justifications for
		// the same conclusions of the same size
		// are processed consequently, if possible
		return priority2_ - o.priority2_;
	}

	public static String[] getStatNames() {
		return new String[] { STAT_NAME_CONTAINS_ALL_COUNT,
				STAT_NAME_CONTAINS_ALL_POSITIVE,
				STAT_NAME_CONTAINS_ALL_FILTERED, };
	}

	public static Map<String, Object> getStatistics() {
		final Map<String, Object> stats = new HashMap<String, Object>();
		stats.put(STAT_NAME_CONTAINS_ALL_COUNT, STATS_CONTAINS_ALL_COUNT_);
		stats.put(STAT_NAME_CONTAINS_ALL_POSITIVE,
				STATS_CONTAINS_ALL_POSITIVE_);
		stats.put(STAT_NAME_CONTAINS_ALL_FILTERED,
				STATS_CONTAINS_ALL_FILTERED_);
		return stats;
	}

	public static void logStatistics() {

		if (LOGGER_.isDebugEnabled()) {
			if (STATS_CONTAINS_ALL_COUNT_ != 0) {
				long negativeTests = STATS_CONTAINS_ALL_COUNT_
						- STATS_CONTAINS_ALL_POSITIVE_;
				if (negativeTests > 0) {
					float negativeSuccessRatio = (float) STATS_CONTAINS_ALL_FILTERED_
							/ negativeTests;
					LOGGER_.debug(
							"{} containsAll tests, {} negative, {} ({}%) filtered",
							STATS_CONTAINS_ALL_COUNT_, negativeTests,
							STATS_CONTAINS_ALL_FILTERED_,
							String.format("%.2f", negativeSuccessRatio * 100));
				} else {
					LOGGER_.debug("{} containsAll tests, all positive",
							STATS_CONTAINS_ALL_COUNT_);
				}
			}
		}
	}

	public static void resetStatistics() {
		STATS_CONTAINS_ALL_COUNT_ = 0;
		STATS_CONTAINS_ALL_FILTERED_ = 0;
		STATS_CONTAINS_ALL_POSITIVE_ = 0;
	}

	@Override
	protected Set<A> delegate() {
		return elements_;
	}

}
