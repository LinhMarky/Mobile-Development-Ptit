package com.homely.rental.payment.controller;
import com.homely.rental.payment.service.PaymentService;
import com.homely.rental.payment.dto.PaymentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("${apiPrefix}/payments") @RequiredArgsConstructor
@ConditionalOnProperty(name="homely.demo.enabled", havingValue="true")
public class DemoPaymentController {
    private final PaymentService payments;
    @PostMapping("/{id}/simulate")
    public PaymentDTO simulate(@PathVariable Long id, @RequestParam(defaultValue="SUCCEEDED") String status) {
        return payments.simulate(id, status);
    }
}
