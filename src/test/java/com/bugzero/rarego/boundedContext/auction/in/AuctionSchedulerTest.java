package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    TaskScheduler taskScheduler;

    @Mock
    AuctionSettleAuctionFacade facade;

    @InjectMocks
    AuctionScheduler scheduler;

    private Map<Long, ScheduledFuture<?>> scheduledTasks;

    @BeforeEach
    void setUp() {
        scheduledTasks = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(scheduler, "scheduledTasks", scheduledTasks);
    }

    @Test
    @DisplayName("auctionId가 null이면 예외 발생")
    void scheduleSettlement_NullAuctionId() {
        // when & then
        assertThatThrownBy(() -> scheduler.scheduleSettlement(null, LocalDateTime.now(ZoneId.of("Asia/Seoul"))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT);
    }

    @Test
    @DisplayName("endTime이 null이면 예외 발생")
    void scheduleSettlement_NullEndTime() {
        // when & then
        assertThatThrownBy(() -> scheduler.scheduleSettlement(1L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT);
    }

    @Test
    @DisplayName("미래 시간이면 스케줄링됨")
    void scheduleSettlement_FutureTime() {
        // given
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        // when
        scheduler.scheduleSettlement(1L, LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(10));

        // then
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        assertThat(scheduledTasks).containsKey(1L);
    }

    @Test
    @DisplayName("과거 시간이면 즉시 실행")
    void scheduleSettlement_PastTime() {
        // when
        scheduler.scheduleSettlement(1L, LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(10));

        // then
        verify(facade).settleOne(1L);
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("기존 예약이 있으면 취소 후 재예약")
    void scheduleSettlement_CancelExisting() {
        // given
        ScheduledFuture<?> existingFuture = mock(ScheduledFuture.class);
        ScheduledFuture<?> newFuture = mock(ScheduledFuture.class);

        doReturn(false).when(existingFuture).isDone();
        doReturn(newFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        scheduledTasks.put(1L, existingFuture);

        // when
        scheduler.scheduleSettlement(1L, LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(10));

        // then
        verify(existingFuture).cancel(false);
        assertThat(scheduledTasks).containsEntry(1L, newFuture);
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancelSchedule_Success() {
        // given
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(false).when(mockFuture).isDone();
        scheduledTasks.put(1L, mockFuture);

        // when
        scheduler.cancelSchedule(1L);

        // then
        verify(mockFuture).cancel(false);
        assertThat(scheduledTasks).doesNotContainKey(1L);
    }

    @Test
    @DisplayName("예약되지 않은 경매 취소 시 예외 없음")
    void cancelSchedule_NotScheduled() {
        // when & then (예외 발생 안 함)
        scheduler.cancelSchedule(999L);
    }

    @Test
    @DisplayName("예약 상태 확인")
    void isScheduled() {
        // given
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(false).when(mockFuture).isDone();
        scheduledTasks.put(1L, mockFuture);

        // when & then
        assertThat(scheduler.isScheduled(1L)).isTrue();
        assertThat(scheduler.isScheduled(999L)).isFalse();
    }

    @Test
    @DisplayName("예약 개수 조회")
    void getScheduledTaskCount() {
        // given
        ScheduledFuture<?> mockFuture1 = mock(ScheduledFuture.class);
        ScheduledFuture<?> mockFuture2 = mock(ScheduledFuture.class);
        doReturn(false).when(mockFuture1).isDone();
        doReturn(false).when(mockFuture2).isDone();

        scheduledTasks.put(1L, mockFuture1);
        scheduledTasks.put(2L, mockFuture2);

        // when & then
        assertThat(scheduler.getScheduledTaskCount()).isEqualTo(2);
    }
}