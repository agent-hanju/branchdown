package me.hanju.branchdown.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;

@Repository
public interface PointRepository extends JpaRepository<PointEntity, Long> {
  Optional<PointEntity> findByUuid(UUID uuid);

  @Query(value = """
      SELECT * FROM points AS p
      WHERE p.stream_id = :streamId
        AND (p.depth, p.branch_num) IN (
          SELECT p2.depth, MAX(p2.branch_num)
          FROM points AS p2
          WHERE p2.stream_id = :streamId
            AND p2.branch_num IN :branchNums
          GROUP BY p2.depth
        )
        AND p.depth > :depth
      ORDER BY p.depth
      """, nativeQuery = true)
  List<PointEntity> findAllUsingPath(
      Long streamId,
      List<Integer> branchNums,
      int depth);

  boolean existsByBranchAndDepth(BranchEntity branch, int i);
}
