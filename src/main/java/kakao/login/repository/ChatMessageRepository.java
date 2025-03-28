package kakao.login.repository;

import kakao.login.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 특정 채팅방의 메시지를 페이지네이션하여 조회
     */
    Page<ChatMessage> findByRoomId(Long roomId, Pageable pageable);

    /**
     * 특정 채팅방의 메시지를 모두 조회
     */
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(Long roomId);

    // 수정 (timestamp 필드 사용)
    List<ChatMessage> findTop50ByRoomIdOrderByTimestampDesc(Long roomId);

    /**
     * 특정 사용자의 읽지 않은 메시지 조회
     */
    @Query("{'roomId': {'$in': ?0}, 'readBy': {'$ne': ?1}, 'senderId': {'$ne': ?1}}")
    List<ChatMessage> findUnreadMessagesByUserId(List<String> roomIds, String userId);

    /**
     * 특정 기간 동안의 메시지 조회
     */
    List<ChatMessage> findByRoomIdAndTimestampBetween(Long roomId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 최신 두 개 메시지를 역순으로 조회 (날짜 구분용)
     */
    List<ChatMessage> findTop2ByRoomIdOrderByTimestampDesc(Long roomId);


    /**
     * 특정 키워드가 포함된 메시지 검색
     */
    List<ChatMessage> findByContentContaining(String keyword);

    /**
     * 특정 사용자가 보낸 메시지 조회
     */
    List<ChatMessage> findBySenderId(String senderId);

    /**
     * 메시지를 읽음 처리 (커스텀 쿼리 메서드)
     */

    @Query(
            value = "{ 'roomId': ?0, 'readBy': { $ne: ?1 }, 'senderId': { $ne: ?1 }, " +
                    "'deleted': false, 'senderName': { $ne: '시스템' } }",
            count = true
    )
    long countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("userId") String userId
    );

    // 새로운 쿼리: joinedAt 이후의 메시지만 조회하고 exitMessage가 true인 시스템 메시지 제외
    @Query(
            value = "{ 'roomId': ?0, 'readBy': { $nin: [?1] }, 'senderId': { $ne: ?1 }, " +
                    "'deleted': false, 'timestamp': { $gt: ?2 }, " +
                    "$or: [" +
                    "  { 'senderName': { $ne: '시스템' } }, " +
                    "  { $and: [" +
                    "      { 'senderName': '시스템' }, " +
                    "      { 'exitMessage': { $ne: true } }" +
                    "    ]}" +
                    "] }",
            count = true
    )
    long countUnreadMessagesAfterJoin(
            @Param("roomId") Long roomId,
            @Param("userId") String userId,
            @Param("joinedAt") Date joinedAt
    );
}