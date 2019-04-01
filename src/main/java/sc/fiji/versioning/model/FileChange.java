package sc.fiji.versioning.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileChange {

	public enum Status {
		ADD, DELETE, MODIFY, RENAME, COPY, VERSION_CHANGE;
	}

	// keep this synchronized with net.imagej.updater.FileObject
	public static Pattern versionPattern = Pattern.compile("(.+?)(-\\d+(\\.\\d+)+[a-z]?(-[A-Za-z0-9.]+|\\.GA)*)(\\.jar)");

	static final String NULLSTR = "/dev/null";

	public String oldPath, newPath;
	public Status status;

	@Override
	public String toString() {
		if(oldPath == null || oldPath.isEmpty() || oldPath.equals(NULLSTR) || oldPath.equals(newPath)) {
			return status.name() + " " + versionedName(newPath);
		}
		if(newPath == null || newPath.isEmpty() || newPath.equals(NULLSTR)) {
			return status.name() + " " + versionedName(oldPath);
		}
		if(status.equals(Status.VERSION_CHANGE)) {
			return status.name() + " " + unversionedName(oldPath) + " (" + version(oldPath) + " -> " + version(newPath) + ")";
		}
		return status.name() + " " + versionedName(oldPath) + " -> " + versionedName(newPath);
	}

	String unversionedName(String name) {
		Matcher matcher = versionPattern.matcher(name);
		if(matcher.matches()) {
			return matcher.group(1);
		}else {
			return name;
		}
	}

	String version(String name) {
		Matcher matcher = versionPattern.matcher(name);
		if(matcher.matches()) {
			return matcher.group(2).substring(1);
		}else {
			return "";
		}
	}

	String versionedName(String name) {
		Matcher matcher = versionPattern.matcher(name);
		if(matcher.matches()) {
			return matcher.group(1) + " (" + matcher.group(2).substring(1) + ")";
		}else {
			return name;
		}
	}

}
