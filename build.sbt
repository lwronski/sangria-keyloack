name := "sangria-akka-http-example"
version := "0.1.0-SNAPSHOT"

description := "An example GraphQL server written with akka-http, circe and sangria."

scalaVersion := "2.12.6"
scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-slowlog" % "0.1.8",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",

  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",

  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",

  "org.keycloak"      % "keycloak-core"         % "4.0.0.Final",
  "org.jboss.logging" % "jboss-logging"         % "3.3.0.Final",
  "org.apache.httpcomponents" % "httpclient" % "4.5.1",
  "net.liftweb" %% "lift-json" % "3.3.0",
  "com.pauldijou" %% "jwt-core" % "0.18.0",
  "io.spray" %%  "spray-json" % "1.3.5",

)

Revolver.settings
enablePlugins(JavaAppPackaging)
