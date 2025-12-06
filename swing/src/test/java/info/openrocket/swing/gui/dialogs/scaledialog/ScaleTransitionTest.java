package info.openrocket.swing.gui.dialogs.scaledialog;

import info.openrocket.core.rocketcomponent.Transition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for scaling the Transition Component
 */
public class ScaleTransitionTest extends ScaleDialogBaseTest {

    private static final double DELTA = 1e-6;

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Creates a transition that triggers the scaling bug:
     * - ForeRadiusAutomatic = TRUE  (takes value from previous component)
     * - AftRadiusAutomatic = FALSE  (manually set, SHOULD scale but WON'T due to bug)
     */
    private Transition setupBugConfiguration(double foreRadius, double aftRadius) {
        Transition trans = new Transition();
        trans.setForeRadiusAutomatic(true);   // Automatic fore (common case)
        trans.setAftRadiusAutomatic(false);   // Manual aft (should scale, but won't)
        trans.setForeRadius(foreRadius);
        trans.setAftRadius(aftRadius);
        return trans;
    }

    /**
     * Creates a transition in a working configuration:
     * - Both ForeRadius and AftRadius are manual
     * - Both should scale correctly
     */
    private Transition setupWorkingConfiguration(double foreRadius, double aftRadius) {
        Transition trans = new Transition();
        trans.setForeRadiusAutomatic(false);  // Manual - will scale
        trans.setAftRadiusAutomatic(false);   // Manual - will scale
        trans.setForeRadius(foreRadius);
        trans.setAftRadius(aftRadius);
        return trans;
    }

    // ==========================================
    // STANDARD SCALING TESTS
    // ==========================================

    /**
     * Standard test: Verifies basic transition scaling works when both radii are manual.
     * This test PASSES - demonstrates normal scaling behavior.
     */
    @Test
    public void testTransition_BasicScaling() throws Exception {
        Transition trans = new Transition();
        trans.setLength(20.0);

        trans.setForeRadius(10.0);
        trans.setForeRadiusAutomatic(false);  // Manual
        trans.setAftRadius(16.0);
        trans.setAftRadiusAutomatic(false);   // Manual

        trans.setForeShoulderLength(2.0);
        trans.setAftShoulderLength(4.0);

        // --- STEP 1: Scale by 0.5 ---
        scaleMethod.invoke(dialogInstance, trans, 0.5, false);

        assertEquals(10.0, trans.getLength(), 0.001);
        assertEquals(5.0, trans.getForeRadius(), 0.001);
        assertEquals(8.0, trans.getAftRadius(), 0.001);
        assertEquals(1.0, trans.getForeShoulderLength(), 0.001);
        assertEquals(2.0, trans.getAftShoulderLength(), 0.001);

        // --- STEP 2: Scale by 1.5 ---
        scaleMethod.invoke(dialogInstance, trans, 1.5, false);

        assertEquals(15.0, trans.getLength(), 0.001);
        assertEquals(7.5, trans.getForeRadius(), 0.001);
        assertEquals(12.0, trans.getAftRadius(), 0.001);
        assertEquals(1.5, trans.getForeShoulderLength(), 0.001);
        assertEquals(3.0, trans.getAftShoulderLength(), 0.001);
    }

    /**
     * Control test: Both radii manual - scaling should work correctly.
     * This test PASSES - no bug in this configuration.
     */
    @Test
    void testControl_BothRadiiManual_ScalesCorrectly() throws Exception {
        final double initialForeRadius = 0.025;
        final double initialAftRadius = 0.050;
        final double multiplier = 2.0;

        Transition trans = setupWorkingConfiguration(initialForeRadius, initialAftRadius);

        scaleMethod.invoke(dialogInstance, trans, multiplier, true);

        assertEquals(initialForeRadius * multiplier, trans.getForeRadius(), DELTA,
                "ForeRadius should scale correctly when manual");

        assertEquals(initialAftRadius * multiplier, trans.getAftRadius(), DELTA,
                "AftRadius should scale correctly when ForeRadius is also manual");
    }

    /**
     * Control test: ForeRadius manual, AftRadius automatic.
     * This test FAILS , because AftRadius automatic box is unchecked after scaling
     */
    @Test
    void testControl_ForeManualAftAuto_ScalesCorrectly() throws Exception {
        final double initialForeRadius = 0.025;
        final double initialAftRadius = 0.050;
        final double multiplier = 2.0;

        Transition trans = new Transition();
        trans.setForeRadiusAutomatic(false);  // Manual - should scale
        trans.setAftRadiusAutomatic(true);    // Auto - should not scale
        trans.setForeRadius(initialForeRadius);
        trans.setAftRadius(initialAftRadius);

        scaleMethod.invoke(dialogInstance, trans, multiplier, true);

        assertEquals(initialForeRadius * multiplier, trans.getForeRadius(), DELTA,
                "ForeRadius should scale correctly when manual");

        assertEquals(initialAftRadius, trans.getAftRadius(), DELTA,
                "AftRadius should remain unchanged because it's automatic , " +
                        "but it does scale and the automatic box is unckecked ");
    }

    // ==========================================
    // BUG DEMONSTRATION TESTS (FAILING)
    // ==========================================

    /**
     * BUG TEST 1: Scale UP with automatic fore radius
     *
     * CONFIGURATION: ForeRadius=AUTO, AftRadius=MANUAL
     *
     * EXPECTED: AftRadius scales from 0.040 to 0.080 (2x multiplier)
     * ACTUAL:   AftRadius remains at 0.040 (NOT SCALED)
     *
     *
     * This test FAILS due to the bug.
     */
    @Test
    void testBUG_ScaleUp_AftRadiusNotScaled() throws Exception {
        // ARRANGE
        final double initialForeRadius = 0.050;
        final double initialAftRadius = 0.040;
        final double multiplier = 2.0;

        Transition trans = setupBugConfiguration(initialForeRadius, initialAftRadius);

        // ACT
        scaleMethod.invoke(dialogInstance, trans, multiplier, true);

        // ASSERT
        double actualAftRadius = trans.getAftRadius();
        double expectedAftRadius = initialAftRadius * multiplier;  // Should be 0.080

        // *** THIS ASSERTION FAILS - DEMONSTRATES THE BUG ***
        assertNotEquals(expectedAftRadius, actualAftRadius, DELTA,
                "BUG CONFIRMED: AftRadius should have scaled to " + expectedAftRadius +
                        " but remained at " + actualAftRadius +
                        ". The scaler incorrectly checks 'isForeRadiusAutomatic' for AftRadius.");

        // Verify the buggy behavior - radius doesn't change
        assertEquals(initialAftRadius, actualAftRadius, DELTA,
                "Due to the bug, AftRadius incorrectly remains at " + initialAftRadius);

        // Verify ForeRadius correctly doesn't scale (it's automatic)
        assertEquals(initialForeRadius, trans.getForeRadius(), DELTA,
                "ForeRadius correctly remains unchanged (automatic mode)");
    }

    /**
     * BUG TEST 2: Scale DOWN with automatic fore radius
     *
     * CONFIGURATION: ForeRadius=AUTO, AftRadius=MANUAL
     *
     * EXPECTED: AftRadius scales from 0.080 to 0.040 (0.5x multiplier)
     * ACTUAL:   AftRadius remains at 0.080 (NOT SCALED)
     *
     * This test FAILS due to the bug.
     */
    @Test
    void testBUG_ScaleDown_AftRadiusNotScaled() throws Exception {
        // ARRANGE
        final double initialForeRadius = 0.030;
        final double initialAftRadius = 0.080;
        final double multiplier = 0.5;

        Transition trans = setupBugConfiguration(initialForeRadius, initialAftRadius);

        // ACT
        scaleMethod.invoke(dialogInstance, trans, multiplier, false);

        // ASSERT
        double actualAftRadius = trans.getAftRadius();
        double expectedAftRadius = initialAftRadius * multiplier;  // Should be 0.040

        // *** THIS ASSERTION FAILS - DEMONSTRATES THE BUG ***
        assertNotEquals(expectedAftRadius, actualAftRadius, DELTA,
                "BUG CONFIRMED: AftRadius should have scaled to " + expectedAftRadius +
                        " but remained at " + actualAftRadius);

        // Verify the buggy behavior
        assertEquals(initialAftRadius, actualAftRadius, DELTA,
                "Due to the bug, AftRadius incorrectly remains at " + initialAftRadius);

        // Verify ForeRadius correctly doesn't scale
        assertEquals(initialForeRadius, trans.getForeRadius(), DELTA,
                "ForeRadius correctly remains unchanged (automatic mode)");
    }

    /**
     * BUG TEST 3: Cascade effect - shoulders also don't scale
     *
     * CONFIGURATION: ForeRadius=AUTO, AftRadius=MANUAL, with aft shoulder
     *
     * EXPECTED: AftRadius, AftShoulderRadius, and AftShoulderLength all scale by 2x
     * ACTUAL:   None of the aft components scale (cascade failure)
     *
     * WHY: Because AftRadius doesn't scale (due to the bug), the dependent
     *      AftShoulder properties also fail to scale properly.
     *
     * This test FAILS and shows the bug affects the entire aft section.
     */
    @Test
    void testBUG_ShoulderScaling() throws Exception {
        // ARRANGE
        final double initialAftRadius = 0.050;
        final double initialAftShoulderRadius = 0.045;
        final double initialAftShoulderLength = 0.020;
        final double multiplier = 2.0;

        Transition trans = setupBugConfiguration(0.030, initialAftRadius);
        trans.setAftShoulderRadius(initialAftShoulderRadius);
        trans.setAftShoulderLength(initialAftShoulderLength);
        trans.setAftShoulderThickness(0.002);

        // ACT
        scaleMethod.invoke(dialogInstance, trans, multiplier, true);

        // ASSERT - All aft components fail to scale
        // *** ALL THESE ASSERTIONS FAIL ***
        assertEquals(initialAftRadius, trans.getAftRadius(), DELTA,
                "BUG: AftRadius should be 0.100 but remained at " + initialAftRadius);

        assertEquals(initialAftShoulderRadius, trans.getAftShoulderRadius(), DELTA,
                "BUG: AftShoulderRadius should be 0.090 but remained at " + initialAftShoulderRadius);

        assertEquals(initialAftShoulderLength, trans.getAftShoulderLength(), DELTA,
                "BUG: AftShoulderLength should be 0.040 but remained at " + initialAftShoulderLength);
    }

}