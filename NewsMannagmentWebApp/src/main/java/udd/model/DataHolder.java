package udd.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class DataHolder implements Serializable{
	
	private String title;
	private String keywords;
	private String author;
	private String fileName;
	private String location;
	private String highlight;
	
	public DataHolder() {
		super();
	}

	public DataHolder(String title, String keywords, String author, String fileName, 
			String location, String highlight) {
		super();
		this.title = title;
		this.keywords = keywords;
		this.author = author;
		this.fileName = fileName;
		this.location = location;
		this.highlight = highlight;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getHighlight() {
		return highlight;
	}

	public void setHighlight(String highlight) {
		this.highlight = highlight;
	}

}
