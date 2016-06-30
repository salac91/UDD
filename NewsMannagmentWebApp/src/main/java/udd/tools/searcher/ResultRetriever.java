
package udd.tools.searcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import udd.tools.analyzer.SerbianAnalyzer;

public class ResultRetriever {
	
	private static int maxHits = 10;
	private static final Version matchVersion = Version.LUCENE_4_9;
	private static Analyzer analyzer = new SerbianAnalyzer(matchVersion);
	private static QueryParser queryParser = new QueryParser(matchVersion, "", analyzer);
	
	public static void setAnalyzer(Analyzer analyzer){
		ResultRetriever.analyzer = analyzer;
		ResultRetriever.queryParser = new QueryParser(matchVersion, "", analyzer);
	}
	
	public static void setMaxHits(int maxHits){
		ResultRetriever.maxHits = maxHits;
	}
	
	public static int getMaxHits(){
		return ResultRetriever.maxHits;
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit
	 * Smatra se da je upit analiziran
	 * Direktorijum iz kojeg ce se citati indeksi se ucitava iz <i>index.properties</i> datoteke
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @return Lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 *  
	 */
	public static List<Document> getResults(Query query){
		String path = ResourceBundle.getBundle("index").getString("index");
		File indexDirPath = new File(path);
		return getResults(query, indexDirPath, false, null, null);
	}
	
	public static List<Document> getResults(Query query, Filter filter){
		String path = ResourceBundle.getBundle("index").getString("index");
		File indexDirPath = new File(path);
		return getResults(query, indexDirPath, false, null, filter);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit
	 * Smatra se da je upit analiziran
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param indexDirPath - direktorijum u kojem se nalaze indeksi
	 * @return Lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 */
	public static List<Document> getResults(Query query, File indexDirPath){
		return getResults(query, indexDirPath, false, null);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit
	 * Direktorijum iz kojeg ce se citati indeksi se ucitava iz <i>index.properties</i> datoteke
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param analyzeQuery - da li je potrebno analizirati upit.<p>Za vrednost:<br/> - <i>true</i> -> upit ce biti analiziran postojecim analyzer-om<br> - <i>false</i> -> upit nece biti analiziran<p><b>Napomena:</b> podrazumevani analyzer je {@link SerbianAnalyzer}. Za promenu analyzer-a koristiti metodu {@link #setAnalyzer(Analyzer)} 
	 * @return Lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 * @throws ParseException ako upit nije moguce parsirati
	 */
	public static List<Document> getResults(Query query, boolean analyzeQuery){
		String path = ResourceBundle.getBundle("index").getString("index");
		File indexDirPath = new File(path);
		return getResults(query, indexDirPath, analyzeQuery, null);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit. Dokumenata ima najvise
	 * Smatra se da je upit analiziran
	 * Direktorijum iz kojeg ce se citati indeksi se ucitava iz <i>index.properties</i> datoteke
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param sort - instanca klase {@link Sort} koja ce biti upotrebljena da sortira dokumente. Ukoliko je prosledjena <b>null</b> vrednost, dokumenti ce biti u redosledu pronalazenja u indeksu
	 * @return Sortirana lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 */
	public static List<Document> getResults(Query query, Sort sort, Filter filter){
		String path = ResourceBundle.getBundle("index").getString("index");
		File indexDirPath = new File(path);
		return getResults(query, indexDirPath, false, sort, filter);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param indexDirPath - direktorijum u kojem se nalaze indeksi
	 * @param analyzeQuery - da li je potrebno analizirati upit.<p>Za vrednost:<br/> - <i>true</i> -> upit ce biti analiziran postojecim analyzer-om<br> - <i>false</i> -> upit nece biti analiziran<p><b>Napomena:</b> podrazumevani analyzer je {@link SerbianAnalyzer}. Za promenu analyzer-a koristiti metodu {@link #setAnalyzer(Analyzer)} 
	 * @return Lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 */
	public static List<Document> getResults(Query query, File indexDirPath, boolean analyzeQuery, Filter filter){
		return getResults(query, indexDirPath, analyzeQuery, null, filter);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit
	 * Smatra se da je upit analiziran
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param indexDirPath - direktorijum u kojem se nalaze indeksi
	 * @param sort - instanca klase {@link Sort} koja ce biti upotrebljena da sortira dokumente. Ukoliko je prosledjena <b>null</b> vrednost, dokumenti ce biti u redosledu pronalazenja u indeksu
	 * @return Sortirana lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 */
	public static List<Document> getResults(Query query, File indexDirPath, Sort sort, Filter filter){
		return getResults(query, indexDirPath, false, sort, filter);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit. Dokumenata ima najvise
	 * Direktorijum iz kojeg ce se citati indeksi se ucitava iz <i>index.properties</i> datoteke
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param indexDirPath - direktorijum u kojem se nalaze indeksi
	 * @param analyzeQuery - da li je potrebno analizirati upit.<p>Za vrednost:<br/> - <i>true</i> -> upit ce biti analiziran postojecim analyzer-om<br> - <i>false</i> -> upit nece biti analiziran<p><b>Napomena:</b> podrazumevani analyzer je {@link SerbianAnalyzer}. Za promenu analyzer-a koristiti metodu {@link #setAnalyzer(Analyzer)} 
	 * @param sort - instanca klase {@link Sort} koja ce biti upotrebljena da sortira dokumente. Ukoliko je prosledjena <b>null</b> vrednost, dokumenti ce biti u redosledu pronalazenja u indeksu
	 * @return Sortirana lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 * @throws ParseException ako upit nije moguce parsirati
	 */
	public static List<Document> getResults(Query query, boolean analyzeQuery, Sort sort,Filter filter){
		String path = ResourceBundle.getBundle("index").getString("index");
		File indexDirPath = new File(path);
		return getResults(query, indexDirPath, analyzeQuery, sort, filter);
	}
	
	/**
	 * Pronalazi dokumente koji zavodoljavaju upit. Dokumenata ima najvise
	 * 
	 * @param query - upit po kojem ce biti trazeni dokumenti
	 * @param analyzeQuery - da li je potrebno analizirati upit.<p>Za vrednost:<br/> - <i>true</i> -> upit ce biti analiziran postojecim analyzer-om<br> - <i>false</i> -> upit nece biti analiziran<p><b>Napomena:</b> podrazumevani analyzer je {@link SerbianAnalyzer}. Za promenu analyzer-a koristiti metodu {@link #setAnalyzer(Analyzer)} 
	 * @param sort - instanca klase {@link Sort} koja ce biti upotrebljena da sortira dokumente. Ukoliko je prosledjena <b>null</b> vrednost, dokumenti ce biti u redosledu pronalazenja u indeksu
	 * @return Sortirana lista dokumenata koji odgovaraju (analiziranom) upitu.<br> - praznu listu ukoliko ni jedan dokument ne odgovara upitu<br> - <b>null</b> ukoliko je prosledjeni upit null
	 * @throws IllegalArgumentException ako se u direktorijumu ne nalaze indeksi ili je direktorijum zakljucan
	 * @throws ParseException ako upit nije moguce parsirati
	 */
	public static List<Document> getResults(Query query, File indexDirPath, boolean analyzeQuery, Sort sort, Filter filter){
		if(query == null){
			return null;
		}
		try {
			if(analyzeQuery){
				query = queryParser.parse(query.toString());
			}
			Directory indexDir = new SimpleFSDirectory(indexDirPath);
			DirectoryReader reader = DirectoryReader.open(indexDir);
			IndexSearcher is = new IndexSearcher(reader);
			
			List<Document> docs = new ArrayList<Document>();
			if(sort == null){
				sort = Sort.INDEXORDER;
			}
			ScoreDoc[] scoreDocs;
			if(filter == null)
				scoreDocs = is.search(query, maxHits, sort).scoreDocs;
			else 
				scoreDocs = is.search(query, filter, maxHits, sort).scoreDocs;
			
			for(ScoreDoc sd : scoreDocs){
				docs.add(is.doc(sd.doc));
			}
			return docs;
		} catch (ParseException e) {
			throw new IllegalArgumentException("Upit nije moguce parsirati");
		} catch (IOException e) {
			throw new IllegalArgumentException("U prosledjenom direktorijumu ne postoje indeksi ili je direktorijum zakljucan");
		}
	}

}
