package info.openrocket.core.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.openrocket.core.aerodynamics.AerodynamicForces;
import info.openrocket.core.aerodynamics.FlightConditions;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.masscalc.RigidBody;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.simulation.listeners.AbstractSimulationListener;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.core.util.BaseTestCase;
import info.openrocket.core.util.TestRockets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class CorrectiveMomentCoefficientTest extends BaseTestCase {

	// The computed values are simple products of doubles; allow tiny floating-point noise.
	private static final double TOLERANCE = 1.0e-12;

	@Test
	public void testCorrectiveMomentCoefficientTypeIsDefined() {
		assertSame(UnitGroup.UNITS_MOMENT, FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF.getUnitGroup());
		assertEquals("Ccm", FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF.getSymbol());

		List<FlightDataType> allTypes = Arrays.asList(FlightDataType.ALL_TYPES);
		assertTrue(allTypes.contains(FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF));

		// Ensure we have a non-empty translated name for UI display.
		assertFalse(FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF.getName().isBlank());
	}

	@ParameterizedTest
	@EnumSource(SimulationStepperMethod.class)
	public void testCorrectiveMomentCoefficientStoredAndMatchesFormula(SimulationStepperMethod stepperMethod)
			throws SimulationException {
		Rocket rocket = TestRockets.makeEstesAlphaIII();
		Simulation simulation = new Simulation(rocket);
		simulation.setFlightConfigurationId(TestRockets.TEST_FCID_0);
		simulation.getOptions().setISAAtmosphere(true);
		simulation.getOptions().setTimeStep(0.05);
		simulation.getOptions().setRandomSeed(0xC0FFEE);
		simulation.getOptions().setSimulationStepperMethodChoice(stepperMethod);

		// Capture the inputs used to compute Ccm (rho, V, Ar, CNa, CP, CG) at stored data points.
		CorrectiveMomentCaptureListener captureListener = new CorrectiveMomentCaptureListener();
		simulation.simulate(captureListener);

		FlightDataBranch branch = simulation.getSimulatedData().getBranch(0);
		assertNotNull(branch);

		List<Double> ccm = branch.get(FlightDataType.TYPE_CORRECTIVE_MOMENT_COEFF);
		assertNotNull(ccm);
		assertFalse(ccm.isEmpty());

		boolean foundFinite = false;
		for (int i = 0; i < ccm.size(); i++) {
			double actual = ccm.get(i);
			if (Double.isNaN(actual)) {
				continue;
			}

			Inputs inputs = captureListener.getInputs(i);
			assertNotNull(inputs.flightConditions(), "Missing captured flight conditions for index " + i);
			assertNotNull(inputs.forces(), "Missing captured aerodynamic forces for index " + i);
			assertNotNull(inputs.rocketMass(), "Missing captured rocket mass for index " + i);
			assertTrue(inputs.launchRodCleared(), "Expected launch rod cleared for finite Ccm at index " + i);

			double expected = computeExpectedCcm(inputs);
			assertFalse(Double.isNaN(expected), "Expected a finite Ccm value at index " + i);
			assertEquals(expected, actual, TOLERANCE);
			foundFinite = true;
		}

		assertTrue(foundFinite, "Expected at least one finite Ccm value");
	}

	/**
	 * Compute the expected Ccm value using the same formula as the simulation code:
	 * Ccm = (rho/2) * V^2 * Ar * CNa * (CP - CG).
	 */
	private static double computeExpectedCcm(Inputs inputs) {
		// Mirror gating in AbstractSimulationStepper.computeCorrectiveMomentCoefficient.
		if (!inputs.launchRodCleared || inputs.flightConditions == null || inputs.rocketMass == null ||
				inputs.forces == null || inputs.forces.getCP() == null) {
			return Double.NaN;
		}
		if (Double.isNaN(inputs.flightConditions.getAOA())) {
			return Double.NaN;
		}

		double rho = inputs.flightConditions.getAtmosphericConditions().getDensity();
		double v = inputs.flightConditions.getVelocity();
		double ar = inputs.flightConditions.getRefArea();

		// OpenRocket currently stores CNa in the CP "weight" field.
		double cna = inputs.forces.getCP().getWeight();
		double cp = inputs.forces.getCP().getX();
		double cg = inputs.rocketMass.getCM().getX();

		return 0.5 * rho * v * v * ar * cna * (cp - cg);
	}

	/**
	 * Captures the inputs used for the corrective moment coefficient computation.
	 *
	 * We key captured values by the current FlightDataBranch index to avoid relying on floating-point time matching.
	 * Only the "main" stored point of each step is captured (sub-step evaluations are ignored).
	 */
	private static final class CorrectiveMomentCaptureListener extends AbstractSimulationListener {
		private final Map<Integer, FlightConditions> flightConditionsByIndex = new HashMap<>();
		private final Map<Integer, AerodynamicForces> forcesByIndex = new HashMap<>();
		private final Map<Integer, MassPair> massesByIndex = new HashMap<>();
		private final Map<Integer, Boolean> launchRodClearedByIndex = new HashMap<>();

		@Override
		public FlightConditions postFlightConditions(SimulationStatus status, FlightConditions flightConditions) {
			if (!isStoredPoint(status)) {
				return null;
			}

			int index = currentIndex(status);
			flightConditionsByIndex.putIfAbsent(index, flightConditions);
			launchRodClearedByIndex.putIfAbsent(index, status.isLaunchRodCleared());
			return null;
		}

		@Override
		public AerodynamicForces postAerodynamicCalculation(SimulationStatus status, AerodynamicForces forces) {
			if (!isStoredPoint(status)) {
				return null;
			}

			int index = currentIndex(status);
			forcesByIndex.putIfAbsent(index, forces);
			launchRodClearedByIndex.putIfAbsent(index, status.isLaunchRodCleared());
			return null;
		}

		@Override
		public RigidBody postMassCalculation(SimulationStatus status, RigidBody massData) {
			if (!isStoredPoint(status)) {
				return null;
			}

			int index = currentIndex(status);
			massesByIndex.computeIfAbsent(index, i -> new MassPair()).add(massData);
			return null;
		}

		Inputs getInputs(int index) {
			FlightConditions flightConditions = flightConditionsByIndex.get(index);
			AerodynamicForces forces = forcesByIndex.get(index);
			MassPair massPair = massesByIndex.get(index);
			RigidBody rocketMass = (massPair == null) ? null : massPair.combined();
			boolean launchRodCleared = launchRodClearedByIndex.getOrDefault(index, false);
			return new Inputs(flightConditions, forces, rocketMass, launchRodCleared);
		}

		/**
		 * Determine whether the current callback corresponds to the stored data point of the step.
		 * Sub-step evaluations (e.g. RK4 k2/k3/k4, RK6 intermediate stages) are ignored by comparing
		 * the status time against the last stored TYPE_TIME value in the branch.
		 */
		private static boolean isStoredPoint(SimulationStatus status) {
			return status.getSimulationTime() == status.getFlightDataBranch().getLast(FlightDataType.TYPE_TIME);
		}

		private static int currentIndex(SimulationStatus status) {
			return status.getFlightDataBranch().getLength() - 1;
		}

		/**
		 * We see two postMassCalculation callbacks per stored point: structure mass and motor mass.
		 * They are summed to obtain the combined rocket mass/CG used by the simulation.
		 */
		private static final class MassPair {
			private RigidBody first;
			private RigidBody second;

			void add(RigidBody mass) {
				if (first == null) {
					first = mass;
					return;
				}
				if (second == null) {
					second = mass;
				}
			}

			RigidBody combined() {
				if (first == null || second == null) {
					return null;
				}
				return first.add(second);
			}
		}
	}

	private record Inputs(FlightConditions flightConditions, AerodynamicForces forces, RigidBody rocketMass,
			boolean launchRodCleared) {
	}
}
