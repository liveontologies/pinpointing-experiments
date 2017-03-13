package org.semanticweb.elk.justifications;

import org.liveontologies.puli.GenericInferenceSet;
import org.liveontologies.puli.JustifiedInference;
import org.semanticweb.elk.justifications.ResolutionJustificationComputation.DerivedInference;
import org.semanticweb.elk.justifications.ResolutionJustificationComputation.SelectionFunction;

public class ThresholdSelection<C, A> implements SelectionFunction<C, A> {

	private final int threshold_;

	public ThresholdSelection(final int threshold) {
		this.threshold_ = threshold;
	}

	public ThresholdSelection() {
		this(2);
	}

	@Override
	public C getResolvingAtom(DerivedInference<C, A> inference,
			GenericInferenceSet<C, ? extends JustifiedInference<C, A>> inferences,
			C goal) {
		// select the premise derived by the fewest inferences
		// unless the number of such inferences is larger than the
		// give threshold and the conclusion is not the goal;
		// in this case select the conclusion
		int minInferenceCount = Integer.MAX_VALUE;
		C result = null;
		for (C c : inference.getPremises()) {
			int inferenceCount = inferences.getInferences(c).size();
			if (inferenceCount < minInferenceCount) {
				result = c;
				minInferenceCount = inferenceCount;
			}
		}
		if (minInferenceCount > threshold_
				&& !goal.equals(inference.getConclusion())) {
			// resolve on the conclusion
			result = null;
		}
		return result;
	}

}