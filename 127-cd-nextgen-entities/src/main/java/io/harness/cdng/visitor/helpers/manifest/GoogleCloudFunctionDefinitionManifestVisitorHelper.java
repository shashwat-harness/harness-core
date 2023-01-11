/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.GoogleCloudFunctionDefinitionManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class GoogleCloudFunctionDefinitionManifestVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    GoogleCloudFunctionDefinitionManifest googleCloudFunctionDefinitionManifest =
        (GoogleCloudFunctionDefinitionManifest) originalElement;
    return GoogleCloudFunctionDefinitionManifest.builder()
        .identifier(googleCloudFunctionDefinitionManifest.getIdentifier())
        .build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }
}