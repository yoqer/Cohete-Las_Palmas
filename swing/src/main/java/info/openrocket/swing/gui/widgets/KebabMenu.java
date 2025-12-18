package info.openrocket.swing.gui.widgets;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A kebab menu (three vertical dots) that can be placed anywhere like a button, while still being a {@link JMenu}.
 * <pre>
 * KebabMenu menu = new KebabMenu();
 * menu.add(new javax.swing.JMenuItem("Rename"));
 * menu.addSeparator();
 * menu.add(new javax.swing.JCheckBoxMenuItem("Pinned", true));
 * </pre>
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class KebabMenu extends JMenu {
	private static final Insets DEFAULT_MARGIN = new Insets(3, 1, 3, 1);
	private static final int MIN_ICON_SIZE = 16;

	private boolean hovering = false;
	private boolean popupVisibleOnPress = false;
	private transient JButton paintButton;

	public KebabMenu() {
		super();
		init();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		paintButton = null;
		applyButtonStyle();
		updateKebabIcon();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (!isStandaloneMenu()) {
			super.paintComponent(g);
			return;
		}

		JButton button = getPaintButton();
		syncPaintButton(button);
		button.getUI().update(g, button);
	}

	/**
	 * Treat this menu as a "top-level" menu unless it's inside a {@link JPopupMenu}.
	 * This avoids submenu arrow/check layout when the menu is used as a standalone component.
	 */
	@Override
	public boolean isTopLevelMenu() {
		return !(getParent() instanceof JPopupMenu);
	}

	private void init() {
		setText(null);
		setHorizontalAlignment(SwingConstants.CENTER);
		setFocusable(true);
		setFocusPainted(false);
		setContentAreaFilled(true);
		setMargin(DEFAULT_MARGIN);

		applyButtonStyle();
		updateKebabIcon();
		installPopupMenuListener();
		installMouseListener();
	}

	/**
	 * Apply button-like styling from the current Look and Feel.
	 */
	private void applyButtonStyle() {
		setOpaque(false);
		setRolloverEnabled(true);

		if (UIManager.getBorder("Button.border") != null) {
			setBorder(UIManager.getBorder("Button.border"));
			setBorderPainted(true);
		}

		Color background = UIManager.getColor("Button.background");
		if (background != null) {
			setBackground(background);
		}

		Color foreground = UIManager.getColor("Button.foreground");
		if (foreground != null) {
			setForeground(foreground);
		}
	}

	/**
	 * Lazily create and return the button used for painting.
	 * @return the paint button
	 */
	private JButton getPaintButton() {
		if (paintButton == null) {
			paintButton = new JButton();
			paintButton.setFocusable(false);
			paintButton.setFocusPainted(false);
			paintButton.setContentAreaFilled(true);
			paintButton.setBorderPainted(true);
			paintButton.setRolloverEnabled(true);
		}
		return paintButton;
	}

	/**
	 * Sync the paint button's properties with this menu.
	 * @param button the paint button
	 */
	private void syncPaintButton(JButton button) {
		button.setBounds(0, 0, getWidth(), getHeight());
		button.setComponentOrientation(getComponentOrientation());
		button.setEnabled(isEnabled());
		button.setFont(getFont());
		button.setBackground(getBackground());
		button.setForeground(getForeground());
		button.setBorder(getBorder());
		button.setBorderPainted(isBorderPainted());
		button.setContentAreaFilled(isContentAreaFilled());
		button.setMargin(getMargin());
		button.setHorizontalAlignment(getHorizontalAlignment());
		button.setVerticalAlignment(getVerticalAlignment());
		button.setIcon(getIcon());
		button.setText(getText());

		boolean popupVisible = isPopupMenuVisible();
		button.getModel().setRollover(hovering && !popupVisible);
		button.getModel().setPressed(popupVisible);
		button.getModel().setArmed(popupVisible);
	}

	/**
	 * Update the kebab icon size based on the current font size.
	 */
	private void updateKebabIcon() {
		int fontSize = getFont() != null ? getFont().getSize() : MIN_ICON_SIZE;
		int iconSize = Math.max(MIN_ICON_SIZE, fontSize);
		setIcon(new KebabIcon(iconSize));
	}

	private void installPopupMenuListener() {
		getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				getModel().setArmed(true);
				getModel().setSelected(true);
				repaint();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				getModel().setSelected(false);
				getModel().setArmed(hovering);
				repaint();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				getModel().setSelected(false);
				getModel().setArmed(hovering);
				repaint();
			}
		});
	}

	private void installMouseListener() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (!isStandaloneMenu()) {
					return;
				}
				hovering = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!isStandaloneMenu()) {
					return;
				}
				hovering = false;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (!isStandaloneMenu() || !isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				requestFocusInWindow();
				popupVisibleOnPress = isPopupMenuVisible();
				if (popupVisibleOnPress) {
					hidePopupMenu();
				}
				e.consume();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!isStandaloneMenu() || !isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				if (popupVisibleOnPress) {
					return;
				}
				SwingUtilities.invokeLater(KebabMenu.this::showPopupMenu);
				e.consume();
			}
		});
	}

	/**
	 * Show the popup menu below this component.
	 */
	private void showPopupMenu() {
		Component invoker = getPopupInvoker();
		if (invoker == null) {
			return;
		}
		Point location = SwingUtilities.convertPoint(this, 0, getHeight(), invoker);
		getPopupMenu().show(invoker, location.x, location.y);
		repaint();
	}

	/**
	 * Hide the popup menu.
	 */
	private void hidePopupMenu() {
		getPopupMenu().setVisible(false);
		repaint();
	}

	/**
	 * Get the component to use as the popup menu invoker.
	 * @return the invoker component
	 */
	private Component getPopupInvoker() {
		if (!isStandaloneMenu()) {
			return this;
		}
		return getParent();
	}

	/**
	 * Check if this menu is used as a standalone component (not in a menu bar or popup menu).
	 * @return true if standalone, false otherwise
	 */
	private boolean isStandaloneMenu() {
		Container parent = getParent();
		return !(parent instanceof JMenuBar) && !(parent instanceof JPopupMenu);
	}

	/**
	 * An icon that paints three vertical dots (kebab menu).
	 */
		private record KebabIcon(int size) implements Icon {

		@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}

			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2 = (Graphics2D) g.create();
				try {
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					Color color = c.isEnabled() ? c.getForeground() : UIManager.getColor("Label.disabledForeground");
					if (color == null) {
						color = c.getForeground();
					}
					g2.setColor(color);

					int dot = Math.max(2, Math.round(size / 5f));
					int gap = dot;
					int total = dot * 3 + gap * 2;

					int dotX = x + (size - dot) / 2;
					int startY = y + (size - total) / 2;

					for (int i = 0; i < 3; i++) {
						int dotY = startY + i * (dot + gap);
						g2.fillOval(dotX, dotY, dot, dot);
					}
				} finally {
					g2.dispose();
				}
			}
		}
}
