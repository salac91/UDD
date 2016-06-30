package udd.rest.mvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import udd.core.models.entities.Account;
import udd.core.services.AccountService;
import udd.model.DocumentModel;
import udd.tools.searcher.ResultRetriever;
import udd.tools.indexer.IndexManager;
import udd.util.DocumentList;
import udd.util.GetImagesFromPDF;
import udd.util.SendMail;

@Controller
@RequestMapping("/rest/documents")
public class DocumentController {

	@Autowired
	ServletContext servletContext;

	private AccountService accountService;

	@Autowired
	public DocumentController(AccountService accountService) {
		this.accountService = accountService;
	}

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("permitAll")
	public ResponseEntity<DocumentList> getAllDocs() throws ParseException,
			FileNotFoundException, IOException {

		List<DocumentModel> allDocs = new ArrayList<DocumentModel>();

		Object principal = SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal();
		Account loggedIn = null;
		if (principal != null) {
			if (principal instanceof UserDetails) {
				UserDetails details = (UserDetails) principal;
				loggedIn = accountService.findByAccountName(details
						.getUsername());
			}
		}

		Date highDate = new Date();

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		Date lowDate = cal.getTime(); // datum pre sedam dana
		List<Document> docsFilteredByDate;
		List<Document> journalistSpecificDocs = new ArrayList<Document>();
		List<Document> unpublished = new ArrayList<Document>();

		if (loggedIn == null) { // obican korisnik
			docsFilteredByDate = ResultRetriever
					.getResults(
							new TermRangeQuery("publishingDate", new BytesRef(
									DateTools.dateToString(lowDate,
											DateTools.Resolution.MINUTE)),
									new BytesRef(DateTools.dateToString(
											highDate,
											DateTools.Resolution.MINUTE)),
									true, false), new Sort(new SortField(
									"publishingDate", SortField.Type.STRING)),
							null);
		} else {
			Document[] allDocuments = IndexManager.getIndexer()
					.getAllDocuments();
			// ako je novinar
			if (!loggedIn.getRole().equals("Editor")) {
				docsFilteredByDate = ResultRetriever
						.getResults(
								new TermRangeQuery("publishingDate",
										new BytesRef(DateTools.dateToString(
												lowDate,
												DateTools.Resolution.MINUTE)),
										new BytesRef(DateTools.dateToString(
												highDate,
												DateTools.Resolution.MINUTE)),
										true, false), new Sort(
										new SortField("publishingDate",
												SortField.Type.STRING)), null);
				for (Document d : allDocuments)
					if (d.get("author").equals(
							loggedIn.getFirstName() + " "
									+ loggedIn.getLastName()))
						journalistSpecificDocs.add(d);
				for (Document d : allDocuments)
					if (d.get("status").equals("NOT_ACTIVE")
							&& d.get("author").equals(
									loggedIn.getFirstName() + " "
											+ loggedIn.getLastName()))
						unpublished.add(d);
			}
			// ako je editor
			else {
				Calendar cal2 = Calendar.getInstance();
				cal2.add(Calendar.DATE, -5000);
				Date lowDate2 = cal2.getTime();
				docsFilteredByDate = ResultRetriever
						.getResults(
								new TermRangeQuery("publishingDate",
										new BytesRef(DateTools.dateToString(
												lowDate2,
												DateTools.Resolution.MINUTE)),
										new BytesRef(DateTools.dateToString(
												highDate,
												DateTools.Resolution.MINUTE)),
										true, false), new Sort(
										new SortField("publishingDate",
												SortField.Type.STRING)), null);
			}
		}

		for (int i = docsFilteredByDate.size() - 1; i >= 0; i--) {
			DocumentModel dm = new DocumentModel();
			dm.setUid(docsFilteredByDate.get(i).get("id"));
			dm.setTitle(docsFilteredByDate.get(i).get("title"));
			dm.setApstract(docsFilteredByDate.get(i).get("apstract"));
			dm.setPublishingDate(docsFilteredByDate.get(i)
					.get("publishingDate"));
			dm.setCategory(docsFilteredByDate.get(i).get("category"));
			dm.setAuthor(docsFilteredByDate.get(i).get("author"));
			dm.setStatus(docsFilteredByDate.get(i).get("status"));
			dm.setText(docsFilteredByDate.get(i).get("text"));

			if (docsFilteredByDate.get(i).get("status").equals("ACTIVE"))
				allDocs.add(dm);
		}

		if (loggedIn != null) {
			for (int i = unpublished.size() - 1; i >= 0; i--) {
				DocumentModel dm = new DocumentModel();
				dm.setUid(unpublished.get(i).get("id"));
				dm.setTitle(unpublished.get(i).get("title"));
				dm.setApstract(unpublished.get(i).get("apstract"));
				dm.setPublishingDate(unpublished.get(i).get("publishingDate"));
				dm.setCategory(unpublished.get(i).get("category"));
				dm.setAuthor(unpublished.get(i).get("author"));
				dm.setStatus(unpublished.get(i).get("status"));
				dm.setText(unpublished.get(i).get("text"));

				allDocs.add(dm);
			}
		}

		if (journalistSpecificDocs.size() != 0) {
			for (int i = journalistSpecificDocs.size() - 1; i >= 0; i--) {
				DocumentModel dm = new DocumentModel();
				dm.setUid(journalistSpecificDocs.get(i).get("id"));
				dm.setTitle(journalistSpecificDocs.get(i).get("title"));
				dm.setApstract(journalistSpecificDocs.get(i).get("apstract"));
				dm.setPublishingDate(journalistSpecificDocs.get(i).get(
						"publishingDate"));
				dm.setCategory(journalistSpecificDocs.get(i).get("category"));
				dm.setAuthor(journalistSpecificDocs.get(i).get("author"));
				dm.setStatus(journalistSpecificDocs.get(i).get("status"));
				dm.setText(journalistSpecificDocs.get(i).get("text"));

				boolean exists = false;
				if (journalistSpecificDocs.get(i).get("status")
						.equals("ACTIVE")) {
					for (DocumentModel d : allDocs) {
						if (d.getUid().equals(dm.getUid())) {
							exists = true;
							break;
						}
					}

					if (!exists)
						allDocs.add(dm);
				}
			}
		}

		DocumentList dl = new DocumentList();
		dl.setDocuments(allDocs);
		return new ResponseEntity<DocumentList>(dl, HttpStatus.OK);
	}

	@RequestMapping(value = "/unpublished", method = RequestMethod.GET)
	@PreAuthorize("hasRole('Editor')")
	public ResponseEntity<DocumentList> getAllUnpublishedDocs()
			throws FileNotFoundException, IOException {

		List<DocumentModel> allUnpublishedDocs = new ArrayList<DocumentModel>();

		String realPath = servletContext.getRealPath("/docs");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				DocumentModel dm = new DocumentModel();

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();

				String status = info.getCustomMetadataValue("status");

				if (status.equals("NOT_ACTIVE")) {
					String uid = info.getCustomMetadataValue("id");
					String title = info.getTitle();
					String apstract = info.getCustomMetadataValue("apstract");
					String text = info.getCustomMetadataValue("text");
					String author = info.getAuthor();

					dm.setUid(uid);
					dm.setTitle(title);
					dm.setApstract(apstract);
					dm.setText(text);
					dm.setAuthor(author);
					dm.setFileName(file.getName());

					allUnpublishedDocs.add(dm);
				}
				pdf.close();
			}
		}

		DocumentList dl = new DocumentList();
		dl.setDocuments(allUnpublishedDocs);
		return new ResponseEntity<DocumentList>(dl, HttpStatus.OK);
	}

	@RequestMapping(value = "/unpublishedByAuthor", method = RequestMethod.GET)
	@PreAuthorize("hasRole('Journalist')")
	public ResponseEntity<DocumentList> getUnpublishedByAuthorDocs()
			throws FileNotFoundException, IOException {

		List<DocumentModel> allUnpublishedDocs = new ArrayList<DocumentModel>();

		String realPath = servletContext.getRealPath("/docs");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		Object principal = SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal();
		Account loggedIn = null;
		if (principal instanceof UserDetails) {
			UserDetails details = (UserDetails) principal;
			loggedIn = accountService.findByAccountName(details.getUsername());
		}

		for (File file : listOfFiles) {
			if (file.isFile()) {
				DocumentModel dm = new DocumentModel();

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();

				String status = info.getCustomMetadataValue("status");

				if (status.equals("NOT_ACTIVE")) {
					String uid = info.getCustomMetadataValue("id");
					String title = info.getTitle();
					String apstract = info.getCustomMetadataValue("apstract");
					String author = info.getAuthor();
					String categories = info.getCustomMetadataValue("category");
					String tags = info.getCustomMetadataValue("tags");
					String keywords = info.getKeywords();
					Document[] docs = IndexManager.getIndexer()
							.getAllDocuments();
					String text = "";
					for (Document doc : docs) {
						if (doc.get("id").equals(uid)) {
							text = doc.get("text");
							break;
						}
					}

					dm.setUid(uid);
					dm.setTitle(title);
					dm.setApstract(apstract);
					dm.setKeyWords(keywords);
					dm.setTags(tags);
					dm.setCategory(categories);
					dm.setFileName(file.getName());
					dm.setText(text);

					if (loggedIn != null) {
						String name = loggedIn.getFirstName() + " "
								+ loggedIn.getLastName();
						if (author.equals(name))
							allUnpublishedDocs.add(dm);
					}
				}
				pdf.close();
			}
		}

		DocumentList dl = new DocumentList();
		dl.setDocuments(allUnpublishedDocs);
		return new ResponseEntity<DocumentList>(dl, HttpStatus.OK);
	}

	@RequestMapping(value = "/newDocument", method = RequestMethod.POST)
	@PreAuthorize("hasRole('Journalist')")
	public ResponseEntity<?> UploadFile(
			@RequestParam(value = "file", required = true) MultipartFile file)
			throws MalformedURLException, COSVisitorException {
		String fileName = file.getOriginalFilename();

		String realPath = servletContext.getRealPath("/docs");
		String realPathSpellchecker = servletContext
				.getRealPath("/spellchecker");
		
		Object principal = SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal();
		Account loggedIn = null;
		if (principal instanceof UserDetails) {
			UserDetails details = (UserDetails) principal;
			loggedIn = accountService.findByAccountName(details.getUsername());
		}

		File destFile = new File(realPath + "/" + fileName);
		try {

			file.transferTo(destFile);

			PDFParser parser = new PDFParser(new FileInputStream(destFile));
			parser.parse();
			PDDocument pdf = parser.getPDDocument();
			PDDocumentInformation info = pdf.getDocumentInformation();
			info.setAuthor(loggedIn.getFirstName() + " "
					+ loggedIn.getLastName());
			info.setCustomMetadataValue("id", "" + System.currentTimeMillis());
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			Date date = new Date();
			info.setCustomMetadataValue("creationDate", dateFormat.format(date));
			info.setCustomMetadataValue("status", "NOT_ACTIVE");
			info.setCustomMetadataValue("location",
					realPath + "/" + file.getName());
			info.setCustomMetadataValue("apstract", "");
			info.setCustomMetadataValue("publishingDate", "");
			info.setCustomMetadataValue("category", "");
			info.setCustomMetadataValue("tags", "");
			pdf.setDocumentInformation(info);
			pdf.save(destFile);
			pdf.close();

			IndexManager.getIndexer().index(destFile);
			
			// kopiraj dokument u  spellchecker dir
			InputStream inStream = null;
			OutputStream outStream = null;

			try {

				File afile = destFile;
				File cfile = new File(realPathSpellchecker + "/"
						+ destFile.getName());

				inStream = new FileInputStream(afile);
				outStream = new FileOutputStream(cfile);

				byte[] buffer = new byte[1024];
				int length;

				while ((length = inStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, length);
				}

				inStream.close();
				outStream.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<DocumentList>(HttpStatus.OK);
	}

	@RequestMapping(value = "/publishDocument/{uid}", method = RequestMethod.POST)
	@PreAuthorize("hasRole('Editor')")
	public @ResponseBody
	String publishDocument(@PathVariable String uid)
			throws FileNotFoundException, IOException, COSVisitorException {

		String realPath = servletContext.getRealPath("/docs");
		String realPathSpellchecker = servletContext
				.getRealPath("/spellchecker");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		String message = "";
		for (File file : listOfFiles) {
			if (file.isFile()) {

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");
				String status = info.getCustomMetadataValue("status");

				if (uid.equals(id) && status.equals("NOT_ACTIVE")) {
					String error = "";

					String title = info.getTitle();
					if (title == null || title.trim().equals("")) {
						error += "missing title ";
					}

					String categories = info.getCustomMetadataValue("category");
					if (categories == null || categories.trim().equals("")) {
						error += "missing categories ";
					}

					String keywords = info.getKeywords();
					if (keywords == null || keywords.trim().equals("")) {
						error += "missing keywords ";
					}

					String apstract = info.getCustomMetadataValue("apstract");
					if (apstract == null || apstract.trim().equals("")) {
						error += "missing apstract ";
					}

					String tags = info.getCustomMetadataValue("tags");
					if (tags == null || tags.trim().equals("")) {
						error += "missing tags ";
					}

					PDFTextStripper stripper = new PDFTextStripper("utf-8");
					String text = stripper.getText(pdf);
					if (text == null || text.trim().equals("")) {
						error += "missing text ";
					}

					if (error.equals("")) {
						// dokument je uredu postavi i ostale meta podatke
						message = "ok";
						Date date = new Date();
						info.setCustomMetadataValue("publishingDate", DateTools
								.dateToString(date, DateTools.Resolution.DAY));
						Object principal = SecurityContextHolder.getContext()
								.getAuthentication().getPrincipal();
						Account loggedIn = null;
						if (principal instanceof UserDetails) {
							UserDetails details = (UserDetails) principal;
							loggedIn = accountService.findByAccountName(details
									.getUsername());
						}
						info.setCustomMetadataValue(
								"editor",
								loggedIn.getFirstName() + " "
										+ loggedIn.getLastName());
						info.setCustomMetadataValue("location", realPath + "/"
								+ file.getName());
						info.setCustomMetadataValue("status", "ACTIVE");
						pdf.setDocumentInformation(info);
						pdf.save(file);

						// indeksiraj ponovo dokument
						Document[] docs = IndexManager.getIndexer()
								.getAllDocuments();
						for (Document doc : docs) {
							if (doc.get("id").equals(id)) {
								IndexManager.getIndexer().deleteDocument(doc);
								IndexManager.getIndexer().index(file);
								break;
							}
						}

						// kopiraj dokument u  spellchecker dir
						InputStream inStream = null;
						OutputStream outStream = null;

						try {

							File afile = file;
							File cfile = new File(realPathSpellchecker + "/"
									+ file.getName());

							inStream = new FileInputStream(afile);
							outStream = new FileOutputStream(cfile);

							byte[] buffer = new byte[1024];
							int length;

							while ((length = inStream.read(buffer)) > 0) {
								outStream.write(buffer, 0, length);
							}

							inStream.close();
							outStream.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					} else { // dokument nije kompletan, obavesti autora
						message = "missing";
						String author = info.getAuthor();
						String[] tokens = author.split(" ");
						String firstName = tokens[0];
						String lastName = tokens[1];
						@SuppressWarnings("resource")
						ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
								"spring/business-config.xml");
						SendMail mm = (SendMail) context.getBean("sendMail");
						Account account = accountService
								.findByFirstAndLastName(firstName, lastName);

						String titleMissing = "";
						if (error.contains("title"))
							titleMissing = " title";
						String apstractMissing = "";
						if (error.contains("title"))
							apstractMissing = " apstract";
						String categoriesMissing = "";
						if (error.contains("categories"))
							categoriesMissing = " categories";
						String tagsMissing = "";
						if (error.contains("tags"))
							tagsMissing = " tags";
						String keywordsMissing = "";
						if (error.contains("keywords"))
							keywordsMissing = " keywords";
						String textMissing = "";
						if (error.contains("text"))
							textMissing = " text";
						mm.sendMail(
								"uddeditor@gmail.com",
								account.getName(),
								"Unfinished document informations!",
								"Hello "
										+ account.getFirstName()
										+ " "
										+ account.getLastName()
										+ "!\nPlease enter next missing metadata for your document "
										+ file.getName() + ":" + titleMissing
										+ apstractMissing + categoriesMissing
										+ tagsMissing + keywordsMissing
										+ textMissing + ".");
					}

					pdf.close();
				}
			}
		}
		return message;
	}

	@RequestMapping(value = "/removeDocument/{uid}", method = RequestMethod.POST)
	@PreAuthorize("hasRole('Editor')")
	public ResponseEntity<DocumentList> removeDocument(@PathVariable String uid)
			throws FileNotFoundException, IOException, COSVisitorException {

		String realPath = servletContext.getRealPath("/docs");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");

				if (uid.equals(id)) {
					info.setCustomMetadataValue("status", "REMOVED");
					pdf.setDocumentInformation(info);
					pdf.save(file);

					Document[] docs = IndexManager.getIndexer()
							.getAllDocuments();
					for (Document doc : docs) {
						if (doc.get("id").equals(id)) {
							IndexManager.getIndexer().deleteDocument(doc);
							IndexManager.getIndexer().index(file);
							break;
						}
					}

				}

				pdf.close();

			}
		}

		List<DocumentModel> allDocs = new ArrayList<DocumentModel>();

		Document[] docs = IndexManager.getIndexer().getAllDocuments();
		for (Document document : docs) {
			DocumentModel dm = new DocumentModel();
			dm.setUid(document.get("id"));
			dm.setTitle(document.get("title"));
			dm.setApstract(document.get("apstract"));
			dm.setText(document.get("text"));

			if (document.get("status").equals("ACTIVE"))
				allDocs.add(dm);
		}
		DocumentList dl = new DocumentList();
		dl.setDocuments(allDocs);
		return new ResponseEntity<DocumentList>(dl, HttpStatus.OK);
	}

	@RequestMapping(value = "/updateDocument", method = RequestMethod.POST)
	@PreAuthorize("hasRole('Journalist')")
	public @ResponseBody
	void updateDocument(@RequestBody DocumentModel docModel)
			throws FileNotFoundException, IOException, COSVisitorException {

		String realPath = servletContext.getRealPath("/docs");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");

				if (docModel.getUid().equals(id)) {

					info.setCustomMetadataValue("apstract",
							docModel.getApstract());

					info.setCustomMetadataValue("category",
							docModel.getCategory());

					info.setKeywords(docModel.getKeyWords());

					info.setCustomMetadataValue("tags", docModel.getTags());

					info.setTitle(docModel.getTitle());
					
				}

				pdf.setDocumentInformation(info);
				pdf.save(file);
				pdf.close();
				
				Document[] docs = IndexManager.getIndexer()
						.getAllDocuments();
				for (Document doc : docs) {
					if (doc.get("id").equals(id)) {
						IndexManager.getIndexer().deleteDocument(doc);
						IndexManager.getIndexer().index(file);
						break;
					}
				}
			}
		}

	}

	@RequestMapping(value = "/updateText", method = RequestMethod.POST)
	@PreAuthorize("hasRole('Journalist')")
	public @ResponseBody
	void updateText(@RequestBody DocumentModel docModel) throws IOException {

		String realPath = servletContext.getRealPath("/docs");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");

				if (id.equals(docModel.getUid())) {
					Document[] docs = IndexManager.getIndexer()
							.getAllDocuments();
					for (Document d : docs) {
						if (d.get("id").equals(docModel.getUid()))
							IndexManager.getIndexer().updateDocument(
									d,
									new StringField("text", docModel.getText(),
											Store.YES));
					}
				}

				pdf.close();
			}
		}
	}

	@RequestMapping(value = "/getImages/{uid}", method = RequestMethod.GET)
	@PreAuthorize("permitAll")
	public @ResponseBody
	List<String> getImages(@PathVariable String uid)
			throws FileNotFoundException, IOException {

		String realPath = servletContext.getRealPath("/docs");
		// String imagesDir = servletContext.getRealPath("/images");

		File folder = new File(realPath);
		File[] listOfFiles = folder.listFiles();
		List<String> imagesPath = null;
		for (File file : listOfFiles) {
			if (file.isFile()) {

				PDFParser parser = new PDFParser(new FileInputStream(file));
				parser.parse();
				PDDocument pdf = parser.getPDDocument();
				PDDocumentInformation info = pdf.getDocumentInformation();
				String id = info.getCustomMetadataValue("id");

				if (uid.equals(id)) {
					imagesPath = GetImagesFromPDF.getImages(file);
					break;
				}
			}
		}
		return imagesPath;
	}
}
