package com.x.order.repository;

import com.x.order.entity.Order;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Page<Order> findByStoreId(Long storeId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<Order> findByStoreIdAndOrderChannelAndOrderStatus(
            Long storeId, OrderChannel orderChannel, OrderStatus orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<Order> findByStoreIdAndOrderStatusNot(Long storeId, OrderStatus orderStatus, Pageable pageable);

    @Query("select distinct o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("""
            select count(o.id) as orderCount,
                   coalesce(sum(o.subtotal), 0) as subtotal,
                   coalesce(sum(o.discount), 0) as discount,
                   coalesce(sum(o.tax), 0) as tax,
                   coalesce(sum(o.grandTotal), 0) as grandTotal
              from Order o
             where o.storeId = :storeId and o.orderStatus = :status
               and o.completedAt >= :from and o.completedAt < :to
            """)
    SalesSummaryProjection salesSummary(
            @Param("storeId") Long storeId, @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select i.variantId as variantId, i.productName as productName, i.sku as sku,
                   sum(i.qty) as quantity, sum(i.total) as salesAmount
              from OrderItem i
             where i.order.storeId = :storeId and i.order.orderStatus = :status
               and i.order.completedAt >= :from and i.order.completedAt < :to
             group by i.variantId, i.productName, i.sku
             order by sum(i.qty) desc
            """)
    List<TopProductProjection> topProducts(
            @Param("storeId") Long storeId, @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query(value = """
            select date(o.completed_at) as salesDate,
                   count(o.id) as orderCount,
                   coalesce(sum(o.grand_total), 0) as grandTotal
              from orders o
             where o.store_id = :storeId and o.order_status = :status
               and o.completed_at >= :from and o.completed_at < :to
             group by date(o.completed_at)
             order by date(o.completed_at)
            """, nativeQuery = true)
    List<SalesTrendProjection> salesTrend(
            @Param("storeId") Long storeId, @Param("status") String status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
