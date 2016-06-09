package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorCodes.UNKNOWN_EXECUTOR_TYPE_ERROR;

import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * A factory for creating SshExecutor objects.
 */
public class SshExecutorFactory {
  @Inject private FileService fileService;
  @Inject private LogService logService;

  /**
   * Gets the executor.
   *
   * @param executorType the executor type
   * @return the executor
   */
  public SshExecutor getExecutor(ExecutorType executorType) {
    SshExecutor executor;
    switch (executorType) {
      case PASSWORD_AUTH:
        executor = new SshPwdAuthExecutor(fileService, logService);
        break;
      case KEY_AUTH:
        executor = new SshPubKeyAuthExecutor(fileService, logService);
        break;
      case BASTION_HOST:
        executor = new SshJumpboxExecutor(fileService, logService);
        break;
      default:
        throw new WingsException(UNKNOWN_EXECUTOR_TYPE_ERROR, new Throwable("Unknown executor type: " + executorType));
    }
    return executor;
  }
}
