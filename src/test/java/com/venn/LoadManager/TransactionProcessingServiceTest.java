package com.venn.LoadManager;

import com.venn.LoadManager.dao.CustomerTransactionDao;
import com.venn.LoadManager.domain.CustomerTransaction;
import com.venn.LoadManager.dto.LoadRequestDTO;
import com.venn.LoadManager.dto.LoadResponseDTO;
import com.venn.LoadManager.service.TransactionProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TransactionProcessingService.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")

class TransactionProcessingServiceTest {

    @MockBean
    private CustomerTransactionDao dao;

    @Autowired
    private TransactionProcessingService service;

    private static final String CUSTOMER_ID = "1";
    private static final String LOAD_ID = "1";
    private static final String LOAD_AMOUNT = "$100";
    private static final String TIME = "2025-08-23T12:00:00Z";
    private static final Long DAILY_LOAD_AMOUNT_LIMIT_CENTS = 5000_00L;
    private static final Long DAILY_TRANSACTION_LIMIT = 3L;

    @Test
    void skipDuplicateLoad() {
        LoadRequestDTO req = request(LOAD_ID, CUSTOMER_ID, LOAD_AMOUNT, TIME);

        when(dao.existsByCustomerIdAndLoadId(CUSTOMER_ID, LOAD_ID)).thenReturn(true);

        Optional<LoadResponseDTO> result = service.process(req);
        assertThat(result).isEmpty();
        verify(dao, never()).save(any(CustomerTransaction.class));
    }

    @Test
    void acceptsWithinLimits() {
        LoadRequestDTO req = request(LOAD_ID, CUSTOMER_ID, LOAD_AMOUNT, TIME);

        when(dao.existsByCustomerIdAndLoadId(CUSTOMER_ID, LOAD_ID)).thenReturn(false);
        when(dao.getAcceptedAmountSum(eq(CUSTOMER_ID), any(), any())).thenReturn(0L);
        when(dao.getAcceptedTransactionCount(eq(CUSTOMER_ID), any(), any())).thenReturn(0L);

        Optional<LoadResponseDTO> result = service.process(req);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(LOAD_ID);
        assertThat(result.get().getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.get().isAccepted()).isTrue();
        verify(dao, times(1)).save(any(CustomerTransaction.class));
    }

    @Test
    void process_rejectsWhenDailyAmountExceeded() {
        LoadRequestDTO req = request(LOAD_ID, CUSTOMER_ID, LOAD_AMOUNT, TIME);

        when(dao.existsByCustomerIdAndLoadId(CUSTOMER_ID, LOAD_ID)).thenReturn(false);
        when(dao.getAcceptedAmountSum(eq(CUSTOMER_ID), any(), any())).thenReturn(DAILY_LOAD_AMOUNT_LIMIT_CENTS);
        when(dao.getAcceptedTransactionCount(eq(CUSTOMER_ID), any(), any())).thenReturn(0L);

        Optional<LoadResponseDTO> result = service.process(req);
        assertThat(result).isPresent();
        assertThat(result.get().isAccepted()).isFalse();
        verify(dao, times(1)).save(any(CustomerTransaction.class));
    }

    @Test
    void rejectsWhenWeeklyAmountExceeded() {
        LoadRequestDTO req = request(LOAD_ID, CUSTOMER_ID, "$2000", TIME);

        when(dao.existsByCustomerIdAndLoadId(CUSTOMER_ID, LOAD_ID)).thenReturn(false);
        when(dao.getAcceptedAmountSum(eq(CUSTOMER_ID), any(), any())).thenReturn(19_000_00L);
        when(dao.getAcceptedTransactionCount(eq(CUSTOMER_ID), any(), any())).thenReturn(0L);

        Optional<LoadResponseDTO> result = service.process(req);
        assertThat(result).isPresent();
        assertThat(result.get().isAccepted()).isFalse();
        verify(dao, times(1)).save(any(CustomerTransaction.class));
    }

    @Test
    void rejectsWhenDailyCountReached() {
        LoadRequestDTO req = request(LOAD_ID, CUSTOMER_ID, LOAD_AMOUNT, TIME);

        when(dao.existsByCustomerIdAndLoadId(CUSTOMER_ID, LOAD_ID)).thenReturn(false);
        when(dao.getAcceptedAmountSum(eq(CUSTOMER_ID), any(), any())).thenReturn(0L);
        when(dao.getAcceptedTransactionCount(eq(CUSTOMER_ID), any(), any())).thenReturn(DAILY_TRANSACTION_LIMIT);

        Optional<LoadResponseDTO> result = service.process(req);
        assertThat(result).isPresent();
        assertThat(result.get().isAccepted()).isFalse();
        verify(dao, times(1)).save(any(CustomerTransaction.class));
    }

    private static LoadRequestDTO request(String id, String customerId, String amount, String isoUtc) {
        LoadRequestDTO dto = new LoadRequestDTO();
        dto.setId(id);
        dto.setCustomerId(customerId);
        dto.setLoadAmount(amount);
        dto.setTime(isoUtc);
        return dto;
    }
}
