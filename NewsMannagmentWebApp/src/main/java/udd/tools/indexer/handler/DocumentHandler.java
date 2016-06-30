package udd.tools.indexer.handler;

import java.io.File;

import org.apache.lucene.document.Document;

import udd.exceptions.IncompleteIndexDocumentException;

public abstract class DocumentHandler {
	public abstract Document getDocument(File file) throws IncompleteIndexDocumentException;
}
