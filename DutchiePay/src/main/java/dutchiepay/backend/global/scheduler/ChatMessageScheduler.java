package dutchiepay.backend.global.scheduler;

import dutchiepay.backend.domain.chat.dto.MessageResponse;
import dutchiepay.backend.domain.chat.repository.MessageJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageScheduler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageJdbcRepository messageJdbcRepository;

    private static final String CHAT_KEY_PREFIX = "chat:";
    private static final String MESSAGES_SUFFIX = ":messages:";
    private static final int RETENTION_DAYS = 6;

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "syncMessageToDB", lockAtMostFor = "30m")
    @Transactional
    public void syncMessageToDB() {
        log.info("채팅 메시지 동기화 스케쥴링 시작");
        String pattern = CHAT_KEY_PREFIX + "*" + MESSAGES_SUFFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            log.info("동기화할 메시지가 없습니다.");
            return;
        }

        for (String key : keys) {
            log.info("동기화할 채팅방 키: {}", key);
            syncMessageFromKey(key);
        }
        log.info("채팅 메시지 동기화 스케쥴링 종료");
    }

    private void syncMessageFromKey(String key) {
        Set<Object> messages = redisTemplate.opsForZSet().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<MessageResponse> messageResponseList = new ArrayList<>();
        for (Object obj : messages) {
            MessageResponse mr = (MessageResponse) obj;
            messageResponseList.add(mr);
        }

        messageJdbcRepository.syncMessage(messageResponseList, Long.parseLong(key.split(":")[1]));
    }

    @Scheduled(cron = "0 0 4 * * ?")
    @SchedulerLock(name = "cleanupOldMessages", lockAtMostFor = "30m")
    public void cleanupOldMessages() {
        log.info("Redis 메시지 정리 스케줄링 시작");
        String pattern = CHAT_KEY_PREFIX + "*" + MESSAGES_SUFFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            log.info("삭제할 메시지가 없습니다.");
            return;
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS);

        for (String key : keys) {
            deleteIfOldMessage(key, cutoffDate);
        }
        log.info("Redis 메시지 정리 스케줄링 종료");
    }

    private void deleteIfOldMessage(String key, LocalDate cutoffDate) {
        String dateStr = key.substring(key.lastIndexOf(":") + 1);
        LocalDate messageDate = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);

        if (messageDate.isBefore(cutoffDate)) {
            redisTemplate.delete(key);
        }
    }
}
