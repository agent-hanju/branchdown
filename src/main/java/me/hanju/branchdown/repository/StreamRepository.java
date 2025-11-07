package me.hanju.branchdown.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import me.hanju.branchdown.dto.StreamDto;
import me.hanju.branchdown.entity.StreamEntity;

@Repository
public interface StreamRepository extends JpaRepository<StreamEntity, Long> {

  Optional<StreamEntity> findByUuid(UUID uuid);

  @Query("""
      SELECT
        s.uuid AS id,
        s.title AS title,
        COALESCE(MAX(p.createdAt), s.updatedAt) AS updatedAt
      FROM StreamEntity s
      LEFT JOIN PointEntity p ON p.stream = s
      WHERE s.createdBy = :createdBy
      GROUP BY s.uuid, s.title, s.updatedAt
      """)
  Page<StreamDto.ListItem> findAllByCreatedBy(String createdBy,
      Pageable pageable);

  @Query("""
      SELECT
        s.uuid AS id,
        s.title AS title,
        COALESCE(MAX(p.createdAt), s.updatedAt) AS updatedAt
      FROM StreamEntity s
      LEFT JOIN PointEntity p ON p.stream = s
      WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
      GROUP BY s.uuid, s.title, s.updatedAt
      """)
  Page<StreamDto.ListItem> findAllByTitleContainingIgnoreCase(String query, Pageable pageable);

  @Query("""
      SELECT
        s.uuid AS id,
        s.title AS title,
        COALESCE(MAX(p.createdAt), s.updatedAt) AS updatedAt
      FROM StreamEntity s
      LEFT JOIN PointEntity p ON p.stream = s
      WHERE s.createdBy = :createdBy
        AND LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
      GROUP BY s.uuid, s.title, s.updatedAt
      """)
  Page<StreamDto.ListItem> findAllByTitleContainingIgnoreCaseAndCreatedBy(String query, String createdBy,
      Pageable pageable);
}
