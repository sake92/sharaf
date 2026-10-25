---
layout: howto.html
title: Authorize scoped relationships
description: Authorize scoped relationships in Sharaf
---

# {{ page.title }}

Use relationship-based checks when access depends on how the caller relates to a particular course, project, or tenant.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Model memberships as independent facts

Keep actor identity separate from memberships and subscription attributes. Store a membership's user, target course,
and set of course roles together. A person can be both teacher and student in one course, and only a student in another.
Do not model these combinations as mutually exclusive subclasses of the user.

## Check the target resource in the service

Look up membership using **both the current user's ID and the requested course ID**. Require that membership for reads.
For edits, require a teacher relationship to that course and require the course to be unarchived.
The example policy also checks that membership and course IDs agree and that the membership belongs to the actor.
`Courses.rename` calls `CoursePolicy.requireCanTeach(user, course, member)` before changing stored state. A denied
policy check throws `RejectedException`, mapped to 403 inside the pac4j boundary.

Resolve memberships from trusted storage. Do not accept a role, membership scope, or actor ID from the rename request.
If you pass membership through a context function, validate its scope against the actual resource at the enforcement point.
A context describing course A must never authorize a write to course B.

Return 404 for nonmembers and 403 for members who lack editing permission. Continue to enforce the policy inside the
service even if the UI hides the edit button.

## Run and check combined roles and scope

```sh
scala run examples/scala-cli/authorization/05_relationships.scala --server=false
```

```sh
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Revised A"}' http://localhost:8181/courses/a # 200: teacher AND student
curl -i -u bob:demo http://localhost:8181/courses/a # 200: student may read
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/courses/a # 403: student only here
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied","courseId":"a","roles":["Teacher"]}' http://localhost:8181/courses/b # 403
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Revised B"}' http://localhost:8181/courses/b # 200: teacher in B
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/courses/archived # 403: archived
curl -i -u outsider:demo http://localhost:8181/courses/a # 404: no membership
```

The policy combines a relationship (teacher of this course) with a resource attribute (not archived).
For the distinction between the access-control models, see [the explanation](/philosophy/authorization.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 05_relationships.scala 05_relationships.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/05_relationships.scala). The listing is synchronized from the tested source.

```scala
{% include "authorization-relationships.scala" %}
```
