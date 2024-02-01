Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(Seq(
  organization := "com.github.cornerman",

  crossScalaVersions := Seq("2.13.12", "3.3.0"),
  scalaVersion := crossScalaVersions.value.head,

  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),

  homepage := Some(url("https://github.com/cornerman/authn-scala")),

  scmInfo := Some(ScmInfo(
    url("https://github.com/cornerman/authn-scala"),
    "scm:git:git@github.com:cornerman/authn-scala.git",
    Some("scm:git:git@github.com:cornerman/authn-scala.git"))
  ),

  pomExtra :=
    <developers>
      <developer>
        <id>jkaroff</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
      </developer>
    </developers>
))

lazy val commonSettings = Seq(
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => Seq.empty
    case _ => Seq(compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full))
  }),
)

lazy val backend = project
  .settings(commonSettings)
  .settings(
    name := "keratin-authn-backend",
    libraryDependencies ++= Seq(
      "org.typelevel"                         %% "cats-effect"             % "3.5.3",
      "org.http4s"                            %% "http4s-core"             % "0.23.24",
      "org.http4s"                            %% "http4s-client"           % "0.23.24",
      "com.github.cornerman"                  %% "http4s-jsoniter"         % "0.1.1",
      "com.auth0"                              % "java-jwt"                % "4.4.0",
      "com.auth0"                              % "jwks-rsa"                % "0.22.1",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"     % "2.28.0",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"   % "2.28.0",
    ),
  )

lazy val frontend = project
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterGenSourcePlugin)
  .settings(commonSettings)
  .settings(
    name := "keratin-authn-backend",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.5.3",
    ),

    // scalablytyped
    useYarn := true,
    stOutputPackage := "authn.frontend.authnJS",
    Compile / npmDependencies ++= Seq(
      "keratin-authn" -> "^1.4.1",
    ),
    stMinimize := Selection.AllExcept("keratin-authn"),

    scalacOptions += (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => "-Wconf:msg=unused import:s" //TODO: src filter does not work in scala3?
      case _ => "-Wconf:src=src_managed/.*:s", // silence warnings for generated sources
    }),
  )
