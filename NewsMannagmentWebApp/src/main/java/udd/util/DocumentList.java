package udd.util;

import java.util.ArrayList;
import java.util.List;

import udd.model.DocumentModel;

public class DocumentList {
	
	private List<DocumentModel> documents = new ArrayList<DocumentModel>();
	
	public List<DocumentModel> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DocumentModel> documents) {
		this.documents = documents;
	}
	
	
}
