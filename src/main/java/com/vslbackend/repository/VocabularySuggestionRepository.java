package com.vslbackend.repository;

import com.vslbackend.entity.SuggestionStatus;
import com.vslbackend.entity.VocabularySuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularySuggestionRepository extends JpaRepository<VocabularySuggestion, Long> {

    @Query(value = "SELECT s FROM VocabularySuggestion s JOIN FETCH s.category JOIN FETCH s.requester",
           countQuery = "SELECT COUNT(s) FROM VocabularySuggestion s")
    Page<VocabularySuggestion> findAllWithDetails(Pageable pageable);

    @Query("SELECT s FROM VocabularySuggestion s JOIN FETCH s.category JOIN FETCH s.requester " +
           "WHERE s.requester.userId = :userId ORDER BY s.createdAt DESC")
    List<VocabularySuggestion> findMineWithDetails(@Param("userId") Long userId);

    long countByStatus(SuggestionStatus status);

    /**
     * Cac de xuat dang cho duyet - dung de chan de xuat TRUNG (khong phan biet hoa/thuong
     * va khong phan biet dau). So khop phai lam o Java qua foldVietnamese() nen lay ca danh
     * sach PENDING (thuc te rat nho) roi loc, giong cach findMatchingVocab() dang lam.
     */
    List<VocabularySuggestion> findByStatus(SuggestionStatus status);
}
