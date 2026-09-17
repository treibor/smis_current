package com.smis.audit;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smis.dbservice.AuditService;
import com.smis.dbservice.Dbservice;
import com.smis.entity.AuditTrail;
import com.smis.entity.Work;
import com.smis.entity.WorkNew;
import com.vaadin.flow.server.VaadinRequest;
@Service
public class Audit {
	private static final long serialVersionUID = 1L;
	@Autowired
	private AuditService auditservice;
	private Dbservice uservice;
	
	
	
	public Audit(Dbservice uservice) {
		this.uservice=uservice;
		// TODO Auto-generated constructor stub
	}
	public String getRealClientIp() {
	    VaadinRequest request = VaadinRequest.getCurrent();
	    String xForwardedForHeader = request.getHeader("X-Forwarded-For");
	    if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
	        return request.getRemoteAddr();
	    } else {
	        return xForwardedForHeader.split(",")[0].trim();
	    }
	}

	public void saveAudit(Work work, String action) {
		AuditTrail audit=new AuditTrail();
		audit.setAction(action);
		audit.setActionBy(uservice.getloggeduser());
		audit.setActionOn(LocalDateTime.now());
		audit.setIpAddress(getRealClientIp());
		//String villageText=work.getSanctionNo() == null ? "Master Data" : crop.getVillage().getVillageName();
		audit.setDetails(work.getWorkCode()+"- Sanction No:" +work.getSanctionNo() +", Sanction Date-"+work.getSanctionDate()+",Name-"+ work.getWorkName()+", Previous User-"+work.getEnteredBy()+", Previous Entry Date-"+work.getEnteredOn());
		auditservice.updateAudit(audit);
	}
	public void saveAudit(WorkNew work, String action) {
		AuditTrail audit=new AuditTrail();
		audit.setAction(action);
		audit.setActionBy(uservice.getloggeduser());
		audit.setActionOn(LocalDateTime.now());
		audit.setIpAddress(getRealClientIp());
		//String villageText=work.getSanctionNo() == null ? "Master Data" : crop.getVillage().getVillageName();
		audit.setDetails(work.getWorkCode()+"- Sanction No:" +work.getSanctionNo() +", Sanction Date-"+work.getSanctionDate()+",Name-"+ work.getWorkName()+", Previous User-"+work.getEnteredBy()+", Previous Entry Date-"+work.getEnteredOn());
		auditservice.updateAudit(audit);
	}
	public void saveLoginAudit(String action, String details) {
		AuditTrail audit=new AuditTrail();
		audit.setAction(action);
		audit.setActionOn(LocalDateTime.now());
		audit.setDetails(details);
		audit.setIpAddress(getRealClientIp());
		auditservice.updateAudit(audit);
	}
}
