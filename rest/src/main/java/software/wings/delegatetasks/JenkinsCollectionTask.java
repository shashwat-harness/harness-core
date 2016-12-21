package software.wings.delegatetasks;

import static software.wings.delegatetasks.DelegateFile.DelegateFileBuilder.aDelegateFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(JenkinsCollectionTask.class);
  public static final String BUILD_NO = "buildNo";

  @Inject private JenkinsFactory jenkinsFactory;
  @Inject private DelegateFileManager delegateFileManager;

  public JenkinsCollectionTask(
      String delegateId, DelegateTask delegateTask, Consumer<ListNotifyResponseData> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (String) parameters[2], (String) parameters[3],
        (List<String>) parameters[4], (Map<String, String>) parameters[5]);
  }

  public ListNotifyResponseData run(String jenkinsUrl, String username, String password, String jobName,
      List<String> artifactPaths, Map<String, String> arguments) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      Jenkins jenkins = jenkinsFactory.create(jenkinsUrl, username, password);

      for (String artifactPath : artifactPaths) {
        Pair<String, InputStream> fileInfo = jenkins.downloadArtifact(jobName, arguments.get(BUILD_NO), artifactPath);
        if (fileInfo == null) {
          throw new FileNotFoundException("Unable to get artifact from jenkins for path " + artifactPath);
        }
        in = fileInfo.getValue();

        DelegateFile delegateFile = aDelegateFile()
                                        .withFileName(fileInfo.getKey())
                                        .withDelegateId(getDelegateId())
                                        .withTaskId(getTaskId())
                                        .withAccountId(getAccountId())
                                        .build(); // TODO: more about delegate and task info
        DelegateFile fileRes = delegateFileManager.upload(delegateFile, in);

        ArtifactFile artifactFile = new ArtifactFile();
        artifactFile.setFileUuid(fileRes.getFileId());
        artifactFile.setName(fileInfo.getKey());
        res.addData(artifactFile);
      }
    } catch (Exception e) {
      logger.warn("Exception: ", e);
      // TODO: better error handling

      //      if (e instanceof WingsException)
      //        WingsException ex = (WingsException) e;
      //        errorMessage = Joiner.on(",").join(ex.getResponseMessageList().stream()
      //            .map(responseMessage ->
      //            ResponseCodeCache.getInstance().getResponseMessage(responseMessage.getCode(),
      //            ex.getParams()).getMessage()) .collect(toList()));
      //      } else {
      //        errorMessage = e.getMessage();
      //      }
      //      executionStatus = executionStatus.FAILED;
      //      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    } finally {
      IOUtils.closeQuietly(in);
    }

    return res;
  }
}
