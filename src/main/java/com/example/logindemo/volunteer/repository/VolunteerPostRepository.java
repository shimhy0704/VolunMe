package com.example.logindemo.volunteer.repository;

import com.example.logindemo.volunteer.entity.VolunteerPost;
import com.example.logindemo.volunteer.entity.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// VolunteerPostRepository.java
@Repository
public interface VolunteerPostRepository extends JpaRepository<VolunteerPost, Long> {

    @Query("""
        SELECT v
          FROM VolunteerPost v
         WHERE v.lat BETWEEN :minLat AND :maxLat
           AND v.lng BETWEEN :minLng AND :maxLng
           AND (:status IS NULL OR v.status = :status)
           AND (:category IS NULL OR v.category = :category)
           AND (
                 :q IS NULL
              OR LOWER(v.title)   LIKE CONCAT('%', LOWER(:q), '%')
              OR LOWER(v.address) LIKE CONCAT('%', LOWER(:q), '%')
              OR (v.description IS NOT NULL AND v.description LIKE CONCAT('%', :q, '%'))
           )
    """)
    List<VolunteerPost> findInBBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("status") VolunteerStatus status,
            @Param("category") String category,
            @Param("q") String q
    );
}
