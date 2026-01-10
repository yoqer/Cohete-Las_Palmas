package info.openrocket.core.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import info.openrocket.core.document.Simulation;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.core.util.BaseTestCase;
import info.openrocket.core.util.TestRockets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class DampingRatioTest extends BaseTestCase {

	// The damping ratio is computed from a handful of stored doubles; allow tiny floating-point noise.
	private static final double TOLERANCE = 1.0e-12;

	@Test
	public void testDampingRatioTypeIsDefined() {
		assertSame(UnitGroup.UNITS_COEFFICIENT, FlightDataType.TYPE_DAMPING_RATIO.getUnitGroup());
		assertEquals("\u03b6", FlightDataType.TYPE_DAMPING_RATIO.getSymbol());

		List<FlightDataType> allTypes = Arrays.asList(FlightDataType.ALL_TYPES);
		assertTrue(allTypes.contains(FlightDataType.TYPE_DAMPING_RATIO));

		// Ensure we have a non-empty translated name for UI display.
		assertFalse(FlightDataType.TYPE_DAMPING_RATIO.getName().isBlank());
	}

	@ParameterizedTest
	@EnumSource(SimulationStepperMethod.class)
	public void testDampingRatioStoredAndMatchesFormula(SimulationStepperMethod stepperMethod)
			throws SimulationException {
		Rocket rocket = TestRockets.makeEstesAlphaIII();
		Simulation simulation = new Simulation(rocket);
		simulation.setFlightConfigurationId(TestRockets.TEST_FCID_0);
		simulation.getOptions().setISAAtmosphere(true);
		simulation.getOptions().setTimeStep(0.05);
		simulation.getOptions().setRandomSeed(0xC0FFEE);
		simulation.getOptions().setSimulationStepperMethodChoice(stepperMethod);

		simulation.simulate();

		FlightDataBranch branch = simulation.getSimulatedData().getBranch(0);
		assertNotNull(branch);

		List<Double> zeta = branch.get(FlightDataType.TYPE_DAMPING_RATIO);
		List<Double> dampingMoment = branch.get(FlightDataType.TYPE_DAMPING_MOMENT_COEFF);
		List<Double> correctiveMoment = branch.get(FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF);
		List<Double> longitudinalInertia = branch.get(FlightDataType.TYPE_LONGITUDINAL_INERTIA);

		assertNotNull(zeta);
		assertNotNull(dampingMoment);
		assertNotNull(correctiveMoment);
		assertNotNull(longitudinalInertia);

		assertEquals(zeta.size(), dampingMoment.size());
		assertEquals(zeta.size(), correctiveMoment.size());
		assertEquals(zeta.size(), longitudinalInertia.size());
		assertFalse(zeta.isEmpty());

		boolean foundFinite = false;
		for (int i = 0; i < zeta.size(); i++) {
			double expected = computeExpectedZeta(dampingMoment.get(i), correctiveMoment.get(i), longitudinalInertia.get(i));
			double actual = zeta.get(i);

			if (Double.isNaN(expected)) {
				assertTrue(Double.isNaN(actual));
				continue;
			}

			assertEquals(expected, actual, TOLERANCE);
			foundFinite = true;
		}

		assertTrue(foundFinite, "Expected at least one finite ζ value");
	}

	/**
	 * Compute the expected damping ratio value using the same relationship as the simulation code:
	 * ζ = Cdm / (2 * sqrt(Ccm * IL)).
	 */
	private static double computeExpectedZeta(double dampingMomentCoefficient, double correctiveMomentCoefficient,
			double longitudinalInertia) {
		if (Double.isNaN(dampingMomentCoefficient) || Double.isNaN(correctiveMomentCoefficient) || Double.isNaN(longitudinalInertia)) {
			return Double.NaN;
		}
		if (!(longitudinalInertia > 0)) {
			return Double.NaN;
		}

		double product = correctiveMomentCoefficient * longitudinalInertia;
		if (!(product > 0)) {
			return Double.NaN;
		}

		double denominator = 2.0 * Math.sqrt(product);
		if (!(denominator > 0)) {
			return Double.NaN;
		}

		return dampingMomentCoefficient / denominator;
	}
}

