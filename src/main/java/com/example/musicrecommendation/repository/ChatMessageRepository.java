package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최신순/오름차순은 Pageable의 Sort로 제어
    List<ChatMessage> findByRoomId(String roomId, Pageable pageable);

    @Modifying
    @Query("delete from ChatMessage m where m.createdAt < :threshold and m.holdForDispute = false")
    int deleteOlderThan(@Param("threshold") OffsetDateTime threshold);
    
    @Query("SELECT DISTINCT m.senderId FROM ChatMessage m WHERE m.roomId = :roomId")
    List<Long> findDistinctSenderIdByRoomId(@Param("roomId") String roomId);
    
    /**
     * 신고 시점까지의 채팅 메시지 조회 (문자열 roomId 지원)
     */
    @Query(value = "SELECT m.id, m.content_cipher, m.iv_base64, m.sender_id, m.created_at " +
                   "FROM chat_message m " +
                   "WHERE m.room_id = :roomId " +
                   "AND m.created_at <= :reportTime " +
                   "ORDER BY m.created_at ASC",
           nativeQuery = true)
    List<Object[]> findMessagesByRoomIdBeforeTime(@Param("roomId") String roomId, 
                                                  @Param("reportTime") LocalDateTime reportTime);
    
    /**
     * 디버그용 - 최근 채팅 메시지 조회
     */
    @Query(value = "SELECT m.id, m.room_id, m.sender_id, m.created_at " +
                   "FROM chat_message m " +
                   "ORDER BY m.created_at DESC " +
                   "LIMIT 20",
           nativeQuery = true)
    List<Object[]> findRecentMessagesForDebug();
}
