package com.meridian.backups.repository;

import com.meridian.backups.entity.BackupPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupPolicyRepository extends JpaRepository<BackupPolicy, Integer> {}
