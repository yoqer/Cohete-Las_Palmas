package info.openrocket.core.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import info.openrocket.core.document.Simulation;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.util.BaseTestCase;
import info.openrocket.core.util.TestRockets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class AngleOfAttackTest extends BaseTestCase {

	/**
	 * Check that the initial angle of attack is zero while the rocket remains on the launch rod.
	 */
	@ParameterizedTest
	@EnumSource(SimulationStepperMethod.class)
	public void testAngleOfAttackIsZeroOnLaunchRodAtT0(SimulationStepperMethod stepperMethod)
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

		List<Double> time = branch.get(FlightDataType.TYPE_TIME);
		List<Double> aoa = branch.get(FlightDataType.TYPE_AOA);

		assertNotNull(time);
		assertNotNull(aoa);
		assertFalse(aoa.isEmpty());
		assertEquals(time.size(), aoa.size());

		// At t=0 the rocket has not cleared the launch rod, so angle of attack is zero.
		assertEquals(0.0, aoa.get(0), "Expected zero AoA at t=0 while on launch rod");
	}
}
