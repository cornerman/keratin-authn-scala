# keratin-authn-scala

This is a client library for [keratin/authn-server](https://github.com/keratin/authn-server).

- Backend library (jvm-only): works similarily to the existing backend libraries like [authn-node](https://github.com/keratin/authn-node).
- Frontend library (js-only): wraps the existing [authn-js](https://github.com/keratin/authn-js).

## Get started

Backend:
```scala
libraryDependencies += "com.github.cornerman" %% "keratin-authn-backend" % "0.1.1"
```

Frontend:
```scala
libraryDependencies += "com.github.cornerman" %%% "keratin-authn-frontend" % "0.1.1"
// You need to bundle the npm package "keratin-authn"
```

We additonally publish snapshot releases for every commit.
