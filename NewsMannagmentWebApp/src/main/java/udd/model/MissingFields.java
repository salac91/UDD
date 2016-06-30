package udd.model;

public class MissingFields {
	
	private boolean uid = false;; 
	
	private boolean title = false;
	
	private boolean apstract = false;
	
	private boolean category = false;
	
	private boolean keywords = false;
	
	private boolean tags = false;
	
	private boolean author = false;
	
	private boolean creationDate = false;

	public boolean isUid() {
		return uid;
	}

	public void setUid(boolean uid) {
		this.uid = uid;
	}

	public boolean isTitle() {
		return title;
	}

	public void setTitle(boolean title) {
		this.title = title;
	}

	public boolean isApstract() {
		return apstract;
	}

	public void setApstract(boolean apstract) {
		this.apstract = apstract;
	}

	public boolean isCategory() {
		return category;
	}

	public void setCategory(boolean category) {
		this.category = category;
	}

	public boolean isKeywords() {
		return keywords;
	}

	public void setKeywords(boolean keywords) {
		this.keywords = keywords;
	}

	public boolean isTags() {
		return tags;
	}

	public void setTags(boolean tags) {
		this.tags = tags;
	}

	public boolean isAuthor() {
		return author;
	}

	public void setAuthor(boolean author) {
		this.author = author;
	}

	public boolean isCreationDate() {
		return creationDate;
	}

	public void setCreationDate(boolean creationDate) {
		this.creationDate = creationDate;
	}
	
	
}
