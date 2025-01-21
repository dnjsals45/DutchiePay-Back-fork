package dutchiepay.backend.domain.commerce.dto;

import com.querydsl.core.types.OrderSpecifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class OrderAndSortCondition {
    private final OrderSpecifier[] orderBy;
}
