package me.hanju.branchdown.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.hanju.branchdown.entity.StreamEntity;

@Repository
public interface StreamRepository extends JpaRepository<StreamEntity, Long> {
}
