package dutchiepay.backend.domain.commerce.repository;

import dutchiepay.backend.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Product> bulkInsert(List<Product> productList) {
        String sql = """
        INSERT INTO product (
            store_id, product_name, detail_img, original_price, sale_price, discount_percent, product_img, created_at, updated_at, deleted_at
        )
        VALUES (
            ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL
        )
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        List<Product> resultList = new ArrayList<>();

        for (Product product : productList) {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, product.getStore().getStoreId());
                ps.setString(2, product.getProductName());
                ps.setString(3, product.getDetailImg());
                ps.setInt(4, product.getOriginalPrice());
                ps.setInt(5, product.getSalePrice());
                ps.setInt(6, product.getDiscountPercent());
                ps.setString(7, product.getProductImg());
                return ps;
            }, keyHolder);

            Product savedProduct = Product.builder()
                    .productId(keyHolder.getKey().longValue())
                    .store(product.getStore())
                    .productName(product.getProductName())
                    .detailImg(product.getDetailImg())
                    .originalPrice(product.getOriginalPrice())
                    .salePrice(product.getSalePrice())
                    .discountPercent(product.getDiscountPercent())
                    .productImg(product.getProductImg())
                    .build();

            resultList.add(savedProduct);
        }

        return resultList;
    }
}
