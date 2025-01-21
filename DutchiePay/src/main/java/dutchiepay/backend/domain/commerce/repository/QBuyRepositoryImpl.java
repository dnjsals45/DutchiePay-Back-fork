package dutchiepay.backend.domain.commerce.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dutchiepay.backend.domain.commerce.dto.*;
import dutchiepay.backend.domain.commerce.exception.CommerceErrorCode;
import dutchiepay.backend.domain.commerce.exception.CommerceException;
import dutchiepay.backend.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class QBuyRepositoryImpl implements QBuyRepository{

    private final JPAQueryFactory jpaQueryFactory;

    QUser user = QUser.user;
    QBuy buy = QBuy.buy;
    QProduct product = QProduct.product;
    QStore store = QStore.store;
    QReview review = QReview.review;
    QLike like = QLike.like;
    QAsk ask = QAsk.ask;
    QScore score = QScore.score;
    QBuyCategory buyCategory = QBuyCategory.buyCategory;
    QCategory category = QCategory.category;
    QOrder order = QOrder.order;

    @Override
    public GetBuyResponseDto getBuyPageByBuyId(User user, Long buyId) {
        JPAQuery<Tuple> query = jpaQueryFactory
                .select(
                        product.productName,
                        product.productImg,
                        product.detailImg,
                        product.originalPrice,
                        product.salePrice,
                        product.discountPercent,
                        store.storeName,
                        store.contactNumber,
                        store.representative,
                        store.storeAddress,
                        buy.skeleton,
                        buy.nowCount,
                        buy.deadline,
                        JPAExpressions
                                .select(like.count())
                                .from(like)
                                .where(like.buy.buyId.eq(buyId)),
                        user != null ?
                                JPAExpressions
                                        .selectOne()
                                        .from(like)
                                        .where(like.user.eq(user)
                                                .and(like.buy.buyId.eq(buyId)))
                                        .exists()
                                : Expressions.nullExpression(),
                        JPAExpressions
                                .select(ask.count())
                                .from(ask)
                                .where(ask.buy.buyId.eq(buyId))
                                .where(ask.deletedAt.isNull()),
                        score.one,
                        score.two,
                        score.three,
                        score.four,
                        score.five
                )
                .from(buy)
                .join(buy.product, product)
                .join(product.store, store)
                .leftJoin(score).on(score.buy.buyId.eq(buy.buyId))
                .where(buy.buyId.eq(buyId));

        Tuple result = query.fetchOne();

        if (result == null) {
            return null;
        }

        List<Review> reviews = jpaQueryFactory
                .selectFrom(review)
                .where(review.order.buy.buyId.eq(buyId))
                .where(review.deletedAt.isNull())
                .fetch();

        long reviewCount = 0;
        long photoReviewCount = 0;

        for (Review review : reviews) {
            reviewCount++;
            if (review.getReviewImg() != null) {
                photoReviewCount++;
            }
        }

        Integer one = Optional.ofNullable(result.get(16, Integer.class)).orElse(0);
        Integer two = Optional.ofNullable(result.get(17, Integer.class)).orElse(0);
        Integer three = Optional.ofNullable(result.get(18, Integer.class)).orElse(0);
        Integer four = Optional.ofNullable(result.get(19, Integer.class)).orElse(0);
        Integer five = Optional.ofNullable(result.get(20, Integer.class)).orElse(0);

        Integer[] ratingCount = new Integer[]{five, four, three, two, one};

        double average;

        if (one + two + three + four + five == 0) {
            average = 0.0;
        } else {
            average = (one + two * 2 + three * 3 + four * 4 + five * 5) / (double) (one + two + three + four + five);
        }

        List<String> categoryList = jpaQueryFactory
                .select(category.name)
                .from(buyCategory)
                .join(buyCategory.category, category)
                .where(buyCategory.buy.buyId.eq(buyId))
                .fetch();

        String[] categories = categoryList.toArray(new String[0]);

        return GetBuyResponseDto.builder()
                .productName(result.get(0, String.class))
                .productImg(result.get(1, String.class))
                .productDetail(result.get(2, String.class))
                .originalPrice(result.get(3, Integer.class))
                .salePrice(result.get(4, Integer.class))
                .discountPercent(result.get(5, Integer.class))
                .storeName(result.get(6, String.class))
                .contactNumber(result.get(7, String.class))
                .representative(result.get(8, String.class))
                .storeAddress(result.get(9, String.class))
                .skeleton(result.get(10, Integer.class))
                .nowCount(result.get(11, Integer.class))
                .deadline(result.get(12, LocalDate.class))
                .likeCount(result.get(13, Long.class))
                .isLiked(user != null ? result.get(14, Boolean.class) : null)
                .reviewCount(reviewCount)
                .photoReviewCount(photoReviewCount)
                .askCount(result.get(15, Long.class))
                .ratingCount(ratingCount)
                .rating(average)
                .category(categories)
                .build();
    }


    @Override
    public GetBuyListResponseDto getBuyList(User user, String filter, String categoryName, String word, int end, Long cursor, int limit) {
        cursor = (cursor == null) ? Long.MAX_VALUE : cursor;

        BooleanBuilder conditions = buildBaseConditions(categoryName, end, word);
        OrderAndCursorCondition orderAndCursor = buildOrderAndCursorCondition(filter, cursor);

        List<GetBuyListResponseDto.ProductDto> results = executeMainQuery(user, conditions,
                orderAndCursor.getOrderBy(), orderAndCursor.getCursorCondition(), limit, filter);

        Long nextCursor = results.size() > limit ? results.get(limit).getBuyId() : null;

        List<GetBuyListResponseDto.ProductDto> limitedResults =
                results.size() > limit ? results.subList(0, limit) : results;

        Map<Long, Long> reviewCounts = fetchReviewCounts(limitedResults.stream()
                .map(GetBuyListResponseDto.ProductDto::getBuyId)
                .collect(Collectors.toList()));

        results.forEach(product ->
                product.setReviewCount(reviewCounts.getOrDefault(product.getBuyId(), 0L))
        );

        return GetBuyListResponseDto.builder()
                .products(limitedResults)
                .cursor(nextCursor)
                .build();
    }

    @Override
    public GetBuyListResponseDto getBuyListPage(User user, String filter, String categoryName, String word, int end, int page) {
        int limit = 16;

        BooleanBuilder conditions = buildBaseConditions(categoryName, end, word);
        OrderAndSortCondition orderAndSort = buildOrderAndSortCondition(filter);

        List<GetBuyListResponseDto.ProductDto> results = executeMainQuery(user, conditions,
                orderAndSort.getOrderBy(), page, limit, filter);

        Map<Long, Long> reviewCounts = fetchReviewCounts(results.stream()
                .map(GetBuyListResponseDto.ProductDto::getBuyId)
                .collect(Collectors.toList()));

        results.forEach(product ->
                product.setReviewCount(reviewCounts.getOrDefault(product.getBuyId(), 0L))
        );

        return GetBuyListResponseDto.builder()
                .products(results)
                .cursor(null)
                .build();
    }

    private OrderAndSortCondition buildOrderAndSortCondition(String filter) {
        return switch (filter) {
            case "like" -> new OrderAndSortCondition(
                    new OrderSpecifier[]{like.count().desc(), buy.buyId.desc()}
            );
            case "discount" -> new OrderAndSortCondition(
                    new OrderSpecifier[]{product.discountPercent.desc(), buy.buyId.desc()}
            );
            case "endDate" -> new OrderAndSortCondition(getEndDateOrderSpecifiers());
            case "newest" -> new OrderAndSortCondition(
                    new OrderSpecifier[]{buy.buyId.desc()}
            );
            default -> throw new CommerceException(CommerceErrorCode.INVALID_FILTER);
        };
    }

    private BooleanBuilder buildBaseConditions(String categoryName, int end, String word) {
        BooleanBuilder conditions = new BooleanBuilder();

        if (StringUtils.hasText(categoryName)) {
            conditions.and(buy.buyCategories.any().category.name.eq(categoryName));
        }

        if (end == 0) {
            conditions.and(buy.deadline.after(LocalDate.now().minusDays(1)));
        }

        if (StringUtils.hasText(word)) {
            conditions.and(buy.title.contains(word).or(buy.tags.contains(word)));
        }

        return conditions;
    }

    private BooleanBuilder getEndDateCursorCondition(Long cursor) {
        BooleanBuilder condition = new BooleanBuilder();

        Buy cursorBuy = jpaQueryFactory
                .selectFrom(buy)
                .where(buy.buyId.eq(cursor))
                .fetchOne();

        if (cursorBuy != null) {
            condition.and(
                    buy.deadline.gt(cursorBuy.getDeadline())
                            .or(buy.deadline.eq(cursorBuy.getDeadline())
                                    .and(buy.buyId.loe(cursor)))
            );
        }

        return condition;
    }

    private Expression<Boolean> getUserLikeExpression(User user) {
        return user != null ?
                JPAExpressions
                        .selectOne()
                        .from(like)
                        .where(like.buy.eq(buy)
                                .and(like.user.eq(user)))
                        .exists() :
                Expressions.constant(false);
    }

    private OrderSpecifier[] getEndDateOrderSpecifiers() {
        return new OrderSpecifier[]{
                Expressions.cases()
                        .when(buy.deadline.goe(LocalDate.now()))
                        .then(0)
                        .otherwise(1)
                        .asc(),
                buy.deadline.asc(),
                buy.buyId.desc()
        };
    }

    private Integer getCursorDiscountPercent(Long cursor) {
        return jpaQueryFactory
                .select(product.discountPercent)
                .from(buy)
                .join(buy.product, product)
                .where(buy.buyId.eq(cursor))
                .fetchOne();
    }

    private Long getCursorLikeCount(Long cursor) {
        return jpaQueryFactory
                .select(like.count())
                .from(like)
                .join(like.buy, buy)
                .where(buy.buyId.eq(cursor))
                .fetchOne();
    }

    private OrderAndCursorCondition buildOrderAndCursorCondition(String filter, Long cursor) {
        switch (filter) {
            case "like" -> {
                OrderSpecifier[] orderBy = {like.count().desc(), buy.buyId.desc()};
                BooleanBuilder cursorCondition = new BooleanBuilder();

                if (cursor < Long.MAX_VALUE) {
                    Long cursorLike = getCursorLikeCount(cursor);
                    if (cursorLike != null) {
                        cursorCondition.and(
                                like.count().lt(cursorLike)
                                        .or(like.count().eq(cursorLike)
                                                .and(buy.buyId.loe(cursor)))
                        );
                    }
                }
                return new OrderAndCursorCondition(orderBy, cursorCondition);
            }
            case "discount" -> {
                OrderSpecifier[] orderBy = {product.discountPercent.desc(), buy.buyId.desc()};
                BooleanBuilder cursorCondition = new BooleanBuilder();

                if (cursor < Long.MAX_VALUE) {
                    Integer cursorDiscount = getCursorDiscountPercent(cursor);
                    if (cursorDiscount != null) {
                        cursorCondition.and(
                                product.discountPercent.lt(cursorDiscount)
                                        .or(product.discountPercent.eq(cursorDiscount)
                                                .and(buy.buyId.loe(cursor)))
                        );
                    }
                }
                return new OrderAndCursorCondition(orderBy, cursorCondition);
            }
            case "endDate" -> {
                return new OrderAndCursorCondition(
                        getEndDateOrderSpecifiers(),
                        cursor < Long.MAX_VALUE ? getEndDateCursorCondition(cursor) : new BooleanBuilder()
                );
            }
            case "newest" -> {
                return new OrderAndCursorCondition(
                        new OrderSpecifier[]{buy.buyId.desc()},
                        new BooleanBuilder().and(buy.buyId.loe(cursor))
                );
            }
            default -> throw new CommerceException(CommerceErrorCode.INVALID_FILTER);
        }
    }

    private List<GetBuyListResponseDto.ProductDto> executeMainQuery(User u, BooleanBuilder conditions,
                                                                    OrderSpecifier[] orderBy, BooleanBuilder cursorCondition, int limit, String filter) {

        JPAQuery<GetBuyListResponseDto.ProductDto> query = jpaQueryFactory
                .select(Projections.constructor(GetBuyListResponseDto.ProductDto.class,
                        buy.buyId,
                        product.productName,
                        product.productImg,
                        product.originalPrice,
                        product.salePrice,
                        product.discountPercent,
                        buy.skeleton,
                        buy.nowCount,
                        buy.deadline,
                        getUserLikeExpression(u)
                ))
                .from(buy)
                .innerJoin(buy.product, product);

        query.where(conditions);

        if ("like".equals(filter)) {
            query.leftJoin(like).on(like.buy.eq(buy));
            query.groupBy(buy.buyId, product.productName, product.productImg,
                    product.originalPrice, product.salePrice, product.discountPercent,
                    buy.skeleton, buy.nowCount, buy.deadline);

            query.having(cursorCondition);
        } else {
            query.where(cursorCondition);
        }

        return query.orderBy(orderBy)
                .limit(limit + 1L)
                .fetch();
    }

    private List<GetBuyListResponseDto.ProductDto> executeMainQuery(User user, BooleanBuilder conditions,
                                                                    OrderSpecifier[] orderBy, int page, int size, String filter) {
        JPAQuery<GetBuyListResponseDto.ProductDto> query = jpaQueryFactory
                .select(Projections.constructor(GetBuyListResponseDto.ProductDto.class,
                        buy.buyId,
                        product.productName,
                        product.productImg,
                        product.originalPrice,
                        product.salePrice,
                        product.discountPercent,
                        buy.skeleton,
                        buy.nowCount,
                        buy.deadline,
                        getUserLikeExpression(user)
                ))
                .from(buy)
                .innerJoin(buy.product, product);

        query.where(conditions);

        if ("like".equals(filter)) {
            query.leftJoin(like).on(like.buy.eq(buy));
            query.groupBy(buy.buyId, product.productName, product.productImg,
                    product.originalPrice, product.salePrice, product.discountPercent,
                    buy.skeleton, buy.nowCount, buy.deadline);
        }

        return query.orderBy(orderBy)
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    private Map<Long, Long> fetchReviewCounts(List<Long> buyIds) {
        List<Tuple> reviewCounts = jpaQueryFactory
                .select(order.buy.buyId, review.count())
                .from(review)
                .innerJoin(review.order, order)
                .where(order.buy.buyId.in(buyIds)
                        .and(review.deletedAt.isNull()))
                .groupBy(order.buy.buyId)
                .fetch();

        return reviewCounts.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, Long.class),
                        tuple -> tuple.get(1, Long.class)
                ));
    }

    @Override
    public List<GetProductReviewResponseDto> getProductReview(Long buyId, Long photo, PageRequest pageable) {
        List<Tuple> result = jpaQueryFactory
                .select(review.reviewId,
                        review.user.nickname,
                        review.contents,
                        review.rating,
                        review.reviewImg,
                        review.createdAt)
                .from(review)
                .join(review.user, user)
                .join(review.order.buy, buy)
                .where(buy.buyId.eq(buyId))
                .where(photoCondition(photo))
                .where(review.deletedAt.isNull())
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<GetProductReviewResponseDto> reviews = new ArrayList<>();

        for (Tuple tuple : result) {
            GetProductReviewResponseDto reviewDto = GetProductReviewResponseDto.builder()
                    .reviewId(tuple.get(0, Long.class))
                    .nickname(tuple.get(1, String.class))
                    .content(tuple.get(2, String.class))
                    .rating(tuple.get(3, Integer.class))
                    .reviewImg(tuple.get(4, String.class) != null ? tuple.get(4, String.class).split(",") : new String[0])
                    .createdAt(tuple.get(5, LocalDateTime.class).toLocalDate())
                    .build();

            reviews.add(reviewDto);
        }

        return reviews;
    }

    private BooleanExpression photoCondition(Long photo) {
        if (photo == null) {
            return null;
        }
        return photo == 1 ? review.reviewImg.isNotNull() : null;
    }

    public List<String> findAllTags() {
        return jpaQueryFactory
                .select(buy.tags)
                .from(buy)
                .where(buy.tags.isNotNull())
                .fetch();
    }
}