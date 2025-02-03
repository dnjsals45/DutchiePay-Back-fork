package dutchiepay.backend.domain.chat.repository;

import dutchiepay.backend.domain.chat.dto.MessageResponse;
import dutchiepay.backend.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<Message> bulkInsert(List<Message> messageList, Long chatRoomId) {
        String sql = "INSERT INTO message (chatroom_id, type ,sender_id, content, date, time, unread_count, created_at, updated_at, deleted_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int size = messageList.size();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        List<Message> resultList = new ArrayList<>();

        for (Message message : messageList) {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, chatRoomId);
                ps.setString(2, message.getType());
                ps.setLong(3, message.getSenderId());
                ps.setString(4, message.getContent());
                ps.setString(5, message.getDate());
                ps.setString(6, message.getTime());
                ps.setInt(7, message.getUnreadCount());
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(10, message.getDeletedAt() != null ? java.sql.Timestamp.valueOf(LocalDateTime.now()) : null);

                return ps;
            }, keyHolder);

            Message savedMessage = Message.builder()
                    .messageId(keyHolder.getKey().longValue())
                    .chatroom(message.getChatroom())
                    .type(message.getType())
                    .senderId(message.getSenderId())
                    .content(message.getContent())
                    .date(message.getDate())
                    .time(message.getTime())
                    .unreadCount(message.getUnreadCount())
                    .build();

            resultList.add(savedMessage);
        }

        return resultList;
    }

    @Transactional
    public void syncMessage(List<MessageResponse> messageResponseList, Long chatRoomId) {
        String sql = """
            INSERT INTO message (
                message_id, chatroom_id, sender_id, type, content, unread_count, 
                date, time, created_at, updated_at, deleted_at
            )
            VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, NOW(), NOW(), NULL
            )
            ON DUPLICATE KEY UPDATE
                sender_id = VALUES(sender_id),
                chatroom_id = VALUES(chatroom_id),
                type = VALUES(type),
                content = VALUES(content),
                unread_count = VALUES(unread_count),
                date = VALUES(date),
                time = VALUES(time),
                updated_at = NOW()
        """;

        int size = messageResponseList.size();
        jdbcTemplate.batchUpdate(sql, messageResponseList, size, (ps, message) -> {
            ps.setLong(1, message.getMessageId());
            ps.setLong(2, chatRoomId);
            ps.setLong(3, message.getSenderId());
            ps.setString(4, message.getType());
            ps.setString(5, message.getContent());
            ps.setInt(6, message.getUnreadCount());
            ps.setString(7, message.getDate());
            ps.setString(8, message.getTime());
        });
    }
}
