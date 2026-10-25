---
layout: philosophy.html
title: Authorization
description: Authorization models and context functions in Sharaf
---

# {{ page.title }}

Authentication establishes identity. Authorization decides whether an actor may perform an operation on a resource.
An authenticated route can return shared content without looking up an application user. Identity becomes relevant
when a query or policy needs to distinguish callers.

## Broad roles and resource-specific decisions

Role-based access control (RBAC) is useful for broad requirements such as administrator or support access.
A role set supports multiple roles simultaneously, but a global role does not describe which course, tenant, or document
an actor may access.

Attribute-based access control (ABAC) evaluates facts about the actor, resource, and possibly environment.
An editing policy might combine active account status, subscription state, ownership, and whether a document is archived.
Relationship-based access control (ReBAC) focuses on connections such as owning a document or teaching a specific course.
A policy can combine both approaches.

[OWASP recommends preferring ABAC and ReBAC over relying solely on RBAC](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html#prefer-attribute-and-relationship-based-access-control-over-rbac).
A role can still be one policy input. These patterns can be implemented with focused application functions; they do not
require a separate policy engine.

## Identity and relationships are different facts

Teacher, student, and customer are overlapping relationships or attributes. Modeling them as mutually exclusive user
categories loses valid combinations. A person may teach and study in the same course, study in a second course, and
independently hold a subscription.

Membership scoped to a resource expresses those facts without a hierarchy of increasingly specific user categories.
The resource identifier is part of the relationship: teacher of course A says nothing about editing course B.

## Context functions carry facts

A method with `(using CurrentUser)` declares that it requires an actor. A `CurrentUser ?=> A` helper can resolve that
actor once and make it available through a call chain. Explicit parameters provide the same access-control behavior.
The choice is one of clarity at the call sites.

Neither form establishes permission by itself. A context can be constructed by application code, and an authenticated
actor can still lack access to the target resource. The service's policy checks remain the runtime enforcement point.
There is no need to construct fake users to discover routes; authentication can decorate handlers before route execution.

## Enforcement belongs with the operation

Pac4j establishes authenticated profiles and supports broad role requirements at the HTTP boundary. Application code
can check profile roles inside selected routes, map profiles to domain identities when needed, and apply resource rules in service operations. This keeps the same rules
in force for HTML controllers, API routes, and other callers.

A policy predicate can also inform the UI, but client-side visibility cannot enforce access. Checks must remain tied
to the actual actor, resource, and write. For changing state, a transaction or a conditional database mutation preserves
that relationship between the checked facts and the update.

Runnable implementations and request commands are in the [authorization How-to guides](/howtos/authorization.html).
Deployment concerns are covered by the [production security guide](/howtos/security.html).
