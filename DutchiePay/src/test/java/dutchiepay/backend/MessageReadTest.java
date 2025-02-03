package dutchiepay.backend;

import dutchiepay.backend.domain.chat.dto.GetMessageListResponseDto;
import dutchiepay.backend.domain.chat.service.ChatRoomService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessageReadTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Test
    @Order(1)
    void contextLoads() {
    }

    @Test
    @Order(2)
    void DB_메시지조회() {
        Long chatRoomId = 16L;

        String cursor = null;

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            GetMessageListResponseDto result = chatRoomService.getChatRoomMessagesFromDB(chatRoomId, cursor, 50L);

            cursor = result.getCursor();
        }

        System.out.println("cursor = " + cursor);

        long end = System.currentTimeMillis();
        System.out.println("실행 시간 " + (end - start) + "ms");
    }

    @Test
    @Disabled
    @Order(3)
    void Reds_메시지조회() {
        Long chatRoomId = 16L;

        String cursor = null;

        long start = System.currentTimeMillis();

        for (int i = 0; i < 2000; i++) {
            GetMessageListResponseDto result = chatRoomService.getChatRoomMessagesFromRedis(chatRoomId, cursor, 50L);

            cursor = result.getCursor();
        }

        long end = System.currentTimeMillis();
        System.out.println("실행 시간 " + (end - start) + "ms");
    }
}
