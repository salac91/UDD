package udd.util;

import java.util.ArrayList;
import java.util.List;

import udd.model.DocumentModel;
import udd.model.RequiredHighlight;

public class SearchResultModel {
	
	private List<DocumentModel> documents = new ArrayList<DocumentModel>();
	
	private List<RequiredHighlight> suggestions = new ArrayList<RequiredHighlight>();

	public List<DocumentModel> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DocumentModel> documents) {
		this.documents = documents;
	}

	public List<RequiredHighlight> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<RequiredHighlight> suggestions) {
		this.suggestions = suggestions;
	}
	
	
}
