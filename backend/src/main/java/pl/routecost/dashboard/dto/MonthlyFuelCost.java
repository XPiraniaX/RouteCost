package pl.routecost.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyFuelCost(int year, int month, BigDecimal totalCost, long count) {
}
