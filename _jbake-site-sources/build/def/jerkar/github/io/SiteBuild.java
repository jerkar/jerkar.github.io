package jerkar.github.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkZipFile;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;

public class SiteBuild extends JkBuild {
	
	String jbakeSrcPath = "jbake-src";
	JkFileTree contentSource = baseDir().from("src-content");
	
	JkFileTree jbakeDir = baseDir().from("build/binaries/jbake-2.3.2");
	JkFileTree siteBase = JkFileTree.of(file("..")); 
	
	JkFileTree jerkarCoreDocDir = JkFileTree.of(file("../../jerkar/org.jerkar.core/src/main/doc"));
	JkFileTree jerkarDistJavadoc = JkFileTree.of(file("../../jerkar/org.jerkar.distrib-all/build/output/javadoc-all"));
	File jerkarDistZip = siteBase.file("../jerkar/org.jerkar.distrib-all/build/output/jerkar-distrib.zip");

	
	JkFileTree jbakeSrcContent = baseDir().from(jbakeSrcPath + "/content");
	JkFileTree siteSourceDocDir = jbakeSrcContent.from("documentation");
	File siteDistDir = siteBase.file("binaries");
	JkFileTree siteTargetDocDir = baseDir().from("content/documentation");
	
	List<File> menuAddedFiles = jbakeSrcContent.include("**/*.md").exclude("about.md", "download.md", "tell-me-more.md").files(false);
	
	@Override
	public void clean() {
		siteBase.exclude(".*/**", "_*/**", "binaries/**").deleteAll();
		jbakeSrcContent.createIfNotExist().deleteAll();
	}
	
	@Override
	public void doDefault() {
		jbake();
	}
	
	@JkDoc({"Generates the site and imports documentation inside.", 
		    "You must have the Jerkar repo (containing the documentation) in your git home."})
	public void full() {
		clean();
		importContent();
		addMenu();
		addHeaders();
		jbake();
		copyCurrentDist();
		copyCurrentJavadoc();
	}
	
	public void importContent() {
		importDocFromJerkarProject();
		importSiteDoc();
	}
	
	public void importDocFromJerkarProject() {
		JkFileTree targetDocDir = siteSourceDocDir.from("latest");
		List<File> files = jerkarCoreDocDir.include("**/*.md").exclude("reference/**/*").files(false);
		File temp = JkUtilsFile.tempFile("reference.md");
		jerkarCoreDocDir.from("reference").mergeTo(temp);
		files.add(temp);
		for (File file : files) {
			String relativePath = JkUtilsFile.isAncestor(jerkarCoreDocDir.root(),  file) ? 
					jerkarCoreDocDir.relativePath(file) : file.getName();
			File copied = targetDocDir.file(relativePath);
			JkLog.startln("Importing doc file " + file + " to " + copied.getPath());
			String content = JkUtilsFile.read(file);
			JkUtilsFile.writeString(copied, content, true);
			JkLog.done();
		}
		temp.delete();
	}
	
	public void addHeaders() {
		for (File file : menuAddedFiles) {
			String content = header(file);
			JkUtilsFile.writeStringAtTop(file, content);
		}
	}
	
	
	public void importSiteDoc() {
		File dest = new File(this.jbakeSrcPath, "content");
		contentSource.copyReplacingTokens(dest, JkUtilsIterable.mapOf("jerkarVersion", currentJerkarVersion()));
	}
	
	public void copyCurrentDist() {
		JkUtilsFile.copyFileToDir(jerkarDistZip, siteDistDir, JkLog.infoStream());
		
		// Retrieve the current version
		
	}
	
	private String currentJerkarVersion() {
		JkZipFile zipFile = JkZipFile.of(jerkarDistZip);
		InputStream jarStream = zipFile.inputStream("org.jerkar.core.jar");
		ZipInputStream manifestStream = JkUtilsIO.readZipEntry(jarStream, "META-INF/MANIFEST.MF");
		JkManifest manifest = JkManifest.of(manifestStream);
		JkUtilsIO.closeQuietly(manifestStream, jarStream, zipFile);
		return manifest.mainAttribute("version");		
	}
	
	public void copyCurrentJavadoc() {
		JkUtilsFile.copyDirContent(jerkarDistJavadoc.root(), siteBase.file("javadoc/latest"), false);
	}
	
	
	public void jbake() {
		JkJavaProcess.of().withClasspath(jbakeDir.include("lib.*.jar"))
		.runJarSync(jbakeDir.file("jbake-core.jar"), jbakeSrcPath, "..");
	}
	
	private static String header(File file) {
		String title = JkUtilsString.substringBeforeLast(file.getName(), ".md");
		title = title.replace("_", " ");
		StringBuilder result = new StringBuilder();
		result.append("title=").append(title).append("\n")
			.append("date="+ JkUtilsTime.now("yyyy-MM-dd")).append("\n")
			.append("type=page\n")
			.append("status=published\n")
			.append("addSideMenu=true\n")
			.append("~~~~~~\n\n");
		return result.toString();
	}
	
	public static void main(String[] args) {
		new SiteBuild().clean();
	}
	
	private void addMenu() {
		for (File file : menuAddedFiles) {
			String menuHtml = ImplicitMenu.ofMdFile(file, 2).divSideBarAndScriptHtml();
			JkUtilsFile.writeStringAtTop(file, menuHtml);
			JkUtilsFile.writeString(file, ImplicitMenu.endDivHtml("end of wrapper div"), true);
		}
	}
	
	

}
