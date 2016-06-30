package udd.core.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import udd.core.models.entities.Account;
import udd.core.repositories.AccountRepo;
import udd.core.services.AccountService;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

	@Autowired
	private AccountRepo repo;


    public Account findAccount(Long id) {
        return repo.findAccount(id);
    }

 
    public Account createAccount(Account data) {
        Account account = repo.findAccountByName(data.getName());
        if(account == null)
        	return repo.createAccount(data);
        else 
        	return null;
    }

    public List<Account> findAllAccounts() {
        return repo.findAllAccounts();
    }

    public Account findByAccountName(String name) {
        return repo.findAccountByName(name);
    }
    
    public Account updateAccount(Account data) {
    	return repo.updateAccount(data);
    }
    
    public Account findByFirstAndLastName(String first, String last) {   	
    	return repo.findByFirstAndLastName(first, last);
    }
    
    public Account removeAccount(Account data) {
    	return repo.removeAccount(data);
    }
	
}
