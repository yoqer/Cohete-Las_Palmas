package info.openrocket.swing.gui.main;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import info.openrocket.core.plugin.PluginModule;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.NoseCone;
import info.openrocket.core.rocketcomponent.Parachute;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.TrapezoidFinSet;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.ServicesForTesting;

/**
 * RocketActions Tester
 * 
 */
public class RocketActionsTest {

	private static Injector injector;

    @BeforeAll
	public static void setup() {
		Module applicationModule = new ServicesForTesting();
		Module pluginModule = new PluginModule();

		injector = Guice.createInjector(applicationModule, pluginModule);
		Application.setInjector(injector);
	}

    /**
     * 
     * Method: copyComponentsMaintainParent
     * 
     */
    @Test
    public void testCopyComponentsMaintainParent() {

        // Create a list of RocketComponent objects
        List<RocketComponent> components = new ArrayList<>();
        BodyTube bodyTube = new BodyTube(0.5, 0.05);
        components.add(bodyTube);
        NoseCone noseCone = new NoseCone(NoseCone.Shape.CONICAL, 6 * NoseCone.DEFAULT_RADIUS, NoseCone.DEFAULT_RADIUS);
        components.add(noseCone);
        TrapezoidFinSet trapezoidFinSet = new TrapezoidFinSet(3, 0.05, 0.05, 0.025, 0.03);
        components.add(trapezoidFinSet);
        Parachute parachute = new Parachute();
        components.add(parachute);

        // Copy the components
        List<RocketComponent> copiedComponents = RocketActions.copyComponentsMaintainParent(components);

        // TODO: Add assertions to verify that the copied components are correct
    }
}