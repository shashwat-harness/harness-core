/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CommonEntitiesKryoRegistrar;
import io.harness.serializer.kryo.CvNextGenCommonsKryoRegistrar;
import io.harness.serializer.morphia.CommonEntitiesMorphiaRegister;

import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CvNextGenCommonsRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(CvNextGenCommonsKryoRegistrar.class)
          .add(CommonEntitiesKryoRegistrar.class)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(CvNextGenBeansRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(CommonEntitiesMorphiaRegister.class)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .build();
}