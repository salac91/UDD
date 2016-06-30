package udd.tools.indexer.handler;

import java.io.File;
import java.io.FileInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

import udd.exceptions.IncompleteIndexDocumentException;

public class PDFHandler extends DocumentHandler {

	@Override
	public Document getDocument(File file) throws IncompleteIndexDocumentException {
		Document doc = new Document();
		//StringField id = new StringField("id", ""+System.currentTimeMillis(), Store.YES);
		TextField fileNameField = new TextField("fileName",	file.getName(), Store.YES);
		//doc.add(id);
		doc.add(fileNameField);
		@SuppressWarnings("unused")
		String error = "";
		try {
			StringField locationField = new StringField("location", file.getCanonicalPath(), Store.YES);
			doc.add(locationField);
			//napraviti pdf parser
			PDFParser parser = new PDFParser(new FileInputStream(file));
			//izvrsiti parsiranje
			parser.parse();
			
			//od parsera preuzeti parsirani pdf dokument (PDDocument)
			PDDocument pdf = parser.getPDDocument();
			
			//Upotrebiti text stripper klasu za ekstrahovanje teksta sa utf-8 kodnom stranom (PDFTextStripper)
			PDFTextStripper stripper = new PDFTextStripper("utf-8");
			String text = stripper.getText(pdf);
			if(text!=null && !text.trim().equals("")){
				doc.add(new TextField("text", text, Store.YES));
			}else{
				error += "Document without text\n";
			}
			
			//iz dokumenta izvuci objekat u kojem su svi metapodaci (PDDocumentInformation)
			PDDocumentInformation info = pdf.getDocumentInformation();
			
			String id = info.getCustomMetadataValue("id");
			if(id!=null && !id.trim().equals("")){
				doc.add(new StringField("id", id, Store.YES));
			}else{
				error += "Document without id\n";
			}
			
			String author = info.getAuthor();
			if(author!=null && !author.trim().equals("")){
				doc.add(new TextField("author", author, Store.YES));
			}else{
				error += "Document without author\n";
			}
			
			String apstract = info.getCustomMetadataValue("apstract");
			if(apstract!=null && !apstract.trim().equals("")){
				doc.add(new TextField("apstract", apstract, Store.YES));
			}else{
				error += "Document without apstract\n";
			}
			
			String title = info.getTitle();
			if(title!=null && !title.trim().equals("")){
				doc.add(new TextField("title", title, Store.YES));
			}else{
				error += "Document without title\n";
			}
			
			String publishingDate = info.getCustomMetadataValue("publishingDate");
			if(publishingDate!=null && !publishingDate.trim().equals("")){
				doc.add(new StringField("publishingDate", publishingDate, Store.YES));
			}else{
				error += "Document without publishing date\n";
			}
			
			String keywords = info.getKeywords();
			if(keywords==null){
				error += "Document without keywords\n";
			} else {
				
				String[] kws = keywords.trim().split(",");
				for(String kw : kws){
					if(!kw.trim().equals("")){
						doc.add(new TextField("keyword", kw, Store.YES));
					}
				}
			}
			
			String categories = info.getCustomMetadataValue("category");
			if(categories==null){
				error += "Document without categories\n";
			} else {
				
				String[] ctgs = categories.trim().split(",");
				for(String ctg : ctgs){
					if(!ctg.trim().equals("")){
						doc.add(new TextField("category", ctg, Store.YES));
					}
				}		
			}
			String tags = info.getCustomMetadataValue("tags");
			if(tags==null){
				error += "Document without tags\n";
			} else {
			
				String[] tgs = tags.trim().split(",");
				for(String tg : tgs){
					if(!tg.trim().equals("")){
						doc.add(new TextField("tags", tg, Store.YES));
					}
				}
			}
			String status = info.getCustomMetadataValue("status");						
			if(status!=null && !status.trim().equals("")){
				doc.add(new StringField("status", status, Store.YES));
			}else{
				error += "Document without status\n";
			}
			
			//zatvoriti pdf dokument
			pdf.close();
		} catch (Exception e) {
			System.out.println("Greska pri konvertovanju pdf dokumenta");
			error = "Document is incomplete. An exception occured";
		}
		
		/*
		if(!error.equals("")){
			throw new IncompleteIndexDocumentException(error.trim());
		}
		*/
		
		return doc;
	}

}
