package info.openrocket.swing.gui.figureelements;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.theme.UITheme;

/**
 * A FigureElement that displays a snap mode instruction message.
 * Shows "Select the edge or point to snap caliper %d to" when in snap mode.
 *
 * @author OpenRocket Team
 */
public class SnapModeInfo implements FigureElement {

	private static final Translator trans = Application.getTranslator();
	private static final int MARGIN = 8;
	private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

	private int caliperNumber;
	private static Color textColor;

	static {
		initColors();
	}

	public SnapModeInfo(int caliperNumber) {
		this.caliperNumber = caliperNumber;
	}

	private static void initColors() {
		updateColors();
		UITheme.Theme.addUIThemeChangeListener(SnapModeInfo::updateColors);
	}

	public static void updateColors() {
		textColor = GUIUtil.getUITheme().getTextColor();
	}

	@Override
	public void paint(Graphics2D g2, double scale) {
		paint(g2, scale, null);
	}

	@Override
	public void paint(Graphics2D g2, double scale, Rectangle visible) {
		if (visible == null) {
			return;
		}

		// Create the message text
		GlyphVector text = FONT.createGlyphVector(g2.getFontRenderContext(),
				String.format(trans.get("CaliperManager.snapModeMessage"), caliperNumber));

		// Position at top center of visible area
		Rectangle2D textBounds = text.getVisualBounds();
		float x = (float)(visible.x + (visible.width - textBounds.getWidth()) / 2);
		float y = visible.y + MARGIN + (float)textBounds.getHeight();

		// Draw the text
		g2.setColor(textColor);
		g2.drawGlyphVector(text, x, y);
	}
}

