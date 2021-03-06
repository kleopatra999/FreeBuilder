/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inferred.freebuilder.processor;

import static com.google.common.truth.Truth.assertThat;
import static org.inferred.freebuilder.processor.BuilderFactory.BUILDER_METHOD;
import static org.inferred.freebuilder.processor.BuilderFactory.NEW_BUILDER_METHOD;
import static org.inferred.freebuilder.processor.BuilderFactory.NO_ARGS_CONSTRUCTOR;
import static org.inferred.freebuilder.processor.BuilderFactory.TypeInference.EXPLICIT_TYPES;
import static org.inferred.freebuilder.processor.BuilderFactory.TypeInference.INFERRED_TYPES;
import static org.inferred.freebuilder.processor.util.feature.SourceLevel.JAVA_6;
import static org.inferred.freebuilder.processor.util.feature.SourceLevel.JAVA_7;

import javax.lang.model.element.TypeElement;

import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.ParameterizedType;
import org.inferred.freebuilder.processor.util.QualifiedName;
import org.inferred.freebuilder.processor.util.SourceStringBuilder;
import org.inferred.freebuilder.processor.util.testing.ModelRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.base.Optional;

@RunWith(JUnit4.class)
public class BuilderFactoryTest {

  @ClassRule public static final ModelRule model = new ModelRule();

  @Test
  public void testImplicitConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class ImplicitConstructor {",
        "  ---> class Builder {}",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NO_ARGS_CONSTRUCTOR);
  }

  @Test
  public void testAbstractInnerClass() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class AbstractInnerClass {",
        "  ---> abstract class Builder {}",
        "}");

    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testExplicitNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class ExplicitNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder() {}",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NO_ARGS_CONSTRUCTOR);
  }

  @Test
  public void testExplicitPrivateNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class ExplicitProtectedNoArgsConstructor {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testMissingNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class MissingNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder(int ignored) {}",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testBuilderFactoryMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class BuilderFactoryMethod {",
        "  ---> class Builder {}",
        "",
        "  static Builder builder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(BUILDER_METHOD);
  }

  @Test
  public void testBuilderFactoryMethod_privateNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class BuilderFactoryMethod_PrivateNoArgsConstructor {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static Builder builder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(BUILDER_METHOD);
  }

  @Test
  public void testBuilderFactoryMethod_visibleNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class BuilderFactoryMethod_VisibleNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder() {}",
        "  }",
        "",
        "  static Builder builder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(BUILDER_METHOD);
  }

  @Test
  public void testPrivateBuilderFactoryMethod_visibleNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class PrivateBuilderFactoryMethod_VisibleNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder() {}",
        "  }",
        "",
        "  private static Builder builder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NO_ARGS_CONSTRUCTOR);
  }

  @Test
  public void testParameterTakingBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class ParameterTakingBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static Builder builder(int ignored) {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNonBuilderReturningBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NonBuilderReturningBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static MissingNoArgsConstructor builder() {",
        "    return null;",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNonStaticBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NonStaticBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  Builder builder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNewBuilderFactoryMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NewBuilderFactoryMethod {",
        "  ---> class Builder {}",
        "",
        "  static Builder newBuilder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NEW_BUILDER_METHOD);
  }

  @Test
  public void testNewBuilderFactoryMethod_privateNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NewBuilderFactoryMethod_PrivateNoArgsConstructor {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static Builder newBuilder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NEW_BUILDER_METHOD);
  }

  @Test
  public void testNewBuilderFactoryMethod_visibleNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NewBuilderFactoryMethod_VisibleNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder() {}",
        "  }",
        "",
        "  static Builder newBuilder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NEW_BUILDER_METHOD);
  }

  @Test
  public void testPrivateNewBuilderFactoryMethod_visibleNoArgsConstructor() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class PrivateNewBuilderFactoryMethod_VisibleNoArgsConstructor {",
        "  ---> class Builder {",
        "    Builder() {}",
        "  }",
        "",
        "  private static Builder newBuilder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).hasValue(NO_ARGS_CONSTRUCTOR);
  }

  @Test
  public void testParameterTakingNewBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class ParameterTakingNewBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static Builder newBuilder(int ignored) {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNonBuilderReturningNewBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NonBuilderReturningNewBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  static MissingNoArgsConstructor newBuilder() {",
        "    return null;",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNonStaticNewBuilderMethod() {
    TypeElement builderType = (TypeElement) model.newElementWithMarker(
        "package com.example;",
        "class NonStaticNewBuilderMethod {",
        "  ---> class Builder {",
        "    private Builder() {}",
        "  }",
        "",
        "  Builder newBuilder() {",
        "    return new Builder();",
        "  }",
        "}");
    Optional<BuilderFactory> factory = BuilderFactory.from(builderType);
    assertThat(factory).isAbsent();
  }

  @Test
  public void testNewBuilderForGenericType_noArgsConstructor_inferredTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NO_ARGS_CONSTRUCTOR.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("new Foo.Builder<E>()");
  }

  @Test
  public void testNewBuilderForGenericType_noArgsConstructor_explicitTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NO_ARGS_CONSTRUCTOR.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("new Foo.Builder<E>()");
  }

  @Test
  public void testNewBuilderForGenericType_noArgsConstructor_inferredTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NO_ARGS_CONSTRUCTOR.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("new Foo.Builder<>()");
  }

  @Test
  public void testNewBuilderForGenericType_noArgsConstructor_explicitTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NO_ARGS_CONSTRUCTOR.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("new Foo.Builder<E>()");
  }

  @Test
  public void testNewBuilderForGenericType_builderMethod_inferredTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = BUILDER_METHOD.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.builder()");
  }

  @Test
  public void testNewBuilderForGenericType_builderMethod_explicitTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = BUILDER_METHOD.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.<E>builder()");
  }

  @Test
  public void testNewBuilderForGenericType_builderMethod_inferredTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = BUILDER_METHOD.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.builder()");
  }

  @Test
  public void testNewBuilderForGenericType_builderMethod_explicitTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = BUILDER_METHOD.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.<E>builder()");
  }

  @Test
  public void testNewBuilderForGenericType_newBuilderMethod_inferredTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NEW_BUILDER_METHOD.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.newBuilder()");
  }

  @Test
  public void testNewBuilderForGenericType_newBuilderMethod_explicitTypes_j6() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NEW_BUILDER_METHOD.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_6).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.<E>newBuilder()");
  }

  @Test
  public void testNewBuilderForGenericType_newBuilderMethod_inferredTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NEW_BUILDER_METHOD.newBuilder(fooBuilder, INFERRED_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.newBuilder()");
  }

  @Test
  public void testNewBuilderForGenericType_newBuilderMethod_explicitTypes_j7() {
    ParameterizedType fooBuilder =
        QualifiedName.of("com.example", "Foo", "Builder").withParameters("E");
    Excerpt newFooBuilder = NEW_BUILDER_METHOD.newBuilder(fooBuilder, EXPLICIT_TYPES);
    String code = SourceStringBuilder.simple(JAVA_7).add(newFooBuilder).toString();
    assertThat(code).isEqualTo("Foo.<E>newBuilder()");
  }
}
