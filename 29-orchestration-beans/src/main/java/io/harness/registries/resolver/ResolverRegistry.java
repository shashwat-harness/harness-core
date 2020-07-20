package io.harness.registries.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.resolvers.Resolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(CDC)
@Redesign
@Singleton
public class ResolverRegistry implements Registry<RefType, Class<? extends Resolver<?>>> {
  @Inject private Injector injector;

  private Map<RefType, Class<? extends Resolver<?>>> registry = new ConcurrentHashMap<>();

  public void register(RefType refType, Class<? extends Resolver<?>> resolver) {
    if (registry.containsKey(refType)) {
      throw new DuplicateRegistryException(getType(), "Resolver Already Registered with this type: " + refType);
    }
    registry.put(refType, resolver);
  }

  public Resolver obtain(RefType refType) {
    if (registry.containsKey(refType)) {
      return injector.getInstance(registry.get(refType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No Resolver registered for type: " + refType);
  }

  @Override
  public String getType() {
    return RegistryType.RESOLVER.name();
  }
}
