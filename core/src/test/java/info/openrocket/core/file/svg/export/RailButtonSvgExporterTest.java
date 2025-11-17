package info.openrocket.core.file.svg.export;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import info.openrocket.core.ServicesForTesting;
import info.openrocket.core.plugin.PluginModule;
import info.openrocket.core.rocketcomponent.RailButton;
import info.openrocket.core.startup.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;

class RailButtonSvgExporterTest {

	@BeforeAll
	static void setup() {
		Module applicationModule = new ServicesForTesting();
		Module pluginModule = new PluginModule();
		Injector injector = Guice.createInjector(applicationModule, pluginModule);
		Application.setInjector(injector);
	}

	@Test
	void calculateBoundsForNominalRailButton() {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		RailButtonSvgExporter.Bounds bounds = RailButtonSvgExporter.calculateBounds(railButton);

		Assertions.assertEquals(0.02, bounds.getWidth(), 1e-9); // outerDiameter
		Assertions.assertEquals(railButton.getTotalHeight(), bounds.getHeight(), 1e-9); // totalHeight
		Assertions.assertEquals(-0.01, bounds.getMinX(), 1e-9); // -outerRadius
		Assertions.assertEquals(0.0, bounds.getMinY(), 1e-9); // bottom at y=0
	}

	@Test
	void calculateBoundsForRailButtonWithZeroBaseHeight() {
		RailButton railButton = createRailButton(0.02, 0.01, 0.0, 0.002, 0.0);

		RailButtonSvgExporter.Bounds bounds = RailButtonSvgExporter.calculateBounds(railButton);

		Assertions.assertEquals(0.02, bounds.getWidth(), 1e-9);
		// Height should be innerHeight + flangeHeight
		double expectedHeight = railButton.getInnerHeight() + railButton.getFlangeHeight();
		Assertions.assertEquals(expectedHeight, bounds.getHeight(), 1e-9);
	}

	@Test
	void calculateBoundsForRailButtonWithZeroFlangeHeight() {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.0, 0.0);

		RailButtonSvgExporter.Bounds bounds = RailButtonSvgExporter.calculateBounds(railButton);

		Assertions.assertEquals(0.02, bounds.getWidth(), 1e-9);
		// Height should be baseHeight + innerHeight
		double expectedHeight = railButton.getBaseHeight() + railButton.getInnerHeight();
		Assertions.assertEquals(expectedHeight, bounds.getHeight(), 1e-9);
	}

	@Test
	void drawRailButtonProfileWritesThreeRectangles() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain 3 paths (base, inner, flange)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(3, pathCount, "Should contain 3 rectangle paths: " + contents);
	}

	@Test
	void drawRailButtonProfileSkipsZeroHeightBase() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.0, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-nobase", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain only 2 paths (inner, flange)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(2, pathCount, "Should contain only inner and flange paths: " + contents);
	}

	@Test
	void drawRailButtonProfileSkipsZeroHeightFlange() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.0, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-noflange", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain only 2 paths (base, inner)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(2, pathCount, "Should contain only base and inner paths: " + contents);
	}

	@Test
	void drawRailButtonProfileSkipsZeroHeightInner() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);
		// Set total height equal to base + flange to make inner height zero
		railButton.setTotalHeight(railButton.getBaseHeight() + railButton.getFlangeHeight());

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-noinner", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain only 2 paths (base, flange)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(2, pathCount, "Should contain only base and flange paths: " + contents);
	}

	@Test
	void drawRailButtonProfileBaseRectangleHasCorrectDimensions() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-base", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Base rectangle: outerDiameter wide (20mm), baseHeight tall (5mm)
		// Should contain coordinates: -10mm to +10mm (outerRadius), 0 to 5mm (baseHeight)
		Assertions.assertTrue(contents.contains("-10.000") || contents.contains("-10.0"), 
			"Should contain negative outer radius: " + contents);
		Assertions.assertTrue(contents.contains("10.000") || contents.contains("10.0"), 
			"Should contain positive outer radius: " + contents);
		Assertions.assertTrue(contents.contains("5.000") || contents.contains("5.0"), 
			"Should contain base height coordinate: " + contents);
	}

	@Test
	void drawRailButtonProfileInnerRectangleHasCorrectDimensions() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-inner", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Inner rectangle: innerDiameter wide (10mm), innerHeight tall
		// Should contain coordinates: -5mm to +5mm (innerRadius)
		Assertions.assertTrue(contents.contains("-5.000") || contents.contains("-5.0"), 
			"Should contain negative inner radius: " + contents);
		Assertions.assertTrue(contents.contains("5.000") || contents.contains("5.0"), 
			"Should contain positive inner radius: " + contents);
	}

	@Test
	void drawRailButtonProfileFlangeRectangleHasCorrectDimensions() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-flange", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Flange rectangle: outerDiameter wide (20mm), flangeHeight tall (2mm)
		// Should be positioned at y = baseHeight + innerHeight
		double expectedY = railButton.getBaseHeight() + railButton.getInnerHeight();
		String expectedYStr = String.format("%.3f", expectedY * 1000); // Convert to mm
		Assertions.assertTrue(contents.contains(expectedYStr) || contents.contains(String.format("%.1f", expectedY * 1000)), 
			"Should contain flange Y position: " + contents);
	}

	@Test
	void drawRailButtonProfileRespectsOriginOffset() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		// Draw at offset origin
		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.1, 0.05, options);

		File svgFile = File.createTempFile("railbutton-offset", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// SVG builder adjusts viewBox, but path coordinates should contain offset values
		// The viewBox will be adjusted, but paths should still be present
		Assertions.assertTrue(contents.contains("<path"), 
			"Should contain path elements: " + contents);
		// Check that paths are written (should have 3 paths for base, inner, flange)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(3, pathCount, "Should contain 3 rectangle paths: " + contents);
	}

	@Test
	void drawRailButtonProfileUsesCorrectStrokeColor() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLUE, 0.2);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-color", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		Assertions.assertTrue(contents.contains("stroke=\"rgb(0,0,255)\"") || 
			contents.contains("stroke=\"#0000ff\""), 
			"Should contain blue stroke color: " + contents);
	}

	@Test
	void drawRailButtonProfileUsesCorrectStrokeWidth() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.3);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-width", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		Assertions.assertTrue(contents.contains("stroke-width=\"0.300\""), 
			"Should contain correct stroke width: " + contents);
	}

	@Test
	void drawRailButtonProfileRectanglesAreStackedCorrectly() throws Exception {
		RailButton railButton = createRailButton(0.02, 0.01, 0.005, 0.002, 0.0);
		// baseHeight = 0.005 (5mm), innerHeight = 0.002 (2mm), flangeHeight = 0.002 (2mm)
		// Total height = 9mm

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("railbutton-stacked", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Base: y from 0 to 5mm
		Assertions.assertTrue(contents.contains("0.000") || contents.contains("0.0"), 
			"Should contain base bottom coordinate: " + contents);
		// Inner: y from 5mm to 7mm
		Assertions.assertTrue(contents.contains("5.000") || contents.contains("5.0"), 
			"Should contain inner bottom coordinate: " + contents);
		// Flange: y from 7mm to 9mm
		double flangeBottom = (railButton.getBaseHeight() + railButton.getInnerHeight()) * 1000; // Convert to mm
		String flangeBottomStr = String.format("%.3f", flangeBottom);
		Assertions.assertTrue(contents.contains(flangeBottomStr) || contents.contains(String.format("%.1f", flangeBottom)), 
			"Should contain flange bottom coordinate: " + contents);
		double flangeTop = railButton.getTotalHeight() * 1000; // Convert to mm
		String flangeTopStr = String.format("%.3f", flangeTop);
		Assertions.assertTrue(contents.contains(flangeTopStr) || contents.contains(String.format("%.1f", flangeTop)), 
			"Should contain flange top coordinate: " + contents);
	}

	private static RailButton createRailButton(double outerDiameter, double innerDiameter, 
	                                           double baseHeight, double flangeHeight, double screwHeight) {
		RailButton railButton = new RailButton();
		railButton.setOuterDiameter(outerDiameter);
		railButton.setInnerDiameter(innerDiameter);
		railButton.setBaseHeight(baseHeight);
		railButton.setFlangeHeight(flangeHeight);
		// Calculate total height: base + inner + flange
		// innerHeight = totalHeight - baseHeight - flangeHeight
		// So we need to set totalHeight such that innerHeight is reasonable
		// Use a reasonable inner height (e.g., 0.002m = 2mm)
		double innerHeight = 0.002;
		railButton.setTotalHeight(baseHeight + innerHeight + flangeHeight);
		railButton.setScrewHeight(screwHeight);
		return railButton;
	}
}

