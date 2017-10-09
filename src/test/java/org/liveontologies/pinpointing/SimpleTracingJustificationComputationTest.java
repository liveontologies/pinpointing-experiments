package org.liveontologies.pinpointing;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.reasoner.tracing.Conclusion;
import org.semanticweb.elk.reasoner.tracing.TracingInference;

public class SimpleTracingJustificationComputationTest
		extends TracingJustificationComputationTest {

	@Parameters
	public static Collection<Object[]> parameters() throws URISyntaxException {
		return BaseJustificationComputationTest.getParameters(
				getJustificationComputationFactories(), "test_input/simple",
				JUSTIFICATION_DIR_NAME);
	}

	public SimpleTracingJustificationComputationTest(
			final MinimalSubsetsFromProofs.Factory<Conclusion, TracingInference, ElkAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws Exception {
		super(factory, ontoFile, entailFilesPerJustFile);
	}

}