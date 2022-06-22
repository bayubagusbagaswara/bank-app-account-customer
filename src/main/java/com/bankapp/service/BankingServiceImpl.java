package com.bankapp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.bankapp.dto.AccountInformation;
import com.bankapp.dto.CustomerDetails;
import com.bankapp.dto.TransactionDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.dto.TransferDetails;
import com.bankapp.model.Account;
import com.bankapp.model.Address;
import com.bankapp.model.Contact;
import com.bankapp.model.Customer;
import com.bankapp.model.CustomerAccountXRef;
import com.bankapp.model.Transaction;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.CustomerAccountXRefRepository;
import com.bankapp.repository.CustomerRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.service.helper.BankingServiceHelper;

@Service
@Transactional
public class BankingServiceImpl implements BankingService {

	private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerAccountXRefRepository custAccXRefRepository;
    private final BankingServiceHelper bankingServiceHelper;

	public BankingServiceImpl(CustomerRepository customerRepository, AccountRepository accountRepository, TransactionRepository transactionRepository, CustomerAccountXRefRepository custAccXRefRepository, BankingServiceHelper bankingServiceHelper) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.custAccXRefRepository = custAccXRefRepository;
		this.bankingServiceHelper = bankingServiceHelper;
	}

	public List<CustomerDetails> findAll() {
    	List<CustomerDetails> allCustomerDetails = new ArrayList<>();
        Iterable<Customer> customerList = customerRepository.findAll();
        customerList.forEach(customer -> allCustomerDetails.add(bankingServiceHelper.convertToCustomerDomain(customer)));
        return allCustomerDetails;
    }

	public ResponseEntity<Object> addCustomer(CustomerDetails customerDetails) {
		Customer customer = bankingServiceHelper.convertToCustomerEntity(customerDetails);
		customer.setCreateDateTime(new Date());
		customerRepository.save(customer);
		return ResponseEntity.status(HttpStatus.CREATED).body("New Customer created successfully.");
	}
    
	public CustomerDetails findByCustomerNumber(Long customerNumber) {
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);
		return customerEntityOpt.map(bankingServiceHelper::convertToCustomerDomain).orElse(null);
	}

	public ResponseEntity<Object> updateCustomer(CustomerDetails customerDetails, Long customerNumber) {
		Optional<Customer> managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);
		Customer unmanagedCustomerEntity = bankingServiceHelper.convertToCustomerEntity(customerDetails);
		if(managedCustomerEntityOpt.isPresent()) {
			Customer managedCustomerEntity = managedCustomerEntityOpt.get();
			
			if(Optional.ofNullable(unmanagedCustomerEntity.getContactDetails()).isPresent()) {
				
				Contact managedContact = managedCustomerEntity.getContactDetails();
				if(managedContact != null) {
					managedContact.setEmailId(unmanagedCustomerEntity.getContactDetails().getEmailId());
					managedContact.setHomePhone(unmanagedCustomerEntity.getContactDetails().getHomePhone());
					managedContact.setWorkPhone(unmanagedCustomerEntity.getContactDetails().getWorkPhone());
				} else
					managedCustomerEntity.setContactDetails(unmanagedCustomerEntity.getContactDetails());
			}
			
			if(Optional.ofNullable(unmanagedCustomerEntity.getAddress()).isPresent()) {
				
				Address managedAddress = managedCustomerEntity.getAddress();
				if(managedAddress != null) {
					managedAddress.setAddress1(unmanagedCustomerEntity.getAddress().getAddress1());
					managedAddress.setAddress2(unmanagedCustomerEntity.getAddress().getAddress2());
					managedAddress.setCity(unmanagedCustomerEntity.getAddress().getCity());
					managedAddress.setState(unmanagedCustomerEntity.getAddress().getState());
					managedAddress.setZip(unmanagedCustomerEntity.getAddress().getZip());
					managedAddress.setCountry(unmanagedCustomerEntity.getAddress().getCountry());
				} else
					managedCustomerEntity.setAddress(unmanagedCustomerEntity.getAddress());
			}
			
			managedCustomerEntity.setUpdateDateTime(new Date());
			managedCustomerEntity.setStatus(unmanagedCustomerEntity.getStatus());
			managedCustomerEntity.setFirstName(unmanagedCustomerEntity.getFirstName());
			managedCustomerEntity.setMiddleName(unmanagedCustomerEntity.getMiddleName());
			managedCustomerEntity.setLastName(unmanagedCustomerEntity.getLastName());
			managedCustomerEntity.setUpdateDateTime(new Date());
			
			customerRepository.save(managedCustomerEntity);
			return ResponseEntity.status(HttpStatus.OK).body("Success: Customer updated.");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number " + customerNumber + " not found.");
		}
	}

	public ResponseEntity<Object> deleteCustomer(Long customerNumber) {
		Optional<Customer> managedCustomerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(managedCustomerEntityOpt.isPresent()) {
			Customer managedCustomerEntity = managedCustomerEntityOpt.get();
			customerRepository.delete(managedCustomerEntity);
			return ResponseEntity.status(HttpStatus.OK).body("Success: Customer deleted.");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Customer does not exist.");
		}
		
		//TODO: Delete all customer entries from CustomerAccountXRef
	}

	public ResponseEntity<Object> findByAccountNumber(Long accountNumber) {
		Optional<Account> accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);
		return accountEntityOpt.<ResponseEntity<Object>>map(account -> ResponseEntity.status(HttpStatus.FOUND).body(bankingServiceHelper.convertToAccountDomain(account))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account Number " + accountNumber + " not found."));
	}

	public ResponseEntity<Object> addNewAccount(AccountInformation accountInformation, Long customerNumber) {
		
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		if(customerEntityOpt.isPresent()) {
			accountRepository.save(bankingServiceHelper.convertToAccountEntity(accountInformation));
			
			// Add an entry to the CustomerAccountXRef
			custAccXRefRepository.save(CustomerAccountXRef.builder()
					.accountNumber(accountInformation.getAccountNumber())
					.customerNumber(customerNumber)
					.build());
		}

		return ResponseEntity.status(HttpStatus.CREATED).body("New Account created successfully.");
	}

	public ResponseEntity<Object> transferDetails(TransferDetails transferDetails, Long customerNumber) {
		
		List<Account> accountEntities = new ArrayList<>();
		Account fromAccountEntity = null;
		Account toAccountEntity = null;
		
		Optional<Customer> customerEntityOpt = customerRepository.findByCustomerNumber(customerNumber);

		// If customer is present
		if(customerEntityOpt.isPresent()) {
			
			// get FROM ACCOUNT info
			Optional<Account> fromAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.getFromAccountNumber());
			if(fromAccountEntityOpt.isPresent()) {
				fromAccountEntity = fromAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("From Account Number " + transferDetails.getFromAccountNumber() + " not found.");
			}
			
			// get TO ACCOUNT info
			Optional<Account> toAccountEntityOpt = accountRepository.findByAccountNumber(transferDetails.getToAccountNumber());
			if(toAccountEntityOpt.isPresent()) {
				toAccountEntity = toAccountEntityOpt.get();
			}
			else {
			// if from request does not exist, 404 Bad Request
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To Account Number " + transferDetails.getToAccountNumber() + " not found.");
			}

			
			// if not sufficient funds, return 400 Bad Request
			if(fromAccountEntity.getAccountBalance() < transferDetails.getTransferAmount()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient Funds.");
			}
			else {
				synchronized (this) {
					// update FROM ACCOUNT 
					fromAccountEntity.setAccountBalance(fromAccountEntity.getAccountBalance() - transferDetails.getTransferAmount());
					fromAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(fromAccountEntity);
					
					// update TO ACCOUNT
					toAccountEntity.setAccountBalance(toAccountEntity.getAccountBalance() + transferDetails.getTransferAmount());
					toAccountEntity.setUpdateDateTime(new Date());
					accountEntities.add(toAccountEntity);
					
					accountRepository.saveAll(accountEntities);
					
					// Create transaction for FROM Account
					Transaction fromTransaction = bankingServiceHelper.createTransaction(transferDetails, fromAccountEntity.getAccountNumber(), "DEBIT");
					transactionRepository.save(fromTransaction);
					
					// Create transaction for TO Account
					Transaction toTransaction = bankingServiceHelper.createTransaction(transferDetails, toAccountEntity.getAccountNumber(), "CREDIT");
					transactionRepository.save(toTransaction);
				}

				return ResponseEntity.status(HttpStatus.OK).body("Success: Amount transferred for Customer Number " + customerNumber);
			}
				
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Number " + customerNumber + " not found.");
		}
	}

	public List<TransactionDetails> findTransactionsByAccountNumber(Long accountNumber) {
		List<TransactionDetails> transactionDetails = new ArrayList<>();
		Optional<Account> accountEntityOpt = accountRepository.findByAccountNumber(accountNumber);
		if(accountEntityOpt.isPresent()) {
			Optional<List<Transaction>> transactionEntitiesOpt = transactionRepository.findByAccountNumber(accountNumber);
			transactionEntitiesOpt.ifPresent(transactions -> transactions.forEach(transaction -> {
				transactionDetails.add(bankingServiceHelper.convertToTransactionDomain(transaction));
			}));
		}
		
		return transactionDetails;
	}

}