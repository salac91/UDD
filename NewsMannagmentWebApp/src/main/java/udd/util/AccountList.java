package udd.util;

import java.util.ArrayList;
import java.util.List;

import udd.core.models.entities.Account;

public class AccountList {
	
	private List<Account> accounts = new ArrayList<Account>();

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	
}
