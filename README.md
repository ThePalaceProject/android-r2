Simplified-R2-Android
=====================

[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.librarysimplified.r2/org.librarysimplified.r2.api?style=flat-square)](https://repo1.maven.org/maven2/org/librarysimplified/r2/)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.librarysimplified.r2/org.librarysimplified.r2.api.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/org/librarysimplified/r2/org.librarysimplified.r2.api/)

The [Library Simplified](http://www.librarysimplified.org/) Android Readium 2 navigator component.

![r2](./src/site/resources/r2.jpg?raw=true)

_Image by [Jose Antonio Alba](https://pixabay.com/users/josealbafotos-1624766/) from [Pixabay](https://pixabay.com/photos/leaves-books-color-coffee-cup-1076307/)_

|Build|Status|
|-----|------|
|[Nightly, Authenticated, JDK 11](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20Authenticated,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|
|[Nightly, Unauthenticated, JDK 11](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Unauthenticated%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20Unauthenticated,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Unauthenticated%2C+JDK+11%29%22)|
|[Nightly, Unauthenticated, JDK 15](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Unauthenticated%2C+JDK+15%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Daily%20Unauthenticated,%20JDK%2015)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Daily+Unauthenticated%2C+JDK+15%29%22)|
|[Last Commit](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-R2-Android/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-R2-Android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|

### What Is This?

The contents of this repository define an API and a set of views for working with
the [Readium 2](https://readium.org/technical/r2-toc/) library.

Make sure you clone this repository with `git clone --recursive`. 
If you forgot to use `--recursive`, then execute:

```
$ git submodule init
$ git submodule update --remote --recursive
```

### Architecture

The architecture of the project is modularized and carefully structured to separate
presentation and logic, and to clearly distinguish between functionality provided by
the project and functionality that must be provided by the hosting application.

![Architecture](./src/site/resources/arch.png?raw=true)

### Controller

The [org.librarysimplified.r2.api](org.librarysimplified.r2.api) module defines a _controller API_ that accepts
commands and publishes events in response to those commands. A _controller_ encapsulates
a Readium [Publication](https://readium.org/webpub-manifest/) and an internal server
used to expose resources from that `Publication`. A single _controller_ instance has
a lifetime matching that of the `Publication`; when the user wants to open a book,
a new _controller_ instance is created for that book, and then destroyed when the
user closes the book.

#### Commands

The _controller_ accepts commands in the form of values of a sealed `SR2Command` type. Commands
are accepted by the controller and executed serially and asynchronously. The _controller_ is 
thread-safe and commands can be submitted from any thread - a single-threaded executor is used 
internally to execute commands in submission order and without interleaving command executions.

#### Events

The _controller_ exposes an [rxjava](https://github.com/ReactiveX/RxJava) `Observable` stream
of events to which subscribers can react. The events are values of a sealed `SR2Event` type,
so consumers of the events are given static guarantees that they have handled all possible events
(assuming that they use total `when` expressions to examine the events).

#### Web Views

Readium requires a [WebView](https://developer.android.com/guide/webapps/webview)
in order to execute JavaScript code, the controller API provides methods to dynamically
attach and detach a `WebView` in accordance with the Android Fragment lifecycle; the
_controller_ is responsible for effectively managing state such as the currently
selected color scheme, the current reading position, and is responsible for correctly
restoring this state each time a `WebView` is (re)connected.

### Views

The [org.librarysimplified.r2.views](org.librarysimplified.r2.views) module defines a set of Android [Fragments](https://developer.android.com/guide/fragments)
that implement a simple user interface for displaying a book and allowing the user to
manage bookmarks and choose items from the book's table of contents. The fragments are
conceptually stateless views that simply respond to events published by the current
_controller_ instance. The fragments communicate by a shared, public [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel),
with the _controller_ instance being stored in this `ViewModel`.

### Usage

The user's application has the following responsibilities:

  * The application _must_ instantiate the `SR2ReaderFragment` and attach it to an activity.

  * The application _should_ subscribe to the _controller_ instance's event stream in order
    to perform actions such as saving and retrieving bookmarks from its own internal
    database. Failing to handle events will merely result in bookmarks not being saved.
    
  * The application _must_ respond to requests from the fragments to instantiate other
    fragments, and to pop the backstack as necessary. For example, when the user clicks
    the _table of contents_ button in the `SR2ReaderFragment`, the `ViewModel` will
    publish an event indicating that the user's application should now instantiate and
    attach a `SR2TOCFragment` in order to show the table of contents. Failing to handle
    these events will merely result in a UI that does nothing when the user selects
    various menu items.

An extremely minimal [demo application](org.librarysimplified.r2.demo) is included that
describes the bare minimum an application can do in order to provide a basic reading
experience.

### Modules

|Module|Description|
|------|-----------|
|[org.librarysimplified.r2.api](org.librarysimplified.r2.api)|R2 API|
|[org.librarysimplified.r2.demo](org.librarysimplified.r2.demo)|R2 demo application|
|[org.librarysimplified.r2.ui_thread](org.librarysimplified.r2.ui_thread)|R2 UI Thread service|
|[org.librarysimplified.r2.vanilla](org.librarysimplified.r2.vanilla)|R2 vanilla implementation|
|[org.librarysimplified.r2.views](org.librarysimplified.r2.views)|R2 views|
