package org.vfdtech.alm.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.vfdtech.alm.dto.DebtMaturityResponse;
import org.vfdtech.alm.model.DebtMaturityProfile;
import org.vfdtech.alm.model.LiabilityReport;
import org.vfdtech.alm.repository.DebtMaturityProfileRepository;
import org.vfdtech.alm.service.DebtMaturityProfileService;
import org.vfdtech.alm.service.LiabilityReportService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtMaturityProfileServiceImpl implements DebtMaturityProfileService {


    private final DebtMaturityProfileRepository debtMaturityProfileRepository;

    private final LiabilityReportService liabilityReportService;



    @Transactional
    @Override
    public DebtMaturityResponse computeAndSaveDebtMaturityProfileForBucket(String bucket) {
        debtMaturityProfileRepository.deleteByBucket(bucket);

        BigDecimal totalAmount = BigDecimal.ZERO;
        totalAmount = totalAmount.add(computeForLiabilityReport(bucket));

        DebtMaturityProfile profile = new DebtMaturityProfile();
        profile.setBucket(bucket);
        profile.setAmount(totalAmount);
        debtMaturityProfileRepository.save(profile);

        return new DebtMaturityResponse(totalAmount);
    }

    private BigDecimal computeForLiabilityReport(String bucket) {
        Pair<Long, Long> daysRange = determineDaysRangeForBucket(bucket);
        List<LiabilityReport> reports = liabilityReportService.getAllLiabilityReportsWithinDays(daysRange.getFirst(), daysRange.getSecond());

        BigDecimal total = BigDecimal.ZERO;
        for (LiabilityReport report : reports) {
            total = total.add(report.getFaceAmount());
        }
        return total;
    }



    @Override
    public Pair<Long, Long> determineDaysRangeForBucket(String bucket) {
        if (bucket.matches("\\d+")) {
            long day = Long.parseLong(bucket);
            return Pair.of(day, day);
        }

        else if (bucket.matches("\\d+-\\d+")) {
            String[] parts = bucket.split("-");
            long startDay = Long.parseLong(parts[0].trim());
            long endDay = Long.parseLong(parts[1].split(" ")[0].trim());
            return Pair.of(startDay, endDay);
        }
        else {
            throw new IllegalArgumentException("Invalid bucket format: " + bucket);
        }
    }

}
