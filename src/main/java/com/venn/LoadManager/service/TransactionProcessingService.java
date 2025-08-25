package com.venn.LoadManager.service;

import com.venn.LoadManager.domain.CustomerTransaction;
import com.venn.LoadManager.dto.LoadRequestDTO;
import com.venn.LoadManager.dto.LoadResponseDTO;
import com.venn.LoadManager.dao.CustomerTransactionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.venn.LoadManager.util.MoneyUtil;

import javax.transaction.Transactional;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.Optional;

@Service
public class TransactionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessingService.class);

    @Value( "${daily.amount.limit.cents}")
    private Long dailyAmountLimitCents;

    @Value( "${weekly.amount.limit.cents}")
    private Long weeklyAmountLimitCents;

    @Value( "${daily.load.limit}")
    private Long dailyLoadLimit;

    private final CustomerTransactionDao dao;

    public TransactionProcessingService(CustomerTransactionDao dao) {
        this.dao = dao;
    }

    @Transactional
    public Optional<LoadResponseDTO> process(LoadRequestDTO req) {
        // Skips duplicate load for the same customer
        if (dao.existsByCustomerIdAndLoadId(req.getCustomerId(), req.getId())) {
            log.debug("Duplicate detected for customerId={}, loadId={}", req.getCustomerId(), req.getId());
            return Optional.empty();
        }

        Instant when = parseInstantUTC(req.getTime());
        long amountCents = MoneyUtil.parseDollarsToCents(req.getLoadAmount());

        // Compute day boundaries [start, end)
        Instant dayStart = startOfDayUtc(when);
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));

        // Compute ISO week boundaries (Monday-based) [start, end)
        Instant weekStart = startOfIsoWeekUtc(when);
        Instant weekEnd = weekStart.plus(Duration.ofDays(7));

        long daySum = dao.getAcceptedAmountSum(req.getCustomerId(), dayStart, dayEnd);
        long weekSum = dao.getAcceptedAmountSum(req.getCustomerId(), weekStart, weekEnd);
        long dayCount = dao.getAcceptedTransactionCount(req.getCustomerId(), dayStart, dayEnd);

        boolean accepted = (daySum + amountCents) <= dailyAmountLimitCents
                && (weekSum + amountCents) <= weeklyAmountLimitCents
                && (dayCount < dailyLoadLimit);

        // Persist the attempt with decision (ensures future queries see accepted loads only)
        CustomerTransaction attempt = new CustomerTransaction(req.getId(), req.getCustomerId(), amountCents, when, accepted);
        dao.save(attempt);

        return Optional.of(new LoadResponseDTO(req.getId(), req.getCustomerId(), accepted));
    }

    private Instant parseInstantUTC(String iso) {
        return Instant.parse(iso);
    }

    private Instant startOfDayUtc(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant startOfIsoWeekUtc(Instant instant) {
        return instant
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(WeekFields.ISO.dayOfWeek(), 1)  // Monday
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
