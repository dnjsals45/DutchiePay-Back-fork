package dutchiepay.backend.domain.commerce.repository;

import dutchiepay.backend.domain.commerce.dto.GetBuyListResponseDto;
import dutchiepay.backend.domain.commerce.dto.GetBuyResponseDto;
import dutchiepay.backend.domain.commerce.dto.GetProductReviewResponseDto;
import dutchiepay.backend.entity.User;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface QBuyRepository {
    GetBuyResponseDto getBuyPageByBuyId(User user, Long buyId);

    GetBuyListResponseDto getBuyList(User user, String filter, String categoryName, String word, int end, Long cursor, int limit);

    List<GetProductReviewResponseDto> getProductReview(Long buyId, Long photo, PageRequest pageable);

    GetBuyListResponseDto getBuyListPage(User user, String filter, String category, String word, int end, int page);
}
