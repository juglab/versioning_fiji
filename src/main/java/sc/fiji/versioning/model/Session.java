package sc.fiji.versioning.model;

public class Session {
	public String name;
	public String lastChange;

	@Override
	public String toString() {
		String printName = name.replace("refs/heads/", "");
		if(printName.equals("master")) return "default";
		return printName;
	}
}
