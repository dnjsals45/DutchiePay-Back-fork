package dutchiepay.backend.domain.commerce.repository;

import dutchiepay.backend.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StoreJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<Store> bulkInsert(List<Store> storeList) {
        String sql = """
        INSERT INTO store (
            store_name, contact_number, representative, store_address, created_at, updated_at, deleted_at
        )
        VALUES (
            ?, ?, ?, ?, NOW(), NOW(), NULL
        )
    """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        List<Store> resultList = new ArrayList<>();

        for (Store store : storeList) {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, store.getStoreName());
                ps.setString(2, store.getContactNumber());
                ps.setString(3, store.getRepresentative());
                ps.setString(4, store.getStoreAddress());
                return ps;
            }, keyHolder);

            Store savedStore = Store.builder()
                    .storeId(keyHolder.getKey().longValue())
                    .storeName(store.getStoreName())
                    .contactNumber(store.getContactNumber())
                    .representative(store.getRepresentative())
                    .storeAddress(store.getStoreAddress())
                    .build();

            resultList.add(savedStore);
        }

        return resultList;
    }
}
