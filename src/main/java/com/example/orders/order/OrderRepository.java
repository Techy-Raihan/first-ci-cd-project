package com.example.orders.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<PurchaseOrder> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
            select o from PurchaseOrder o
            where (:customerId is null or o.customer.id = :customerId)
              and (:status is null or o.status = :status)
            """)
    Page<PurchaseOrder> search(@Param("customerId") Long customerId,
                               @Param("status") OrderStatus status,
                               Pageable pageable);
}
