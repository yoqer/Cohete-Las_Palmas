package info.openrocket.swing.gui.util;

import info.openrocket.core.util.ChangeSource;
import info.openrocket.core.util.StateChangeListener;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Helpers for applying FlatLaf component outlines (e.g. "warning", "error") via
 * the {@code JComponent.outline} client property.
 */
public final class FlatLafOutlines {
	public static final String OUTLINE_PROPERTY = "JComponent.outline";

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

	public static void setOutline(JComponent component, Outline outline) {
		Objects.requireNonNull(component, "component");
		final String value = outline == null ? null : outline.getFlatLafValue();

		component.putClientProperty(OUTLINE_PROPERTY, value);

		if (component instanceof JSpinner spinner) {
			JComponent editor = spinner.getEditor();
			if (editor != null) {
				editor.putClientProperty(OUTLINE_PROPERTY, value);
				if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
					JFormattedTextField textField = defaultEditor.getTextField();
					if (textField != null) {
						textField.putClientProperty(OUTLINE_PROPERTY, value);
					}
				}
			}
		}
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

	public static final class Validator implements AutoCloseable {
		private record Condition(Outline outline, BooleanSupplier predicate) {
		}

		private final JComponent component;
		private final List<Condition> conditions = new ArrayList<>();
		private final Set<ChangeSource> sources = new HashSet<>();
		private final StateChangeListener listener = e -> update();

		private Validator(JComponent component) {
			this.component = Objects.requireNonNull(component, "component");
		}

		public Validator warnIf(BooleanSupplier predicate) {
			conditions.add(new Condition(Outline.WARNING, Objects.requireNonNull(predicate, "predicate")));
			return this;
		}

		public Validator errorIf(BooleanSupplier predicate) {
			conditions.add(new Condition(Outline.ERROR, Objects.requireNonNull(predicate, "predicate")));
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
			for (Condition condition : conditions) {
				if (condition.predicate().getAsBoolean()) {
					outline = max(outline, condition.outline());
				}
			}
			FlatLafOutlines.setOutline(component, outline);
		}

		@Override
		public void close() {
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

