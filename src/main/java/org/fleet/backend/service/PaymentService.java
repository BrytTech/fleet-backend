package org.fleet.backend.service;

import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.OrderStatus;
import org.fleet.backend.entity.PaymentStatus;
import org.fleet.backend.entity.RiderProfile;
import org.fleet.backend.repository.OrderRepository;
import org.fleet.backend.repository.RiderProfileRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final NotificationService notificationService;

    public PaymentService(OrderRepository orderRepository,
                          RiderProfileRepository riderProfileRepository,
                          NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.riderProfileRepository = riderProfileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void releasePaymentToRider(Long orderId) {
        // 1. Get order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Verify order is confirmed by customer
        if (order.getOrderStatus() != OrderStatus.CUSTOMER_CONFIRMED) {
            throw new RuntimeException("Order must be confirmed by customer before payment release");
        }

        // 3. Check if payment already released
        if (order.isPaymentReleased()) {
            throw new RuntimeException("Payment already released for this order");
        }

        // 4. Get rider
        RiderProfile rider = order.getRider();
        if (rider == null) {
            throw new RuntimeException("No rider assigned to this order");
        }

        // 5. Update payment status
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentReleased(true);
        order.setOrderStatus(OrderStatus.PAYMENT_RELEASED);
        orderRepository.save(order);

        // 6. Update rider earnings
        rider.setTotalEarnings(rider.getTotalEarnings() + order.getPrice().doubleValue());
        rider.setCompletedDeliveries(rider.getCompletedDeliveries() + 1);
        riderProfileRepository.save(rider);

        // 7. Notify rider
        notificationService.createNotification(
                rider.getUser().getId(),
                "Payment Released!",
                "Payment of GHS " + order.getPrice() + " has been released for order #" + order.getOrderNumber(),
                "PAYMENT_RELEASED",
                orderId
        );

        // 8. Notify customer
        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Payment Sent",
                "Payment for order #" + order.getOrderNumber() + " has been released to the rider.",
                "PAYMENT_RELEASED",
                orderId
        );

        System.out.println("Payment released for order: " + order.getOrderNumber());
    }

    public double getRiderEarnings(Long riderId) {
        RiderProfile rider = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Rider not found"));
        return rider.getTotalEarnings();
    }

    public double getTotalDeliveries(Long riderId) {
        RiderProfile rider = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Rider not found"));
        return rider.getCompletedDeliveries();
    }
}