# Android runtime dependency provenance

Inventory verified on 2026-10-12 from `app/gradle.lockfile` and the resolved
`debugRuntimeClasspath`: 129 coordinates, 128 Apache-2.0 and one BSD-3-Clause.
This includes BOM constraints and multiplatform facades, not 129 packaged files.
The debug APK includes `ui-test-manifest`; instrumentation libraries and build
tools are outside this runtime inventory.

Full applicable license texts and source copyright attributions ship in
`app/src/main/assets/THIRD_PARTY_NOTICES.txt`, together with TavernDroid's GPL-3.0-only
license in `LICENSE.txt`. The AndroidX AAR/JAR license entries and nested lint JARs
were inspected; no separate NOTICE file was found in that resolved runtime scan.
Do not infer copyright years from organization names or the Apache appendix.

DataStore's shaded protobuf 1.2.1 embeds BSD-3-Clause, despite the surrounding
DataStore modules being Apache-2.0. Its binary license has no holder/year line;
the separately labelled [upstream protobuf attribution](https://github.com/protocolbuffers/protobuf/blob/v3.25.5/LICENSE)
is retained as supplementary provenance. Real Guava ListenableFuture 1.0 contains
code and its [source copyright](https://github.com/google/guava/blob/v26.0/guava/src/com/google/common/util/concurrent/ListenableFuture.java)
is retained; it is not the empty `9999.0-empty-to-avoid-conflict-with-guava` artifact.

JUnit (EPL-1.0), ktlint (MIT), compiler/plugin/test dependencies are not shipped
runtime components. Their complete tooling closure has not been audited here.
Re-resolve, inspect notices and regenerate this inventory when dependencies change.

Each link below points to the exact published artifact POM. Binary license
inspection supplies the shaded protobuf exception where a general family label
would be incorrect.

| Resolved coordinate / published metadata | License |
| --- | --- |
| [androidx.activity:activity-compose:1.13.0](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/1.13.0/activity-compose-1.13.0.pom) | Apache-2.0 |
| [androidx.activity:activity-ktx:1.13.0](https://dl.google.com/dl/android/maven2/androidx/activity/activity-ktx/1.13.0/activity-ktx-1.13.0.pom) | Apache-2.0 |
| [androidx.activity:activity:1.13.0](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0.pom) | Apache-2.0 |
| [androidx.annotation:annotation-experimental:1.5.0](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-experimental/1.5.0/annotation-experimental-1.5.0.pom) | Apache-2.0 |
| [androidx.annotation:annotation-jvm:1.9.1](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-jvm/1.9.1/annotation-jvm-1.9.1.pom) | Apache-2.0 |
| [androidx.annotation:annotation:1.9.1](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation/1.9.1/annotation-1.9.1.pom) | Apache-2.0 |
| [androidx.arch.core:core-common:2.2.0](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-common/2.2.0/core-common-2.2.0.pom) | Apache-2.0 |
| [androidx.arch.core:core-runtime:2.2.0](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-runtime/2.2.0/core-runtime-2.2.0.pom) | Apache-2.0 |
| [androidx.autofill:autofill:1.0.0](https://dl.google.com/dl/android/maven2/androidx/autofill/autofill/1.0.0/autofill-1.0.0.pom) | Apache-2.0 |
| [androidx.collection:collection-jvm:1.5.0](https://dl.google.com/dl/android/maven2/androidx/collection/collection-jvm/1.5.0/collection-jvm-1.5.0.pom) | Apache-2.0 |
| [androidx.collection:collection-ktx:1.5.0](https://dl.google.com/dl/android/maven2/androidx/collection/collection-ktx/1.5.0/collection-ktx-1.5.0.pom) | Apache-2.0 |
| [androidx.collection:collection:1.5.0](https://dl.google.com/dl/android/maven2/androidx/collection/collection/1.5.0/collection-1.5.0.pom) | Apache-2.0 |
| [androidx.compose:compose-bom:2026.09.00](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom) | Apache-2.0 |
| [androidx.compose.animation:animation-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-android/1.12.1/animation-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.animation:animation-core-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-core-android/1.12.1/animation-core-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.animation:animation-core:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-core/1.12.1/animation-core-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.animation:animation:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation/1.12.1/animation-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.foundation:foundation-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.12.1/foundation-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.foundation:foundation-layout-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-layout-android/1.12.1/foundation-layout-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.foundation:foundation-layout:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-layout/1.12.1/foundation-layout-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.foundation:foundation:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation/1.12.1/foundation-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.material:material-ripple-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-ripple-android/1.12.1/material-ripple-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.material:material-ripple:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-ripple/1.12.1/material-ripple-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.material3:material3-android:1.4.0](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0.pom) | Apache-2.0 |
| [androidx.compose.material3:material3:1.4.0](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3/1.4.0/material3-1.4.0.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-android/1.12.1/runtime-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-annotation-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-annotation-android/1.12.1/runtime-annotation-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-annotation:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-annotation/1.12.1/runtime-annotation-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-retain-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-retain-android/1.12.1/runtime-retain-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-retain:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-retain/1.12.1/runtime-retain-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-saveable-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-saveable-android/1.12.1/runtime-saveable-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime-saveable:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-saveable/1.12.1/runtime-saveable-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.runtime:runtime:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime/1.12.1/runtime-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.12.1/ui-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-geometry-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-geometry-android/1.12.1/ui-geometry-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-geometry:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-geometry/1.12.1/ui-geometry-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-graphics-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-graphics-android/1.12.1/ui-graphics-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-graphics:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-graphics/1.12.1/ui-graphics-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-test-manifest:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-manifest/1.12.1/ui-test-manifest-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-text-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-text-android/1.12.1/ui-text-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-text:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-text/1.12.1/ui-text-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-unit-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-unit-android/1.12.1/ui-unit-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-unit:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-unit/1.12.1/ui-unit-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-util-android:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-util-android/1.12.1/ui-util-android-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui-util:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-util/1.12.1/ui-util-1.12.1.pom) | Apache-2.0 |
| [androidx.compose.ui:ui:1.12.1](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui/1.12.1/ui-1.12.1.pom) | Apache-2.0 |
| [androidx.concurrent:concurrent-futures:1.1.0](https://dl.google.com/dl/android/maven2/androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.pom) | Apache-2.0 |
| [androidx.core:core-ktx:1.18.0](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.18.0/core-ktx-1.18.0.pom) | Apache-2.0 |
| [androidx.core:core-viewtree:1.0.0](https://dl.google.com/dl/android/maven2/androidx/core/core-viewtree/1.0.0/core-viewtree-1.0.0.pom) | Apache-2.0 |
| [androidx.core:core:1.18.0](https://dl.google.com/dl/android/maven2/androidx/core/core/1.18.0/core-1.18.0.pom) | Apache-2.0 |
| [androidx.customview:customview-poolingcontainer:1.0.0](https://dl.google.com/dl/android/maven2/androidx/customview/customview-poolingcontainer/1.0.0/customview-poolingcontainer-1.0.0.pom) | Apache-2.0 |
| [androidx.datastore:datastore-android:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-android/1.2.1/datastore-android-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-core-android:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-android/1.2.1/datastore-core-android-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-core-okio-jvm:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-okio-jvm/1.2.1/datastore-core-okio-jvm-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-core-okio:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-okio/1.2.1/datastore-core-okio-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-core:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core/1.2.1/datastore-core-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-preferences-android:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-android/1.2.1/datastore-preferences-android-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-preferences-core-android:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-core-android/1.2.1/datastore-preferences-core-android-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-preferences-core:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-core/1.2.1/datastore-preferences-core-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-preferences-external-protobuf:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-external-protobuf/1.2.1/datastore-preferences-external-protobuf-1.2.1.pom) | BSD-3-Clause |
| [androidx.datastore:datastore-preferences-proto:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-proto/1.2.1/datastore-preferences-proto-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore-preferences:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/1.2.1/datastore-preferences-1.2.1.pom) | Apache-2.0 |
| [androidx.datastore:datastore:1.2.1](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore/1.2.1/datastore-1.2.1.pom) | Apache-2.0 |
| [androidx.emoji2:emoji2:1.4.0](https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2/1.4.0/emoji2-1.4.0.pom) | Apache-2.0 |
| [androidx.graphics:graphics-path:1.0.1](https://dl.google.com/dl/android/maven2/androidx/graphics/graphics-path/1.0.1/graphics-path-1.0.1.pom) | Apache-2.0 |
| [androidx.interpolator:interpolator:1.0.0](https://dl.google.com/dl/android/maven2/androidx/interpolator/interpolator/1.0.0/interpolator-1.0.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-common-java8:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common-java8/2.11.0/lifecycle-common-java8-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-common-jvm:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common-jvm/2.11.0/lifecycle-common-jvm-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-common:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common/2.11.0/lifecycle-common-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-livedata-core:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata-core/2.11.0/lifecycle-livedata-core-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-process:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-process/2.11.0/lifecycle-process-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-android/2.11.0/lifecycle-runtime-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose-android/2.11.0/lifecycle-runtime-compose-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime-compose:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/2.11.0/lifecycle-runtime-compose-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime-ktx-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx-android/2.11.0/lifecycle-runtime-ktx-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime-ktx:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx/2.11.0/lifecycle-runtime-ktx-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime/2.11.0/lifecycle-runtime-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-android/2.11.0/lifecycle-viewmodel-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-compose-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose-android/2.11.0/lifecycle-viewmodel-compose-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/2.11.0/lifecycle-viewmodel-compose-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-ktx/2.11.0/lifecycle-viewmodel-ktx-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate-android/2.11.0/lifecycle-viewmodel-savedstate-android-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/2.11.0/lifecycle-viewmodel-savedstate-2.11.0.pom) | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel:2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel/2.11.0/lifecycle-viewmodel-2.11.0.pom) | Apache-2.0 |
| [androidx.navigation:navigation-common-android:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-common-android/2.10.2/navigation-common-android-2.10.2.pom) | Apache-2.0 |
| [androidx.navigation:navigation-common:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-common/2.10.2/navigation-common-2.10.2.pom) | Apache-2.0 |
| [androidx.navigation:navigation-compose-android:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-compose-android/2.10.2/navigation-compose-android-2.10.2.pom) | Apache-2.0 |
| [androidx.navigation:navigation-compose:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-compose/2.10.2/navigation-compose-2.10.2.pom) | Apache-2.0 |
| [androidx.navigation:navigation-runtime-android:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-runtime-android/2.10.2/navigation-runtime-android-2.10.2.pom) | Apache-2.0 |
| [androidx.navigation:navigation-runtime:2.10.2](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-runtime/2.10.2/navigation-runtime-2.10.2.pom) | Apache-2.0 |
| [androidx.navigationevent:navigationevent-android:1.1.2](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-android/1.1.2/navigationevent-android-1.1.2.pom) | Apache-2.0 |
| [androidx.navigationevent:navigationevent-compose-android:1.1.2](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose-android/1.1.2/navigationevent-compose-android-1.1.2.pom) | Apache-2.0 |
| [androidx.navigationevent:navigationevent-compose:1.1.2](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/1.1.2/navigationevent-compose-1.1.2.pom) | Apache-2.0 |
| [androidx.navigationevent:navigationevent:1.1.2](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent/1.1.2/navigationevent-1.1.2.pom) | Apache-2.0 |
| [androidx.profileinstaller:profileinstaller:1.4.0](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/1.4.0/profileinstaller-1.4.0.pom) | Apache-2.0 |
| [androidx.room:room-common-jvm:2.8.5](https://dl.google.com/dl/android/maven2/androidx/room/room-common-jvm/2.8.5/room-common-jvm-2.8.5.pom) | Apache-2.0 |
| [androidx.room:room-common:2.8.5](https://dl.google.com/dl/android/maven2/androidx/room/room-common/2.8.5/room-common-2.8.5.pom) | Apache-2.0 |
| [androidx.room:room-runtime-android:2.8.5](https://dl.google.com/dl/android/maven2/androidx/room/room-runtime-android/2.8.5/room-runtime-android-2.8.5.pom) | Apache-2.0 |
| [androidx.room:room-runtime:2.8.5](https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/2.8.5/room-runtime-2.8.5.pom) | Apache-2.0 |
| [androidx.savedstate:savedstate-android:1.5.0](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-android/1.5.0/savedstate-android-1.5.0.pom) | Apache-2.0 |
| [androidx.savedstate:savedstate-compose-android:1.5.0](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-compose-android/1.5.0/savedstate-compose-android-1.5.0.pom) | Apache-2.0 |
| [androidx.savedstate:savedstate-compose:1.5.0](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-compose/1.5.0/savedstate-compose-1.5.0.pom) | Apache-2.0 |
| [androidx.savedstate:savedstate-ktx:1.5.0](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-ktx/1.5.0/savedstate-ktx-1.5.0.pom) | Apache-2.0 |
| [androidx.savedstate:savedstate:1.5.0](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate/1.5.0/savedstate-1.5.0.pom) | Apache-2.0 |
| [androidx.sqlite:sqlite-android:2.6.2](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-android/2.6.2/sqlite-android-2.6.2.pom) | Apache-2.0 |
| [androidx.sqlite:sqlite-framework-android:2.6.2](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-framework-android/2.6.2/sqlite-framework-android-2.6.2.pom) | Apache-2.0 |
| [androidx.sqlite:sqlite-framework:2.6.2](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-framework/2.6.2/sqlite-framework-2.6.2.pom) | Apache-2.0 |
| [androidx.sqlite:sqlite:2.6.2](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite/2.6.2/sqlite-2.6.2.pom) | Apache-2.0 |
| [androidx.startup:startup-runtime:1.1.1](https://dl.google.com/dl/android/maven2/androidx/startup/startup-runtime/1.1.1/startup-runtime-1.1.1.pom) | Apache-2.0 |
| [androidx.tracing:tracing:1.2.0](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/1.2.0/tracing-1.2.0.pom) | Apache-2.0 |
| [androidx.versionedparcelable:versionedparcelable:1.1.1](https://dl.google.com/dl/android/maven2/androidx/versionedparcelable/versionedparcelable/1.1.1/versionedparcelable-1.1.1.pom) | Apache-2.0 |
| [androidx.window:window-core-android:1.5.0](https://dl.google.com/dl/android/maven2/androidx/window/window-core-android/1.5.0/window-core-android-1.5.0.pom) | Apache-2.0 |
| [androidx.window:window-core:1.5.0](https://dl.google.com/dl/android/maven2/androidx/window/window-core/1.5.0/window-core-1.5.0.pom) | Apache-2.0 |
| [androidx.window:window:1.5.0](https://dl.google.com/dl/android/maven2/androidx/window/window/1.5.0/window-1.5.0.pom) | Apache-2.0 |
| [com.google.guava:listenablefuture:1.0](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.pom) | Apache-2.0 |
| [com.squareup.okio:okio-jvm:3.9.1](https://repo.maven.apache.org/maven2/com/squareup/okio/okio-jvm/3.9.1/okio-jvm-3.9.1.pom) | Apache-2.0 |
| [com.squareup.okio:okio:3.9.1](https://repo.maven.apache.org/maven2/com/squareup/okio/okio/3.9.1/okio-3.9.1.pom) | Apache-2.0 |
| [org.jetbrains:annotations:23.0.0](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0.pom) | Apache-2.0 |
| [org.jetbrains.kotlin:kotlin-stdlib:2.4.21](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.4.21/kotlin-stdlib-2.4.21.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.11.0/kotlinx-coroutines-android-1.11.0.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-bom/1.11.0/kotlinx-coroutines-bom-1.11.0.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.11.0/kotlinx-coroutines-core-jvm-1.11.0.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.11.0/kotlinx-coroutines-core-1.11.0.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-bom:1.7.3](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-bom/1.7.3/kotlinx-serialization-bom-1.7.3.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/1.7.3/kotlinx-serialization-core-jvm-1.7.3.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/1.7.3/kotlinx-serialization-core-1.7.3.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.7.3/kotlinx-serialization-json-jvm-1.7.3.pom) | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/1.7.3/kotlinx-serialization-json-1.7.3.pom) | Apache-2.0 |
| [org.jspecify:jspecify:1.0.0](https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.pom) | Apache-2.0 |
