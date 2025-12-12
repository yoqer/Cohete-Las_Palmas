package info.openrocket.swing.gui.util;

import info.openrocket.core.util.ChangeSource;
import info.openrocket.core.util.StateChangeListener;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Helpers for applying FlatLaf component outlines (e.g. "warning", "error") via
 * the {@code JComponent.outline} client property.
 */
public final class FlatLafOutlines {
	public static final String OUTLINE_PROPERTY = "JComponent.outline";
	private static final String BASE_TOOLTIP_PROPERTY = FlatLafOutlines.class.getName() + ".baseToolTip";
	private static final Object NULL_TOOLTIP = new Object();

	public enum Outline {
		NONE(null),
		WARNING("warning"),
		ERROR("error");

		private final String flatLafValue;

		Outline(String flatLafValue) {
			this.flatLafValue = flatLafValue;
		}

		String getFlatLafValue() {
			return flatLafValue;
		}
	}

	private FlatLafOutlines() {
	}

	private static void forEachRelatedComponent(JComponent component, java.util.function.Consumer<JComponent> consumer) {
		consumer.accept(component);
		if (component instanceof JSpinner spinner) {
			JComponent editor = spinner.getEditor();
			if (editor != null) {
				consumer.accept(editor);
				if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
					JFormattedTextField textField = defaultEditor.getTextField();
					if (textField != null) {
						consumer.accept(textField);
					}
				}
			}
		}
	}

	public static void setOutline(JComponent component, Outline outline) {
		Objects.requireNonNull(component, "component");
		final String value = outline == null ? null : outline.getFlatLafValue();

		forEachRelatedComponent(component, c -> c.putClientProperty(OUTLINE_PROPERTY, value));
	}

	public static Outline getOutline(JComponent component) {
		Objects.requireNonNull(component, "component");
		Object value = component.getClientProperty(OUTLINE_PROPERTY);
		if (!(value instanceof String outlineValue) || outlineValue.isEmpty()) {
			return Outline.NONE;
		}
		if (Outline.ERROR.getFlatLafValue().equals(outlineValue)) {
			return Outline.ERROR;
		}
		if (Outline.WARNING.getFlatLafValue().equals(outlineValue)) {
			return Outline.WARNING;
		}
		return Outline.NONE;
	}

	private static void setValidationTooltip(JComponent component, String message) {
		String tooltipMessage = message != null ? message.trim() : null;
		if (tooltipMessage != null && tooltipMessage.isEmpty()) {
			tooltipMessage = null;
		}

		final String finalTooltipMessage = tooltipMessage;
		forEachRelatedComponent(component, c -> setValidationTooltipOnSingleComponent(c, finalTooltipMessage));
	}

	private static void setValidationTooltipOnSingleComponent(JComponent component, String message) {
		if (message == null) {
			Object base = component.getClientProperty(BASE_TOOLTIP_PROPERTY);
			if (base == null) {
				return;
			}
			component.setToolTipText(base == NULL_TOOLTIP ? null : (String) base);
			component.putClientProperty(BASE_TOOLTIP_PROPERTY, null);
			return;
		}

		Object base = component.getClientProperty(BASE_TOOLTIP_PROPERTY);
		if (base == null) {
			String existing = component.getToolTipText();
			component.putClientProperty(BASE_TOOLTIP_PROPERTY, existing == null ? NULL_TOOLTIP : existing);
		}

		component.setToolTipText(formatTooltipText(message));
	}

	private static String formatTooltipText(String tooltip) {
		if (tooltip == null) {
			return null;
		}
		String trimmed = tooltip.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (isHtml(trimmed)) {
			return trimmed;
		}
		return "<html>" + escapeHtml(trimmed).replace("\n", "<br>") + "</html>";
	}

	private static boolean isHtml(String tooltip) {
		return tooltip.toLowerCase(Locale.ROOT).startsWith("<html");
	}

	private static String escapeHtml(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&#39;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	public static final class Validator implements AutoCloseable {
		private record Condition(Outline outline, BooleanSupplier predicate, Supplier<String> message) {
		}

		private final JComponent component;
		private final List<Condition> conditions = new ArrayList<>();
		private final Set<ChangeSource> sources = new HashSet<>();
		private final StateChangeListener listener = e -> update();
		private int transientMessageDurationMs = 0;
		private Outline lastOutline = Outline.NONE;
		private String lastMessage = null;
		private Popup transientPopup;
		private Timer transientTimer;

		private Validator(JComponent component) {
			this.component = Objects.requireNonNull(component, "component");
		}

		public Validator warnIf(BooleanSupplier predicate) {
			return warnIf(predicate, (Supplier<String>) null);
		}

		public Validator warnIf(BooleanSupplier predicate, String message) {
			return warnIf(predicate, message == null ? null : () -> message);
		}

		public Validator warnIf(BooleanSupplier predicate, Supplier<String> message) {
			conditions.add(new Condition(Outline.WARNING, Objects.requireNonNull(predicate, "predicate"), message));
			return this;
		}

		public Validator errorIf(BooleanSupplier predicate) {
			return errorIf(predicate, (Supplier<String>) null);
		}

		public Validator errorIf(BooleanSupplier predicate, String message) {
			return errorIf(predicate, message == null ? null : () -> message);
		}

		public Validator errorIf(BooleanSupplier predicate, Supplier<String> message) {
			conditions.add(new Condition(Outline.ERROR, Objects.requireNonNull(predicate, "predicate"), message));
			return this;
		}

		/**
		 * When enabled, shows the validation message as a tooltip-like popup near the component
		 * for a short period when the invalid state first appears or when the message changes.
		 */
		public Validator showMessagePopup(int durationMs) {
			this.transientMessageDurationMs = Math.max(0, durationMs);
			return this;
		}

		public Validator listenTo(ChangeSource... changeSources) {
			if (changeSources == null) {
				return this;
			}
			for (ChangeSource source : changeSources) {
				if (source == null || !sources.add(source)) {
					continue;
				}
				source.addChangeListener(listener);
			}
			update();
			return this;
		}

		public void update() {
			if (SwingUtilities.isEventDispatchThread()) {
				updateNow();
			} else {
				SwingUtilities.invokeLater(this::updateNow);
			}
		}

		private void updateNow() {
			Outline outline = Outline.NONE;
			List<String> messages = new ArrayList<>();
			for (Condition condition : conditions) {
				if (!condition.predicate().getAsBoolean()) {
					continue;
				}
				Outline conditionOutline = condition.outline();
				if (conditionOutline.ordinal() > outline.ordinal()) {
					outline = conditionOutline;
					messages.clear();
				}
				if (conditionOutline == outline && condition.message() != null) {
					String message = condition.message().get();
					if (message != null && !message.trim().isEmpty()) {
						messages.add(message.trim());
					}
				}
			}
			FlatLafOutlines.setOutline(component, outline);
			String message = messages.isEmpty() ? null : String.join("\n", messages);
			setValidationTooltip(component, message);

			if (outline == Outline.NONE || message == null) {
				hideTransientMessage();
			} else if (transientMessageDurationMs > 0 && (lastOutline == Outline.NONE || !Objects.equals(lastMessage, message))) {
				showTransientMessage(message, transientMessageDurationMs);
			}

			lastOutline = outline;
			lastMessage = message;
		}

		private void showTransientMessage(String message, int durationMs) {
			hideTransientMessage();
			if (!component.isShowing()) {
				return;
			}
			Point location;
			try {
				location = component.getLocationOnScreen();
			} catch (IllegalComponentStateException ignore) {
				return;
			}

			JToolTip toolTip = component.createToolTip();
			String tipText = formatTooltipText(message);
			if (tipText == null) {
				return;
			}
			toolTip.setTipText(tipText);
			toolTip.setComponent(component);

			int x = location.x;
			int y = location.y + component.getHeight() + 2;
			transientPopup = PopupFactory.getSharedInstance().getPopup(component, toolTip, x, y);
			transientPopup.show();

			transientTimer = new Timer(durationMs, e -> hideTransientMessage());
			transientTimer.setRepeats(false);
			transientTimer.start();
		}

		private void hideTransientMessage() {
			if (transientTimer != null) {
				transientTimer.stop();
				transientTimer = null;
			}
			if (transientPopup != null) {
				transientPopup.hide();
				transientPopup = null;
			}
		}

		@Override
		public void close() {
			hideTransientMessage();
			setValidationTooltip(component, null);
			for (ChangeSource source : sources) {
				source.removeChangeListener(listener);
			}
			sources.clear();
		}
	}

	public static Validator validator(JComponent component) {
		return new Validator(component);
	}

	private static Outline max(Outline a, Outline b) {
		if (a == null) {
			return b == null ? Outline.NONE : b;
		}
		if (b == null) {
			return a;
		}
		return a.ordinal() >= b.ordinal() ? a : b;
	}
}
