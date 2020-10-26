package io.surfkit.influx

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.influxdb.client.scala.InfluxDBClientScalaFactory
import com.influxdb.query.FluxRecord

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

object InfluxDB2ScalaExample extends App {

  implicit val system: ActorSystem = ActorSystem("it-tests")
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  // You can generate a Token from the "Tokens Tab" in the UI
  val token = Source.fromFile("/Users/coreyauger/.influx.token").getLines.mkString
  val org = "coreyauger@gmail.com"
  val bucket = "coreyauger's Bucket"

  val client = InfluxDBClientScalaFactory.create("https://us-west-2-1.aws.cloud2.influxdata.com", token.toCharArray, org)

  val query = (s"""from(bucket: "$bucket")"""
    + " |> range(start: -1d)"
    + " |> filter(fn: (r) => (r[\"_measurement\"] == \"cpu\" and r[\"_field\"] == \"usage_system\"))")

  println(s"running query ${query}")

  // Result is returned as a stream
  val results = client.getQueryScalaApi().query(query)

  // Example of additional result stream processing on client side
  val sink = results
    // filter on client side using \`filter\` built-in operator
    .filter(it => "cpu0" == it.getValueByKey("cpu"))
    // take first 20 records
    .take(20)
    // print results
    .runWith(Sink.foreach[FluxRecord](it => println(s"Measurement: ${it.getMeasurement}, value: ${it.getValue}")
    ))

  // wait to finish
  Await.result(sink, Duration.Inf)

  client.close()
  system.terminate()


}