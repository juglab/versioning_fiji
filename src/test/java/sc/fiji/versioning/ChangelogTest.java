package sc.fiji.versioning;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class ChangelogTest {

	String githubApi = "https://api.github.com";

	private class Change {
		public String author;
		public String message;
		public String date;
		public String toString() {
			return "\n--------------------\n" + date + ", " + author + ":\n " + message;
		}
	}

	@Test
	public void testChangelog() {
		assertNotNull(getChangelog("imagej-ops", "0.42.0", "0.43.1"));
//		assertNotNull(getChangelog("csbdeep", "0.3.3", "0.3.4"));
//		assertNotNull(getChangelog("imagej-common", "0.28.0", "0.28.1"));
//		assertNotNull(getChangelog("scijava-common", "2.76.1", "2.77.0"));
	}


	public List<Change> getChangelog(String className, String versionBefore, String versionAfter) {
		System.out.println("CHANGELOG " + className + " " + versionBefore + " -> " + versionAfter);
		List<Change> log = getChanges(className, getGithubUrl(className, versionAfter), versionBefore, versionAfter);
		for(Change change : log) {
			System.out.println(change);
		}
		return log;
	}

	private List<Change> getChanges(String className, String githubUrl, String versionBefore, String versionAfter) {
		String repo = githubUrl.replace("https://github.com", "");
		String beforeCommit = null, afterCommit = null;
		List<Change> changes = new ArrayList<>();
		try {
			JSONArray tagsObj = new JSONArray(fromURL(new URL(githubApi + "/repos" + repo + "/tags")));
			for (int i = 0; i < tagsObj.length() && (beforeCommit == null || afterCommit == null); i++) {
				Object name = ((JSONObject) tagsObj.get(i)).get("name");
				if(name == null) continue;
				if(name.equals(className + "-" + versionBefore)) {
					JSONObject commit = (JSONObject) ((JSONObject) tagsObj.get(i)).get("commit");
					if(commit == null) continue;
					beforeCommit = commit.getString("sha");
				}
				if(name.equals(className + "-" + versionAfter)) {
					JSONObject commit = (JSONObject) ((JSONObject) tagsObj.get(i)).get("commit");
					if(commit == null) continue;
					afterCommit = commit.getString("sha");
				}
			}
			URL compareUrl = new URL(githubApi + "/repos" + repo + "/compare/" + beforeCommit + "..." + afterCommit);
			JSONObject compareObj = new JSONObject(fromURL(compareUrl));
			JSONArray commits = (JSONArray) compareObj.get("commits");
			for (int i = 0; i < commits.length(); i++) {
				Change change = new Change();
//				change.author = getNameFromLogin(((JSONObject)((JSONObject) commits.get(i)).get("author")).getString("login"));
				change.author = ((JSONObject)((JSONObject) commits.get(i)).get("author")).getString("login");
				change.message = ((JSONObject)((JSONObject) commits.get(i)).get("commit")).getString("message");
				if(change.message.contains("[maven-release-plugin]")) continue;
				if(change.message.contains("Bump to next development cycle")) continue;
				if(change.message.contains("Merge pull request")) {
					System.out.println("MERGE: " + change.message);
					continue;
				}
				change.date = ((JSONObject)(((JSONObject)((JSONObject) commits.get(i)).get("commit")).get("author"))).getString("date");
				changes.add(change);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return changes;
	}

	private String getNameFromLogin(String login) {
		try {
			URL userUrl = new URL(githubApi + "/users/" + login);
			JSONObject userObj = new JSONObject(fromURL(userUrl));
			if(userObj == null || userObj.isNull("name")) return login;
			return userObj.getString("name");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getGithubUrl(String jarName, String version) {

		URL pomUrl = getPomUrl(jarName, version);
		String output = null;
		try {
			output = fromURL(pomUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(output == null) {
			System.out.println("Could not read " + pomUrl);
			return null;
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(output)));

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList scm = doc.getElementsByTagName("scm");
			if(scm == null || scm.getLength() == 0) {
				System.out.println("no scm tag found");
				return null;
			}
			NodeList url = ((Element) scm.item(0)).getElementsByTagName("url");
			if(url == null || url.getLength() == 0) {
				System.out.println("no scm/url tag found");
				return null;
			}
			return url.item(0).getFirstChild().getNodeValue();

		} catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
		}

		return getPomUrl(jarName, version).getPath();
	}

	private URL getPomUrl(String jarName, String version) {
		try {

			URL url = new URL("https://dais-maven.mpi-cbg.de/service/rest/v1/search/assets?name=" + jarName + "&maven.extension=pom&maven.classifier");

			JSONObject pomObj = new JSONObject(fromURL(url));
			JSONArray items = pomObj.getJSONArray("items");
			if(items == null || items.length() == 0) return null;
			JSONObject entry = (JSONObject) items.get(0);
			String downloadUrl = entry.getString("downloadUrl");

			return new URL(downloadUrl.substring(0, downloadUrl.indexOf(jarName)) + jarName + "/" + version + "/" + jarName + "-" + version + ".pom");

		} catch (MalformedURLException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return null;

	}

	String fromURL(URL url) throws IOException {
		System.out.println("Loading " + url);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

		String line;
		String output = "";
		while ((line = br.readLine()) != null) {
			output += line;
		}
		if(output.isEmpty()) return null;
		conn.disconnect();
		return output;
	}

}
