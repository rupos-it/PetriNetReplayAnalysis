package org.processmining.plugins.petrinet.replay;

/**
 * Possible replay actions.
 * 
 * @author hverbeek
 * 
 */
public enum ReplayAction {
	/**
	 * Insert an enabled transition that matches the head of the trace.
	 */
	INSERT_ENABLED_MATCH(0, "Insert Enabled Match"),
	/**
	 * Insert an enabled invisible transition.
	 */
	INSERT_ENABLED_INVISIBLE(1, "Insert Enabled Invisible"),
	/**
	 * Remove the head of the trace.
	 */
	REMOVE_HEAD(2, "Remove Head"),
	/**
	 * Insert an enabled transition that does not match the head of the trace.
	 */
	INSERT_ENABLED_MISMATCH(3, "Insert Enabled Mismatch"),
	/**
	 * Insert a disabled transition that matches the head of the trace.
	 */
	INSERT_DISABLED_MATCH(4, "Insert Disabled Match"),
	/**
	 * Insert a disabled transition that does not match the head of the trace.
	 */
	INSERT_DISABLED_MISMATCH(5, "Insert Disabled Mismatch");

	private final int value;
	private final String label;

	private ReplayAction(int value, String label) {
		this.value = value;
		this.label = label;
	}

	public int getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}
}
