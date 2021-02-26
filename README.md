Simplified-R2-Android
=====================

[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.librarysimplified.r2/org.librarysimplified.r2.api?style=flat-square)](https://repo1.maven.org/maven2/org/librarysimplified/r2/)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.librarysimplified.r2/org.librarysimplified.r2.api.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/org/librarysimplified/r2/org.librarysimplified.r2.api/)

The [Library Simplified](http://www.librarysimplified.org/) Android Readium 2 navigator component.

|Build|Status|
|-----|------|
|[Nightly, DRM, JDK 11](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20Authenticated,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|
|[Nightly, DRM-Free, JDK 11](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20DRM-Free,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+11%29%22)|
|[Nightly, DRM-Free, JDK 15](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+15%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20DRM-Free,%20JDK%2015)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+15%29%22)|
|[Last Commit](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|

### What Is This?

The contents of this repository define an API and a set of views for working with
the [Readium 2](https://readium.org/technical/r2-toc/) library.

### Architecture

![Architecture](./src/site/resources/arch.png?raw=true)

### Modules

|Module|Description|
|------|-----------|
|[org.librarysimplified.r2.api](org.librarysimplified.r2.api)|R2 API|
|[org.librarysimplified.r2.demo](org.librarysimplified.r2.demo)|R2 demo application|
|[org.librarysimplified.r2.ui_thread](org.librarysimplified.r2.ui_thread)|R2 UI Thread service|
|[org.librarysimplified.r2.vanilla](org.librarysimplified.r2.vanilla)|R2 vanilla implementation|
|[org.librarysimplified.r2.views](org.librarysimplified.r2.views)|R2 views|
