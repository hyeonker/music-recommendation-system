package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최신순/오름차순은 Pageable의 Sort로 제어
    List<ChatMessage> findByRoomId(Long roomId, Pageable pageable);

    @Modifying
    @Query("delete from ChatMessage m where m.createdAt < :threshold and m.holdForDispute = false")
    int deleteOlderThan(@Param("threshold") OffsetDateTime threshold);
}
