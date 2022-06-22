package com.bankapp.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class BankInfo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "branch_name")
	private String branchName;

	@Column(name = "branch_code")
	private Integer branchCode;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Address branchAddress;

	@Column(name = "routing_number")
	private Integer routingNumber;
	
}
