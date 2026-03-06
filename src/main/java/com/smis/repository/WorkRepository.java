package com.smis.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smis.entity.Block;
import com.smis.entity.Constituency;
import com.smis.entity.District;
import com.smis.entity.Scheme;
import com.smis.entity.Work;
import com.smis.entity.Year;


public interface WorkRepository extends JpaRepository<Work, Long> {
	
	Page<Work> findByDistrict(District district, Pageable pageable);//use only on postgres 14+
	long countByDistrict(District district);
	List<Work> findByDistrictOrderByWorkCodeDesc(District district);
	List<Work> findByConstituency(Constituency constituency);	
	List<Work> findByScheme(Scheme scheme);	
	@Query("select coalesce (Max(c.workCode),0) from Work c where c.district= :district")
	Long findMaxWorkCode(@Param ("district") District district);
	
	@Query("SELECT w FROM Work w LEFT JOIN FETCH w.installments ORDER BY w.workCode DESC")
	List<Work> findAllWithInstallments();
	
	@Query("SELECT w.workName FROM Work w")
    List<String> findWorkNames();
	
	@Query("SELECT Distinct(w.sanctionNo) FROM Work w")
    List<String> findSanctionNos();
	/*
	@Query("select  c, d, e,f,g, h, i  from Work c join c.installment d join c.year e  join c.scheme f join c.constituency g join c.district h join c.block i where c.block=:block and c.district=:district and c.scheme=:scheme  and c.constituency=:consti and c.scheme=:scheme and c.year=:year and d.installmentNo=:installment order by c.workCode ASC")
	List<Work> getFilteredWorks(@Param("scheme") Scheme scheme, @Param("consti") Constituency consti,  @Param ("block") Block block ,  @Param ("district") District district, @Param ("year") Year year, @Param ("installment") int installment);
	*/
	@Query("select  c from Work c where  c.district=:district and c.scheme=:scheme and c.year=:year ")
	List<Work> getWorksForCalculation(@Param("scheme") Scheme scheme, @Param("district") District district, @Param("year") Year year);
	
	@Query("select  c from Work c where  c.district=:district and (c.scheme=:scheme or :scheme is null or :scheme=0) and (c.year=:year or :year is null or :year=0) and (c.block=:block or :block is null or :block=0) and (c.constituency=:consti or :consti is null or :consti=0)  order by c.workCode Desc")
	List<Work> getFilteredWorkss(@Param("scheme") Scheme scheme, @Param("district") District district, @Param("year") Year year,@Param("consti") Constituency consti, @Param("block") Block block);
	
	
	@Query("""
		    SELECT c
		    FROM Work c
		    WHERE c.district = :district
		      AND (:scheme IS NULL OR c.scheme = :scheme)
		      AND (:year   IS NULL OR c.year = :year)
		      AND (:block  IS NULL OR c.block = :block)
		      AND (:consti IS NULL OR c.constituency = :consti)

		      AND (
		            :searchTerm IS NULL OR :searchTerm = '' OR
		            str(c.workCode) = :searchTerm OR
		            lower(c.workName) LIKE lower(concat('%', :searchTerm, '%')) OR
		            lower(c.sanctionNo) LIKE lower(concat('%', :searchTerm, '%'))
		          )

		    ORDER BY c.workCode DESC
		""")
		List<Work> getFilteredWorks(
		        @Param("scheme") Scheme scheme,
		        @Param("district") District district,
		        @Param("year") Year year,
		        @Param("consti") Constituency consti,
		        @Param("block") Block block,
		        @Param("searchTerm") String searchTerm,
		        Pageable pageable
		);
	@Query(value = """
		    SELECT *
		    FROM work c
		    WHERE c.district_id = :districtId
		    ORDER BY c.work_code DESC
		    LIMIT 100
		""", nativeQuery = true)
		List<Work> getTop100Works(@Param("districtId") Long districtId);
	
	@Query("select  c, d, e, f, g, h from Work c join c.constituency d join c.block e join c.scheme f join c.year g join c.district h where  c.district=:district and (c.scheme=:scheme or :scheme is null) and (c.year=:year or :year is null) and (c.block=:block or :block is null ) and (c.constituency=:consti or :consti is null ) order by d.constituencyName, e.blockName, f.schemeName, g.yearName, c.workCode Desc")
	List<Work> getReportWorks(@Param("scheme") Scheme scheme, @Param("district") District district, @Param("year") Year year,@Param("consti") Constituency consti, @Param("block") Block block);
	
	String a1="select c from Work c where c.district= :district and(";
	String a2="lower(c.workName) like lower(concat('%', :searchTerm, '%')) ";
	String a3="or lower(c.sanctionNo) like lower(concat('%', :searchTerm, '%')))";
	//@Query(a1+a2+a3)
	@Query("select c from Work c where c.district= :district and(lower(c.workName) like lower(concat('%', :searchTerm, '%'))or lower(c.sanctionNo) like lower(concat('%', :searchTerm, '%'))) order by c.workCode Desc")
	List<Work> search(@Param("searchTerm") String searchTerm, @Param("district") District district);
	
	@Query("select c from Work c where c.district= :district and(str(c.workCode)=:searchTerm or lower(c.workName) like lower(concat('%', :searchTerm, '%'))or lower(c.sanctionNo) like lower(concat('%', :searchTerm, '%'))) order by c.workCode Desc")
	List<Work> searchAll(@Param("searchTerm") String searchTerm, @Param("district") District district);
	
	@Query("select  count(*) from Work c  where  c.enteredOn between :sdate and :edate")
	int getWorksCountBetweenDates(@Param("sdate") LocalDate sdate, @Param("edate") LocalDate edate);
	@Query("select  count(*) from Work c")
	int getWorksCount();
	
	

		
}
