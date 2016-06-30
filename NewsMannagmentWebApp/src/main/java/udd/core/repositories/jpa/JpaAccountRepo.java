package udd.core.repositories.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import udd.core.models.entities.Account;
import udd.core.repositories.AccountRepo;

@Repository
public class JpaAccountRepo implements AccountRepo {
	
	@PersistenceContext
	private EntityManager em;

    @SuppressWarnings("unchecked")
	public List<Account> findAllAccounts() {
        Query query = em.createQuery("SELECT a FROM Account a");
        return query.getResultList();
    }

    public Account findAccount(Long id) {
        return em.find(Account.class, id);
    }

    @SuppressWarnings("unchecked")
	public Account findAccountByName(String name) {
        Query query = em.createQuery("SELECT a FROM Account a WHERE a.name=?1");
        query.setParameter(1, name);
        List<Account> accounts = query.getResultList();
        if(accounts.size() == 0) {
            return null;
        } else {
            return accounts.get(0);
        }
    }

    public Account createAccount(Account data) {
        em.persist(data);
        return data;
    }
    
    public Account updateAccount(Account data) {
        em.merge(data);
        return data;
    }
    
    @SuppressWarnings("unchecked")
	public Account findByFirstAndLastName(String first, String last) {
    	Query query = em.createQuery("SELECT a FROM Account a WHERE a.firstName=?1 AND a.lastName=?2");
        query.setParameter(1, first);
        query.setParameter(2, last);
        List<Account> accounts = query.getResultList();
        if(accounts.size() == 0) {
            return null;
        } else {
            return accounts.get(0);
        }
    }

	public Account removeAccount(Account data) {
		em.remove(em.merge(data));
		return data;
	}



}