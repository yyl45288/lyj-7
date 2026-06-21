package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.Wave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaveRepository extends JpaRepository<Wave, Long> {

    Optional<Wave> findByWaveNo(String waveNo);

    List<Wave> findByStatus(Wave.WaveStatus status);

    @Query("SELECT w FROM Wave w WHERE w.status IN :statuses ORDER BY w.createdAt DESC")
    List<Wave> findByStatusIn(List<Wave.WaveStatus> statuses);

    @Query("SELECT COUNT(w) FROM Wave w WHERE w.status = :status")
    long countByStatus(Wave.WaveStatus status);

    @Query("SELECT w FROM Wave w WHERE w.status = 'RELEASED' OR w.status = 'PICKING' ORDER BY w.createdAt DESC")
    List<Wave> findActiveWaves();

    boolean existsByWaveNo(String waveNo);
}
