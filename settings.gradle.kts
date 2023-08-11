/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

fun includeModules(modulesDirName: String = "kohii-samples") {
    val basePath = "$rootDir/$modulesDirName"
    file(basePath).list()?.forEach { dir ->
        if (file("$basePath/$dir/build.gradle").exists() ||
            file("$basePath/$dir/build.gradle.kts").exists()
        ) {
            include(":$modulesDirName:$dir")
        }
    }
}

include(":kohii-core")
include(":kohii-exoplayer")
include(":kohii-androidx")
include(":kohii-ads")
include(":kohii-experiments")
include(":kohii-sample")
include(":kohii-sample-tiktok")

includeModules()
