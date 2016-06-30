package udd.rest.mvc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.ResponseBody;

import udd.core.models.entities.Account;
import udd.core.services.AccountService;
import udd.util.AccountList;
import udd.util.IsLogedInModel;
import udd.util.SendMail;
import udd.util.UserExistsModel;
import udd.util.UserRoleModel;

@Controller
@RequestMapping("/rest/accounts")
public class AccountController {
	
	private AccountService service;
	
	@Autowired
	public AccountController(AccountService service) {
		this.service = service;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize ("hasRole('Editor')")
    public ResponseEntity<AccountList> findAllAccounts() {
		
		List<Account> list = service.findAllAccounts();
		List<Account> listNoEditors = new ArrayList<Account>();
		
		for(Account account : list)
			if(!account.getRole().equals("Editor"))
				listNoEditors.add(account);		
        AccountList accountList = new AccountList();
        
        
        accountList.setAccounts(listNoEditors);
        return new ResponseEntity<AccountList>(accountList, HttpStatus.OK);
        
    }

    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("permitAll")
    public ResponseEntity<Account> createAccount(
            @RequestBody Account sentAccount
    ) throws MalformedURLException, IOException {
          	
       Account createdAccount = service.createAccount(sentAccount);
        
       if(createdAccount != null) {
	        @SuppressWarnings("resource")
			ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("spring/business-config.xml");
	        SendMail mm = (SendMail) context.getBean("sendMail");
	        Account account = service.findByAccountName(sentAccount.getName());
	       
	        String activationUrl = "http://localhost:8080/NewsMannagment/rest/accounts/activate/" + account.getId();
	     
	        mm.sendMail("uddeditor@gmail.com",
	        		sentAccount.getName(),
	    		   "Please activate your account!", 
	    		   "Hello " + sentAccount.getFirstName() + " " + sentAccount.getLastName() + "!\nThanks for joining our site."
	    		   		+ " Please activate your account by pressing this link below.\n\n" + activationUrl
	    		  );
        
       }
       else {
    	   return null;
       }
        return new ResponseEntity<Account>(createdAccount, HttpStatus.CREATED);
      
    }
    
    @RequestMapping(value="/createByAdmin",
            method = RequestMethod.POST)
    @PreAuthorize ("hasRole('Editor')")
    public ResponseEntity<Account> createAccountByAdmin(
            @RequestBody Account sentAccount
    ) {
    	
    	sentAccount.setRole("Journalist");
    	sentAccount.setStatus("ACTIVE");
    	Account createdAccount = service.createAccount(sentAccount);
    	
    	return new ResponseEntity<Account>(createdAccount, HttpStatus.CREATED);
    	
    }
    
    @RequestMapping(value="/update",method = RequestMethod.POST)
    @PreAuthorize ("hasRole('Editor')")
    public ResponseEntity<Account> updateAccount(
            @RequestBody Account sentAccount
    ) {
    	      	
        Account updatedAccount = service.updateAccount(sentAccount);
        return new ResponseEntity<Account>(updatedAccount, HttpStatus.OK);
      
    }
    
    @RequestMapping(value="/remove/{id}",method = RequestMethod.POST)
    @PreAuthorize ("hasRole('Editor')")
    public ResponseEntity<Account> removeAccount(
    		@PathVariable long id
    ) {
    	      	
        Account account = service.findAccount(id);
        Account removedAccount = service.removeAccount(account);
        return new ResponseEntity<Account>(removedAccount, HttpStatus.OK);
      
    }


    @RequestMapping(value="/{accountId}",
                method = RequestMethod.GET)
    @PreAuthorize ("hasRole('Editor')")
    public ResponseEntity<Account> getAccount(
            @PathVariable Long accountId
    ) {
        Account account = service.findAccount(accountId);
        if(account != null)
        {
            return new ResponseEntity<Account>(account, HttpStatus.OK);
        } else {
            return new ResponseEntity<Account>(HttpStatus.NOT_FOUND);
        }
    }
    
    @RequestMapping(value="/role",
            method = RequestMethod.GET)
    @PreAuthorize("permitAll")
	public ResponseEntity<UserRoleModel> getUserRole(
	       
	) {
    	Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Account loggedIn = null;
		String role;
        if(principal instanceof UserDetails) {
            UserDetails details = (UserDetails)principal;
            loggedIn = service.findByAccountName(details.getUsername());
        }    
        
        if(loggedIn == null) {
        	role = "User";
        }
        else {
        	if(loggedIn.getRole().equals("Journalist")) role = "Journalist";
        	else if(loggedIn.getRole().equals("Editor")) role = "Editor";
        	else role = "Unknown";
        }
        
        UserRoleModel roleModel = new UserRoleModel();
        roleModel.setRole(role);
        
        return new ResponseEntity<UserRoleModel>(roleModel, HttpStatus.OK);
    }
    
    @RequestMapping(value="/activate/{accountId}",
            method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public ResponseEntity<Object> activateAccount(
	        @PathVariable Long accountId
	) throws URISyntaxException {
    	
	    Account account = service.findAccount(accountId);
	    
	    if(account != null) {
	    	account.setStatus("ACTIVE");
	    	service.updateAccount(account);
	    	
	    }
	    
	    URI uri = new URI("http://localhost:8080/NewsMannagment/app/index.html");
	    HttpHeaders httpHeaders = new HttpHeaders();
	    httpHeaders.setLocation(uri);
	    return new ResponseEntity<Object>(httpHeaders, HttpStatus.SEE_OTHER);
    }
    
    @RequestMapping(value="changePassword/{oldPassword}/{newPassword}",
            method = RequestMethod.GET)
    @PreAuthorize ("hasRole('Journalist')")
	public @ResponseBody
	String changePassword(
	        @PathVariable String oldPassword, @PathVariable String newPassword
	) {

    	Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Account loggedIn = null;
        if(principal instanceof UserDetails) {
            UserDetails details = (UserDetails)principal;
            loggedIn = service.findByAccountName(details.getUsername());
        }
        else {
        	return "error";
        }
        
        if(loggedIn.getPassword().equals(oldPassword)) {
        	loggedIn.setPassword(newPassword);
        	service.updateAccount(loggedIn);
        	return "ok";
        }
        
        return "error";
	}
    
    @RequestMapping(value="/logedIn",
            method = RequestMethod.GET)
	@PreAuthorize("permitAll")
	public ResponseEntity<IsLogedInModel> logedIn() {

    	Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		boolean isLogedIn = false;   

        if(principal instanceof UserDetails) {
            UserDetails details = (UserDetails)principal;         
            
            if(!details.getAuthorities().isEmpty()) isLogedIn = true;
        }
                
        IsLogedInModel logedInModel = new IsLogedInModel();
        logedInModel.setIsLoged(isLogedIn);
        return new ResponseEntity<IsLogedInModel>(logedInModel,HttpStatus.OK);
	}
	    
    @RequestMapping(value="/userExists/{email}",
            method = RequestMethod.GET)
    @PreAuthorize("permitAll")
	public ResponseEntity<UserExistsModel> userExists(
	        @PathVariable String email
	) {
    	UserExistsModel existsModel = new UserExistsModel();
    	Account acc = service.findByAccountName(email);
    	if(acc!= null) existsModel.setExists(true);
    	else  existsModel.setExists(false); 
    	return new ResponseEntity<UserExistsModel>(existsModel,HttpStatus.OK);
    }
}
