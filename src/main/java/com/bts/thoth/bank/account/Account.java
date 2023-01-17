package com.bts.thoth.bank.account;

import fr.maif.eventsourcing.State;

import java.math.BigDecimal;

public class Account implements State<Account> {
    public String id;
    public BigDecimal balance;
    public String status;
    public long sequenceNum;

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {return status;}

    public long getSequenceNum() {
        return sequenceNum;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", balance=" + balance +
                ", status=" + status +
                ", sequenceNum=" + sequenceNum +
                '}';
    }

    @Override
    public Long sequenceNum() {
        return sequenceNum;
    }

    @Override
    public Account withSequenceNum(Long sequenceNum) {
        this.sequenceNum = sequenceNum;
        return this;
    }
}