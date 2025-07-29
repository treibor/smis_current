package com.smis.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Entity 
public class Installment implements Serializable {
	 private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO,  generator= "inst_generator")
	@SequenceGenerator(name="inst_generator", allocationSize = 1, sequenceName = "inst_seq", initialValue = 1)
	private long installmentId;
	private int installmentNo;
	@Digits(integer=10, fraction=2)
	private BigDecimal installmentAmountPrev;
	@Digits(integer=10, fraction=2)
	private BigDecimal installmentAmount;
	private String installmentLetter;
	private String installmentCheque;
	private LocalDate installmentDate;
	private String installmentLabel;
	private String ucLetter;
	private LocalDate ucDate;
	private String enteredBy;
	private LocalDate enteredOn;
	@Column(length=2000)
	private String copyTo;
	@ManyToOne 
	@JoinColumn(name="workId", referencedColumnName = "workId")
	@OnDelete(action = OnDeleteAction.CASCADE)
	@NotNull
	private Work work;
	
	
	public long getInstallmentId() {
		return installmentId;
	}
	public void setInstallmentId(long installmentId) {
		this.installmentId = installmentId;
	}
	public int getInstallmentNo() {
		return installmentNo;
	}
	public void setInstallmentNo(int installmentNo) {
		this.installmentNo = installmentNo;
	}
	
	
	public BigDecimal getInstallmentAmountPrev() {
		return installmentAmountPrev;
	}
	public void setInstallmentAmountPrev(BigDecimal installmentAmountPrev) {
		this.installmentAmountPrev = installmentAmountPrev;
	}
	public BigDecimal getInstallmentAmount() {
		return installmentAmount;
	}
	public void setInstallmentAmount(BigDecimal installmentAmount) {
		this.installmentAmount = installmentAmount;
	}
	public String getInstallmentLetter() {
		return installmentLetter;
	}
	public void setInstallmentLetter(String installmentLetter) {
		this.installmentLetter = installmentLetter;
	}
	public String getInstallmentCheque() {
		return installmentCheque;
	}
	public void setInstallmentCheque(String installmentCheque) {
		this.installmentCheque = installmentCheque;
	}
	public LocalDate getInstallmentDate() {
		return installmentDate;
	}
	public void setInstallmentDate(LocalDate installmentDate) {
		this.installmentDate = installmentDate;
	}
	public String getInstallmentLabel() {
		return installmentLabel;
	}
	public void setInstallmentLabel(String installmentLabel) {
		this.installmentLabel = installmentLabel;
	}
	public String getUcLetter() {
		return ucLetter;
	}
	public void setUcLetter(String ucLetter) {
		this.ucLetter = ucLetter;
	}
	public LocalDate getUcDate() {
		return ucDate;
	}
	public void setUcDate(LocalDate ucDate) {
		this.ucDate = ucDate;
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
	public String getCopyTo() {
		return copyTo;
	}
	public void setCopyTo(String copyTo) {
		this.copyTo = copyTo;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public Work getWork() {
		return work;
	}
	public void setWork(Work work) {
		this.work = work;
	}
	

	@Override
	public String toString() {
		return "Installment [installmentId=" + installmentId + ", installmentNo=" + installmentNo
				+ ", installmentAmountPrev=" + installmentAmountPrev + ", installmentAmount=" + installmentAmount
				+ ", installmentLetter=" + installmentLetter + ", installmentCheque=" + installmentCheque
				+ ", installmentDate=" + installmentDate + ", installmentLabel=" + installmentLabel + ", ucLetter="
				+ ucLetter + ", ucDate=" + ucDate + ", work=" + work + "]";
	}
	


}
