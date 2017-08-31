package com.gu.supportermetricsjob

import java.sql.{ Connection, DriverManager }

import com.amazonaws.auth.{ AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.gu.supportermetricsjob.Churn.{ ChurnRequest, ChurnResults }
import org.joda.time._

import scala.util.{ Failure, Success, Try }

object SendChurnEmailsJob extends Logging {

  case class Config(app: String, stack: String, stage: String, emailTo: String, emailFrom: String, prestoUrl: String, emailLine: String)

  implicit val credentials: AWSCredentialsProviderChain = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("membership"),
    DefaultAWSCredentialsProviderChain.getInstance())

  implicit val ses: AmazonSimpleEmailService = Emailer.makeSes()

  def createConn(prestoUrl: String) = Try {
    Class.forName("com.facebook.presto.jdbc.PrestoDriver")
    DriverManager.getConnection(prestoUrl, "user", "***empty***")
  }

  def run(config: Config): String = {

    val result = for {
      conn <- createConn(config.prestoUrl)
      dateToRun = LocalDate.now().minusDays(1)
      request = ChurnRequest(dateToRun, dateToRun, Weeks.ONE, "Supporter")
      count <- PrestoQuery.churnQueries(request)(conn)
      _ = logger.info(s"count: $count")
      emailLines = buildEmailBody(config.emailLine, request, count)
      email = Emailer.Email("Automated Daily Churn Email", emailLines, config.emailFrom, config.emailTo)
      emailResult <- Emailer.sendEmail(email)
    } yield emailResult

    result match {
      case Failure(e) =>
        logger.error("failed to send email", e)
        "failed to send email"
      case Success(message) => s"successfully emailed: $message"
    }
  }

  def buildEmailBody(emailLine: String, request: ChurnRequest, count: ChurnResults): String = {
    Seq(
      "<h2>What is this email?</h2>",
      "This email is to evidence agreement of Churn metric in support of KR3.  It shows a minimum viable product for visibility, but should be extended " +
        "to support other use cases in the form of more emails, a dashboard, or anything else.",
      "The full churn spreadsheet is in Matt's possession, this email is intended as a summary for wider digestability, and to improve confidence that everyone really is using the same definition. " +
        "It comes from the data lake.",
      "<h2>What is 'Churn'?</h2>",
      "Churn is the percentage of total customers who stop using your service during a period.",
      "",
      "<strong>Churn / Average Base</strong>",
      "",
      s"$emailLine",

      "<h2>The figures!</h2>",
      s"${request.pretty}",
      "",
      s"${count.breakdown.mkString("<br>\n")}",
      "",
      s"<strong>${count.result}</strong>",
      "<h2>Link to the data dashboard</h2>",
      "See https://coming.soon.../todo",
      "this might give breakdown by geo/product, weekly figures?, voluntary cancellation figures etc",

      "<h2>Questions/suggestions?</h2>",
      "Let us know of course, and we will improve this.",
      "<h2>Data lake queries for those who want to know...</h2>",
      "See the code at https://github.com/guardian/supporter-metrics-job/blob/master/src/main/scala/com/gu/supportermetricsjob/PrestoQuery.scala"
    ).mkString("<br>\n")
  }
}

object Churn {

  case class ChurnRequest(statusDate: LocalDate, endDate: LocalDate, period: ReadablePeriod, product: String) {

    val periodString: String = period match {
      case Months.ONE => "one month"
      case Weeks.ONE => "one week"
      case x => x.toString
    }

    val pretty: String = s"product $product for $periodString before $endDate"

  }

  case class ChurnResults(baseStart: Int, baseEnd: Int, churnCount: Int, periodsInYear: Double) {

    val subscriptionChurnRate: Double =
      churnCount / ((baseStart + baseEnd) / 2.0)

    val annualChurnRate: Double =
      subscriptionChurnRate * periodsInYear

    val breakdown: Seq[String] = Seq(
      s"base at start: $baseStart",
      s"base at end: $baseEnd",
      s"churner count: $churnCount")

    val result =
      s"annualised churn rate = ${Math.round(annualChurnRate * 1000).toDouble / 10}%"

  }

}
