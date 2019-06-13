package step.artefacts.helper;

public class ArtefactHandlerHelper {
	public static long getNumberValueAsLong(Number value, long defaultValue) {
		return value == null ? defaultValue : value.longValue();
	}
}
