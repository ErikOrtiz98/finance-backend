package com.codex.finance.repository;

import com.codex.finance.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    @Query(value = "SELECT c.id, c.user_id AS userId, c.name, c.color, c.icon, " +
           "c.category_type AS type, c.created_at AS createdAt, c.updated_at AS updatedAt, " +
           "c.deleted_at AS deletedAt, " +
           "CASE WHEN c.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(c.row_version, 1) AS version " +
           "FROM categories c WHERE c.user_id = :userId AND c.deleted_at IS NULL " +
           "ORDER BY c.created_at DESC", nativeQuery = true)
    List<Object[]> listCategories(@Param("userId") UUID userId);
    
//    @Modifying
//    @Query(value = "INSERT INTO categories (user_id, name, color, icon, category_type, " +
//           "created_at, updated_at, row_version) VALUES (:userId, :name, :color, :icon, CAST(:type AS category_type), " +
//           "NOW(), NOW(), 1) RETURNING id, user_id AS userId, name, color, icon, " +
//           "category_type AS type, created_at AS createdAt, updated_at AS updatedAt, " +
//           "deleted_at AS deletedAt, CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
//           "COALESCE(row_version, 1) AS version", nativeQuery = true)
//    Object[] createCategory(@Param("userId") UUID userId, @Param("name") String name,
//                           @Param("color") String color, @Param("icon") String icon,
//                           @Param("type") String type);
    @Modifying
    @Query(value = "INSERT INTO categories (user_id, name, color, icon, category_type, created_at, updated_at, row_version) " +
           "VALUES (:userId, :name, :color, :icon, CAST(:type AS public.category_type), NOW(), NOW(), 1) " +
           "RETURNING id, user_id AS userId, name, color, icon, category_type AS type, created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, 'synced' AS syncStatus, row_version AS version", nativeQuery = true)
    Object[] createCategory(@Param("userId") UUID userId, @Param("name") String name, @Param("color") String color, 
                           @Param("icon") String icon, @Param("type") String type);
    
    @Modifying
    @Query(value = "UPDATE categories SET name = :name, color = :color, icon = :icon, category_type = CAST(:type AS public.category_type), updated_at = NOW(), row_version = row_version + 1 " +
           "WHERE id = :id AND user_id = :userId RETURNING id, user_id AS userId, name, color, icon, category_type AS type, created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, 'synced' AS syncStatus, row_version AS version", nativeQuery = true)
    Object[] updateCategory(UUID id, UUID userId, String name, String color, String icon, String type);
    
    @Modifying
    @Query(value = "UPDATE categories SET deleted_at = NOW(), updated_at = NOW(), " +
           "row_version = COALESCE(row_version, 0) + 1 WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(1) FROM categories WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int existsByUserAndId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT id, deleted_at FROM categories WHERE user_id = :userId " +
    	       "AND deleted_at IS NOT NULL AND deleted_at >= COALESCE(:since, deleted_at) " +
    	       "ORDER BY deleted_at DESC", nativeQuery = true)
    List<Map<String, Object>> findDeleted(@Param("userId") UUID userId, @Param("since") Instant since);
}