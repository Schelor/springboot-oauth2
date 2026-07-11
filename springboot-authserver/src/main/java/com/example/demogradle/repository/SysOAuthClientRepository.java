package com.example.demogradle.repository;

import com.example.demogradle.entity.SysOAuthClientPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysOAuthClientRepository extends JpaRepository<SysOAuthClientPO, Long> {

    /**
     * 根据 clientId 查询客户端信息
     */
    Optional<SysOAuthClientPO> findByClientId(String clientId);
}