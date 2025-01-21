package dutchiepay.backend.domain.commerce.controller;

import dutchiepay.backend.domain.commerce.dto.AddEntityDto;
import dutchiepay.backend.domain.commerce.dto.PaymentInfoResponseDto;
import dutchiepay.backend.domain.commerce.service.CommerceService;
import dutchiepay.backend.domain.commerce.dto.BuyAskResponseDto;
import dutchiepay.backend.entity.User;
import dutchiepay.backend.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/commerce")
@RequiredArgsConstructor
public class CommerceController {

    private final CommerceService commerceService;

    @Operation(summary = "공동구매 리스트 조회")
    @GetMapping(value = "/list")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBuyList(@RequestParam("filter") String filter,
                                        @RequestParam(value = "category", required = false) String category,
                                        @RequestParam("end") int end,
                                        @RequestParam(value = "cursor", required = false) Long cursor,
                                        @RequestParam("limit") int limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            user = userDetails.getUser();
        }

        return ResponseEntity.ok().body(commerceService.getBuyList(user, filter, category, end, cursor, limit));
    }

    @Operation(summary = "공동구매 리스트 조회 페이징")
    @GetMapping(value = "/list/page")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBuyListPage(@RequestParam("filter") String filter,
                                            @RequestParam(value = "category", required = false) String category,
                                            @RequestParam("end") int end,
                                            @RequestParam(value = "page") int page) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            user = userDetails.getUser();
        }

        return ResponseEntity.ok().body(commerceService.getBuyListPage(user, filter, category, end, page));
    }

    @Operation(summary = "공동구매 상품 상세 페이지")
    @GetMapping(value = "", params = "buyId")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBuyPage(@RequestParam("buyId") Long buyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            user = userDetails.getUser();
        }

        return ResponseEntity.ok().body(commerceService.getBuyPage(user, buyId));
    }

    @Operation(summary = "상품 후기 조회")
    @GetMapping("/review")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getProductReview(@RequestParam("buyId") Long buyId,
                                              @RequestParam("photo") Long photo,
                                              @RequestParam("page") Long page,
                                              @RequestParam("limit") Long limit) {
        return ResponseEntity.ok().body(commerceService.getProductReview(buyId, photo, page, limit));
    }

    @Operation(summary = "상품 좋아요 기능")
    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likes(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                @RequestBody Map<String, Long> requestBody) {
        commerceService.likes(userDetails, requestBody.get("buyId"));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 문의내역 조회")
    @GetMapping("/asks")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<BuyAskResponseDto>> getBuyAsks(@RequestParam("buyId") Long buyId,
                                                              @RequestParam("page") int page,
                                                              @RequestParam("limit") int limit) {
        return ResponseEntity.ok(commerceService.getBuyAsks(buyId, page, limit));
    }

    @Operation(summary = "결제 정보 불러오기")
    @GetMapping("/delivery")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentInfoResponseDto> getPaymentInfo(@RequestParam("buyId") Long buyId) {
        return ResponseEntity.ok(commerceService.getPaymentInfo(buyId));
    }

    @PostMapping("/addition")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> addEntity(@RequestParam("size") int size) {
        commerceService.addEntity(size);
        return ResponseEntity.ok().build();
    }
}
