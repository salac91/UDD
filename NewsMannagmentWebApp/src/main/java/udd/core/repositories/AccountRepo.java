package udd.core.repositories;

import java.util.List;

import udd.core.models.entities.Account;

public interface AccountRepo {
	
	public List<Account> findAllAccounts();
    public Account findAccount(Long id);
    public Account findAccountByName(String name);
    public Account createAccount(Account data);
    public Account updateAccount(Account data);
    public Account removeAccount(Account data);
    public Account findByFirstAndLastName(String first, String last);
}
