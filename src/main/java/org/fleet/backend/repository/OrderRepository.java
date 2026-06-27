package org.fleet.backend.repository;

import org.fleet.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(CustomerProfile customer);
    Optional<Order> findByIdAndCustomer(Long id, CustomerProfile customer);

    List<Order> findByOrderStatus(OrderStatus status);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByRider(RiderProfile rider);
    Optional<Order> findByIdAndRider(Long id, RiderProfile rider);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH o.rider r " +
            "LEFT JOIN FETCH r.user " +
            "WHERE o.id = :id AND o.customer = :customer")
    Optional<Order> findByIdAndCustomerWithDetails(@Param("id") Long id, @Param("customer") CustomerProfile customer);

    List<Order> findAllByOrderByIdDesc();

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer c " +
            "LEFT JOIN FETCH c.user u " +
            "LEFT JOIN FETCH o.rider r " +
            "LEFT JOIN FETCH r.user ru " +
            "ORDER BY o.id DESC")
    List<Order> findAllOrdersWithDetails();

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH o.rider r " +
            "LEFT JOIN FETCH r.user " +
            "WHERE o.customer = :customer " +
            "ORDER BY o.id DESC")
    List<Order> findByCustomerWithDetails(@Param("customer") CustomerProfile customer);

    // Check if store is pickup for any active order (status != PAYMENT_RELEASED)
    @Query("SELECT EXISTS (SELECT 1 FROM Order o WHERE o.pickupStore = :store AND o.orderStatus != :status)")
    boolean existsByPickupStoreAndOrderStatusNot(@Param("store") Store store, @Param("status") OrderStatus status);

    // Check if store is dropoff for any active order (status != PAYMENT_RELEASED)
    @Query("SELECT EXISTS (SELECT 1 FROM Order o WHERE o.dropoffStore = :store AND o.orderStatus != :status)")
    boolean existsByDropoffStoreAndOrderStatusNot(@Param("store") Store store, @Param("status") OrderStatus status);

    // Find active order for a store (pickup or dropoff)
    @Query("SELECT o FROM Order o WHERE (o.pickupStore = :store OR o.dropoffStore = :store) AND o.orderStatus != :status")
    Optional<Order> findActiveOrderByStore(@Param("store") Store store, @Param("status") OrderStatus status);
}