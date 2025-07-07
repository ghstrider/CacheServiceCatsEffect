## Cache Service
This project is Cache Management Service, capable of getting data from another cache service,
if not available in first cache store.

### Supported Cache Store
- Redis Store
- File Store

### Setup
1. Run a redis instance
2. Filename with name `filestore.kv`
3. Run `CacheServiceMain.scala`