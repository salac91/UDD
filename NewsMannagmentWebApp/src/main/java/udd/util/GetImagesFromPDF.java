package udd.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfImageObject;

public class GetImagesFromPDF {
	
    public static List<String> getImages(File oldFile) {
		List<String> allImagesPath = new ArrayList<String>();
		File dir = new File("src/main/webapp/app/build/images");
		if(!dir.exists())
			dir.mkdir();
		 try {    
	            if (oldFile.exists()) {

	            	 String fileName = oldFile.getName().replace(".pdf", "_cover");
	            	
	            	  PdfReader pr=new PdfReader(oldFile.toString());
	            	  PRStream pst;
	            	  PdfImageObject pio;
	            	  PdfObject po;
	            	  int n=pr.getXrefSize(); //number of objects in pdf document
	            	  for(int i=0;i<n;i++){
	            	   po=pr.getPdfObject(i); //get the object at the index i in the objects collection
	            	   if(po==null || !po.isStream()) //object not found so continue
	            	    continue;
	            	   pst=(PRStream)po; //cast object to stream
	            	   PdfObject type=pst.get(PdfName.SUBTYPE); //get the object type
	            	   //check if the object is the image type object
	            	   if(type!=null && type.toString().equals(PdfName.IMAGE.toString())){
	            	    pio=new PdfImageObject(pst); //get the image  
	            	    BufferedImage bi=pio.getBufferedImage(); //convert the image to buffered image
	            	    ImageIO.write(bi, "jpg", new File("src/main/webapp/app/build/images/" + fileName+i+".jpg")); //write the buffered image
	            	    //to local disk
	            	    
	            	    allImagesPath.add(fileName+i+".jpg");
	               }
	            	  
	           }	    
	            
	        } else {
	            System.err.println("File does not exist");
	        }
	          
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
			return allImagesPath;
	  }
	
}