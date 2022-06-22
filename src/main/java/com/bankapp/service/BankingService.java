package com.bankapp.service;

import java.util.List;

import com.bankapp.dto.AccountInformation;
import com.bankapp.dto.CustomerDetails;
import com.bankapp.dto.TransactionDetails;
import org.springframework.http.ResponseEntity;

import com.bankapp.dto.TransferDetails;

public interface BankingService {

    List<CustomerDetails> findAll();
    
    ResponseEntity<Object> addCustomer(CustomerDetails customerDetails);
    
    CustomerDetails findByCustomerNumber(Long customerNumber);
    
    ResponseEntity<Object> updateCustomer(CustomerDetails customerDetails, Long customerNumber);
    
    ResponseEntity<Object> deleteCustomer(Long customerNumber) ;
    
    ResponseEntity<Object> findByAccountNumber(Long accountNumber);
    
    ResponseEntity<Object> addNewAccount(AccountInformation accountInformation, Long customerNumber);
    
    ResponseEntity<Object> transferDetails(TransferDetails transferDetails, Long customerNumber);
    
    List<TransactionDetails> findTransactionsByAccountNumber(Long accountNumber);
    
}
