package com.bugzero.rarego.out.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bugzero.rarego.global.util.S3Utils;
import com.bugzero.rarego.in.dto.es.ProductSearchDocumentDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.type.Category;
import com.bugzero.rarego.shared.product.type.InspectionStatus;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchClient {

    private final ElasticsearchClient elasticsearchClient;
    private final S3Utils s3Utils;
    private static final String INDEX_NAME = "product_search";

    // Temporary mitigation for expensive approved-id full scan
    private static final int APPROVED_PAGE_SIZE = 1000;
    private static final long APPROVED_CACHE_TTL_MILLIS = 30_000L;
    private final ReentrantLock approvedCacheLock = new ReentrantLock();
    private volatile List<Long> approvedProductIdsCache = Collections.emptyList();
    private volatile long approvedProductIdsCacheExpiresAt = 0L;

    public Optional<ProductAuctionResponseDto> getProduct(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }

        Map<Long, ProductAuctionResponseDto> result = getProducts(Set.of(productId));
        return Optional.ofNullable(result.get(productId));
    }

    public Map<Long, ProductAuctionResponseDto> getProducts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<FieldValue> values = productIds.stream().map(FieldValue::of).toList();

            SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .size(productIds.size())
                    .query(q -> q.terms(t -> t.field("productId").terms(v -> v.value(values)))),
                ProductSearchDocumentDto.class
            );

            return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                    ProductSearchDocumentDto::productId,
                    this::convertToDto,
                    (existing, replacement) -> existing
                ));

        } catch (IOException e) {
            log.error("ES Bulk Get Error", e);
            return Collections.emptyMap();
        }
    }

    public List<Long> getProductIdsBySellerId(Long sellerId) {
        if (sellerId == null) {
            return Collections.emptyList();
        }

        try {
            SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .size(1000)
                    .source(src -> src.filter(f -> f.includes("productId")))
                    .query(q -> q.term(t -> t.field("sellerId").value(sellerId))),
                ProductSearchDocumentDto.class
            );

            return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(ProductSearchDocumentDto::productId)
                .distinct()
                .toList();
        } catch (IOException e) {
            log.error("ES Seller Search Error", e);
            return Collections.emptyList();
        }
    }

    public List<Long> searchProductIds(String keyword, Category category) {
        try {
            List<Query> filters = new ArrayList<>();
            filters.add(Query.of(q -> q.term(t -> t.field("inspectionStatus").value(InspectionStatus.APPROVED.name()))));
            if (category != null) {
                filters.add(Query.of(q -> q.term(t -> t.field("category").value(category.name()))));
            }

            SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> {
                s.index(INDEX_NAME)
                    .size(1000)
                    .source(src -> src.filter(f -> f.includes("productId")));

                if (StringUtils.hasText(keyword)) {
                    s.query(q -> q.bool(b -> b
                        // 1. 정확한 구문 매칭
                        .should(sh -> sh.matchPhrase(m -> m
                            .field("productName")
                            .query(keyword)
                            .boost(3.0f)))
                        // 2. 구문 접두사 매칭
                        .should(sh -> sh.matchPhrasePrefix(m -> m
                            .field("productName")
                            .query(keyword)
                            .maxExpansions(10)
                            .boost(2.5f)))
                        // 3. 상품명 매칭
                        .should(sh -> sh.match(m -> m
                            .field("productName")
                            .query(keyword)
                            .boost(2.0f)))
                        // 4. 설명 매칭
                        .should(sh -> sh.match(m -> m
                            .field("description")
                            .query(keyword)
                            .boost(0.7f)))
                        // 5. 오타 내성 매칭 - AUTO: 1~2자 편집거리 허용
                        .should(sh -> sh.match(m -> m
                            .field("productName")
                            .query(keyword)
                            .fuzziness("AUTO")
                            .boost(1.0f)))
                        .minimumShouldMatch("1")
                        .filter(filters)
                    ));
                } else {
                    s.query(q -> q.bool(b -> b.filter(filters)));
                }
                return s;
            }, ProductSearchDocumentDto.class);

            return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(ProductSearchDocumentDto::productId)
                .distinct()
                .toList();

        } catch (IOException e) {
            log.error("ES Search Error", e);
            return Collections.emptyList();
        }
    }

    public List<Long> getApprovedProductIds() {
        long now = System.currentTimeMillis();
        List<Long> cached = approvedProductIdsCache;
        if (now < approvedProductIdsCacheExpiresAt && !cached.isEmpty()) {
            return cached;
        }

        approvedCacheLock.lock();
        try {
            now = System.currentTimeMillis();
            cached = approvedProductIdsCache;
            if (now < approvedProductIdsCacheExpiresAt && !cached.isEmpty()) {
                return cached;
            }

            List<Long> refreshed = fetchApprovedProductIdsWithSearchAfter();
            approvedProductIdsCache = List.copyOf(refreshed);
            approvedProductIdsCacheExpiresAt = now + APPROVED_CACHE_TTL_MILLIS;
            return approvedProductIdsCache;
        } catch (IOException e) {
            log.error("ES Approved List Error", e);
            if (!approvedProductIdsCache.isEmpty()) {
                return approvedProductIdsCache;
            }
            return Collections.emptyList();
        } finally {
            approvedCacheLock.unlock();
        }
    }

    private List<Long> fetchApprovedProductIdsWithSearchAfter() throws IOException {
        Set<Long> allIds = new LinkedHashSet<>();
        Long lastProductId = null;

        while (true) {
            Long cursor = lastProductId;
            SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> {
                    s.index(INDEX_NAME)
                        .size(APPROVED_PAGE_SIZE)
                        .source(src -> src.filter(f -> f.includes("productId")))
                        .sort(sort -> sort.field(f -> f.field("productId").order(SortOrder.Asc)))
                        .query(q -> q.term(t -> t.field("inspectionStatus").value(InspectionStatus.APPROVED.name())));

                    if (cursor != null) {
                        s.searchAfter(List.of(FieldValue.of(cursor)));
                    }
                    return s;
                },
                ProductSearchDocumentDto.class
            );

            List<Hit<ProductSearchDocumentDto>> hits = response.hits().hits();
            if (hits == null || hits.isEmpty()) {
                break;
            }

            for (Hit<ProductSearchDocumentDto> hit : hits) {
                ProductSearchDocumentDto source = hit.source();
                if (source != null && source.productId() != null) {
                    allIds.add(source.productId());
                }
            }

            if (hits.size() < APPROVED_PAGE_SIZE) {
                break;
            }

            ProductSearchDocumentDto lastSource = hits.get(hits.size() - 1).source();
            if (lastSource == null || lastSource.productId() == null) {
                break;
            }
            lastProductId = lastSource.productId();
        }

        return new ArrayList<>(allIds);
    }

    private ProductAuctionResponseDto convertToDto(ProductSearchDocumentDto doc) {
        List<String> imageUrls = (doc.imageUrls() != null && !doc.imageUrls().isEmpty())
            ? doc.imageUrls().stream().map(s3Utils::getPublicUrl).filter(Objects::nonNull).toList()
            : List.of();
        String thumbnailUrl = imageUrls.isEmpty() ? "" : imageUrls.get(0);

        return ProductAuctionResponseDto.builder()
            .id(doc.productId())
            .sellerId(doc.sellerId())
            .name(doc.productName())
            .description(doc.description())
            .category(doc.category())
            .thumbnailUrl(thumbnailUrl)
            .imageUrls(imageUrls)
            .inspectionStatus(doc.inspectionStatus())
            .build();
    }
}
