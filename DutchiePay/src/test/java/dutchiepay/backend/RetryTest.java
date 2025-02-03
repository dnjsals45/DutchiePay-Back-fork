//package dutchiepay.backend;
//
//import dutchiepay.backend.domain.commerce.repository.BuyRepository;
//import dutchiepay.backend.domain.order.repository.OrderRepository;
//import dutchiepay.backend.entity.Buy;
//import dutchiepay.backend.entity.User;
//import dutchiepay.backend.global.config.RetryConfig;
//import dutchiepay.backend.global.payment.dto.kakao.KakaoPayReadyResponseDto;
//import dutchiepay.backend.global.payment.dto.kakao.ReadyRequestDto;
//import dutchiepay.backend.global.payment.dto.kakao.ReadyResponseDto;
//import dutchiepay.backend.global.payment.exception.PaymentErrorException;
//import dutchiepay.backend.global.payment.service.KakaoPayRequestService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.HttpStatusCodeException;
//import org.springframework.web.client.RestTemplate;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RetryTest {
//
//    @Mock
//    private BuyRepository buyRepository;
//
//    @Mock
//    private OrderRepository ordersRepository;
//
//    @Mock
//    private RestTemplate restTemplate;
//
//    @InjectMocks
//    private KakaoPayRequestService kakaoPayRequestService;
//
//    @Spy
//    private RetryTemplate kakaoPayRetryTemplate = new RetryConfig().kakaopayRetryTemplate();
//
//    @Value("${payment.kakao.cid}")
//    private String cid;
//
//    @BeforeEach
//    void setUp() {
//        ReflectionTestUtils.setField(kakaoPayRequestService, "cid", "TC0ONETIME");
//        ReflectionTestUtils.setField(kakaoPayRequestService, "secretKey", "DEV4EB2F06B7F77C543499EBCF0E1C9B10125F22");
//        ReflectionTestUtils.setField(kakaoPayRequestService, "backendHost", "http://localhost:8080");
//    }
//
//    @Test
//    @DisplayName("결제 준비 성공 시나리오")
//    void ready_Success() {
//        // Given
//        User user = createTestUser();
//        ReadyRequestDto request = createTestReadyRequest();
//        Buy buy = createTestBuy();
//        ReadyResponseDto expectedResponse = createTestReadyResponse();
//
//        when(buyRepository.findById(any())).thenReturn(Optional.of(buy));
//        when(ordersRepository.existsByOrderNum(any())).thenReturn(false);
//        when(restTemplate.postForEntity(
//                eq("https://open-api.kakaopay.com/online/v1/payment/ready"),
//                any(),
//                eq(ReadyResponseDto.class)
//        )).thenReturn(ResponseEntity.ok(expectedResponse));
//
//        // When
//        KakaoPayReadyResponseDto result = kakaoPayRequestService.ready(user, request);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(expectedResponse.getNext_redirect_pc_url(), result.getRedirectUrl());
//        verify(ordersRepository, times(1)).save(any(Order.class));
//    }
//
//    @Test
//    @DisplayName("진행중인 거래 에러 발생 시나리오")
//    void ready_WhenUserLocked() {
//        // Given
//        User user = createTestUser();
//        ReadyRequestDto request = createTestReadyRequest();
//        Buy buy = createTestBuy();
//
//        HttpStatusCodeException exception = new HttpClientErrorException(
//                HttpStatus.BAD_REQUEST,
//                "Bad Request",
//                "{ \"error_code\": -780, \"error_message\": \"approval failure!\", \"extras\": { \"method_result_code\": \"USER_LOCKED\", \"method_result_message\": \"진행중인 거래가 있습니다.\" } }".getBytes(),
//                StandardCharsets.UTF_8
//        );
//
//        when(buyRepository.findById(any())).thenReturn(Optional.of(buy));
//        when(ordersRepository.existsByOrderNum(any())).thenReturn(false);
//        when(restTemplate.postForEntity(
//                eq("https://open-api.kakaopay.com/online/v1/payment/ready"),
//                any(),
//                eq(ReadyResponseDto.class)
//        )).thenThrow(exception);
//
//        // When & Then
//        assertThrows(PaymentErrorException.class, () -> kakaoPayRequestService.ready(user, request));
//        verify(ordersRepository, times(1)).save(any(Order.class));
//    }
//
//    @Test
//    @DisplayName("네트워크 오류로 인한 재시도 시나리오")
//    void ready_WithRetry() {
//        // Given
//        User user = createTestUser();
//        ReadyRequestDto request = createTestReadyRequest();
//        Buy buy = createTestBuy();
//        ReadyResponseDto expectedResponse = createTestReadyResponse();
//
//        HttpStatusCodeException networkException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
//
//        when(buyRepository.findById(any())).thenReturn(Optional.of(buy));
//        when(ordersRepository.existsByOrderNum(any())).thenReturn(false);
//        when(restTemplate.postForEntity(
//                eq("https://open-api.kakaopay.com/online/v1/payment/ready"),
//                any(),
//                eq(ReadyResponseDto.class)
//        ))
//                .thenThrow(networkException)
//                .thenThrow(networkException)
//                .thenReturn(ResponseEntity.ok(expectedResponse));
//
//        // When
//        KakaoPayReadyResponseDto result = kakaoPayRequestService.ready(user, request);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(expectedResponse.getNext_redirect_pc_url(), result.getRedirectUrl());
//        verify(restTemplate, times(3)).postForEntity(any(), any(), any());
//    }
//
//    private User createTestUser() {
//        return User.builder()
//                .nickname("testUser")
//                .build();
//    }
//
//    private ReadyRequestDto createTestReadyRequest() {
//        return ReadyRequestDto.builder()
//                .buyId(1L)
//                .productName("테스트상품")
//                .quantity(1)
//                .totalAmount(10000)
//                .taxFreeAmount(0)
//                .receiver("수신자")
//                .phone("010-1234-5678")
//                .address("서울시 강남구")
//                .build();
//    }
//
//    private Buy createTestBuy() {
//        return Buy.builder()
//                .id(1L)
//                .product(new Product())
//                .build();
//    }
//
//    private ReadyResponseDto createTestReadyResponse() {
//        ReadyResponseDto response = new ReadyResponseDto();
//        ReflectionTestUtils.setField(response, "tid", "TEST_TID");
//        ReflectionTestUtils.setField(response, "next_redirect_pc_url", "http://test-redirect-url");
//        return response;
//    }
//}
