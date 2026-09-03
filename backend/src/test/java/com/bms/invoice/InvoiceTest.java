package com.bms.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bms.common.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    private Invoice newInvoice() {
        return new Invoice(null, null, "INV-2026-000001", InvoiceType.RENT,
                START, END, START, START.plusDays(14), null);
    }

    @Test
    void totalIsTheSumOfLineAmounts() {
        Invoice invoice = newInvoice();
        invoice.addLine("Rent", BigDecimal.ONE, new BigDecimal("850.00"), "month");
        invoice.addLine("Utilities", BigDecimal.ONE, new BigDecimal("120.50"), "month");

        assertThat(invoice.getTotal()).isEqualByComparingTo("970.50");
    }

    @Test
    void lineAmountRoundsToTwoDecimals() {
        Invoice invoice = newInvoice();
        invoice.addLine("Cold water", new BigDecimal("12.345"), new BigDecimal("3.30"), "m3");

        // 12.345 * 3.30 = 40.7385 -> 40.74
        assertThat(invoice.getLines().getFirst().getAmount()).isEqualByComparingTo("40.74");
    }

    @Test
    void totalIsZeroWithoutLines() {
        assertThat(newInvoice().getTotal()).isEqualByComparingTo("0");
    }

    @Test
    void periodEndBeforeStartIsRejected() {
        assertThatThrownBy(() -> new Invoice(null, null, "INV-1", InvoiceType.RENT,
                END, START, START, START.plusDays(14), null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("period end");
    }

    @Test
    void dueDateBeforeIssueDateIsRejected() {
        assertThatThrownBy(() -> new Invoice(null, null, "INV-1", InvoiceType.RENT,
                START, END, END, START, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("due date");
    }

    @Test
    void draftCanBeSentThenPaid() {
        Invoice invoice = newInvoice();
        invoice.transitionTo(InvoiceStatus.SENT);
        invoice.transitionTo(InvoiceStatus.PAID);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void paidInvoiceCanOnlyBeCancelled() {
        Invoice invoice = newInvoice();
        invoice.transitionTo(InvoiceStatus.PAID);

        assertThatThrownBy(() -> invoice.transitionTo(InvoiceStatus.SENT))
                .isInstanceOf(ValidationException.class);

        invoice.transitionTo(InvoiceStatus.CANCELLED);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void cancelledInvoiceIsTerminal() {
        Invoice invoice = newInvoice();
        invoice.transitionTo(InvoiceStatus.CANCELLED);

        assertThatThrownBy(() -> invoice.transitionTo(InvoiceStatus.DRAFT))
                .isInstanceOf(ValidationException.class);
    }
}
