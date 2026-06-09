package org.fleet.backend.repository;

import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.OrderStatus;
import org.fleet.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer (User customer);
    Optional<Order> findByIdAndCustomer(Long id, User customer);

    List<Order> findByOrderStatus (OrderStatus status);

    List<Order> findByRider(User rider);
    Optional<Order> findByIdAndRider(Long id, User rider);
}
