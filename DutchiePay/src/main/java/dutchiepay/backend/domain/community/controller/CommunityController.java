package dutchiepay.backend.domain.community.controller;

import dutchiepay.backend.domain.community.dto.ChangeStatusRequestDto;
import dutchiepay.backend.domain.community.exception.CommunityErrorCode;
import dutchiepay.backend.domain.community.exception.CommunityException;
import dutchiepay.backend.domain.community.service.CommunityService;
import dutchiepay.backend.domain.community.service.MartService;
import dutchiepay.backend.domain.community.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityController {
    private final CommunityService communityService;
    private final MartService martService;
    private final PurchaseService purchaseService;

    @Operation(summary = "최근 거래 완료글 조회")
    @GetMapping("/recent-post")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserCompleteRecentDeals(@RequestParam Long userId) {
        return ResponseEntity.ok().body(communityService.getUserCompleteRecentDeals(userId));
    }


    @PatchMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changeStatus(@RequestBody ChangeStatusRequestDto req) {
        if (req.getCategory().equals("마트/배달")) {
            martService.changeStatus(req);
            return ResponseEntity.ok().build();
        } else if (req.getCategory().equals("나눔/거래")) {
            purchaseService.changeStatus(req);
            return ResponseEntity.ok().build();
        }
        throw new CommunityException(CommunityErrorCode.INVALID_POST_TYPE);
    }

}
