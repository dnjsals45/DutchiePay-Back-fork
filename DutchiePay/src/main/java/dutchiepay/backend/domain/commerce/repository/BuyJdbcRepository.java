package dutchiepay.backend.domain.commerce.repository;

import dutchiepay.backend.entity.Buy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BuyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void bulkInsert(List<Buy> buyList) {
        String sql = "INSERT INTO buy (product_id, title, deadline, skeleton, now_count, tags, " +
                "created_at, updated_at, deleted_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int size = buyList.size();
        jdbcTemplate.batchUpdate(sql, buyList, size, (PreparedStatement ps, Buy buy) -> {
            ps.setLong(1, buy.getProduct().getProductId());
            ps.setString(2, buy.getTitle());
            ps.setDate(3, java.sql.Date.valueOf(buy.getDeadline()));
            ps.setInt(4, buy.getSkeleton());
            ps.setInt(5, buy.getNowCount());
            ps.setString(6, buy.getTags());
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, buy.getDeletedAt() != null ? java.sql.Timestamp.valueOf(LocalDateTime.now()) : null);
        });
    }
}
