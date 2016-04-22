package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.*;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;
import software.wings.utils.HostFileHelper;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.*;
import java.util.List;

@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Infra> listInfra(String envId, PageRequest<Infra> req) {
    return wingsPersistence.query(Infra.class, req);
  }

  @Override
  public Infra createInfra(Infra infra, String envId) {
    infra.setEnvID(envId);
    return wingsPersistence.saveAndGet(Infra.class, infra);
  }

  @Override
  public PageResponse<Host> listHosts(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host getHost(String infraId, String hostId) {
    return wingsPersistence.get(Host.class, hostId);
  }

  @Override
  public Host createHost(String infraId, Host host) {
    host.setInfraID(infraId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Host updateHost(String infraId, Host host) {
    host.setInfraID(infraId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Tag createTag(String envId, Tag tag) {
    tag.setEnvID(envId);
    return wingsPersistence.saveAndGet(Tag.class, tag);
  }

  @Override
  public Host applyTag(String hostId, String tagId) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    Host host = wingsPersistence.get(Host.class, hostId);
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).add("tags", tag);
    wingsPersistence.update(host, updateOp);
    return wingsPersistence.get(Host.class, hostId);
  }

  @Override
  public Integer importHosts(String infraId, InputStream inputStream, HostFileType fileType) {
    Infra infra = wingsPersistence.get(Infra.class, infraId); // TODO: validate infra
    List<Host> hosts = HostFileHelper.parseHosts(inputStream, infraId, fileType);
    List<String> IDs = wingsPersistence.save(hosts);
    return IDs.size();
  }

  @Override
  public File exportHosts(String infraId, HostFileType fileType) {
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraID").equal(infraId).asList();
    return HostFileHelper.createHostsFile(hosts, fileType);
  }
}
