package info.openrocket.swing.gui.util;

import com.formdev.flatlaf.FlatLaf.DisabledIconProvider;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import info.openrocket.core.arch.SystemInfo;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import com.jthemedetecor.OsThemeDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class Icons {
	private static final Logger log = LoggerFactory.getLogger(Icons.class);
	private static final Translator trans = Application.getTranslator();
	private static final SwingPreferences prefs = (SwingPreferences) Application.getPreferences();
	// SVGs can opt into theme coloring by using this placeholder RGB.
	private static final int SVG_THEME_COLOR_RGB = 0xFF00FF;
	private static final String SVG_DEFAULT_COLOR_KEY = "OR.icons.default";
	// Multiplier to scale icons relative to font height (icons are typically slightly larger than text)
	private static final double ICON_FONT_SIZE_MULTIPLIER = 1.25;
	
	static {
		log.debug("Starting to load icons");
	}
	
	/**
	 * Icons used for showing the status of a simulation (up to date, out of date, etc).
	 */
	public static final Map<Simulation.Status, Icon> SIMULATION_STATUS_ICON_MAP;
	static {
		final String SIM_UPTODATE = "pix/icons/tick.png";
		final String SIM_CANTRUN = "pix/icons/sim_cantrun.png";
		final String SIM_OUTDATED = "pix/icons/refresh.png";
		final String SIM_ABORTED = "pix/eventicons/event-exception.png";

		HashMap<Simulation.Status, Icon> map = new HashMap<>();
		map.put(Simulation.Status.NOT_SIMULATED, loadImageIcon(SIM_OUTDATED, "Not simulated"));
		map.put(Simulation.Status.CANT_RUN, loadImageIcon(SIM_CANTRUN, "Can't run, no motors assigned."));
		map.put(Simulation.Status.UPTODATE, loadImageIcon(SIM_UPTODATE, "Up to date"));
		map.put(Simulation.Status.LOADED, loadImageIcon(SIM_UPTODATE, "Loaded from File"));
		map.put(Simulation.Status.OUTDATED, loadImageIcon(SIM_OUTDATED, "Out-of-date"));
		map.put(Simulation.Status.EXTERNAL, loadImageIcon(SIM_UPTODATE, "Imported data"));
		map.put(Simulation.Status.ABORTED, loadImageIcon(SIM_ABORTED, "Simulation run aborted"));
		SIMULATION_STATUS_ICON_MAP = Collections.unmodifiableMap(map);
	}
	
	public static final Icon SIMULATION_LISTENER_OK;
	public static final Icon SIMULATION_LISTENER_ERROR;
	static {
		SIMULATION_LISTENER_OK = SIMULATION_STATUS_ICON_MAP.get(Simulation.Status.UPTODATE);
		SIMULATION_LISTENER_ERROR = SIMULATION_STATUS_ICON_MAP.get(Simulation.Status.OUTDATED);
	}

	// Note: most of these icons are from famfamicons Silk set
	
	public static final Icon FILE_NEW = loadIcon(
			"pix/icons/lucide/file-plus-corner.svg",
			"pix/icons/document-new.png",
			"New document");
	public static final Icon FILE_OPEN = loadIcon(
			"pix/icons/lucide/file.svg",
			"pix/icons/document-open.png",
			"Open document");
	public static final Icon FILE_OPEN_RECENT = loadSvgIcon("pix/icons/lucide/file-clock.svg", "Open recent document");
	public static final Icon FILE_OPEN_EXAMPLE = loadImageIcon("pix/icons/document-open-example.png", "Open example document");
	public static final Icon FILE_SAVE = loadIcon(
			"pix/icons/lucide/save.svg",
			"pix/icons/document-save.png",
			"Save document");
	public static final Icon FILE_SAVE_AS = loadImageIcon("pix/icons/document-save-as.png", "Save document as");
	public static final Icon SAVE_DECAL = loadImageIcon("pix/icons/Painting-Transparent-PNG_16.png", "Save decal image");
	public static final Icon FILE_PRINT = loadImageIcon("pix/icons/print-design.specs.png", "Print specifications");
	public static final Icon FILE_IMPORT = loadIcon(
			"pix/icons/lucide/import.svg",
			"pix/icons/import.png",
			"Import");
	public static final Icon FILE_EXPORT = loadIcon(
			"pix/icons/lucide/export.svg",
			"pix/icons/export.png",
			"Export");
	public static final Icon SIM_TABLE_EXPORT = loadImageIcon("pix/icons/sim_table_export.png", "Export simulation table");
	public static final Icon EXPORT_3D = loadImageIcon("pix/icons/model_export3d.png", "Export 3D");
	public static final Icon EXPORT_SVG = loadImageIcon("pix/icons/svg-logo.png", "Export SVG");
	public static final Icon FILE_CLOSE = loadImageIcon("pix/icons/document-close.png", "Close document");
	public static final Icon FILE_QUIT = loadImageIcon("pix/icons/application-exit.png", "Quit OpenRocket");
	public static final Icon EDIT_UNDO = loadIcon(
			"pix/icons/lucide/undo.svg",
			"pix/icons/edit-undo.png",
			trans.get("Icons.Undo"));
	public static final Icon EDIT_REDO = loadIcon(
			"pix/icons/lucide/redo.svg",
			"pix/icons/edit-redo.png",
			trans.get("Icons.Redo"));
	public static final Icon EDIT_EDIT = loadIcon(
			"pix/icons/lucide/square-pen.svg",
			"pix/icons/edit-edit.png",
			"Edit");
	public static final Icon EDIT_RENAME = loadImageIcon("pix/icons/edit-rename.png", "Rename");
	public static final Icon EDIT_CUT = loadIcon(
			"pix/icons/lucide/scissors.svg",
			"pix/icons/edit-cut.png",
			"Cut");
	public static final Icon EDIT_COPY = loadIcon(
			"pix/icons/lucide/copy.svg",
			"pix/icons/edit-copy.png",
			"Copy");
	public static final Icon EDIT_PASTE = loadIcon(
			"pix/icons/lucide/clipboard-paste.svg",
			"pix/icons/edit-paste.png",
			"Paste");
	public static final Icon EDIT_DUPLICATE = loadIcon(
			"pix/icons/lucide/copy-plus.svg",
			"pix/icons/edit-duplicate.png",
			"Duplicate");
	public static final Icon EDIT_DELETE = loadIcon(
			"pix/icons/lucide/trash-2.svg",
			"pix/icons/edit-delete.png",
			"Delete");
	public static final Icon EDIT_SCALE = loadIcon(
			"pix/icons/lucide/scaling.svg",
			"pix/icons/edit-scale.png",
			"Scale");

	public static final Icon SIM_RUN = loadImageIcon("pix/icons/sim-run.png", "Run");
	public static final Icon SIM_PLOT = loadImageIcon("pix/icons/sim-plot.png", "Plot");
	
	public static final Icon HELP_ABOUT = loadImageIcon("pix/icons/help-about.png", "About");
	public static final Icon HELP_CHECK_FOR_UPDATES = loadImageIcon("pix/icons/help-check-for-updates.png", "Check For Updates");
	public static final Icon HELP_LICENSE = loadImageIcon("pix/icons/help-license.png", "License");
	public static final Icon HELP_BUG_REPORT = loadImageIcon("pix/icons/help-bug.png", "Bug report");
	public static final Icon HELP_DEBUG_LOG = loadImageIcon("pix/icons/help-log.png", "Debug log");
	public static final Icon HELP_TOURS = loadImageIcon("pix/icons/help-tours.png", "Guided tours");
	public static final Icon DOCUMENTATION = loadImageIcon("pix/icons/documentation.png", "Documentation");

	public static final Icon ZOOM_IN = loadImageIcon("pix/icons/zoom-in.png", "Zoom in");
	public static final Icon ZOOM_OUT = loadImageIcon("pix/icons/zoom-out.png", "Zoom out");
	public static final Icon ZOOM_RESET = loadImageIcon("pix/icons/zoom-reset.png", "Reset Zoom & Pan");
	
	public static final Icon PREFERENCES = loadImageIcon("pix/icons/preferences.png", "Preferences");
	
	public static final Icon CONFIGURE = loadImageIcon("pix/icons/configure.png", "Configure");
	public static final Icon HELP = loadImageIcon("pix/icons/help-about.png", "Help");
	public static final Icon UP = loadIcon(
			"pix/icons/lucide/arrow-big-up-dash.svg",
			"pix/icons/up.png",
			"Up");
	public static final Icon DOWN = loadIcon(
			"pix/icons/lucide/arrow-big-down-dash.svg",
			"pix/icons/down.png",
			"Down");
	public static final Icon REFRESH = loadImageIcon("pix/icons/refresh.png", "Refresh");
	
	public static final Icon NOT_FAVORITE = loadImageIcon("pix/icons/star_silver.png", "Not favorite");
	public static final Icon FAVORITE = loadImageIcon("pix/icons/star_gold.png", "Favorite");

	public static final Icon WARNING_LOW = loadImageIcon("pix/icons/warning_low.png", "Informational");
	public static final Icon WARNING_NORMAL = loadImageIcon("pix/icons/warning_normal.png", "Warning");
	public static final Icon WARNING_HIGH = loadImageIcon("pix/icons/warning_high.png", "Critical");

	public static final Icon MASS_OVERRIDE_LIGHT = loadImageIcon("pix/icons/mass-override_light.png", "Mass Override");
	public static final Icon MASS_OVERRIDE_DARK = loadImageIcon("pix/icons/mass-override_dark.png", "Mass Override");
	public static final Icon MASS_OVERRIDE_SUBCOMPONENT_LIGHT = loadImageIcon("pix/icons/mass-override-subcomponent_light.png", "Mass Override Subcomponent");
	public static final Icon MASS_OVERRIDE_SUBCOMPONENT_DARK = loadImageIcon("pix/icons/mass-override-subcomponent_dark.png", "Mass Override Subcomponent");
	public static final Icon CG_OVERRIDE_LIGHT = loadImageIcon("pix/icons/cg-override_light.png", "CG Override");
	public static final Icon CG_OVERRIDE_DARK = loadImageIcon("pix/icons/cg-override_dark.png", "CG Override");
	public static final Icon CG_OVERRIDE_SUBCOMPONENT_LIGHT = loadImageIcon("pix/icons/cg-override-subcomponent_light.png", "CG Override Subcomponent");
	public static final Icon CG_OVERRIDE_SUBCOMPONENT_DARK = loadImageIcon("pix/icons/cg-override-subcomponent_dark.png", "CG Override Subcomponent");
	public static final Icon CD_OVERRIDE_LIGHT = loadImageIcon("pix/icons/cd-override_light.png", "CD Override");
	public static final Icon CD_OVERRIDE_DARK = loadImageIcon("pix/icons/cd-override_dark.png", "CD Override");
	public static final Icon CD_OVERRIDE_SUBCOMPONENT_LIGHT = loadImageIcon("pix/icons/cd-override-subcomponent_light.png", "CD Override Subcomponent");
	public static final Icon CD_OVERRIDE_SUBCOMPONENT_DARK = loadImageIcon("pix/icons/cd-override-subcomponent_dark.png", "CD Override Subcomponent");

	public static final Icon COMPONENT_HIDDEN = loadImageIcon("pix/icons/component-hidden.png", "Component Hidden");
	public static final Icon COMPONENT_HIDDEN_DARK = loadImageIcon("pix/icons/component-hidden_dark.png", "Component Hidden");
	public static final Icon COMPONENT_HIDDEN_LIGHT = loadImageIcon("pix/icons/component-hidden_light.png", "Component Hidden");
	public static final Icon COMPONENT_SHOWING_DARK = loadImageIcon("pix/icons/component-showing_dark.png", "Component Showing");
	public static final Icon COMPONENT_SHOWING_LIGHT = loadImageIcon("pix/icons/component-showing_light.png", "Component Showing");

	public static final Icon COMPONENT_DISABLED_DARK = loadImageIcon("pix/icons/component-disabled_dark.png", "Component Disabled");
	public static final Icon COMPONENT_DISABLED_LIGHT = loadImageIcon("pix/icons/component-disabled_light.png", "Component Disabled");

	public static final Icon LOCKED = loadImageIcon("pix/icons/locked.png", "Locked");
	public static final Icon UNLOCKED = loadImageIcon("pix/icons/unlocked.png", "Unlocked");

	// MANUFACTURERS ICONS
	public static final Icon RASAERO = loadImageIcon("pix/icons/RASAero_16.png", "RASAero Icon");
	public static final Icon ROCKSIM = loadImageIcon("pix/icons/Rocksim_16.png", "Rocksim Icon");


	static {
		log.debug("Icons loaded");
	}
	
	/**
	 * Load an ImageIcon from the specified file.  The file is obtained as a system
	 * resource from the normal classpath.  If the file cannot be loaded a bug dialog
	 * is opened and <code>null</code> is returned.
	 * 
	 * @param file	the file to load.
	 * @param name	the description of the icon.
	 * @return		the ImageIcon, or null if the ImageIcon could not be loaded (after the user closes the dialog)
	 */
	public static ImageIcon loadImageIcon(String file, String name) {
		if (System.getProperty("openrocket.unittest") != null) {
			return new ImageIcon();
		}
		
		URL url = ClassLoader.getSystemResource(file);
		if (url == null) {
			Application.getExceptionHandler().handleErrorCondition("Image file " + file + " not found, ignoring.");
			return null;
		}
		ImageIcon icon = new ImageIcon(url, name);
		return (ImageIcon) getScaledIcon(icon, prefs.getUIScale());
	}

	/**
	 * Load an SVG icon without explicit color mapping.
	 *
	 * @param file SVG resource path
	 * @param name description of the icon
	 * @return the SVG icon or null if not found
	 */
	public static Icon loadSvgIcon(String file, String name) {
		return loadSvgIcon(file, name, Collections.emptyMap());
	}

	/**
	 * Loads an SVG icon from the specified file with a single color theming key.
	 * @param file the SVG file path
	 * @param name the description of the icon
	 * @param colorKey the UIManager color key for theming (can be null)
	 * @return the loaded Icon, or null if the SVG file could not be found
	 */
	public static Icon loadSvgIcon(String file, String name, String colorKey) {
		if (colorKey == null) {
			return loadSvgIcon(file, name);
		}
		return loadSvgIcon(file, name, Collections.singletonMap(SVG_THEME_COLOR_RGB, colorKey));
	}

	/**
	 * Loads an icon, preferring SVG format if available, otherwise falling back to a raster image.
	 * @param svgFile the SVG file path
	 * @param rasterFile the raster image file path
	 * @param name the description of the icon
	 * @param colorKey the UIManager color key for theming (can be null)
	 * @return the loaded Icon, or null if neither file could be found
	 */
	public static Icon loadIcon(String svgFile, String rasterFile, String name, String colorKey) {
		if (hasResource(svgFile)) {
			return loadSvgIcon(svgFile, name, colorKey);
		}
		return loadImageIcon(rasterFile, name);
	}

	public static Icon loadIcon(String svgFile, String rasterFile, String name) {
		return loadIcon(svgFile, rasterFile, name, SVG_DEFAULT_COLOR_KEY);
	}

	/**
	 * Loads an SVG icon from the specified file with optional color theming.
	 * @param file the SVG file path
	 * @param name the description of the icon
	 * @param colorKeys map of RGB integer values to UIManager color keys for theming
	 * @return the loaded Icon, or null if the SVG file could not be found
	 */
	private static Icon loadSvgIcon(String file, String name, Map<Integer, String> colorKeys) {
		if (System.getProperty("openrocket.unittest") != null) {
			return new ImageIcon();
		}

		if (!hasResource(file)) {
			Application.getExceptionHandler().handleErrorCondition("Image file " + file + " not found, ignoring.");
			return null;
		}

		// Get the target height based on font size
		int targetHeight = (int) Math.round(prefs.getUIFontSize() * ICON_FONT_SIZE_MULTIPLIER);
		
		// First, create a temporary icon to get the original aspect ratio
		FlatSVGIcon tempIcon = new FlatSVGIcon(file, Icons.class.getClassLoader());
		int originalWidth = tempIcon.getIconWidth();
		int originalHeight = tempIcon.getIconHeight();
		
		// If dimensions are invalid, return a basic icon
		if (originalWidth <= 0 || originalHeight <= 0) {
			FlatSVGIcon icon = new FlatSVGIcon(file, Icons.class.getClassLoader());
			icon.setDescription(name);
			FlatSVGIcon.ColorFilter colorFilter = createSvgColorFilter(colorKeys);
			if (colorFilter != null) {
				icon.setColorFilter(colorFilter);
			}
			return icon;
		}
		
		// Calculate target width maintaining aspect ratio
		double scale = (double) targetHeight / originalHeight;
		int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
		
		// Create the icon with the target dimensions directly
		FlatSVGIcon icon = new FlatSVGIcon(file, targetWidth, targetHeight);
		icon.setDescription(name);

		FlatSVGIcon.ColorFilter colorFilter = createSvgColorFilter(colorKeys);
		if (colorFilter != null) {
			icon.setColorFilter(colorFilter);
		}
		
		return icon;
	}

	/**
	 * Checks if a resource file exists in the classpath.
	 * @param file the resource file path
	 * @return true if the resource exists, false otherwise
	 */
	private static boolean hasResource(String file) {
		if (file == null) {
			return false;
		}
		return Icons.class.getClassLoader().getResource(file) != null;
	}

	/**
	 * Creates a color filter for SVG icons that maps specific RGB colors to UIManager color keys.
	 * @param colorKeys map of RGB integer values to UIManager color keys
	 * @return a FlatSVGIcon.ColorFilter that applies the color mapping, or null if no mapping is provided
	 */
	private static FlatSVGIcon.ColorFilter createSvgColorFilter(Map<Integer, String> colorKeys) {
		if (colorKeys == null || colorKeys.isEmpty()) {
			return null;
		}

		return new FlatSVGIcon.ColorFilter((component, color) -> {
			String key = colorKeys.get(color.getRGB() & 0x00ffffff);
			if (key == null) {
				return color;
			}

			Color themed = UIManager.getColor(key);
			if (themed == null) {
				themed = UIManager.getColor(SVG_DEFAULT_COLOR_KEY);
			}
			if (themed == null) {
				themed = UIManager.getColor("Label.foreground");
			}
			if (themed == null) {
				return color;
			}
			return preserveAlpha(themed, color);
		});
	}

	/**
	 * Preserves the alpha channel of the source color when applying the target color.
	 * @param target the target color
	 * @param source the source color
	 * @return a new Color with the RGB of target and the alpha of source
	 */
	private static Color preserveAlpha(Color target, Color source) {
		if (target.getAlpha() == source.getAlpha()) {
			return target;
		}
		return new Color((target.getRGB() & 0x00ffffff) | (source.getRGB() & 0xff000000), true);
	}


	/**
	 * Apply macOS menu icon theming to all menu items in the menu bar.
	 *
	 * @param menuBar the menu bar to update
	 */
	public static void applyMenuBarIconTheme(JMenuBar menuBar) {
		if (menuBar == null || SystemInfo.getPlatform() != SystemInfo.Platform.MAC_OS) {
			return;
		}

		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenu menu = menuBar.getMenu(i);
			if (menu == null) {
				continue;
			}
			applyMenuItemIcon(menu);
			applyMenuIconTheme(menu);
		}
	}

	/**
	 * Apply menu icon theming to menu items recursively.
	 *
	 * @param menu the menu to walk
	 */
	private static void applyMenuIconTheme(JMenu menu) {
		for (int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem item = menu.getItem(i);
			if (item == null) {
				continue;
			}
			applyMenuItemIcon(item);
			if (item instanceof JMenu) {
				applyMenuIconTheme((JMenu) item);
			}
		}
	}

	/**
	 * Replace the icon on a menu item with a themed variant when applicable.
	 *
	 * @param item the menu item to update
	 */
	private static void applyMenuItemIcon(JMenuItem item) {
		Icon icon = item.getIcon();
		if (icon != null) {
			Icon menuIcon = getMenuIcon(icon);
			if (menuIcon != icon) {
				item.setIcon(menuIcon);
			}
		}
	}

	/**
	 * Wrap an icon for use in macOS menus so it follows system light/dark appearance.
	 *
	 * @param icon the source icon
	 * @return the themed icon for macOS menus, or the original icon
	 */
	public static Icon getMenuIcon(Icon icon) {
		if (icon == null) {
			return null;
		}
		if (SystemInfo.getPlatform() != SystemInfo.Platform.MAC_OS) {
			return icon;
		}
		if (icon instanceof MacMenuIcon) {
			return icon;
		}
		if (!(icon instanceof FlatSVGIcon)) {
			return icon;
		}
		return new MacMenuIcon(icon, false);
	}

	/**
	 * Scales an ImageIcon to the specified scale.
	 * @param icon icon to scale
	 * @param scale the scale to scale to (1 = no scale, < 1 = smaller, > 1 = bigger)
	 * @return scaled down icon. If <icon> is not an ImageIcon, the original icon is returned.
	 */
	public static Icon getScaledIcon(Icon icon, final double scale) {
		if (!(icon instanceof ImageIcon) || scale == 1) {
			return icon;
		}

		Image image = ((ImageIcon) icon).getImage();
		if (image == null) {
			return icon;
		}
		int sourceWidth = image.getWidth(null);
		int sourceHeight = image.getHeight(null);

		if (sourceWidth <= 0 || sourceHeight <= 0) {
			return icon;
		}

		int width = Math.max(1, (int) Math.round(sourceWidth * scale));
		int height = Math.max(1, (int) Math.round(sourceHeight * scale));

		// Create a new scaled image
		Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		// Create and return a new ImageIcon with the scaled image
		return new ImageIcon(scaledImage);
	}

	public static Icon createDisabledIcon(Icon icon) {
		if (icon instanceof DisabledIconProvider) {
			return ((DisabledIconProvider) icon).getDisabledIcon();
		}
		if (!(icon instanceof ImageIcon)) {
			return icon;
		}
		Image image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();

		return new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon) icon).getImage()));
	}


	/**
	 * An icon wrapper that tints the icon to match macOS menu text colors.
	 */
	private static final class MacMenuIcon implements Icon, DisabledIconProvider {
		private final Icon delegate;
		private final boolean forcedDisabled;
		private Color cachedColor;
		private int cachedWidth;
		private int cachedHeight;
		private Image cachedImage;

		private MacMenuIcon(Icon delegate, boolean forcedDisabled) {
			this.delegate = delegate;
			this.forcedDisabled = forcedDisabled;
		}

		@Override
		public int getIconWidth() {
			return delegate.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return delegate.getIconHeight();
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			int width = getIconWidth();
			int height = getIconHeight();
			if (width <= 0 || height <= 0) {
				delegate.paintIcon(c, g, x, y);
				return;
			}

			Color color = resolveMenuColor(c);
			if (color == null) {
				delegate.paintIcon(c, g, x, y);
				return;
			}

			Image image = getOrCreateImage(c, color, width, height);
			if (image != null) {
				g.drawImage(image, x, y, null);
			} else {
				delegate.paintIcon(c, g, x, y);
			}
		}

		@Override
		public Icon getDisabledIcon() {
			return new MacMenuIcon(delegate, true);
		}

		/**
		 * Return a cached tinted image for the given size and color.
		 */
		private Image getOrCreateImage(Component c, Color color, int width, int height) {
			if (cachedImage == null || cachedWidth != width || cachedHeight != height || !color.equals(cachedColor)) {
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = image.createGraphics();
				try {
					delegate.paintIcon(c, g2, 0, 0);
					g2.setComposite(AlphaComposite.SrcIn);
					g2.setColor(color);
					g2.fillRect(0, 0, width, height);
				} finally {
					g2.dispose();
				}
				cachedImage = image;
				cachedWidth = width;
				cachedHeight = height;
				cachedColor = color;
			}
			return cachedImage;
		}

		/**
		 * Resolve the menu icon color, preferring macOS menu text colors.
		 */
		private Color resolveMenuColor(Component c) {
			boolean disabled = forcedDisabled || (c != null && !c.isEnabled());
			Color color = getMacOsMenuColor(disabled);
			if (color != null) {
				return color;
			}

			color = (c != null) ? c.getForeground() : null;
			if (disabled) {
				Color disabledColor = UIManager.getColor("MenuItem.disabledForeground");
				if (disabledColor != null) {
					color = disabledColor;
				}
			}
			if (color == null) {
				color = UIManager.getColor(disabled ? "MenuItem.disabledForeground" : "MenuItem.foreground");
			}
			if (color == null) {
				color = UIManager.getColor("Menu.foreground");
			}
			if (color == null) {
				color = UIManager.getColor("Label.foreground");
			}
			if (color == null) {
				color = Color.BLACK;
			}
			return color;
		}

		/**
		 * Determine the macOS menu text color (light/dark) with a disabled fallback.
		 */
		private Color getMacOsMenuColor(boolean disabled) {
			if (SystemInfo.getPlatform() != SystemInfo.Platform.MAC_OS) {
				return null;
			}
			Color base = java.awt.SystemColor.menuText;
			if (base != null) {
				if (!disabled) {
					return base;
				}
				int alpha = (base.getRed() + base.getGreen() + base.getBlue() < 384) ? 120 : 160;
				return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
			}
			try {
				OsThemeDetector detector = OsThemeDetector.getDetector();
				boolean isDark = detector.isDark();
				Color fallback = isDark ? Color.WHITE : Color.BLACK;
				if (!disabled) {
					return fallback;
				}
				int alpha = isDark ? 160 : 120;
				return new Color(fallback.getRed(), fallback.getGreen(), fallback.getBlue(), alpha);
			} catch (Exception ignore) {
				return null;
			}
		}
	}
}
