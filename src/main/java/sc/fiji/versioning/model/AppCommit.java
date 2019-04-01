package sc.fiji.versioning.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AppCommit {
	public String commitMsg;
	public String id;
	public Date date;
	public List<FileChange> changes;

	@Override
	public String toString() {
		return new SimpleDateFormat().format(date);
	}
}
