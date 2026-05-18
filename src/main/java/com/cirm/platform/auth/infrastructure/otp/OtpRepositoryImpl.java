package com.cirm.platform.auth.infrastructure.otp;

import com.cirm.platform.auth.domain.otp.Otp;
import com.cirm.platform.auth.domain.otp.OtpRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Infrastructure implementation of the Domain OtpRepository.
 *
 * This class acts as an Anti-Corruption Layer between:
 * - Domain layer
 * - Spring Data JPA
 */
@Repository
@Transactional
public class OtpRepositoryImpl implements OtpRepository {

    private final OtpJpaRepository jpaRepository;
    private final OtpMapper mapper = new OtpMapper();

    public OtpRepositoryImpl(OtpJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Otp save(Otp otp) {
        OtpJpaEntity entity = mapper.toJpaEntity(otp);
        OtpJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Otp> findByReferenceAndMobile(String otpReference, String mobileNumber) {
        return jpaRepository
                .findByOtpReferenceAndMobileNumber(otpReference, mobileNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Otp> findLatestByMobile(String mobileNumber) {
        return jpaRepository
                .findTopByMobileNumberOrderByCreatedAtDesc(mobileNumber)
                .map(mapper::toDomain);
    }

    /**
     * Instead of deleting rows, we mark existing active OTPs as expired.
     * This preserves audit trail.
     */
    @Override
    public void invalidateActiveOtps(String mobileNumber, LocalDateTime now) {
        jpaRepository.findTopByMobileNumberOrderByCreatedAtDesc(mobileNumber)
                .ifPresent(entity -> {
                    if (entity.getExpiresAt().isAfter(now)) {
                        entity.setExpiresAt(now);
                        jpaRepository.save(entity);
                    }
                });
    }
}
