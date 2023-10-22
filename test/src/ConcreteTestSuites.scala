package usql
import operations.{
  ExprBooleanOpsTests,
  ExprNumericOpsTests,
  ExprSeqNumericOpsTests,
  ExprSeqOpsTests,
  ExprStringOpsTests
}
import query.{InsertTests, SelectTests, SubQueryTests, UpdateTests, ReturningTests}

package mysql {
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with MySqlSuite
  object ExprExprIntOpsTests extends ExprNumericOpsTests with MySqlSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with MySqlSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with MySqlSuite
  object ExprStringOpsTests extends ExprStringOpsTests with MySqlSuite
  object InsertTests extends InsertTests with MySqlSuite
  object SelectTests extends SelectTests with MySqlSuite
  object SubQueryTests extends SubQueryTests with MySqlSuite
  object UpdateTests extends UpdateTests with MySqlSuite
  // MySql does not support INSERT/UPDATE RETURNING
  // object ReturningTests extends ReturningTests with MySqlSuite
}

package postgres {
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with PostgresSuite
  object ExprExprIntOpsTests extends ExprNumericOpsTests with PostgresSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with PostgresSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with PostgresSuite
  object ExprStringOpsTests extends ExprStringOpsTests with PostgresSuite
  object InsertTests extends InsertTests with PostgresSuite
  object SelectTests extends SelectTests with PostgresSuite
  object SubQueryTests extends SubQueryTests with PostgresSuite
  object UpdateTests extends UpdateTests with PostgresSuite
  object ReturningTests extends ReturningTests with PostgresSuite
}

package sqlite {
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with SqliteSuite
  object ExprIntOpsTests extends ExprNumericOpsTests with SqliteSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with SqliteSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with SqliteSuite
  object ExprStringOpsTests extends ExprStringOpsTests with SqliteSuite
  object InsertTests extends InsertTests with SqliteSuite
  object SelectTests extends SelectTests with SqliteSuite
  object SubQueryTests extends SubQueryTests with SqliteSuite
  object UpdateTests extends UpdateTests with SqliteSuite
  object ReturningTests extends ReturningTests with SqliteSuite
}
