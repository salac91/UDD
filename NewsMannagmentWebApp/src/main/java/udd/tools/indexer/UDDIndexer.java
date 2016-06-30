package udd.tools.indexer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import udd.tools.analyzer.SerbianAnalyzer;
import udd.tools.indexer.handler.DocumentHandler;
import udd.tools.indexer.handler.PDFHandler;
import udd.tools.searcher.ResultRetriever;

public final class UDDIndexer {
	
	private static final Version matchVersion = Version.LUCENE_4_9;
	private IndexWriter indexWriter;
	private Directory indexDir;
	
	private final Analyzer analyzer = new SerbianAnalyzer(matchVersion);
	
	private IndexWriterConfig iwc = new IndexWriterConfig(matchVersion, analyzer);
	
	/**
	 * Starts a custom indexer 
	 * @param path - indexer location on disc
	 * @param restart - true to start a new indexer, false to merge with old indexer if present in path folder
	 */
	public UDDIndexer(String path, boolean restart){
		try{
			this.indexDir = new SimpleFSDirectory(new File(path));
			if(restart){
				iwc.setOpenMode(OpenMode.CREATE);
				this.indexWriter = new IndexWriter(indexDir, iwc);
				this.indexWriter.deleteAll(); //nije potrebno
				this.indexWriter.commit();
				this.indexWriter.close();
			}
		}catch(IOException ioe){
			throw new IllegalArgumentException("Path not correct");
		}
	}
	
	private void openIndexWriter() throws IOException{
		iwc = new IndexWriterConfig(matchVersion, analyzer);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		this.indexWriter = new IndexWriter(indexDir, iwc);
	}
	
	/**
	 * Starts an indexer at predefined location
	 * @param restart - true to start a new indexer, false to merge with old indexer if present in path folder
	 */
	public UDDIndexer(boolean restart){
		this(ResourceBundle.getBundle("index").getString("index"), restart);
	}
	
	/**
	 * Starts an indexer at given location or merges with an existing one
	 * @param path - indexer location on disc
	 */
	public UDDIndexer(String path){
		this(path, false);
	}
	
	/**
	 * Starts an indexer at predefined location or merges with an existing one
	 */
	public UDDIndexer(){
		this(ResourceBundle.getBundle("index").getString("index"), false);
	}
	
	public IndexWriter getIndexWriter(){
		return this.indexWriter;
	}
	
	public Directory getIndexDir(){
		return this.indexDir;
	}
	
	/**
	 * 
	 * @param doc - document which will be indexed
	 * @return - true if adding was successful
	 */
	public boolean addDocument(Document doc){ //indeksiranje tacno jednog dokumenta
		try {
			openIndexWriter();
			this.indexWriter.addDocument(doc);
			this.indexWriter.commit();
			this.indexWriter.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param doc - the document which will be updated
	 * @param fields - array of updating fields
	 * @return true if update was successful, othewise false
	 */
	public boolean updateDocument(Document doc, IndexableField... fields){
		String location = doc.get("location");
		replaceFields(doc, fields);
		
		try{
			synchronized (this) {
				openIndexWriter();
				this.indexWriter.updateDocument(new Term("location", location), doc);
				this.indexWriter.forceMergeDeletes();
				this.indexWriter.deleteUnusedFiles();
				this.indexWriter.commit();
				this.indexWriter.close();
			}
			return true;
		}catch(Exception ex){
			return false;
		}
	}
	
	public void replaceFields(Document doc, IndexableField... fields){
		for(IndexableField field : fields){
			doc.removeFields(field.name());
		}
		for(IndexableField field : fields){
			doc.add(field);
		}
	}
	
	/**
	 * 
	 * @param id - ID of the document which will be updated
	 * @param fields - array of updating fields
	 * @return - true if update was successful, false otherwise
	 */
	
	public boolean updateDocument(int id, IndexableField... fields){
		try {
			DirectoryReader reader = DirectoryReader.open(indexDir);
			return updateDocument(reader.document(id), fields);
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Update all documents which contain a field which has a name same as fieldName and a value same as fieldValue
	 * @param fieldName - name of the field
	 * @param fieldValue - value of the field
	 * @param fields - array of updating fields
	 * @return
	 */
	public boolean updateDocuments(String fieldName, String fieldValue, IndexableField... fields){
		try{
			openIndexWriter();
			
			Term term = new Term(fieldName, fieldValue);
			Query query = new TermQuery(term); 
			List<Document> docs = ResultRetriever.getResults(query);
			
			String location;
			for(Document doc : docs){
				location = doc.get("location");
				replaceFields(doc, fields);
				this.indexWriter.updateDocument(new Term("location", location), doc);
			}
			//commit-ovanje promena
			this.indexWriter.forceMergeDeletes();
			this.indexWriter.deleteUnusedFiles();
			this.indexWriter.commit();
			this.indexWriter.close();
			return true;
		}catch(IOException ioe){
			return false;
		}
		
	}
	
	/**
	 * Delete a single document
	 * @param doc
	 * @return
	 */
	public boolean deleteDocument(Document doc){
		if(doc == null) return false;
		//obrisati tacno jedan dokument i to ovaj prosledjeni
		Term delTerm = new Term("id", doc.get("id"));
		try {
			synchronized (this) {
				openIndexWriter();
				this.indexWriter.deleteDocuments(delTerm);
				this.indexWriter.deleteUnusedFiles();
				this.indexWriter.forceMergeDeletes();
				this.indexWriter.commit();
				this.indexWriter.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Delete a single document
	 * @param id
	 * @return
	 */
	public boolean deleleDocument(int id){
		try {
			DirectoryReader reader = DirectoryReader.open(indexDir);
			return deleteDocument(reader.document(id));
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Delete all documents which have a field named fieldName with value fieldValue
	 * @param fieldName - name of the field
	 * @param fieldValue - value of the field
	 * @return
	 */
	public boolean deleteDocuments(String fieldName, String fieldValue){
		return deleteDocuments(new Term(fieldName, fieldValue));
	}
	
	/**
	 * Delete all documents which contain any of the given terms
	 * @param fieldName - name of the field
	 * @param fieldValue - value of the field
	 * @return
	 */
	public boolean deleteDocuments(Term... delTerms){
		try {
			synchronized (this) {
				openIndexWriter();
				this.indexWriter.deleteDocuments(delTerms);
				this.indexWriter.forceMergeDeletes();
				this.indexWriter.deleteUnusedFiles();
				this.indexWriter.commit();
				this.indexWriter.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Delete all documents which match any of the given queries
	 * @param fieldName - name of the field
	 * @param fieldValue - value of the field
	 * @return
	 */
	public boolean deleteDocuments(Query... delQueries){
		try {
			synchronized (this) {
				openIndexWriter();
				this.indexWriter.deleteDocuments(delQueries);
				this.indexWriter.forceMergeDeletes();
				this.indexWriter.deleteUnusedFiles();
				this.indexWriter.commit();
				this.indexWriter.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * 
	 * @return all indexed lucene documents
	 */
	public Document[] getAllDocuments(){
		//collect and return all documents
		try {
			DirectoryReader reader = DirectoryReader.open(indexDir);
			Document[] docs = new Document[reader.maxDoc()];
			for(int i = 0; i < reader.maxDoc(); i++){
				docs[i] = reader.document(i);
			}
			return docs;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Index a predefined set of files
	 * @return true if indexing was successful
	 */
	public void index(){
		index(new File(ResourceBundle.getBundle("index").getString("docs")));
	}
	
	/**
	 * 
	 * @param folder - folder which contains a set of documents to be indexed. All subfolders will be indexed too. Existing folder is expected and IllegalArgumentException is thrown if otherwise
	 * @return true if indexing was successful
	 */
	public void index(File folder){
		try {
			openIndexWriter();
			if(folder.isDirectory()){
				folderIndexer(folder);
			}else{
				//indeksiranje tacno jednog file-a
				fileIndexer(folder);
			}
			this.indexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Requires indexer to be opened
	 * @param folder - a folder in file system which contains documents to be indexed
	 * @return
	 */
	private void folderIndexer(File folder){
		if(!folder.isDirectory()) return;
		File[] files = folder.listFiles();
		
		for(File file : files){
			if(file.isFile()){
				fileIndexer(file);
			}else{
				folderIndexer(file);
			}
		}
	}
	
	private void fileIndexer(File file){
		DocumentHandler handler;
		handler = getHandler(file);
		if(handler != null){
			try {
				this.indexWriter.addDocument(handler.getDocument(file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			//unsupported type
		}
	}
	
	/**
	 * 
	 * @param file
	 * @return proper document handler if document is supported, otherwise null
	 */
	public static DocumentHandler getHandler(File file){
		//za svaki file uzeti odgovarajuci DocumentHandler
		if(file.isDirectory()) return null; //ako je u pitanju direktorijum
		DocumentHandler handler = null;
		
		if(file.getName().endsWith(".pdf"))
			handler = new PDFHandler();
	
		return handler;
	}

}
