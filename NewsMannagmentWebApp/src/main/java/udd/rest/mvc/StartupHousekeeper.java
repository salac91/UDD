package udd.rest.mvc;

import java.io.File;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import udd.tools.indexer.IndexManager;

@Component
public class StartupHousekeeper implements ApplicationListener<ContextRefreshedEvent> {
	
  @Autowired
  ServletContext servletContext;
  
  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
	  
	  String realPathAllDocs = servletContext.getRealPath("/docs");
	  File docsDirAllDocs = new File(realPathAllDocs);
  	  IndexManager.getIndexer().index(docsDirAllDocs); 
  }
}