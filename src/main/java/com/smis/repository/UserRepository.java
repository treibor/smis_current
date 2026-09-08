package com.smis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smis.entity.District;
import com.smis.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long>{

	Users findByUserName(String username);
	Users findByUserNameAndEnabled(String username, boolean enabled);
	@Query("select distinct u from Users u join u.Roles r "
			+ "where u.enabled = true and upper(r.roleName) = upper(?1) order by u.userId")
	List<Users> findEnabledUsersWithRole(String roleName);
	List<Users> findByDistrict(District district);
	List <Users> findByDistrictAndUserNameNot(District dist, String username);
	@Query("select Max(c.userId) from Users c ")
	Long findMaxSerial();
}
