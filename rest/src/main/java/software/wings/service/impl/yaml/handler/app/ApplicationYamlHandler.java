package software.wings.service.impl.yaml.handler.app;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.APPLICATION;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.Application.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.AppService;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class ApplicationYamlHandler extends BaseYamlHandler<Application.Yaml, Application> {
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject AppService appService;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Application application = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (application != null) {
      appService.delete(application.getUuid());
    }
  }

  @Override
  public Application.Yaml toYaml(Application application, String appId) {
    return Application.Yaml.Builder.anApplicationYaml()
        .withType(APPLICATION.name())
        .withDescription(application.getDescription())
        .build();
  }

  @Override
  public Application upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);
    String accountId = changeContext.getChange().getAccountId();
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appName = yamlSyncHelper.getAppName(yamlFilePath);
    Application current =
        anApplication().withAccountId(accountId).withName(appName).withDescription(yaml.getDescription()).build();
    Application previous = get(accountId, yamlFilePath);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return appService.update(current);
    } else {
      return appService.save(current);
    }
  }

  @Override
  public Application updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public Application createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Application.Yaml.class;
  }

  @Override
  public Application get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getApp(accountId, yamlFilePath);
  }
}
