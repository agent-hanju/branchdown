package me.hanju.branchdown.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.hanju.auth.validator.domain.Account;
import me.hanju.branchdown.dto.StreamDto;

/** 여러 브랜치를 관리하는 하나의 흐름 엔티티 */
@SuperBuilder
@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@ToString(callSuper = false, exclude = { "updatedBy", "updatedAt", "branches", "nextBranchNum" })
@EqualsAndHashCode(callSuper = false, exclude = { "updatedBy", "updatedAt", "branches", "nextBranchNum" })
@Entity
@Table(name = "streams", uniqueConstraints = @UniqueConstraint(name = "UK_stream_uuid", columnNames = "uuid"))
@DynamicUpdate
public class StreamEntity extends CreateAuditEntity {

  @PrePersist
  public void generateUuid() {
    if (this.uuid == null) {
      this.uuid = UUID.randomUUID();
    }
    if (this.updatedAt == null) {
      this.updatedAt = Instant.now();
    }
    if (this.updatedBy == null) {
      this.updatedBy = this.getCreatedBy() != null ? this.getCreatedBy() : "system";
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "stream_id")
  private Long id;

  @Column(nullable = false)
  @Comment("공개 UUID (외부 노출용)")
  private UUID uuid;

  @Column(nullable = false, name = "updated_by")
  private String updatedBy;

  @Column(nullable = false, name = "updated_at")
  @ColumnDefault(value = "now()")
  private Instant updatedAt;

  @Setter
  @Builder.Default
  @Column(nullable = false, length = 64)
  private String title = "";

  @Builder.Default
  @OneToMany(mappedBy = "stream", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BranchEntity> branches = new ArrayList<>();

  /** 다음에 붙일 브랜치 번호(addBranch 시 동시에 업데이트) */
  @Builder.Default
  @Column(name = "next_branch_num")
  @Comment("다음에 붙일 브랜치 번호")
  private Integer nextBranchNum = 0;

  /**
   * 브랜치를 추가하고 nextBranchNum을 branches의 size에 맞춰 증가
   *
   * @param branch 추가할 BranchEntity
   */
  public void addBranch(final BranchEntity branch) {
    if (branch != null) {
      this.branches.add(branch);
      nextBranchNum = this.branches.size();
    }
  }

  /** 수동 업데이트 시점에 호출 */
  public void update() {
    this.updatedAt = Instant.now();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof Account account) {
        this.updatedBy = account.getPublicId();
        return;
      }
    }
    this.updatedBy = "system";

  }

  public StreamDto.Response toResponse() {
    return new StreamDto.Response(this.uuid, this.title, this.getCreatedBy(), this.getCreatedAt(), this.updatedBy,
        this.updatedAt);
  }
}
