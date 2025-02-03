package dutchiepay.backend.domain.chat.controller;

import dutchiepay.backend.domain.chat.dto.ChatMessage;
import dutchiepay.backend.domain.chat.dto.JoinChatRoomRequestDto;
import dutchiepay.backend.domain.chat.dto.KickUserRequestDto;
import dutchiepay.backend.domain.chat.service.ChatRoomService;
import dutchiepay.backend.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatRoomService chatroomService;

    @MessageMapping("/chat/{chatRoomId}")
    public ChatMessage chat(@DestinationVariable String chatRoomId, ChatMessage message) {
        chatroomService.sendToChatRoomUser(chatRoomId, message);
        return message;
    }

    @Operation(summary = "채팅방 입장", description = "postId에 연결된 채팅방 입장")
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinChatRoomFromPost(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                  @RequestBody JoinChatRoomRequestDto dto) {
        return ResponseEntity.ok().body(chatroomService.joinChatRoomFromPost(userDetails.getUser(), dto));
    }

    @Operation(summary = "채팅방 나가기")
    @DeleteMapping("")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> leaveChatRoom(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                           @RequestParam Long chatRoomId) {
        chatroomService.leaveChatRoom(userDetails.getUser(), chatRoomId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/chatRoomList")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getChatRoomList(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(chatroomService.getChatRoomList(userDetails.getUser()));
    }

    @Operation(summary = "사용자 내보내기")
    @PostMapping("/kick")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> kickUser(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                      @RequestBody KickUserRequestDto dto) {
        chatroomService.kickUser(userDetails.getUser(), dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅 사용자 목록 조회")
    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getChatRoomUsers(@RequestParam Long chatRoomId) {
        return ResponseEntity.ok(chatroomService.getChatRoomUsers(chatRoomId));
    }

    @Operation(summary = "채팅방 메시지 목록 조회")
    @GetMapping("/message")
//    @PreAuthorize("isAuthenticated()")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getChatRoomMessages(@RequestParam(value = "chatRoomId") Long chatRoomId,
                                                 @RequestParam(value = "cursor", required = false) String cursor,
                                                 @RequestParam(value = "limit") Long limit) {
        return ResponseEntity.ok(chatroomService.getChatRoomMessages(chatRoomId, cursor, limit));
    }

    @Operation(summary = "채팅 메시지 더미데이터 생성")
    @GetMapping("/save/test")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> saveChatMessageToDB(@RequestParam(value = "chatRoomId") Long chatRoomId,
                                                 @RequestParam(value = "size") int size) {
        chatroomService.saveChatMessageToDB(chatRoomId, size);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "DB에서 데이터 조회 테스트")
    @GetMapping("/from/db")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getChatRoomMessagesFromDB(@RequestParam(value = "chatRoomId") Long chatRoomId,
                                                       @RequestParam(value = "cursor", required = false) String cursor,
                                                       @RequestParam(value = "limit") Long limit) {
        return ResponseEntity.ok(chatroomService.getChatRoomMessagesFromDB(chatRoomId, cursor, limit));
    }

    @Operation(summary = "Redis에서 데이터 조회 테스트")
    @GetMapping("/from/redis")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getChatRoomMessagesFromRedis(@RequestParam(value = "chatRoomId") Long chatRoomId,
                                                          @RequestParam(value = "cursor", required = false) String cursor,
                                                          @RequestParam(value = "limit") Long limit) {
        return ResponseEntity.ok(chatroomService.getChatRoomMessagesFromRedis(chatRoomId, cursor, limit));
    }
}
