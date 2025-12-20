package me.hanju.branchdown.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.hanju.branchdown.config.IntegerListConverter;
import me.hanju.branchdown.dto.PointDto;

/** 하나의 포인트를 지정하는 엔티티 */
@Builder
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = { "branch", "childBranchNums" })
@EqualsAndHashCode(exclude = { "branch", "childBranchNums" })
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "points")
@DynamicUpdate
public class PointEntity {

  // 수동 동기화 필요
  @PostLoad
  @PostPersist
  public void syncBranchNum() {
    if (this.branch != null) {
      this.branchNum = this.branch.getBranchNum();
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "point_id")
  private Long id;

  @Column(name = "item_id", updatable = false)
  @Comment("저장할 아이템의 ID, root의 경우 null")
  private String itemId;

  /** 0부터 시작하는 stream 내에서의 depth */
  @Column(name = "depth", nullable = false, updatable = false)
  @Comment("0부터 시작하는 stream 내에서의 depth")
  private int depth;

  @CreatedDate
  @Column(nullable = false, name = "created_at")
  @ColumnDefault(value = "now()")
  private Instant createdAt;

  /** 이 포인트의 소속 브랜치 */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(value = {
      @JoinColumn(name = "stream_id", referencedColumnName = "stream_id", nullable = false, updatable = false),
      @JoinColumn(name = "branch_num", referencedColumnName = "branch_num", nullable = false, updatable = false),
  }, foreignKey = @ForeignKey(name = "FK_point_to_branch"))
  private BranchEntity branch;

  // ========== 읽기 전용 필드 ==========

  /**
   * 이 포인트의 소속 스트림(읽기 전용), branch 필드에 의해 결정
   * <p>
   * <strong>주의:</strong> 이 필드는 Repository의 JPQL 쿼리 전용입니다.
   * Java 코드에서는 {@code point.getBranch().getStream()}을 사용하세요.
   * </p>
   */
  @Getter(AccessLevel.PRIVATE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stream_id", referencedColumnName = "stream_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_point_to_stream"))
  @Comment("소속 스트림 ID")
  private StreamEntity stream;

  /** 이 포인트의 소속 브랜치 번호(읽기 전용), branch 필드에 의해 결정 */
  @Column(name = "branch_num", insertable = false, updatable = false)
  @Comment("소속 브랜치의 branchNum")
  private Integer branchNum;
  // ====================================

  /** 이 포인트 아래로 이어지는 브랜치의 branchNum */
  @Builder.Default
  @Column(name = "child_branch_nums", length = 256)
  @Convert(converter = IntegerListConverter.class)
  @Comment("이 포인트를 베이스로 하는 branch_num 목록(쉼표로 구분)")
  private List<Integer> childBranchNums = new ArrayList<>();

  /**
   * 이 포인트 아래로 이어지는 branchNum 추가
   *
   * @param branchNum 추가할 branchNum
   */
  public void addChildBranchNum(final int branchNum) {
    // JPA 기준으로는 값이기 때문에 포인터 변경 필요
    List<Integer> newChildBranchNums = new ArrayList<>(this.childBranchNums);
    newChildBranchNums.add(branchNum);
    this.childBranchNums = newChildBranchNums;
  }

  public PointDto.Response toResponse() {
    return new PointDto.Response(this.id, this.getBranchNum(), this.itemId, this.childBranchNums, this.createdAt);
  }
}
