package udd.rest.mvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.BytesRef;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import udd.core.models.entities.Account;
import udd.core.services.AccountService;
import udd.model.DocumentModel;
import udd.model.RequiredHighlight;
import udd.model.SearchModel;
import udd.model.SearchType;
import udd.tools.indexer.IndexManager;
import udd.tools.query.QueryBuilder;
import udd.tools.searcher.InformationRetriever;
import udd.tools.searcher.ResultRetriever;
import udd.util.DocumentList;
import udd.util.SearchResultModel;


@Controller
@RequestMapping("/rest/search")
public class SearchController {
	
	@Autowired
	ServletContext servletContext;
	
	private AccountService accountService;
	
	@Autowired
	public SearchController(AccountService accountService) {
		this.accountService = accountService;
	}
	
	@RequestMapping(value="/searchSimple/{searchTerm}",method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public ResponseEntity<SearchResultModel> simpleSearch(@PathVariable String searchTerm) throws IllegalArgumentException, ParseException, IOException {
		
		SearchResultModel resultModel = new SearchResultModel();
		Query queryText = QueryBuilder.buildQuery(SearchType.REGULAR, "text", searchTerm.trim());
		Query queryAuthor = QueryBuilder.buildQuery(SearchType.REGULAR, "author", searchTerm.trim());
		Query queryTitle = QueryBuilder.buildQuery(SearchType.REGULAR, "title", searchTerm.trim());
		
		List<RequiredHighlight> rhs = new ArrayList<RequiredHighlight>();
		List<RequiredHighlight> suggestions = new ArrayList<RequiredHighlight>();
		BooleanQuery bquery = new BooleanQuery();
		RequiredHighlight textrh = null;
		RequiredHighlight authorrh = null;
		RequiredHighlight titlerh = null;
		
		if(!(searchTerm == null || searchTerm.equals(""))) {
			
			bquery.add(queryText,Occur.SHOULD);
			bquery.add(queryAuthor,Occur.SHOULD);
			bquery.add(queryTitle,Occur.SHOULD);
			
			textrh = new RequiredHighlight("text", searchTerm.trim(), null);
			authorrh = new RequiredHighlight("author", searchTerm.trim(), null);
			titlerh = new RequiredHighlight("title", searchTerm.trim(), null);
			
			rhs.add(textrh);
			rhs.add(authorrh);
			rhs.add(titlerh);

			if(textrh != null) {		
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(textrh.getFieldName(),textrh.getValue()),servletContext);
				
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+searchTerm+"&author="+searchTerm+"&text="+rhl.getValue();
					rhl.setSearchLink(searchLink);
				}
				
				suggestions.addAll(list);		
			}
			
			if(authorrh != null) {		
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(authorrh.getFieldName(),authorrh.getValue()),servletContext);
				
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+searchTerm+"&author="+rhl.getValue()+"&text="+searchTerm;
					rhl.setSearchLink(searchLink);
				}
				
				suggestions.addAll(list);		
			}
			
			if(titlerh != null) {		
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(titlerh.getFieldName(),titlerh.getValue()),servletContext);
				
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+rhl.getValue()+"&author="+searchTerm+"&text="+searchTerm;
					rhl.setSearchLink(searchLink);
				}
				
				suggestions.addAll(list);		
			}
		}
		
		TermFilter filter = new TermFilter(new Term("status","ACTIVE"));
		List<DocumentModel> resultDocsAllPublished = InformationRetriever.getData(bquery, rhs, filter);		
		
		List<DocumentModel> resultDocsAllRemoved = new ArrayList<DocumentModel>();		
		
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Account loggedIn = null;
		if(principal != null) {
	        if(principal instanceof UserDetails) {
	            UserDetails details = (UserDetails)principal;
	            loggedIn = accountService.findByAccountName(details.getUsername());
	        } 
		}
		
		 if(loggedIn == null) { //obican korisnik
	          //nema pristup uklonjenim (removed) dokumentima      	
	        }
	        else {
	        	//ako je novinar
	        	if(!loggedIn.getRole().equals("Editor")) {
	        		
	        		TermsFilter statusFilter2 = new TermsFilter(new Term("status", "REMOVED"),new Term("author", loggedIn.getFirstName() + " " + loggedIn.getLastName()));	   
	        		resultDocsAllRemoved = InformationRetriever.getData(bquery, rhs, statusFilter2);
	        	}
	        	//ako je editor
	        	else {
	        		TermsFilter statusFilter2 = new TermsFilter(new Term("status", "REMOVED"));
	        		resultDocsAllRemoved = InformationRetriever.getData(bquery, rhs, statusFilter2);
	        	}
	     }
					
		List<DocumentModel> resultDocs = new ArrayList<DocumentModel>();
		List<Document> docsFilteredByDate = getAllDocsAvaibleForThisUser();
			
		//samo ako je dokument dostupan datom korsniku dodaj u listu rezultata pretrage
		for(DocumentModel dm : resultDocsAllPublished)
			for(Document doc : docsFilteredByDate)
				if(doc.get("id").equals(dm.getUid()))
					resultDocs.add(dm);
		
		if(resultDocsAllRemoved != null)
			resultDocs.addAll(resultDocsAllRemoved);
				
		resultModel.setDocuments(resultDocs);
		resultModel.setSuggestions(suggestions);
		
		return new ResponseEntity<SearchResultModel>(resultModel, HttpStatus.OK);
		
	
	}
	
	@RequestMapping(value="/searchAdvanced",method = RequestMethod.POST)
    @PreAuthorize("permitAll")
    public ResponseEntity<SearchResultModel> advancedSearch(@RequestBody SearchModel searchModel) throws IOException {
		
		String text = searchModel.getText();
		String textst = searchModel.getTextSearchType();
		SearchType.Type textSearchType = SearchType.getType(textst);
		String textsc = searchModel.getTextSearchCondition();
		Occur textOccur = null;
		if(textsc != null)
			textOccur = getOccur(textsc);
		
		String title = searchModel.getTitle();
		String titlest = searchModel.getTitleSearchType();
		SearchType.Type titleSearchType = SearchType.getType(titlest);
		String titlesc = searchModel.getTitleSearchCondition();
		Occur titleOccur = null;
		if(titlesc != null)
			titleOccur = getOccur(titlesc);
		
		String author = searchModel.getAuthor();
		String authorst = searchModel.getAuthorSearchType();
		SearchType.Type authorSearchType = SearchType.getType(authorst);
		String authorsc = searchModel.getAuthorSearchCondition();
		Occur authorOccur = null;
		if(authorsc != null)
			authorOccur = getOccur(authorsc);
		
		String publishDate = "";
		if(searchModel.getPublishingDate() != null) {
			String publishDateTemp = searchModel.getPublishingDate().trim();
			String[] tokens = publishDateTemp.split(" ");
			if(tokens.length > 1) {
				String[] tokens2 = tokens[0].split("/");
				String[] tokens3 = tokens[1].split("/");
				publishDate = tokens2[2] + tokens2[1] + tokens2[0] + " " + tokens3[2] + tokens3[1] + tokens3[0];
			}
			else {
				String[] tokens2 = tokens[0].split("/");
				if(tokens2.length == 3)
				publishDate = tokens2[2] + tokens2[1] + tokens2[0];
			}
		}
		
		String publishDatest = searchModel.getDateSearchType();
		SearchType.Type publishDateSearchType = SearchType.getType(publishDatest);
		String publishDatesc = searchModel.getDateSearchCondition();
		Occur publishDateOccur = null;
		if(publishDatesc != null)
			publishDateOccur = getOccur(publishDatesc);
		
		String apstract = searchModel.getApstract();
		String apstractst = searchModel.getApstractSearchType();
		SearchType.Type apstractSearchType = SearchType.getType(apstractst);
		String apstractsc = searchModel.getApstractSearchCondition();
		Occur apstractOccur = null;
		if(apstractsc != null)
			apstractOccur = getOccur(apstractsc);
		
		String category = searchModel.getCategory();
		String categoryst = searchModel.getCategorySearchType();
		SearchType.Type categorySearchType = SearchType.getType(categoryst);
		String categorysc = searchModel.getCategorySearchCondition();
		Occur categoryOccur = null;
		if(categorysc != null)
			categoryOccur = getOccur(categorysc);
    				
		SearchResultModel resultModel = new SearchResultModel();
		
		try {
			BooleanQuery bquery = new BooleanQuery();
			List<RequiredHighlight> rhs = new ArrayList<RequiredHighlight>();
			RequiredHighlight titlerh = null;
			RequiredHighlight textrh = null;
			RequiredHighlight authorrh = null;
			RequiredHighlight apstractrh = null;
			RequiredHighlight categoryrh = null;
			RequiredHighlight publishDaterh = null;
			
			
			if(!(title == null || title.equals(""))){
				Query query = QueryBuilder.buildQuery(titleSearchType, "title", title);
				bquery.add(query, titleOccur);
				titlerh = new RequiredHighlight("title", title, null);
				rhs.add(titlerh);
			}
			
			if(!(author == null || author.equals(""))){
				Query query = QueryBuilder.buildQuery(authorSearchType, "author", author);
				bquery.add(query, authorOccur);
				authorrh = new RequiredHighlight("author", author, null);
				rhs.add(authorrh);
			}
			
			if(!(apstract == null || apstract.equals(""))){
				Query query = QueryBuilder.buildQuery(apstractSearchType, "apstract", apstract);
				bquery.add(query, apstractOccur);
				apstractrh = new RequiredHighlight("apstract", apstract, null);
				rhs.add(apstractrh);
			}
			
			if(!(text == null || text.equals(""))){
				Query query = QueryBuilder.buildQuery(textSearchType, "text", text);
				bquery.add(query, textOccur);
				textrh = new RequiredHighlight("text", text, null);
				rhs.add(textrh);
			}
			
			if(!(publishDate == null || publishDate.equals(""))){
				Query query = QueryBuilder.buildQuery(publishDateSearchType, "publishingDate", publishDate);
				bquery.add(query, publishDateOccur);
				publishDaterh = new RequiredHighlight("publishingDate", publishDate, null);
				rhs.add(publishDaterh);
			}
			
			if(!(category == null || category.equals(""))){
				Query query = QueryBuilder.buildQuery(categorySearchType, "category", category);
				bquery.add(query, categoryOccur);
				categoryrh = new RequiredHighlight("category", category, null);
				rhs.add(categoryrh);
			}
			
			List<RequiredHighlight> suggestions = new ArrayList<RequiredHighlight>();
			if(titlerh != null){
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(titlerh.getFieldName(),titlerh.getValue()),servletContext);
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+rhl.getValue()+"&titlesc="+titlesc+"&titlest="+titlest
							+"&author="+author+"&authorst="+authorst+"&authorsc="+authorsc
							+"&apstract="+apstract+"&apstractst="+apstractst+"&apstractsc="+apstractsc
							+"&text="+text+"&textst="+textst+"&textsc="+textsc
							+"&category="+category+"&categoryst="+categoryst+"&categorysc="+categorysc
							+"&publishDate="+publishDate+"&publishDatest="+publishDatest+"&publishDatesc="+publishDatesc;
					rhl.setSearchLink(searchLink);
				}
				
				suggestions.addAll(list);
			}
			
			if(textrh != null){
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(textrh.getFieldName(),textrh.getValue()),servletContext);
				
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+title+"&titlesc="+titlesc+"&titlest="+titlest
							+"&author="+author+"&authorst="+authorst+"&authorsc="+authorsc
							+"&apstract="+apstract+"&apstractst="+apstractst+"&apstractsc="+apstractsc
							+"&text="+rhl.getValue()+"&textst="+textst+"&textsc="+textsc
							+"&category="+category+"&categoryst="+categoryst+"&categorysc="+categorysc
							+"&publishDate="+publishDate+"&publishDatest="+publishDatest+"&publishDatesc="+publishDatesc;
					rhl.setSearchLink(searchLink);
				}
				
				suggestions.addAll(list);
			}
			
			if(authorrh != null){
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(authorrh.getFieldName(),authorrh.getValue()),servletContext);
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+title+"&titlesc="+titlesc+"&titlest="+titlest
							+"&author="+rhl.getValue()+"&authorst="+authorst+"&authorsc="+authorsc
							+"&apstract="+apstract+"&apstractst="+apstractst+"&apstractsc="+apstractsc
							+"&text="+text+"&textst="+textst+"&textsc="+textsc
							+"&category="+category+"&categoryst="+categoryst+"&categorysc="+categorysc
							+"&publishDate="+publishDate+"&publishDatest="+publishDatest+"&publishDatesc="+publishDatesc;
					rhl.setSearchLink(searchLink);
				}
				suggestions.addAll(list);
			}
			
			if(categoryrh != null){
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(categoryrh.getFieldName(),categoryrh.getValue()),servletContext);
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+title+"&titlesc="+titlesc+"&titlest="+titlest
							+"&author="+author+"&authorst="+authorst+"&authorsc="+authorsc
							+"&apstract="+apstract+"&apstractst="+apstractst+"&apstractsc="+apstractsc
							+"&text="+text+"&textst="+textst+"&textsc="+textsc
							+"&category="+rhl.getValue()+"&categoryst="+categoryst+"&categorysc="+categorysc
							+"&publishDate="+publishDate+"&publishDatest="+publishDatest+"&publishDatesc="+publishDatesc;
					rhl.setSearchLink(searchLink);
				}
				suggestions.addAll(list);
			}
			
			if(publishDaterh != null){
				List<RequiredHighlight> list = InformationRetriever.getSuggestions(new Term(publishDaterh.getFieldName(),publishDaterh.getValue()),servletContext);
				String searchLink = "";
				for(RequiredHighlight rhl : list){
					searchLink = "Search?title="+title+"&titlesc="+titlesc+"&titlest="+titlest
							+"&author="+author+"&authorst="+authorst+"&authorsc="+authorsc
							+"&apstract="+apstract+"&apstractst="+apstractst+"&apstractsc="+apstractsc
							+"&text="+text+"&textst="+textst+"&textsc="+textsc
							+"&category="+category+"&categoryst="+categoryst+"&categorysc="+categorysc
							+"&publishDate="+rhl.getValue()+"&publishDatest="+publishDatest+"&publishDatesc="+publishDatesc;
					rhl.setSearchLink(searchLink);
				}
				suggestions.addAll(list);
			}	
			
			TermFilter filter = new TermFilter(new Term("status","ACTIVE"));
			
			List<DocumentModel> resultDocsAllPublished = InformationRetriever.getData(bquery, rhs, filter);
			List<DocumentModel> resultDocs = new ArrayList<DocumentModel>();
			List<DocumentModel> resultDocsAllRemoved = searchForRemovedDocs(bquery, rhs);
			List<Document> docsFilteredByDate = getAllDocsAvaibleForThisUser();
			
			//samo ako je dokument dostupan datom korsniku dodaj u listu rezultata pretrage
			for(DocumentModel dm : resultDocsAllPublished)
				for(Document doc : docsFilteredByDate)
					if(doc.get("id").equals(dm.getUid()))
						resultDocs.add(dm);
			
			if(resultDocsAllRemoved != null)
				resultDocs.addAll(resultDocsAllRemoved);
			
			resultModel.setDocuments(resultDocs);
			resultModel.setSuggestions(suggestions);
			
		} catch (IllegalArgumentException e) {
			
		} catch (ParseException e) {
			
		}
        return new ResponseEntity<SearchResultModel>(resultModel, HttpStatus.OK);
    }
		
	@RequestMapping(value="/getMoreLikeThis/{uid}",method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public ResponseEntity<DocumentList> getMoreLikeThis(@PathVariable String uid) throws FileNotFoundException, IOException {
		
		String realPath = servletContext.getRealPath("/docs");
		
		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();
		
		List<DocumentModel> moreLikeThisDocs = new ArrayList<DocumentModel>();
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		    	
		    	PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();			
				PDDocument pdf = parser.getPDDocument();					
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");
				
		    	if(id.equals(uid)) {  
		    		moreLikeThisDocs = InformationRetriever.getMoreLikeThis(file.getName(), realPath);
		    		
		    		for(DocumentModel dm : moreLikeThisDocs) {
		    			if(dm.getUid().equals(id)) {
		    				moreLikeThisDocs.remove(dm);
		    				break;
		    			}
		    		}
		    		
		    	}
		    	
		    	pdf.close();
		    }
		}
		
		List<DocumentModel> moreLikeThisFinal = new ArrayList<DocumentModel>();
		List<Document> allAvaibleDocs = getAllDocsAvaibleForThisUser();
		for(DocumentModel dm : moreLikeThisDocs)
			for(Document doc : allAvaibleDocs)
				if(doc.get("id").equals(dm.getUid()))
					moreLikeThisFinal.add(dm);
			
		DocumentList dl = new DocumentList();
		dl.setDocuments(moreLikeThisFinal);
        return new ResponseEntity<DocumentList>(dl, HttpStatus.OK);
	
	}
	
	private Occur getOccur(String value) {
		if(value.equals("must")){
			return Occur.MUST;
		} else if(value.equals("mustNot")) {
			return Occur.MUST_NOT;
		} else {
			return Occur.SHOULD;
		}
	}
	
	private List<Document> getAllDocsAvaibleForThisUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Account loggedIn = null;
		if(principal != null) {
	        if(principal instanceof UserDetails) {
	            UserDetails details = (UserDetails)principal;
	            loggedIn = accountService.findByAccountName(details.getUsername());
	        } 
		}
        		
		Date highDate = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		Date lowDate = cal.getTime(); // datum pre sedam dana
		List<Document> docsFilteredByDate;
		List<Document> journalistSpecificDocs = new ArrayList<Document>();
		List<Document> unpublished = new ArrayList<Document>();
		
        if(loggedIn == null) { //obican korisnik
            docsFilteredByDate = ResultRetriever.getResults(new TermRangeQuery(
    			    "publishingDate",
    			    new BytesRef(DateTools.dateToString(lowDate, DateTools.Resolution.MINUTE)),
    			    new BytesRef(DateTools.dateToString(highDate, DateTools.Resolution.MINUTE)),
    			    true,
    			    false), new Sort (new SortField("publishingDate",SortField.Type.STRING)), null);       	
        	
        }
        else {
        	Document[] allDocuments = IndexManager.getIndexer().getAllDocuments();
        	//ako je novinar
        	if(!loggedIn.getRole().equals("Editor")) {
        		docsFilteredByDate = ResultRetriever.getResults(new TermRangeQuery(
        			    "publishingDate",
        			    new BytesRef(DateTools.dateToString(lowDate, DateTools.Resolution.MINUTE)),
        			    new BytesRef(DateTools.dateToString(highDate, DateTools.Resolution.MINUTE)),
        			    true,
        			    false),new Sort (new SortField("publishingDate",SortField.Type.STRING)), null);
        		for(Document d : allDocuments)
        			if(d.get("author").equals(loggedIn.getFirstName() + " " + loggedIn.getLastName()))
        				journalistSpecificDocs.add(d);
        		for(Document d : allDocuments)
            		if(d.get("status").equals("NOT_ACTIVE") && d.get("author").equals(loggedIn.getFirstName() + " " + loggedIn.getLastName()))
            			unpublished.add(d);
        	}
        	//ako je editor
        	else {
        		Calendar cal2 = Calendar.getInstance();
        		cal2.add(Calendar.DATE, -5000);
        		Date lowDate2 = cal2.getTime();
        		docsFilteredByDate = ResultRetriever.getResults(new TermRangeQuery(
        			    "publishingDate",
        			    new BytesRef(DateTools.dateToString(lowDate2, DateTools.Resolution.MINUTE)),
        			    new BytesRef(DateTools.dateToString(highDate, DateTools.Resolution.MINUTE)),
        			    true,
        			    false),new Sort (new SortField("publishingDate",SortField.Type.STRING)), null);
        		for(Document d : allDocuments)
            		if(d.get("status").equals("NOT_ACTIVE"))
            			unpublished.add(d);
        	}
        }
        
        if(journalistSpecificDocs.size() != 0) {
	        for(int i=journalistSpecificDocs.size()-1; i>=0; i--) {
	        	boolean exists = false;
				if(journalistSpecificDocs.get(i).get("status").equals("ACTIVE")) {
					for(Document d : docsFilteredByDate) {
						if(d.get("id").equals(journalistSpecificDocs.get(i).get("id"))) {
							exists = true;
							break;
						}
					}
					
					if(!exists) docsFilteredByDate.add(journalistSpecificDocs.get(i));
				}
			}
        }
        
        //dodaj i nepublikovane u dostupne
        if(loggedIn != null) {
	        for(int i=unpublished.size()-1; i>=0; i--) {							
	        	docsFilteredByDate.add(unpublished.get(i));
	        }
        }
        
        return docsFilteredByDate;
	}
	
	private List<DocumentModel> searchForRemovedDocs(Query query, List<RequiredHighlight> requiredHighlights) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Account loggedIn = null;
		if(principal != null) {
	        if(principal instanceof UserDetails) {
	            UserDetails details = (UserDetails)principal;
	            loggedIn = accountService.findByAccountName(details.getUsername());
	        } 
		}
		
		 List<DocumentModel> resultDocsRemoved = null;
		 if(loggedIn == null) { //obican korisnik
	          //nema pristup uklonjenim (removed) dokumentima      	
	        }
	        else {
	        	//ako je novinar
	        	if(!loggedIn.getRole().equals("Editor")) {
	        		TermsFilter statusFilter = new TermsFilter(new Term("status", "REMOVED"),new Term("author", loggedIn.getFirstName() + " " + loggedIn.getLastName()));	   
	        		resultDocsRemoved = InformationRetriever.getData(query, requiredHighlights, statusFilter);
	        	}
	        	//ako je editor
	        	else {
	        		TermsFilter statusFilter = new TermsFilter(new Term("status", "REMOVED"));
	        		resultDocsRemoved = InformationRetriever.getData(query, requiredHighlights, statusFilter);
	        	}
	     }
		 
		return resultDocsRemoved;
		
	}
}
