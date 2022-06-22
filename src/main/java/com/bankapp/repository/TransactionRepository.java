package com.bankapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.bankapp.model.Transaction;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    Optional<List<Transaction>> findByAccountNumber(Long accountNumber);
    
}
