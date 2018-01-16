package software.wings.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.exception.WingsException.Scenario.BACKGROUND_JOB;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.MockTest;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;

import java.time.Duration;

/**
 * The Class PersistentLockerTest.
 */
public class PersistentLockerTest extends MockTest {
  @Mock private DistributedLockSvc distributedLockSvc;

  @Inject @InjectMocks private PersistentLocker persistentLocker;

  @Test
  public void testAcquireLockDoLock() {
    Duration timeout = Duration.ofMillis(1000);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);

    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(true);
    when(distributedLockSvc.create(matches("abc-cba"), any())).thenReturn(distributedLock);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba", Duration.ofMinutes(1))) {
    }

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(1)).unlock();
  }

  @Test
  public void testAcquireLockDoNotRunTheBody() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create(matches("abc-cba"), any())).thenReturn(distributedLock);

    boolean body = false;
    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba", Duration.ofMinutes(1))) {
      body = true;
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("GENERAL_ERROR");
    }

    assertThat(body).isFalse();

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(0)).unlock();
  }

  @Test
  public void testAcquireLockNonLockedAtRelease() {
    Duration timeout = Duration.ofMillis(1000);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);

    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(false);
    when(distributedLockSvc.create(matches("abc-cba"), any())).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);
    Whitebox.setInternalState(new AcquiredLock(null, 0L), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba", timeout)) {
    }

    verify(logger).error(matches("attempt to release lock that is not currently locked"), any(Throwable.class));
  }

  @Test
  public void testAcquireLockLogging() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create(matches("abc-cba"), any())).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);

    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", logger);
    Whitebox.setInternalState(new WingsException(""), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba", Duration.ofMinutes(1))) {
    } catch (WingsException exception) {
      exception.logProcessedMessages(BACKGROUND_JOB);
    }

    verify(logger, times(0)).error(any());
  }

  @Test
  public void testAcquireTimeout() throws InterruptedException {
    Duration timeout = Duration.ofMillis(1);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);
    when(distributedLock.getName()).thenReturn("abc-cba");
    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLockSvc.create(matches("abc-cba"), any())).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);

    Whitebox.setInternalState(new AcquiredLock(null, 0L), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba", timeout)) {
      Thread.sleep(10);
    } catch (WingsException exception) {
      exception.logProcessedMessages(BACKGROUND_JOB);
    }

    verify(logger).error(matches(
        "The distributed lock abc-cba was not released on time. THIS IS VERY BAD!!!, elapsed: \\d+, timeout 1"));
  }
}
