package com.gu.supportermetricsjob

import java.sql.{ Connection, ResultSet }

import com.gu.supportermetricsjob.Churn.{ ChurnRequest, ChurnResults }
import org.joda.time._

import scala.util.Try

object PrestoQuery {

  def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()
      def next() = f(resultSet)
    }
  }

  def baseQueryString(statusDate: LocalDate, date: LocalDate) = {
    val statusDateString = s"date '${statusDate.toString("yyyy-MM-dd")}'"
    val dateString = s"date '${date.toString("yyyy-MM-dd")}'"
    s"""
         select count(1) as base
         from subscriptions
         where status_date = $statusDateString
         and end_date is null or end_date > $dateString
         and signup_date <= $dateString
         and product_name = 'Supporter'
      """
  }

  def churnQueryString(statusDate: LocalDate, endDate: LocalDate, startDate: LocalDate) = {
    val statusDateString = s"date '${statusDate.toString("yyyy-MM-dd")}'"
    val endDateString = s"date '${endDate.toString("yyyy-MM-dd")}'"
    val startDateString = s"date '${startDate.toString("yyyy-MM-dd")}'"
    s"""
         select count(1) as churn
       from subscriptions
       where status_date = $statusDateString
       and end_date < $endDateString and (end_date > $startDateString or cancelled_at > $startDateString)
       and product_name = 'Supporter'
      """
  }

  def churnQueries(request: ChurnRequest)(implicit conn: Connection): Try[ChurnResults] = Try {
    val stmt = conn.createStatement

    val startDate = request.endDate.minus(request.period)
    val baseStart = results(stmt.executeQuery(baseQueryString(request.statusDate, startDate)))(x => x.getInt("base")).toList.head // todo nice error if !=1 item
    val baseEnd = results(stmt.executeQuery(baseQueryString(request.statusDate, request.endDate)))(x => x.getInt("base")).toList.head // todo nice error if !=1 item
    val absoluteChurn = results(stmt.executeQuery(churnQueryString(request.statusDate, request.endDate, startDate)))(x => x.getInt("churn")).toList.head // todo nice error if !=1 item

    val periodsInYear: Double = {
      val daysInPeriod = Days.daysBetween(startDate, request.endDate).getDays
      val oneYearAgo = request.endDate.minus(Years.ONE)
      val daysInYear = Days.daysBetween(oneYearAgo, request.endDate).getDays
      daysInYear.toDouble / daysInPeriod
    }
    ChurnResults(baseStart, baseEnd, absoluteChurn, periodsInYear)
  }

}
