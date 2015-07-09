package jerkar.github.io;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;

public class ImplicitMenu {
	
	private static final String INDENT = "    ";
	
	private List<Item> items;
	
	public static ImplicitMenu ofMdFile(File file, int startLevel) {
		List<Item> items = new LinkedList<ImplicitMenu.Item>();
		List<Item> parents = new LinkedList<ImplicitMenu.Item>();
		for (String line : JkUtilsFile.readLines(file)) {
			if (!line.startsWith("#")) {
				continue;
			}
			for (int i = startLevel; i <= 6; i++) {
				String header = JkUtilsString.repeat("#", i) + " ";
				if (line.startsWith(header)) {
					String name = line.substring(header.length());
					name = name.contains(">") ? JkUtilsString.substringAfterLast(name, ">") : name;
					Item item = new Item(name, i);
					Item parent = findParent(parents, item);
					if (parent == null) {
						items.add(item);
					} else {
						parent.children.add(item);
					}
					parents.add(item);
					
				}
			}
		}
		return new ImplicitMenu(items);
	}
	
	private static Item findParent(List<Item> parents, Item item) {
		if (parents.isEmpty()) {
			return null;
		}
		List<Item> list = new LinkedList<ImplicitMenu.Item>(parents);
		Collections.reverse(list);
		for (Item candidate : list) {
			if (candidate.level < item.level) {
				return candidate;
			}
		}
		return null;
	}  
	
	private ImplicitMenu(List<Item> items) {
		this.items = Collections.unmodifiableList(items);
	}
	
	public static String divContainerHtml() {
		return new StringBuilder("<div id&#61;\"static-content\" class&#61;\"col-md-10\">   <!-- wrapper content -->").append("\n")
				.toString();
		
	}
	
	public static String endDivHtml(String comment) {
		return new StringBuilder("</div> <!--").append(comment).append(" --> \n").toString();
	}
	
	public String divSideBarAndScriptHtml() {
		StringBuilder builder = new StringBuilder();
		builder.append("<div id=\"sidebar-menu\" class=\"col-md-3 hidden-xs hidden-sm\">\n" + INDENT + "<ul class=\"main-menu nav nav-stacked affix\">\n");
		for (Item item : items) {
			builder.append(htmlList(item, true));
		}
		builder.append(INDENT + "</ul>\n</div>\n");
		builder.append(htmlScript());
		builder.append("<div id=\"static-content\" class=\"col-md-9\">\n");
		return builder.toString();
	}
	
	public String htmlScript() {
		return JkUtilsIO.readAsString(ImplicitMenu.class.getResourceAsStream("js.txt"));
	}
	
	private String htmlList(Item item, boolean first) {
		StringBuilder builder = new StringBuilder();
		String indent = JkUtilsString.repeat(INDENT, item.level);
		builder.append(indent);
		builder.append("<li><a href=\""+action(item)).append("\">")
			.append(JkUtilsString.elipse(item.name, 40 - item.level*2)).append("</a>");
		if (item.children.isEmpty()) {
			builder.append("</li>\n");
		} else {
			String classname = first ? "main-menu" : "sub-menu";
			builder.append("\n").append(indent).append("  ")
			.append("<ul class=\""+ classname + "\">\n");
			for (Item child : item.children) {
				builder.append(htmlList(child, false));
			}
			
			builder.append(indent).append("  ")
			.append("</ul>\n").append(indent).append("</li>\n");
		}
		return builder.toString();
		
	}
	
	private String action(Item item) {
		return  "javascript:jerkarMoveTo('"+item.name+"',"+item.level+");";
	}
	
	static class Item {
		
		final String name;
		
		final int level;
		
		final List<Item> children;

		public Item(String name, int level) {
			super();
			this.name = name;
			this.level = level;
			this.children = new LinkedList<ImplicitMenu.Item>();
		}
		
		@Override
		public String toString() {
			return name + "(" + level + ")";
		}
		
	}

}
