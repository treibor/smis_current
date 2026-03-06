package com.smis.dbservice;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smis.entity.Block;
import com.smis.entity.Constituency;
import com.smis.entity.District;
import com.smis.entity.Impldistrict;
import com.smis.entity.Installment;
import com.smis.entity.MasterProcess;
import com.smis.entity.ProcessFlow;
import com.smis.entity.ProcessUser;
import com.smis.entity.Scheme;
import com.smis.entity.State;
import com.smis.entity.Users;
import com.smis.entity.UsersRoles;
import com.smis.entity.Village;
import com.smis.entity.Work;
import com.smis.entity.Year;
import com.smis.repository.BlockRepository;
import com.smis.repository.ConstituencyRepository;
import com.smis.repository.DistrictRepository;
import com.smis.repository.ImpldistrictRepository;
import com.smis.repository.InstallmentRepository;
import com.smis.repository.MasterProcessRepository;
import com.smis.repository.ProcessFlowRepo;
import com.smis.repository.ProcessUserRepository;
import com.smis.repository.RoleRepository;
import com.smis.repository.SchemeRepository;
import com.smis.repository.StateRepository;
import com.smis.repository.UserRepository;
import com.smis.repository.VillageRepository;
import com.smis.repository.WorkRepository;
import com.smis.repository.YearRepository;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

@Service
public class Dbservice implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final WorkRepository wrepo;
	private final YearRepository yrepo;
	private final SchemeRepository srepo;
	private final ConstituencyRepository crepo;
	private final BlockRepository brepo;
	private final DistrictRepository drepo;
	private final InstallmentRepository irepo;
	private final ImpldistrictRepository idrepo;
	private final UserRepository urepo;
	private final StateRepository strepo;
	private final VillageRepository vtrepo;
	private final RoleRepository rolerepo;
	
	//Notification Notification = new Notification();
	//@Autowired
	private final MasterProcessRepository schemeprocessrepo;
	private final ProcessUserRepository processuserrepo;
	private final ProcessFlowRepo pflowrepo;
	public Dbservice(StateRepository strepo, UserRepository urepo, WorkRepository workrepo, YearRepository yrepo,
			SchemeRepository srepo, ConstituencyRepository crepo, BlockRepository brepo, DistrictRepository drepo,
			InstallmentRepository irepo, ImpldistrictRepository idrepo, VillageRepository vrepo, RoleRepository rolerepo,  MasterProcessRepository schemeprocessrepo,ProcessUserRepository processuserrepo,ProcessFlowRepo pflowrepo) {
		this.wrepo = workrepo;
		this.yrepo = yrepo;
		this.srepo = srepo;
		this.crepo = crepo;
		this.brepo = brepo;
		this.drepo = drepo;
		this.irepo = irepo;
		this.idrepo = idrepo;
		this.urepo = urepo;
		this.strepo = strepo;
		this.vtrepo = vrepo;
		this.rolerepo=rolerepo;
		this.schemeprocessrepo=schemeprocessrepo;
		this.processuserrepo=processuserrepo;
		this.pflowrepo=pflowrepo;
	}

	// Development Phase only
	public List<Village> getVillage(Block block) {
		return vtrepo.findByBlock(block);
	}

	public District getDistrict() {
		return drepo.findByDistrictId(getLoggedUser().getDistrict().getDistrictId());
	}

	public State getState(State state) {
		return strepo.findByStateId(state.getStateId());
	}
	public List<UsersRoles> getRoles(){
		return rolerepo.findByUser(getLoggedUser());
	}
	public List<UsersRoles> getRolesByUser(Users username){
		return rolerepo.findByUser(username);
	}
	public List<String> fetchRolesForSelectedUser(Users user) {
	    // Use your service to fetch the roles of the logged-in user
	    //List<UsersRoles> userRoles = rolerepo.findByUser(getLoggedUser());

	    // Map the UsersRoles objects to a list of role names
	    return getRolesByUser(user).stream()
	                    .map(UsersRoles::getRoleName)
	                    .collect(Collectors.toList());
	}
	
	public boolean isUser() {
	    
	    return getRoles().stream()
	            .anyMatch(role -> role.getRoleName().equalsIgnoreCase("Admin")
	                    || role.getRoleName().equalsIgnoreCase("SUPER")
	                    || role.getRoleName().equalsIgnoreCase("USER"));
	}

	
	public boolean isAdmin() {
		//Users loggedUser = getLoggedUser(); // Retrieve the logged-in user
	    return getRoles().stream()
	            .anyMatch(role -> role.getRoleName().equalsIgnoreCase("Admin")
	                    || role.getRoleName().equalsIgnoreCase("SUPER"));
	}

	public boolean isSuperAdmin() {
		//Users loggedUser = getLoggedUser(); // Retrieve the logged-in user
		return getRoles().stream()
	            .anyMatch(role -> 
	                    role.getRoleName().equalsIgnoreCase("SUPER")
	                   );
	}

	// Users
	public Users findUser(String username) {
		return urepo.findByUserName(username);
	}
	public List<Users> findUsersByDistrict(District district) {
		return urepo.findByDistrict(district);
	}
	public List<Users> findUsersByDistrictAndUserNameNot(District district, String username) {
		return urepo.findByDistrictAndUserNameNot(district, username);
	}
	public Users getLoggedUser() {
		return urepo.findByUserName(getloggeduser());
	}
	public Users getUser(String user) {
		return urepo.findByUserName(user);
	}
	public String getloggeduser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth.getName();
	}

	public void saveUser(Users user) {
		if (user == null) {
			Notification.show("Fail Fail Fail-7734");
			return;
		}
		urepo.save(user);

	}

	public long findMaxUserSerial() {

		try {

			return urepo.findMaxSerial();

		} catch (NullPointerException e) {

			return (long) 0;

		}
	}

	public List<Users> getAllUsers() {
		return urepo.findAll();
	}

	public BigDecimal calculateTotalReleasedAmount(Scheme scheme, Year year) {
		BigDecimal amount = BigDecimal.ZERO;
		List<Work> worksentered = wrepo.getWorksForCalculation(scheme, getDistrict(), year);
		int count = worksentered.size();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				amount = amount.add(worksentered.get(i).getWorkAmount());
			}
			return amount;
		} else {
			return BigDecimal.ZERO;
		}
	}

	// Installment Service
	public int getInstallmentCount(Work work) {
		return irepo.countByWork(work);
	}

	public List<Installment> getInstallments(Work work) {
		return irepo.findByWork(work);
	}

	public List<Installment> getMaxInstallment(Work work, int inst_no) {
		return irepo.findByWorkAndInstallmentNo(work, inst_no);
	}

	public List<Installment> getFilteredInstallments(Scheme scheme, Constituency consti, Block block, Year year,
			int installment) {
		return irepo.getFilteredInstallment(scheme, consti, block, getDistrict(), year, installment);
	}

	public Installment getInstallmentByWorkAndNo(int insallment, Work work) {
		return irepo.getInstallmentByNoAndWork(insallment, work);
	}

	public List<Installment> getInstallmentForReport(Scheme scheme, Year year, Constituency consti, Block block) {
		return irepo.getReportData(scheme, getDistrict(), year, consti, block);
	}

	public void saveInstallment(Installment install) {
		try {
			if (install == null) {

				return;
			}
			irepo.save(install);
		} catch (Exception e) {
			Notification.show("Unable to Save Installment. Error:" + e, 5000, Position.TOP_CENTER);
		}
	}

	public void deleteInstallments(Work work) {
		try {
			irepo.deleteByWork(work);
		} catch (Exception e) {
			Notification.show("Unable to Delete Installment. Error:" + e, 5000, Position.TOP_CENTER);
		}
	}

	// Works Queries
	public List<Work> getAllWorksAndInstallments() {
		try {
			return wrepo.findAllWithInstallments();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
	
	public List<Work> getFilteredWorks(String searchTerm) {
		try {
			return wrepo.search(searchTerm, getDistrict());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
	public List<Work> getFilteredWorkss(String searchTerm) {
		try {
			return wrepo.searchAll(searchTerm, getDistrict());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public List<Work> getFilteredWorks(Scheme scheme, Constituency consti, Block block, Year year, String searchTerm) {
		try {
			 boolean noFilters =
		                scheme == null &&
		                consti == null &&
		                block == null &&
		                year == null &&
		                (searchTerm == null || searchTerm.isBlank());

			 if (noFilters) {
				 	//System.out.println("No Filtere"+getDistrict().getDistrictName());
		            return wrepo.getTop100Works(getDistrict().getDistrictId());
		        }

		        return wrepo.getFilteredWorks(
		                scheme, getDistrict(), year, consti, block, searchTerm, Pageable.unpaged()
		        );
		} catch (Exception e) {

			return Collections.emptyList();

		}
	}

	public List<Work> getReportWorks(Scheme scheme, Constituency consti, Block block, Year year) {
		try {
			return wrepo.getReportWorks(scheme, getDistrict(), year, consti, block);
		} catch (Exception e) {

			return Collections.emptyList();

		}
	}

	public long getWorkCode() {
		return wrepo.findMaxWorkCode(getDistrict());
	}

	public long getWorkCount(Constituency consti) {
		return wrepo.findByConstituency(consti).size();
	}

	public long getWorkCount(Scheme scheme) {
		return wrepo.findByScheme(scheme).size();
	}

	public void saveWork(Work work) {
		try {
			if (work == null) {

				return;
			}
			wrepo.save(work);
		} catch (Exception e) {

			Notification.show("Unable to Save Work. Error:" + e, 5000, Position.TOP_CENTER);
		}
	}

	public void deleteWork(Work work) {
		// irepo.deleteByWork(work);
		try {
			wrepo.delete(work);
			Notification.show("Deleted Successfully");
		} catch (Exception e) {
			Notification.show("Unable to Delete Work. Error:" + e, 5000, Position.TOP_CENTER);
		}
	}

	// save & Delete Constituency
	public void saveConstituency(Constituency consti) {
		try {
			if (consti == null) {

				return;
			}
			crepo.save(consti);
		} catch (Exception e) {
			Notification.show("Unable to Save Constituency. Error:" + e, 5000, Position.TOP_CENTER);
		}

	}

	public void deleteConstituency(Constituency consti) {
		try {
			crepo.delete(consti);
		} catch (Exception e) {
			Notification.show("Unable to Delete Constituency " + e, 5000, Position.TOP_CENTER);
		}
	}

	// save & Delete Year
	public void saveYear(Year year) {
		if (year == null) {

			return;
		}
		yrepo.save(year);
	}

	public void deleteYear(Year year) {
		try {
			yrepo.delete(year);
		} catch (Exception e) {
			Notification.show("Unable to Delete Year " + e, 5000, Position.TOP_CENTER);
		}
	}

	// save & Delete scheme
	public void saveScheme(Scheme scheme) {
		try {
			if (scheme == null) {

				return;
			}
			srepo.save(scheme);
		} catch (Exception e) {
			Notification.show("Unable to Save Scheme " + e, 5000, Position.TOP_CENTER);
		}

	}

	public void deleteScheme(Scheme scheme) {
		try {
			srepo.delete(scheme);
		} catch (Exception e) {
			Notification.show("Unable to Delete Constituency " + e, 5000, Position.TOP_CENTER);
		}

	}

	// save & Delete blocks
	public void saveBlock(Block block) {
		try {
			if (block == null) {

				return;
			}
			brepo.save(block);
		} catch (DataIntegrityViolationException e) {
			Notification.show("Unable to Save Block/MB as It already Exists" + e, 5000, Position.TOP_CENTER);
		}
	}

	public void deleteBlock(Block block) {
		try {
			brepo.delete(block);
		} catch (Exception e) {
			Notification.show("Unable to Delete Constituency " + e, 5000, Position.TOP_CENTER);
		}
	}

	// save & Delete state
	public void saveState(State state) {
		try {
			if (state == null) {

				return;
			}
			strepo.save(state);
		} catch (Exception e) {
			Notification.show("Unable to Save State" + e, 5000, Position.TOP_CENTER);
		}
	}

	public void deleteState(State state) {
		try {
			strepo.delete(state);
		} catch (Exception e) {
			Notification.show("Unable to Delete Constituency " + e, 5000, Position.TOP_CENTER);
		}

	}

	// save & Delete district
	public void saveDistrict(District dist) {
		if (dist == null) {

			return;
		}
		drepo.save(dist);
	}

	public void deleteDistrict(District dist) {
		try {
			Impldistrict impdist = idrepo.findByDistrictCodeAndStateOrderByDistrictNameAsc(dist.getDistrictCode(),
					dist.getState());
			idrepo.delete(impdist);
			drepo.delete(dist);
		} catch (Exception e) {
			Notification.show("Unable to Delete District " + e, 5000, Position.TOP_CENTER);
		}

	}

	public long getMaxDistrictCode(State state) {
		return drepo.findMaxDistrictCode(state);
	}

	// save & Delete impldistrict
	public void saveImplDistrict(Impldistrict dist) {
		if (dist == null) {

			return;
		}
		idrepo.save(dist);
	}

	public Impldistrict getImpldistrict(long id) {
		return idrepo.findByDistrictIdOrderByDistrictName(id);
	}

	// Methods to Get All Data from Individual Tables
	public List<Work> getAllWorks() {
		if (isSuperAdmin()) {
			return wrepo.findAll();
		} else {
			// return wrepo.findByDistrict(getDistrict());
			return wrepo.findByDistrictOrderByWorkCodeDesc(getDistrict());
		}
	}
	public Stream<Work> fetch(int offset, int limit, Sort sort) {
        int page = offset / limit;
        PageRequest pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "workCode"));
        return wrepo.findByDistrict(getDistrict(), pageable).stream();
    }

    public int count() {
        return Math.toIntExact(wrepo.countByDistrict(getDistrict()));
    }

	public List<Year> getAllYears() {
		if (isSuperAdmin()) {
			return yrepo.findAll();
		} else {
			return yrepo.findByDistrictAndInUseOrderByYearNameDesc(getDistrict(), true);
		}

	}

	public List<Year> getAllYearsWIthNotInUse() {
		if (isSuperAdmin()) {
			return yrepo.findAll();
		} else {
			//return yrepo.findByDistrict(getDistrict());
			return yrepo.findByDistrictOrderByYearNameDesc(getDistrict());
		}

	}

	public List<Scheme> getAllSchemes() {
		if (isSuperAdmin()) {
			return srepo.findAll();
		} else {
			return srepo.findByDistrictAndInUse(getDistrict(), true);
		}

	}

	public List<Scheme> getAllSchemesWIthNotInUse() {
		if (isSuperAdmin()) {
			return srepo.findAll();
		} else {
			return srepo.findByDistrict(getDistrict());
		}

	}

	public List<Constituency> getAllConstituencies() {
		if (isSuperAdmin()) {
			return crepo.findAll();
		} else {
			// return crepo.findByDistrictAndInUse(getDistrict(), true);
			return crepo.findByDistrictAndInUseOrderByConstituencyNoAsc(getDistrict(), true);
		}
		// return crepo.findAll();
	}
	public List<String> getWorkNames(){
		return wrepo.findWorkNames();
	}
	public List<String> getSanctionNos(){
		return wrepo.findSanctionNos();
	}
	public List<Constituency> getAllConstituenciesWIthNotInUse() {
		if (isSuperAdmin()) {
			return crepo.findAll();
		} else {
			return crepo.findByDistrict(getDistrict());
		}
		// return crepo.findAll();
	}

	public List<Block> getAllBlocks() {
		if (isSuperAdmin()) {
			return brepo.findAll();
		} else {
			// return brepo.findByDistrictAndInUse(getDistrict(), true);
			return brepo.findByDistrictAndInUseOrderByBlockNameAsc(getDistrict(), true);
		}

	}

	public List<Block> getAllBlocksWithNotInUse() {
		if (isSuperAdmin()) {
			return brepo.findAll();
		} else {
			return brepo.findByDistrict(getDistrict());
		}

	}

	public List<District> getAllDistricts(State state) {
		return drepo.findByState(state);
	}

	public List<District> getAllDistrictsOfAllStates() {
		return drepo.findAll();
	}

	public List<State> getAllStates() {
		return strepo.findAll();
	}

	public List<Installment> getAllInstallments() {
		return irepo.findAll();
	}

	public List<Impldistrict> getAllImplDistricts() {
		return idrepo.findAll();
	}
	
	public void saveRole(UsersRoles role) {
	    try {
	        if (role != null) {
	            rolerepo.save(role); // Save or update the role
	        }
	    } catch (Exception e) {
	        Notification.show("Unable to Save Role. Error: " + e, 5000, Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
	    }
	}
	public void deleteRole(UsersRoles role) {
	    try {
	        if (role != null) {
	            rolerepo.delete(role); // Save or update the role
	        }
	    } catch (Exception e) {
	        Notification.show("Unable to Save Role. Error: " + e, 5000, Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
	    }
	}
	
	
	
	public void saveSchemeProcess(MasterProcess process) {
		schemeprocessrepo.save(process);
	}
	public void saveProcessUser(ProcessUser processuser) {
		processuserrepo.save(processuser);
	}
	public void saveProcessFlow(ProcessFlow processflow) {
		pflowrepo.save(processflow);
	}
	public void deleteInstallment(Installment inst) {
		irepo.delete(inst);
	}
	public boolean isLargestInstallment(Installment inst) {
	    if (inst == null || inst.getWork() == null) {
	        return false;
	    }
	    Integer maxNo = irepo.findMaxInstallmentNoByWork(inst.getWork());

	    if (maxNo == null) {
	        return false;
	    }
	    return inst.getInstallmentNo() == maxNo;
	}
}
