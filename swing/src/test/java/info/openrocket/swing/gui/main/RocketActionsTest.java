package info.openrocket.swing.gui.main;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import info.openrocket.core.plugin.PluginModule;
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
        //TODO: Test goes here...
    }
}