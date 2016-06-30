package udd.core.services;

import java.util.List;

import udd.core.models.entities.Account;

public interface AccountService {

	public Account findAccount(Long id);
    public Account createAccount(Account data);
    public List<Account> findAllAccounts();
    public Account findByAccountName(String name);
    public Account updateAccount(Account data);
    public Account removeAccount(Account data);
    public Account findByFirstAndLastName(String first, String last);
}
