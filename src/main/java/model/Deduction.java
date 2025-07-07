package model;

import java.math.BigDecimal;

public class Deduction {

    private String description;
    private BigDecimal amount;

    public Deduction(String description, BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
