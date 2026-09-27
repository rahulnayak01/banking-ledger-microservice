package com.example.ledger.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Document(indexName = "ledger-transactions")
public class TransactionDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String transactionType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private String debitAccountNumber;

    @Field(type = FieldType.Keyword)
    private String creditAccountNumber;

    @Field(type = FieldType.Keyword)
    private String debitAccountId;

    @Field(type = FieldType.Keyword)
    private String creditAccountId;

    @Field(type = FieldType.Date)
    private OffsetDateTime createdAt;

    public TransactionDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDebitAccountNumber() {
        return debitAccountNumber;
    }

    public void setDebitAccountNumber(String debitAccountNumber) {
        this.debitAccountNumber = debitAccountNumber;
    }

    public String getCreditAccountNumber() {
        return creditAccountNumber;
    }

    public void setCreditAccountNumber(String creditAccountNumber) {
        this.creditAccountNumber = creditAccountNumber;
    }

    public String getDebitAccountId() {
        return debitAccountId;
    }

    public void setDebitAccountId(String debitAccountId) {
        this.debitAccountId = debitAccountId;
    }

    public String getCreditAccountId() {
        return creditAccountId;
    }

    public void setCreditAccountId(String creditAccountId) {
        this.creditAccountId = creditAccountId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}