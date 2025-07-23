# poket
Small multi-implementation facade utilities around data handling in distributed
environments. Advanced backend features, always in your poket.

Currently it provides utilities for: distributed caches and mutex, backend
configuration and generic transaction support.

## Table of Contents
* [The problem](#problem)  
* [The solution](#solution)
* [Poket library philosophy](#philosophy)
* [Usage](#usage)
  * [Gradle dependencies](#gradle)
  * [Dependency injection](#dependency-injection)
  * [Configuration](#configuration)
  * [Optional injected objects](#optional-injected-objects)
  * [Object cache](#object-cache)
  * [Distributed mutex](#distributed-mutex)
  * [Generic transactions](#generic-transactions)

<a name="problem"></a>
## The problem

When you need to create a backend service, you usually choose:
* Some web framework (or not web e.g. gRPC), providing an
interface to accept calls from the outside.
* Some dependency injection engine, provided by the web framework or not.
* Some database driver and access library.

Just this is enough to create your backend. But then it grows and grows, and
starts getting more complicated, and you start needing features like:
* Caching
* Distributed mutex

Then you implement them.
It's not really complicated, you install some cache
server like Redis or Memcached and you implement locks with your database or Redis.
And you use some of the libraries that exist for accessing this, such as Redisson
or Lettuce. And it works.

But then you start having problems with that functionality. For example:
* Your cache fails because you changed the format of the stored data.
* Your cache stores incorrect data that gets from other service, and when that
service is fixed in the middle of the night, the incorrect data is still in your
cache. And fixing that takes a long time.
* Your cache 
* A mutex gets stuck and many requests have timeout errors when reaching it.

And you realize that:
1. Fixing any of those problems takes too much time, as you need to change your
code and run a new build.
2. You don't have visibility of how well are your caches and your mutex working.
What's the hit rate? How much processing time they are saving, if any?
3. You are duplicating the same code again and again.

Not just that, as your backend starts getting more and more requests, you
start having new optimization needs in your backend:
* You realize that one of your caches is getting data from a very slow source
and receives many requests in a short time, so it makes many concurrent requests
to that same source.
* You need to get or generate multiple keys from your cache, and write some ugly
code to generate only the keys that are not already in the cache.
* You realize that you could speed up some slow generation if you return the
element in cache and you generate and store a new value with a background process.
* You realize that some cache takes too much time reading and deserializing some
large data from Redis, and start thinking of trying to replace it for some memory
based cache like Caffeine. But that would require changing your whole cache access
code.

Also, you start getting some problems related to transactions:
Your DB model starts getting more complicated. You encapsulate the accesses to a table
in some data access class, let's call it Repository, DAL, DAO or whatever other name.
Then you need to get data from several tables at once, and you start using that
DB access class for more than one table.

And then, you start needing to delete data from several tables inside a single
transaction. And you need to choose between:
* Creating the DB transaction in the service layer, although that implies using
the DB library and stop hiding the low level access to it.
* Make a data access class create the transaction and call the data access
classes that handle the other tables, to delete this data. And you start creating
circular dependencies between these data access classes, because you are not
creating clear dependencies between them, but rather creating them because you
need a transaction. So that turns into spaghetti code.
* Access directly different tables from the data access class, although the access
to other tables is theoretically encapsulated in other data access classes.

So all this starts getting messy some way. You are moderately happy with you somehow
dirty solution, but then you start having more problems:
* You store data that you get from the database in a cache. But then you start
having incidents because some functionality is using data that is not the data
you know you had in the database. And then you find out that the database transaction
was rolled back. But the new data was stored in the DB, and that wasn't rolled back.
* In other cases, someone gets data from the cache, but when they access the database
the DB transaction hasn't been committed yet, so there's a race condition there.
* You call a service to do some action, and then you store something related to it
in your database. But it fails, and the transaction is rolled back. But the external
service has already changed the status. You have heard of distributed transactions,
and you are really scared of them. You have also heard of the saga pattern, but
it also looks difficult to implement in the context of a rolled back transaction.

<a name="solution"></a>
## The solution
Wouldn't it be nice having some library that solves all these small problems
that are not the core of a backend, but that you are going to find before or later
as your backend grows?

Wouldn't it be nice having the solution to these little problems... **in your poket**?

Poket provides:

### Distributed caches
* Using the same interface and added features for any cache underlying library,
for the moment:
  * Lettuce (redis)
  * Redisson (redis)
  * Caffeine (local data with TTL)
* With added features like:
  * Easily configurable for any specific cache or for all of them, especially:
    * Version of the stored data
    * Disable a problematic cache
    * The library you are using to store the data
    * The TTL of your data
  * Easy "get or put" function. Also including easy handling of multi-key
  "get or put"
  * Optional request collapsing (i.e. combine multiple identical requests for
  the same resource into a single request)
  * Outdate time: entries can have an outdate time < TTL, where current cached
  data is returned, and new data is fetched in a background process, making
  the original request faster.
  * Compatible with various serialization libraries, providing at the moment
  bindings for Jackson (more bindings are easily to develop)
 
### Distributed mutex

* Using the same interface and added features for any cache underlying library,
  for the moment:
    * Lettuce (redis)
    * Redisson (redis)
    * Local mutex with TTL (only working for a single node)
* With added features like:
    * Easily configurable for any specific cache or for all of them, especially:
        * Behavior in case of an error trying to get the mutex
        * Timeout trying to get the mutex
        * TTL of the mutex guard
    * Define a fallback system if the original fails. E.g. we can use local
    locks if Redis fails, so that at least we guarantee exclusive access at
    the instance level.

### General transaction support

* Two-phase transaction protocol with multiple backends
  * First phase should have only a "main" backend with guaranteed atomicity.
  This will usually be the database.
  * Second phase should include other secondary backends with no guaranteed
  atomicity. They will be rolled back in case of an error and will run some
  action in that case, or the opposite, will run some action after
  a commit. This does not have a guarantee, but will reduce greatly the
  probability of losing data integrity.
* The interface is generic, so it can be used in the service layer.
Same as the service layer knows that there is some data that needs to be persisted,
but doesn't know how, the service layer knows what data needs to be persisted
as an all-or-nothing operation.
* These are some of the added features:
  * Compatible with Poket caches, i.e., any operation done to a Poket cache
  in the context of a transaction, will only succeed in case of a commit.
  * Implementation of the Saga pattern in transactions. You can run an action
  in a transaction, and provide a compensation action to be run in case of
  a rollback.
  * Transaction hooks, allowing running some action right after a transaction
  commit.
  
### Other smaller utilities
* Configurable retrier
* Rate limiter: cache and mutex based

### Common to all functionalities
* Metrics for everything
* Hot reloading of configuration. So if e.g. you get the configuration from an
external system or a k8s descriptor, you don't need to build and redeploy
your whole project, just changing the configuration makes everything work.
* Easy binding of the configuration to almost any different type of configuration
system you are using (provided by your framework or any library).
* And if you don't have a configuration system, it provides one.
Poket configuration system is able to use different sources with overrides,
providing any or all of these sources:
  * YAML files
  * Property files
  * Environment variables
  * Injected configuration objects


<a name="phylosophy"></a>
## Poket library philosophy

The utilities have been created with this approach:
* Generic facades, not bound to any specific storage.
* Configurable parameters, with hot reload.
* Configuration based on overrides, with a base configuration for
all profiles and a configuration by profile.
* Coroutines based. But alternative blocking interfaces are always provided.
* Adaptable to your favourite framework, with any configuration type, dependency injection
engine and/or serialization.
* Use pure Kotlin libraries whenever it's possible. Currently, most of the codebase
is 100% kotlin, and some day it's expected to turn into a multiplatform project.

<a name="usage"></a>
## Usage

<a name="gradle"></a>
### Gradle dependencies

```
poket-xxx = { module = "io.github.andresviedma.poket:poket-xxx", version = "z.z.z" }
```

| Library         | Description                                                                                       |
|-----------------|---------------------------------------------------------------------------------------------------|
| poket-facade    | Core library, with the interface for all features.                                                |
| poket-guice     | Dependency injection with [Guice](https://github.com/google/guice/wiki/).                         |
| poket-koin      | Dependency injection with [Koin](https://insert-koin.io/).                                                               |
| poket-jackson   | Serialization with [Jackson JSON library](https://github.com/FasterXML/jackson).                  |
| poket-snakeyaml | Serialization with [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/src/master/).            |
| poket-lettuce   | Backend implementations for [Lettuce Redis library](https://redis.github.io/lettuce/).            |
| poket-redisson  | Backend implementations for [Redisson Redis library](https://redisson.pro/).                      |
| poket-caffeine  | Backend implementations for [Caffeine memory-based cache](https://github.com/ben-manes/caffeine). |

<a name="dependency-injection"></a>
### Dependency injection

Poket provides the dependencies in the form of generic "Bindings", that are passed
to the specific dependency injection library to do all the magic.

For all the libraries you want to use, you can find its bindings in a `Bindings.kt`
file in the main package of the library.

#### Guice

```kotlin
class MyModule ... {
    override fun configure() {
        // Poket bindings
        install(
            GuicePoketModule(
                poketCoreBindings,
                injectGuiceBindings,
                // any other bindings variable you'll find in Bindings.kt files
                // for your backend / serialization libraries
                (...)
            )
        )
      
        // Config using ... (whatever you want to use as your config)
        val configBinder = KotlinMultibinder.newSetBinder<ConfigSource>(binder())
        configBinder.addBinding().to...  // do your magic
    }
}
```

#### Koin

```kotlin
startKoin {
    modules(
        poketModule(
            poketCoreBindings,
            injectGuiceBindings,
            // any other bindings variable you'll find in Bindings.kt files
            // for your backend / serialization libraries
            (...)
        )
    )
}

class MyModule ... {
    override fun configure() {
        // Poket bindings
        install(
            GuicePoketModule(
                poketCoreBindings,
                injectGuiceBindings,
                // any other bindings variable you'll find in Bindings.kt files
                // for your backend / serialization libraries
                (...) 
            )
        )
      
        // Config using ... (whatever you want to use as your config)
        single { /* do your magic */ }.bind<ConfigSource>()
    }
}
```

<a name="configuration"></a>
### Configuration

The only config source bound by default is using the dependency injection engine.
So for example if you are using koin you can create configuration objects like
this:

```kotlin
single {
    MutexTypeConfig(
        lockSystem = "lettuce-redis",
        timeoutInMillis = 10_000,
        ttlInMillis = 10_000,
    )
}
```

In order to implement other sources, class PropertyTreeConfigSource is able to
get configuration as a tree of properties, using key-value properties or json-like
tree maps.

As an example using koin and some files, you could do something like this:

```kotlin
single {
    PropertyTreeConfigSource(
        propertySources = setOf(
            ConfigPropertiesSource {
                Properties().apply {
                    load(File("/.../myfile.properties").bufferedReader())
                } as Map<String, String>
            },
        ),
        treeSources = setOf(
            YamlConfigTreeSource(File("/.../myfile.yaml").bufferedReader()),  
        ),
        configClassBindings = setOf(
            // Prefix for each config, same as property and as yaml
            ConfigClassBindingList(
                "mutex" to MutexConfig::class,
                "cache" to CacheConfig::class,
                "redis" to RedisConfig::class,
            ),
        ),
        mapper = get(),
    )
}.bind<ConfigSource>()
```

Any config framework or library will probably return the keys in form of either
properties, json-like maps, or injected objects.

<a name="optional-injected-objects"></a>
### Optional injected objects

Poket will use these objects if they are bound in the dependency injection engine.
If they are not, it will just use some working defaults:

* `MeterRegistry`, for metrics. By default, it does not generate metrics.
* `PoketAsyncRunner`: interface to create background processes. By default it uses
a coroutine with some reserved threads (see `DefaultPoketAsyncRunner`).
* `kotlinx.datetime.Clock`: to get current time. By default it uses system clock.

<a name="object-cache"></a>
### Object cache

The normal usage to create a cache will be injecting the factory and creating the specific
cache we need from it:

```kotlin
class BookRepository @Inject constructor(
    //(...)
    cacheFactory: ObjectCacheFactory
) {
    private val bookCache: ObjectCache<Long, Book> = cacheFactory.createCache("book", serializationVersion = "1")

    suspend fun getBook(bookId: Long): Book =
        bookCache.getOrPut(bookId) {
            dbdao.getBook(bookId)
        }
}
```

#### Operations
Some available operations in the cache are:
* `getOrPut`: gets and returns the element from the cache. If it is not there, it generates
  the value using the given function and sets it in the cache.
* `getOrPutBlock`: Gets a list of keys from the cache. For the keys that are not
  in the cache, the generator function will be called, using the list of missed keys.
* `get`, `put`, `invalidate`: Simple cache operations

#### On cache keys
The library is guaranteed to allow strings, primitive types, collection of primitives
or simple data classes, but they should include only the bare minimum information
identifying the element key. In case of doubt, just use your own generated String.

#### Configuration
There are some settings than can be configured either at a global level or
per any specific cache (all settings are available at both levels).

```yaml
cache:
  default:
    # Default implementation for caches in the service
    cacheSystem: redis
    
    # Staging environment using different keys to prod
    distributedComponentNamespace: staging

  type:
    book:
      # TTL for elements in this cache ("book") Default: 300 (= 5 minutes)
      ttlInSeconds: 3600
      
      # Cache implementation for this specific cache. Default: memory (In general should always be overridden with a proper default provider)
      cacheSystem: memory-perpetual
      
      # Cache elements version. Default: 1
      version: 2

      # Deactivate request collapsing for this cache. Default: true
      requestCollapsing: false

    another_cache:
      ttlInSeconds: xxx
```

#### Testing
When you need to unit test a class using a cache, the easiest way to do it is using
`MapCacheSystem` implementation, which uses an in-memory Map.

```kotlin
val simpleCacheSystem = MapCacheSystem()

val simpleCacheFactory = ObjectCacheFactory(
    distributedMutexFactoryStub(),
    CacheSystemProvider(CacheMetrics(CompositeMeterRegistry()), simpleCacheSystem),
    ConstantConfigProvider(CacheConfig(default = CacheTypeConfig(cacheSystem = simpleCacheSystem.getId()))),
    CacheMetrics(CompositeMeterRegistry())
)

// (...)

// (specific test)
    // (given)
    Given(simpleCacheSystem) {
        contains("<namespace>", "<key>", "<value>")
    }
```

#### Operational parameters tuning / monitoring
The library generates automatically some metrics which allow monitoring the efficiency
of the cache. All metrics include a tag `type` with the type of the cache element
(a string passed to the cache when creating it to identify it), and a tag `cacheSystem`
with the identifier of the cache implementation.

* `cache.get`: timer measuring the cache access time for single get operations.
  Includes a tag `result` with the value `hit` when the element is found in the cache
  and `miss` when it isn't.
* `cache.put`: timer measuring the cache access time for single put/set operations.
* `cache.invalidate`: timer measuring the cache access time for single invalidate operations.
* `cache.generate`: timer measuring the time to generate a missed value in getOrPut operations.
* `cache.blockGet`: timer measuring the cache access time for multi get operations.
  Includes a tag `blockSize` with the number of elements tried to retrieve.
* `cache.blockGetSize`: "timer" (an histogram, in essence) with the number of elements queried in a block get.
* `cache.blockGenerate`: timer measuring the time to generate several missed values in a multi get operation.
  Includes a tag `blockSize` with the number of elements generated.
* `cache.blockGenerateSize`: "timer" (an histogram, in essence) with the number of elements queried in a block generate.

These metrics allow us to verify the efficiency of the cache and detect if the cache is
useful or not and tune the TTL of the cache elements in config.

#### Transaction aware
The caches are compatible with the generic [transaction system](transactions.md) in
the library. So if we run operations inside a transaction, every set or invalidate
operations will be executed only after the transaction has been committed in the main
storage.

#### Request collapsing
When the generation of the data to be cached is a costly operation, it is very useful
having a mechanism of request collapsing, so in case of different concurrent requests,
only the first one requests the data and provides it to the others, which are hold
waiting for it. Request collapsing is activated by default and can be disabled by config
globally or for a specific cache.

#### Versioning of cache elements format
A quite common operation that we will need for our distributed caches will be version
incrementing. This is needed when we create a new version of the service where the
format of the cached elements is not compatible with the data being read, so we need
to avoid reading the existing values when the service pod version is updated, while
the other pods with older versions keep using the existing cache elements. Changing
the `serializationVersion` parameter when creating the cache in code, different values
will be read and written in the cache.

```kotlin
private val bookCache: ObjectCache<Long, Book> = cacheFactory.createCache(
    "book",
    serializationVersion = "2" // New version
)
```

Another use case for cache versions is when the service has some bug and the cached
elements are not correct. For that case we have defined a config key, `version`, which
can be set in k8s configuration for a faster change.

Both versions will be appended to the cache namespace, which will normally be used as key
in the underlying cache system.

#### Asynchronous update of values in the cache
In addition to the TTL of any element in the cache, you can optionally assign an
outdate time to any element type (`outdateTimeInSeconds`). When a value is read from the cache, if it was
generated before the defined outdate time, then the value is considered outdated.
In that case, it will be returned and updated asynchronously so that following calls can get
the new value while the generation time does not affect current request.

This feature only works when using individual `getOrPut` operation, `getOrPutBlock` does not
provide it.

#### Independent caches in the same cluster
Some times we will have a distributed cache cluster shared by different services or
different environments and we will want to have independent cache values for them.
A typical example for this will be the staging environment, where we might possibly
want to have cached values independent of the production environment.

In this case, a `distributedComponentNamespace` config key can be defined with different
values in each namespace.

#### Rolling factor for load testing
When we are load testing a service, sometimes we might want to replicate a load of N users
but using a reduced number of users. The problem with that is usually that if we are using
caches we are going to have many more cache hits, so the load will not be equivalent.
This library allows setting by config a "rolling factor" for any cache, so that we can reduce its
efficiency.

If we set the `loadTestRollingFactor` > 1, the cache will have an expected hit ratio of
`1 / loadTestRollingFactor`
over normal usage, so it would behave in a way similar to using a number of users
multiplied by the rolling factor.

This is intended only for load testing and should not be used when the feature is live.

#### Ignore errors
By default, caches are going to ignore "get" and "put" errors in the underlying cache system,
but will fail when there are errors in invalidations. If there is an error in a
"put", the value might need to be invalidated, so it will fail in that case.

This behaviour can be changed with three independent boolean config keys:
`failOnGetError`, `failOnPutError`, `failOnInvalidateError`.


<a name="distributed-mutex"></a>
### Distributed mutex

Use case: we need some piece of code never to be run concurrently, in a single service
instance or also in different instances, for some object key. For example, to asynchronously
refresh the recommendations queue of a given user. You want to do that for different users at
the same time, but you don't want to run different generations for the same user at the same time.

Some times you can use a mechanism of database optimistic locks instead of a mutex
to achieve this, but that is not always the best solution. For example, if we want
to do some heavy processing or access an external service.

The normal usage to create a mutex will be injecting the factory and creating the specific
mutex we need from it:

```kotlin
class BookUserQueueGenerator @Inject constructor(
    //(...)
    mutexFactory: DistributedMutexFactory
) {
    private val queueRefreshMutex = mutexFactory.createMutex("recommendations-refresh")
    
    suspend fun createUserQueue(user: User) {
        queueRefreshMutex.synchronized(user.userId) {
            // (...) Mutual exclusive code, not run concurrently for the same user
        }
    }
}
```

`synchronized` method will wait until the other task has finished. We also have the method
`maybeSynchronized` that does not wait, gets the lock if no other process has got it and
provides a boolean variable to know if the lock was got or not.

```kotlin
suspend fun createUserQueue(user: User) {
    queueRefreshMutex.maybeSynchronized(user.userId) { gotLock ->
        if (gotLock) {
            // We have the lock, run the mutex code
        } else {
            // We don't have the lock, run anything you want for this case
        }
    }
}
```

The other method provided is `ifSynchronized` that only runs the code inside if the
lock can be acquired.

#### Configuration
There are some settings than can be configured either at a global level or
per any specific mutex.

```yaml
mutex:
  default:  # Default values applied to all mutex 
    lockSystem: syncdb
    
  type:  # Settings for specific mutex
    recommendations-refresh:
      timeoutInMillis: 10000 # After this time the wait will time out and an error will be thrown
      ttlInMillis: 20000 # After this time since got the lock, it will expire and other waiting coroutine will be able to get it
```

Some mutex can also have a hierarchical configuration, so that the final configuration
is obtained mixing all of them. This allows having some common configuration for
a group of generic mutex. The separator that needs to be used for this is "::":

```yaml
mutex:
  default:  # Default values applied to all mutex 
    timeoutInMillis: 10000
  type:  # Settings for specific mutex
    cache:
      timeoutInMillis: 20000 # This group of mutex will have a longer timeout
      ttlInMillis: 10000
    "cache::x":
      timeoutInMillis: 30000 # This especific type will have an even longer timeout
          # ttl for this time will be the one defined in "cache" group
```

#### Ignoring errors
If we want to ignore any error in a mutex because we know it is not critical,
the easiest way is using `forceIgnoreLockErrors` = `true` when creating the mutex.

```kotlin
val mutex = distributedMutexFactory.createMutex("my-mutex-type", forceIgnoreLockErrors = true)
```

Even without that, any mutex can be configured so that errors in the underlying lock system are ignored.
In the config there are two keys to control this, `onLockSystemError`:

* `FAIL`: This is the default, the exception will be thrown to the caller.
* `GET`: Continue as if the lock was obtained.
* `FALLBACK`: Get the lock with the fallback system defined in `fallbackLockSystem` (if any)

Example of configuration with fallback:
```yaml
mutex:
  default:
    lockSystem: redis
    onLockSystemError: FALLBACK
    fallbackLockSystem: syncdb
```

When releasing the lock by default an error will be ignored, but it can be configured
to fail using `failOnLockReleaseError` = `true`

#### Testing
When you need to unit test a class using a mutex, the easiest way to do it is using
`LockSystemStub` implementation. This class by default allows the caller to get the lock,
and it has some methods to stops the caller from getting the lock.

```kotlin
val lockSystem = lockSystemStub() // Function to rapidly get an instance
val mutexFactory = distributedMutexFactoryStub(lockSystem)

// (...)

// (specific test)
    // (given)
    lockSystem.willNotGetLock() // The lock will not be acquired when tried
```


<a name="generic-transactions"></a>
### Generic transactions

The library uses a generic system of "pluggable" transaction handlers, intending to
have one per any "transaction-aware" storage system used.

The library allows having transactions for more than one system, but it must
be highlighted that it shouldn't be used for more than one "main" storage system
in the same transaction, as transaction reliability could not be guaranteed in case that
one of the main systems failed when committing.

A common practical pattern for this multi-storage approach is using a database plus
a cache system, invalidating effectively the cache keys once the data has really been
committed, and hence avoiding race conditions.

In order to have new transaction aware backends, you just need to import the bindings
of the Poket library in your dependency injection system.

#### Running code inside a transaction
To run code inside a transaction we just need to use a `transactional` block.
When a transactional block is defined running inside a transaction, it will use the same
running transaction.

```kotlin
transactional {
    // Transactional code
}

transactional {
    // Transactional code
    
    transactional {
        // Same transaction
    }
}
```

There are two versions of the `transactional` function: one for coroutines (suspendable code)
and other for blocking code (not suspendable).

#### Rolling back
By default the transaction will be rolled back on any exception in the code inside. But a
transaction can be configured to be rolled back only when some types of exceptions are caught,
using rollbackOn / dontRollbackOn parameters:

```kotlin
transactional(rollbackOn = setOf(RuntimeException::class.java)) {
    // Transactional code
    // Will be rolled back on any runtime exception, but committed on any other exception
}

transactional(dontRollbackOn = setOf(IOException::class.java)) {
    // Transactional code
    // Will be rolled back on IO exception, but committed on any other exception
}
```

#### Transaction isolation level
An isolation level can also be specified for the transaction, following
standard [JDBC transaction levels definition](https://docs.oracle.com/cd/E19226-01/820-7695/beamv/index.html).
If a transaction is nested inside other one, only the outmost transaction level
will be used.

```kotlin
transactional(isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED) {
    // Transactional code, using the declared level
}
```

#### Coroutine transaction preserving
This code is intended to be used in coroutines code. To achieve that, the coroutine context
must include ```transactionCoroutineContext()```. This way, when changing the thread of a coroutine
the transaction status will be preserved.

```kotlin
CoroutineScope(transactionCoroutineContext()) {
    transactional {
        // transactional code
        delay(200)
        // different thread, same transaction
        
        withContext(Dispatchers.IO) {
            // same transaction
        }
    }
}
```

Some practical examples can be found in ```TransactionManagerTest```.

#### New coroutines
If we launch some asynchronous code in a new coroutine, by default it will inherit the same
coroutine context of the parent, so the code will be run in the same transaction. That means
that, depending on the handler implementation, more than one more thread could be using the same
connection to the storage system. In case of JDBC, for example, it works but is not very performant
because in case of concurrent calls the second call will be waiting until the first ends. So we
need to be careful and think if we need the same transaction in the new coroutine or not.

In case that we want to launch the coroutine using a new transaction context (which would start
without a transaction), we can use ```newTransactionCoroutineContext()```.

```kotlin
async(newTransactionCoroutineContext()) {
    // This code will not be inside the transaction context
    
    transactional {
        // new transaction
    }
}
```

#### New coroutine scopes
If we launch a coroutine in a new coroutine scope, the transaction will not be used.
It could be used if we create the new CoroutineScope using transactionCoroutineContext,
but it should not be necessary in a normal usage, as logically new scopes should not share
transactions.

#### Non-coroutines code
The transaction mechanism can also be used for non-coroutines code. The transaction will
be preserved as long as the execution remains in the same thread where it started.
We should make sure that we use the `transactional` function in `transaction.blocking`
package.

```kotlin
transactional {
    // Transactional code

    transactional {
        // Same transaction
    }
}
```

#### Saga pattern in a transaction
We might need to run an operation in a transaction that cannot be done
transactionally (for example a call to an external service),
but have a compensaction action which can undo it. In that case, we can use `sagaOperation`
function and the reverse action will be performed automatically in case of
transaction rollback:

```kotlin
val chat = sagaOperation(
    undo = { deleteBook(it) }
) {
    createBook()
}
```

This piece of code will create a book calling the corresponding external service,
and if there is a transaction rollback, the call to delete the book will also be
run.

#### Actions after transaction commit
We can also need to run a piece of code after transaction commit. As transactions
are nested, you might need to run a block of code from an inner class or function
after the transaction gets committed. That is assuming you are inside a transactional
block, otherwise it will be invoked immediately.

For that, you can use `runAfterTransactionCommit`, which will handle a chain of
post commit actions:

```kotlin
runAfterTransactionCommit {
    trackActionEffectivelyPerformed()
}
```

The action will be run out of the transaction, and if it fails the exception
will be thrown, but the transaction will be effectively committed.
