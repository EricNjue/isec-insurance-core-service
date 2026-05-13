package com.isec.platform.modules.applications.service.motor;

import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.dto.motor.ManualPaymentInstructions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ManualPaymentInstructionService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    public ManualPaymentInstructions getInstructions(
            PaymentMethod method,
            String businessNumber,
            String accountNumber,
            BigDecimal amount,
            String currency
    ) {
        log.info("Generating manual instructions for method: {}, businessNumber: {}, accountNumber: {}", 
                method, businessNumber, accountNumber);
        
        List<String> instructions = new ArrayList<>();
        if (method == PaymentMethod.MPESA_PAYBILL) {
            instructions.add("Go to <b>M-Pesa</b>");
            instructions.add("Select <b>Lipa na M-Pesa</b>");
            instructions.add("Select <b>Pay Bill</b>");
            instructions.add("Enter Business No <b>" + businessNumber + "</b>");
            instructions.add("Account No <b>" + accountNumber + "</b>");
            instructions.add("Enter Amount <b>" + currency + " " + CURRENCY_FORMAT.format(amount) + "</b>");
        } else {
            // Generic fallback or other methods
            instructions.add("Please pay " + currency + " " + CURRENCY_FORMAT.format(amount) + 
                    " to " + method + " account " + businessNumber + " (Ref: " + accountNumber + ")");
        }

        return ManualPaymentInstructions.builder()
                .paymentMethod(method)
                .businessNumber(businessNumber)
                .accountNumber(accountNumber)
                .amount(amount)
                .currency(currency)
                .instructions(instructions)
                .build();
    }
}
