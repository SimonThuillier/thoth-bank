package com.bts.thoth.bank.account;

public enum TransferMode {

    CASH("cash"),
    BANK_TRANSFER("bank transfer");

    private final String mode;

    TransferMode(String mode) {
        this.mode = mode;
    }

    public String toString() {
        return mode;
    }
}
