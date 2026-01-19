Create a admin page using spring & java

Should be able to login via single-sign-on or other strategy where admin can access below listed sites, without login/password. Just By clicking on website icons, admin will be redirected to the site. 

List of websites to have:
Business Section:
ERPNext
Umami
Ditto

Developer Section:
ParadeDB
DragonFlyDB
Grafana stack sites



1. Fix ERPNextService to use `findPendingOrders()`
2. Add pagination to SupportController (critical)
3. Fix remaining findAll() calls


4. Fix Redis serializer (deprecated)
5. Refactor class-level `@Transactional`
6. Add `@Transactional(readOnly = true)` for queries