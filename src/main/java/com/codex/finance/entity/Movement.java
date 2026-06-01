package com.codex.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movements")
public class Movement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;
    
    @Column(name = "account_id", columnDefinition = "uuid")
    private UUID accountId;
    
    @Column(name = "transfer_account_id", columnDefinition = "uuid")
    private UUID transferAccountId;
    
    @Column(name = "category_id", columnDefinition = "uuid")
    private UUID categoryId;
    
    @Column(name = "debt_id", columnDefinition = "uuid")
    private UUID debtId;
    
    @Column(name = "movement_type", columnDefinition = "public.movement_type")
    private String movementType;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal amount;
    
    private String description;
    
    @Column(name = "movement_date")
    private LocalDate movementDate;
    
    @Column(columnDefinition = "text[]")
    private String tags;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    @Column(name = "row_version")
    private Long rowVersion;

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getTransferAccountId() { return transferAccountId; }
    public void setTransferAccountId(UUID transferAccountId) { this.transferAccountId = transferAccountId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public UUID getDebtId() { return debtId; }
    public void setDebtId(UUID debtId) { this.debtId = debtId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getRowVersion() { return rowVersion; }
    public void setRowVersion(Long rowVersion) { this.rowVersion = rowVersion; }
}