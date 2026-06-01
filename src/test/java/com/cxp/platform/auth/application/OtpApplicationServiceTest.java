package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.otp.Otp;
import com.cxp.platform.auth.domain.otp.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OtpApplicationService.
 *
 * Key consistency invariant: invalidateActiveOtps (step 2) and save(newOtp) (step 6)
 * are wrapped in a single @Transactional. These tests verify:
 *   1. invalidateActiveOtps is called BEFORE save — correct ordering under the transaction.
 *   2. Cooldown is enforced before any write.
 *   3. If within cooldown, neither write is issued.
 *
 * @Transactional rollback behavior (if invalidate succeeds but save fails → user retains
 * their old OTP) is infrastructure-level and requires a real DB — not tested here.
 */
@ExtendWith(MockitoExtension.class)
class OtpApplicationServiceTest {

    @Mock private OtpRepository otpRepository;

    private OtpApplicationService service;

    private static final String MOBILE = "9876543210";

    @BeforeEach
    void setUp() {
        service = new OtpApplicationService(otpRepository);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void requestOtp_noExistingOtp_invalidatesThenSaves() {
        when(otpRepository.findLatestByMobile(MOBILE)).thenReturn(Optional.empty());
        when(otpRepository.save(any(Otp.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpApplicationService.OtpResponse response = service.requestOtp(MOBILE);

        InOrder order = inOrder(otpRepository);
        order.verify(otpRepository).findLatestByMobile(MOBILE);
        order.verify(otpRepository).invalidateActiveOtps(eq(MOBILE), any(LocalDateTime.class));
        order.verify(otpRepository).save(any(Otp.class));

        assertThat(response.otpReference()).isNotNull();
        assertThat(response.otp()).hasSize(6);
    }

    @Test
    void requestOtp_previousOtpExpired_allowsNewRequest() {
        // OTP created 60 seconds ago (well past the 30-second cooldown)
        Otp oldOtp = makeOtpCreatedSecondsAgo(60);
        when(otpRepository.findLatestByMobile(MOBILE)).thenReturn(Optional.of(oldOtp));
        when(otpRepository.save(any(Otp.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpApplicationService.OtpResponse response = service.requestOtp(MOBILE);

        verify(otpRepository).invalidateActiveOtps(eq(MOBILE), any(LocalDateTime.class));
        verify(otpRepository).save(any(Otp.class));
        assertThat(response.otp()).hasSize(6);
    }

    // ── Cooldown enforcement ──────────────────────────────────────────────────

    @Test
    void requestOtp_withinCooldown_throwsWithoutWriting() {
        // OTP created 10 seconds ago (within the 30-second cooldown)
        Otp recentOtp = makeOtpCreatedSecondsAgo(10);
        when(otpRepository.findLatestByMobile(MOBILE)).thenReturn(Optional.of(recentOtp));

        assertThatThrownBy(() -> service.requestOtp(MOBILE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("frequently");

        verify(otpRepository, never()).invalidateActiveOtps(anyString(), any());
        verify(otpRepository, never()).save(any());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Otp makeOtpCreatedSecondsAgo(int secondsAgo) {
        // OTP expiry = now + (OTP_EXPIRY_MINUTES=2) - secondsAgo
        // Creation time = expiresAt - OTP_EXPIRY_MINUTES
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(2)
                .minusSeconds(secondsAgo);
        return new Otp(
                java.util.UUID.randomUUID(),
                "ref-" + secondsAgo,
                MOBILE,
                "hash",
                expiresAt,
                0,
                5
        );
    }
}
