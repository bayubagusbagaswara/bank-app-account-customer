package com.bankapp.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.bankapp.model.Account;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {

	Optional<Account> findByAccountNumber(Long accountNumber);
}
