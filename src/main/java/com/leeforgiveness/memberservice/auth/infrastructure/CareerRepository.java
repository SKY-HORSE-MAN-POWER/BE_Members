package com.leeforgiveness.memberservice.auth.infrastructure;

import com.leeforgiveness.memberservice.auth.domain.Career;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {

    List<Career> findByUuid(String uuid);
    Optional<Career> findByUuidAndJob(String uuid, String job);
}