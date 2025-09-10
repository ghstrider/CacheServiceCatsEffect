## Cache Service

A Scala-based cache management service built with Cats Effect and functional programming principles. This service provides a flexible key-value store abstraction with support for multiple storage backends and a layered caching strategy.

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd CacheServiceCatsEffect

# Build the CLI JAR
sbt cli/assembly

# Use the cache service
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key hello --value world
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key hello
# Output: world

# Use with TTL (expires after 60 seconds)
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key temp --value "expires soon" --ttl 60
java -jar cli/target/scala-2.13/cache-service-cli.jar ttl --key temp
# Output: TTL for key 'temp': 55 seconds
```

### Overview

This project implements a cache management system that:
- Provides a unified interface for different cache storage backends
- Supports fallback mechanisms between cache layers
- Offers a CLI for cache operations (GET, PUT, DELETE, TTL)
- **TTL (Time-To-Live) Support**: Set expiration times for cache entries with automatic cleanup
- Uses functional programming patterns for safe, composable IO operations

### Architecture

The project is organized as a multi-module SBT build with the following structure:

#### Core Module (`core/`)
Contains the main cache service implementation:
- **KeyValueStore Trait**: Abstract interface defining cache operations (get, put, putWithTTL, delete, ttl)
- **CacheServiceImpl**: Main service implementation managing cache resources
- **Storage Backends**:
  - **Redis Store**: Distributed caching using redis4cats with TTL support
  - **File Store**: Local persistence using fs2 file operations with TTL support
  - **Fs2FileKeyValueStoreTTL**: TTL-aware file store with automatic cleanup

#### CLI Module (`cli/`)
Command-line interface that depends on the core module:
- **CLI Interface**: Command-line tool for interacting with the cache
- **Argument Parsing**: Using decline for type-safe command parsing

#### WebAPI Module (`webapi/`)
HTTP REST API server that depends on the core module:
- **HTTP Routes**: RESTful endpoints for cache operations
- **JSON API**: Request/response models with automatic JSON serialization
- **Web Server**: Http4s-based server with CORS support

### Supported Cache Stores

- **Redis Store**: For distributed, in-memory caching with high performance
- **File Store**: For persistent local storage as a fallback option

### Technologies Used

- **Scala 2.13.7**: Core programming language
- **Cats Effect**: For functional IO and resource management
- **fs2**: For streaming and file operations
- **redis4cats**: Redis client for Cats Effect
- **decline**: For building the CLI interface
- **Http4s**: HTTP server and client library
- **Circe**: JSON library for automatic serialization/deserialization
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

The CLI supports the following operations:

```bash
# Get a value by key
java -jar cache-service-cli.jar get --key <key>

# Store a key-value pair
java -jar cache-service-cli.jar put --key <key> --value <value>

# Store a key-value pair with TTL (Time-To-Live in seconds)
java -jar cache-service-cli.jar put --key <key> --value <value> --ttl <seconds>

# Delete a key
java -jar cache-service-cli.jar del --key <key>

# Check remaining TTL for a key
java -jar cache-service-cli.jar ttl --key <key>
```

### HTTP API Usage

The web API provides RESTful endpoints for cache operations:

#### Start the Web Server
```bash
# Build and start the web API server
sbt webapi/assembly
java -jar webapi/target/scala-2.13/cache-service-webapi.jar

# Server starts on http://localhost:8080
```

#### API Endpoints
```bash
# Health check
GET /api/health

# Get value by key
GET /api/cache/{key}

# Store value (without TTL)
PUT /api/cache/{key}
Content-Type: application/json
{"value": "your-value"}

# Store value with TTL
PUT /api/cache/{key}
Content-Type: application/json
{"value": "your-value", "ttl": 3600}

# Delete key
DELETE /api/cache/{key}

# Get remaining TTL for key
GET /api/cache/{key}/ttl
```

#### Example API Usage
```bash
# Store a value
curl -X PUT "http://localhost:8080/api/cache/user:123" \
  -H "Content-Type: application/json" \
  -d '{"value": "John Doe"}'

# Store with TTL (expires in 1 hour)
curl -X PUT "http://localhost:8080/api/cache/session:abc" \
  -H "Content-Type: application/json" \
  -d '{"value": "active", "ttl": 3600}'

# Get value
curl -X GET "http://localhost:8080/api/cache/user:123"
# Response: {"value": "John Doe"}

# Check TTL
curl -X GET "http://localhost:8080/api/cache/session:abc/ttl"
# Response: {"ttl": 3542, "message": "TTL for key 'session:abc': 3542 seconds"}

# Delete key
curl -X DELETE "http://localhost:8080/api/cache/user:123"
# Response: {"message": "Successfully deleted key 'user:123'", "key": "user:123"}
```

### Project Structure

```
├── core/                             # Core cache service module
│   └── src/
│       ├── main/scala/com/arya/
│       │   ├── cache/
│       │   │   └── CacheServiceImpl.scala    # Main cache service implementation
│       │   ├── dsl/
│       │   │   └── KeyValueStore.scala       # Core store interface with TTL support
│       │   ├── filestore/
│       │   │   ├── FileKVStore.scala         # File-based storage implementation
│       │   │   ├── Fs2File.scala             # fs2 file operations
│       │   │   ├── Fs2FileKeyValueStore.scala # Standard fs2-based file store
│       │   │   ├── Fs2FileKeyValueStoreTTL.scala # TTL-aware file store
│       │   │   └── implicit/
│       │   │       └── package.scala         # Implicit instances
│       │   └── redisstore/
│       │       └── RedisStore.scala          # Redis storage with TTL support
│       └── test/scala/com/arya/
│           ├── cache/
│           │   └── CacheServiceImplSpec.scala # Cache service tests
│           ├── filestore/
│           │   ├── FileKVStoreSpec.scala      # File store tests
│           │   └── Fs2FileKeyValueStoreTTLSpec.scala # TTL functionality tests
│           ├── integration/
│           │   └── CacheLayersIntegrationSpec.scala # Integration tests
│           └── redisstore/
│               └── RedisStoreSpec.scala      # Redis store tests
├── cli/                              # CLI module (depends on core)
│   └── src/
│       ├── main/scala/com/arya/cli/
│       │   ├── Cli.scala             # CLI application entry point
│       │   └── CliArgs.scala         # CLI argument definitions with TTL support
│       └── test/scala/com/arya/cli/
│           └── CliArgsSpec.scala     # CLI argument tests
└── webapi/                           # WebAPI module (depends on core)
    └── src/
        ├── main/scala/com/arya/webapi/
        │   ├── WebApiServer.scala    # HTTP server main class
        │   ├── model/
        │   │   └── ApiModels.scala   # JSON request/response models
        │   └── routes/
        │       └── CacheRoutes.scala # HTTP route definitions
        └── test/scala/com/arya/webapi/
            └── routes/
                └── CacheRoutesSpec.scala # HTTP API tests
```

### Building from Source

```bash
# Run tests
sbt test

# Run tests for specific module
sbt core/test
sbt cli/test
sbt webapi/test

# Compile all modules
sbt compile

# Run with SBT (requires specifying the main class)
sbt "cli/runMain com.arya.cli.Cli get --key mykey"
sbt "cli/runMain com.arya.cli.Cli put --key mykey --value myvalue"
sbt "cli/runMain com.arya.cli.Cli del --key mykey"

# Run web API server with SBT
sbt "webapi/runMain com.arya.webapi.WebApiServer"
```

### Building Standalone JARs

Both CLI and WebAPI modules can be packaged as standalone executable JAR files that include all dependencies.

```bash
# Build CLI JAR
sbt cli/assembly
# Creates: cli/target/scala-2.13/cache-service-cli.jar

# Build WebAPI JAR
sbt webapi/assembly
# Creates: webapi/target/scala-2.13/cache-service-webapi.jar

# Build both JARs
sbt assembly
```

### Using the Standalone JARs

Once built, the JARs can be executed directly with Java:

#### CLI JAR Usage

#### Store a value (PUT operation)
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key mykey --value "my value"
# Output: Successfully stored key 'mykey'
```

#### Store a value with TTL (expires after 60 seconds)
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key mykey --value "my value" --ttl 60
# Output: Successfully stored key 'mykey' with TTL of 60 seconds
```

#### Retrieve a value (GET operation)
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key mykey
# Output: my value
```

#### Check remaining TTL for a key
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar ttl --key mykey
# Output: TTL for key 'mykey': 45 seconds
# (Or: Key 'mykey' has no TTL set or does not exist)
```

#### Delete a key (DELETE operation)
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar del --key mykey
# Output: Successfully deleted key 'mykey'
```

#### Get non-existent key
```bash
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key nonexistent
# Output: Key 'nonexistent' not found
# Exit code: 1 (error)
```

#### WebAPI JAR Usage
```bash
# Start the web server
java -jar webapi/target/scala-2.13/cache-service-webapi.jar
# Server starts on http://localhost:8080

# Use the API as shown in the HTTP API Usage section above
```

### TTL (Time-To-Live) Features

The cache service supports comprehensive TTL functionality:

#### Setting TTL
- **With TTL**: Use `--ttl <seconds>` when storing data
- **Without TTL**: Regular `put` operations store data permanently
- **Update TTL**: Overwrite existing keys with new TTL values

#### Automatic Cleanup
- **On Access**: Expired entries are automatically removed when accessed via `get` operations
- **Transparent**: No manual cleanup required
- **Consistent**: Works across application restarts

#### TTL Storage Backends
- **File Store**: TTL data persisted in `filestore.kv.ttl` file
- **Redis Store**: Uses Redis native TTL functionality for optimal performance

### Data Persistence

The cache service uses file-based storage by default, storing data in `filestore.kv` in the current working directory. TTL data is stored in a separate `filestore.kv.ttl` file. This means:
- Data persists between JAR executions
- Multiple JAR invocations can share the same data
- Both data and TTL files are created automatically if they don't exist
- **Automatic Cleanup**: Expired entries are automatically removed when accessed

### Examples

#### Complete workflow example with TTL
```bash
# Build the JAR
sbt cli/assembly

# Store some data with different TTL settings
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key user:1 --value "John Doe"
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key session:abc123 --value "active" --ttl 3600
java -jar cli/target/scala-2.13/cache-service-cli.jar put --key temp:data --value "expires soon" --ttl 30

# Retrieve the data
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key user:1
# Output: John Doe

java -jar cli/target/scala-2.13/cache-service-cli.jar get --key session:abc123
# Output: active

# Check TTL for different keys
java -jar cli/target/scala-2.13/cache-service-cli.jar ttl --key user:1
# Output: Key 'user:1' has no TTL set or does not exist

java -jar cli/target/scala-2.13/cache-service-cli.jar ttl --key session:abc123
# Output: TTL for key 'session:abc123': 3545 seconds

java -jar cli/target/scala-2.13/cache-service-cli.jar ttl --key temp:data
# Output: TTL for key 'temp:data': 25 seconds

# Wait for expiration and try to access expired key
# (After 30 seconds)
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key temp:data
# Output: Key 'temp:data' not found (automatically cleaned up)

# Delete a key
java -jar cli/target/scala-2.13/cache-service-cli.jar del --key user:1

# Verify deletion
java -jar cli/target/scala-2.13/cache-service-cli.jar get --key user:1
# Output: Key 'user:1' not found
```

#### Using the JAR from any directory
```bash
# Copy the JAR to a convenient location
cp cli/target/scala-2.13/cache-service-cli.jar ~/bin/kvstore.jar

# Use it from anywhere
cd /any/directory
java -jar ~/bin/kvstore.jar put --key config --value "production"
java -jar ~/bin/kvstore.jar get --key config
```
