package dutchiepay.backend.domain.chat.service;

import dutchiepay.backend.domain.chat.dto.*;
import dutchiepay.backend.domain.chat.exception.ChatErrorCode;
import dutchiepay.backend.domain.chat.exception.ChatException;
import dutchiepay.backend.domain.chat.repository.ChatRoomRepository;
import dutchiepay.backend.domain.chat.repository.MessageJdbcRepository;
import dutchiepay.backend.domain.chat.repository.MessageRepository;
import dutchiepay.backend.domain.community.service.MartService;
import dutchiepay.backend.domain.community.service.PurchaseService;
import dutchiepay.backend.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserChatroomService userChatroomService;
    private final MartService martService;
    private final PurchaseService purchaseService;
    private final RedisMessageService redisMessageService;

    private static final String CHAT_ROOM_PREFIX = "/sub/chat/";
    private final MessageJdbcRepository messageJdbcRepository;

    /**
     * 게시글에 연결된 채팅방에 참여한다.
     * @param user 유저
     * @param dto 게시글 Id 및 타입
     * @throws ChatException 채팅방이 가득 찼을 경우
     * @throws ChatException 유효하지 않은 타입일 경우
     * @throws ChatException 사용자가 채팅방에서 차단된 경우
     */
    @Transactional
    public JoinChatRoomResponseDto joinChatRoomFromPost(User user, JoinChatRoomRequestDto dto) {
        Long postId = dto.getPostId();
        String type = dto.getType();
        // postId 및 type 검증
        Object post = validatePostIdAndType(postId, type);

        // 채팅방이 존재하는지 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findByPostIdAndType(postId, type.equals("share") ? "group" : "direct");

        // 존재하지 않는다면 새로운 채팅방 생성
        if (chatRoom == null) {
            ChatRoom newChatRoom = createChatRoom(post, postId, type);

            // 유저가 게시글 작성자라면 1명만 참여, 작성자가 아니라면 작성자도 포함해서 참여시킨다.
            if (post instanceof Share s) {
                if (s.getUser().getUserId().equals(user.getUserId())) {
                    userChatroomService.joinChatRoom(user, newChatRoom, "manager");
                } else {
                    userChatroomService.joinChatRoom(user, newChatRoom, "member");
                    userChatroomService.joinChatRoom(s.getUser(), newChatRoom, "manager");
                }
            } else if (post instanceof Purchase p) {
                if (p.getUser().getUserId().equals(user.getUserId())) {
                    userChatroomService.joinChatRoom(user, newChatRoom, "manager");
                } else {
                    userChatroomService.joinChatRoom(user, newChatRoom, "member");
                    userChatroomService.joinChatRoom(p.getUser(), newChatRoom, "manager");
                }
            }

            return JoinChatRoomResponseDto.of(newChatRoom.getChatroomId(), newChatRoom.getChatRoomName(), newChatRoom.getNowPartInc());
        }
        // 유저가 이미 채팅방에 속해있는 지 검증
        if (userChatroomService.alreadyJoined(user, chatRoom)) {
            return JoinChatRoomResponseDto.of(chatRoom.getChatroomId(), chatRoom.getChatRoomName(), chatRoom.getNowPartInc());
        }

        // 채팅방의 인원이 가득 찼을 경우 예외처리
        if (chatRoom.getNowPartInc() >= chatRoom.getMaxPartInc()) {
            throw new ChatException(ChatErrorCode.FULL_CHAT);
        }

        Boolean isBanned = userChatroomService.isBanned(user.getUserId(), chatRoom.getChatroomId());
        // 블랙리스트 여부 확인
        if (isBanned != null && isBanned) {
            throw new ChatException(ChatErrorCode.USER_BANNED);
        }

        // 채팅방에 유저 참여
        userChatroomService.joinChatRoom(user, chatRoom, "member");

        return JoinChatRoomResponseDto.of(chatRoom.getChatroomId(), chatRoom.getChatRoomName(), chatRoom.getNowPartInc());
    }

    /**
     * 채팅방을 나간다.
     * @param user 유저
     * @param chatRoomId 채팅방 Id
     * @throws ChatException 채팅방이 존재하지 않을 경우
     * @throws ChatException 채팅방 방장이 나가려고 할 경우
     */
    @Transactional
    public void leaveChatRoom(User user, Long chatRoomId) {
        UserChatRoom ucr = userChatroomService.findByUserAndChatRoomId(user, chatRoomId);

        if (ucr.getRole().equals("manager")) {
            throw new ChatException(ChatErrorCode.MANAGER_CANNOT_LEAVE);
        }

        userChatroomService.leaveChatRoom(ucr);
        ucr.getChatroom().leave();
    }

    /**
     * 채팅방을 생성한다.
     * @param post 게시글 object
     * @param postId 게시글 postId
     * @param type 게시글 타입
     * @return ChatRoom 생성된 채팅방
     */
    private ChatRoom createChatRoom(Object post, Long postId, String type) {
        int maxPartInc = 2;

        // 마트/배달일 경우 최대 인원 데이터를 가져온다.
        if (post instanceof Share s) {
            maxPartInc = s.getMaximum();
        }

        String chatRoomImg = null;

        if (post instanceof Share s) {
            chatRoomImg = s.getThumbnail();
        } else if (post instanceof Purchase p) {
            chatRoomImg = p.getThumbnail();
        }

        // 채팅방을 생성한다.
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(post instanceof Share ? ((Share) post).getTitle() : ((Purchase) post).getTitle())
                .chatRoomImg(chatRoomImg)
                .postId(postId)
                .type(type.equals("share") ? "group" : "direct")
                .maxPartInc(maxPartInc)
                .nowPartInc(0)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 게시글 ID 및 타입을 검증하고 검증된 게시글 Obejct를 반환한다.
     * @param postId 게시글Id
     * @param type 게시글타입
     * @return 타입에 맞는 게시글 Object
     */
    private Object validatePostIdAndType(Long postId, String type) {
        if (type.equals("share")) return martService.findById(postId);
        else if (type.equals("purchase")) return purchaseService.findById(postId);
        else throw new ChatException(ChatErrorCode.INVALID_TYPE);
    }

    /**
     * 채팅방 구독을 시도한 유저에게 채팅방 정보를 전송한다.
     * 메시지의 경우 채팅방에 속한 모든 유저에게 전달되며, 클라이언트 측에서 해당하는 유저에서만 데이터를 처리한다.
     * @param userId 유저 Id
     * @param chatRoomId 채팅방 Id
     * @throws ChatException 채팅방이 존재하지 않을 경우
     */
    public void sendChatRoomInfo(String userId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_CHAT));

        boolean isBanned = userChatroomService.isBanned(Long.valueOf(userId), chatRoomId);
        ChatRoomInfoResponse chatRoomInfo = ChatRoomInfoResponse.from(Long.valueOf(userId), chatRoom, !isBanned);

        simpMessagingTemplate.convertAndSend(CHAT_ROOM_PREFIX + chatRoomId, chatRoomInfo);
    }

    /**
     * 채팅방에 메시지를 전송한다.
     * @param chatRoomId 채팅방 Id
     * @param message 메시지 객체
     * @throws ChatException 채팅방이 존재하지 않을 경우
     */
    @Transactional
    public void sendToChatRoomUser(String chatRoomId, ChatMessage message) {
        ChatRoom chatRoom = chatRoomRepository.findById(Long.parseLong(chatRoomId))
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_CHAT));

        Message newMessage = Message.builder()
                .chatroom(chatRoom)
                .type(message.getType())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .date(LocalDate.parse(message.getDate(), DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")).format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .time(message.getTime())
                .unreadCount(chatRoom.getNowPartInc() - getSubscribedUserCount(chatRoomId))
                .build();

        messageRepository.save(newMessage);

        redisMessageService.saveMessage(chatRoomId, newMessage);
        updateLastMessageToAllSubscribers(chatRoomId, newMessage.getMessageId());

        simpMessagingTemplate.convertAndSend(CHAT_ROOM_PREFIX + chatRoomId, MessageResponse.of(newMessage));
    }

    /**
     * 채팅방 목록을 가져온다.
     * @param user 유저
     * @return 채팅방 목록 Dto
     */
    public List<GetChatRoomListResponseDto> getChatRoomList(User user) {
        return userChatroomService.getChatRoomList(user);
    }

    private int getSubscribedUserCount(String chatRoomId) {
        String destination = CHAT_ROOM_PREFIX + chatRoomId;
        int count = 0;

        for (SimpUser user : simpUserRegistry.getUsers()) {
            for (SimpSession session : user.getSessions()) {
                for (SimpSubscription subscription : session.getSubscriptions()) {
                    if (destination.equals(subscription.getDestination())) {
                        count++;
                        break;
                    }
                }
            }
        }

        return count;
    }

    public void kickUser(User user, KickUserRequestDto dto) {
        UserChatRoom ucr = userChatroomService.findByUserAndChatRoomId(user, dto.getChatRoomId());

        if (!ucr.getRole().equals("manager")) {
            throw new ChatException(ChatErrorCode.NOT_MANAGER);
        }

        for (Long userId : dto.getUserId()) {
            UserChatRoom target = userChatroomService.findByUserUserIdAndChatRoomId(userId, dto.getChatRoomId());
            userChatroomService.kickedChatRoom(target);
        }
    }

    public List<GetChatRoomUsersResponseDto> getChatRoomUsers(Long chatRoomId) {
        return userChatroomService.getChatRoomUsers(chatRoomId);
    }

    public GetMessageListResponseDto getChatRoomMessages(Long chatRoomId, String cursor, Long limit) {
        String cursorDate;
        Long cursorMessageId = null;

        if (cursor == null) {
            cursorDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else {
            cursorDate = cursor.substring(0, 8);
            cursorMessageId = Long.parseLong(cursor.substring(8)) != 0 ? Long.parseLong(cursor.substring(8)) : null;
        }

        LocalDate currentDate = LocalDate.now();
        LocalDate requestDate = LocalDate.parse(cursorDate,
                DateTimeFormatter.ofPattern("yyyyMMdd"));

        long daysDifference = ChronoUnit.DAYS.between(requestDate, currentDate);

        if (daysDifference <= 7) {
            GetMessageListResponseDto redisMessages =
                    redisMessageService.getMessageFromMemory(chatRoomId, cursorDate, cursorMessageId, limit);

            if (redisMessages != null) {
                return redisMessages;
            }
        }

        return chatRoomRepository.findChatRoomMessages(chatRoomId, cursorDate, cursorMessageId, limit);
    }

    public GetMessageListResponseDto getChatRoomMessagesFromDB(Long chatRoomId, String cursor, Long limit) {
        String cursorDate;
        Long cursorMessageId = null;

        if (cursor == null) {
            cursorDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else {
            cursorDate = cursor.substring(0, 8);
            cursorMessageId = Long.parseLong(cursor.substring(8)) != 0 ? Long.parseLong(cursor.substring(8)) : null;
        }

        return chatRoomRepository.findChatRoomMessages(chatRoomId, cursorDate, cursorMessageId, limit);
    }

    public GetMessageListResponseDto getChatRoomMessagesFromRedis(Long chatRoomId, String cursor, Long limit) {
        String cursorDate;
        Long cursorMessageId = null;

        if (cursor == null) {
            cursorDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else {
            cursorDate = cursor.substring(0, 8);
            cursorMessageId = Long.parseLong(cursor.substring(8)) != 0 ? Long.parseLong(cursor.substring(8)) : null;
        }

        return redisMessageService.getMessageFromMemory(chatRoomId, cursorDate, cursorMessageId, limit);
    }

    public void checkCursorId(Long chatRoomId, Long userId) {
        Long cursor = messageRepository.findCursorId(chatRoomId, userId);

        if (cursor != null) {
            simpMessagingTemplate.convertAndSend(CHAT_ROOM_PREFIX + chatRoomId, CursorResponse.of(cursor));
            userChatroomService.updateLastMessageToUser(userId, chatRoomId);
            redisMessageService.decreaseUnreadCountWithCursor(chatRoomId, cursor);
        } else {
            simpMessagingTemplate.convertAndSend(CHAT_ROOM_PREFIX + chatRoomId, CursorResponse.of(0L));
            userChatroomService.updateLastMessageToUser(userId, chatRoomId);
        }
    }

    private void updateLastMessageToAllSubscribers(String chatRoomId, Long messageId) {
        List<Long> userIds = new ArrayList<>();

        for (SimpUser user : simpUserRegistry.getUsers()) {
            for (SimpSession session : user.getSessions()) {
                for (SimpSubscription subscription : session.getSubscriptions()) {
                    if (subscription.getDestination().equals(CHAT_ROOM_PREFIX + chatRoomId)) {
                        userIds.add(Long.parseLong(user.getName()));
                    }
                }
            }
        }

        userChatroomService.updateLastMessageToAllSubscribers(userIds, Long.parseLong(chatRoomId), messageId);
    }

    public void saveChatMessageToDB(Long chatRoomId, int size) {
        List<Message> messages = new ArrayList<>();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_CHAT));

        for (int i = 0; i < size; i++) {
            messages.add(Message.builder()
                            .chatroom(chatRoom)
                            .type("text")
                            .senderId(1L)
                            .content("테스트 메시지" + i)
                            .date(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .time("00:0" + i)
                            .build());
        }

        messages = messageJdbcRepository.bulkInsert(messages, chatRoomId);

        for (Message message : messages) {
            redisMessageService.saveMessage(chatRoomId.toString(), message);
        }
    }
}
