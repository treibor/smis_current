package com.smis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smis.entity.Block;
import com.smis.entity.Constituency;
import com.smis.entity.District;
import com.smis.entity.Installment;
import com.smis.entity.Scheme;
import com.smis.entity.Work;
import com.smis.entity.Year;

public interface InstallmentRepository extends JpaRepository<Installment, Long>{
	List<Installment> findByWork(Work work);
	List<Installment> findByWorkAndInstallmentNo(Work work, int inst_no);
	List<Installment> deleteByWork(Work work);
	int countByWork(Work work);
	
	
	
	@Query("select  c, d, e,f,g, h, i  from Installment c join c.work d join d.year e  join d.scheme f join d.constituency g join d.district h join d.block i where d.block=:block and d.district=:district and d.scheme=:scheme  and d.constituency=:consti and d.scheme=:scheme and d.year=:year and c.installmentNo=:installment order by d.workCode ASC")
	List<Installment> getFilteredInstallment(@Param("scheme") Scheme scheme, @Param("consti") Constituency consti,  @Param ("block") Block block ,  @Param ("district") District district, @Param ("year") Year year, @Param ("installment") int installment);
	
	@Query("select  c, d, e, g, h  from Installment c join c.work d join d.year e  join d.constituency g join d.district h  where  c.installmentNo=:installment and c.work=:work")
	Installment getInstallmentByNoAndWork( int installment,  @Param ("work") Work work);
	
	@Query("select  a, c, d, e, f, g from Installment a join a.work c join c.constituency d join c.scheme e join c.year f join c.block g  where  c.district=:district and (c.scheme=:scheme or :scheme is null ) and (c.year=:year or :year is null ) and (c.block=:block or :block is null ) and (c.constituency=:consti or :consti is null ) order by d.constituencyName, g.blockName, e.schemeName, f.yearName, c.workCode, a.installmentNo ASC")
	List<Installment> getReportData(@Param("scheme") Scheme scheme, @Param("district") District district, @Param("year") Year year,@Param("consti") Constituency consti, @Param("block") Block block);
	
	@Query("select  count(*) from Installment c")
	int getInstallmentCount();
	@Query("""
		       select max(i.installmentNo)
		       from Installment i
		       where i.work = :work
		         
		       """)
		Integer findMaxInstallmentNoByWork(@Param("work") Work work);
}
