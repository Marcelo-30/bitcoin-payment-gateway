package com.bitcoinpaymentgateway.backend.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedBitcoinAddressGenerator
        implements BitcoinAddressGenerator {

    @Override
    public String generate() {
        String randomValue = UUID.randomUUID()
                .toString()
                .replace("-", "");

        return "tb1q" + randomValue;
    }
}
