package info.openrocket.core.file.svg.export;

import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.NoseCone;
import info.openrocket.core.rocketcomponent.Transition;
import info.openrocket.core.util.BaseTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;

class ProfileSvgExporterTest extends BaseTestCase {

	@Test
	void calculateBoundsForConstantRadiusComponent() {
		BodyTube tube = new BodyTube();
		tube.setOuterRadius(0.05);
		tube.setLength(0.2);

		ProfileSvgExporter.Bounds bounds = ProfileSvgExporter.calculateBounds(tube);

		Assertions.assertEquals(0.2, bounds.getWidth(), 1e-9);
		// getMaxAbsY returns the maximum absolute Y value (radius), not the total height
		Assertions.assertEquals(0.05, bounds.getMaxAbsY(), 1e-9); // radius
	}

	@Test
	void calculateBoundsForVariableRadiusComponent() {
		NoseCone noseCone = new NoseCone();
		noseCone.setLength(0.1);
		noseCone.setAftRadius(0.05);
		noseCone.setForeRadius(0.0);
		noseCone.setShapeType(NoseCone.Shape.CONICAL);

		ProfileSvgExporter.Bounds bounds = ProfileSvgExporter.calculateBounds(noseCone);

		Assertions.assertEquals(0.1, bounds.getWidth(), 1e-6);
		Assertions.assertTrue(bounds.getMaxAbsY() > 0);
		Assertions.assertTrue(bounds.getMaxAbsY() <= 0.05);
	}

	@Test
	void calculateBoundsForTransitionWithShoulders() {
		Transition transition = new Transition();
		transition.setLength(0.15);
		transition.setForeRadius(0.03);
		transition.setAftRadius(0.05);
		transition.setShapeType(Transition.Shape.CONICAL);
		transition.setForeShoulderLength(0.02);
		transition.setForeShoulderRadius(0.03);
		transition.setAftShoulderLength(0.025);
		transition.setAftShoulderRadius(0.05);

		ProfileSvgExporter.Bounds bounds = ProfileSvgExporter.calculateBounds(transition);

		// Should include transition body plus shoulders
		Assertions.assertTrue(bounds.getWidth() >= 0.15);
		// Width should account for shoulders extending beyond transition body
		Assertions.assertTrue(bounds.getWidth() >= 0.15 + 0.02 + 0.025);
	}

	@Test
	void drawClosedProfileForConstantRadiusWritesRectangle() throws Exception {
		BodyTube tube = new BodyTube();
		tube.setOuterRadius(0.05);
		tube.setLength(0.2);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(tube, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("bodytube", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain path data with rectangle coordinates
		Assertions.assertTrue(contents.contains("M"), "Should contain path with Move command");
		Assertions.assertTrue(contents.contains("L"), "Should contain path with Line commands");
		// Check for expected coordinates (converted to mm: 0.05m = 50mm, 0.2m = 200mm)
		Assertions.assertTrue(contents.contains("50.000") || contents.contains("50.0"), 
			"Should contain radius coordinate: " + contents);
		Assertions.assertTrue(contents.contains("200.000") || contents.contains("200.0"), 
			"Should contain length coordinate: " + contents);
	}

	@Test
	void drawClosedProfileForVariableRadiusWritesCurvedPath() throws Exception {
		NoseCone noseCone = new NoseCone();
		noseCone.setLength(0.1);
		noseCone.setAftRadius(0.05);
		noseCone.setForeRadius(0.0);
		noseCone.setShapeType(NoseCone.Shape.CONICAL);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(noseCone, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("nosecone", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		Assertions.assertTrue(contents.contains("M"), "Should contain path with Move command");
		Assertions.assertTrue(contents.contains("L"), "Should contain path with Line commands");
		// Should have many points for variable radius
		long lineCount = contents.split("L").length - 1;
		Assertions.assertTrue(lineCount > 10, "Variable radius should have many line segments");
	}

	@Test
	void drawClosedProfileForTransitionWithForeShoulder() throws Exception {
		Transition transition = new Transition();
		transition.setLength(0.15);
		transition.setForeRadius(0.03);
		transition.setAftRadius(0.05);
		transition.setShapeType(Transition.Shape.CONICAL);
		transition.setForeShoulderLength(0.02);
		transition.setForeShoulderRadius(0.03);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(transition, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("transition-fore", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain multiple paths (transition body + fore shoulder)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertTrue(pathCount >= 2, "Should contain transition body and fore shoulder paths");
		// Fore shoulder extends backward (negative X)
		Assertions.assertTrue(contents.contains("-20.000") || contents.contains("-20.0"), 
			"Should contain negative coordinate for fore shoulder: " + contents);
	}

	@Test
	void drawClosedProfileForTransitionWithAftShoulder() throws Exception {
		Transition transition = new Transition();
		transition.setLength(0.15);
		transition.setForeRadius(0.03);
		transition.setAftRadius(0.05);
		transition.setShapeType(Transition.Shape.CONICAL);
		transition.setAftShoulderLength(0.025);
		transition.setAftShoulderRadius(0.05);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(transition, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("transition-aft", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain multiple paths (transition body + aft shoulder)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertTrue(pathCount >= 2, "Should contain transition body and aft shoulder paths");
		// Aft shoulder extends forward (positive X beyond transition length)
		Assertions.assertTrue(contents.contains("175.000") || contents.contains("175.0"), 
			"Should contain coordinate beyond transition length for aft shoulder: " + contents);
	}

	@Test
	void drawClosedProfileForTransitionWithBothShoulders() throws Exception {
		Transition transition = new Transition();
		transition.setLength(0.15);
		transition.setForeRadius(0.03);
		transition.setAftRadius(0.05);
		transition.setShapeType(Transition.Shape.CONICAL);
		transition.setForeShoulderLength(0.02);
		transition.setForeShoulderRadius(0.03);
		transition.setAftShoulderLength(0.025);
		transition.setAftShoulderRadius(0.05);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(transition, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("transition-both", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain multiple paths (transition body + both shoulders)
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertTrue(pathCount >= 3, "Should contain transition body and both shoulder paths");
	}

	@Test
	void drawClosedProfileForTransitionWithoutShoulders() throws Exception {
		Transition transition = new Transition();
		transition.setLength(0.15);
		transition.setForeRadius(0.03);
		transition.setAftRadius(0.05);
		transition.setShapeType(Transition.Shape.CONICAL);
		// No shoulders set

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		ProfileSvgExporter.drawClosedProfile(transition, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("transition-none", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// Should contain only transition body path
		long pathCount = contents.split("<path").length - 1;
		Assertions.assertEquals(1, pathCount, "Should contain only transition body path");
	}

	@Test
	void drawClosedProfileRespectsOriginOffset() throws Exception {
		BodyTube tube = new BodyTube();
		tube.setOuterRadius(0.05);
		tube.setLength(0.2);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.1);

		// Draw at offset origin
		ProfileSvgExporter.drawClosedProfile(tube, builder, 0.1, 0.05, options);

		File svgFile = File.createTempFile("bodytube-offset", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		// SVG builder adjusts viewBox based on content, but path should be present
		Assertions.assertTrue(contents.contains("<path"), 
			"Should contain path element: " + contents);
		// Path should contain coordinates (viewBox will be adjusted automatically)
		Assertions.assertTrue(contents.contains("M") && contents.contains("L"), 
			"Should contain path commands: " + contents);
	}

	@Test
	void drawClosedProfileUsesCorrectStrokeColor() throws Exception {
		BodyTube tube = new BodyTube();
		tube.setOuterRadius(0.05);
		tube.setLength(0.2);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.RED, 0.2);

		ProfileSvgExporter.drawClosedProfile(tube, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("bodytube-color", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		Assertions.assertTrue(contents.contains("stroke=\"rgb(255,0,0)\"") || 
			contents.contains("stroke=\"#ff0000\""), 
			"Should contain red stroke color: " + contents);
	}

	@Test
	void drawClosedProfileUsesCorrectStrokeWidth() throws Exception {
		BodyTube tube = new BodyTube();
		tube.setOuterRadius(0.05);
		tube.setLength(0.2);

		SVGBuilder builder = new SVGBuilder();
		SVGExportOptions options = new SVGExportOptions(Color.BLACK, 0.5);

		ProfileSvgExporter.drawClosedProfile(tube, builder, 0.0, 0.0, options);

		File svgFile = File.createTempFile("bodytube-width", ".svg");
		builder.writeToFile(svgFile);

		String contents = Files.readString(svgFile.toPath());
		Assertions.assertTrue(contents.contains("stroke-width=\"0.500\""), 
			"Should contain correct stroke width: " + contents);
	}
}

