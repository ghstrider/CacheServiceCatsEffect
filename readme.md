## Cache Service

A Scala-based cache management service built with Cats Effect and functional programming principles. This service provides a flexible key-value store abstraction with support for multiple storage backends and a layered caching strategy.

### Overview

This project implements a cache management system that:
- Provides a unified interface for different cache storage backends
- Supports fallback mechanisms between cache layers
- Offers a CLI for basic cache operations (GET, PUT, DELETE)
- Uses functional programming patterns for safe, composable IO operations

### Architecture

The service is built around these core components:

- **KeyValueStore Trait**: Abstract interface defining cache operations
- **CacheServiceImpl**: Main service implementation managing cache resources
- **Storage Backends**:
  - **Redis Store**: Distributed caching using redis4cats
  - **File Store**: Local persistence using fs2 file operations
- **CLI Interface**: Command-line tool for interacting with the cache

### Supported Cache Stores

- **Redis Store**: For distributed, in-memory caching with high performance
- **File Store**: For persistent local storage as a fallback option

### Technologies Used

- **Scala 2.13.7**: Core programming language
- **Cats Effect**: For functional IO and resource management
- **fs2**: For streaming and file operations
- **redis4cats**: Redis client for Cats Effect
- **decline**: For building the CLI interface
- **ScalaTest**: Testing framework

### Setup

1. **Install Prerequisites**:
   - JDK 8 or higher
   - SBT (Scala Build Tool)
   - Redis server (if using Redis backend)

2. **Start Redis** (if using Redis backend):
   ```bash
   redis-server
   ```

3. **Create File Store** (if using file backend):
   - The service expects a file named `filestore.kv` in the working directory
   - This file will be created automatically if it doesn't exist

4. **Build the Project**:
   ```bash
   sbt compile
   ```

5. **Run the CLI**:
   ```bash
   sbt run
   ```
   Or build and run the JAR:
   ```bash
   sbt assembly
   java -jar target/scala-2.13/keystore.jar
   ```

### CLI Usage

The CLI supports three main operations:

```bash
# Get a value by key
kvstore get <key>

# Store a key-value pair
kvstore put <key> <value>

# Delete a key
kvstore del <key>
```

### Project Structure

```
src/
├── main/scala/com/arya/
│   ├── cache/
│   │   └── CacheServiceImpl.scala    # Main cache service implementation
│   ├── cli/
│   │   ├── Cli.scala                 # CLI application entry point
│   │   └── CliArgs.scala             # CLI argument definitions
│   ├── dsl/
│   │   ├── DataSource.scala          # Data source abstractions
│   │   ├── KVStore.scala             # Key-value store traits
│   │   └── KeyValueStore.scala       # Core store interface
│   ├── filestore/
│   │   ├── FileKVStore.scala         # File-based storage implementation
│   │   ├── Fs2File.scala             # fs2 file operations
│   │   └── Fs2FileKeyValueStore.scala # fs2-based file store
│   └── redisstore/
│       └── RedisStore.scala          # Redis storage implementation
└── test/scala/com/arya/
    └── cli/
        └── CliArgsSpec.scala         # CLI argument tests
```

### Building from Source

```bash
# Run tests
sbt test

# Create executable JAR
sbt assembly

# Run with SBT
sbt "run get mykey"
sbt "run put mykey myvalue"
sbt "run del mykey"
```
