package com.smis.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Entity
public class Work implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO,  generator= "work_generator")
	@SequenceGenerator(name="work_generator", allocationSize = 1, sequenceName = "work_seq", initialValue = 1)
	private long workId;
	@Column(unique=false)
	private long workCode;
	@NotEmpty(message = "Work name cannot be blank")
	@Column(length=1000)
	@Length(max = 1000, message="Work Name Has Exceeded the Limit")
	private String workName;
	@Digits(integer=10, fraction=2)
	private BigDecimal workAmount;
	@NotNull
	private int noOfInstallments=2;
	@NotEmpty (message = "Please Enter the Sanction No")
	@Length(max = 100, message="Sanction Number Has exceeded the Limit")
	private String sanctionNo;
	private LocalDate sanctionDate;
	//private long workCode;
	private String workStatus;
	private String workLabel;
	@ManyToOne
	@JoinColumn(name="yearId")
	@NotNull(message = "Please select the Year")
	private Year year;
	@ManyToOne
	@JoinColumn(name="schemeId")
	@NotNull(message = "Please select the scheme")
	private Scheme scheme;
	@ManyToOne
	@JoinColumn(name="blockId")
	@NotNull(message = "Please select the block")
	private Block block;
	@ManyToOne
	@JoinColumn(name="villageId")
	@NotNull(message = "Please select the village")
	private Village village;
	@ManyToOne
	@JoinColumn(name="constituencyId")
	@NotNull(message = "Please select the constituency")
	private Constituency constituency;
	@ManyToOne
	@JoinColumn(name="districtId")
	@NotNull
	private District district;
	@OneToMany(mappedBy = "work", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Installment> installments;
	
	public List<Installment> getInstallments() {
		return installments;
	}
	public void setInstallments(List<Installment> installments) {
		this.installments = installments;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	private String enteredBy;
	private LocalDate enteredOn;
	
	
	
	
	public Village getVillage() {
		return village;
	}
	public void setVillage(Village village) {
		this.village = village;
	}
	
	
	public long getWorkId() {
		return workId;
	}
	public void setWorkId(long workId) {
		this.workId = workId;
	}
	
	public long getWorkCode() {
		return workCode;
	}
	public void setWorkCode(long workCode) {
		this.workCode = workCode;
	}
	public String getWorkName() {
		return workName;
	}
	public void setWorkName(String workName) {
		this.workName = workName;
	}
	
	
	public BigDecimal getWorkAmount() {
		return workAmount;
	}
	public void setWorkAmount(BigDecimal workAmount) {
		this.workAmount = workAmount;
	}
	public int getNoOfInstallments() {
		return noOfInstallments;
	}
	public void setNoOfInstallments(int noOfInstallments) {
		this.noOfInstallments = noOfInstallments;
	}
	
	public String getSanctionNo() {
		return sanctionNo;
	}
	public void setSanctionNo(String sanctionNo) {
		this.sanctionNo = sanctionNo;
	}
	public LocalDate getSanctionDate() {
		return sanctionDate;
	}
	public void setSanctionDate(LocalDate sanctionDate) {
		this.sanctionDate = sanctionDate;
	}
	public String getWorkStatus() {
		return workStatus;
	}
	public void setWorkStatus(String workStatus) {
		this.workStatus = workStatus;
	}
	public String getWorkLabel() {
		return workLabel;
	}
	public void setWorkLabel(String workLabel) {
		this.workLabel = workLabel;
	}
	public Year getYear() {
		return year;
	}
	public void setYear(Year year) {
		this.year = year;
	}
	public Scheme getScheme() {
		return scheme;
	}
	public void setScheme(Scheme scheme) {
		this.scheme = scheme;
	}
	public Block getBlock() {
		return block;
	}
	public void setBlock(Block block) {
		this.block = block;
	}
	public Constituency getConstituency() {
		return constituency;
	}
	public void setConstituency(Constituency constituency) {
		this.constituency = constituency;
	}
	public District getDistrict() {
		return district;
	}
	public void setDistrict(District district) {
		this.district = district;
	}
	
	public String getEnteredBy() {
		return enteredBy;
	}
	public void setEnteredBy(String enteredBy) {
		this.enteredBy = enteredBy;
	}
	public LocalDate getEnteredOn() {
		return enteredOn;
	}
	public void setEnteredOn(LocalDate enteredOn) {
		this.enteredOn = enteredOn;
	}
	@Override
	public String toString() {
		return "Work [workId=" + workId + ", workCode=" + workCode + ", workName=" + workName + ", workAmount="
				+ workAmount + ", noOfInstallments=" + noOfInstallments + ", sanctionNo=" + sanctionNo
				+ ", sanctionDate=" + sanctionDate + ", workStatus=" + workStatus + ", workLabel=" + workLabel
				+ ", year=" + year + ", scheme=" + scheme + ", block=" + block + ", constituency=" + constituency
				+ ", district=" + district + ", enteredBy=" + enteredBy + ", enteredOn=" + enteredOn + "]";
	}
	
	
		
}
