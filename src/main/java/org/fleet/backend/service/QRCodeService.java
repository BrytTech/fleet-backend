package org.fleet.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.fleet.backend.entity.Order;
import org.fleet.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class QRCodeService {

    private final OrderRepository orderRepository;

    @Value("${qr.image.directory:uploads/qr-codes/}")
    private String qrImageDirectory;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public QRCodeService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    //Generate QR code for an orderReturns the QR code string (order ID + order number)
    public String generateQRCode(Order order) {
        // Create unique QR code data
        String qrData = "FLEET:ORDER:" + order.getId() + ":" + order.getOrderNumber();

        // Save QR code as image
        String imageUrl = generateQRCodeImage(order, qrData);

        return imageUrl;
    }

    //Generate QR code image and save to file system
    private String generateQRCodeImage(Order order, String qrData) {
        try {
            int width = 300;
            int height = 300;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, width, height);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Create directory if it doesn't exist
            Path directory = Paths.get(qrImageDirectory);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Generate unique filename
            String filename = "qr_" + order.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            Path filePath = directory.resolve(filename);

            // Save image
            ImageIO.write(qrImage, "PNG", filePath.toFile());

            // Return image URL
            return baseUrl + "/uploads/qr-codes/" + filename;

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    //Generate QR code as Base64 string (can be sent directly in API response)
    public String generateQRCodeBase64(Order order) {
        String qrData = "FLEET:ORDER:" + order.getId() + ":" + order.getOrderNumber();

        try {
            int width = 300;
            int height = 300;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, width, height);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] qrBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(qrBytes);

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    //Validate QR code - check if it exists in database
    public boolean validateQRCode(String qrCode) {
        if (qrCode == null || qrCode.isEmpty()) {
            return false;
        }

        // Extract order ID from QR code
        // Format: FLEET:ORDER:12345:ORD-12345
        try {
            String[] parts = qrCode.split(":");
            if (parts.length >= 3 && parts[0].equals("FLEET") && parts[1].equals("ORDER")) {
                Long orderId = Long.parseLong(parts[2]);
                return orderRepository.existsById(orderId);
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    //Get order from QR code
    public Order getOrderFromQRCode(String qrCode) {
        String[] parts = qrCode.split(":");
        if (parts.length >= 3 && parts[0].equals("FLEET") && parts[1].equals("ORDER")) {
            Long orderId = Long.parseLong(parts[2]);
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for QR code"));
        }
        throw new RuntimeException("Invalid QR code format");
    }
}